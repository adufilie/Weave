/* ***** BEGIN LICENSE BLOCK *****
 *
 * This file is part of the Weave API.
 *
 * The Initial Developer of the Weave API is the Institute for Visualization
 * and Perception Research at the University of Massachusetts Lowell.
 * Portions created by the Initial Developer are Copyright (C) 2008-2012
 * the Initial Developer. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * ***** END LICENSE BLOCK ***** */

package
{
	import avmplus.DescribeType;
	
	import flash.utils.Dictionary;
	import flash.utils.getDefinitionByName;
	
	import mx.core.FlexGlobals;
	
	import weave.api.core.IClassRegistry;
	import weave.api.core.IErrorManager;
	import weave.api.core.IExternalSessionStateInterface;
	import weave.api.core.ILinkableHashMap;
	import weave.api.core.ILocaleManager;
	import weave.api.core.IProgressIndicator;
	import weave.api.core.ISessionManager;
	import weave.api.core.IStageUtils;
	import weave.api.data.IAttributeColumnCache;
	import weave.api.data.ICSVParser;
	import weave.api.data.IProjectionManager;
	import weave.api.data.IQualifiedKeyManager;
	import weave.api.data.IStatisticsCache;
	import weave.api.services.IURLRequestUtils;
	import weave.api.ui.IEditorManager;
	import weave.core.ClassRegistryImpl;

	/**
	 * Static functions for managing implementations of Weave framework classes.
	 * 
	 * @author adufilie
	 */
	public class WeaveAPI
	{
		/**
		 * For use with StageUtils.startTask(); this priority is used for things that must be done before anything else.
		 * Tasks having this priority will take over the scheduler and prevent any other asynchronous task from running until it is completed.
		 */
		public static const TASK_PRIORITY_0_IMMEDIATE:uint = 0;
		/**
		 * For use with StageUtils.startTask(); this priority is associated with rendering.
		 */
		public static const TASK_PRIORITY_1_RENDERING:uint = 1;
		/**
		 * For use with StageUtils.startTask(); this priority is associated with data manipulation tasks such as building an index.
		 */
		public static const TASK_PRIORITY_2_BUILDING:uint = 2;
		/**
		 * For use with StageUtils.startTask(); this priority is associated with parsing raw data.
		 */
		public static const TASK_PRIORITY_3_PARSING:uint = 3;
		
		/**
		 * Static instance of ClassRegistry
		 */
		private static var _classRegistry:ClassRegistryImpl = null;
		
		/**
		 * This is the singleton instance of the registered ISessionManager implementation.
		 */
		public static function get ClassRegistry():IClassRegistry
		{
			if (!_classRegistry)
			{
				_classRegistry = new ClassRegistryImpl();
				
				///////////////////////
				// TEMPORARY SOLUTION (until everything is a plug-in.)
				// run static initialization code to register weave implementations
//				try
//				{
					/*
					// before initializing other classes, initialize all public properties of this class
					for each (var array:Object in DescribeType.getClassInfo(WeaveAPI).traits)
					for each (var item:Object in array)
					if (item.metadata.access != 'writeonly')
					WeaveAPI[item.name];
					*/
					
					getDefinitionByName("_InitializeWeaveCore");
					getDefinitionByName("_InitializeWeaveData"); 
					getDefinitionByName("_InitializeWeaveUISpark");
					getDefinitionByName("_InitializeWeaveUI");
//				}
//				catch (e:Error)
//				{
//					trace(e.getStackTrace() || e);
//				}
				// END TEMPORARY SOLUTION
				///////////////////////////
			}
			return _classRegistry;
		}
		
		/**
		 * This is the singleton instance of the registered ISessionManager implementation.
		 */
		public static function get SessionManager():ISessionManager
		{
			if (!_classRegistry)
				ClassRegistry;
			return _classRegistry.singletonInstances[ISessionManager]
				|| _classRegistry.getSingletonInstance(ISessionManager);
		}
		/**
		 * This is the singleton instance of the registered IStageUtils implementation.
		 */
		public static function get StageUtils():IStageUtils
		{
			if (!_classRegistry)
				ClassRegistry;
			return _classRegistry.singletonInstances[IStageUtils]
				|| _classRegistry.getSingletonInstance(IStageUtils);
		}
		/**
		 * This is the singleton instance of the registered IErrorManager implementation.
		 */
		public static function get ErrorManager():IErrorManager
		{
			if (!_classRegistry)
				ClassRegistry;
			return _classRegistry.singletonInstances[IErrorManager]
				|| _classRegistry.getSingletonInstance(IErrorManager);
		}
		/**
		 * This is the singleton instance of the registered IExternalSessionStateInterface implementation.
		 */
		public static function get ExternalSessionStateInterface():IExternalSessionStateInterface
		{
			if (!_classRegistry)
				ClassRegistry;
			return _classRegistry.singletonInstances[IExternalSessionStateInterface]
				|| _classRegistry.getSingletonInstance(IExternalSessionStateInterface);
		}
		/**
		 * This is the singleton instance of the registered IProgressIndicator implementation.
		 */
		public static function get ProgressIndicator():IProgressIndicator
		{
			if (!_classRegistry)
				ClassRegistry;
			return _classRegistry.singletonInstances[IProgressIndicator]
				|| _classRegistry.getSingletonInstance(IProgressIndicator);
		}
		/**
		 * This is the singleton instance of the registered IAttributeColumnCache implementation.
		 */
		public static function get AttributeColumnCache():IAttributeColumnCache
		{
			if (!_classRegistry)
				ClassRegistry;
			return _classRegistry.singletonInstances[IAttributeColumnCache]
				|| _classRegistry.getSingletonInstance(IAttributeColumnCache);
		}
		/**
		 * This is the singleton instance of the registered IStatisticsCache implementation.
		 */
		public static function get StatisticsCache():IStatisticsCache
		{
			if (!_classRegistry)
				ClassRegistry;
			return _classRegistry.singletonInstances[IStatisticsCache]
				|| _classRegistry.getSingletonInstance(IStatisticsCache);
		}
		/**
		 * This is the singleton instance of the registered IProjectionManager implementation.
		 */
		public static function get ProjectionManager():IProjectionManager
		{
			if (!_classRegistry)
				ClassRegistry;
			return _classRegistry.singletonInstances[IProjectionManager]
				|| _classRegistry.getSingletonInstance(IProjectionManager);
		}
		/**
		 * This is the singleton instance of the registered IQualifiedKeyManager implementation.
		 */
		public static function get QKeyManager():IQualifiedKeyManager
		{
			if (!_classRegistry)
				ClassRegistry;
			return _classRegistry.singletonInstances[IQualifiedKeyManager]
				|| _classRegistry.getSingletonInstance(IQualifiedKeyManager);
		}
		/**
		 * This is the singleton instance of the registered ICSVParser implementation.
		 */
		public static function get CSVParser():ICSVParser
		{
			if (!_classRegistry)
				ClassRegistry;
			return _classRegistry.singletonInstances[ICSVParser]
				|| _classRegistry.getSingletonInstance(ICSVParser);
		}
		/**
		 * This is the singleton instance of the registered IURLRequestUtils implementation.
		 */
		public static function get URLRequestUtils():IURLRequestUtils
		{
			if (!_classRegistry)
				ClassRegistry;
			return _classRegistry.singletonInstances[IURLRequestUtils]
				|| _classRegistry.getSingletonInstance(IURLRequestUtils);
		}
		/**
		 * This is the singleton instance of the registered ILocaleManager implementation.
		 */
		public static function get LocaleManager():ILocaleManager
		{
			if (!_classRegistry)
				ClassRegistry;
			return _classRegistry.singletonInstances[ILocaleManager]
				|| _classRegistry.getSingletonInstance(ILocaleManager);
		}
		/**
		 * This is the singleton instance of the registered IEditorManager implementation.
		 */
		public static function get EditorManager():IEditorManager
		{
			if (!_classRegistry)
				ClassRegistry;
			return _classRegistry.singletonInstances[IEditorManager]
				|| _classRegistry.getSingletonInstance(IEditorManager);
		}
		/**
		 * This is the top-level object in Weave.
		 */		
		public static function get globalHashMap():ILinkableHashMap
		{
			if (!_classRegistry)
				ClassRegistry;
			return _classRegistry.singletonInstances[ILinkableHashMap]
				|| _classRegistry.getSingletonInstance(ILinkableHashMap);
		}
		/**************************************/

		
		/**
		 * This returns the top level application as defined by FlexGlobals.topLevelApplication
		 * or WeaveAPI.topLevelApplication if FlexGlobals isn't defined.
		 */
		public static function get topLevelApplication():Object
		{
			return FlexGlobals.topLevelApplication;
		}
		
		private static const _javaScriptInitialized:Dictionary = new Dictionary();
		
		/**
		 * This will be true after initializeJavaScript() completes successfully.
		 */
		public static function get javaScriptInitialized():Boolean
		{
			return !!_javaScriptInitialized[WeavePath];
		}
		
		/**
		 * This function will initialize the external API so calls can be made from JavaScript to Weave.
		 * After initializing, this will call an external function weave.apiReady(weave) if it exists, where
		 * 'weave' is a pointer to the instance of Weave that was initialized.
		 * @param scripts A list of JavaScript files containing initialization code, each given as a Class (for an embedded file) or a String.
		 *                Within the script, the "weave" variable can be used as a pointer to the Weave instance.
		 * 
		 * @example Example
		 * <listing version="3.0">
		 *     [Embed(source="MyScript.js", mimeType="application/octet-stream")]
		 *     private static const MyScript:Class;
		 * 
		 *     WeaveAPI.initializeJavaScript(MyScript);
		 * </listing>
		 */
		public static function initializeJavaScript(...scripts):void
		{
			if (!JavaScript.available)
				return;
			
			// we want WeavePath to be initialized first
			var firstTime:Boolean = !javaScriptInitialized;
			if (firstTime)
			{
				// always include WeavePath
				if (scripts.indexOf(WeavePath) < 0)
					scripts.unshift(WeavePath);
				
				// initialize Direct API before anything else
				scripts.unshift(IExternalSessionStateInterface);
			}
			
			try
			{
				for each (var script:Object in scripts)
				{
					// skip scripts we've already initialized
					if (_javaScriptInitialized[script])
						continue;
					
					if (script is Class)
					{
						var instanceInfo:Object = DescribeType.getInfo(script, DescribeType.INCLUDE_TRAITS | DescribeType.INCLUDE_METHODS | DescribeType.HIDE_NSURI_METHODS | DescribeType.USE_ITRAITS | DescribeType.INCLUDE_BASES);
						if (instanceInfo.traits.bases.indexOf('mx.core::ByteArrayAsset') >= 0)
						{
							// run embedded script file
							JavaScript.exec({"this": "weave"}, new script());
						}
						else
						{
							// initialize interface
							registerJavaScriptInterface(ClassRegistry.getSingletonInstance(script as Class), instanceInfo);
						}
					}
					else
					{
						// run the script
						JavaScript.exec({"this": "weave"}, script);
					}
					
					// remember that we initialized the script
					_javaScriptInitialized[script] = true;
				}
				
				if (firstTime)
				{
					// call external weaveApiReady(weave)
					JavaScript.exec(
						{method: "weaveApiReady"},
						'if (this[method]) this[method](this);',
						'if (window[method]) window[method](this);'
					);
				}
			}
			catch (e:*)
			{
				handleExternalError(e);
			}
		}

		[Embed(source="WeavePath.js", mimeType="application/octet-stream")]
		public static const WeavePath:Class;

		/**
		 * Calls external function(s) weave.weaveReady(weave) and/or window.weaveReady(weave).
		 */		
		public static function callExternalWeaveReady():void
		{
			initializeJavaScript();
			
			if (!JavaScript.available)
				return;
			
			try
			{
				JavaScript.exec(
					{method: "weaveReady"},
					'if (this[method]) this[method](this);',
					'if (window[method]) window[method](this);'
				);
			}
			catch (e:Error)
			{
				handleExternalError(e);
			}
		}

		/**
		 * Exposes an interface to JavaScript.
		 * @param host The host object containing methods to expose to JavaScript.
		 * @param typeInfo Type information from describeTypeJSON() listing methods to expose.
		 */
		public static function registerJavaScriptInterface(host:Object, classInfo:Object):void
		{
			// register each external interface function
			for each (var methodInfo:Object in classInfo.traits.methods)
				JavaScript.registerMethod(methodInfo.name, host[methodInfo.name]);
		}
		
		private static function handleExternalError(e:Error):void
		{
			if (e.errorID == 2060)
				ErrorManager.reportError(e, "In the HTML embedded object tag, make sure that the parameter 'allowScriptAccess' is set appropriately. " + e.message);
			else
				ErrorManager.reportError(e);
		}
		
		/**
		 * Outputs to external console.log()
		 */
		public static function externalTrace(...params):void
		{
			JavaScript.exec(
				{"params": params},
				"console.log.apply(console, params)"
			);
		}
		
		/**
		 * Outputs to external console.error()
		 */
		public static function externalError(...params):void
		{
			JavaScript.exec(
				{"params": params},
				"console.error.apply(console, params)"
			);
		}
	}
}
