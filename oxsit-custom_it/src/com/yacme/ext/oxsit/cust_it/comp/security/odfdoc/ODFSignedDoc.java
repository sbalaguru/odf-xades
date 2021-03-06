/**
 * 
 */
package com.yacme.ext.oxsit.cust_it.comp.security.odfdoc;

import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.UUID;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.xml.sax.SAXException;

import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.document.XStorageBasedDocument;
import com.sun.star.embed.ElementModes;
import com.sun.star.embed.InvalidStorageException;
import com.sun.star.embed.StorageWrappedTargetException;
import com.sun.star.embed.XStorage;
import com.sun.star.io.IOException;
import com.sun.star.io.XInputStream;
import com.sun.star.io.XStream;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XSingleServiceFactory;
import com.sun.star.packages.WrongPasswordException;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.yacme.ext.oxsit.Utilities;
import com.yacme.ext.oxsit.cust_it.ConstantCustomIT;
import com.yacme.ext.oxsit.cust_it.comp.security.ODFPackageItem;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.CertID;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.CertValue;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.DataFile;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.KeyInfo;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.Reference;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.Signature;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.SignatureProductionPlace;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.SignedDoc;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.SignedDocException;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.SignedInfo;
import com.yacme.ext.oxsit.cust_it.comp.security.xades.SignedProperties;
import com.yacme.ext.oxsit.logging.DynamicLogger;

/**
 * describes the ODF document structure
 * @author beppe
 *
 */
public class ODFSignedDoc extends SignedDoc {

	/**
	 * 
	 */

	//  public OOoServerInfo SvrInfo = new OOoServerInfo();

	private DynamicLogger m_aLogger;
	protected XComponentContext m_xCtx;
	protected XMultiComponentFactory m_xMFC;
	private XStorage m_xDocumentStorage;

	/** Creates a new instance of __NAME__ 
	 * @param version13 
	 * @param formatOdfXades 
	 * @param mXDocumentStorage */
	//  public ODFSignedDoc(XMultiComponentFactory _xMFC, XComponentContext _context, XStorage mXDocumentStorage, String formatOdfXades, String version13) {
	//  	m_xCtx = _context;
	//  	m_xMFC = _xMFC;
	//  	m_aLogger = new DynamicLogger(this, _context);
	//  	m_aLogger.enableLogging();
	//  	m_aLogger.info("ctor","");
	//  }

	/**
	 * Creates new SignedDoc
	 * 
	 * @param format
	 *            file format name
	 * @param version
	 *            file version number
	 * @throws SignedDocException
	 *             for validation errors
	 */
	public ODFSignedDoc(XMultiComponentFactory _xMFC, XComponentContext _context, XStorage _XDocumentStorage, String format,
			String version) throws SignedDocException {
		super(format, version);
		m_xCtx = _context;
		m_xMFC = _xMFC;
		m_aLogger = new DynamicLogger(this, _context);
		m_aLogger.enableLogging();
		m_aLogger.info("ctor", "");
		m_xDocumentStorage = _XDocumentStorage;
	}

