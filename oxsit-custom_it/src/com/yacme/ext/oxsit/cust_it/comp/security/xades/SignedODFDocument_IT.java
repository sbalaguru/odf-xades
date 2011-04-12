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
 * The Original Code is oxsit-custom_it/src/com/yacme/ext/oxsit/cust_it/comp/security/SignedODFDocument_IT.java.
 *
 * The Initial Developer of the Original Code is
 * AUTHOR:  Veiko Sinivee, S|E|B IT Partner Estonia
 * Copyright (C) AS Sertifitseerimiskeskus
 * from which ideas and part of the code are derived
 * 
 * Portions created by the Initial Developer are Copyright (C) 2009-2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Giuseppe Castagno giuseppe.castagno@acca-esse.it
 * FIXME add Rob e-mail
 * Roberto Resoli 
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

package com.yacme.ext.oxsit.cust_it.comp.security.xades;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.UUID;

import javax.crypto.Cipher;

import com.yacme.ext.oxsit.cust_it.comp.security.xades.utils.ConfigManager;

/**
 * @author beppe
 * @Class this class encapsulate the current ODF Document being signed
 * 
 * FIXME: check if it can be used for the document being checked.
 * FIXME: we need this encapsulation for newly opened document as well, need to see how it can be done !
 * 
 *
 */
public class SignedODFDocument_IT {
    /** digidoc format */
    private String m_format;
    /** format version */
    private String m_version;
    /** DataFile_IT objects */
    private ArrayList m_dataFiles;
    /** SignatureXADES_IT objects */
    private ArrayList m_signatures;
    
    /** the only supported formats are SK-XML and DIGIDOC-XML */
    public static final String FORMAT_SK_XML = "SK-XML";
    public static final String FORMAT_DIGIDOC_XML = "DIGIDOC-XML";
    //ROB
	public static final String FORMAT_ODF_XADES = "urn:oasis:names:tc:opendocument:xmlns:digitalsignature:1.0";
    
    /** supported versions are 1.0 and 1.1 */
    public static final String VERSION_1_0 = "1.0";
    public static final String VERSION_1_1 = "1.1";
    public static final String VERSION_1_2 = "1.2";
    public static final String VERSION_1_3 = "1.3";
    public static final String VERSION_1_4 = "1.4";
    /** the only supported algorithm is SHA1 */
    public static final String SHA1_DIGEST_ALGORITHM = "http://www.w3.org/2000/09/xmldsig#sha1";
    /** SHA1 digest data is always 20 bytes */
    public static final int SHA1_DIGEST_LENGTH = 20;
    /** the only supported canonicalization method is 20010315 */
    public static final String CANONICALIZATION_METHOD_20010315 = "http://www.w3.org/TR/2001/REC-xml-c14n-20010315";
    /** the only supported signature method is RSA-SHA1 */
    public static final String RSA_SHA1_SIGNATURE_METHOD = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";
    /** the only supported transform is digidoc detatched transform */
    public static final String DIGIDOC_DETATCHED_TRANSFORM = "http://www.sk.ee/2002/10/digidoc#detatched-document-signature";
    /** XML-DSIG namespace */
    public static String xmlns_xmldsig = "http://www.w3.org/2000/09/xmldsig#";
	/** ETSI namespace */
	public static String xmlns_etsi = "http://uri.etsi.org/01903/v1.1.1#";
	/** DigiDoc namespace */
	public static String xmlns_digidoc = "http://www.sk.ee/DigiDoc/v1.3.0#";
	/** program & library name */
	public static final String LIB_NAME = "JDigiDoc";
	/** program & library version */
	public static final String LIB_VERSION = "2.3.29";

	
    /** 
     * Creates new SignedDoc 
     * Initializes everything to null
     */
    public SignedODFDocument_IT() {
        m_format = null;
        m_version = null;
        m_dataFiles = null;
        m_signatures = null;
    }
    
