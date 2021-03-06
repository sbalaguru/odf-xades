/*************************************************************************
 * 
 *  Copyright 2009 by Giuseppe Castagno beppec56@openoffice.org
 *  
 *  The Contents of this file are made available subject to
 *  the terms of European Union Public License (EUPL) version 1.1
 *  as published by the European Community.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the EUPL.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  EUPL for more details.
 *
 *  You should have received a copy of the EUPL along with this
 *  program.  If not, see:
 *  https://www.osor.eu/eupl, http://ec.europa.eu/idabc/eupl.
 *
 ************************************************************************/

package com.yacme.ext.oxsit.comp.options;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XItemListener;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.yacme.ext.oxsit.Helpers;
import com.yacme.ext.oxsit.logging.XOX_Logger;
import com.yacme.ext.oxsit.ooo.GlobConstant;
import com.yacme.ext.oxsit.ooo.registry.MessageConfigurationAccess;
import com.yacme.ext.oxsit.ooo.ui.DialogFileOrFolderPicker;
import com.yacme.ext.oxsit.options.SingleControlDescription;
import com.yacme.ext.oxsit.options.SingleControlDescription.ControlTypeCode;

/**
 * 
 * manages the General option page on Tools > Options...
 * 
 * @author beppe
 *
 */
public class ManageLoggingOptions extends ManageOptions  implements XItemListener {
	// needed for registration
	public static final String			m_sImplementationName	= ManageLoggingOptions.class.getName();
	public static final String[]		m_sServiceNames			= { "com.yacme.ext.oxsit.comp.options.ManageLoggingOptions" };

    private int m_nBrowseSystemPathPB = 0;
	private int m_nEnableFileCtl; // the file enable/disable check box
	//the following are the index inside ArrayOfControls of the controls that need to be
	//enabled/disabled according to the status of the file selection
	private int m_nLogFilePathIdxTF = 0;
	private int m_nLogFileSizeTF;
	private int m_nLogFileCountTF;

    private static final int m_nNumberOfControls = 8;
    
    private String	m_sDialogTitle = "id_opt_dlg_log_dir";

    /**
     * 
     * @param xCompContext
     */
	public ManageLoggingOptions(XComponentContext xCompContext) {
		super(xCompContext, m_nNumberOfControls, "leaf_logging");//leaf refers to OOo documentation about
															// extension options
//DEBUG		m_aLogger.enableLogging();// disabled in base class
		m_aLogger.ctor();
		//prepare the list of controls on the page

		//the parameter sName comes from basic dialog
		//the parameter sProperty comes from file AddonConfiguration.xcs.xml
		//Important the string sName is the name in the basic part of the dialog (the GUI part)
		//
		int iter = 0;
		//checkbox
		SingleControlDescription aControl = 
			new SingleControlDescription("CheckInfo", ControlTypeCode.CHECK_BOX, -1, "EnableInfoLevel", 0, 0, true);
		ArrayOfControls[iter++] = aControl;
//checkbox
		aControl = 
			new SingleControlDescription("CheckDebugLog", ControlTypeCode.CHECK_BOX, -1, GlobConstant.m_sENABLE_DEBUG_LOGGING, 0, 0, true);
		ArrayOfControls[iter++] = aControl;
		aControl = 
			new SingleControlDescription("CheckEnConsole", ControlTypeCode.CHECK_BOX, -1, "EnableConsoleOutput", 0, 0, true);
		ArrayOfControls[iter++] = aControl;
		aControl = 
			new SingleControlDescription("CheckEnFile", ControlTypeCode.CHECK_BOX, -1, "EnableFileOutput", 0, 0, true);
//add to control elements
		aControl.m_xAnItemListener = this;
		m_nEnableFileCtl = iter;
		ArrayOfControls[iter++] = aControl;
		aControl = 
				new	SingleControlDescription("LogFilePath", ControlTypeCode.EDIT_TEXT, -1, "LogFilePath", 0, 0, true);
		m_nLogFilePathIdxTF = iter;
		ArrayOfControls[iter++] = aControl;
//the actionPerformed pushbutton		
		aControl = 
			new SingleControlDescription("BrowseSystemPath", ControlTypeCode.PUSH_BUTTON, -1, "", 0, 0, true);
		m_nBrowseSystemPathPB = iter;
		aControl.m_xAnActionListener = this;
		ArrayOfControls[iter++] = aControl;
//the file elements, counts and size
		aControl = 
			new	SingleControlDescription("LogFileSize", ControlTypeCode.EDIT_TEXT_INT, -1, "MaxFileSize", 100000000, 1000, true );
//set the actionPerformed, for enable/disable
		//....
		m_nLogFileSizeTF = iter;
		ArrayOfControls[iter++] = aControl;
		aControl = 
			new	SingleControlDescription("LogFileCount", ControlTypeCode.EDIT_TEXT_INT, -1, "FileRotationCount",100 , 1, true);
//set the actionPerformed, for enable/disable
		m_nLogFileCountTF = iter;
		ArrayOfControls[iter++] = aControl;
		
//grab the title string for configuration dialog
		MessageConfigurationAccess m_aRegAcc = null;
		m_aRegAcc = new MessageConfigurationAccess(m_xComponentContext, m_xMultiComponentFactory);
		try {
			m_sDialogTitle = m_aRegAcc.getStringFromRegistry( m_sDialogTitle );
		} catch (Exception e) {
			m_aLogger.severe("ctor",e);
		}			
		m_aRegAcc.dispose();
	}

