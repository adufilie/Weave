/*
    Weave (Web-based Analysis and Visualization Environment)
    Copyright (C) 2008-2011 University of Massachusetts Lowell

    This file is a part of Weave.

    Weave is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License, Version 3,
    as published by the Free Software Foundation.

    Weave is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Weave.  If not, see <http://www.gnu.org/licenses/>.
*/

package weave.servlets;

import java.io.File;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;
import java.util.Vector;

import javax.script.ScriptException;

import org.postgresql.jdbc2.optional.SimpleDataSource;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPList;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.REXPUnknown;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.RFactor;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import weave.beans.HierarchicalClusteringResult;
import weave.beans.LinearRegressionResult;
import weave.beans.RResult;
import weave.utils.DebugTimer;
import weave.utils.ListUtils;
import weave.utils.Strings;


public class RServiceUsingRserve 
{
	public RServiceUsingRserve()
	{
	}

	private static String rFolderName = "R_output";
	
	private static RConnection getRConnection() throws RemoteException
	{
		
		
		RConnection rConnection = null; // establishing R connection		
		try
		{
			rConnection = new RConnection();
		}
		catch (RserveException e)
		{
			//e.printStackTrace();
			throw new RserveConnectionException(e);
		}
		return rConnection;
	}
	public  static class RserveConnectionException extends RemoteException{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public  RserveConnectionException(Exception e){
			super("Unable to connect to RServe",e);
		}
	}

	private static String plotEvalScript( RConnection rConnection,String docrootPath , String script, boolean showWarnings) throws RserveException, REXPMismatchException
	{
		String file = String.format("user_script_%s.jpg", UUID.randomUUID());
		String dir = docrootPath + rFolderName + "/";
		(new File(dir)).mkdirs();
		String str = null;
		try
		{
			str = String.format("jpeg(\"%s\")", dir + file);
			evalScript(rConnection, str, showWarnings);
			rConnection.eval(str = script);
			
			rConnection.eval(str = script);
			rConnection.eval(str = "dev.off()");
		}
		catch (RserveException e)
		{
			System.out.println(str);
			throw e;
		}
		catch (REXPMismatchException e)
		{
			System.out.println(str);
			throw e;
		}
		return rFolderName + "/" + file;
	}
	
	private static String timeLogString = "";
	private static DebugTimer debugger = new DebugTimer();
	private static boolean clearCacheTimeLog;
	
	//used for time logging in the aws home page 
	public static String getCurrentTime(String message)
	{
		String timedMessage = ""; 
		Calendar clr = Calendar.getInstance();
		SimpleDateFormat dformdate = new SimpleDateFormat("MM/dd/yyyy");
		SimpleDateFormat dformtime = new SimpleDateFormat("HH:mm:ss");
		timedMessage = message + " " + dformdate.format(clr.getTime()) + " " + dformtime.format(clr.getTime()) + "\n";
		System.out.print(timedMessage);
		return timedMessage;
	}

	
	private static REXP evalScript(RConnection rConnection, String script, boolean showWarnings) throws REXPMismatchException,RserveException
	{
		//debugger.start();
		
		//have a clean slate during every computation
		timeLogString = "";
		
		REXP evalValue = null;
		//debugger.report("Calling R\n");
		if(!clearCacheTimeLog)
		{
			timeLogString = timeLogString + getCurrentTime("Sending data to R :");
			//timeLogString = timeLogString +"\nSending call to R : "+ debugger.get();
		}
		
		if (showWarnings)			
			evalValue =  rConnection.eval("try({ options(warn=2) \n" + script + "},silent=TRUE)");
		else
			evalValue =  rConnection.eval("try({ options(warn=1) \n" + script + "},silent=TRUE)");
		
		if(!clearCacheTimeLog){
			timeLogString = timeLogString  + getCurrentTime("Retrieving results from R :");
			//timeLogString = timeLogString + "\nResults received From R : " + debugger.get() + " ms";
		}
		//debugger.report("Results received From R\n");
		//debugger.stop("End");
		
		return evalValue;
	}
	