    /** 
     * Creates new SignedDoc 
     * @param format file format name
     * @param version file version number
     * @throws SignedODFDocumentException_IT for validation errors
     */
    public SignedODFDocument_IT(String format, String version) 
        throws SignedODFDocumentException_IT
    {
        setFormat(format);
        setVersion(version);
        m_dataFiles = null;
        m_signatures = null;
    }

    /**
     * Accessor for version attribute
     * @return value of version attribute
     */
    public String getVersion() {
        return m_version;
    }
    
    /**
     * Mutator for version attribute
     * @param str new value for version attribute
     * @throws SignedODFDocumentException_IT for validation errors
     */    
    public void setVersion(String str) 
        throws SignedODFDocumentException_IT
    {
    	SignedODFDocumentException_IT ex = validateVersion(str);
        if(ex != null)
            throw ex;
        m_version = str;
    }
    /**
     * Accessor for format attribute
     * @return value of format attribute
     */
    public String getFormat() {
        return m_format;
    }
    
    /**
     * Mutator for format attribute
     * @param str new value for format attribute
     * @throws SignedODFDocumentException_IT for validation errors
     */    
    public void setFormat(String str) 
        throws SignedODFDocumentException_IT
    {
    	SignedODFDocumentException_IT ex = validateFormat(str);
        if(ex != null)
            throw ex;
        m_format = str;
    }  
    
    /**
     * Helper method to validate a version
     * @param str input data
     * @return exception or null for ok
     */
    private SignedODFDocumentException_IT validateVersion(String str)
    {
    	//ROB
    	SignedODFDocumentException_IT ex = null;
        if(str == null || 
          (!str.equals(VERSION_1_0) && !str.equals(VERSION_1_1) && 
           !str.equals(VERSION_1_2) && !str.equals(VERSION_1_3) &&
		   !str.equals(VERSION_1_4)) ||
          (str.equals(VERSION_1_0) && m_format != null && !m_format.equals(FORMAT_SK_XML)) ||
          ((str.equals(VERSION_1_1) || str.equals(VERSION_1_2) || 
          	str.equals(VERSION_1_3) || str.equals(VERSION_1_4)) 
            && m_format != null && !m_format.equals(FORMAT_DIGIDOC_XML) && !m_format.equals(FORMAT_ODF_XADES))) 
            ex = new SignedODFDocumentException_IT(SignedODFDocumentException_IT.ERR_DIGIDOC_VERSION, 
                "Currently supports only versions 1.0, 1.1, 1.2, 1.3 and 1.4", null);
        return ex;
    }

    /****************************************************
     * methods from /JDigiDoc.old/src/it/plio/ext/oxsit/signature/test/ODFXadesSignedDoc.java
     * written by ROB. 
     * 
     */
	/**
	 * Helper method to validate a format
	 * 
	 * @param str
	 *            input data
	 * @return exception or null for ok
	 */
	private SignedODFDocumentException_IT validateFormat(String str) {
		SignedODFDocumentException_IT ex = null;
		if (str == null || (!str.equals(FORMAT_ODF_XADES)))
			ex = new SignedODFDocumentException_IT(SignedODFDocumentException_IT.ERR_DIGIDOC_FORMAT,
					"Currently supports only ODF_XADES format", null);
		return ex;
	}

