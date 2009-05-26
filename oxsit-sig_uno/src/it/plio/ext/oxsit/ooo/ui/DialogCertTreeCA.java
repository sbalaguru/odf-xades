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

package it.plio.ext.oxsit.ooo.ui;

import it.plio.ext.oxsit.ooo.GlobConstant;
import it.plio.ext.oxsit.security.XOX_AvailableSSCDs;
import it.plio.ext.oxsit.security.cert.XOX_CertificationPathControlProcedure;
import it.plio.ext.oxsit.security.cert.XOX_X509Certificate;

import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XItemListener;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.awt.tree.XMutableTreeNode;
import com.sun.star.awt.tree.XTreeExpansionListener;
import com.sun.star.frame.XFrame;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.script.BasicErrorException;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.view.XSelectionChangeListener;

/**
 * lists the CA certificates available
 * @author beppec56
 *
 */
public class DialogCertTreeCA extends DialogCertTreeBase 
		implements	IDialogCertTreeBase,
		XActionListener, XItemListener, XTreeExpansionListener, XSelectionChangeListener
		{

	private static final String DLG_CERT_TREE = "DialogCertTreeCA";

	protected XOX_AvailableSSCDs	m_axoxAvailableSSCDs;
	
	/**
	 * Note on the display:
	 * two ways on right pane:
	 * - a six line text on a white background for generals
	 * - a multiline text for internal elements.
	 * 
	 * tree structure:
	 *  tree ()
	 *      node ()
	 *      	
	 * 
	 * @param frame
	 * @param context
	 * @param _xmcf
	 */
	public DialogCertTreeCA(XFrame frame, XComponentContext context,
			XMultiComponentFactory _xmcf) {
		super(frame, context, _xmcf);
		// TODO Auto-generated constructor stub
		fillLocalizedString();
// the next value should be read from configuration, when configuration is written...
//		CertifTreeDlgDims.setDialogSize(0, 0); //to test
		CertifTreeDlgDims.setDialogSize(310, 100, 0);
	}

	public void initialize(int _nPosX, int _nPosY ) throws BasicErrorException {
		initialize(null, _nPosX, _nPosY );
	}

	/** Inizializza la finestra di dialogo
	 *  per la visualizzazione strutturata delle firme presenti
	 * @throws BasicErrorException 
	 * 
	 */
	public void initialize(XWindowPeer _xParentWindow, int posX, int posY) throws BasicErrorException {
		m_logger.entering("initialize");
			insertButton(this,
					CertifTreeDlgDims.DS_COL_PB3(),
					CertifTreeDlgDims.DS_ROW_4(),
					CertifTreeDlgDims.dsBtnWidthCertTree(),
					m_sSelectBtn,
					m_sBtn_SelDevice,
					(short) PushButtonType.STANDARD_value, 6);
			insertButton(this,
					CertifTreeDlgDims.DS_COL_PB4(),
					CertifTreeDlgDims.DS_ROW_4(),
					CertifTreeDlgDims.dsBtnWidthCertTree(),
					m_sAddBtn,
					m_sBtn_AddCertLabel,
					(short) PushButtonType.STANDARD_value, 7);
			insertHorizontalFixedLine(
					0, 
					CertifTreeDlgDims.DLGS_BOTTOM_FL_Y(CertifTreeDlgDims.dsHeigh()), 
					CertifTreeDlgDims.dsWidth(), "");

			//must be called AFTER the local init
			super.initializeLocal(DLG_CERT_TREE, m_sDlgListCACertTitle, posX, posY);

//			XWindow xTFWindow = (XWindow) UnoRuntime.queryInterface( XWindow.class,
//					super.m_xDialogControl );
//			xTFWindow.addKeyListener( this );
//			Utilities.showControlNames(m_xDlgContainer);
//			Utilities.showNames(m_xDlgModelNameContainer);
			//instantiate the control to get the certificate list
			try {
				Object oCertPath = m_xMCF.createInstanceWithContext(GlobConstant.m_sCERTIFICATION_PATH_SERVICE_IT, m_xContext);
//object created, we can procced
				XOX_CertificationPathControlProcedure aCtl =
					(XOX_CertificationPathControlProcedure)
					UnoRuntime.queryInterface(
							XOX_CertificationPathControlProcedure.class, oCertPath);
				XComponent[] aList = aCtl.getCertificationAuthorities(m_xParentFrame);
				if(aList != null) {
					//start the status component
					
					//sort the list
					
					
					XMutableTreeNode xCertifNode = addToTreeRootHelper();
					//iterate through the list and set the element in the tree, before display
					for(int idx1=0; idx1<aList.length;idx1++) {
						XOX_X509Certificate xoxCert = (XOX_X509Certificate)
									UnoRuntime.queryInterface(XOX_X509Certificate.class, aList[idx1]);
						//perform certificate verification
						xoxCert.verifyCertificate(m_xParentFrame);
						//then add to the tree control
						addX509CertificateToTree(xCertifNode, xoxCert);
					}

				}
			} catch (Throwable e) {
				m_logger.severe(e);
			}
	}

	@Override
	public short executeDialog() throws BasicErrorException {
		return super.executeDialog();
	}

	/** called when the dialog is closing, to dispose of available certificate list
	 * 
	 */
	public void disposeElements() {
		if(m_axoxAvailableSSCDs != null) {
			XComponent xC = (XComponent)UnoRuntime.queryInterface(XComponent.class, m_axoxAvailableSSCDs);
			if(xC != null)
				xC.dispose();
		}
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.ooo.ui.IDialogCertTreeBase#addButtonPressed()
	 */
	@Override
	public void addButtonPressed() {
		// TODO Auto-generated method stub
		//add the certificate to ?? check the spec
		m_logger.info("cambio stato certificato");
//		addOneSignature();		
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.ooo.ui.IDialogCertTreeBase#removeButtonPressed()
	 */
	@Override
	public void removeButtonPressed() {
		//not implemented here
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.ooo.ui.IDialogCertTreeBase#reportButtonPressed()
	 */
	@Override
	public void reportButtonPressed() {
		//prints a report of the selected CERTIFICATE
		//not implemented here, next code is for test only:
		m_logger.log("list Ca available");
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.ooo.ui.IDialogCertTreeBase#selectButtonPressed()
	 */
	@Override
	public void selectButtonPressed() {
		//select the certificate on tree for signature
		m_logger.info("do operation on selected CA");
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.ooo.ui.IDialogCertTreeBase#verifyButtonPressed()
	 */
	@Override
	public void verifyButtonPressed() {
		//not implemented here
		//execute a verification of the CA certificate, using the available filters 
	}	

	/**
	 * 
	 * @param oTreeNodeObject this is the TreeElement present in the tree element data field
	 * 
	 * (non-Javadoc)
	 * @see it.plio.ext.oxsit.ooo.ui.IDialogCertTreeBase#checkButtonsEnable()
	 */
	public void checkButtonsEnable(Object oTreeNodeObject) {
		XComponent xTheCurrentComp = (XComponent)UnoRuntime.queryInterface( XComponent.class, oTreeNodeObject );
		if(xTheCurrentComp != null) {
//get node type and enable/disable	the pushbutton
			TreeElement aCurrentNode = (TreeElement)oTreeNodeObject;
			boolean bEnableButton = false;
			if(aCurrentNode.getNodeType() == it.plio.ext.oxsit.ooo.ui.TreeElement.TreeNodeType.CERTIFICATE) {
				bEnableButton = true;
			}
			enableSingleButton(m_sAddBtn,bEnableButton);
			aCurrentNode.EnableDisplay(true);
		}
		else
			enableSingleButton(m_sAddBtn,false);		
	}

	private void enableSingleButton(String sButtonName, boolean bEnable) {
//		m_aLoggerDialog.entering("enableSingleButton");
		//grab the button...
		XControl xTFControl = m_xDlgContainer.getControl( sButtonName );
		if(xTFControl != null){
			//...and set state accordingly
			XWindow xaWNode = (XWindow)UnoRuntime.queryInterface( XWindow.class, xTFControl );
			if(xaWNode != null )
				xaWNode.setEnable(bEnable);
		}
	}		
}
