/*
 * SAXDigiDocFactory.java
 * AUTHOR:  Veiko Sinivee, S|E|B IT Partner Estonia
 * Modified by Roberto Resoli
 */

package com.yacme.ext.oxsit.cust_it.comp.security.xades.factory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.security.MessageDigest;

import java.util.Stack;
import java.util.ArrayList;
import java.util.Hashtable;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import java.security.cert.X509Certificate;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TSPException;

import com.sun.star.embed.XStorage;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.XComponentContext;
import com.yacme.ext.oxsit.cust_it.comp.security.odfdoc.ODFSignedDoc;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.Base64Util;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.CertID;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.CertValue;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.CompleteCertificateRefs;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.CompleteRevocationRefs;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.ConvertUtils;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.DataFile;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.DataFileAttribute;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.IncludeInfo;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.KeyInfo;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.Notary;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.Reference;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.Signature;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.SignatureProductionPlace;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.SignatureValue;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.SignedDoc;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.SignedDocException;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.SignedInfo;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.SignedProperties;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.TimestampInfo;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.UnsignedProperties;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.utils.ConfigManager;
import com.yacme.ext.oxsit.logging.DynamicLogger;

/**
 * SAX implementation of DigiDocFactory
 * Provides methods for reading a DigiDoc file
 * @author  Veiko Sinivee
 * @modified Roberto Resoli
 * @version 1.0
 */
public class SAXSignedDocFactory extends DefaultHandler implements DigiDocFactory {
	private Stack m_tags;
	private ODFSignedDoc m_doc;
	private String m_strSigValTs, m_strSigAndRefsTs;
	private StringBuffer m_sbCollectChars;
	private StringBuffer m_sbCollectItem;
	private StringBuffer m_sbCollectSignature;
	private boolean m_bCollectDigest;
	private String m_xmlnsAttr;
	/** This mode means collect SAX events into xml data
	 * and is used to collect all <DataFile>, <SignedInfo> and
	 * <SignedProperties> content. Also servers as level of
	 * embedded DigiDoc files. Initially it should be 0. If
	 * we start collecting data then it's 1 and if we find
	 * another SignedDoc inside a DataFile then it will be incremented
	 * in order to know which is the correct </DataFile> tag to leave
	 * the collect mode
	 */
	private int m_nCollectMode;
	/** log4j logger */
	//private Logger m_logger = null;
	/** root/CA certificates */
	private Hashtable m_rootCerts;
	/** calculation of digest */
	private MessageDigest m_digest;
	/** temp output stream used to cache DataFile content */
	private FileOutputStream m_dfCacheOutStream;

	//ROB
	private DynamicLogger m_aLogger;
	protected XComponentContext m_xCtx;
	protected XStorage m_xDocumentStorage;
	protected XComponentContext m_xCC;
	private XMultiComponentFactory m_xMCF;

	/**
	 * Creates new SAXDigiDocFactory
	 * and initializes the variables
	 */
	public SAXSignedDocFactory(XMultiComponentFactory _xMCF, XComponentContext _context, XStorage _XDocumentStorage) {
		m_tags = new Stack();
		m_doc = null;
		m_sbCollectSignature = null;
		m_xmlnsAttr = null;
		m_rootCerts = null;
		m_sbCollectItem = null;
		m_digest = null;
		m_bCollectDigest = false;
		m_dfCacheOutStream = null;

		//ROB
		m_xCtx = _context;
		m_xMCF = _xMCF;
		m_aLogger = new DynamicLogger(this, _context);
		m_aLogger.enableLogging();
		m_aLogger.ctor();
		m_xDocumentStorage = _XDocumentStorage;
	}

	/**
	 * Helper method to update sha1 digest with some data
	 * @param data
	 */
	private void updateDigest(byte[] data) {
		try {
			// if not inited yet then initialize digest
			if (m_digest == null)
				m_digest = MessageDigest.getInstance("SHA-1");
			m_digest.update(data);
		} catch (Exception ex) {
			m_aLogger.severe("Error calculating digest: ", ex);
			//ex.printStackTrace();
		}
	}

	/**
	 * Helper method to calculate the digest result and 
	 * reset digest
	 * @return sha-1 digest value
	 */
	private byte[] getDigest() {
		byte[] digest = null;
		try {
			// if not inited yet then initialize digest
			digest = m_digest.digest();
			m_digest = null; // reset for next calculation
		} catch (Exception ex) {
			m_aLogger.severe("Error calculating digest: ", ex);
			//ex.printStackTrace();
		}
		return digest;
	}

	/**
	 * initializes the implementation class
	 */
	public void init() throws SignedDocException {
		if (m_rootCerts == null)
			initCACerts();
	}

	/** 
	 * initializes the CA certificates  
	 */
	public void initCACerts() throws SignedDocException {
		try {
			m_rootCerts = new Hashtable();
			m_aLogger.debug("Loading CA certs");
			int nCerts = ConfigManager.instance().getIntProperty("DIGIDOC_CA_CERTS", 0);
			for (int i = 0; i < nCerts; i++) {
				String certFile = ConfigManager.instance().getProperty("DIGIDOC_CA_CERT" + (i + 1));
				m_aLogger.debug("CA: " + ("DIGIDOC_CA_CERT" + (i + 1)) + " file: " + certFile);
				X509Certificate cert = SignedDoc.readCertificate(certFile);
				if (cert != null) {
					m_aLogger.debug("CA subject: " + cert.getSubjectDN() + " issuer: "
							+ cert.getIssuerX500Principal().getName("RFC1779"));
					m_rootCerts.put(cert.getSubjectX500Principal().getName("RFC1779"), cert);
				}
			}
		} catch (Exception e) {
			SignedDocException.handleException(e, SignedDocException.ERR_CA_CERT_READ);
		}
	}

	/**
	 * Verifies that the signers cert
	 * has been signed by at least one
	 * of the known root certs
	 * @param cert certificate to check
	 */
	public boolean verifyCertificate(X509Certificate cert) throws SignedDocException {
		boolean rc = false;
		if (m_rootCerts != null && !m_rootCerts.isEmpty()) {
			try {
				X509Certificate rCert = (X509Certificate) m_rootCerts.get(cert.getIssuerX500Principal().getName("RFC1779"));
				if (rCert != null) {
					cert.verify(rCert.getPublicKey());
					rc = true;
				}
			} catch (Exception ex) {
				SignedDocException.handleException(ex, SignedDocException.ERR_UNKNOWN_CA_CERT);
			}
		}
		return rc;
	}

	/**
	 * Finds the CA for this certificate
	 * if the root-certs table is not empty
	 * @param cert certificate to search CA for
	 * @return CA certificate
	 */
	public X509Certificate findCAforCertificate(X509Certificate cert) {
		X509Certificate caCert = null;
		if (cert != null && m_rootCerts != null && !m_rootCerts.isEmpty()) {
			//String cn = SignedDoc.getCommonName(cert.getIssuerX500Principal().getName("RFC1779"));
			String dn = cert.getIssuerX500Principal().getName("RFC1779");
			m_aLogger.debug("Find CA cert for issuer: " + dn);
			caCert = (X509Certificate) m_rootCerts.get(dn);
			m_aLogger.debug("CA: " + ((caCert == null) ? "NULL" : "OK"));
		}
		return caCert;
	}

	/**
	 * Reads in a DigiDoc file
	 * @param fileName file name
	 * @return signed document object if successfully parsed
	 */
	public SignedDoc readSignedDoc(String fileName) throws SignedDocException {
		// Use an instance of ourselves as the SAX event handler
		SAXSignedDocFactory handler = this;
		// Use the default (non-validating) parser
		SAXParserFactory factory = SAXParserFactory.newInstance();
		//factory.setNamespaceAware(true);
		try {
			SAXParser saxParser = factory.newSAXParser();
			FileInputStream is = new FileInputStream(fileName);
			saxParser.parse(is, handler);
			is.close();
		} catch (SAXSignedDocException ex) {
			throw ex.getSignedDocException();
		} catch (Exception ex) {
			SignedDocException.handleException(ex, SignedDocException.ERR_PARSE_XML);
		}
		if (m_doc == null)
			throw new SignedDocException(SignedDocException.ERR_DIGIDOC_FORMAT, "This document is not in digidoc format", null);
		return m_doc;
	}