	/**
	 * Writes the SignedDoc to an output file and automatically calculates
	 * DataFile_IT sizes and digests
	 * 
	 * @param outputFile
	 *            output file name
	 * @throws SignedODFDocumentException_IT
	 *             for all errors
	 */
	public void writeToStream(OutputStream os) throws SignedODFDocumentException_IT {
		// TODO read DataFile_IT elements from old file

		try {
			os.write(xmlHeader().getBytes());
			//ROB: no xml output for ExternalDataFile
			/*
			for (int i = 0; i < countDataFiles(); i++) {
				DataFile_IT df = getDataFile(i);
				df.writeToFile(os);
				os.write("\n".getBytes());
			}
			*/
			for (int i = 0; i < countSignatures(); i++) {
				SignatureXADES_IT sig = getSignature(i);
				os.write(sig.toXML());
				os.write("\n".getBytes());
			}
			os.write(xmlTrailer().getBytes());
		} catch (SignedODFDocumentException_IT ex) {
			throw ex; // already handled
		} catch (Exception ex) {
			SignedODFDocumentException_IT.handleException(ex,
					SignedODFDocumentException_IT.ERR_WRITE_FILE);
		}
	}
	
	
	//ROB: From uji
	//FIXME BeppeC: this need to be modified and adapted to access
	//the currently active ODF file using OOo API
//	public byte[] addODFData(ODFDocument odf) throws SignedODFDocumentException_IT {
//		
//		byte[] manifestBytes = null;
//		
//		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
//				.newInstance();
//		documentBuilderFactory.setNamespaceAware(true);
//		DocumentBuilder documentBuilder;
//		try {
//			documentBuilder = documentBuilderFactory.newDocumentBuilder();
//
//			// Acceso al manifest.xml y a la lista de elementos que contiene
//			InputStream manifest = new ByteArrayInputStream(odf
//					.getEntry("META-INF/manifest.xml"));
//
//			Document docManifest = documentBuilder.parse(manifest);
//			Element rootManifest = docManifest.getDocumentElement();
//			NodeList listFileEntry = rootManifest
//					.getElementsByTagName("manifest:file-entry");
//
//			for (int i = 0; i < listFileEntry.getLength(); i++) {
//				Element e = ((Element) listFileEntry.item(i));
//
//				String fullPath = e.getAttribute("manifest:full-path");
//				String mediaType = e.getAttribute("manifest:media-type");
//
//				// Solo procesamos los ficheros
//				if (!fullPath.endsWith("/")
//						&& !fullPath.equals("META-INF/documentsignatures.xml")) {
//					if ((odf.getEntry(fullPath).length != 0)
//							&& (fullPath.equals("manifest.rdf") || fullPath
//									.endsWith(".xml"))) {
//						// Obtenemos el fichero, canonizamos y calculamos el
//						// digest
//						InputStream xmlFile = new ByteArrayInputStream(odf
//								.getEntry(fullPath));
//
//						ExternalDataFile df = new ExternalDataFile(xmlFile,
//								fullPath, mediaType, fullPath,
//								ExternalDataFile.CONTENT_ODF_PKG_XML_ENTRY,
//								this);
//						addDataFile(df);
//
//					} else {
//
//						InputStream binaryStream = new ByteArrayInputStream(odf
//								.getEntry(fullPath));
//						ExternalDataFile df = new ExternalDataFile(binaryStream,
//								fullPath, mediaType, fullPath,
//								ExternalDataFile.CONTENT_ODF_PKG_BINARY_ENTRY,
//								this);
//						addDataFile(df);
//
//					}
//
//				}
//			}
//			// ROB: mimetype
//			if (odf.hasEntry("mimetype")) {
//
//				InputStream xmlStream = new ByteArrayInputStream(odf
//						.getEntry("mimetype"));
//				ExternalDataFile df = new ExternalDataFile(xmlStream, "mimetype",
//						"text/text", "mimetype",
//						ExternalDataFile.CONTENT_ODF_PKG_BINARY_ENTRY, this);
//				addDataFile(df);
//			}
//
//			// ROB creazione del data file per manifest.xml aggiornato
//			// Añadimos el fichero de firma al manifest.xml
//			// Aggiungiamo a manifest.xml l'entry per documensignatures.xml
//			Element nodeDocumentSignatures = docManifest
//					.createElement("manifest:file-entry");
//			nodeDocumentSignatures.setAttribute("manifest:media-type", "");
//			nodeDocumentSignatures.setAttribute("manifest:full-path",
//					"META-INF/xadessignatures.xml");
//			rootManifest.appendChild(nodeDocumentSignatures);
//
//			Element nodeMetaInf = docManifest
//					.createElement("manifest:file-entry");
//			nodeMetaInf.setAttribute("manifest:media-type", "");
//			nodeMetaInf.setAttribute("manifest:full-path", "META-INF/");
//			rootManifest.appendChild(nodeMetaInf);
//			
//			ByteArrayOutputStream manifestOs = new ByteArrayOutputStream();
//			writeXML(manifestOs, rootManifest, false);
//			manifestBytes = manifestOs.toByteArray();
//			ByteArrayInputStream manifestIs = new ByteArrayInputStream(manifestBytes);
//			
//			ExternalDataFile df = new ExternalDataFile(manifestIs, "META-INF/manifest.xml",
//					"text/text", "META-INF/manifest.xml",
//					ExternalDataFile.CONTENT_ODF_PKG_XML_ENTRY, this);
//			addDataFile(df);
//			
//			
//			
//
//		} catch (ParserConfigurationException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (SAXException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (TransformerFactoryConfigurationError e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (TransformerException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		return manifestBytes;
//	}


