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

package it.plio.ext.oxsit.ooo.cert;

import it.plio.ext.oxsit.logging.XDynamicLogger;
import it.plio.ext.oxsit.ooo.GlobConstant;

import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import com.sun.star.beans.Property;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XProperty;
import com.sun.star.beans.XPropertyAccess;
import com.sun.star.beans.XPropertySetInfo;
import com.sun.star.container.ElementExistException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XEventListener;
import com.sun.star.lang.XInitialization;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XTypeProvider;
import com.sun.star.lib.uno.helper.WeakAdapter;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.uno.Any;
import com.sun.star.uno.Exception;
import com.sun.star.uno.Type;
import com.sun.star.uno.XAdapter;
import com.sun.star.uno.XComponentContext;
import com.sun.star.uno.XInterface;
import com.sun.star.uno.XWeak;
import com.sun.star.util.XChangesListener;
import com.sun.star.util.XChangesNotifier;

/**
 * THis is a specification, it may change!
 * This service implements the DocumentSignatures service.
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
public class DocumentSignatures //extends WeakBase //help class, implements XTypeProvider, XInterface, XWeak
			implements 
			XWeak,
			XTypeProvider,
			XServiceInfo,
			XComponent,
			XInterface,
			XProperty,
			XPropertyAccess,
			XPropertySetInfo,
			XChangesNotifier,
			XInitialization,
			XNameContainer,
			XOXDocumentSignatures
			 {

	// the name of the class implementing this object
	public static final String			m_sImplementationName	= DocumentSignatures.class.getName();

	// the Object name, used to instantiate it inside the OOo API
	public static final String[]		m_sServiceNames			= { GlobConstant.m_sDOCUMENT_SIGNATURES_SERVICE };

	protected XDynamicLogger m_logger;

	//for XWeak
    private WeakAdapter m_adapter;
	
	//used for methods ox XTypeProvider
	protected static Map<Class, byte[]> _mapImplementationIds= new Hashtable<Class, byte[]>();
	protected static Map<Class, Type[]> _mapTypes= new Hashtable<Class, Type[]>();

	public static	String m_sProperties[] = {"SelfObject","DataInstance"}; 
	private class DocumentDescriptor {
		public String	sURLHash; // the hash computed from the URL name class
		public String	sURL;   //the corresponding URL
		public int		nXAdESSignatureState; // state of the signature(s) in this frame
		// these are the lister on changes on this element only
		// called only if the signature changes state.
		public HashMap<XChangesListener,XChangesListener> listeners;
	};

	private HashMap<String, DocumentDescriptor>	theDocumentList = new HashMap<String, DocumentDescriptor>(10);
	// these are the lister on changes of all the variables
	public HashMap<XChangesListener,XChangesListener> listeners;

	/**
	 * 
	 * 
	 * @param _ctx
	 */
	public DocumentSignatures(XComponentContext _ctx) {
		m_logger = new XDynamicLogger(this, _ctx);
    	m_logger.enableLogging();
    	m_logger.ctor();
	}

	public String getImplementationName() {
		// TODO Auto-generated method stub
		m_logger.entering("getImplementationName");
		return m_sImplementationName;
	}

	
	/* (non-Javadoc)
	 * @see com.sun.star.uno.XWeak#queryAdapter()
	 */
	@Override
	public XAdapter queryAdapter() {		
		if (m_adapter == null)
			m_adapter= new WeakAdapter(this);
		return m_adapter;
	}

	/* (non-Javadoc)
	 * @see com.sun.star.lang.XTypeProvider#getImplementationId()
	 */
	@Override
	public byte[] getImplementationId() {
		// TODO Auto-generated method stub
        byte[] id= null;
        synchronized (_mapImplementationIds) {
        	id= _mapImplementationIds.get(getClass());
        	if (id == null) {
        		int hash = hashCode();
        		String sName= getClass().getName();
        		byte[] arName= sName.getBytes();
        		int nNameLength= arName.length;

        		id= new byte[ 4 + nNameLength];
        		id[0]= (byte)(hash & 0xff);
        		id[1]= (byte)((hash >>> 8) & 0xff);
        		id[2]= (byte)((hash >>> 16) & 0xff);
        		id[3]= (byte)((hash >>>24) & 0xff);
        		for (int i= 0; i < nNameLength; i++) {
        			id[4 + i]= arName[i];
        		}
        		_mapImplementationIds.put(getClass(), id);
        	}
        }
        return id;
	}

	/* (non-Javadoc)
	 * @see com.sun.star.lang.XTypeProvider#getTypes()
	 */
	@Override
	public Type[] getTypes() {
		m_logger.info("getTypes");
		Type[] arTypes= _mapTypes.get( getClass());
		if (arTypes == null) {
			Vector<Type> vec= new Vector<Type>();
			Class<?> currentClass= getClass();
			do {
				Class<?> interfaces[]= currentClass.getInterfaces();
				for(int i = 0; i < interfaces.length; ++ i) {
					// Test if it is a UNO interface (check if is a subinterface of XInterface)
					if (com.sun.star.uno.XInterface.class.isAssignableFrom((interfaces[i])))
						vec.add(new Type(interfaces[i]));
					else {
						m_logger.info("getTypes", interfaces[i]+" doesn't exist in UNO");
					}
				}
				// get the superclass the currentClass inherits from
				currentClass= currentClass.getSuperclass();
			} while (currentClass != null);

			Type types[]= new Type[vec.size()];
			for( int i= 0; i < types.length; i++)
				types[i]= vec.elementAt(i);
			_mapTypes.put(getClass(), types);
			arTypes= types;
		}
		return arTypes;
	}
	
	/* (non-Javadoc)
	 * @see com.sun.star.lang.XServiceInfo#getSupportedServiceNames()
	 */
	public String[] getSupportedServiceNames() {
		// TODO Auto-generated method stub
		m_logger.info("getSupportedServiceNames");
		return m_sServiceNames;
	}

	/* (non-Javadoc)
	 * @see com.sun.star.lang.XServiceInfo#supportsService(java.lang.String)
	 */
	public boolean supportsService(String _sService) {
		int len = m_sServiceNames.length;

		m_logger.info("supportsService",_sService);
		for (int i = 0; i < len; i++) {
			if (_sService.equals( m_sServiceNames[i] ))
				return true;
		}
		return false;
	}
	/* (non-Javadoc)
	 * @see com.sun.star.lang.XComponent#addEventListener(com.sun.star.lang.XEventListener)
	 */
	public void addEventListener(XEventListener arg0) {
		// TODO Auto-generated method stub
		m_logger.info("addEventListener");
	}

	/* (non-Javadoc)
	 * @see com.sun.star.lang.XComponent#dispose()
	 */
	public void dispose() {
		// TODO Auto-generated method stub
		//clean all the element created and exit
		Collection<DocumentDescriptor> cDocuDescriptors = theDocumentList.values();
		
		if(!cDocuDescriptors.isEmpty()) {
			Iterator<DocumentDescriptor> aIter = cDocuDescriptors.iterator();
			while (aIter.hasNext()) {
				DocumentDescriptor aDocDesc = aIter.next();
				aDocDesc.listeners.clear();
			}			
		}
		theDocumentList.clear();	

		m_logger.exiting("dispose","");
	}

	/* (non-Javadoc)
	 * @see com.sun.star.lang.XComponent#removeEventListener(com.sun.star.lang.XEventListener)
	 */
	public void removeEventListener(XEventListener arg0) {
		// TODO Auto-generated method stub
		m_logger.info("removeEventListener");				
	}

	public void indentify() {
//		logger.info(getHashHex());
		m_logger.info("indentify");
	}

	/* (non-Javadoc)
	 * @see com.sun.star.beans.XProperty#getAsProperty()
	 */
	public Property getAsProperty() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.sun.star.beans.XPropertyAccess#getPropertyValues()
	 */
	public PropertyValue[] getPropertyValues() {
		// TODO Auto-generated method stub
		return null;
	}

	/* 
	 * every time a property is set,  all the registered listeners are notified.
	 * Note that if a pproperty does not exists, it is added 
	 * Example:
	 * 
	 *  property.Name = String("Fuhc8222a626")
	 *  property.Value = array[] of properties [].Name "Operation"       the operation to carry out
	 *  										.Value int = 1           remove it from list
	 *                                                       2           add if not present
	 *                                                       3			 set value, if not present throws
	 *                                                       4			 set a ChangeListener
	 *                                                       5			 remove a change listener
	 *  										.Name "URL"
	 *  										.Value 'the full URL'
	 *  										.Name "XAdESSignatureState"
	 *  										.Value int, the signature status
	 *  										.Name "ChangesListener"
	 *  										.Value Obj should ne an object of the kind change listener, e.g.
	 *  													should have a XChangesLister interface implemented
	 *  													
	 *  
	 * 
	 * (non-Javadoc)
	 *  @see com.sun.star.beans.XPropertyAccess#setPropertyValues(com.sun.star.beans.PropertyValue[])
	 */
	public void setPropertyValues(PropertyValue[] aPropertyValue)
			throws UnknownPropertyException, PropertyVetoException,
			IllegalArgumentException, WrappedTargetException {
		// TODO Auto-generated method stub
		
//		if(m_bEnableLogging)
		m_logger.info("setPropertyValues: "+aPropertyValue[0].Name);
	}

	/* returns all the properties, in this case all the registered
	 * (non-Javadoc)
	 * 
	 * @see com.sun.star.beans.XPropertySetInfo#getProperties()
	 */
	public Property[] getProperties() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * return a single property or throws
	 * 
	 * in case of a Document add-on data (e.g. the signature status)
	 * the property is the document URL hash computed as a key. E.g. the URL
	 * becomes someting as: Fuhc8222a626
	 * This is the name name of the frame
	 * the value is a sequence of property containing the corresponding data to be
	 * set.
	 * Example:
	 * 
	 *  property.Name = String("Fuhc8222a626")
	 *  property.Value = array[] of properties [].Name "Operation"            the operation to carry out
	 *  										.Value int = 1           retrieve the value
	 *                                                       2
	 *                                                       3
	 *                                                       4
	 *  										.Name "URL"
	 *  										.Value 'the full URL'
	 *  										.Name "XAdESSignatureState"
	 *  										.Value int, the signature status
	 *  
	 *  
	 *  The properties in the sequence can be in any order.
	 * 
	 * 
	 * @see com.sun.star.beans.XPropertySetInfo#getPropertyByName(java.lang.String)
	 */
	public Property getPropertyByName(String arg0)
			throws UnknownPropertyException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.sun.star.beans.XPropertySetInfo#hasPropertyByName(java.lang.String)
	 */
	public boolean hasPropertyByName(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	// XChangesNotifier
	/* (non-Javadoc)
	 * @see com.sun.star.util.XChangesNotifier#addChangesListener(com.sun.star.util.XChangesListener)
	 */
	public void addChangesListener(XChangesListener _ChangesListener) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.sun.star.util.XChangesNotifier#removeChangesListener(com.sun.star.util.XChangesListener)
	 */
	public void removeChangesListener(XChangesListener _ChangesListener) {
		// TODO Auto-generated method stub

		// last of XChangesNotifier
	}

	/////////////////// XNameContainer

	/* (non-Javadoc)
	 * @see com.sun.star.container.XNameContainer#insertByName(java.lang.String, java.lang.Object)
	 */
	public void insertByName(String _aURLHash, Object _oObj)
			throws IllegalArgumentException, ElementExistException,
			WrappedTargetException {
		// TODO Auto-generated method stub
/*		if(!theDocumentList.containsKey(_aURLHash)) {
			DocumentDescriptor docuDescrip = new DocumentDescriptor();
			docuDescrip.sURLHash = _aURLHash;

			PropertyValue aVal = (PropertyValue)_oObj;
			//we recevive a series of propertyvalues
			PropertyValue[] aValues = (PropertyValue[])aVal.Value;*/
/*			for(int i= 0; i< aValues.length; i++)
				if(aValues[i].Name.compareTo(SigletonGlobalVarConstants.m_sURL_VALUE) == 0) {
					docuDescrip.sURL = (String)aValues[i].Value;
					break;
				}
		
			for(int i= 0; i< aValues.length; i++) {
				if(aValues[i].Name.compareTo(SigletonGlobalVarConstants.m_sXADES_SIGNATURE_STATE) == 0) {
					docuDescrip.nXAdESSignatureState = ((Integer)aValues[i].Value).intValue();
					break;
				}*/
/*			}
			docuDescrip.listeners = new HashMap<XChangesListener, XChangesListener>(10);
			theDocumentList.put(docuDescrip.sURLHash, docuDescrip);
		}
		else
			throw new ElementExistException();*/
	}

	/* (non-Javadoc)
	 * @see com.sun.star.container.XNameContainer#removeByName(java.lang.String)
	 */
	public void removeByName(String _aURLHash) throws NoSuchElementException,
			WrappedTargetException {
		// TODO Auto-generated method stub
//		m_logger.info("removeByName");
		if(theDocumentList.containsKey(_aURLHash)) {
			DocumentDescriptor docuDescrip = theDocumentList.remove(_aURLHash);
			docuDescrip.listeners.clear();
		}
		else
			throw new NoSuchElementException();
	}

	/* (non-Javadoc)
	 * @see com.sun.star.container.XNameReplace#replaceByName(java.lang.String, java.lang.Object)
	 */
	public void replaceByName(String arg0, Object arg1)
			throws IllegalArgumentException, NoSuchElementException,
			WrappedTargetException {
		// TODO Auto-generated method stub		
	}

	/* (non-Javadoc)
	 * @see com.sun.star.container.XNameAccess#getByName(java.lang.String)
	 */
	public Object getByName(String arg0) throws NoSuchElementException,
			WrappedTargetException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.sun.star.container.XNameAccess#getElementNames()
	 */
	public String[] getElementNames() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.sun.star.container.XNameAccess#hasByName(java.lang.String)
	 */
	public boolean hasByName(String _sElementName) { // this is the name of the frame
												// is the key inside the full hash list
		return theDocumentList.containsKey(_sElementName);
	}

	/* (non-Javadoc)
	 * @see com.sun.star.container.XElementAccess#getElementType()
	 */
	public Type getElementType() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.sun.star.container.XElementAccess#hasElements()
	 */
	public boolean hasElements() {
		// TODO Auto-generated method stub
// check if we have some elements onlist, returns
		m_logger.entering("hasElements");
		return false;
		//////////////// last of XNameContainer
	}

	/* (non-Javadoc)
	 * @see com.sun.star.lang.XInitialization#initialize(java.lang.Object[])
	 * when instantiated, 
	 * 	_oObj[0] first argument document URL
	 *  _oObj[1] corresponding XStorage object
	 */
	@Override
	public void initialize(Object[] _oObj) throws Exception {
		// TODO Auto-generated method stub
		m_logger.entering("initialize");		
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.ooo.cert.XOXDocumentSignatures#getDocumentURL()
	 */
	@Override
	public String getDocumentURL() {
		// TODO Auto-generated method stub
		m_logger.info("getDocumentURL");
		return null;
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.ooo.cert.XOXDocumentSignatures#setDocumentURL(java.lang.String)
	 */
	@Override
	public void setDocumentURL(String s1) {
		// TODO Auto-generated method stub
		
	}
}