	/**
	 * This will wrap an object in an REXP object.
	 * @param object
	 * @return
	 * @throws RemoteException if the object type is unsupported
	 */
	private static REXP getREXP(Object object) throws RemoteException
	{
		/*
		 * <p><table>
		 *  <tr><td> null	<td> REXPNull
		 *  <tr><td> boolean, Boolean, boolean[], Boolean[]	<td> REXPLogical
		 *  <tr><td> int, Integer, int[], Integer[]	<td> REXPInteger
		 *  <tr><td> double, Double, double[], double[][], Double[]	<td> REXPDouble
		 *  <tr><td> String, String[]	<td> REXPString
		 *  <tr><td> byte[]	<td> REXPRaw
		 *  <tr><td> Enum	<td> REXPString
		 *  <tr><td> Object[], List, Map	<td> REXPGenericVector
		 *  <tr><td> RObject, java bean (experimental)	<td> REXPGenericVector
		 *  <tr><td> ROpaque (experimental)	<td> only function arguments (REXPReference?)
		 *  </table>
		 */
		
		// if it's an array...
		if (object instanceof Object[])
		{
			Object[] array = (Object[])object;
			if (array.length == 0)
			{
				return new REXPList(new RList());
			}
			else if (array[0] instanceof String)
			{
				String[] strings = ListUtils.copyStringArray(array, new String[array.length]);
				return new REXPString(strings);
			}
			else if (array[0] instanceof Number)
			{
				double[] doubles = ListUtils.copyDoubleArray(array, new double[array.length]);
				return new REXPDouble(doubles);
			}
			else if (array[0] instanceof Object[]) // 2-d matrix
			{
				// handle 2-d matrix
				RList rList = new RList();
				for (Object item : array)
					rList.add(getREXP(item));

				try {
					return REXP.createDataFrame(rList);
				} catch (REXPMismatchException e) {
					throw new RemoteException("Failed to Create Dataframe",e);
				}
			}
			else
				throw new RemoteException("Unsupported value type");
		}
		
		// handle non-array by wrapping it in an array
		return getREXP(new Object[]{object});
	}

	private  static void assignNamesToVector(RConnection rConnection,String[] inputNames,Object[] inputValues) throws Exception
	{
		for (int i = 0; i < inputNames.length; i++)
		{
			String name = inputNames[i];
			rConnection.assign(name, getREXP(inputValues[i]));
		}
	}
	
	
	private static  void evaluvateInputScript(RConnection rConnection,String script,Vector<RResult> resultVector,boolean showIntermediateResults,boolean showWarnings ) throws ScriptException, RserveException, REXPMismatchException{
		/* REXP evalValue = */ evalScript( rConnection, script, showWarnings);
		if (showIntermediateResults){
			Object storedRdatas = evalScript( rConnection, "ls()", showWarnings);
			if(storedRdatas instanceof REXPString){
				String[] Rdatas =((REXPString) storedRdatas).asStrings();
				for(int i=0;i<Rdatas.length;i++){
					String scriptToAcessRObj = Rdatas[i];
					if(scriptToAcessRObj.compareTo("mycache") == 0)
						continue;
					REXP RobjValue = evalScript(rConnection, scriptToAcessRObj, false);
					//When function reference is called returns null
					if(RobjValue == null)
						continue;
					resultVector.add(new RResult(scriptToAcessRObj, rexp2javaObj(RobjValue)));	
				}
			}			
		}
		
		//To do find a better way of doing this
//		if(evalValue.isList())
//		{
//			Vector<String> names = evalValue.asList().names;
//			resultVector.add(new RResult("columnNames" ,names ));
//			resultVector.add(new RResult("columnValues" ,rexp2javaObj(evalValue) ));		
//		}
		
	}
	
	
	
	public static RResult[] runScript( String docrootPath, String[] inputNames, Object[] inputValues, String[] outputNames, String script, String plotScript, boolean showIntermediateResults, boolean showWarnings) throws Exception
	{		
		RConnection rConnection = getRConnection();
		
		RResult[] results = null;
		Vector<RResult> resultVector = new Vector<RResult>();
		try
		{
			// ASSIGNS inputNames to respective Vector in R "like x<-c(1,2,3,4)"			
			assignNamesToVector(rConnection,inputNames,inputValues);
			
			//evaluvateInputScript(rConnection, script, resultVector, showIntermediateResults, showWarnings);
			
			evaluateWithTypeChecking( rConnection, script, resultVector, showIntermediateResults, showWarnings);
			
			if (plotScript != ""){// R Script to EVALUATE plotScript
				String plotEvalValue = plotEvalScript(rConnection,docrootPath, plotScript, showWarnings);
				resultVector.add(new RResult("Plot Results", plotEvalValue));
			}
			for (int i = 0; i < outputNames.length; i++){// R Script to EVALUATE output Script
				String name = outputNames[i];						
				REXP evalValue = evalScript( rConnection, name, showWarnings);	
				resultVector.add(new RResult(name, rexp2javaObj(evalValue)));					
			}
			// clear R objects
			clearCacheTimeLog = true;
			evalScript( rConnection, "rm(list=ls())", false);
			
		}
		catch (Exception e)	{
			e.printStackTrace();
			System.out.println("printing error");
			System.out.println(e.getMessage());
			String errorStatement = e.getMessage();
			// to send error from R to As3 side results is created with one
			// object			
			resultVector.add(new RResult("Error Statement state", errorStatement));
		}
		finally
		{
			results = new RResult[resultVector.size()];
			resultVector.toArray(results);
			rConnection.close();
		}
		return results;
	}
	
	
	

