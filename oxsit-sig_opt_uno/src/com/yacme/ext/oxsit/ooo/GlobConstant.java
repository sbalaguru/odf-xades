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

package com.yacme.ext.oxsit.ooo;

/**
 * this class contains the global variables needed by all this implemenatation
 * 
 * @author beppe
 * 
 */
public class GlobConstant {	
	/// base of configuration item
	public static final String m_sWEBIDENTBASE = "com.yacme.ext"; // extension owner, used in building it,
																// same name, same meaning in extension_conf_files/build.xml
	public static final String m_sEXT_NAME ="oxsit"; //name of the extension, used in building it,
																		// same name, same meaning in extension_conf_files/build.xml
	public static final String m_sEXTENSION_IDENTIFIER = m_sWEBIDENTBASE+"."+m_sEXT_NAME;
	public static final String m_sEXTENSION_BASE_KEY = "/"+m_sWEBIDENTBASE+"."+ m_sEXT_NAME;
	public static final String m_sEXTENSION_CONF_BASE_KEY = m_sEXTENSION_BASE_KEY+".Configuration/";
	public static final String m_sEXTENSION_CONF_FRAME_KEY = m_sEXTENSION_CONF_BASE_KEY	+ "Frames/";
	public static final String m_sEXTENSION_CONF_OPTIONS = m_sEXTENSION_CONF_BASE_KEY	+ "SignatureOptionsParameters/";
	public static final String m_sEXTENSION_CONF_SSCDS = m_sEXTENSION_CONF_BASE_KEY	+ "SSCDCollection";
	public static final String m_sEXTENSION_CONF_FRAME_ID = "Fuhc";
	public static final String m_sOFFICE_ADDONS_BASE_CONF ="/org.openoffice.Office.Addons/";
	public static final String m_sTOOLBAR_CONF_BASE_KEY = m_sOFFICE_ADDONS_BASE_CONF+"AddonUI/OfficeToolBar";
	public static final String m_sEXTENSION_TOOLBAR_CONF_BASE_KEY = m_sTOOLBAR_CONF_BASE_KEY+m_sEXTENSION_BASE_KEY+".OfficeToolBar/";
	
	/**
	 * the following constant are used in ProtocolHandler.xcu(.xml) and
	 * Addon.xcu(.xml) in the extension pay attention before modifiying !
	 */
	public static final String	m_sSIGN_PROTOCOL_BASE_URL				= m_sWEBIDENTBASE + ".oxsit.comp.SignatureHandler:";
	//object to sign for menu item
	public static final String	m_sSIGN_DIALOG_PATH						= "SignDialog";
	public static final String	m_sSIGN_DIALOG_PATH_COMPLETE			= m_sSIGN_PROTOCOL_BASE_URL+m_sSIGN_DIALOG_PATH;
	// specific object for extended toolbar
	public static final String	m_sSIGN_DIALOG_PATH_TB					= "SignDialogTB";
	public static final String	m_sSIGN_DIALOG_PATH_TB_COMPLETE			= m_sSIGN_PROTOCOL_BASE_URL+m_sSIGN_DIALOG_PATH_TB;
	//object to sign for menu item
	public static final String	m_sDISPLAY_SIGNED_FILE_PATH						= "DisplaySign";
	public static final String	m_sDISPLAY_SIGNED_FILE_PATH_COMPLETE			= m_sSIGN_PROTOCOL_BASE_URL+m_sDISPLAY_SIGNED_FILE_PATH;
//	object for help
	public static final String	m_sON_HELP_ABOUT_PATH					= "HelpAbout";
	public static final String	m_sON_HELP_ABOUT_PATH_COMPLETE			= m_sSIGN_PROTOCOL_BASE_URL+m_sON_HELP_ABOUT_PATH;

	/** end of ProtocolHandler.xcu(.xml) Addon.xcu(.xml) constants */
	// constant for UNO dispatch URL (intercepted url)
	public static final String	m_sUNO_PROTOCOL								= ".uno:";
	public static final String m_sUNO_SAVE_URL_COMPLETE 					= ".uno:Save";
	public static final String m_sUNO_SSAVE_AS_URL_COMPLETE 				= ".uno:SaveAs";
	public static final String	m_sUNO_SIGNATURE_URL_COMPLETE				= ".uno:Signature";
	public static final String	m_sUNO_MACRO_SIGNATURE_URL_COMPLETE			= ".uno:MacroSignature";