	/**
	 * return a new available SignatureXADES_IT id
	 * 
	 * @return new SignatureXADES_IT id
	 */
	public String getNewSignatureId() {
		String id = "ID_"+UUID.randomUUID().toString();
		return id;
	}

	/**
	 * Adds a new uncomplete signature to signed doc
	 * 
	 * @param cert
	 *            signers certificate
	 * @param claimedRoles
	 *            signers claimed roles
	 * @param adr
	 *            signers address
	 * @return new SignatureXADES_IT object
	 */
	//FIXME BeppeC: this need to be modified and adapted to access
	//the currently active ODF file using OOo API
	public SignatureXADES_IT prepareSignature(X509Certificate cert,
			String[] claimedRoles, SignatureProductionPlace_IT adr)
			throws SignedODFDocumentException_IT {
		SignatureXADES_IT sig = new SignatureXADES_IT(this);
		sig.setId(getNewSignatureId());
		// create SignedInfo block
		SignedInfoXADES_IT si = new SignedInfoXADES_IT(sig, RSA_SHA1_SIGNATURE_METHOD,
				CANONICALIZATION_METHOD_20010315);
		// add DataFile_IT references
		for (int i = 0; i < countDataFiles(); i++) {
			DataFile_IT df = getDataFile(i);
			ReferenceXADES_IT ref = new ReferenceXADES_IT(si, df);
			ref.setUri(df.getId());
			si.addReference(ref);
		}
		// create key info
		KeyInfo_IT ki = new KeyInfo_IT(cert);
		sig.setKeyInfo(ki);
		ki.setSignature(sig);
		CertValue_IT cval = new CertValue_IT();
		cval.setType(CertValue_IT.CERTVAL_TYPE_SIGNER);
		cval.setCert(cert);
		sig.addCertValue(cval);
		CertID_IT cid = new CertID_IT(sig, cert, CertID_IT.CERTID_TYPE_SIGNER);
		sig.addCertID(cid);
		// create signed properties
		SignedPropertiesXADES_IT sp = new SignedPropertiesXADES_IT(sig, cert, claimedRoles, adr);
		sp.setId("ID_"+UUID.randomUUID().toString());

		ReferenceXADES_IT ref = new ReferenceXADES_IT(si, sp);
		ref.setUri("#"+sp.getId());
		si.addReference(ref);
		sig.setSignedInfo(si);
		sig.setSignedProperties(sp);
		addSignature(sig);
		return sig;
	}
	  
    /**
     * Adds a new Signature object
     * @param attr Signature object to add
     */
    public void addSignature(SignatureXADES_IT sig) 
    {
        if(m_signatures == null)
            m_signatures = new ArrayList();
        m_signatures.add(sig);
    }