	public static LinearRegressionResult linearRegression(String docrootPath,double[] dataX, double[] dataY) throws RemoteException
	{
		RConnection rConnection = getRConnection();
		if (dataX.length == 0 || dataY.length == 0)
			throw new RemoteException("Unable to run computation on zero-length arrays.");
		if (dataX.length != dataY.length)
			throw new RemoteException("Unable to run computation on two arrays with different lengths (" + dataX.length
					+ " != " + dataY.length + ").");
		// System.out.println("entering linearRegression()");
		// System.out.println("got r connection");
		LinearRegressionResult result = new LinearRegressionResult();
		try
		{

			// Push the data to R
			rConnection.assign("x", dataX);
			rConnection.assign("y", dataY);

			// Perform the calculation
			rConnection.eval("fit <- lm(y~x)");

			// option to draw the plot, regression line and store the image

			rConnection.eval(String.format("jpeg(\"%s\")", docrootPath + rFolderName + "/Linear_Regression.jpg"));
			rConnection.eval("plot(x,y)");
			rConnection.eval("abline(fit)");
			rConnection.eval("dev.off()");

			// Get the data from R
			result.setIntercept(rConnection.eval("coefficients(fit)[1]").asDouble());
			result.setSlope(rConnection.eval("coefficients(fit)[2]").asDouble());
			result.setRSquared(rConnection.eval("summary(fit)$r.squared").asDouble());
			result.setSummary("");// rConnection.eval("summary(fit)").asString());
			result.setResidual(rConnection.eval("resid(fit)").asDoubles());

		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new RemoteException(e.getMessage());
		}
		finally
		{
			rConnection.close();
		}
		return result;
	}
	
	/*
	 * Taken from rJava Opensource code and 
	 * added support for  Rlist
	 * added support for RFactor(REngine)
	 */
	private static Object rexp2javaObj(REXP rexp) throws REXPMismatchException {
		
		
		if(rexp == null || rexp.isNull() || rexp instanceof REXPUnknown) {
			return null;
		}
		if(rexp.isVector()) {
			int len = rexp.length();
			if(rexp.isString()) {
				return len == 1 ? rexp.asString() : rexp.asStrings();
			}
			if(rexp.isFactor()){
				return rexp.asFactor();
			}
			if(rexp.isInteger()) {
				return len == 1 ? rexp.asInteger() : rexp.asIntegers();
			}
			if(rexp.isNumeric()) {
				int[] dim = rexp.dim();
				return (dim != null && dim.length == 2) ? rexp.asDoubleMatrix() :
					(len == 1) ? rexp.asDouble() : rexp.asDoubles();
			}
			if(rexp.isLogical()) {
				boolean[] bools = ((REXPLogical)rexp).isTRUE();
				return len == 1 ? bools[0] : bools;
			}
			if(rexp.isRaw()) {
				return rexp.asBytes();
			}
			
			
			if(rexp.isList()) {
				
				Vector<String> namesVector = rexp.asList().names;
				String [] namesArray = namesVector.toArray(new String[namesVector.size()]);
				
				RList rList = rexp.asList();
				Object[] listOfREXP = rList.toArray() ;
				//convert object in List as Java Objects
				// eg: REXPDouble as Double or Doubles
				for(int i = 0; i < listOfREXP.length ;  i++)
				{
				
					REXP obj = (REXP)listOfREXP[i];
					
					Object javaObj =  rexp2javaObj(obj);
					
					if(javaObj instanceof RFactor)
					{
						RFactor factorjavaObj = (RFactor)javaObj;
						String[] levels = factorjavaObj.asStrings();
						listOfREXP[i] = levels;
						
					}
			
					else {
						listOfREXP[i] =  javaObj;
					}
					
				}
				return listOfREXP;
			}
		}
		else{//rlist
			
			return rexp.toDebugString();
		}
		return rexp;
		
		
	}

