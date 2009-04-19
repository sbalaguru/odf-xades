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

package it.plio.ext.oxsit;

import it.plio.ext.oxsit.comp.options.ManageGeneralOptions;
import it.plio.ext.oxsit.comp.options.ManageLoggingOptions;
import it.plio.ext.oxsit.ooo.cert.DocumentSignatures;

import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.registry.XRegistryKey;

public class RegisterServices {
	/** Gives a factory for creating the service(s).
	 * This method is called by the <code>JavaLoader</code>
	 * <p>
	 * @return Returns a <code>XSingleServiceFactory</code> for creating the
	 * component.
	 * @see com.sun.star.comp.loader.JavaLoader#
	 * @param stringImplementationName The implementation name of the component.
	 * @param xmultiservicefactory The service manager, who gives access to every
	 * known service.
	 */
	public synchronized static XSingleComponentFactory __getComponentFactory( String sImplementationName ) {
		XSingleComponentFactory xFactory = null;
	
		if ( sImplementationName.equals( ManageGeneralOptions.m_sImplementationName ) ) {
			xFactory = Factory.createComponentFactory( ManageGeneralOptions.class, ManageGeneralOptions.m_sServiceNames );
//DEBUG			System.out.println("__getComponentFactory: "+ManageGeneralOptions.m_sImplementationName);
		}
		else if ( sImplementationName.equals( ManageLoggingOptions.m_sImplementationName ) ) {
			xFactory = Factory.createComponentFactory( ManageLoggingOptions.class, ManageLoggingOptions.m_sServiceNames );
//DEBUG			System.out.println("__getComponentFactory: "+ManageLoggingOptions.m_sImplementationName);
		}
		else if ( sImplementationName.equals( DocumentSignatures.m_sImplementationName ) ) {
			xFactory = Factory.createComponentFactory( DocumentSignatures.class, DocumentSignatures.m_sServiceNames );
//DEBUG		System.out.println("__getComponentFactory: "+DocumentSignatures.m_sImplementationName);
		}
		return xFactory;
	}
	/** Writes the service information into the given registry key.
	 * This method is called by the <code>JavaLoader</code>.
	 * @return returns true if the operation succeeded
	 * @see com.sun.star.comp.loader.JavaLoader#
	 * @see com.sun.star.lib.uno.helper.Factory#
	 * @param xregistrykey Makes structural information (except regarding tree
	 * structures) of a single
	 * registry key accessible.
	 */
	public synchronized static boolean __writeRegistryServiceInfo( XRegistryKey xRegistryKey ) {
//		System.out.println("__writeRegistryServiceInfo: "+ManageOptions.m_sImplementationName+" "+ManageOptions.m_sServiceNames[0] );
		boolean retGeneral = 
			Factory.writeRegistryServiceInfo( ManageGeneralOptions.m_sImplementationName, ManageGeneralOptions.m_sServiceNames, xRegistryKey );
		boolean retLogging = 
			Factory.writeRegistryServiceInfo( ManageLoggingOptions.m_sImplementationName, ManageLoggingOptions.m_sServiceNames, xRegistryKey );

		boolean retDigitalSignatures = 
			Factory.writeRegistryServiceInfo( DocumentSignatures.m_sImplementationName, DocumentSignatures.m_sServiceNames, xRegistryKey );

		if (!retGeneral)
			System.out.println("__writeRegistryServiceInfo: "+ManageGeneralOptions.m_sImplementationName + "failed");		

		if (!retLogging)
			System.out.println("__writeRegistryServiceInfo: "+ManageLoggingOptions.m_sImplementationName + "failed");		
		
		if (!retDigitalSignatures)
			System.out.println("__writeRegistryServiceInfo: "+DocumentSignatures.m_sImplementationName + "failed");		

		if(retGeneral && retLogging && retDigitalSignatures)
			return true;
		return false;
	}
}
