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

package it.plio.ext.oxsit.comp.security.cert;

import it.plio.ext.oxsit.Helpers;
import it.plio.ext.oxsit.logging.DynamicLogger;
import it.plio.ext.oxsit.ooo.GlobConstant;
import it.plio.ext.oxsit.ooo.registry.MessageConfigurationAccess;
import it.plio.ext.oxsit.security.cert.CertificateAuthorityState;
import it.plio.ext.oxsit.security.cert.CertificateState;
import it.plio.ext.oxsit.security.cert.XOX_CertificateExtension;
import it.plio.ext.oxsit.security.cert.XOX_QualifiedCertificate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;

import com.sun.star.lang.XInitialization;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lib.uno.helper.ComponentBase;
import com.sun.star.uno.Exception;
import com.sun.star.uno.XComponentContext;

/**
 *  This service implements the QualifiedCertificate service.
 * receives the doc information from the task  
 *  
 * This objects has properties, they are set by the calling UNO objects.
 * 
 * The service is initialized with URL and XStorage of the document under test
 * Information about the certificates, number of certificates, status of every signature
 * ca be retrieved through properties 
 * 
 * @author beppec56
 *
 */
public class QualifiedCertificate extends ComponentBase //help class, implements XTypeProvider, XInterface, XWeak
			implements 
			XServiceInfo,
			XInitialization,
			XOX_QualifiedCertificate
			 {

	// the name of the class implementing this object
	public static final String			m_sImplementationName	= QualifiedCertificate.class.getName();

	// the Object name, used to instantiate it inside the OOo API
	public static final String[]		m_sServiceNames			= { GlobConstant.m_sQUALIFIED_CERTIFICATE_SERVICE };
	private XComponentContext m_xContext;
	private XMultiComponentFactory m_xMCF;
	
	protected String m_sTimeLocaleString = "id_validity_time_locale";//"%1$td %1$tB %1$tY %1$tH:%1$tM:%1$tS (%1$tZ)";
	protected String m_sLocaleLanguage = "id_iso_lang_code"; //"it";

	protected DynamicLogger m_aLogger;
	
	protected CertificateAuthorityState m_CAState;
	protected CertificateState			m_CState;
	
	protected boolean	m_bDisplayOID;

	//the certificate representation
	private X509CertificateStructure m_aX509;

	private String m_sSubjectDisplayName;
	
	private String m_sSubjectName = "";

	private String m_sVersion = "";

	private String m_sSerialNumber = "";

	private String m_sIssuerDisplayName = "";

	private String m_sIssuerName = "";

	private String m_sNotValidAfter = "";

	private String m_sNotValidBefore = "";

	private String m_sSubjectPublicKeyAlgorithm = "";

	private String m_sSubjectPublicKeyValue = "";

	private String m_sSignatureAlgorithm = "";

	private String m_sIssuerUniqueID = "";

	private String m_sMD5Thumbprint = "";

	private String m_sSHA1Thumbprint = "";

	private String m_sSubjectUniqueID = "";

	private Locale m_lTheLocale;

	/**
	 * 
	 * 
	 * @param _ctx
	 */
	public QualifiedCertificate(XComponentContext _ctx) {
		m_aLogger = new DynamicLogger(this, _ctx);
//		m_aLogger.enableLogging();
    	m_aLogger.ctor();
    	m_CAState = CertificateAuthorityState.NO_CNIPA_ROOT;
    	m_CState = CertificateState.NOT_VERIFIABLE;
    	m_xContext = _ctx;
    	m_xMCF = m_xContext.getServiceManager();
    	
    	m_bDisplayOID = false;
    	
//grab the locale string, we'll use the interface language as a locale
    	//e.g. if interface language is Italian, the locale will be Italy, Italian
		MessageConfigurationAccess m_aRegAcc = null;
		m_aRegAcc = new MessageConfigurationAccess(m_xContext, m_xMCF);

		try {
			m_sTimeLocaleString = m_aRegAcc.getStringFromRegistry( m_sTimeLocaleString );			
			m_sLocaleLanguage = m_aRegAcc.getStringFromRegistry( m_sLocaleLanguage );
		} catch (com.sun.star.uno.Exception e) {
			m_aLogger.severe("ctor", e);
		}
		m_aRegAcc.dispose();
		//locale of the extension
		m_lTheLocale = new Locale(m_sLocaleLanguage);
	}

	public String getImplementationName() {
//		m_aLogger.entering("getImplementationName");
		return m_sImplementationName;
	}
	
	/* (non-Javadoc)
	 * @see com.sun.star.lang.XServiceInfo#getSupportedServiceNames()
	 */
	public String[] getSupportedServiceNames() {
//		m_aLogger.info("getSupportedServiceNames");
		return m_sServiceNames;
	}

	/* (non-Javadoc)
	 * @see com.sun.star.lang.XServiceInfo#supportsService(java.lang.String)
	 */
	public boolean supportsService(String _sService) {
		int len = m_sServiceNames.length;

		m_aLogger.info("supportsService",_sService);
		for (int i = 0; i < len; i++) {
			if (_sService.equals( m_sServiceNames[i] ))
				return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see com.sun.star.lang.XInitialization#initialize(java.lang.Object[])
	 * 
	 * arg0[0] = the DER stream of the certificate, e.g. a byte array that can be read as a
	 * certificate.
	 * It will initialize all the object contents
	 */
	@Override
	public void initialize(Object[] _DEREncoded) throws Exception {
		// TODO Auto-generated method stub
		
		//Will simply call its
		//setDEREncoded(byte[] ) method
		
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.security.cert.XOX_QualifiedCertificate#getSubjectDisplayName()
	 */
	@Override
	public String getSubjectDisplayName() {
		// TODO Auto-generated method stub
		return m_sSubjectDisplayName;
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.security.cert.XOX_QualifiedCertificate#getVersion()
	 */
	@Override
	public String getVersion() {
		return m_sVersion;
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.security.cert.XOX_QualifiedCertificate#getNotValidAfter()
	 */
	@Override
	public String getNotValidAfter() {
		return m_sNotValidAfter;
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.security.cert.XOX_QualifiedCertificate#getNotValidBefore()
	 */
	@Override
	public String getNotValidBefore() {
		return m_sNotValidBefore;
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.security.cert.XOX_QualifiedCertificate#getIssuerDisplayName()
	 */
	@Override
	public String getIssuerDisplayName() {
		return m_sIssuerDisplayName;
	}

	/* (non-Javadoc)

	 * @see it.plio.ext.oxsit.security.cert.XOX_QualifiedCertificate#getIssuerName()
	 */
	@Override
	public String getIssuerName() {
		return m_sIssuerName;
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.security.cert.XOX_QualifiedCertificate#getIssuerUniqueID()
	 */
	@Override
	public String getIssuerUniqueID() {
		return m_sIssuerUniqueID;
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.security.cert.XOX_QualifiedCertificate#getMD5Thumbprint()
	 */
	@Override
	public String getMD5Thumbprint() {
		return m_sMD5Thumbprint;
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.security.cert.XOX_QualifiedCertificate#getSHA1Thumbprint()
	 */
	@Override
	public String getSHA1Thumbprint() {
		return m_sSHA1Thumbprint;
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.security.cert.XOX_QualifiedCertificate#getSerialNumber()
	 */
	@Override
	public String getSerialNumber() {
		return m_sSerialNumber;
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.security.cert.XOX_QualifiedCertificate#getSignatureAlgorithm()
	 */
	@Override
	public String getSignatureAlgorithm() {
		return m_sSignatureAlgorithm;
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.security.cert.XOX_QualifiedCertificate#getSubjectName()
	 */
	@Override
	public String getSubjectName() {
		return m_sSubjectName;
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.security.cert.XOX_QualifiedCertificate#getSubjectPublicKeyAlgorithm()
	 */
	@Override
	public String getSubjectPublicKeyAlgorithm() {
		return m_sSubjectPublicKeyAlgorithm;
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.security.cert.XOX_QualifiedCertificate#getSubjectPublicKeyValue()
	 */
	@Override
	public String getSubjectPublicKeyValue() {
		return m_sSubjectPublicKeyValue;
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.security.cert.XOX_QualifiedCertificate#getSubjectUniqueID()
	 */
	@Override
	public String getSubjectUniqueID() {
		return m_sSubjectUniqueID;
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.security.cert.XOX_QualifiedCertificate#verifyCAForCertificate()
	 */
	@Override
	public boolean verifyCAForCertificate() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.security.cert.XOX_QualifiedCertificate#verifyCRLForCertificate()
	 */
	@Override
	public boolean verifyCRLForCertificate() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.security.cert.XOX_QualifiedCertificate#getCertificateState()
	 */
	@Override
	public CertificateState getCertificateState() {
		return m_CState;
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.security.cert.XOX_QualifiedCertificate#getCertificationAuthorityState()
	 */
	@Override
	public CertificateAuthorityState getCertificationAuthorityState() {
		return m_CAState;
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.security.cert.XOX_QualifiedCertificate#getCertificationPath()
	 */
	@Override
	public XOX_QualifiedCertificate getCertificationPath() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.security.cert.XOX_QualifiedCertificate#getCertificateExtension(java.lang.String)
	 */
	@Override
	public XOX_CertificateExtension getCertificateExtension(String _aOID) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.security.cert.XOX_QualifiedCertificate#getExtensions()
	 */
	@Override
	public XOX_CertificateExtension[] getExtensions() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.security.cert.XOX_QualifiedCertificate#getCriticalExtensions()
	 */
	@Override
	public XOX_CertificateExtension[] getCriticalExtensions() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.security.cert.XOX_QualifiedCertificate#getNonCriticalExtensions()
	 */
	@Override
	public XOX_CertificateExtension[] getNonCriticalExtensions() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.security.cert.XOX_QualifiedCertificate#getDEREncoded()
	 */
	@Override
	public byte[] getDEREncoded() {
		return m_aX509.getDEREncoded();
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.security.cert.XOX_QualifiedCertificate#setDEREncoded(byte[])
	 * 
	 * When this method is called, the DER image passed will be used as the new certificate representation
	 * and the certificate extensions will be evaluated again. 
	 */
	@Override
	public void setDEREncoded(byte[] _DEREncoded) {
		//
		m_aX509 = null; //remove old certificate
		ByteArrayInputStream as = new ByteArrayInputStream(_DEREncoded); 
		ASN1InputStream aderin = new ASN1InputStream(as);
		DERObject ado;
		try {
			ado = aderin.readObject();
			m_aX509 = new X509CertificateStructure((ASN1Sequence) ado);
//initializes the certificate display information
			initSubjectName();
			m_sVersion = String.format("V%d", m_aX509.getVersion());
			m_sSerialNumber = new String(""+m_aX509.getSerialNumber().getValue());
			initIssuerName();
			m_sNotValidBefore = initCertDate(m_aX509.getStartDate().getDate());
			m_sNotValidAfter =  initCertDate(m_aX509.getEndDate().getDate());
			m_sSubjectPublicKeyAlgorithm = initPublicKeyAlgorithm();
			m_sSubjectPublicKeyValue = initPublicKeyData();
			m_sSignatureAlgorithm = initSignatureAlgorithm();
			initThumbPrints();
			//now initializes the Extension listing
		} catch (IOException e) {
			m_aLogger.severe("setDEREncoded", e);
		}
	}

	////////////////// internal functions
	/**
	 * 
	 */
	protected void initThumbPrints() {
		//obtain a byte block of the entire certificate data
		ByteArrayOutputStream   bOut = new ByteArrayOutputStream();
		DEROutputStream         dOut = new DEROutputStream(bOut);
		try {
			dOut.writeObject(m_aX509);
			byte[] certBlock = bOut.toByteArray();

			//now compute the certificate SHA1 & MD5 digest
			SHA1Digest digsha1 = new SHA1Digest();
			digsha1.update(certBlock, 0, certBlock.length);
			byte[] hashsha1 = new byte[digsha1.getDigestSize()];
			digsha1.doFinal(hashsha1, 0);
			m_sSHA1Thumbprint = Helpers.printHexBytes(hashsha1);
			MD5Digest  digmd5 = new MD5Digest();
			digmd5.update(certBlock, 0, certBlock.length);
			byte[] hashmd5 = new byte[digmd5.getDigestSize()];
			digmd5.doFinal(hashmd5, 0);
			m_sMD5Thumbprint = Helpers.printHexBytes(hashmd5);
		} catch (IOException e) {
			m_aLogger.severe("initThumbPrints", e);
		}		
	}

	protected String initSignatureAlgorithm() {
		DERObjectIdentifier oi = m_aX509.getSignatureAlgorithm().getObjectId();
		return new String(""+(
				(oi.equals(X509CertificateStructure.sha1WithRSAEncryption)) ? 
				"pkcs-1 sha1WithRSAEncryption" : oi.toString())
				);
	}

	protected String initPublicKeyData() {
		byte[] sbjkd = m_aX509.getSubjectPublicKeyInfo().getPublicKeyData().getBytes();
		return Helpers.printHexBytes(sbjkd);
	}

	protected String initPublicKeyAlgorithm() {
//		AlgorithmIdentifier aid = m_aX509.getSignatureAlgorithm();
		DERObjectIdentifier oi = m_aX509.getSubjectPublicKeyInfo().getAlgorithmId().getObjectId();
		return new String(""+(
				(oi.equals(X509CertificateStructure.rsaEncryption)) ?
					"pkcs-1 rsaEncryption" : oi.getId())
					);
	}

	protected String initCertDate(Date _aTime) {
		//force UTC time
		TimeZone gmt = TimeZone.getTimeZone("UTC");
		Calendar calendar = new GregorianCalendar(gmt,m_lTheLocale);
		calendar.setTime(_aTime);	
//string with time only
//the locale should be the one of the extension not the Java one.
		String time = String.format(m_lTheLocale,m_sTimeLocaleString, calendar);
		return time;
	}	

	protected void initSubjectName() {
		m_sSubjectName = "";
		//print the subject
		//order of printing is as got in the CNIPA spec
		//first, grab the OID in the subject name
		X509Name aName = m_aX509.getSubject();
		Vector<DERObjectIdentifier> oidv =  aName.getOIDs();
		Vector<?> values = aName.getValues();
		HashMap<DERObjectIdentifier, String> hm = new HashMap<DERObjectIdentifier, String>(20);
		for(int i=0; i< oidv.size(); i++) {
			m_sSubjectName = m_sSubjectName + X509Name.DefaultSymbols.get(oidv.elementAt(i))+"="+values.elementAt(i).toString()+
					((m_bDisplayOID) ? (" (OID: "+oidv.elementAt(i).toString()+")" ): "") +" \n";
			hm.put(oidv.elementAt(i), values.elementAt(i).toString());
		}
		//extract data from subject name following CNIPA recommendation
		/*
		 * first lookup for givenname and surname, if not existent
		 * lookup for commonName (cn), if not existent
		 * lookup for pseudonym ()
		 */

		//look for givename (=nome di battesimo)
			m_sSubjectDisplayName = "";			
			//see BC source code for details about DefaultLookUp behaviour
			DERObjectIdentifier oix = (DERObjectIdentifier)(X509Name.DefaultLookUp.get("givenname")); 
			if(hm.containsKey(oix)) {
				String tmpName = hm.get(oix).toString();
				oix = (DERObjectIdentifier)(X509Name.DefaultLookUp.get("surname"));
				if(hm.containsKey(oix))
					m_sSubjectDisplayName = tmpName +" "+hm.get(oix).toString();
			}
			if(m_sSubjectDisplayName.length() == 0) {
				//check for CN
				oix = (DERObjectIdentifier)(X509Name.DefaultLookUp.get("cn")); 
				if(hm.containsKey(oix)) {
					m_sSubjectDisplayName = hm.get(oix).toString();
				}
			}
			if(m_sSubjectDisplayName.length() == 0) {
				//if still not, check for pseudodym
				oix = (DERObjectIdentifier)(X509Name.DefaultLookUp.get("pseudonym"));
				if(hm.containsKey(oix))
					m_sSubjectDisplayName = hm.get(oix).toString();						
			}
			if(m_sSubjectDisplayName.length() == 0)
				m_sSubjectDisplayName = m_sSubjectName;
	}

	protected void initIssuerName() {
		m_sIssuerName = "";
		X509Name aName = m_aX509.getIssuer();
		Vector<DERObjectIdentifier> oidv =  aName.getOIDs();
		HashMap<DERObjectIdentifier, String> hm = new HashMap<DERObjectIdentifier, String>(20);
		Vector<?> values = aName.getValues();
		for(int i=0; i< oidv.size(); i++) {
			m_sIssuerName = m_sIssuerName + X509Name.DefaultSymbols.get(oidv.elementAt(i))+"="+values.elementAt(i).toString()+
			((m_bDisplayOID) ? (" (OID: "+oidv.elementAt(i).toString()+")" ): "") +" \n";
			hm.put(oidv.elementAt(i), values.elementAt(i).toString());
		}		
		//look for givename (=nome di battesimo)
		m_sIssuerDisplayName = "";			
		//see BC source code for details about DefaultLookUp behaviour
		DERObjectIdentifier oix = (DERObjectIdentifier)(X509Name.DefaultLookUp.get("givenname")); 
		if(hm.containsKey(oix)) {
			String tmpName = hm.get(oix).toString();
			oix = (DERObjectIdentifier)(X509Name.DefaultLookUp.get("surname"));
			if(hm.containsKey(oix))
				m_sIssuerDisplayName = tmpName +" "+hm.get(oix).toString();
		}
		if(m_sIssuerDisplayName.length() == 0) {
			//check for CN
			oix = (DERObjectIdentifier)(X509Name.DefaultLookUp.get("cn")); 
			if(hm.containsKey(oix)) {
				m_sIssuerDisplayName = hm.get(oix).toString();
			}
		}
		if(m_sIssuerDisplayName.length() == 0) {
			//if still not, check for pseudodym
			oix = (DERObjectIdentifier)(X509Name.DefaultLookUp.get("pseudonym"));
			if(hm.containsKey(oix))
				m_sIssuerDisplayName = hm.get(oix).toString();						
		}
		if(m_sIssuerDisplayName.length() == 0)
			m_sIssuerDisplayName = m_sIssuerName;
	}
}