	//testing for JavascriptAPI calls
	private static Vector<RResult> evaluateWithTypeChecking(RConnection rConnection, String script, Vector<RResult> newResultVector, boolean showIntermediateResults, boolean showWarnings ) throws ScriptException, RserveException, REXPMismatchException 
	{
		REXP evalValue= evalScript(rConnection, script, showWarnings);
		Object resultArray = rexp2javaObj(evalValue);
		Object[] columns;
		if (resultArray instanceof Object[])
		{
			columns = (Object[])resultArray;
		}
		else
		{
			throw new ScriptException(String.format("Script result is not an Array as expected: \"%s\"", resultArray));
		}

		Object[][] final2DArray;//collecting the result as a two dimensional arrray 
		
		Vector<String> names = evalValue.asList().names;
		
	//try{
			//getting the rowCounter variable 
			int rowCounter = 0;
			/*picking up first one to determine its length, 
			all objects are different kinds of arrays that have the same length
			hence it is necessary to check the type of the array*/
			Object currentRow = columns[0];
			if(currentRow instanceof int[])
			{
				rowCounter = ((int[]) currentRow).length;
									
			}
			else if (currentRow instanceof Integer[])
			{
				rowCounter = ((Integer[]) currentRow).length;
				
			}
			else if(currentRow instanceof double[])
			{
				rowCounter = ((double[]) currentRow).length;
			}
			else if(currentRow instanceof RFactor)
			{
				rowCounter = ((RFactor[]) currentRow).length;
			}
			else if(currentRow instanceof String[])
			{
				rowCounter = ((String[]) currentRow).length;
			}
			
			int columnHeadingsCount = 1;
			
			rowCounter = rowCounter + columnHeadingsCount;//we add an additional row for column Headings
			
			final2DArray = new Object[rowCounter][columns.length];
			
			//we need to push the first entry as column names to generate this structure
			/*[
			["k","x","y","z"]
			["k1",1,2,3]
			["k2",3,4,6]
			["k3",2,4,56]
			] */
		
			String [] namesArray = new String[names.size()];
			names.toArray(namesArray);
			final2DArray[0] = namesArray;//first entry is column names
			
			for( int j = 1; j < rowCounter; j++)
			{
				ArrayList<Object> tempList = new ArrayList<Object>();//one added for every column in 'columns'
				for(int f =0; f < columns.length; f++){
					//pick up one column
					Object currentCol = columns[f];
					//check its type
					if(currentCol instanceof int[])
					{
						//the second index in the new list should coincide with the first index of the columns from which values are being picked
						tempList.add(f, ((int[])currentCol)[j-1]);
					}
					else if (currentCol instanceof Integer[])
					{
						tempList.add(f,((Integer[])currentCol)[j-1]);
					}
					else if(currentCol instanceof double[])
					{
						tempList.add(f,((double[])currentCol)[j-1]);
					}
					else if(currentCol instanceof RFactor)
					{
						tempList.add(f,((RFactor[])currentCol)[j-1]);
					}
					else if(currentCol instanceof String[])
					{
						tempList.add(f,((String[])currentCol)[j-1]);
					}
				}
				Object[] tempArray = new Object[columns.length];
				tempList.toArray(tempArray);
				final2DArray[j] = tempArray;//after the first entry (column Names)
				//final2DArray.add(tempList);
				//tempList.clear();
			}
			
			System.out.print(final2DArray);
			newResultVector.add(new RResult("endResult", final2DArray));
			newResultVector.add(new RResult("timeLogString", timeLogString));
			

			return newResultVector;
			
	//	}
	//	catch (Exception e){
			//e.printStackTrace();
	//	}
		
//do the rest to generate a single continuous string representation of the result 
		//	String finalresultString = "";
//		String namescheck = Strings.join(",", names);
//		finalresultString = finalresultString.concat(namescheck);
//		finalresultString = finalresultString.concat("\n");
//
//		
//
//		int numberOfRows = 0;
//		
//		Vector<String[]> columnsInStrings = new Vector<String[]>();
//		
//		String[] tempStringArray = new String[0];
//		
//		try
//		{
//			for (int r= 0; r < columns.length; r++)					
//			{
//				Object currentColumn = columns[r];
//						
//						if(currentColumn instanceof int[])
//						{
//							 int[] columnAsIntArray = (int[])currentColumn;
//							 tempStringArray = new String[columnAsIntArray.length] ; 
//							 for(int g = 0; g < columnAsIntArray.length; g++)
//							 {
//								 tempStringArray[g] = ((Integer)columnAsIntArray[g]).toString();
//							 }
//						}
//						
//						else if (currentColumn instanceof Integer[])
//						{
//							 Integer[] columnAsIntegerArray = (Integer[])currentColumn;
//							 tempStringArray = new String[columnAsIntegerArray.length] ;  
//							 for(int g = 0; g < columnAsIntegerArray.length; g++)
//							 {
//								 tempStringArray[g] = columnAsIntegerArray[g].toString();
//							 }
//						}
//						
//						else if(currentColumn instanceof double[])
//						{
//							double[] columnAsDoubleArray = (double[])currentColumn;
//							 tempStringArray = new String[columnAsDoubleArray.length] ;  
//							 for(int g = 0; g < columnAsDoubleArray.length; g++)
//							 {
//								 tempStringArray[g] = ((Double)columnAsDoubleArray[g]).toString();
//							 }
//						}
//						else if(currentColumn instanceof RFactor)
//						{
//							tempStringArray = ((RFactor)currentColumn).levels();
//						}
//						else if(currentColumn instanceof String[]){
//							 int lent = ((Object[]) currentColumn).length;
//							 //String[] columnAsStringArray = currentColumn;
//							 tempStringArray = new String[lent];  
//							 for(int g = 0; g < lent; g++)
//							 {
//								 tempStringArray[g] = ((Object[]) currentColumn)[g].toString();
//							 }
//						/*	String[] temp = (String[])
//							int arrsize = ((String[])currentColumn).length;
//							tempStringArray = new String[arrsize];
//							tempStringArray = (String[])currentColumn;*/
//						}
//						
//						columnsInStrings.add(tempStringArray);
//						numberOfRows = tempStringArray.length;
//			}
//			
//			
//			//if(rowresult.charAt(rowresult.length()-1) == ',')
//				//rowresult.substring(0, rowresult.length()-1);
//		}
//		catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		for(int currentRow =0; currentRow <numberOfRows; currentRow ++)
//		{
//			for(int currentColumn= 0; currentColumn < columnsInStrings.size(); currentColumn++)
//			{
//				finalresultString += columnsInStrings.get(currentColumn)[currentRow] + ',';
//			}
//			
//			/*remove last comma and  new line*/
//			finalresultString = finalresultString.substring(0, finalresultString.length()-1);
//			finalresultString += '\n';
//		}
		
		//newResultVector.add(new RResult("endResult", finalresultString));
		//newResultVector.add(new RResult("timeLogString", timeLogString));

	}
	
