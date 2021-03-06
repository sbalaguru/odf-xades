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
 * The Original Code is /oxsit-custom_it/src/com/yacme/ext/oxsit/cust_it/comp/security/ca/CertificationPathCache_IT.java.
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

package com.yacme.ext.oxsit.cust_it.comp.security.ca;

import com.yacme.ext.oxsit.security.cert.CertificateElementID;
import com.yacme.ext.oxsit.security.cert.CertificateElementState;
import com.yacme.ext.oxsit.security.cert.CertificateState;
import com.yacme.ext.oxsit.security.cert.CertificateStateConditions;
import com.yacme.ext.oxsit.security.cert.CertificationAuthorityState;
import com.yacme.ext.oxsit.security.cert.XOX_CertificateRevocationStateProcedure;
import com.yacme.ext.oxsit.security.cert.XOX_CertificationPathProcedure;
import com.yacme.ext.oxsit.security.cert.XOX_X509Certificate;
import com.yacme.ext.oxsit.security.cert.XOX_X509CertificateDisplay;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.TreeMap;

import org.bouncycastle.cms.CMSException;

import com.sun.star.frame.XFrame;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XEventListener;
import com.sun.star.lang.XInitialization;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lib.uno.helper.ComponentBase;
import com.sun.star.task.XStatusIndicator;
import com.sun.star.task.XStatusIndicatorFactory;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.yacme.ext.oxsit.Helpers;
import com.yacme.ext.oxsit.cust_it.ConstantCustomIT;
import com.yacme.ext.oxsit.cust_it.security.crl.CertificationAuthorities;
import com.yacme.ext.oxsit.cust_it.security.crl.RootsVerifier;
import com.yacme.ext.oxsit.cust_it.security.crl.X509CertRL;
import com.yacme.ext.oxsit.logging.DynamicLogger;
import com.yacme.ext.oxsit.logging.IDynamicLogger;
import com.yacme.ext.oxsit.ooo.GlobConstant;

/**
 *  This service implements the CertificationPathCache_IT service, used to check the
 *  certificate for compliance on Italian law.
 *  
 *  This is basically a memory cache that remains active for the entire OOo session.
 *  It holds the CA root list, and the dowloaded CRLs.
 *  
 *  This object is made persistent through the use of the singleton service
 *  com.yacme.ext.oxsit.singleton.SingleGlobalVariables
 *  It is saved and can be retrieved through a name
 *  
 *  
 * @author beppec56
 *
 */
