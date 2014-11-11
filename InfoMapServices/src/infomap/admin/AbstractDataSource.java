package infomap.admin;

import infomap.utils.ArrayUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;

import weave.utils.FileUtils;

/**
 * IDataSource: an abstract class for defining new data sources to query from.
 * 
 * @author Sebastin Kolman
 *
 */
public abstract class AbstractDataSource implements Runnable
{
	public static int documentCap = 100;
	public String[] requiredQueryTerms;
	
	public String[] relatedQueryTerms;
	
	public String solrServerURL;

	/**
	 * Returns the source name, maybe used by other classes to get results from specific sources
	 * @return source name
	 */
	abstract String getSourceName();
	
	/**
	 * Makes a query for the terms mentioned in query and returns an 2D array of the results.
	 * 
	 * @param query the query terms to search for
	 * @return An array of SolrInputDocument 
	 */
	abstract SolrInputDocument[] searchForQuery();
	
	/**
	 * Returns total number of results for given query terms 
	 */
	abstract long getTotalNumberOfQueryResults();
	
	abstract String getSourceType();
	/**
	 * This function should be called after getting the search results for a query
	 * @param results An array of SolrInputDocument
	 * Any additional 
	 */
	void updateSolrServer(SolrInputDocument[] results)
	{
		if(results == null)
			return;
		System.out.println("adding "+ results.length + " documents " + "for " + getSourceName());
		HttpSolrServer solrServer = new HttpSolrServer(solrServerURL);
		ArrayList<SolrInputDocument>updatedResults = new ArrayList<SolrInputDocument>();
		int count = 0;
		for(int i=0;i<results.length;i++)
		{
			try{
				SolrQuery q = new SolrQuery().setQuery("link:"+"\""+(String)results[i].getFieldValue("link")+"\"");
				QueryResponse resp = solrServer.query(q);
				if(resp.getResults().getNumFound()>0)
				{
//					System.out.println("DOCUMENT ALREADY ADDED " + (String)results[i].getFieldValue("link"));
					continue;
				}
				else{
					updatedResults.add(results[i]);
					count++;
				}
			}catch (Exception e) {
				System.out.println("Error querying SOlr Server in Data source " + getSourceName());
				continue;
			}
			
			
		}
		
		SolrInputDocument[] updatedResultsArray = new SolrInputDocument[updatedResults.size()];
		AdminService.addDocuments(updatedResults.toArray(updatedResultsArray),solrServerURL);
	}
	
	Boolean copyImageFromURL(String sourceURL,String imageName)
	{
		int index = sourceURL.lastIndexOf('.');
		
		String imgExtension = sourceURL.substring(index, sourceURL.length());
		
		Properties prop = new Properties();
		
		try
		{
			InputStream config = getClass().getClassLoader().getResourceAsStream("infomap/resources/config.properties");
			prop.load(config);
			String thumbnailPath = prop.getProperty("thumbnailPath");
			String destinationPath = thumbnailPath + imageName + imgExtension; 
			
			return FileUtils.copyFileFromURL(sourceURL, destinationPath);
		
		}catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/* why this?*/
	public String[] getRequiredQueryTerms()
	{
		String[] requiredTerms;
		if(relatedQueryTerms == null || relatedQueryTerms.length == 0)
		{
			requiredTerms = new String[1];
			requiredTerms[0] = ArrayUtils.joinArrayElements(requiredQueryTerms, " ");
		}
		else
		{
			requiredTerms = requiredQueryTerms;
		}
		return requiredTerms;
	}
	
	public void run()
	{
		if(requiredQueryTerms.length>0)
		{
			SolrInputDocument[] results = searchForQuery();
			updateSolrServer(results);
		}else
		{
			System.out.println("Query Terms are empty for " + getSourceName());
		}
		
	}
}