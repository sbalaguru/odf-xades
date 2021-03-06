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
 * The Original Code is /oxsit-custom_it/src/com/yacme/ext/oxsit/cust_it/comp/security/cert/CertificateRevocation_IT.java.
 *
 * The Initial Developers of the Original Code are
 * Giuseppe Castagno giuseppe.castagno@acca-esse.it
 * Roberto Resoli - Servizio Sistema Informativo - Comune di Trento.
 * 
 * Portions created by the Initial Developer are Copyright (C) 2009-2011
 * the Initial Developer. All Rights Reserved.
 * Part of the code is adapted from j4sign, hence part of
 * the copyright is:
 * j4sign - an open, multi-platform digital signature solution
 * Copyright (c) 2004 Roberto Resoli - Servizio Sistema Informativo - Comune di Trento.
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

package com.yacme.ext.oxsit.cust_it.comp.security.cert;

import com.yacme.ext.oxsit.XOX_SingletonDataAccess;
import com.yacme.ext.oxsit.security.cert.CertificateState;
import com.yacme.ext.oxsit.security.cert.CertificateStateConditions;
import com.yacme.ext.oxsit.security.cert.XOX_CertificateRevocationStateProcedure;
import com.yacme.ext.oxsit.security.cert.XOX_X509Certificate;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.frame.XFrame;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.NoSuchMethodException;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XEventListener;
import com.sun.star.lang.XInitialization;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lib.uno.helper.ComponentBase;
import com.sun.star.ucb.ServiceNotFoundException;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.yacme.ext.oxsit.Helpers;
import com.yacme.ext.oxsit.cust_it.ConstantCustomIT;
import com.yacme.ext.oxsit.logging.DynamicLogger;
import com.yacme.ext.oxsit.logging.IDynamicLogger;

/**
 *  This service implements the CertificateRevocation_IT service, used to check the
 *  certificate for its revocation state.
 *  
 * @author beppec56
 *
 */