	/**
	 * Reads in a DigiDoc file
	 * @param digiDocStream opened stream with DigiDoc data
	 * The user must open and close it.
	 * @return signed document object if successfully parsed
	 */
	public SignedDoc readSignedDoc(InputStream digiDocStream) throws SignedDocException {
		// Use an instance of ourselves as the SAX event handler
		SAXSignedDocFactory handler = this;
		// Use the default (non-validating) parser
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(digiDocStream, handler);
		} catch (SAXSignedDocException ex) {
			m_aLogger.severe(ex);
			throw ex.getSignedDocException();
		} catch (Exception ex) {
			m_aLogger.severe(ex);
			SignedDocException.handleException(ex, SignedDocException.ERR_PARSE_XML);
		}
		if (m_doc == null)
			throw new SignedDocException(SignedDocException.ERR_DIGIDOC_FORMAT, "This document is not in digidoc format", null);
		return m_doc;
	}

	/**
	 * Reads in only one <Signature>
	 * @param sdoc SignedDoc to add this signature to
	 * @param sigStream opened stream with Signature data
	 * The user must open and close it.
	 * @return signed document object if successfully parsed
	 */
	public Signature readSignature(SignedDoc sdoc, InputStream sigStream) throws SignedDocException {
		// Use an instance of ourselves as the SAX event handler
		SAXSignedDocFactory handler = this;
		// Use the default (non-validating) parser
		SAXParserFactory factory = SAXParserFactory.newInstance();
		//FIXME: hack (the casting) added to read in the ODF special document, needs to be checked
		m_doc = (ODFSignedDoc)sdoc;
		//m_doc.addSignature(new Signature(sdoc));
		m_nCollectMode = 0;
		try {
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(sigStream, handler);
		} catch (SAXSignedDocException ex) {
			throw ex.getSignedDocException();
		} catch (Exception ex) {
			SignedDocException.handleException(ex, SignedDocException.ERR_PARSE_XML);
		}
		if (m_doc.getLastSignature() == null)
			throw new SignedDocException(SignedDocException.ERR_DIGIDOC_FORMAT, "This document is not in Signature format", null);
		return m_doc.getLastSignature();
	}

	/**
	 * Helper method to canonicalize a piece of xml
	 * @param xml data to be canonicalized
	 * @return canonicalized xml
	 */
	private String canonicalizeXml(String xml) {
		try {
			CanonicalizationFactory canFac = ConfigManager.instance().getCanonicalizationFactory();
			byte[] tmp = canFac.canonicalize(xml.getBytes("UTF-8"), SignedDoc.CANONICALIZATION_METHOD_20010315);
			return new String(tmp, "UTF-8");
		} catch (Exception ex) {
			m_aLogger.severe("Canonicalizing exception: ", ex);
		}
		return null;
	}

	public SignedDoc getSignedDoc() {
		return m_doc;
	}

	public Signature getLastSignature() {
		return m_doc.getLastSignature();
	}

	/**
	 * Start Document handler
	 */
	public void startDocument() throws SAXException {
		m_nCollectMode = 0;
		m_xmlnsAttr = null;
		m_dfCacheOutStream = null;
	}

	private void findCertIDandCertValueTypes(Signature sig) {
		//if(m_logger.isDebugEnabled())
		m_aLogger.debug("Sig: " + sig.getId() + " certids: " + sig.countCertIDs());
		for (int i = 0; (sig != null) && (i < sig.countCertIDs()); i++) {
			CertID cid = sig.getCertID(i);
			//if(m_logger.isDebugEnabled())
			m_aLogger.debug("CertId: " + cid.getId() + " type: " + cid.getType() + " nr: " + cid.getSerial());
			if (cid.getType() == CertID.CERTID_TYPE_UNKNOWN) {
				CertValue cval = sig.findCertValueWithSerial(cid.getSerial());
				if (cval != null) {
					String cn = null;
					try {
						cn = SignedDoc.getCommonName(cval.getCert().getSubjectDN().getName());
						//if(m_logger.isDebugEnabled())
						m_aLogger.debug("CertId type: " + cid.getType() + " nr: " + cid.getSerial() + " cval: " + cval.getId()
								+ " CN: " + cn);
						if (ConvertUtils.isKnownOCSPCert(cn)) {
							//if(m_logger.isInfoEnabled())
							m_aLogger.debug("Cert: " + cn + " is OCSP responders cert");
							cid.setType(CertID.CERTID_TYPE_RESPONDER);
							cval.setType(CertValue.CERTVAL_TYPE_RESPONDER);
						}
						if (ConvertUtils.isKnownTSACert(cn)) {
							//if(m_logger.isDebugEnabled())
							m_aLogger.debug("Cert: " + cn + " is TSA cert");
							cid.setType(CertID.CERTID_TYPE_TSA);
							cval.setType(CertValue.CERTVAL_TYPE_TSA);
							//if(m_logger.isDebugEnabled())
							m_aLogger.debug("CertId: " + cid.getId() + " type: " + cid.getType() + " nr: " + cid.getSerial());
						}
					} catch (SignedDocException ex) {
						m_aLogger.severe("Error setting type on certid or certval: ", cn);
					}
				}
			}

		} // for i < sig.countCertIDs()
			//if(m_logger.isDebugEnabled())
		m_aLogger.debug("Sig: " + sig.getId() + " certvals: " + sig.countCertValues());
		for (int i = 0; (sig != null) && (i < sig.countCertValues()); i++) {
			CertValue cval = sig.getCertValue(i);
			//if(m_logger.isDebugEnabled())
			m_aLogger.debug("CertValue: " + cval.getId() + " type: " + cval.getType());
			if (cval.getType() == CertValue.CERTVAL_TYPE_UNKNOWN) {
				String cn = null;
				try {
					cn = SignedDoc.getCommonName(cval.getCert().getSubjectDN().getName());
					if (ConvertUtils.isKnownOCSPCert(cn)) {
						//if(m_logger.isDebugEnabled())
						m_aLogger.debug("Cert: " + cn + " is OCSP responders cert");
						cval.setType(CertValue.CERTVAL_TYPE_RESPONDER);
					}
					if (ConvertUtils.isKnownTSACert(cn)) {
						//if(m_logger.isDebugEnabled())
						m_aLogger.debug("Cert: " + cn + " is TSA cert");
						cval.setType(CertValue.CERTVAL_TYPE_TSA);
					}
				} catch (SignedDocException ex) {
					m_aLogger.severe("Error setting type on certid or certval: ", cn);
				}
			}
		}
	}

	/**
	 * End Document handler
	 */
	public void endDocument() throws SAXException {

	}

	/*private void debugWriteFile(String name, String data)
	{
	    try {
	        String str = "C:\\veiko\\JDigiTest1\\" + name;
	        System.out.println("Writing debug file: " + str);
	        FileOutputStream fos = new FileOutputStream(str);
	        fos.write(data.getBytes("UTF-8"));
	        fos.close();
	    } catch(Exception ex) {
	        System.out.println("Error: " + ex);
	        ex.printStackTrace(System.out);
	    }
	}*/

	/**
	 * Start Element handler
	 * @param namespaceURI namespace URI
	 * @param lName local name
	 * @param qName qualified name
	 * @param attrs attributes
	 */
	public void startElement(String namespaceURI, String lName, String qName, Attributes attrs) throws SAXSignedDocException {
		//if(m_logger.isDebugEnabled())
//		m_aLogger.debug("Start Element: " + qName + " lname: " + lName + " uri: " + namespaceURI);
		m_tags.push(qName);
		if (qName.equals("SigningTime") || qName.equals("IssuerSerial") || qName.equals("X509SerialNumber")
				|| qName.equals("X509IssuerName") || qName.equals("ClaimedRole") || qName.equals("City")
				|| qName.equals("StateOrProvince") || qName.equals("CountryName") || qName.equals("PostalCode")
				|| qName.equals("SignatureValue") || qName.equals("DigestValue")
				||
				//qName.equals("EncapsulatedX509Certificate") ||
				qName.equals("IssuerSerial") || qName.equals("ResponderID") || qName.equals("X509SerialNumber")
				|| qName.equals("ProducedAt") || qName.equals("EncapsulatedTimeStamp") || qName.equals("EncapsulatedOCSPValue"))
			m_sbCollectItem = new StringBuffer();
		// <X509Certificate>
		// Prepare CertValue object
		if (qName.equals("X509Certificate")) {
			Signature sig = getLastSignature();
			CertValue cval = null;
			try {
				//if(m_logger.isDebugEnabled())
				m_aLogger.debug("Adding signers cert to: " + sig.getId());
				cval = sig.getOrCreateCertValueOfType(CertValue.CERTVAL_TYPE_SIGNER);
			} catch (SignedDocException ex) {
				SAXSignedDocException.handleException(ex);
			}
			m_sbCollectItem = new StringBuffer();
		}
		// <EncapsulatedX509Certificate>
		// Prepare CertValue object and record it's id
		if (qName.equals("EncapsulatedX509Certificate")) {
			Signature sig = getLastSignature();
			String id = null;
			for (int i = 0; i < attrs.getLength(); i++) {
				String key = attrs.getQName(i);
				if (key.equals("Id")) {
					id = attrs.getValue(i);
				}
			}
			CertValue cval = new CertValue();
			if (id != null) {
				cval.setId(id);
				try {
					if (id.indexOf("RESPONDER_CERT") != -1)
						cval.setType(CertValue.CERTVAL_TYPE_RESPONDER);
				} catch (SignedDocException ex) {
					SAXSignedDocException.handleException(ex);
				}
			}
			//if (m_logger.isDebugEnabled())
			m_aLogger.debug("Adding cval " + cval.getId() + " type: " + cval.getType() + " to: " + sig.getId());
			sig.addCertValue(cval);
			m_sbCollectItem = new StringBuffer();
		}
		//System.out.println("Allocating default buf");
		// the following elements switch collect mode
		// in and out
		// <DataFile>
		if (qName.equals("DataFile")) {
			String ContentType = null, Filename = null, Id = null, MimeType = null, Size = null, DigestType = null, Codepage = null;
			byte[] DigestValue = null;
			ArrayList dfAttrs = new ArrayList();
			for (int i = 0; i < attrs.getLength(); i++) {
				String key = attrs.getQName(i);
				if (key.equals("ContentType")) {
					ContentType = attrs.getValue(i);
				} else if (key.equals("Filename")) {
					Filename = attrs.getValue(i);
				} else if (key.equals("Id")) {
					Id = attrs.getValue(i);
				} else if (key.equals("MimeType")) {
					MimeType = attrs.getValue(i);
				} else if (key.equals("Size")) {
					Size = attrs.getValue(i);
				} else if (key.equals("DigestType")) {
					DigestType = attrs.getValue(i);
				} else if (key.equals("Codepage")) {
					Codepage = attrs.getValue(i);
				} else if (key.equals("DigestValue")) {
					DigestValue = Base64Util.decode(attrs.getValue(i));
				} else {
					try {
						if (!key.equals("xmlns")) {
							DataFileAttribute attr = new DataFileAttribute(key, attrs.getValue(i));
							dfAttrs.add(attr);
						}
					} catch (SignedDocException ex) {
						SAXSignedDocException.handleException(ex);
					}
				} // else
			} // for
			if (m_nCollectMode == 0) {
				try {
					DataFile df = new DataFile(Id, ContentType, Filename, MimeType, m_doc);
					m_dfCacheOutStream = null; // default is don't use cache file
					if (Size != null)
						df.setSize(Long.parseLong(Size));
					if (DigestType != null)
						df.setDigestType(DigestType);
					if (DigestValue != null)
						df.setDigestValue(DigestValue);
					if (Codepage != null)
						df.setInitialCodepage(Codepage);
					for (int i = 0; i < dfAttrs.size(); i++)
						df.addAttribute((DataFileAttribute) dfAttrs.get(i));
					// enable caching if requested
					if (df.schouldUseTempFile()) {
						File fCache = df.createCacheFile();
						//if(m_logger.isDebugEnabled())
						m_aLogger.debug("DF: " + Id + " size: " + df.getSize() + " cache-file: " + fCache.getAbsolutePath());
						m_dfCacheOutStream = new FileOutputStream(fCache);
					}
					m_doc.addDataFile(df);
				} catch (IOException ex) {
					SAXSignedDocException.handleException(ex);
				} catch (SignedDocException ex) {
					SAXSignedDocException.handleException(ex);
				}
			}
			m_nCollectMode++;
			// try to anticipate how much memory we need for collecting this <DataFile>
			try {
				if (Size != null) {
					int nSize = Integer.parseInt(Size);
					if (ContentType.equals(DataFile.CONTENT_EMBEDDED)) {
						nSize += 1024; // just a little bit for whitespace & xml tags
						m_bCollectDigest = false;
					}
					if (ContentType.equals(DataFile.CONTENT_EMBEDDED_BASE64)) {
						nSize *= 2;
						m_bCollectDigest = true;
						m_aLogger.debug("Start collecting digest");
					}
					//System.out.println("Allocating buf: " + nSize + " Element: "	+ qName + " lname: "  + lName + " uri: " + namespaceURI);
					if (m_dfCacheOutStream == null) // if we use temp files then we don't cache in memory 
						m_sbCollectChars = new StringBuffer(nSize);
				}
			} catch (Exception ex) {
				m_aLogger.severe("Error: ", ex);
			}
		}

		// <SignedInfo>
		if (qName.equals("SignedInfo")) {
			if (m_nCollectMode == 0) {
				try {
					if (m_doc.getVersion().equals(SignedDoc.VERSION_1_3) || m_doc.getVersion().equals(SignedDoc.VERSION_1_4))
						m_xmlnsAttr = null;
					else
						m_xmlnsAttr = SignedDoc.xmlns_xmldsig;
					Signature sig = getLastSignature();
					SignedInfo si = new SignedInfo(sig);
					sig.setSignedInfo(si);
				} catch (SignedDocException ex) {
					SAXSignedDocException.handleException(ex);
				}
			}
			m_nCollectMode++;
			m_sbCollectChars = new StringBuffer(1024);
		}
		// <SignedProperties>
		if (qName.equals("SignedProperties")) {
			String Id = attrs.getValue("Id");
			String Target = attrs.getValue("Target");
			if (m_nCollectMode == 0) {
				try {
					if (m_doc.getVersion().equals(SignedDoc.VERSION_1_3) || m_doc.getVersion().equals(SignedDoc.VERSION_1_4))
						m_xmlnsAttr = null;
					else
						m_xmlnsAttr = SignedDoc.xmlns_xmldsig;
					Signature sig = getLastSignature();
					SignedProperties sp = new SignedProperties(sig);
					sp.setId(Id);
					if (Target != null)
						sp.setTarget(Target);
					sig.setSignedProperties(sp);
				} catch (SignedDocException ex) {
					SAXSignedDocException.handleException(ex);
				}
			}
			m_nCollectMode++;
			m_sbCollectChars = new StringBuffer(2048);
		}
		// <Signature>
		if (qName.equals("Signature") && m_nCollectMode == 0) {
			//if (m_logger.isDebugEnabled())
			m_aLogger.debug("Start collecting <Signature>");
			String str1 = attrs.getValue("Id");
			Signature sig = getLastSignature();
			if (sig == null || !sig.getId().equals(str1)) {
				//if (m_logger.isDebugEnabled())
				m_aLogger.debug("Create signature: " + str1);
				sig = new Signature(m_doc);
				m_doc.addSignature(sig);
			}
			try {
				if (str1 != null)
					sig.setId(str1);
			} catch (SignedDocException ex) {
				SAXSignedDocException.handleException(ex);
			}
			m_sbCollectSignature = new StringBuffer();
		}
		// <SignatureValue>
		if (qName.equals("SignatureValue") && m_nCollectMode == 0) {
			m_strSigValTs = null;
			m_nCollectMode++;
			m_sbCollectChars = new StringBuffer(1024);
		}
		// <SignatureTimeStamp>
		if (qName.equals("SignatureTimeStamp") && m_nCollectMode == 0) {
			m_strSigAndRefsTs = null;
			m_nCollectMode++;
			m_sbCollectChars = new StringBuffer(2048);
		}
		// collect <Signature> data
		if (m_sbCollectSignature != null) {
			m_sbCollectSignature.append("<");
			m_sbCollectSignature.append(qName);
			for (int i = 0; i < attrs.getLength(); i++) {
				m_sbCollectSignature.append(" ");
				m_sbCollectSignature.append(attrs.getQName(i));
				m_sbCollectSignature.append("=\"");
				m_sbCollectSignature.append(attrs.getValue(i));
				m_sbCollectSignature.append("\"");
			}
			m_sbCollectSignature.append(">");
		}
		// if we just switched to collect-mode
		// collect SAX event data to original XML data
		// for <DataFile> we don't collect the begin and
		// end tags unless this an embedded <DataFile>
		if (m_nCollectMode > 0 || m_sbCollectChars != null) {
			StringBuffer sb = new StringBuffer();
			sb.append("<");
			sb.append(qName);
			for (int i = 0; i < attrs.getLength(); i++) {
				if (attrs.getQName(i).equals("xmlns"))
					m_xmlnsAttr = null; // allready have it from document
				sb.append(" ");
				sb.append(attrs.getQName(i));
				sb.append("=\"");
				sb.append(attrs.getValue(i));
				sb.append("\"");
			}
			if (m_xmlnsAttr != null) {
				sb.append(" xmlns=\"" + m_xmlnsAttr + "\"");
				m_xmlnsAttr = null;
			}
			sb.append(">");
			//canonicalize & calculate digest over DataFile begin-tag without content
			if (qName.equals("DataFile") && m_nCollectMode == 1) {
				String strCan = sb.toString() + "</DataFile>";
				strCan = canonicalizeXml(strCan);
				strCan = strCan.substring(0, strCan.length() - 11);
				//if(m_logger.isDebugEnabled())
				m_aLogger.debug("Canonicalized: \'" + strCan + "\'");
				try {
					updateDigest(ConvertUtils.str2data(strCan));
				} catch (SignedDocException ex) {
					SAXSignedDocException.handleException(ex);
				}
			} // we don't collect <DataFile> begin and end - tags and we don't collect if we use temp files 
			else {
				if (m_sbCollectChars != null)
					m_sbCollectChars.append(sb.toString());
				try {
					if (m_dfCacheOutStream != null)
						m_dfCacheOutStream.write(ConvertUtils.str2data(sb.toString()));
				} catch (IOException ex) {
					SAXSignedDocException.handleException(ex);
				} catch (SignedDocException ex) {
					SAXSignedDocException.handleException(ex);
				}
			}
		}

		// the following stuff is used also on level 1
		// because it can be part of SignedInfo or SignedProperties
		if (m_nCollectMode == 1) {
			// <CanonicalizationMethod>
			if (qName.equals("CanonicalizationMethod")) {
				String Algorithm = attrs.getValue("Algorithm");
				try {
					Signature sig = getLastSignature();
					SignedInfo si = sig.getSignedInfo();
					si.setCanonicalizationMethod(Algorithm);
				} catch (SignedDocException ex) {
					SAXSignedDocException.handleException(ex);
				}
			}
			// <SignatureMethod>
			if (qName.equals("SignatureMethod")) {
				String Algorithm = attrs.getValue("Algorithm");
				try {
					Signature sig = getLastSignature();
					SignedInfo si = sig.getSignedInfo();
					si.setSignatureMethod(Algorithm);
				} catch (SignedDocException ex) {
					SAXSignedDocException.handleException(ex);
				}
			}
			// <Reference>
			if (qName.equals("Reference")) {
				String URI = attrs.getValue("URI");
				try {
					Signature sig = getLastSignature();
					SignedInfo si = sig.getSignedInfo();
					Reference ref = new Reference(si);
					ref.setUri(URI);
					si.addReference(ref);
				} catch (SignedDocException ex) {
					SAXSignedDocException.handleException(ex);
				}
			}
			// <Transform>
			if (qName.equals("Transform")) {
				String Algorithm = attrs.getValue("Algorithm");
				try {
					if (m_tags.search("Reference") != -1) {
						Signature sig = getLastSignature();
						SignedInfo si = sig.getSignedInfo();
						Reference ref = si.getLastReference();
						ref.setTransformAlgorithm(Algorithm);
					}
				} catch (SignedDocException ex) {
					SAXSignedDocException.handleException(ex);
				}
			}
			// <SignatureProductionPlace>
			if (qName.equals("SignatureProductionPlace")) {
				try {
					Signature sig = getLastSignature();
					SignedProperties sp = sig.getSignedProperties();
					SignatureProductionPlace spp = new SignatureProductionPlace();
					sp.setSignatureProductionPlace(spp);
				} catch (SignedDocException ex) {
					SAXSignedDocException.handleException(ex);
				}
			}

		}
		// the following is collected anyway independent of collect mode
		// <SignatureValue>
		if (qName.equals("SignatureValue")) {
			String Id = attrs.getValue("Id");
			try {
				SignatureValue sv = new SignatureValue();
				// VS: 2.2.24 - fix to allow SignatureValue without Id attribute
				if (Id != null)
					sv.setId(Id);
				Signature sig = getLastSignature();
				sig.setSignatureValue(sv);
			} catch (SignedDocException ex) {
				SAXSignedDocException.handleException(ex);
			}
		}
		// <DigestMethod>
		if (qName.equals("DigestMethod")) {
			String Algorithm = attrs.getValue("Algorithm");
			try {
				if (m_tags.search("Reference") != -1) {
					Signature sig = getLastSignature();
					SignedInfo si = sig.getSignedInfo();
					Reference ref = si.getLastReference();
					ref.setDigestAlgorithm(Algorithm);
				} else if (m_tags.search("SigningCertificate") != -1) {
					Signature sig = getLastSignature();
					CertID cid = sig.getOrCreateCertIdOfType(CertID.CERTID_TYPE_SIGNER);
					cid.setDigestAlgorithm(Algorithm);
				} else if (m_tags.search("CompleteCertificateRefs") != -1) {
					Signature sig = getLastSignature();
					CertID cid = sig.getLastCertId(); // initially set to unknown type !
					cid.setDigestAlgorithm(Algorithm);
				} else if (m_tags.search("CompleteRevocationRefs") != -1) {
					Signature sig = getLastSignature();
					UnsignedProperties up = sig.getUnsignedProperties();
					CompleteRevocationRefs rrefs = up.getCompleteRevocationRefs();
					rrefs.setDigestAlgorithm(Algorithm);
				}
			} catch (SignedDocException ex) {
				SAXSignedDocException.handleException(ex);
			}
		}
		// <Cert>
		if (qName.equals("Cert")) {
			String id = attrs.getValue("Id");
			try {
				Signature sig = getLastSignature();
				if (m_tags.search("SigningCertificate") != -1) {
					CertID cid = sig.getOrCreateCertIdOfType(CertID.CERTID_TYPE_SIGNER);
					if (id != null)
						cid.setId(id);
				}
				if (m_tags.search("CompleteCertificateRefs") != -1) {
					CertID cid = new CertID();
					if (id != null)
						cid.setId(id);
					sig.addCertID(cid);
				}
			} catch (SignedDocException ex) {
				SAXSignedDocException.handleException(ex);
			}
		}
		// <AllDataObjectsTimeStamp>
		if (qName.equals("AllDataObjectsTimeStamp")) {
			String id = attrs.getValue("Id");
			try {
				Signature sig = getLastSignature();
				TimestampInfo ts = new TimestampInfo(id, TimestampInfo.TIMESTAMP_TYPE_ALL_DATA_OBJECTS);
				sig.addTimestampInfo(ts);
			} catch (SignedDocException ex) {
				SAXSignedDocException.handleException(ex);
			}
		}
		// <IndividualDataObjectsTimeStamp>
		if (qName.equals("IndividualDataObjectsTimeStamp")) {
			String id = attrs.getValue("Id");
			try {
				Signature sig = getLastSignature();
				TimestampInfo ts = new TimestampInfo(id, TimestampInfo.TIMESTAMP_TYPE_INDIVIDUAL_DATA_OBJECTS);
				sig.addTimestampInfo(ts);
			} catch (SignedDocException ex) {
				SAXSignedDocException.handleException(ex);
			}
		}
		// <SignatureTimeStamp>
		if (qName.equals("SignatureTimeStamp")) {
			String id = attrs.getValue("Id");
			try {
				Signature sig = getLastSignature();
				TimestampInfo ts = new TimestampInfo(id, TimestampInfo.TIMESTAMP_TYPE_SIGNATURE);
				sig.addTimestampInfo(ts);
			} catch (SignedDocException ex) {
				SAXSignedDocException.handleException(ex);
			}
		}
		// <SigAndRefsTimeStamp>
		if (qName.equals("SigAndRefsTimeStamp")) {
			String id = attrs.getValue("Id");
			try {
				Signature sig = getLastSignature();
				TimestampInfo ts = new TimestampInfo(id, TimestampInfo.TIMESTAMP_TYPE_SIG_AND_REFS);
				sig.addTimestampInfo(ts);
			} catch (SignedDocException ex) {
				SAXSignedDocException.handleException(ex);
			}
		}
		// <RefsOnlyTimeStamp>
		if (qName.equals("RefsOnlyTimeStamp")) {
			String id = attrs.getValue("Id");
			try {
				Signature sig = getLastSignature();
				TimestampInfo ts = new TimestampInfo(id, TimestampInfo.TIMESTAMP_TYPE_REFS_ONLY);
				sig.addTimestampInfo(ts);
			} catch (SignedDocException ex) {
				SAXSignedDocException.handleException(ex);
			}
		}
		// <ArchiveTimeStamp>
		if (qName.equals("ArchiveTimeStamp")) {
			String id = attrs.getValue("Id");
			try {
				Signature sig = getLastSignature();
				TimestampInfo ts = new TimestampInfo(id, TimestampInfo.TIMESTAMP_TYPE_ARCHIVE);
				sig.addTimestampInfo(ts);
			} catch (SignedDocException ex) {
				SAXSignedDocException.handleException(ex);
			}
		}
		// <Include>
		if (qName.equals("Include")) {
			String uri = attrs.getValue("URI");
			try {
				Signature sig = getLastSignature();
				TimestampInfo ts = sig.getLastTimestampInfo();
				IncludeInfo inc = new IncludeInfo(uri);
				ts.addIncludeInfo(inc);
			} catch (SignedDocException ex) {
				SAXSignedDocException.handleException(ex);
			}
		}
		// <CompleteCertificateRefs>
		if (qName.equals("CompleteCertificateRefs")) {
			String Target = attrs.getValue("Target");
			try {
				Signature sig = getLastSignature();
				UnsignedProperties up = sig.getUnsignedProperties();
				CompleteCertificateRefs crefs = new CompleteCertificateRefs();
				up.setCompleteCertificateRefs(crefs);
				crefs.setUnsignedProperties(up);
			} catch (SignedDocException ex) {
				SAXSignedDocException.handleException(ex);
			}
		}
		// <CompleteRevocationRefs>
		if (qName.equals("CompleteRevocationRefs")) {
			try {
				Signature sig = getLastSignature();
				UnsignedProperties up = sig.getUnsignedProperties();
				CompleteRevocationRefs rrefs = new CompleteRevocationRefs();
				up.setCompleteRevocationRefs(rrefs);
				rrefs.setUnsignedProperties(up);
			} catch (SignedDocException ex) {
				SAXSignedDocException.handleException(ex);
			}
		}
		// <OCSPIdentifier>
		if (qName.equals("OCSPIdentifier")) {
			String URI = attrs.getValue("URI");
			try {
				Signature sig = getLastSignature();
				UnsignedProperties up = sig.getUnsignedProperties();
				CompleteRevocationRefs rrefs = up.getCompleteRevocationRefs();
				rrefs.setUri(URI);
			} catch (SignedDocException ex) {
				SAXSignedDocException.handleException(ex);
			}
		}

		// the following stuff is ignored in collect mode
		// because it can only be the content of a higher element
		if (m_nCollectMode == 0) {
			// <SignedDoc>
			//if (qName.equals("SignedDoc")) {
			//ROB
			//document-signatures
			if (qName.equals("document-signatures")) {
				String format = null, version = null;
				for (int i = 0; i < attrs.getLength(); i++) {
					String key = attrs.getQName(i);
					if (key.equals("format"))
						format = attrs.getValue(i);
					if (key.equals("version"))
						version = attrs.getValue(i);
				}
				try {
					//m_doc = new SignedDoc(format, version);
					m_doc = new ODFSignedDoc(m_xMCF, m_xCC, m_xDocumentStorage, ODFSignedDoc.FORMAT_ODF_XADES,
							SignedDoc.VERSION_1_3);
					
					//FIXME: BEPPEC56 check it !
					m_doc.addODFData();
				} catch (SignedDocException ex) {
					SAXSignedDocException.handleException(ex);
				}
			}
			// <Signature>
			/*if (qName.equals("Signature")) {
				String Id = attrs.getValue("Id");
				try {
					System.out.println("Creating signature: " + Id);
					Signature sig = new Signature(m_doc);
					if(Id != null)
						sig.setId(Id);
					m_doc.addSignature(sig);
				} catch (SignedDocException ex) {
					SAXSignedDocException.handleException(ex);
				}
			}*/
			// <KeyInfo>
			if (qName.equals("KeyInfo")) {
				try {
					KeyInfo ki = new KeyInfo();
					Signature sig = getLastSignature();
					sig.setKeyInfo(ki);
					ki.setSignature(sig);
				} catch (SignedDocException ex) {
					SAXSignedDocException.handleException(ex);
				}
			}
			// <UnsignedProperties>
			if (qName.equals("UnsignedProperties")) {
				String Target = attrs.getValue("Target");
				try {
					Signature sig = getLastSignature();
					UnsignedProperties up = new UnsignedProperties(sig);
					sig.setUnsignedProperties(up);
				} catch (SignedDocException ex) {
					SAXSignedDocException.handleException(ex);
				}
			}
			// <EncapsulatedOCSPValue>
			if (qName.equals("EncapsulatedOCSPValue")) {
				String Id = attrs.getValue("Id");
				try {
					Signature sig = getLastSignature();
					UnsignedProperties up = sig.getUnsignedProperties();
					Notary not = new Notary();
					not.setId(Id);
					up.setNotary(not);
				} catch (SignedDocException ex) {
					SAXSignedDocException.handleException(ex);
				}
			}
		} // if(m_nCollectMode == 0)
	}

	/**
	 * End Element handler
	 * @param namespaceURI namespace URI
	 * @param lName local name
	 * @param qName qualified name
	 */
	public void endElement(String namespaceURI, String sName, String qName) throws SAXException {
		//if(m_logger.isDebugEnabled())
//		m_aLogger.debug("End Element: " + qName + " collect: " + m_nCollectMode);
		// remove last tag from stack
		String currTag = (String) m_tags.pop();
		// collect SAX event data to original XML data
		// for <DataFile> we don't collect the begin and
		// end tags unless this an embedded <DataFile>
		StringBuffer sb = null;
		if (m_nCollectMode > 0 && (!qName.equals("DataFile") || m_nCollectMode > 1)) {
			sb = new StringBuffer();
			sb.append("</");
			sb.append(qName);
			sb.append(">");
		}
		if (m_sbCollectSignature != null) {
			m_sbCollectSignature.append("</");
			m_sbCollectSignature.append(qName);
			m_sbCollectSignature.append(">");
		}
		// if we do cache in mem
		if (m_sbCollectChars != null && sb != null)
			m_sbCollectChars.append(sb.toString());

		// </DataFile>
		if (qName.equals("DataFile")) {
			m_nCollectMode--;
			if (m_nCollectMode == 0) {
				// close DataFile cache if necessary
				DataFile df = m_doc.getLastDataFile();
				try {
					if (m_dfCacheOutStream != null) {
						if (sb != null) {
							/*if(df.getContentType().equals(DataFile.CONTENT_EMBEDDED_BASE64))
								m_dfCacheOutStream.write(Base64Util.decode(sb.toString()));
							else*/
							m_dfCacheOutStream.write(ConvertUtils.str2data(sb.toString()));
						}
						m_dfCacheOutStream.close();
						m_dfCacheOutStream = null;
					}
				} catch (IOException ex) {
					SAXSignedDocException.handleException(ex);
				} catch (SignedDocException ex) {
					SAXSignedDocException.handleException(ex);
				}

				System.out.println("End DF: " + df.getId() + " collect: " + m_nCollectMode);
				//debugWriteFile("DF-" + df.getId() + ".txt", m_sbCollectChars.toString()); 
				if (df.getContentType().equals(DataFile.CONTENT_EMBEDDED)) {
					try {
						System.out.println("Set body: " + df.getId() + " -> " + m_sbCollectChars.toString());
						if (df.getDfCacheFile() == null)
							df.setBody(ConvertUtils.str2data(sb.toString(), df.getInitialCodepage()));
						// canonicalize and calculate digest of body
						String str1 = sb.toString();
						m_sbCollectChars = null;
						// check for whitespace before first tag of body
						int idx1 = 0;
						while (Character.isWhitespace(str1.charAt(idx1)))
							idx1++;
						//idx1 = str1.indexOf('<');
						String str2 = null;
						if (idx1 > 0) {
							str2 = str1.substring(0, idx1);
							//System.out.println("prefix: \"" + str2 + "\"");
							updateDigest(str2.getBytes());
							str2 = null;
							str1 = str1.substring(idx1);
						}
						// check for whitespace after the last xml tag of body
						idx1 = str1.length() - 1;
						while (Character.isWhitespace(str1.charAt(idx1)))
							idx1--;
						//idx1 = str1.lastIndexOf('>');
						if (idx1 < str1.length() - 1) {
							str2 = str1.substring(idx1 + 1);
							//System.out.println("suffix: \"" + str2 + "\"");
							str1 = str1.substring(0, idx1 + 1);
						}
						//System.out.println("Body: \"" + str1 + "\"");
						//debugWriteFile("DF-" + df.getId() + "-body.txt", str1); 
						// canonicalized body
						String str3 = null;
						if (str1.charAt(0) == '<')
							str3 = canonicalizeXml(str1);
						else
							str3 = str1;
						//System.out.println("Canonical: \"" + str3 + "\"");
						//debugWriteFile("DF-" + df.getId() + "-can.txt", str3); 
						updateDigest(ConvertUtils.str2data(str3));

						if (str2 != null) {
							updateDigest(ConvertUtils.str2data(str2));
							str2 = null;
						}
						//calc digest over end tag
						updateDigest(ConvertUtils.str2data("</DataFile>"));
						df.setDigest(getDigest());
						System.out.println("Digest: " + df.getId() + " - " + Base64Util.encode(df.getDigest()));
						m_sbCollectChars = null; // stop collecting
						//} catch(java.io.UnsupportedEncodingException ex) {
						//SAXSignedDocException.handleException(ex);
					} catch (SignedDocException ex) {
						SAXSignedDocException.handleException(ex);
					}
				}
				if (df.getContentType().equals(DataFile.CONTENT_EMBEDDED_BASE64)) {
					try {
						// calc digest over end tag
						updateDigest("</DataFile>".getBytes());
						//System.out.println("Set digest: " + df.getId());
						df.setDigest(getDigest());
						System.out.println("Digest: " + df.getId() + " - " + Base64Util.encode(df.getDigest()));
						//System.out.println("Set body: " + df.getId());
						if (df.getDfCacheFile() == null && sb != null)
							df.setBody(ConvertUtils.str2data(sb.toString(), df.getInitialCodepage()));
						m_sbCollectChars = null; // stop collecting
					} catch (SignedDocException ex) {
						SAXSignedDocException.handleException(ex);
					}
					// this would throw away whitespace so calculate digest before it
					//df.setBody(Base64Util.decode(m_sbCollectChars.toString()));
				}
				//System.out.println("Done: " + df.getId());
				m_bCollectDigest = false;
			}
		}
		// </SignedInfo>
		if (qName.equals("SignedInfo")) {
			if (m_nCollectMode > 0)
				m_nCollectMode--;
			// calculate digest over the original
			// XML form of SignedInfo block and save it
			try {
				Signature sig = getLastSignature();
				SignedInfo si = sig.getSignedInfo();
				//debugWriteFile("SigInfo1.xml", m_sbCollectChars.toString());
				CanonicalizationFactory canFac = ConfigManager.instance().getCanonicalizationFactory();
				byte[] bCanSI = canFac.canonicalize(ConvertUtils.str2data(m_sbCollectChars.toString(), "UTF-8"),
						SignedDoc.CANONICALIZATION_METHOD_20010315);
				si.setOrigDigest(SignedDoc.digest(bCanSI));
				m_sbCollectChars = null; // stop collecting
				//debugWriteFile("SigInfo2.xml", si.toString());
			} catch (SignedDocException ex) {
				m_aLogger.severe(ex);
				SAXSignedDocException.handleException(ex);
			}

		}
		// </SignedProperties>
		if (qName.equals("SignedProperties")) {
			if (m_nCollectMode > 0)
				m_nCollectMode--;
			// calculate digest over the original
			// XML form of SignedInfo block and save it
			//debugWriteFile("SigProps-orig.xml", m_sbCollectChars.toString());
			try {
				Signature sig = getLastSignature();
				SignedProperties sp = sig.getSignedProperties();
				String sigProp = m_sbCollectChars.toString();
				//debugWriteFile("SigProp1.xml", sigProp);
				//System.out.println("SigProp1: " + sigProp.length() 
				//    + " digest: " + Base64Util.encode(SignedDoc.digest(sigProp.getBytes())));
				CanonicalizationFactory canFac = ConfigManager.instance().getCanonicalizationFactory();
				byte[] bCanProp = canFac.canonicalize(ConvertUtils.str2data(sigProp, "UTF-8"),
						SignedDoc.CANONICALIZATION_METHOD_20010315);
				//debugWriteFile("SigProp2.xml", new String(bCanProp));
				sp.setOrigDigest(SignedDoc.digest(bCanProp));
				//System.out.println("Digest: " + Base64Util.encode(SignedDoc.digest(bCanProp)));
				//System.out.println("SigProp2: " + sp.toString());
				m_sbCollectChars = null; // stop collecting
				CertID cid = sig.getCertIdOfType(CertID.CERTID_TYPE_SIGNER);
				if (cid != null) {
					/*System.out.println("CID: " + cid.getId() + " serial: " + cid.getSerial() +
							" alg: " + cid.getDigestAlgorithm() + " diglen: " +
							((cid.getDigestValue() == null) ? 0 : cid.getDigestValue().length));*/
					if (cid.getId() != null)
						sp.setCertId(cid.getId());
					else if (!sig.getSignedDoc().getVersion().equals(SignedDoc.VERSION_1_3))
						sp.setCertId(sig.getId() + "-CERTINFO");
					sp.setCertSerial(cid.getSerial());
					sp.setCertDigestAlgorithm(cid.getDigestAlgorithm());
					if (cid.getDigestValue() != null) {
						sp.setCertDigestValue(cid.getDigestValue());
					}
				}
			} catch (SignedDocException ex) {
				SAXSignedDocException.handleException(ex);
			}
		}
		// </SignatureValue>
		if (qName.equals("SignatureValue")) {
			if (m_nCollectMode > 0)
				m_nCollectMode--;
			m_strSigValTs = m_sbCollectChars.toString();
			//System.out.println("SigValTS mode: " + m_nCollectMode + "\n---\n" + m_strSigValTs + "\n---\n");			
			m_sbCollectChars = null; // stop collecting				
		}
		// </CompleteRevocationRefs>
		if (qName.equals("CompleteRevocationRefs")) {
			if (m_nCollectMode > 0)
				m_nCollectMode--;
			if (m_sbCollectChars != null)
				m_strSigAndRefsTs = m_strSigValTs + m_sbCollectChars.toString();
			//System.out.println("SigAndRefsTs mode: " + m_nCollectMode + "\n---\n" + m_strSigAndRefsTs + "\n---\n");
			m_sbCollectChars = null; // stop collecting			
		}
		// </Signature>
		if (qName.equals("Signature")) {
			if (m_nCollectMode == 0) {
				m_aLogger.debug("End collecting <Signature>");
				try {
					Signature sig = getLastSignature();
					m_aLogger.debug("Set sig content:\n---\n" + m_sbCollectSignature.toString() + "\n---\n");
					if (m_sbCollectSignature != null) {
						sig.setOrigContent(ConvertUtils.str2data(m_sbCollectSignature.toString(), "UTF-8"));
						m_aLogger.debug("SIG orig content set: " + sig.getId() + " len: "
								+ ((sig.getOrigContent() == null) ? 0 : sig.getOrigContent().length));
						//debugWriteFile("SIG-" + sig.getId() + ".txt", m_sbCollectSignature.toString()); 
						m_sbCollectSignature = null; // reset collecting
					}
				} catch (SignedDocException ex) {
					SAXSignedDocException.handleException(ex);
				}
			}
		}
		// </SignatureTimeStamp>
		if (qName.equals("SignatureTimeStamp")) {
			m_aLogger.debug("End collecting <SignatureTimeStamp>");
			try {
				Signature sig = getLastSignature();
				TimestampInfo ts = sig.getTimestampInfoOfType(TimestampInfo.TIMESTAMP_TYPE_SIGNATURE);
				if (ts != null && m_strSigValTs != null) {
					//System.out.println("SigValTS \n---\n" + m_strSigValTs + "\n---\n");
					CanonicalizationFactory canFac = ConfigManager.instance().getCanonicalizationFactory();
					byte[] bCanXml = canFac.canonicalize(ConvertUtils.str2data(m_strSigValTs, "UTF-8"),
							SignedDoc.CANONICALIZATION_METHOD_20010315);
					byte[] hash = SignedDoc.digest(bCanXml);
					//System.out.println("SigValTS hash: " + Base64Util.encode(hash));
					//debugWriteFile("SigProp2.xml", new String(bCanProp));
					ts.setHash(hash);
				}
			} catch (SignedDocException ex) {
				SAXSignedDocException.handleException(ex);
			}
		}
		// </SigAndRefsTimeStamp>
		if (qName.equals("SigAndRefsTimeStamp")) {
			//if (m_logger.isDebugEnabled())
			m_aLogger.debug("End collecting <SigAndRefsTimeStamp>");
			try {
				Signature sig = getLastSignature();
				TimestampInfo ts = sig.getTimestampInfoOfType(TimestampInfo.TIMESTAMP_TYPE_SIG_AND_REFS);
				if (ts != null && m_strSigAndRefsTs != null) {
					String canXml = "<a>" + m_strSigAndRefsTs + "</a>";
					//System.out.println("SigAndRefsTS \n---\n" + m_strSigAndRefsTs + "\n---\n");
					CanonicalizationFactory canFac = ConfigManager.instance().getCanonicalizationFactory();
					byte[] bCanXml = canFac.canonicalize(ConvertUtils.str2data(canXml, "UTF-8"),
							SignedDoc.CANONICALIZATION_METHOD_20010315);
					canXml = new String(bCanXml, "UTF-8");
					canXml = canXml.substring(3, canXml.length() - 4);
					//System.out.println("canonical \n---\n" + canXml + "\n---\n");
					//debugWriteFile("SigProp2.xml", new String(bCanProp));
					byte[] hash = SignedDoc.digest(ConvertUtils.str2data(canXml, "UTF-8"));
					//System.out.println("SigAndRefsTS hash: " + Base64Util.encode(hash));
					ts.setHash(hash);
				}
			} catch (SignedDocException ex) {
				SAXSignedDocException.handleException(ex);
			} catch (Exception ex) {
				//SAXSignedDocException.handleException(ex);
			}
		}
		// the following stuff is used also in
		// collect mode level 1 because it can be part 
		// of SignedInfo or SignedProperties
		if (m_nCollectMode == 1) {
			// </SigningTime>
			if (qName.equals("SigningTime")) {
				try {
					Signature sig = getLastSignature();
					SignedProperties sp = sig.getSignedProperties();
					sp.setSigningTime(ConvertUtils.string2date(m_sbCollectItem.toString(), m_doc));
					m_sbCollectItem = null; // stop collecting
				} catch (SignedDocException ex) {
					SAXSignedDocException.handleException(ex);
				}
			}
			// </ClaimedRole>
			if (qName.equals("ClaimedRole")) {
				Signature sig = getLastSignature();
				SignedProperties sp = sig.getSignedProperties();
				sp.addClaimedRole(m_sbCollectItem.toString());
				m_sbCollectItem = null; // stop collecting
			}
			// </City>
			if (qName.equals("City")) {
				Signature sig = getLastSignature();
				SignedProperties sp = sig.getSignedProperties();
				SignatureProductionPlace spp = sp.getSignatureProductionPlace();
				spp.setCity(m_sbCollectItem.toString());
				m_sbCollectItem = null; // stop collecting
			}
			// </StateOrProvince>
			if (qName.equals("StateOrProvince")) {
				Signature sig = getLastSignature();
				SignedProperties sp = sig.getSignedProperties();
				SignatureProductionPlace spp = sp.getSignatureProductionPlace();
				spp.setStateOrProvince(m_sbCollectItem.toString());
				m_sbCollectItem = null; // stop collecting
			}
			// </CountryName>
			if (qName.equals("CountryName")) {
				Signature sig = getLastSignature();
				SignedProperties sp = sig.getSignedProperties();
				SignatureProductionPlace spp = sp.getSignatureProductionPlace();
				spp.setCountryName(m_sbCollectItem.toString());
				m_sbCollectItem = null; // stop collecting
			}
			// </PostalCode>
			if (qName.equals("PostalCode")) {
				Signature sig = getLastSignature();
				SignedProperties sp = sig.getSignedProperties();
				SignatureProductionPlace spp = sp.getSignatureProductionPlace();
				spp.setPostalCode(m_sbCollectItem.toString());
				m_sbCollectItem = null; // stop collecting
			}

		} // level 1	
			// the following is collected on any level
			// </DigestValue>
		if (qName.equals("DigestValue")) {
			try {
				//System.out.println("DIGEST: " + (m_sbCollectItem != null ? m_sbCollectItem.toString() : "NULL"));
				if (m_tags.search("Reference") != -1) {
					Signature sig = getLastSignature();
					SignedInfo si = sig.getSignedInfo();
					Reference ref = si.getLastReference();
					ref.setDigestValue(Base64Util.decode(m_sbCollectItem.toString()));
					m_sbCollectItem = null; // stop collecting
				} else if (m_tags.search("SigningCertificate") != -1) {
					Signature sig = getLastSignature();
					SignedProperties sp = sig.getSignedProperties();
					sp.setCertDigestValue(Base64Util.decode(m_sbCollectItem.toString()));
					CertID cid = sig.getCertIdOfType(CertID.CERTID_TYPE_SIGNER);
					if (cid != null)
						cid.setDigestValue(Base64Util.decode(m_sbCollectItem.toString()));
					m_sbCollectItem = null; // stop collecting
				} else if (m_tags.search("CompleteCertificateRefs") != -1) {
					Signature sig = getLastSignature();
					UnsignedProperties up = sig.getUnsignedProperties();
					CompleteCertificateRefs crefs = up.getCompleteCertificateRefs();
					CertID cid = crefs.getLastCertId();
					if (cid != null)
						cid.setDigestValue(Base64Util.decode(m_sbCollectItem.toString()));
					//System.out.println("CertID: " + cid.getId() + " digest: " + m_sbCollectItem.toString());
					m_sbCollectItem = null; // stop collecting
				} else if (m_tags.search("CompleteRevocationRefs") != -1) {
					Signature sig = getLastSignature();
					UnsignedProperties up = sig.getUnsignedProperties();
					CompleteRevocationRefs rrefs = up.getCompleteRevocationRefs();
					rrefs.setDigestValue(Base64Util.decode(m_sbCollectItem.toString()));
					m_sbCollectItem = null; // stop collecting
				}
			} catch (SignedDocException ex) {
				SAXSignedDocException.handleException(ex);
			}
		}
		// </IssuerSerial>
		if (qName.equals("IssuerSerial") && !m_doc.getVersion().equals(SignedDoc.VERSION_1_3)
				&& !m_doc.getVersion().equals(SignedDoc.VERSION_1_4)) {
			try {
				Signature sig = getLastSignature();
				CertID cid = sig.getLastCertId();
				if (cid != null)
					cid.setSerial(ConvertUtils.string2bigint(m_sbCollectItem.toString()));
				m_sbCollectItem = null; // stop collecting
			} catch (SignedDocException ex) {
				SAXSignedDocException.handleException(ex);
			}
		}
		// </X509SerialNumber>
		if (qName.equals("X509SerialNumber")
				&& (m_doc.getVersion().equals(SignedDoc.VERSION_1_3) || m_doc.getVersion().equals(SignedDoc.VERSION_1_4))) {
			try {
				Signature sig = getLastSignature();
				CertID cid = sig.getLastCertId();
				if (cid != null)
					cid.setSerial(ConvertUtils.string2bigint(m_sbCollectItem.toString()));
				//System.out.println("X509SerialNumber: " + cid.getSerial() + " type: " + cid.getType());
				m_sbCollectItem = null; // stop collecting
			} catch (SignedDocException ex) {
				SAXSignedDocException.handleException(ex);
			}
		}
		// </X509IssuerName>
		if (qName.equals("X509IssuerName")
				&& (m_doc.getVersion().equals(SignedDoc.VERSION_1_3) || m_doc.getVersion().equals(SignedDoc.VERSION_1_4))) {
			try {
				Signature sig = getLastSignature();
				CertID cid = sig.getLastCertId();
				String s = m_sbCollectItem.toString();
				if (cid != null)
					cid.setIssuer(s);
				//System.out.println("X509IssuerName: " + s + " type: " + cid.getType() + " nr: " + cid.getSerial());
				m_sbCollectItem = null; // stop collecting
			} catch (SignedDocException ex) {
				SAXSignedDocException.handleException(ex);
			}
		}
		//</EncapsulatedTimeStamp>
		if (qName.equals("EncapsulatedTimeStamp")) {
			try {
				Signature sig = getLastSignature();
				TimestampInfo ts = sig.getLastTimestampInfo();
				try {
					//System.out.println("\n--TS_RESP--\n" + m_sbCollectItem.toString() + "\n--TS_RESP--\n");
					ts.setTimeStampResponse(new TimeStampResponse(Base64Util.decode(m_sbCollectItem.toString())));
				} catch (TSPException ex) {
					throw new SignedDocException(SignedDocException.ERR_TIMESTAMP_RESP, "Invalid timestamp response", ex);
				} catch (IOException ex) {
					throw new SignedDocException(SignedDocException.ERR_TIMESTAMP_RESP, "Invalid timestamp response", ex);
				}
				m_sbCollectItem = null; // stop collecting
			} catch (SignedDocException ex) {
				SAXSignedDocException.handleException(ex);
			}
		}
		// </ResponderID>
		if (qName.equals("ResponderID")) {
			try {
				Signature sig = getLastSignature();
				UnsignedProperties up = sig.getUnsignedProperties();
				CompleteRevocationRefs rrefs = up.getCompleteRevocationRefs();
				rrefs.setResponderId(m_sbCollectItem.toString());
				m_sbCollectItem = null; // stop collecting
			} catch (SignedDocException ex) {
				SAXSignedDocException.handleException(ex);
			}
		}
		// </ProducedAt>
		if (qName.equals("ProducedAt")) {
			try {
				Signature sig = getLastSignature();
				UnsignedProperties up = sig.getUnsignedProperties();
				CompleteRevocationRefs rrefs = up.getCompleteRevocationRefs();
				rrefs.setProducedAt(ConvertUtils.string2date(m_sbCollectItem.toString(), m_doc));
				m_sbCollectItem = null; // stop collecting
			} catch (SignedDocException ex) {
				SAXSignedDocException.handleException(ex);
			}
		}

		// the following stuff is ignored in collect mode
		// because it can only be the content of a higher element
		//if (m_nCollectMode == 0) {
		// </SignatureValue>
		if (qName.equals("SignatureValue")) {
			try {
				Signature sig = getLastSignature();
				SignatureValue sv = sig.getSignatureValue();
				//debugWriteFile("SigVal.txt", m_sbCollectItem.toString());
				//System.out.println("SIGVAL mode: " + m_nCollectMode + ":\n--\n" + (m_sbCollectItem != null ? m_sbCollectItem.toString() : "NULL"));
				sv.setValue(Base64Util.decode(m_sbCollectItem.toString().trim()));
				m_sbCollectItem = null; // stop collecting
			} catch (SignedDocException ex) {
				SAXSignedDocException.handleException(ex);
			}
		}
		// </X509Certificate>
		if (qName.equals("X509Certificate")) {
			try {
				Signature sig = getLastSignature();
				CertValue cval = sig.getLastCertValue();
				cval.setCert(SignedDoc.readCertificate(Base64Util.decode(m_sbCollectItem.toString())));
				m_sbCollectItem = null; // stop collecting
			} catch (SignedDocException ex) {
				SAXSignedDocException.handleException(ex);
			}
		}
		// </EncapsulatedX509Certificate>
		if (qName.equals("EncapsulatedX509Certificate")) {
			try {
				Signature sig = getLastSignature();
				CertValue cval = sig.getLastCertValue();
				cval.setCert(SignedDoc.readCertificate(Base64Util.decode(m_sbCollectItem.toString())));
				m_sbCollectItem = null; // stop collecting
			} catch (SignedDocException ex) {
				SAXSignedDocException.handleException(ex);
			}
		}
		// </EncapsulatedOCSPValue>
		if (qName.equals("EncapsulatedOCSPValue")) {
			try {
				Signature sig = getLastSignature();
				// first we have to find correct certid and certvalue types
				findCertIDandCertValueTypes(sig);
				UnsignedProperties up = sig.getUnsignedProperties();
				Notary not = up.getNotary();
				not.setOcspResponseData(Base64Util.decode(m_sbCollectItem.toString()));
				NotaryFactory notFac = ConfigManager.instance().getNotaryFactory();
				notFac.parseAndVerifyResponse(sig, not);
				// in 1.1 we had bad OCPS digest
				if (m_doc.getVersion().equals(SignedDoc.VERSION_1_1)) {
					CompleteRevocationRefs rrefs = up.getCompleteRevocationRefs();
					rrefs.setDigestValue(SignedDoc.digest(not.getOcspResponseData()));
				}
				m_sbCollectItem = null; // stop collecting
			} catch (SignedDocException ex) {
				SAXSignedDocException.handleException(ex);
			}
		}

		//} // if(m_nCollectMode == 0)
	}

	/**
	 * SAX characters event handler
	 * @param buf received bytes array
	 * @param offset offset to the array
	 * @param len length of data
	 */
	public void characters(char buf[], int offset, int len) throws SAXException {
		String s = new String(buf, offset, len);
		//System.out.println("Chars: " + s);
		// just collect the data since it could
		// be on many lines and be processed in many events
		if (s != null) {
			if (m_sbCollectItem != null)
				m_sbCollectItem.append(s);
			if (m_sbCollectChars != null)
				m_sbCollectChars.append(s);
			if (m_sbCollectSignature != null)
				m_sbCollectSignature.append(s);
			if (m_digest != null && m_bCollectDigest)
				updateDigest(s.getBytes());
			try {
				if (m_dfCacheOutStream != null) {
					/*DataFile df = m_doc.getLastDataFile();
					if(df != null && df.getContentType().equals(DataFile.CONTENT_EMBEDDED_BASE64))
						m_dfCacheOutStream.write(Base64Util.decode(s));
					else*/
					m_dfCacheOutStream.write(ConvertUtils.str2data(s));
				}
			} catch (SignedDocException ex) {
				SAXSignedDocException.handleException(ex);
			} catch (IOException ex) {
				SAXSignedDocException.handleException(ex);
			}
		}
	}

}
