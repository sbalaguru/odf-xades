/* ***** BEGIN LICENSE BLOCK ********************************************
 * Version: EUPL 1.1/GPL 3.0
 * 
 * The contents of this file are subject to the EUPL, Version 1.1 or 
 * - as soon they will be approved by the European Commission - 
 * subsequent versions of the EUPL (the "Licence");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.osor.eu/eupl/european-union-public-licence-eupl-v.1.1
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is /oxsit-custom_it/src/com/yacme/ext/oxsit/cust_it/comp/security/DocumentSigner_IT.java.
 *
 * The Initial Developer of the Original Code is
 * Giuseppe Castagno giuseppe.castagno@acca-esse.it
 * 
 * Portions created by the Initial Developer are Copyright (C) 2009-2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 3 or later (the "GPL")
 * in which case the provisions of the GPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of the GPL, and not to allow others to
 * use your version of this file under the terms of the EUPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EUPL, or the GPL.
 *
 * ***** END LICENSE BLOCK ******************************************** */

package com.yacme.ext.oxsit.comp;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.star.lang.XServiceInfo;
import com.sun.star.lib.uno.helper.ComponentBase;
import com.sun.star.uno.XComponentContext;
import com.yacme.ext.oxsit.logging.LocalLogFormatter;
import com.yacme.ext.oxsit.logging.XOX_Logger;
import com.yacme.ext.oxsit.ooo.GlobConstant;
import com.yacme.ext.oxsit.singleton.LoggerParametersAccess;

/**
 * This class is a singleton UNO object.
 * 
 * This class implements the global logger for the extension.
 * It needs to be a singleton object.
 * NOTE: it can't use the DynamicLogger, but instead will use the 'real' Java logger.
 * @author beppe
 *
 */