public class CertificationPathCache_IT extends ComponentBase //help class, implements XTypeProvider, XInterface, XWeak
		implements XServiceInfo, XInitialization, XOX_CertificationPathProcedure, XOX_CertificateRevocationStateProcedure {

	// the name of the class implementing this object
	public static final String m_sImplementationName = CertificationPathCache_IT.class.getName();

	// the Object name, used to instantiate it inside the OOo API
	public static final String[] m_sServiceNames = { ConstantCustomIT.m_sCERTIFICATION_PATH_CACHE_SERVICE_IT };

	private XComponentContext m_xCC;
	private XMultiComponentFactory m_xMCF;
	protected IDynamicLogger m_aLogger;

	protected XOX_X509Certificate m_xQc;

	private CertificateState m_aCertificateState;
	private CertificateStateConditions m_aCertificateStateConditions;
	private CertificationAuthorityState m_aCAState;

	private RootsVerifier m_aRootVerifier;

	private CertificationAuthorities m_aCADbData;

	private String m_bUseGUI;

	private X509CertRL CRL;

	private XComponent[] m_aCAList;

	/**
	 * 
	 * 
	 * @param _ctx
	 */
	public CertificationPathCache_IT(XComponentContext _ctx) {
		m_xCC = _ctx;
		m_xMCF = m_xCC.getServiceManager();
		m_aLogger = new DynamicLogger(this, _ctx);
		//		m_aLogger = new DynamicLazyLogger();
//		m_aLogger.enableLogging();
		m_aLogger.ctor();
		m_aCertificateState = CertificateState.NOT_YET_VERIFIED;
		m_aCertificateStateConditions = CertificateStateConditions.REVOCATION_NOT_YET_CONTROLLED;
		m_aCAState = CertificationAuthorityState.NOT_YET_CHECKED;
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

		m_aLogger.info("supportsService", _sService);
		for (int i = 0; i < len; i++) {
			if (_sService.equals(m_sServiceNames[i]))
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
	 * @see com.yacme.ext.oxsit.security.cert.XOX_CertificationPathProcedure#configureOptions(com.sun.star.frame.XFrame, com.sun.star.uno.XComponentContext)
	 */
	@Override
	public void configureOptions(XFrame arg0, XComponentContext arg1) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.yacme.ext.oxsit.security.cert.XOX_CertificationPathProcedure#getCertificationAuthorityState()
	 */
	@Override
	public CertificationAuthorityState getCertificationAuthorityState() {
		return m_aCAState;
	}

	/* (non-Javadoc)
	 * @see com.yacme.ext.oxsit.security.cert.XOX_CertificationPathProcedure#verifyCertificationPath(com.sun.star.lang.XComponent)
	 */
	@Override
	public CertificationAuthorityState verifyCertificationPath(XFrame _aFrame, Object arg0) throws IllegalArgumentException,
			Exception {
		m_aLogger.debug("verifyCertificationPath");
		//check for certificate
		m_xQc = (XOX_X509Certificate) UnoRuntime.queryInterface(XOX_X509Certificate.class, arg0);
		if (m_xQc == null)
			throw (new IllegalArgumentException(
					"XOX_CertificateComplianceControlProcedure#verifyCertificateCertificateCompliance wrong argument"));

		initializeCADataBase(_aFrame);
		checkPathValidity();
		return null;
	}

	/* (non-Javadoc)
	 * @see com.yacme.ext.oxsit.security.cert.XOX_CertificationPathProcedure#initializeProcedure(com.sun.star.frame.XFrame)
	 */
	@Override
	public void initializeProcedure(XFrame arg0) {
		initializeCADataBase(arg0);
	}

	/* (non-Javadoc)
	 * @see com.yacme.ext.oxsit.security.cert.XOX_CertificationPathProcedure#initialize(boolean)
	 */
	private void initializeCADataBase(XFrame _aFrame) {
		XStatusIndicator xStatusIndicator = null;
		if (_aFrame != null) {
			//check interface
			//
			XStatusIndicatorFactory xFact = (XStatusIndicatorFactory) UnoRuntime.queryInterface(XStatusIndicatorFactory.class,
					_aFrame);
			if (xFact != null) {
				xStatusIndicator = xFact.createStatusIndicator();
				if (xStatusIndicator != null)
					xStatusIndicator.start("", 100); //meaning 100%
			}
		}

		if (xStatusIndicator != null)
			xStatusIndicator.setText("Verify and confirm root signature");
		//here:
		//init the root authority elements,
		if (m_aRootVerifier == null)
			m_aRootVerifier = new RootsVerifier(_aFrame, m_xCC);
		X509Certificate aCert = m_aRootVerifier.getRootSignatureCert();
		/* the following needs implementation, do we need it?
		 * 		else
					if(m_aRootVerifier.getUserApprovedFingerprint() == null)
		*/
		if (xStatusIndicator != null) {
			xStatusIndicator.setText("Read CA root Data base");
			xStatusIndicator.setValue(5);
		}
		//fill the list of certificate authorities available
		//FIXME, this should be changed to a general algorithm:
		// 1) load the first file it finds into "ca-list-signed-p7m-it" in
		//    extension directory verify the file against the signature,
		//    if ok get the signing date
		// 2) check if there is a file downloaded in the "store" directory
		// if a file exists there, then get the date when the file in "store" was
		// signed and compare it with the
		// 
		if (m_aCADbData == null && aCert != null) {
			//prepare file path
			URL listURL, rootURL;
			try {
				listURL = new URL(Helpers.getExtensionInstallationPath(m_xCC) 
						+ System.getProperty("file.separator")
						+ "ca-list-signed-p7m-it" //fixed path, the directory containing the current root zip file
						+ System.getProperty("file.separator") + ConstantCustomIT.m_sCA_LIST_SIGNED_FILE);
				
				rootURL= new URL(Helpers.getExtensionInstallationPath(m_xCC)
						+ System.getProperty("file.separator") 
						+ "ca-root-digitpa-it" 
						+ System.getProperty("file.separator") + ConstantCustomIT.m_sCA_CNIPA_ROOT);
				
				m_aCADbData = new CertificationAuthorities(xStatusIndicator, m_xCC, listURL, rootURL, false //to display debug data
				);

				

				X509CertRL aCrl = new X509CertRL(_aFrame, m_xCC, m_aCADbData);
				aCrl.isNotRevokedOCSP(xStatusIndicator, aCert, new Date());
			} catch (MalformedURLException e) {
				m_aLogger.severe(e);
			} catch (GeneralSecurityException e) {
				m_aLogger.severe(e);
			} catch (IOException e) {
				m_aLogger.severe(e);
			} catch (CMSException e) {
				m_aLogger.severe(e);
			}
		}
		if (xStatusIndicator != null)
			xStatusIndicator.end();
	}

	//FIXME: another one, see behavior of this with a longer certification path
	// at the moment it doesn't function well...
	//FIXME: check with cert path problem
	private boolean checkPathValidity() {
		//convert the certificate to java internal representation
		java.security.cert.CertificateFactory cf;
		try {
			cf = java.security.cert.CertificateFactory.getInstance("X.509");
			java.io.ByteArrayInputStream bais = null;
			bais = new java.io.ByteArrayInputStream(m_xQc.getCertificateAttributes().getDEREncoded());
			X509Certificate certChild = (java.security.cert.X509Certificate) cf.generateCertificate(bais);
			XOX_X509Certificate qCertChild = m_xQc;

			m_aCAState = CertificationAuthorityState.NOT_TRUSTED;
			//now loop, and add the certificate path to the current path, actually empty            
			X509Certificate certParent = null;
			boolean isPathValid = false;
			while (!certChild.getIssuerDN().equals(certChild.getSubjectDN())) { //until CA is self signed

				try {
					certParent = m_aCADbData.getCACertificate(certChild.getIssuerX500Principal());
				} catch (GeneralSecurityException ex) {
					//set 'CA unknown to Italian PA'
					//set the current XOX_X509Certificate state as well
					//this can be an intermediate certificate, it's the last one that should be ok
					//we need to set the certificate path of the current
					//main XOX_X509Certificate as invalid for Italian signature

					//FIXME we should add the possibility to check for an alternative CA
					//in order to see if the certificate is still ok
					// in the end is the root CA of the certification path that rules
					// all
					setCertPathErrorStateHelper("The Certification Authority is NOT CNIPA trusted.\r\r"
							+ "It does NOT exist in the data base of the trusted Entities.");
					return isPathValid;
				}
				//set the main user certificate certification authority as trusted
				m_aCAState = CertificationAuthorityState.TRUSTED;
				//check the child certificate to see if it's correctly verified
				//use the public CA key
				String sNoCAPKey = "The Certification Authority Public Key cannot be used\r\r" + "to verify this certificate";
				try {
					certChild.verify(certParent.getPublicKey());
				} catch (InvalidKeyException e) {
					m_aLogger.severe(e);
					setCertPathErrorStateHelper(sNoCAPKey);
					return isPathValid;
				} catch (NoSuchAlgorithmException e) {
					m_aLogger.severe(e);
					setCertPathErrorStateHelper(sNoCAPKey);
					return isPathValid;
				} catch (NoSuchProviderException e) {
					m_aLogger.severe(e);
					// alert the user the certificate cannot be verified against the public key of
					setCertificateStateHelper(CertificateState.CA_CERTIFICATE_SIGNATURE_INVALID);
					return isPathValid;
				} catch (SignatureException e) {
					m_aLogger.severe(e);
					//finish off, the path starts ok, but the child certificate is
					//incorrectly signed, set it as malformed, add a note to explain the reason
					setCertificateStateHelper(CertificateState.CA_CERTIFICATE_SIGNATURE_INVALID);
					return isPathValid;
				}

				//still need to check if the certificate of 

				certChild = certParent;
				//instantiate a qualified certificate to represent the parent,
				certParent.getEncoded();
				//all seems right, instantiate the certificate service
				//now create the Certificate Control UNO objects
				//first the certificate compliance control
				//FIXME, may be we can change this to a better behavior, moving the tests currently carried out in
				//CertificationAuthorities here
				//Object oACCObj = m_xMCF.createInstanceWithContext(ConstantCustomIT.m_sCERTIFICATE_COMPLIANCE_SERVICE_IT, m_xCC);
				//Object oCertPath = m_xMCF.createInstanceWithContext(ConstantCustomIT.m_sCERTIFICATION_PATH_SERVICE_IT, m_xCC);

				//now the certification path control

				//prepare objects for subordinate service
				Object[] aArguments = new Object[6];
				//												byte[] aCert = cert.getEncoded();

				Object oCertDisp = m_xMCF.createInstanceWithContext(ConstantCustomIT.m_sX509_CERTIFICATE_DISPLAY_SERVICE_CA_IT,
						m_xCC);
				/*
				 * the following service exposes all the interfaces needed to check a CA certificate<.
				 * - compliance
				 * - certification path
				 * - certificate revocation 
				 */
				Object oCertCompl = m_xMCF.createInstanceWithContext(ConstantCustomIT.m_sCERTIFICATE_COMPLIANCE_SERVICE_CA_IT,
						m_xCC);
				/*				Object oCertPath = m_xMCF.createInstanceWithContext(ConstantCustomIT.m_sCERTIFICATE_PATH_SERVICE_CA_IT, m_xCC);
								Object oCertRev = m_xMCF.createInstanceWithContext(ConstantCustomIT.m_sCERTIFICATE_PATH_SERVICE_CA_IT, m_xCC);*/

				//set the certificate raw value
				aArguments[0] = certParent.getEncoded();//aCert;
				aArguments[1] = new Boolean(m_bUseGUI);//FIXME change according to UI (true) or not UI (false)
				aArguments[2] = oCertDisp; //the display object
				aArguments[3] = oCertCompl; //the checker object, in this case implements all
				aArguments[4] = oCertCompl; //the checker object, in this case implements all
				aArguments[5] = oCertCompl; //the checker object, in this case implements all

				Object oACertificate = m_xMCF.createInstanceWithArgumentsAndContext(GlobConstant.m_sX509_CERTIFICATE_SERVICE,
						aArguments, m_xCC);
				//get the main interface
				XOX_X509Certificate xQualCert = (XOX_X509Certificate) UnoRuntime.queryInterface(XOX_X509Certificate.class,
						oACertificate);

				//FIXME we need to do what the RFC 3280 explains in chapter 6 
				xQualCert.verifyCertificateCompliance(null);
				xQualCert.verifyCertificationPath(null);
				//we don't check the revocation state of the CA, if the CA is in the CA root, then it's supposed to be valid and not revoked
				// now get the current CA state, get it from the child process
				//FIXME need to check for broken certification Path
				CertificationAuthorityState aCAState = CertificationAuthorityState.fromInt(xQualCert
						.getCertificationAuthorityState());
				/*				if (aCAState == CertificationAuthorityState.CANNOT_BE_CHECKED)
									;*/
				m_aCAState = aCAState;
				//set it to the current child certificate, and put it as a new qualified certificate
				//set the status flags of the new certificate as correct for a CA certificate
				qCertChild.setCertificationPath(xQualCert);
				qCertChild = xQualCert;
				m_aLogger.debug("added a certificate");
			}
			//FIXME arrive here if the current child certificate is sels signed
			// need to see if the certificate is not revoked (or something similar)

			;
			return isPathValid;
		} catch (CertificateException e) {
			m_aLogger.severe(e);
		} catch (Exception e) {
			m_aLogger.severe(e);
		}
		return false;
	}

	private void setCertPathErrorStateHelper(String string) {
		m_xQc.setCertificateElementErrorState(GlobConstant.m_sX509_CERTIFICATE_CERTPATH, CertificateElementState.INVALID_value);
		m_xQc.getCertificateDisplayObj().setCertificateElementCommentString(CertificateElementID.CERTIFICATION_PATH, string);
		setCertificateStateHelper(CertificateState.CA_CERTIFICATE_SIGNATURE_INVALID);
	}

	/* (non-Javadoc)
	 * @see com.yacme.ext.oxsit.security.cert.XOX_CertificateRevocationStateProcedure#getCertificateState()
	 * @see com.yacme.ext.oxsit.security.cert.XOX_CertificationPathProcedure#getCertificateState()
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

	////////////////////// verify revocation state functions
	/* (non-Javadoc)
	 * @see com.yacme.ext.oxsit.security.cert.XOX_CertificateRevocationStateProcedure#verifyCertificateRevocationState(com.sun.star.frame.XFrame, com.sun.star.lang.XComponent)
	 */
	@Override
	public CertificateState verifyCertificateRevocationState(XFrame _aFrame, Object arg1) throws IllegalArgumentException,
			Exception {
		
//		m_aLogger.info("verifyCertificateRevocationState", "checking Internet...");
//		if(!Helpers.isInternetReachable()) {
//			m_aLogger.severe("verifyCertificateRevocationState", "NO Internet !");
//		}
//		else
//			m_aLogger.info("verifyCertificateRevocationState", "Internet OK");

		m_aLogger.debug("verifyCertificateRevocationState");
		m_aCertificateState = CertificateState.NOT_YET_VERIFIED;
		m_aCertificateStateConditions = CertificateStateConditions.REVOCATION_NOT_YET_CONTROLLED;

		//check for certificate
		m_xQc = (XOX_X509Certificate) UnoRuntime.queryInterface(XOX_X509Certificate.class, arg1);
		if (m_xQc == null)
			throw (new IllegalArgumentException(
					"XOX_CertificateRevocationStateProcedure#verifyCertificateRevocationState wrong argument"));

		initializeCADataBase(_aFrame);

		if (CRL == null) {
			CRL = new X509CertRL(_aFrame, m_xCC, m_aCADbData);
		}
		//now check the revocation state using the crl
		//		m_aCertificateState = CertificateState.OK;
		//convert the certificate to java internal representation
		java.security.cert.X509Certificate m_JavaCert = null;

		java.security.cert.CertificateFactory cf;
		try {
			cf = java.security.cert.CertificateFactory.getInstance("X.509");
			java.io.ByteArrayInputStream bais = null;
			bais = new java.io.ByteArrayInputStream(m_xQc.getCertificateAttributes().getDEREncoded());
			m_JavaCert = (java.security.cert.X509Certificate) cf.generateCertificate(bais);
			XStatusIndicator xStatusIndicator = null;
			if (_aFrame != null) {
				//check interface
				//
				XStatusIndicatorFactory xFact = (XStatusIndicatorFactory) UnoRuntime.queryInterface(XStatusIndicatorFactory.class,
						_aFrame);
				if (xFact != null) {
					xStatusIndicator = xFact.createStatusIndicator();
					if (xStatusIndicator != null)
						xStatusIndicator.start("Control CRL", 100);
				}
			}

			//            CRL.isNotRevokedCRL(xStatusIndicator,m_JavaCert);
			CRL.isNotRevokedOCSP(xStatusIndicator, m_JavaCert, new Date());
			xStatusIndicator.end();

			//grab certificate state and conditions
			setCertificateStateHelper(CRL.getCertificateState());
			m_aCertificateStateConditions = CRL.getCertificateStateConditions();
		} catch (CertificateException e) {
			m_aLogger.severe(e);
			setCertificateStateHelper(CertificateState.MALFORMED_CERTIFICATE);
			throw (new com.sun.star.uno.Exception(" wrapped exception: "));
		}
		return m_aCertificateState;
	}

	/**
	 * check for priority of certificate state and set it accordingly
	 * @param _newState
	 */
	private void setCertificateStateHelper(CertificateState _newState) {
		if (_newState.getValue() > m_aCertificateState.getValue())
			m_aCertificateState = _newState;
	}

	/* (non-Javadoc)
	 * @see com.yacme.ext.oxsit.security.cert.XOX_CertificationPathProcedure#getCertificationAutorities()
	 */
	@Override
	public int getCertificationAuthoritiesNumber() {
		if (m_aCADbData != null)
			return m_aCADbData.getCANumber();
		return -1;
	}

	/* (non-Javadoc)
	 * @see com.yacme.ext.oxsit.security.cert.XOX_CertificationPathProcedure#getCertificationAuthorities()
	 */
	@Override
	public XComponent[] getCertificationAuthorities(XFrame _aFrame) {
		initializeCADataBase(_aFrame);
		if (m_aCADbData != null) {
			if (m_aCAList != null)
				return m_aCAList;

			int numCA = m_aCADbData.getCANumber();
			if (numCA > 0) {
				m_aCAList = new XComponent[numCA];

				TreeMap<String, XComponent> aTreeMap = new TreeMap<String, XComponent>();

				Collection<X509Certificate> c = m_aCADbData.getCA();
				Iterator<X509Certificate> i = c.iterator();
				int idx = 0;

				//get the certificates and put them in a collection
				while (i.hasNext()) {
					X509Certificate cert = i.next();
					//prepare objects for subordinate service
					//				byte[] aCert = cert.getEncoded();
					//set the certificate raw value
					try {
						Object[] aArguments = new Object[4];
						//this are supposed to be CA, so we'll add the 
						//ca test & display functions known by this component
						//note that this component is localized, no need for it to be adapted
						Object oCertDisp = m_xMCF.createInstanceWithContext(
								ConstantCustomIT.m_sX509_CERTIFICATE_DISPLAY_SERVICE_CA_IT, m_xCC);
						Object oCertCompl = m_xMCF.createInstanceWithContext(
								ConstantCustomIT.m_sCERTIFICATE_COMPLIANCE_SERVICE_CA_IT, m_xCC);
						aArguments[0] = cert.getEncoded();
						aArguments[1] = new Boolean(false);//FIXME change according to UI (true) or not UI (false)
						aArguments[2] = oCertDisp; //the certificate display
						aArguments[3] = oCertCompl; //the compliance

						Object oACertificate = m_xMCF.createInstanceWithArgumentsAndContext(
								GlobConstant.m_sX509_CERTIFICATE_SERVICE, aArguments, m_xCC);
						//get the main interface
						XOX_X509CertificateDisplay xQualCert = (XOX_X509CertificateDisplay) UnoRuntime.queryInterface(
								XOX_X509CertificateDisplay.class, oACertificate);
						if (xQualCert != null) {
							//add the certificate to the sorted collection, using the name as a key (may be duplicated)
							XComponent com = (XComponent) UnoRuntime.queryInterface(XComponent.class, xQualCert);
							//sort the collection while constructing the tree
							XComponent xCo = aTreeMap.put(xQualCert.getSubjectDisplayName() + xQualCert.getSubjectName(), com);
							if (xCo != null) {
								m_aLogger.debug("duplicated CA: " + xQualCert.getSubjectDisplayName() + " - "
										+ xQualCert.getSubjectName());
							}
						}
					} catch (CertificateEncodingException e) {
						m_aLogger.severe(e);
					} catch (Exception e) {
						m_aLogger.severe(e);
					}
				}
				//now compose the array and exit
				idx = 0;
				Collection<XComponent> aComp = aTreeMap.values();
				Iterator<XComponent> ic = aComp.iterator();
				while (ic.hasNext()) {
					m_aCAList[idx++] = ic.next();
				}
				return m_aCAList;
			}
		}
		return null;
	}

}