	// these come from sfx2/inc/sfx2/signaturestate.hxx
	/** signatures are present in this document, (SEE SPEC !)
	 * 
	 */
	public static final int		m_nSIGNATURESTATE_UNKNOWN					= -1;
	public static final int		m_nSIGNATURESTATE_NOSIGNATURES				= 0;

	/** all the signatures in this document checked so far are correct
	 * 
	 */
	public static final int		m_nSIGNATURESTATE_SIGNATURES_OK				= 1;
	/**
	 *  signature is OK, but certificate could not be validated
	 */
	public static final int		m_nSIGNATURESTATE_SIGNATURES_NOTVALIDATED	= 2;	
	/**
	 *  signature wrong 
	 */
	public static final int		m_nSIGNATURESTATE_SIGNATURES_BROKEN			= 3;
	/** State was SIGNATURES_OK, but doc is modified now
	 * NOTE: this doesn't seem to be implemented in OOo as of 2.4. The
	 * behavior is different: when the document is modified, the signatures
	 * status disappears.
	 */
	public static final int		m_nSIGNATURESTATE_SIGNATURES_INVALID		= 4;

	//names of icons used to display state of certificate/certificate elements

	public static final String	m_sSSCD_ELEMENT = "sscd-device";

	public static final String	m_nCERTIFICATE = "certificato";
	public static final String	m_nCERTIFICATE_CHECKED_OK = "certificato-ok";
	public static final String	m_nCERTIFICATE_CHECKED_WARNING = "certificato-warning";
	public static final String	m_nCERTIFICATE_CHECKED_INVALID = "certificato-err";
	public static final String	m_nCERTIFICATE_CHECKED_BROKEN = "certificato-rotto"; //TODO check alternative
	public static final String	m_nCERTIFICATE_CHECKED_BROKEN2 = "certificato-rotto-2"; //TODO
	public static final String	m_nCERTIFICATE_CHECKED_UNKNOWN = "certificato-interrogativo";
	public static final String	m_nCERTIFICATE_REMOVING = "certificato-remove";
	public static final String	m_nCERTIFICATE_ADDING = "certificato-add";
	//state of single elements, when needed
	public static final	String	m_nCERT_ELEM_OK = "check_ok";
	public static final	String	m_nCERT_ELEM_WARNING = "warning";
	public static final	String	m_nCERT_ELEM_INVALID = "errore";
	public static final	String	m_nCERT_ELEM_BROKEN = "rotto";
	public static final	String	m_nCERT_ELEM_BROKEN2 = "rotto-2";

	public static final String	m_sXADES_SIGNATURE_STREAM_NAME					= "xadessignatures.xml";
	
	// Names used in configuration, the names are defined in file
	// extension_conf_files/extension/AddonConfiguration.xcs.xml
	//// logging configuration
	public static final String	m_sENABLE_INFO_LEVEL 							= "EnableInfoLevel";// boolean
	public static final String	m_sENABLE_DEBUG_LOGGING 						= "EnableDebugLogging";// boolean
	public static final String	m_sENABLE_CONSOLE_OUTPUT 						= "EnableConsoleOutput";// boolean
	public static final String	m_sENABLE_FILE_OUTPUT 							= "EnableFileOutput";// boolean
	public static final String	m_sLOG_FILE_PATH 								= "LogFilePath";// string
	public static final String	m_sFILE_ROTATION_COUNT 							= "FileRotationCount";// int
	public static final String	m_sMAX_FILE_SIZE	 							= "MaxFileSize";// int

	//// for singleton and XLogger
	public static final String m_sSINGLETON_SERVICE = m_sWEBIDENTBASE + ".oxsit.singleton.SingleGlobalVariables";
	public static final String m_sSINGLETON_SERVICE_INSTANCE = "/singletons/"+m_sSINGLETON_SERVICE;
	
	//// for XLogger (still to be implemented
	public static final String m_sSINGLETON_LOGGER_SERVICE = m_sWEBIDENTBASE + ".oxsit.singleton.GlobalLogger";
	public static final String m_sSINGLETON_LOGGER_SERVICE_INSTANCE = "/singletons/"+m_sSINGLETON_LOGGER_SERVICE;
		