	/**
	 * Helper method to create the xml header
	 * 
	 * @return xml header
	 */
	private String xmlHeader() {
		StringBuffer sb = new StringBuffer(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append("<document-signatures xmlns=\"");
		sb.append(getFormat());
		sb.append("\">\n");
		return sb.toString();
	}

	/**
	 * Helper method to create the xml trailer
	 * 
	 * @return xml trailer
	 */
	private String xmlTrailer() {
		return "\n</document-signatures>";
	}

    /**
     * return the count of Signature objects
     * @return count of Signature objects
     */
    public int countSignatures()
    {
        return ((m_signatures == null) ? 0 : m_signatures.size());
    }
    
    /**
     * return the desired Signature object
     * @param idx index of the Signature object
     * @return desired Signature object
     */
    public SignatureXADES_IT getSignature(int idx) {
        return (SignatureXADES_IT)m_signatures.get(idx);
    }
    
    
    /**
     * return the desired DataFile object
     * @param idx index of the DataFile object
     * @return desired DataFile object
     */
    public DataFile_IT getDataFile(int idx) {
        return (DataFile_IT)m_dataFiles.get(idx);
    }

    /**
     * return the count of DataFile objects
     * @return count of DataFile objects
     */
    public int countDataFiles()
    {
        return ((m_dataFiles == null) ? 0 : m_dataFiles.size());
    }

	//ROB: From uji
	//FIXME: this procedure writes the signature file to ODF doc, needs to be adapted to ODF from OOo
	
//	private static void writeXML(OutputStream outStream, Node node,
//			boolean indent) throws TransformerFactoryConfigurationError,
//			TransformerException {
//		writeXML(new BufferedWriter(new OutputStreamWriter(outStream, Charset
//				.forName("UTF-8"))), node, indent);
//	}

//	private static void writeXML(Writer writer, Node node, boolean indent)
//			throws TransformerFactoryConfigurationError, TransformerException {
//		Transformer serializer = TransformerFactory.newInstance()
//				.newTransformer();
//		serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
//
//		if (indent) {
//			serializer.setOutputProperty(OutputKeys.INDENT, "yes");
//		}
//		serializer.transform(new DOMSource(node), new StreamResult(writer));
//	}
	/*********** end of methods from 	 */

    
    /**
     * Computes an SHA1 digest
     * @param data input data
     * @return SHA1 digest
     */
    public static byte[] digest(byte[] data)
        throws SignedODFDocumentException_IT 
    {
        byte[] dig = null;
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            sha.update(data);
            dig = sha.digest();
        } catch(Exception ex) {
            SignedODFDocumentException_IT.handleException(ex, SignedODFDocumentException_IT.ERR_CALCULATE_DIGEST);
        }
        return dig;
    }
    

    /**
     * Helper method to verify the whole SignedDoc object. 
     * Use this method to verify all signatures
     * @param checkDate Date on which to check the signature validity
     * @param demandConfirmation true if you demand OCSP confirmation from
     * every signature
     * @return a possibly empty list of SignedODFDocumentException_IT objects
     */
    public ArrayList verify(boolean checkDate, boolean demandConfirmation)
    {
        ArrayList errs = validate(false);
        for(int i = 0; i < countSignatures(); i++) {
            SignatureXADES_IT sig = getSignature(i);
            ArrayList e = sig.verify(this, checkDate, demandConfirmation);
            if(!e.isEmpty())
                errs.addAll(e);
        }    
        if(countSignatures() == 0) {
        	errs.add(new SignedODFDocumentException_IT(SignedODFDocumentException_IT.ERR_NOT_SIGNED, "This document is not signed!", null));
        }            
        return errs;
    }
    