	public static RResult[] kMeansClustering( String[] inputNames, Object[][] inputValues, 
													      boolean showWarnings,
													     	int numberOfClusters, int iterations)throws RemoteException
	{
		RConnection rConnection = getRConnection();	

		int []noOfClusters = new int [1];
		noOfClusters[0] = numberOfClusters;
		
		int[]iterationNumber = new int[1];
		iterationNumber[0] = iterations;
		
		
		try {
			rConnection.assign("clusternumber", noOfClusters);
		} catch (REngineException e1) {
			throw new RemoteException ("",e1);
			
		}
		
		
		//storing column length
		int columnLength = inputValues[0].length; 
		
		//to check if columns are not empty and if all columns are of the same length
	for (int j = 1; j < inputValues.length; j++)
		{
			if (columnLength == 0 || inputValues[j].length == 0)
			throw new RemoteException("Unable to run computation on zero-length arrays.");
			if (inputValues[j].length != columnLength)
			throw new RemoteException("Unable to run computation on two arrays with different lengths (" + columnLength
				+ " != " + inputValues[j].length + ").");
			
		}
		
		
	
	
		String output = "";
		REXP evalValue;	
		RResult [] kClusteringResult;
		String dataframeInput = new String();
		String names = "";
		
		try
		{
//			We have to send columns to R and receive them back to be sent once again to R
			//enables 'n' number of columns to be sent
			for (int i = 0; i < inputNames.length; i++)
			{
				String name = inputNames[i];
				if(names.length() != 0){
					names = names + "," + name;}
				else{
					names = name;
				}
				double[] value = ListUtils.copyDoubleArray(inputValues[i], new double[inputValues[i].length]);
				rConnection.assign(name, value);	
		
				dataframeInput = "data.frame(" + names + ")";
			}
			evalValue = rConnection.eval(dataframeInput);
		
			rConnection.assign("frame",evalValue);
			rConnection.assign("clusternumber", noOfClusters);
			rConnection.assign("iterations",iterationNumber);

			
			//String script = "Clus <- kmeans(frame, "+numberOfClusters+","+iterations+")";
			
//		String clusteringScript = "Clustering <- function(dframe, clusternumber, iterations)\n" +
//									  "{result1 <- kmeans(dframe, clusternumber, iterations)\n " +
//									  "result2 <- kmeans(dframe, clusternumber, (iterations - 1))\n " +
//									  "while(result1$totss != result2$totss)\n"+
//									  "{iterations <- iterations + 1 \n " +
//									  "result1 <- kmeans(dframe, clusternumber, iterations)\n " +
//									  "result2 <- kmeans(dframe, clusternumber, (iterations - 1))\n }" +
//									  "print(result1)" +
//									  "print(result2)" +
//									  "}" +
//									  "KCluResult <- Clustering(frame,clusternumber, iterations)";
	
		    String clusteringScript = "KClusResult <- kmeans(frame, clusternumber,iterations)";
									  
			
			 int i = 0;
			String[] outputNames = {"KClusResult$cluster", "KClusResult$centers"};
			evalScript(rConnection, clusteringScript, showWarnings);
			
			int iterationTimes = outputNames.length;
		
			kClusteringResult = new RResult[outputNames.length];
			for (; i < iterationTimes; i++)
			{
				String name;
				// Boolean addedTolist = false;
				if (iterationTimes == outputNames.length + 1){
					name = outputNames[i - 1];
				}
				else{
					name = outputNames[i];
				}
				// Script to get R - output
				evalValue = evalScript( rConnection, name, showWarnings);				
//				System.out.println(evalValue);
				if (evalValue.isVector()){
					if (evalValue instanceof REXPString)
						kClusteringResult[i] = new RResult(name, evalValue.asStrings());
					else if (evalValue instanceof REXPInteger)
						kClusteringResult[i] = new RResult(name, evalValue.asIntegers());
					else if (evalValue instanceof REXPDouble){
						if (evalValue.dim() == null)
							kClusteringResult[i] = new RResult(name, evalValue.asDoubles());
						else
							kClusteringResult[i] = new RResult(name, evalValue.asDoubleMatrix());
					}
					else{
						// if no previous cases were true, return debug String
						kClusteringResult[i] = new RResult(name, evalValue.toDebugString());
					}
				}
				else{
					kClusteringResult[i] = new RResult(name, evalValue.toDebugString());
				}
			}
		}
		catch (Exception e)	{
			e.printStackTrace();
			output += e.getMessage();
			// to send error from R to As3 side results is created with one
			// object
			kClusteringResult = new RResult[1];
			kClusteringResult[0] = new RResult("Error Statement", output);
		}
	
		finally
		{
			rConnection.close();
		}
		
		return kClusteringResult;
	}
	