	public static final int	m_nLOG_LEVEL_FINE									= 0;
	public static final int	m_nLOG_LEVEL_INFO									= 1;
	public static final int	m_nLOG_LEVEL_DEBUG									= 2;
	public static final int	m_nLOG_LEVEL_WARNING								= 3;
	public static final int	m_nLOG_LEVEL_SEVERE									= 4;
	public static final int	m_nLOG_CONFIG									    = 5;
	public static final int	m_nLOG_ALWAYS									    = 6;

	//service for document signatures, this service implements specific interfaces, not available in
	// standard OOo, with functionality similar to some stock OOo interfaces declared unpublished
	public static final String m_sDOCUMENT_SIGNATURES_SERVICE = m_sWEBIDENTBASE + ".oxsit.ooo.security.DocumentSignatures";

	//service to hold a single certificate
	public static final String m_sX509_CERTIFICATE_SERVICE = m_sWEBIDENTBASE + ".oxsit.security.cert.X509Certificate";
	//names used to exchange extensions state
	public static final String m_sX509_CERTIFICATE_VERSION = "Version";		
	public static final String m_sX509_CERTIFICATE_ISSUER = "IssuerName";
	public static final String m_sX509_CERTIFICATE_NOT_BEFORE = "NotValidBefore";
	public static final String m_sX509_CERTIFICATE_NOT_AFTER = "NotValidAfter";	
	public static final String m_sX509_CERTIFICATE_CEXT = "CritExt";	
	public static final String m_sX509_CERTIFICATE_NCEXT = "NotCritExt";	
	public static final String m_sX509_CERTIFICATE_CERTPATH = "CertifPath";	

	//this path is the path to the temporary CRL storage cache in OOo
	//it's used with the OpenOffic.org user store area
	//(in 3.0 in GNU/Linux it's <OOo user directory> )/3/user/store 
	public	static	final	String		m_sCRL_CACHE_PATH		= "crlc";

	//service to hold a single certificate extension
	public static final String m_sCERTIFICATE_EXTENSION_SERVICE = m_sWEBIDENTBASE + ".oxsit.security.cert.CertificateExtension";

	//service to implement a dispatch interceptor
	public static final String m_sDISPATCH_INTERCEPTOR_SERVICE = m_sWEBIDENTBASE + ".oxsit.DipatchIntercept";	
	
	////////// the following UNO service names should go to the registry
	// under a key tree specifying the current signature profile.
	//currently they are mirroring the same name in IT specialized jar library
	// service to hold all the information available from SSCD devices (PKCS 11 m_nTokens) available on system.
	public static final String m_sAVAILABLE_SSCD_SERVICE = m_sWEBIDENTBASE + ".oxsit.cust_it.security.AvailableSSCDs_IT";

	//service to manage the trusted entities used to validate the user certificate and the certification chain
	//This service implements the interface XOX_TrustedEntities
	public static final String m_sTRUSTED_ENTITIES_MANAGEMENT_SERVICE_IT = m_sWEBIDENTBASE + ".oxsit.cust_it.security.cert.CertificationPath_IT";

	public static final String m_sDOCUMENT_SIGNER_SERVICE_IT = m_sWEBIDENTBASE + ".oxsit.ooo.cust_it.security.DocumentSigner_IT";
	// service to verify the document signatures present
	public static final String m_sDOCUMENT_VERIFIER_SERVICE_IT = m_sWEBIDENTBASE + ".oxsit.cust_it.security.DocumentSignaturesVerifier_IT";

	// service to implement the interfaces needed to hold the signature state after verification
	public static final String m_sSIGNATURE_STATE_SERVICE_IT = m_sWEBIDENTBASE + ".oxsit.cust_it.security.SignatureState_IT";

	/////////// end of service to be moved to registryS

	/////// this string comes from
	/// iaik.pkcs.pkcs11.wrapper.PKCS11Implementation.WRAPPER
	// pay attention when changing it !
	public static final String PKCS11_WRAPPER = "pkcs11wrapper";

	public static final String m_sPCSC_WRAPPER_NATIVE = "OCFPCSC1";
}