    /**
     * Verifies the siganture
     * @param digest input data digest
     * @param signature signature value
     * @param cert certificate to be used on verify
     * @return true if signature verifies
     */
    public static boolean verify(byte[] digest, byte[] signature, X509Certificate cert) 
        throws SignedODFDocumentException_IT
    {
        boolean rc = false;
        try {
        	// VS - for some reason this JDK internal method sometimes failes
        	
        	//System.out.println("Verify digest: " + bin2hex(digest) +
        	//	" signature: " + Base64Util.encode(signature, 0));
        	/*
            // check keystore...
            java.security.Signature sig = 
                java.security.Signature.getInstance("SHA1withRSA");
            sig.initVerify((java.security.interfaces.RSAPublicKey)cert.getPublicKey());
            sig.update(digest);
            rc = sig.verify(signature);
            */
            Cipher cryptoEngine = Cipher.getInstance(ConfigManager.
            	instance().getProperty("DIGIDOC_VERIFY_ALGORITHM"), "BC");
        	cryptoEngine.init(Cipher.DECRYPT_MODE, cert);
        	byte[] decryptedDigestValue = cryptoEngine.doFinal(signature);
        	byte[] cdigest = new byte[digest.length];
            System.arraycopy(decryptedDigestValue, 
              	decryptedDigestValue.length - digest.length,
              	cdigest, 0, digest.length);
        	//System.out.println("Decrypted digest: \'" + bin2hex(cdigest) + "\'");
        	// now compare the digests
            rc = compareDigests(digest, cdigest); 
                       
            //System.out.println("Result: " + rc);
            if(!rc)
            	throw new SignedODFDocumentException_IT(SignedODFDocumentException_IT.ERR_VERIFY, "Invalid signature value!", null);
        } catch(SignedODFDocumentException_IT ex) {
        	throw ex; // pass it on, but check other exceptions
        } catch(Exception ex) {
        	//System.out.println("Exception: " + ex);
            SignedODFDocumentException_IT.handleException(ex, SignedODFDocumentException_IT.ERR_VERIFY);
        }
        return rc;
    }
    
    /**
     * Helper method for comparing
     * digest values
     * @param dig1 first digest value
     * @param dig2 second digest value
     * @return true if they are equal
     */
    public static boolean compareDigests(byte[] dig1, byte[] dig2)
    {
        boolean ok = (dig1 != null) && (dig2 != null) && 
            (dig1.length == dig2.length);
        for(int i = 0; ok && (i < dig1.length); i++)
            if(dig1[i] != dig2[i])
                ok = false;
        return ok;
    }

    /**
     * Helper method to validate the whole
     * SignedDoc object
     * @param bStrong flag that specifies if Id atribute value is to
     * be rigorously checked (according to digidoc format) or only
     * as required by XML-DSIG
     * @return a possibly empty list of SignedODFDocumentException_IT objects
     */
    public ArrayList validate(boolean bStrong)
    {
        ArrayList errs = new ArrayList();
        SignedODFDocumentException_IT ex = validateFormat(m_format);
        if(ex != null)
            errs.add(ex);
        ex = validateVersion(m_version);
        if(ex != null)
            errs.add(ex);
        for(int i = 0; i < countDataFiles(); i++) {
            DataFile_IT df = getDataFile(i);
            ArrayList e = df.validate(bStrong);
            if(!e.isEmpty())
                errs.addAll(e);
        }
        for(int i = 0; i < countSignatures(); i++) {
            SignatureXADES_IT sig = getSignature(i);
            ArrayList e = sig.validate();
            if(!e.isEmpty())
                errs.addAll(e);
        }                
        return errs;
    }


    /**
     * return certificate owners first name
     * @return certificate owners first name or null
     */
    public static String getSubjectFirstName(X509Certificate cert) {
        String name = null;
        String dn = cert.getSubjectDN().getName();
        int idx1 = dn.indexOf("CN=");
        if(idx1 != -1) {
            while(idx1 < dn.length()-1 && dn.charAt(idx1) != ',')
                idx1++;
            if(idx1 < dn.length()-1)
              idx1++;
            int idx2 = idx1;
            while(idx2 < dn.length()-1 && dn.charAt(idx2) != ',' && dn.charAt(idx2) != '/')
                idx2++;
            name = dn.substring(idx1, idx2);            
        }
        return name;
    }
    