	public static HierarchicalClusteringResult hierarchicalClustering(String docrootPath,double[] dataX, double[] dataY) throws RemoteException
	{
		RConnection rConnection = getRConnection();
		String[] agglomerationMethod = new String[7];
		agglomerationMethod[0] = "ward";
		agglomerationMethod[1] = "average";
		agglomerationMethod[2] = "centroid";
		agglomerationMethod[3] = "single";
		agglomerationMethod[4] = "complete";
		agglomerationMethod[5] = "median";
		agglomerationMethod[6] = "mcquitty";
		String agglomerationMethodType = new String("ward");

		if (dataX.length == 0 || dataY.length == 0)
			throw new RemoteException("Unable to run computation on zero-length arrays.");
		if (dataX.length != dataY.length)
			throw new RemoteException("Unable to run computation on two arrays with different lengths (" + dataX.length
					+ " != " + dataY.length + ").");

		HierarchicalClusteringResult hclresult = new HierarchicalClusteringResult();
		try
		{

			// Push the data to R
			rConnection.assign("x", dataX);
			rConnection.assign("y", dataY);

			// checking for user method match
			for (int j = 0; j < agglomerationMethod.length; j++)
			{
				if (agglomerationMethod[j].equals(agglomerationMethodType))
				{
					rConnection.assign("method", agglomerationMethod[j]);
				}
			}

			// Performing the calculations
			rConnection.eval("dataframe1 <- data.frame(x,y)");
			rConnection.eval("HCluster <- hclust(d = dist(dataframe1), method)");

			// option for drawing the hierarchical tree and storing the image
			rConnection.eval(String.format("jpeg(\"%s\")", docrootPath + rFolderName + "/Hierarchical_Clustering.jpg"));
			rConnection.eval("plot(HCluster, main = \"Hierarchical Clustering\")");
			rConnection.eval("dev.off()");

			// Get the data from R
			hclresult.setClusterSequence(rConnection.eval("HCluster$merge").asDoubleMatrix());
			hclresult.setClusterMethod(rConnection.eval("HCluster$method").asStrings());
			// hclresult.setClusterLabels(rConnection.eval("HCluster$labels").asStrings());
			hclresult.setClusterDistanceMeasure(rConnection.eval("HCluster$dist.method").asStrings());

		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new RemoteException(e.getMessage());
		}
		finally
		{
			rConnection.close();
		}
		return hclresult;
	}