public class GlobalLogger extends ComponentBase 
			implements XServiceInfo,
			XOX_Logger {

	// the name of the class implementing this object
	public static final String			m_sImplementationName	= GlobalLogger.class.getName();

	// the Object name, used to instantiate it inside the OOo APIs
	public static final String[]		m_sServiceNames			= { GlobConstant.m_sSINGLETON_LOGGER_SERVICE };

/// This instead is the global logger, instantiated to have a Java singleton available	
	protected static ConsoleHandler		m_aConsoleHandl;
	protected static LocalLogFormatter 	m_aLogFormatter;
	protected static FileHandler		m_aLogFileHandl;
	protected static LocalLogFormatter 	m_aLogFileFormatter;

	//logger configuration
	protected int m_nLogLevel;

	protected static String		m_sName;
	protected static boolean	m_bEnableInfoLevel = true;
	protected static boolean	m_bEnableDebugLogging = true;
	protected static boolean	m_bEnableConsoleOutput = false;
	protected static boolean	m_bEnableFileOutput = true;
	protected static String		m_sLogFilePath = "";
	protected static int		m_nFileRotationCount = 1;
	protected static int		m_nMaxFileSize = 200000;
	protected	boolean			m_bCanLogMyself;

//only used as a synchronizing object
	private static Boolean 				m_bLogConfigChanged = new Boolean(false);

// the 'real' global logger
	private static	Logger				m_aLogger;
	private static	boolean				m_bEnableLogging = true;

    private LoggerParametersAccess m_aLoggerConfigAccess;

	/**
	 * 
	 * 
	 * @param _ctx
	 */
	public GlobalLogger(XComponentContext _ctx) {
		//read the logger configuration locally
		//get configuration access, using standard registry functions
		m_aLoggerConfigAccess = new LoggerParametersAccess(_ctx);

		m_sName = GlobConstant.m_sEXTENSION_IDENTIFIER;
		m_aLogger = Logger.getLogger(m_sName);
		m_aLogger.setUseParentHandlers(false);//disables the console output of the root logger

		getLoggingConfiguration();
		configureLogger();

		if(m_bCanLogMyself)
			m_aLogger.config("ctor"); //this correspond to the application DEBUG level
	}

	/* (non-Javadoc)
	 * @see com.sun.star.lang.XServiceInfo#getImplementationName()
	 */
	@Override
	public String getImplementationName() {
		return m_sImplementationName;
	}

	/* (non-Javadoc)
	 * @see com.sun.star.lang.XServiceInfo#getSupportedServiceNames()
	 */
	@Override
	public String[] getSupportedServiceNames() {
		if(m_bCanLogMyself)
			m_aLogger.fine("getSupportedServiceNames");
		return m_sServiceNames;
	}

	/* (non-Javadoc)
	 * @see com.sun.star.lang.XServiceInfo#supportsService(java.lang.String)
	 */
	@Override
	public boolean supportsService(String _sService) {
		int len = m_sServiceNames.length;

		for (int i = 0; i < len; i++) {
			if (_sService.equals( m_sServiceNames[i] ))
				return true;
		}
		return false;
	}

// protected logger functions
	
	/**
	 * read logging configuration from registry and set internal variables
	 */
	protected void getLoggingConfiguration() {
		m_bEnableInfoLevel = m_aLoggerConfigAccess.getBoolean(GlobConstant.m_sENABLE_INFO_LEVEL);
		m_bEnableDebugLogging = m_aLoggerConfigAccess.getBoolean(GlobConstant.m_sENABLE_DEBUG_LOGGING);
		m_bEnableConsoleOutput = m_aLoggerConfigAccess.getBoolean(GlobConstant.m_sENABLE_CONSOLE_OUTPUT);
		m_bEnableFileOutput = m_aLoggerConfigAccess.getBoolean(GlobConstant.m_sENABLE_FILE_OUTPUT);
		m_sLogFilePath = m_aLoggerConfigAccess.getText(GlobConstant.m_sLOG_FILE_PATH);
		m_nFileRotationCount = m_aLoggerConfigAccess.getNumber(GlobConstant.m_sFILE_ROTATION_COUNT);
		m_nMaxFileSize = m_aLoggerConfigAccess.getNumber(GlobConstant.m_sMAX_FILE_SIZE);
	}

	protected void configureLogger() {
		//set the general level
		m_aLogger.setLevel(Level.FINEST);

		if(m_bEnableConsoleOutput) {
			m_aConsoleHandl = new ConsoleHandler();
			m_aConsoleHandl.setLevel(Level.FINEST);
			m_aLogFormatter = new LocalLogFormatter();
			m_aConsoleHandl.setFormatter(m_aLogFormatter);		
			m_aLogger.addHandler(m_aConsoleHandl);
			System.out.println("console logging enabled");
		}
//DEBUG		else	System.out.println("console logging NOT enabled");
		
		if(m_bEnableFileOutput) {
			String sFileName = GlobConstant.m_sEXTENSION_IDENTIFIER+".log";
			try {
				if(m_sLogFilePath.length() > 0) {
// e.g.: get the path separator, then scan the file path and change from whatever value to '/'
					String	aFileSeparator = System.getProperty("file.separator");
					//now, copy in a new string the sored path, changing the file separator char to '/'
					String aNewPath = "";							
					for(int i = 0; i < m_sLogFilePath.length(); i++)
						if(m_sLogFilePath.charAt(i)	== aFileSeparator.charAt(0))
							aNewPath = aNewPath +"/";
						else
							aNewPath = aNewPath + m_sLogFilePath.charAt(i);

					sFileName = aNewPath+"/"+sFileName;
				}
				else
					sFileName = "%h/"+sFileName;
				m_aLogFileHandl = new FileHandler( sFileName,m_nMaxFileSize,m_nFileRotationCount);
				m_aLogFileHandl.setLevel(Level.FINEST);
				m_aLogFileFormatter = new LocalLogFormatter();
				m_aLogFileHandl.setFormatter(m_aLogFileFormatter);
				m_aLogger.addHandler(m_aLogFileHandl);
//FIXME DEBUG				System.out.println("files logging enabled, path "+" "+sFileName+" size: "+m_nMaxFileSize+" count: "+m_nFileRotationCount);
			} catch (SecurityException e) {
				//FIXME it seems the formatter does act accordingly
				e.printStackTrace();
				System.out.println("file logging NOT enabled ");
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("File logging NOT enabled: problem with formatter or file access.");
				System.out.println("Trying fallback to user home directory..");
//FIXME: add a fall back to %HOME directory
				try {

					sFileName = "%h/"+GlobConstant.m_sEXTENSION_IDENTIFIER+".log";
					m_aLogFileHandl = new FileHandler( sFileName,m_nMaxFileSize,m_nFileRotationCount);
					m_aLogFileHandl.setLevel(Level.FINEST);
					m_aLogFileFormatter = new LocalLogFormatter();
					m_aLogFileHandl.setFormatter(m_aLogFileFormatter);
					m_aLogger.addHandler(m_aLogFileHandl);
					System.out.println("Fallback to user home directory succeeded.");					
				} catch (SecurityException e1) {
					e1.printStackTrace();
					System.out.println("file logging NOT enabled ");
				} catch (IOException e1) {
					e1.printStackTrace();
					System.out.println("File logging NOT enabled: problem with formatter or file access.");
					System.out.println("Fallback to user home directory failed as well !");
				}				
			}
		}
/*FIXME DEBUG		else System.out.println("file logging NOT enabled ");*/
			
		if(!m_bEnableConsoleOutput && !m_bEnableFileOutput)
			m_bEnableLogging = false;

		m_bCanLogMyself =  m_bEnableLogging && m_bEnableInfoLevel;
		//set all levels, the levels are filtered by this class.
	}

	/* (non-Javadoc)
	 * @see com.sun.star.logging.XOX_Logger#getLevel()
	 */
	@Override
	public int getLevel() {
		return m_nLogLevel;
	}

	/* (non-Javadoc)
	 * @see com.sun.star.logging.XOX_Logger#getName()
	 */
	@Override
	public String getName() {
		return m_sName;
	}

	/* (non-Javadoc)
	 * @see com.sun.star.logging.XOX_Logger#logp(int, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void logp(int _nLevel, String arg1, String arg2, String arg3) {
			synchronized (m_bLogConfigChanged) {			
				if(m_bEnableLogging)		
					switch (_nLevel) {
					default:
						m_aLogger.logp(Level.FINE, arg1, arg2, arg3);
						break;
					case GlobConstant.m_nLOG_CONFIG:
						m_aLogger.logp(Level.CONFIG, arg1, arg2, arg3);						
						break;
					case GlobConstant.m_nLOG_ALWAYS:
						if(m_bEnableInfoLevel)
							m_aLogger.logp(Level.FINER, arg1, arg2, arg3);						
						break;
					case GlobConstant.m_nLOG_LEVEL_INFO:
						if(m_bEnableInfoLevel)
							m_aLogger.logp(Level.INFO, arg1, arg2, arg3);						
						break;
					case GlobConstant.m_nLOG_LEVEL_DEBUG:
						if(m_bEnableDebugLogging)
							m_aLogger.logp(Level.FINE, arg1, arg2, arg3);						
						break;
					case GlobConstant.m_nLOG_LEVEL_SEVERE:
						m_aLogger.logp(Level.SEVERE, arg1, arg2, arg3);						
						break;			
					case GlobConstant.m_nLOG_LEVEL_WARNING:
//FIXME: for the time being a warning is always logged
//						if(m_bEnableDebugLogging)
							m_aLogger.logp(Level.WARNING, arg1, arg2, arg3);						
						break;
					}
			}
	}

	/* 
	 *   
	 * (non-Javadoc)
	 * @see com.sun.star.logging.XOX_Logger#setLevel(int)
	 */
	@Override
	public void setLevel(int _nNewVal) {
		synchronized (m_bLogConfigChanged) {
//			m_nLogLevel = _nNewVal;
		}
	}

	/* (non-Javadoc)
	 * @see com.yacme.ext.oxsit.logging.XOX_Logger#getEnableConsoleOutput()
	 */
	@Override
	public boolean getEnableConsoleOutput() {
		return m_bEnableConsoleOutput;
	}

	/* (non-Javadoc)
	 * @see com.yacme.ext.oxsit.logging.XOX_Logger#getEnableFileOutput()
	 */
	@Override
	public boolean getEnableFileOutput() {
		return m_bEnableFileOutput;
	}

	/* (non-Javadoc)
	 * @see com.yacme.ext.oxsit.logging.XOX_Logger#getEnableInfoLevel()
	 */
	@Override
	public boolean getEnableInfoLevel() {
		return m_bEnableInfoLevel;
	}

	/* (non-Javadoc)
	 * @see com.yacme.ext.oxsit.logging.XOX_Logger#getEnableLogging()
	 */
	@Override
	public boolean getEnableLogging() {
		return m_bEnableLogging;
	}

	/* (non-Javadoc)
	 * @see com.yacme.ext.oxsit.logging.XOX_Logger#getEnableWarningLevel()
	 */
	@Override
	public boolean getEnableDebugLogging() {
		return m_bEnableDebugLogging;
	}

	/* (non-Javadoc)
	 * @see com.yacme.ext.oxsit.logging.XOX_Logger#localConfigurationChanged()
	 */
	@Override
	public void localConfigurationChanged() {
	}

	/* (non-Javadoc)
	 * @see com.yacme.ext.oxsit.logging.XOX_Logger#optionsConfigurationChanged()
	 */
	@Override
	public void optionsConfigurationChanged() {
		synchronized (m_bLogConfigChanged) {
			m_aLogger.config("setLevel (change config) called");
			Level aLev = m_aLogger.getLevel();
			// protected area to change base elements of configuration			
			getLoggingConfiguration();
// restart logger, what is possible to restart, that is...		
//			configureLogger();
		}		
	}

	/* (non-Javadoc)
	 * @see com.yacme.ext.oxsit.logging.XOX_Logger#setEnableConsoleOutput(boolean)
	 */
	@Override
	public void setEnableConsoleOutput(boolean _bNewVal) {
		synchronized (m_bLogConfigChanged) {
			m_bEnableConsoleOutput = _bNewVal;
		}
	}

	/* (non-Javadoc)
	 * @see com.yacme.ext.oxsit.logging.XOX_Logger#setEnableFileOutput(boolean)
	 */
	@Override
	public void setEnableFileOutput(boolean _bNewVal) {
		synchronized (m_bLogConfigChanged) {
			m_bEnableFileOutput = _bNewVal;
		}
	}

	/* (non-Javadoc)
	 * @see com.yacme.ext.oxsit.logging.XOX_Logger#setEnableInfoLevel(boolean)
	 */
	@Override
	public void setEnableInfoLevel(boolean _bNewVal) {
		synchronized (m_bLogConfigChanged) {
			m_bEnableInfoLevel = _bNewVal;
		}
	}

	/* (non-Javadoc)
	 * @see com.yacme.ext.oxsit.logging.XOX_Logger#setEnableLogging(boolean)
	 */
	@Override
	public void setEnableLogging(boolean _bNewVal) {
		synchronized (m_bLogConfigChanged) {
			m_bEnableLogging = _bNewVal;
		}
	}

	/* (non-Javadoc)
	 * It's called from configuration when the logging
	 * levels in the configuration change: the new level
	 * will be taken into account immediately.
	 * as well as the file name:
	 * close the current handler,
	 *  TODO
	 *  May be we can use a changelistener object on the
	 *  configuration parameters.
	 *  
	 *  The event is fired when the parameters change.
	 * @see com.yacme.ext.oxsit.logging.XOX_Logger#setEnableWarningLevel(boolean)
	 */
	@Override
	public void setEnableDebugLogging(boolean _bNewVal) {
		synchronized(m_bLogConfigChanged) {
			m_bEnableDebugLogging = _bNewVal;
		}
	}

	/* (non-Javadoc)
	 * @see com.yacme.ext.oxsit.logging.XOX_Logger#stopLogging()
	 */
	@Override
	public void stopLogging() {
		synchronized(m_bLogConfigChanged) {
			setEnableLogging(false);
		}

		synchronized(m_bLogConfigChanged) {
			if(m_bEnableFileOutput && m_aLogFileHandl != null) {
				m_aLogFileHandl.close();
				setEnableFileOutput(false);
				m_aLogFileHandl = null;
			}
		}		
	}

	/* (non-Javadoc)
	 * @see com.sun.star.lang.XComponent#dispose()
	 */
	@Override
	public void dispose() {
		if(m_bCanLogMyself)
			m_aLogger.entering("disposing of global logger", "");
		stopLogging();
		super.dispose();
	}
}