    /**
     * return certificate owners last name
     * @return certificate owners last name or null
     */
    public static String getSubjectLastName(X509Certificate cert) {
        String name = null;
        String dn = cert.getSubjectDN().getName();
        int idx1 = dn.indexOf("CN=");
        if(idx1 != -1) {
            idx1 += 2;
            while(idx1 < dn.length()-1 && !Character.isLetter(dn.charAt(idx1)))
                idx1++;
            int idx2 = idx1;
            while(idx2 < dn.length()-1 && dn.charAt(idx2) != ',' && dn.charAt(idx2) != '/')
                idx2++;
            name = dn.substring(idx1, idx2);            
        }
        return name;
    }
    
    /**
     * return certificate owners personal code
     * @return certificate owners personal code or null
     */
    public static String getSubjectPersonalCode(X509Certificate cert) {
        String code = null;
        String dn = cert.getSubjectDN().getName();
        int idx1 = dn.indexOf("CN=");
        //System.out.println("DN: " + dn);
        if(idx1 != -1) {
            while(idx1 < dn.length()-1 && !Character.isDigit(dn.charAt(idx1)))
                idx1++;
            int idx2 = idx1;
            while(idx2 < dn.length()-1 && Character.isDigit(dn.charAt(idx2)))
                idx2++;
            code = dn.substring(idx1, idx2);            
        }
        //System.out.println("Code: " + code);
        return code;
    }
    
    // VS: 02.01.2009 - fix finding ocsp responders cert
    /**
     * return certificate's fingerprint
     * @param cert X509Certificate object
     * @return certificate's fingerprint or null
     */
    public static byte[] getCertFingerprint(X509Certificate cert) {
    	byte[] bdat = cert.getExtensionValue("2.5.29.14");
        if(bdat != null) {
          if(bdat.length > 20) {
        	  byte[] bdat2 = new byte[20];
        	  System.arraycopy(bdat, bdat.length - 20, bdat2, 0, 20);
        	  return bdat2;
          } else
        	  return bdat;
        }
        //System.out.println("Code: " + code);
        return null;
    }
    // VS: 02.01.2009 - fix finding ocsp responders cert
        
    /**
     * return CN part of DN
     * @return CN part of DN or null
     */
    public static String getCommonName(String dn) {
        String name = null;
        if(dn != null) {
        	int idx1 = dn.indexOf("CN=");
        	if(idx1 != -1) {
            	idx1 += 2;
            	while(idx1 < dn.length() && 
            		!Character.isLetter(dn.charAt(idx1)))
                	idx1++;
            	int idx2 = idx1;
            	while(idx2 < dn.length() && 
            		dn.charAt(idx2) != ',' && 
            		dn.charAt(idx2) != '/')
                	idx2++;
            	name = dn.substring(idx1, idx2);            
        	}
        }
        return name;
    }
    
    /**
     * Reads X509 certificate from a data stream
     * @param data input data in Base64 form
     * @return X509Certificate object
     * @throws EFormException for all errors
     */
    public static X509Certificate readCertificate(byte[] data)
        throws SignedODFDocumentException_IT 
    {
        X509Certificate cert = null;
        try {
        //ByteArrayInputStream certStream = new ByteArrayInputStream(Base64Util.decode(data));
        ByteArrayInputStream certStream = new ByteArrayInputStream(data);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        cert = (X509Certificate)cf.generateCertificate(certStream);
        certStream.close();
        } catch(Exception ex) {
            SignedODFDocumentException_IT.handleException(ex, SignedODFDocumentException_IT.ERR_READ_CERT);
        }
        return cert;        
    }    

    /**
     * Reads in data file
     * @param inFile input file
     */
    public static byte[] readFile(File inFile)
        throws IOException, FileNotFoundException
    {
        byte[] data = null;
        FileInputStream is = new FileInputStream(inFile);
        DataInputStream dis = new DataInputStream(is);
        data = new byte[dis.available()];
        dis.readFully(data);
        dis.close();
        is.close();
        return data;
    }

}