	//this function does not take in a script from the as3 side for imputation, but the script is built in
	public static RResult[] handlingMissingData(String[] inputNames, Object[][] inputValues, String[] outputNames, boolean showIntermediateResults, boolean showWarnings, boolean completeProcess) throws Exception
	{
		RConnection rConnection = getRConnection();
		
		String output = "";
		String script= "";
		REXP evalValue;
		RResult[] mdResult ;
		String bindingInput = new String();
		String names = "";
		
		try
		{
//			We have to send columns to R and receive them back to be sent once again to R
			for (int i = 0; i < inputNames.length; i++)
			{
				String name = inputNames[i];
				if(names.length() != 0){
					names = names + "," + name;}
				else{
					names = name;
				}
				double[] value = ListUtils.copyDoubleArray(inputValues[i], new double[inputValues[i].length]);
				rConnection.assign(name, value);	
		
				bindingInput = "cbind(" + names + ")";
			}
			
			evalValue = rConnection.eval(bindingInput);
			rConnection.assign("Bind",evalValue);
			
			//Built in script
			if(completeProcess = false)
			{
				script = "library(norm) \n pre <- prelim.norm(Bind)";
				
			}
			else
			{
				script = "library(norm) \n pre <- prelim.norm(Bind) \n eeo <- em.norm(pre) \n rngseed(12345) \n" +
				"imputed <- imp.norm(pre, eeo,Bind)";
			}
			
			
					
			evalScript(rConnection, script, showWarnings);
		
			int i = 0;
			int iterationTimes = outputNames.length;
		
			mdResult = new RResult[outputNames.length];
			for (; i < iterationTimes; i++)
			{
				String name;
				// Boolean addedTolist = false;
				if (iterationTimes == outputNames.length + 1){
					name = outputNames[i - 1];
				}
				else{
					name = outputNames[i];
				}
				// Script to get R - output
				evalValue = evalScript( rConnection, name, showWarnings);				
//				System.out.println(evalValue);
				if (evalValue.isVector()){
					if (evalValue instanceof REXPString)
						mdResult[i] = new RResult(name, evalValue.asStrings());
					else if (evalValue instanceof REXPInteger)
						mdResult[i] = new RResult(name, evalValue.asIntegers());
					else if (evalValue instanceof REXPDouble){
						if (evalValue.dim() == null)
							mdResult[i] = new RResult(name, evalValue.asDoubles());
						else
							mdResult[i] = new RResult(name, evalValue.asDoubleMatrix());
					}
					else{
						// if no previous cases were true, return debug String
						mdResult[i] = new RResult(name, evalValue.toDebugString());
					}
				}
				else{
					mdResult[i] = new RResult(name, evalValue.toDebugString());
				}
			}
		}
		catch (Exception e)	{
			e.printStackTrace();
			output += e.getMessage();
			// to send error from R to As3 side results is created with one
			// object
			mdResult = new RResult[1];
			mdResult[0] = new RResult("Error Statement", output);
		}
		finally
		{
			rConnection.close();
		}
		return mdResult;
	}
}