	public String getImplementationName() {
		return m_sImplementationName;
	}

	public String[] getSupportedServiceNames() {
		return m_sServiceNames;
	}

	public boolean supportsService(String _sService) {
		int len = m_sServiceNames.length;

		m_aLogger.info( "supportsService" );
		for (int i = 0; i < len; i++) {
			if (_sService.equals( m_sServiceNames[i] ))
				return true;
		}
		return false;
	}

	@Override
	protected void loadData(com.sun.star.awt.XWindow aWindow)
	  throws com.sun.star.uno.Exception {
		super.loadData(aWindow);
//when return from load, we should have the container initialized, so activate the right state
		//for the subordinate controls
		
		if(m_xContainer != null) {
			//retrieve the file control checkbox
			XControl xControl = m_xContainer.getControl(ArrayOfControls[m_nEnableFileCtl].m_sControlName);
	        XControlModel xControlModel = xControl.getModel();
	        XPropertySet xPSet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xControlModel);
// check the state and set a boolean accordingly
	        boolean bEnable = AnyConverter.toInt(xPSet.getPropertyValue("State")) == 1;
			enableTheFileControls(bEnable);
		}
		else
			m_aLogger.severe("enableTheFileControls", "there is no window!");
	}

	protected void saveData(com.sun.star.awt.XWindow aWindow)
		throws com.sun.star.lang.IllegalArgumentException, com.sun.star.uno.Exception {
		super.saveData(aWindow);
//data saved, now, notifies the Global Logger
		//instantiates the global logger
		
		//and call the updated module
		XOX_Logger m_xLogger = (XOX_Logger)UnoRuntime.queryInterface(XOX_Logger.class, 
				m_xComponentContext.getValueByName(GlobConstant.m_sSINGLETON_LOGGER_SERVICE_INSTANCE));
			if(m_xLogger != null) {
				m_xLogger.optionsConfigurationChanged();
			}
			else {
//notifies the changed config
				System.out.println("no main logger!");
			}
	}	
	/* (non-Javadoc)
	 * @see com.sun.star.awt.XActionListener#actionPerformed(com.sun.star.awt.ActionEvent)
	 */
	public void actionPerformed(ActionEvent rEvent) {
		m_aLogger.entering("actionPerformed");
        try{
            // get the control that has fired the event,
            XControl xControl = (XControl) UnoRuntime.queryInterface(XControl.class, rEvent.Source);
            XControlModel xControlModel = xControl.getModel();
            XPropertySet xPSet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xControlModel);
            String sName = (String) xPSet.getPropertyValue("Name");
            // just in case the listener has been added to several controls,
            // we make sure we refer to the right one
            if (sName.equals(ArrayOfControls[m_nBrowseSystemPathPB].m_sControlName)) {
            	m_aLogger.info("browse the system for a path");
//standard dialog for file/folder access
            	DialogFileOrFolderPicker aDlg = new DialogFileOrFolderPicker(m_xMultiComponentFactory,m_xComponentContext);
// we need to get the frame, the component context and from it the multiservice factory
            	//grab the current text value, expressed in system path
            	String sStartFolder = "";
            	{
	    		    xControl = ArrayOfControls[m_nLogFilePathIdxTF].m_xTheControl;
	    	    	XPropertySet xProp = (XPropertySet) UnoRuntime.queryInterface(
	    	    			XPropertySet.class, xControl.getModel());
	    	    	if (xProp == null)
	    	    		throw new com.sun.star.uno.Exception(
	    	    				"Could not get XPropertySet from control.", this);
		    		String sTheText = 
		    			AnyConverter.toString( xProp.getPropertyValue( "Text" ) );

		    		if(sTheText.length() == 0) {
		    			//init to user home directory
		    			sTheText = System.getProperty("user.home");
		    		}
	    			//create a new file only with the parent of the full path, that is the directory
	    			//with this dirty trick we separate the two part, file and folder
	    			//to grab the path only
	    			File aFileFolder = new File(sTheText);
	    			URI aUri = aFileFolder.toURI();
	    			//then form the URL for the dialog
					sStartFolder = aUri.getScheme()+"://" + aUri.getPath();									    			
	    			m_aLogger.log(sStartFolder);
            	}
            	//call the dialog
            	String aPath = aDlg.runFolderPicker(m_sDialogTitle, sStartFolder);
            	//the returned path is a URL, change into the system path
            	if(aPath.length() > 0) {
					String aFile = "";
					try {
						aFile = Helpers.fromURLtoSystemPath(aPath);
					} catch (URISyntaxException e) {
						m_aLogger.severe("actionPerformed", e);
					} catch (IOException e) {
						m_aLogger.severe("actionPerformed", e);
					}
	    			//grab the current control
	    		    xControl = ArrayOfControls[m_nLogFilePathIdxTF].m_xTheControl;
	    	    	XPropertySet xProp = (XPropertySet) UnoRuntime.queryInterface(
	    	    			XPropertySet.class, xControl.getModel());
	    	    	if (xProp == null)
	    	    		throw new com.sun.star.uno.Exception(
	    	    				"Could not get XPropertySet from control.", this);
	    			xProp.setPropertyValue("Text", aFile);
            	}
            }
            else {
            	m_aLogger.info("Activated: "+sName);            	
            }
        }catch (com.sun.star.uno.Exception ex){
            // perform individual exception handling here.
            // Possible exception types are:
            // com.sun.star.lang.WrappedTargetException,
            // com.sun.star.beans.UnknownPropertyException,
            // com.sun.star.uno.Exception
        	m_aLogger.severe("", "", ex);
        }		
	}

	/* (non-Javadoc)
	 * @see com.sun.star.awt.XItemListener#itemStateChanged(com.sun.star.awt.ItemEvent)
	 */
	public void itemStateChanged(ItemEvent rIEvent) {
		m_aLogger.entering("itemStateChanged");
        try{
            // get the control that has fired the event,
            XControl xControl = (XControl) UnoRuntime.queryInterface(XControl.class, rIEvent.Source);
            XControlModel xControlModel = xControl.getModel();
            XPropertySet xPSet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xControlModel);
            
//FIXME DEBUG            Utilities1.showProperties(xControlModel, xPSet);
            
            String sName = (String) xPSet.getPropertyValue("Name");
            // just in case the listener has been added to several controls,
            // we make sure we refer to the right one
            if (sName.equals(ArrayOfControls[m_nEnableFileCtl].m_sControlName)) {
//            	m_aLoggerDialog.info("check box of file changed state");
            	// retrieve the status of the control
                int nState = AnyConverter.toInt(xPSet.getPropertyValue("State"));
//FIXME DEBUg                m_aLoggerDialog.info("itemStateChanged","State is "+nState);
            	// if the control is active, enables the relevant controls else disable them            	
                enableTheFileControls(( nState == 0 ) ? false : true); 
            }
            else {
            	m_aLogger.info("Activated: "+sName);            	
            }
        } catch (com.sun.star.uno.Exception ex){
            // perform individual exception handling here.
            // Possible exception types are:
            // com.sun.star.lang.WrappedTargetException,
            // com.sun.star.beans.UnknownPropertyException,
            // com.sun.star.uno.Exception
        	m_aLogger.severe("", "", ex);
        }		
	}

	protected void enableOneFileControl(boolean _bEnable, int _index) {
		XControl xControl = m_xContainer.getControl(ArrayOfControls[_index].m_sControlName);
		ArrayOfControls[_index].m_bEnableSave = _bEnable; // enable the saving
        XControlModel xControlModel = xControl.getModel();
        XPropertySet xPSet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xControlModel);
        try {
			xPSet.setPropertyValue("Enabled", new Boolean((_bEnable) ? true : false));
		} catch (UnknownPropertyException e) {
			m_aLogger.severe("enableOneFileControl", "", e);
		} catch (PropertyVetoException e) {
			m_aLogger.severe("enableOneFileControl", "", e);
		} catch (IllegalArgumentException e) {
			m_aLogger.severe("enableOneFileControl", "", e);
		} catch (WrappedTargetException e) {
			m_aLogger.severe("enableOneFileControl", "", e);
		}
	}

	/**
	 * 
	 * @param _bEnable true enable the four controls the file checkbox enables/disables
	 */
	protected void enableTheFileControls(boolean _bEnable) {
// retrieve the controls
		//grab the current control
		if(m_xContainer != null) {
			enableOneFileControl(_bEnable,m_nLogFilePathIdxTF);
			enableOneFileControl(_bEnable,m_nBrowseSystemPathPB);
			enableOneFileControl(_bEnable,m_nLogFileCountTF);
			enableOneFileControl(_bEnable,m_nLogFileSizeTF);
		}
		else
			m_aLogger.severe("enableTheFileControls", "there is no window!");
	}
}