	/**
	 * @param xThePackage the storage element to examine
	 * @param _List the list to be filled, or updated
	 * @param _rootElement the name of the root element of the package 'xThePackage' 
	 * @param _bRecurse if can recurse (true) or not (false)
	 */
	private void fillElementList(XStorage xThePackage, Vector<ODFPackageItem> _List, String _rootElement, boolean _bRecurse) {
		String[] aElements = xThePackage.getElementNames();
		/*		m_aLoggerDialog.info(_rootElement+" elements:");
				for(int i = 0; i < aElements.length; i++)
					m_aLoggerDialog.info("'"+aElements[i]+"'");*/
		for (int i = 0; i < aElements.length; i++) {
			if (aElements[i] != "META-INF") {
				try {
					if (xThePackage.isStreamElement(aElements[i])) {
						//try to open the element, read a few bytes, close it
						try {
							Object oObjXStreamSto = xThePackage.cloneStreamElement(aElements[i]);
							String sMediaType = "";
							int nSize = 0;
							XPropertySet xPset = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, oObjXStreamSto);
							if (xPset != null) {
								try {
									sMediaType = AnyConverter.toString(xPset.getPropertyValue("MediaType"));
								} catch (UnknownPropertyException e) {
									m_aLogger.severe("fillElementList", e);
								} catch (WrappedTargetException e) {
									m_aLogger.severe("fillElementList", e);
								}
							} else
								m_aLogger.log("properties don't exist!");
							XStream xSt = (XStream) UnoRuntime.queryInterface(XStream.class, oObjXStreamSto);
							XInputStream xI = xSt.getInputStream();
							nSize = xI.available();
							//							xI.closeInput();
							_List.add(new ODFPackageItem(_rootElement + aElements[i], sMediaType, xI, nSize));
							m_aLogger.info("element: "+_rootElement+aElements[i]);
						} catch (WrongPasswordException e) {
							// TODO Auto-generated catch block
							m_aLogger.warning("fillElementList", aElements[i] + " wrong password", e);
						}
					} else if (_bRecurse && xThePackage.isStorageElement(aElements[i])) {
						try {
							XStorage xSubStore = xThePackage.openStorageElement(aElements[i], ElementModes.READ);
							m_aLogger.info("recurse into element: "+_rootElement+aElements[i]);							
							fillElementList(xSubStore, _List, _rootElement+aElements[i]+"/", _bRecurse);
							xSubStore.dispose();
						}
						catch (IOException e) {
								m_aLogger.info("fillElementList", "the substorage "+aElements[i]+" might be locked, get the last committed version of it");
								   try {
									   Object oObj = m_xMFC.createInstanceWithContext("com.sun.star.embed.StorageFactory", m_xCtx);
									   XSingleServiceFactory xStorageFactory = (XSingleServiceFactory)UnoRuntime.queryInterface(XSingleServiceFactory.class,oObj);
									   Object oMyStorage =xStorageFactory.createInstance();
									   XStorage xAnotherSubStore = (XStorage) UnoRuntime.queryInterface( XStorage.class, oMyStorage );
									   xThePackage.copyStorageElementLastCommitTo( aElements[i], xAnotherSubStore );
									   fillElementList(xAnotherSubStore, _List,_rootElement+aElements[i]+"/", true);
									   xAnotherSubStore.dispose();						   
								   } catch (Exception e1) {
										m_aLogger.severe("fillElementList", "\""+aElements[i]+"\""+" missing", e1);
								   } // should create an empty temporary storage
						}
					}
				} catch (InvalidStorageException e) {
					m_aLogger.warning("fillElementList", aElements[i] + " missing", e);
				} catch (NoSuchElementException e) {
					m_aLogger.warning("fillElementList", aElements[i] + " missing", e);
				} catch (IllegalArgumentException e) {
					m_aLogger.warning("fillElementList", aElements[i] + " missing", e);
				} catch (StorageWrappedTargetException e) {
					m_aLogger.warning("fillElementList", aElements[i] + " missing", e);
				} catch (IOException e) {
					m_aLogger.warning("fillElementList", aElements[i] + " missing", e);
				}
			}
		}
	}

	/**
	 * closely resembles the function  DocumentSignatureHelper::CreateElementList
	 * FIXME but need to be redesigned, because of concurrent access to streams/elements 
	 * this list list only the main components, but not all the substore
	 * We need instead to check for all the available Names and check them
	 * 
	 * @param _thePackage
	 * @return
	 */
	private Vector<ODFPackageItem> makeTheElementList(Object _othePackage, XStorage _xStorage) {
		//TODO check for ODF 1.0 structure, see what to do in that case.
		Vector<ODFPackageItem> aElements = new Vector<ODFPackageItem>(20);

		//print the storage ODF version

		XStorage xThePackage;
		if (_xStorage == null) {
			xThePackage = (XStorage) UnoRuntime.queryInterface(XStorage.class, _othePackage);
			m_aLogger.info("makeTheElementList", "use the URL storage");
			Utilities.showInterfaces(this, xThePackage);
		} else {
			xThePackage = _xStorage;
			m_aLogger.info("makeTheElementList", "use the document storage");
		}

		//		Utilities.showInterfaces(this,_othePackage);
		XPropertySet xPropset = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, _othePackage);

		//this chunk of code should be at the top package level
		if (xPropset != null) { // grab the version
			String sVersion = "1.0";
			try {
				sVersion = (String) xPropset.getPropertyValue("Version");
			} catch (UnknownPropertyException e) {
				m_aLogger.warning("makeTheElementList", "Version missing", e);
				//no problem if not existent
			} catch (WrappedTargetException e) {
				m_aLogger.warning("makeTheElementList", "Version missing", e);
			}
			if (sVersion.length() != 0)
				m_aLogger.log("Version is: " + sVersion); // this should be 1.2 or more
			else
				m_aLogger.log("Version is 1.0 or 1.1");
		}
		/*		else
					m_aLogger.log("Version does not exists! May be this is not a ODF package?");*/

		//if version <1.2 then all excluding META-INF
		// else only the ones indicated
		//main contents
		fillElementList(xThePackage, aElements, "", true);

		/*    	//Thumbnails
				try {
					XStorage xSubStore = xThePackage.openStorageElement("Thumbnails", ElementModes.READ);
					fillElementList(xSubStore, aElements,"Thumbnails"+"/", true);
					xSubStore.dispose();
				}
				catch (IOException e) {
					//no problem if not existent
					m_aLoggerDialog.warning("makeTheElementList", "\"Thumbnails\" substorage missing", e);
				} catch (StorageWrappedTargetException e) {
					// TODO Auto-generated catch block
					//no problem if not existent
					m_aLoggerDialog.warning("makeTheElementList", "\"Thumbnails\" substorage missing", e);
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					//no problem if not existent
					m_aLoggerDialog.warning("makeTheElementList", "\"Thumbnails\" substorage missing", e);
				}*/

		//Basic
		/*		try {
					XStorage xSubStore = xThePackage.openStorageElement("Basic", ElementModes.READ);
					fillElementList(xSubStore, aElements,"Basic"+"/", true);
					xSubStore.dispose();
				}
				catch (IOException e) {
					//no problem if not existent
					m_aLoggerDialog.warning("makeTheElementList", "\"Basic\" substorage missing", e);
				} catch (StorageWrappedTargetException e) {
					// TODO Auto-generated catch block
					//no problem if not existent
					m_aLoggerDialog.warning("makeTheElementList", "\"Basic\" substorage missing", e);
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					//no problem if not existent
					m_aLoggerDialog.warning("makeTheElementList", "\"Basic\" substorage missing", e);
				}*/

		//Pictures
		/*		try {
					XStorage xSubStore = xThePackage.openStorageElement("Pictures", ElementModes.READ);
					fillElementList(xSubStore, aElements,"Pictures"+"/", true);
					xSubStore.dispose();
				}
				catch (IOException e) {
					//no problem if not existent
					m_aLoggerDialog.warning("makeTheElementList", "\"Pictures\" substorage missing", e);
				} catch (StorageWrappedTargetException e) {
					// TODO Auto-generated catch block
					//no problem if not existent
					m_aLoggerDialog.warning("makeTheElementList", "\"Pictures\" substorage missing", e);
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					//no problem if not existent
					m_aLoggerDialog.warning("makeTheElementList", "\"Pictures\" substorage missing", e);
				}*/

		//OLE
		String sElementName = "";

		/*		try {
					try {
						sElementName = "ObjectReplacements";
						XStorage xSubStore = xThePackage.openStorageElement(sElementName, ElementModes.READ);
						fillElementList(xSubStore, aElements,sElementName+"/", true);
						xSubStore.dispose();
					}
					catch (IOException e) {
						//no problem if not existent
						m_aLoggerDialog.warning("makeTheElementList", "\""+sElementName+"\""+" missing", e);
					} catch (StorageWrappedTargetException e) {
						// TODO Auto-generated catch block
						//no problem if not existent
						m_aLoggerDialog.warning("makeTheElementList", "\""+sElementName+"\""+" missing", e);
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						//no problem if not existent
						m_aLoggerDialog.warning("makeTheElementList", "\""+sElementName+"\""+" missing", e);
					}*/

		//Object folders
		/*			String aObjectName = new String("Object ");
					String[] aObjName = xThePackage.getElementNames();
					for(int i = 0; i < aObjName.length; i++) {
						sElementName = aObjName[i];
						if((aObjName[i].indexOf(aObjectName) != -1) && xThePackage.isStorageElement(aObjName[i]))  {
							XStorage xAnotherSubStore;
							try
							{
							   xAnotherSubStore = xThePackage.openStorageElement(aObjName[i], ElementModes.READ);
								fillElementList(xAnotherSubStore, aElements,aObjName[i]+"/", true);
								xAnotherSubStore.dispose();					
							}
							catch (IOException e)
							{
								m_aLoggerDialog.info("makeTheElementList", "the substorage "+aObjName[i]+" might be locked, get the last committed version of it");
							   try {
								   Object oObj = m_xMFC.createInstanceWithContext("com.sun.star.embed.StorageFactory", m_xCtx);
								   XSingleServiceFactory xStorageFactory = (XSingleServiceFactory)UnoRuntime.queryInterface(XSingleServiceFactory.class,oObj);
								   Object oMyStorage =xStorageFactory.createInstance();
								   xAnotherSubStore = (XStorage) UnoRuntime.queryInterface( XStorage.class, oMyStorage );
								   xThePackage.copyStorageElementLastCommitTo( aObjName[i], xAnotherSubStore );
								   fillElementList(xAnotherSubStore, aElements,aObjName[i]+"/", true);
								   xAnotherSubStore.dispose();						   
							   } catch (Exception e1) {
								// TODO Auto-generated catch block
									m_aLoggerDialog.severe("makeTheElementList", "\""+sElementName+"\""+" missing", e1);
							   } // should create an empty temporary storage
							} 					
						}
					}
				} */
		/*		catch (IOException e) {
					//no problem if not existent
					m_aLoggerDialog.severe("makeTheElementList", "\""+sElementName+"\""+" missing", e);
				} catch (StorageWrappedTargetException e) {
					// TODO Auto-generated catch block
					//no problem if not existent
					m_aLoggerDialog.warning("makeTheElementList", "\""+sElementName+" missing", e);
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					//no problem if not existent
					m_aLoggerDialog.warning("makeTheElementList", "\""+sElementName+"\""+" missing", e);
				} catch (NoSuchElementException e) {
					// TODO Auto-generated catch block
					//no problem if not existent
					m_aLoggerDialog.warning("makeTheElementList", "", e);
				}*/
		return aElements;
	}

	/**
	* @return
	 * @throws SignedDocException 
	*/
	public byte[] addODFData() throws SignedDocException {

		byte[] manifestBytes = null;

		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		DocumentBuilder documentBuilder;
		try {
			documentBuilder = documentBuilderFactory.newDocumentBuilder();

			// Acceso al manifest.xml y a la lista de elementos que contiene
			//			InputStream manifest = new ByteArrayInputStream(odf
			//					.getEntry("META-INF/manifest.xml"));
			//
			//			Document docManifest = documentBuilder.parse(manifest);
			//			Element rootManifest = docManifest.getDocumentElement();
			//			NodeList listFileEntry = rootManifest
			//					.getElementsByTagName("manifest:file-entry");

			//insert a loop to read all the stuff from the external document, using the internal
			//OOo API

			Object oObj = m_xMFC.createInstanceWithContext("com.sun.star.embed.StorageFactory", m_xCtx);
			if (oObj != null) {
//				XSingleServiceFactory xStorageFactory = (XSingleServiceFactory) UnoRuntime.queryInterface(
//						XSingleServiceFactory.class, oObj);
				/*	            Object args[]=new Object[2];
					            args[0] = aTheDocURL;
					            args[1] = ElementModes.READ;
					            Object oMyStorage = xStorageFactory.createInstanceWithArguments(args);*/

				//	            Vector<String> aElements = makeTheElementList(oMyStorage, null); // force the use of the package object from URL
				//	            Vector<String> aElements = makeTheElementList(oMyStorage, _xStorage); // use of the package object from document
				Vector<ODFPackageItem> aElements = makeTheElementList(null, m_xDocumentStorage); // use of the package object from document
				m_aLogger.log("\nThis package contains the following elements:");

				//loop to add the data to the internal object for signature
				for (int i = 0; i < aElements.size(); i++) {
					ODFPackageItem aElm = aElements.get(i);
					m_aLogger.log("Type: " + aElm.m_sMediaType + " name: " + aElm.m_stheName + " size: " + aElm.m_nSize);
					if(!aElm.m_stheName.equalsIgnoreCase(ConstantCustomIT.m_sSignatureFileName)) {
						if ((aElm.m_sMediaType.equalsIgnoreCase("text/xml") || aElm.m_stheName.endsWith(".xml")) && aElm.m_nSize != 0) {//FIXME: verify what to do in size == 0
							m_aLogger.debug(" Adding an XML file: "+aElm.m_stheName);
							//is an xml file
							ODFDataDescription df = new ODFDataDescription(aElm.m_xInputStream, aElm.m_stheName, aElm.m_sMediaType,
									aElm.m_stheName, ODFDataDescription.CONTENT_ODF_PKG_XML_ENTRY, this);
							addDataFile(df);
						} else if (aElm.m_sMediaType.length() == 0 && aElm.m_nSize != 0) {//FIXME: verify what to do in size == 0
							m_aLogger.debug(" Adding a binary file: "+aElm.m_stheName);
							ODFDataDescription df = new ODFDataDescription(aElm.m_xInputStream, aElm.m_stheName, aElm.m_sMediaType,
									aElm.m_stheName, ODFDataDescription.CONTENT_ODF_PKG_BINARY_ENTRY, this);
							addDataFile(df);
						}
					}
				}
			}

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
			//
			//						
			//						
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

		} catch (ParserConfigurationException e1) {
			m_aLogger.log(e1, true);
		} catch (IOException e) {
			m_aLogger.log(e, true);
			//		} catch (SAXException e) {
			//			// TODO Auto-generated catch block
			//			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			m_aLogger.log(e, true);
			//		} catch (TransformerException e) {
			//			// TODO Auto-generated catch block
			//			e.printStackTrace();
		} catch (SignedDocException e) {
			// TODO Auto-generated catch block
			m_aLogger.log(e, true);
			throw (e);
		} catch (Exception e) {
			m_aLogger.log(e, true);
		}

		return manifestBytes;
	}

	@Override
	public Signature prepareSignature(X509Certificate cert, String[] claimedRoles, SignatureProductionPlace adr)
	throws SignedDocException {
		return prepareSignature(cert, claimedRoles, adr, new Date());
	}

	public Signature prepareSignature(X509Certificate cert, String[] claimedRoles, SignatureProductionPlace adr, Date _signingTime)
			throws SignedDocException {
		
		Signature sig = new Signature(this);
		sig.setId(getNewSignatureId());
		m_aLogger.log("Got signature Id: "+sig.getId());
		// create SignedInfo block
		SignedInfo si = new SignedInfo(sig, RSA_SHA256_SIGNATURE_METHOD, CANONICALIZATION_METHOD_20010315);
		m_aLogger.log("After new SignedInfo");
		// add DataFile references
		for (int i = 0; i < countDataFiles(); i++) {
			DataFile df = getDataFile(i);
			Reference ref = new Reference(si, df);
			ref.setUri(df.getId());
			si.addReference(ref);
			
			m_aLogger.log(i + ") added Reference to: "+df.getFileName());
		}
		// create key info
		KeyInfo ki = new KeyInfo(cert);
		sig.setKeyInfo(ki);
		ki.setSignature(sig);
		
		m_aLogger.log("KeyInfo added");
		
		CertValue cval = new CertValue();
		cval.setType(CertValue.CERTVAL_TYPE_SIGNER);
		cval.setCert(cert);
		sig.addCertValue(cval);
		
		m_aLogger.log("CertValue added");
		
		CertID cid = new CertID(sig, cert, CertID.CERTID_TYPE_SIGNER);
		sig.addCertID(cid);
		
		m_aLogger.log("CertID added");
		
		// create signed properties
		SignedProperties sp = new SignedProperties(sig, cert, claimedRoles, adr, _signingTime);

		m_aLogger.log("SignedProperties created");
		
		//ROB: trailing "_SignedProperties triggers "Type" Attribute inclusion in Reference
		sp.setId("ID_" + UUID.randomUUID().toString() + "_SignedProperties");
		
		m_aLogger.log("Signed Properties: "+sp.getId()+ " prepared.");
		
		Reference ref = new Reference(si, sp);
		ref.setUri("#" + sp.getId());
		si.addReference(ref);
		sig.setSignedInfo(si);
		sig.setSignedProperties(sp);
		addSignature(sig);
		
		m_aLogger.log("Signature prepared");
		
		return sig;
	}

	/**
	 * return a new available Signature id
	 * 
	 * @return new Signature id
	 */
	public String getNewSignatureId() {
		String id = "ID_"+UUID.randomUUID().toString();
		return id;
	}

}
