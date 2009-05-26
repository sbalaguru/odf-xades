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

import it.plio.ext.oxsit.logging.IDynamicLogger;
import it.plio.ext.oxsit.ooo.GlobConstant;

import java.util.HashMap;
import java.util.Vector;

import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.x509.X509Name;

import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.uno.XComponentContext;

/**
 *  This service implements the X509Certificate service.
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
public class X509CertDisplaySubject_IT extends X509CertDisplayBase_IT //help class, implements XTypeProvider, XInterface, XWeak
			implements 
			XServiceInfo
			 {

	// the name of the class implementing this object
	public static final String			m_sImplementationName	= X509CertDisplaySubject_IT.class.getName();

	// the Object name, used to instantiate it inside the OOo API
	public static final String[]		m_sServiceNames			= { GlobConstant.m_sX509_CERTIFICATE_DISPLAY_SERVICE_SUBJ_IT };

	/**
	 * 
	 * 
	 * @param _ctx
	 */
	public X509CertDisplaySubject_IT(XComponentContext _ctx) {
		super(_ctx);
	}

	@Override
	public String getImplementationName() {
//		m_aLoggerDialog.entering("getImplementationName");
		return m_sImplementationName;
	}

	/* (non-Javadoc)
	 * @see com.sun.star.lang.XServiceInfo#getSupportedServiceNames()
	 */
	@Override
	public String[] getSupportedServiceNames() {
//		m_aLoggerDialog.info("getSupportedServiceNames");
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
		DERObjectIdentifier oix; 
		if(m_sIssuerDisplayName.length() == 0) {
			//check for O
			oix = (DERObjectIdentifier)(X509Name.DefaultLookUp.get("o")); 
			if(hm.containsKey(oix)) {
				m_sIssuerDisplayName = hm.get(oix).toString();
			}
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