public class CertificateRevocation_IT extends ComponentBase //help class, implements XTypeProvider, XInterface, XWeak
			implements 
			XServiceInfo,
			XInitialization,
			XOX_CertificateRevocationStateProcedure
			 {

	// the name of the class implementing this object
	public static final String			m_sImplementationName	= CertificateRevocation_IT.class.getName();

	// the Object name, used to instantiate it inside the OOo API
	public static final String[]		m_sServiceNames			= { ConstantCustomIT.m_sCERTIFICATE_REVOCATION_SERVICE_IT };

	protected IDynamicLogger m_aLogger;

	protected XOX_X509Certificate m_xQc;

	private XComponentContext m_xCC;

	private XMultiComponentFactory m_xMCF;

	private XOX_CertificateRevocationStateProcedure m_axoxChildProc;

	private CertificateState m_aCertificateState;
	private CertificateStateConditions	m_aCertificateStateConditions;
	
	/**
	 * 
	 * 
	 * @param _ctx
	 */
	public CertificateRevocation_IT(XComponentContext _ctx) {
		m_xCC = _ctx;
		m_xMCF = m_xCC.getServiceManager();
		m_aLogger = new DynamicLogger(this, _ctx);
//		m_aLogger = new DynamicLazyLogger();
//		m_aLogger.enableLogging();
    	m_aLogger.ctor();    	
	}

	@Override
	public String getImplementationName() {
		m_aLogger.entering("getImplementationName");
		return m_sImplementationName;
	}

	/* (non-Javadoc)
	 * @see com.sun.star.lang.XServiceInfo#getSupportedServiceNames()
	 */
	@Override
	public String[] getSupportedServiceNames() {
		m_aLogger.info("getSupportedServiceNames");
		return m_sServiceNames;
	}

	/* (non-Javadoc)
	 * @see com.sun.star.lang.XServiceInfo#supportsService(java.lang.String)
	 */
	@Override
	public boolean supportsService(String _sService) {
		int len = m_sServiceNames.length;

		m_aLogger.info("supportsService",_sService);
		for (int i = 0; i < len; i++) {
			if (_sService.equals( m_sServiceNames[i] ))
				return true;
		}
		return false;
	}

	/** Function used internally when instantiating the object
	 * 
	 * Called using:
	 *
	 *		Object[] aArguments = new Object[4];
	 *
	 *		aArguments[0] = new String(OID);
	 *
	 *		aArguments[1] = new String("TheName"));
	 *		aArguments[2] = new String("The Value");
	 *		aArguments[3] = new Boolean(true or false);
	 * 
	 * Object aExt = m_xMCF.createInstanceWithArgumentsAndContext(
	 *				"com.yacme.ext.oxsit.security.cert.CertificateExtension", aArguments, m_xContext);
	 *
	 * @param _eValue array of ? object:
	 * <p>_eValue[0] string ??</p>
	 * 
	 * (non-Javadoc)
	 * @see com.sun.star.lang.XInitialization#initialize(java.lang.Object[])
	 *  
	 */
	@Override
	public void initialize(Object[] _eValue) throws Exception {
		//the eValue is the byte stream of the extension
	}

	/* (non-Javadoc)
	 * @see com.sun.star.lang.XComponent#addEventListener(com.sun.star.lang.XEventListener)
	 */
	@Override
	public void addEventListener(XEventListener arg0) {
		super.addEventListener(arg0);
	}

	/* (non-Javadoc)
	 * @see com.sun.star.lang.XComponent#dispose()
	 */
	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		m_aLogger.entering("dispose");
//		super.dispose();
	}

	/* (non-Javadoc)
	 * @see com.sun.star.lang.XComponent#removeEventListener(com.sun.star.lang.XEventListener)
	 */
	@Override
	public void removeEventListener(XEventListener arg0) {
		super.removeEventListener(arg0);
	}

	/* (non-Javadoc)
	 * @see com.yacme.ext.oxsit.security.cert.XOX_CertificateRevocationStateProcedure#getCertificateState()
	 */
	@Override
	public CertificateState getCertificateState() {
		return m_aCertificateState;
	}

	/* (non-Javadoc)
	 * @see com.yacme.ext.oxsit.security.cert.XOX_CertificateRevocationStateProcedure#getCertificateStateConditions()
	 */
	@Override
	public CertificateStateConditions getCertificateStateConditions() {
		return m_aCertificateStateConditions;
	}

	/* (non-Javadoc)
	 * @see com.yacme.ext.oxsit.security.cert.XOX_CertificateRevocationStateProcedure#initializeProcedure(com.sun.star.frame.XFrame, com.sun.star.uno.XComponentContext)
	 */
	@Override
	public void initializeProcedure(XFrame arg0) {
		
	}

	/* (non-Javadoc)
	 * @see com.yacme.ext.oxsit.security.cert.XOX_CertificateRevocationStateProcedure#configureOptions(com.sun.star.frame.XFrame, com.sun.star.uno.XComponentContext)
	 */
	@Override
	public void configureOptions(XFrame arg0, XComponentContext arg1) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.yacme.ext.oxsit.security.cert.XOX_CertificateRevocationStateProcedure#verifyCertificateCertificateCompliance(com.sun.star.lang.XComponent)
	 * 
	 * currently only the crl is controlled
	 */
	@Override
	public CertificateState verifyCertificateRevocationState(XFrame _xFrame,
			Object arg0) throws IllegalArgumentException, Exception {
		m_xQc = (XOX_X509Certificate)UnoRuntime.queryInterface(XOX_X509Certificate.class, arg0);
		if(m_xQc == null)
			throw (new IllegalArgumentException("XOX_CertificateRevocationStateProcedure#verifyCertificateRevocationState wrong argument"));

		// FIXME check if revocation control is enabled or not
		m_aLogger.log("verifyCertificateRevocationState");
		try {
			m_axoxChildProc = null;
			XOX_SingletonDataAccess xSingletonDataAccess = Helpers.getSingletonDataAccess(m_xCC);

			try {
				XComponent xComp = xSingletonDataAccess.getUNOComponent(ConstantCustomIT.m_sCERTIFICATION_PATH_CACHE_SERVICE_IT);					
				//yes, grab it and set our component internally
				m_aLogger.info("Cache found!");
				m_axoxChildProc = 
					(XOX_CertificateRevocationStateProcedure)
					UnoRuntime.queryInterface(
							XOX_CertificateRevocationStateProcedure.class, xComp);
			} catch (NoSuchElementException ex ) {
				//no, instantiate it and add to the singleton 
				m_aLogger.info("Cache NOT found!");
				//create the object
				Object oCertPath = m_xMCF.createInstanceWithContext(ConstantCustomIT.m_sCERTIFICATION_PATH_CACHE_SERVICE_IT, m_xCC);
				//add it the singleton
				//now use it
				XComponent xComp = (XComponent)UnoRuntime.queryInterface(XComponent.class, oCertPath); 
				if(xComp != null) {
					xSingletonDataAccess.addUNOComponent(ConstantCustomIT.m_sCERTIFICATION_PATH_CACHE_SERVICE_IT, xComp);
					m_axoxChildProc = (XOX_CertificateRevocationStateProcedure)
								UnoRuntime.queryInterface(XOX_CertificateRevocationStateProcedure.class, xComp);
				}
				else
					throw (new IllegalArgumentException());
			} catch (IllegalArgumentException ex ) {
				m_aLogger.severe(ex);
			} catch (Throwable ex ) { 
				m_aLogger.severe(ex);
			}

			if(m_axoxChildProc != null) {
				XComponent xComp = (XComponent)UnoRuntime.queryInterface(XComponent.class, m_xQc); 
				if(xComp != null)
					m_axoxChildProc.verifyCertificateRevocationState(_xFrame,xComp);
					m_aCertificateState = m_axoxChildProc.getCertificateState();
					m_aCertificateStateConditions = m_axoxChildProc.getCertificateStateConditions();
					return m_aCertificateState;
			}
			else
				m_aLogger.info("CANNOT execute child");
			//instantiate the cache, init it and add it
		} catch (ClassCastException ex) {
			m_aLogger.severe(ex);
		} catch (ServiceNotFoundException ex) {
			m_aLogger.severe(ex);
		} catch (NoSuchMethodException ex) {
			m_aLogger.severe(ex);
		} catch (Throwable ex ) { 
			m_aLogger.severe(ex);
		}
		return null;
	}
}
