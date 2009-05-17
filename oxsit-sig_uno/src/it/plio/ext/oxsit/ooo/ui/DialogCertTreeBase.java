/**
 * 
 */
package it.plio.ext.oxsit.ooo.ui;

import it.plio.ext.oxsit.Helpers;
import it.plio.ext.oxsit.ooo.GlobConstant;
import it.plio.ext.oxsit.ooo.registry.MessageConfigurationAccess;
import it.plio.ext.oxsit.ooo.ui.TreeElement.TreeNodeType;
import it.plio.ext.oxsit.security.XOX_SSCDevice;
import it.plio.ext.oxsit.security.cert.CertificateGraphicDisplayState;
import it.plio.ext.oxsit.security.cert.XOX_QualifiedCertificate;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.FocusEvent;
import com.sun.star.awt.KeyEvent;
import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XItemListener;
import com.sun.star.awt.XKeyListener;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.tree.ExpandVetoException;
import com.sun.star.awt.tree.XMutableTreeDataModel;
import com.sun.star.awt.tree.XMutableTreeNode;
import com.sun.star.awt.tree.XTreeControl;
import com.sun.star.awt.tree.XTreeExpansionListener;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XMultiPropertySet;
import com.sun.star.beans.XPropertySet;
import com.sun.star.frame.XFrame;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.script.BasicErrorException;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.view.XSelectionChangeListener;

/**
 * @author beppe
 *
 */
public class DialogCertTreeBase extends BasicDialog implements
		IDialogCertTreeBase,
		XItemListener,
		XKeyListener,
		XTreeExpansionListener, 
		XSelectionChangeListener {

	protected String 				m_sVerifyBtn = "verifyb"; //verify a signature
	protected String 				m_sRemoveBtn = "remob";  //remove a signature
//	protected String 				sCountSig = "countsigb";	//countersign (not yet implemented) 
	protected String 				m_sAddBtn = "addcertb"; //this can be add certificate, or add signature
	protected String 				m_sSelectBtn = "selectb"; //select SSCD
	protected String 				m_sReportBtn = "reportb"; //select SSCD
	protected String 				m_sTreeCtl = "certmodtree"; // the tree element
	private static final String 	m_sTextLinesBackground = "text_back";	//the control without text
	private static final String 	sEmptyTextLine = "text_L";		//the 1st line superimposed to the empty text control
	private static final String		m_sMultilineText	= "multi_l";  // the control general, with descriptive text in it

	private	Object 					m_oTreeDataModel;
	private XMutableTreeDataModel	m_xTreeDataModel;
	private XMutableTreeNode 		m_aTreeRootNode;
	private Object 					m_oTreeControlModel;	
	private XTreeControl 			m_xTreeControl = null;
	protected XMutableTreeNode 		m_aTheCurrentlySelectedTreeNode = null;
	// the following two fields are needed to be able to change
	// the font at run-time
	private Object					m_xDisplElementModel;				// the service "com.sun.star.awt.UnoControlEditModel"

	
	private String				m_sBtnOKLabel = "id_ok";
	private String				m_sBtn_CancelLabel = "id_cancel";
	private String				m_sBtn_CreateReport = "id_pb_cert_report";

	protected String 			m_sBtn_Verify = "id_pb_verif_sign";
	protected String 			m_sBtn_AddCertLabel = "id_pb_add_cert";
	protected String 			m_sBtn_RemoveCertLabel = "id_pb_rem_cert";
	protected String 			m_sBtn_SelDevice = "id_pb_sel_device";

	//title for dialog and tree structure root element
	protected String 			m_sDlgListCertTitle = "id_title_mod_cert_tree";	
	protected String 			m_sFt_Hint_Doc = "id_title_mod_cert_treew";
	
	//Strings used for certificate elements
	private String				m_sLabelVersion = "id_cert_version";
	private String				m_sLabelSerialNumer = "id_cert_ser_numb";
	private String				m_sLabelIssuer = "id_cert_issuer";
	private String				m_sLabelNotValidBefore = "id_cert_valid_from";
	private String				m_sLabelNotValidAfter = "id_cert_valid_to";
	private String				m_sLabelSubject = "id_cert_subject";
	private String				m_sLabelSubjAlgor = "id_cert_sign_alg";
	private String 				m_sLabelSubjectPublicKey = "id_cert_pub_key";
	private String 				m_sLabelThumbAlgor = "id_cert_thumbp";
	private String 				m_sLabelThumbSHA1 = "id_cert_sha1_thumbp";
	private String 				m_sLabelThumbMDA5 = "id_cert_mda5_thumbp";
	private String 				m_sLabelCertPath = "id_cert_certif_path";
	private String 				m_sLabelCritExtension = "id_cert_crit_ext";
	private String 				m_sLabelNotCritExtension = "id_cert_notcrit_ext";

	//graphic name string for indications for tree elements 
	private String 				sSignatureOK; //signature ok
	private String 				sSignatureNotValidated; //signature ok, but certificate not valid
	private String 				sSignatureBroken; //signature does not mach content: document changed after signature
	private String 				sSignatureInvalid; //signature cannot be validated
	private String 				sSignatureAdding;
	private String 				sSignatureRemoving;
	//certificate grafich path holders
	private String 				m_sCertificateElementWarning;
	private String 				m_sCertificateElementError;
	private String 				m_sCertificateElementBroken;
	protected String[]			m_sCertificateValidityGraphicName = new String[CertificateGraphicDisplayState.LAST_STATE_value]; 

	private String 				sSignatureInvalid2;
	///////////// end of graphic name string
	private String sCertificateSelectedAdd;
	private String sCertificateSelectedRemove;

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
	 * @param frame
	 * @param context
	 * @param _xmcf
	 */
	public DialogCertTreeBase(XFrame frame, XComponentContext context,
			XMultiComponentFactory _xmcf) {
		super(frame, context, _xmcf);
		m_logger.enableLogging();
		m_logger.ctor();
		//fill string for graphics
		String sLoc = Helpers.getExtensionInstallationPath(context);
		if(sLoc != null){
			String aSize = "_26.png"; //for large icons toolbar
//				String aSize = "_16.png"; //for small icons toolbar
			String m_imagesUrl = sLoc + "/images";
//main, depends from application, for now. To be changed
			//TODO change to a name not depending from the application
			sSignatureOK = m_imagesUrl + "/"+GlobConstant.m_nCERTIFICATE_CHECKED_OK+aSize; //signature ok
			sSignatureNotValidated = m_imagesUrl + "/"+GlobConstant.m_nCERTIFICATE_CHECKED_WARNING+aSize; //signature ok, but certificate not valid
			sSignatureBroken = m_imagesUrl + "/"+GlobConstant.m_nCERTIFICATE_CHECKED_INVALID+aSize; //signature does not mach content: document changed after signature
			sSignatureInvalid = m_imagesUrl + "/"+GlobConstant.m_nCERTIFICATE_CHECKED_BROKEN2+aSize; //
			sSignatureInvalid2 = m_imagesUrl + "/"+GlobConstant.m_nCERTIFICATE_CHECKED_BROKEN+aSize; //
			sSignatureAdding = m_imagesUrl + "/"+GlobConstant.m_nCERTIFICATE_ADDING+aSize; //
			sSignatureRemoving = m_imagesUrl + "/"+GlobConstant.m_nCERTIFICATE_REMOVING+aSize; //

			m_sCertificateValidityGraphicName[CertificateGraphicDisplayState.NOT_VERIFIED_value]
			                                  	= m_imagesUrl + "/"+GlobConstant.m_nCERTIFICATE_CHECKED_UNKNOWN +aSize;
			m_sCertificateValidityGraphicName[CertificateGraphicDisplayState.OK_value]
			                                  	= m_imagesUrl + "/"+GlobConstant.m_nCERTIFICATE_CHECKED_OK+aSize;
			m_sCertificateValidityGraphicName[CertificateGraphicDisplayState.WARNING_value]
			                                  	= m_imagesUrl + "/"+GlobConstant.m_nCERTIFICATE_CHECKED_WARNING +aSize;
			m_sCertificateValidityGraphicName[CertificateGraphicDisplayState.NO_DATE_VALID_value]
			                                  	= m_imagesUrl + "/"+GlobConstant.m_nCERTIFICATE_CHECKED_BROKEN2 +aSize;
			m_sCertificateValidityGraphicName[CertificateGraphicDisplayState.NOT_COMPLIANT_value]
			                                  	= m_imagesUrl + "/"+GlobConstant.m_nCERTIFICATE_CHECKED_BROKEN +aSize;
			m_sCertificateValidityGraphicName[CertificateGraphicDisplayState.NOT_VALID_value]
			                                  	= m_imagesUrl + "/"+GlobConstant.m_nCERTIFICATE_CHECKED_INVALID +aSize;
			m_sCertificateValidityGraphicName[CertificateGraphicDisplayState.MARKED_TO_BE_ADDED_value]
			                                  	= m_imagesUrl + "/"+GlobConstant.m_nCERTIFICATE_ADDING +aSize;
			m_sCertificateValidityGraphicName[CertificateGraphicDisplayState.MARKED_TO_BE_REMOVED_value]
			                                  	= m_imagesUrl + "/"+GlobConstant.m_nCERTIFICATE_REMOVING +aSize;
//the certificate elements graphic name string			
			m_sCertificateElementBroken = m_imagesUrl + "/"+GlobConstant.m_nCERT_ELEM_BROKEN +aSize;
			m_sCertificateElementWarning = m_imagesUrl + "/"+GlobConstant.m_nCERT_ELEM_WARNING +aSize;
			m_sCertificateElementBroken = m_imagesUrl + "/"+GlobConstant.m_nCERT_ELEM_INVALID +aSize;
		}
		else
			m_logger.severe("ctor","no package location !");
	}

	/**
	 * prepare the strings for the dialogs
	 */
	protected void fillLocalizedString() {
		MessageConfigurationAccess m_aRegAcc = null;
		m_aRegAcc = new MessageConfigurationAccess(m_xContext, m_xMCF);

		try {
			m_sBtnOKLabel = m_aRegAcc.getStringFromRegistry( m_sBtnOKLabel );			
			m_sBtn_CancelLabel = m_aRegAcc.getStringFromRegistry( m_sBtn_CancelLabel );
			m_sBtn_CreateReport = m_aRegAcc.getStringFromRegistry( m_sBtn_CreateReport );
			m_sBtn_Verify = m_aRegAcc.getStringFromRegistry( m_sBtn_Verify );
			m_sBtn_RemoveCertLabel = m_aRegAcc.getStringFromRegistry( m_sBtn_RemoveCertLabel );
			m_sDlgListCertTitle = m_aRegAcc.getStringFromRegistry( m_sDlgListCertTitle );
			m_sBtn_SelDevice = m_aRegAcc.getStringFromRegistry( m_sBtn_SelDevice );
			m_sBtn_AddCertLabel = m_aRegAcc.getStringFromRegistry( m_sBtn_AddCertLabel );
			m_sFt_Hint_Doc = m_aRegAcc.getStringFromRegistry( m_sFt_Hint_Doc );

//strings for certificate tre control display
			m_sLabelVersion = m_aRegAcc.getStringFromRegistry( m_sLabelVersion );
			m_sLabelSerialNumer = m_aRegAcc.getStringFromRegistry( m_sLabelSerialNumer );
			m_sLabelIssuer = m_aRegAcc.getStringFromRegistry( m_sLabelIssuer );
			m_sLabelNotValidBefore = m_aRegAcc.getStringFromRegistry( m_sLabelNotValidBefore );
			m_sLabelNotValidAfter = m_aRegAcc.getStringFromRegistry( m_sLabelNotValidAfter );
			m_sLabelSubject = m_aRegAcc.getStringFromRegistry( m_sLabelSubject );
			m_sLabelSubjAlgor = m_aRegAcc.getStringFromRegistry( m_sLabelSubjAlgor );
			m_sLabelSubjectPublicKey = m_aRegAcc.getStringFromRegistry( m_sLabelSubjectPublicKey );
			m_sLabelThumbAlgor = m_aRegAcc.getStringFromRegistry( m_sLabelThumbAlgor );
			m_sLabelThumbSHA1 = m_aRegAcc.getStringFromRegistry( m_sLabelThumbSHA1 );
			m_sLabelThumbMDA5 = m_aRegAcc.getStringFromRegistry( m_sLabelThumbMDA5 );
			m_sLabelCertPath = m_aRegAcc.getStringFromRegistry( m_sLabelCertPath );
			m_sLabelCritExtension = m_aRegAcc.getStringFromRegistry( m_sLabelCritExtension );
			m_sLabelNotCritExtension = m_aRegAcc.getStringFromRegistry( m_sLabelNotCritExtension );
		} catch (com.sun.star.uno.Exception e) {
			m_logger.severe("fillLocalizedString", e);
		}
		m_aRegAcc.dispose();
	}

	public void initializeLocal(String _sName, String _sTitle, int posX, int posY) throws BasicErrorException {
		m_logger.entering("initialize");
		try {
			super.initialize(_sName, _sTitle, CertifTreeDlgDims.dsHeigh(), CertifTreeDlgDims.dsWidth(), posX, posY);
			//inserts the control elements needed to display properties
			//multiline text control used as a light yellow background
					//multiline text control for details
			Object oEdit = insertEditFieldModel(this, this,
							CertifTreeDlgDims.dsTextFieldColumn(),
							CertifTreeDlgDims.DS_ROW_0(),
							CertifTreeDlgDims.DS_ROW_3()-CertifTreeDlgDims.DS_ROW_0(),
							CertifTreeDlgDims.dsTextFieldWith(),
							-1,
							"", m_sTextLinesBackground, true, true, false, false);
			//now change its background color
			XPropertySet xPSet = (XPropertySet) UnoRuntime
								.queryInterface( XPropertySet.class, oEdit );
			xPSet.setPropertyValue(new String("BackgroundColor"), new Integer(ControlDims.DLG_CERT_TREE_BACKG_COLOR));

			//insert the fixed text lines layed over the above mentioned element
			insertDisplayLinesOfText();

			//multiline text control for details of tree node element under selection
			m_xDisplElementModel = insertEditFieldModel(this, this,
							CertifTreeDlgDims.dsTextFieldColumn(),
							CertifTreeDlgDims.DS_ROW_0(),
							CertifTreeDlgDims.DS_ROW_3()-CertifTreeDlgDims.DS_ROW_0(),
							CertifTreeDlgDims.dsTextFieldWith(),
							-1,
							"", m_sMultilineText, true, true, true, true);

			xPSet = (XPropertySet) UnoRuntime
								.queryInterface( XPropertySet.class, m_xDisplElementModel );
			xPSet.setPropertyValue(new String("BackgroundColor"), new Integer(ControlDims.DLG_CERT_TREE_BACKG_COLOR));	

			//Insert the tree control
			m_xTreeControl = insertTreeControl(this,
							CertifTreeDlgDims.DS_COL_0(), 
							CertifTreeDlgDims.DS_ROW_0(), 
							CertifTreeDlgDims.DS_ROW_3()-CertifTreeDlgDims.DS_ROW_0(),
							CertifTreeDlgDims.dsTreeControlWith(), //CertifTreeDlgDims.DS_COL_4() - CertifTreeDlgDims.DS_COL_0(),
							m_sTreeCtl,
							m_sFt_Hint_Doc, 10);
			insertButton(this,
							CertifTreeDlgDims.DS_COL_PB5(),
							CertifTreeDlgDims.DS_ROW_4(),
							CertifTreeDlgDims.dsBtnWidthCertTree(),
							m_sReportBtn,
							m_sBtn_CreateReport,
							(short) PushButtonType.STANDARD_value, 8);
					//cancel button
			insertButton(this,
							CertifTreeDlgDims.DLGS_BOTTOM_HELP_X(CertifTreeDlgDims.dsWidth()),
							CertifTreeDlgDims.DLGS_BOTTOM_BTN_Y(CertifTreeDlgDims.dsHeigh()),
							ControlDims.RSC_CD_PUSHBUTTON_WIDTH,
							"cancb",
							m_sBtn_CancelLabel,
							(short) PushButtonType.CANCEL_value, 2);
			// ok button
			insertButton(this,
							CertifTreeDlgDims.DLGS_BOTTOM_CANCEL_X(CertifTreeDlgDims.dsWidth()),
							CertifTreeDlgDims.DLGS_BOTTOM_BTN_Y(CertifTreeDlgDims.dsHeigh()),
							ControlDims.RSC_CD_PUSHBUTTON_WIDTH,
							"okb",
							m_sBtnOKLabel,
							(short) PushButtonType.OK_value, 1);

			xDialog = (XDialog) UnoRuntime.queryInterface(XDialog.class, super.m_xDialogControl);		
			createWindowPeer();
			disableAllNamedControls();

		} catch (UnknownPropertyException e) {
			e.printStackTrace();
			m_logger.severe("initialize", e);
		} catch (PropertyVetoException e) {
			m_logger.severe("initialize", e);
		} catch (IllegalArgumentException e) {
			m_logger.severe("initialize", e);
		} catch (WrappedTargetException e) {
			m_logger.severe("initialize", e);
		}
	}

	private XTreeControl insertTreeControl(XSelectionChangeListener _xActionListener,
			int _nPosX,
			int _nPosY,
			int _nHeight,
			int _nWidth,
			String _sName,
			String _sLabel,
			int _nStep) {

		XTreeControl xTree = null;

		// create a controlmodel at the multiservicefactory of the dialog
		// model...
		try {
			m_oTreeDataModel = m_xMCF.createInstanceWithContext("com.sun.star.awt.tree.MutableTreeDataModel", m_xContext);
			if(m_oTreeDataModel == null) {
				m_logger.severe("insertTreeControl", "the com.sun.star.awt.tree.MutableTreeDataModel wasn't created!");
				return null;
			}

			m_xTreeDataModel = (XMutableTreeDataModel)UnoRuntime.queryInterface( XMutableTreeDataModel.class, m_oTreeDataModel );
			if(m_xTreeDataModel == null) {
				m_logger.severe("insertTreeControl", "the XMutableTreeDataModel not available!");
				return null;
			}
			m_aTreeRootNode = m_xTreeDataModel.createNode(_sLabel, true);
			if(m_aTreeRootNode == null) {
				m_logger.severe("insertTreeControl", "the Node not available!");
				return null;
			}
			m_xTreeDataModel.setRoot(m_aTreeRootNode);

			m_oTreeControlModel = m_xMSFDialogModel.createInstance( "com.sun.star.awt.tree.TreeControlModel" );
			if(m_oTreeControlModel == null) {
				m_logger.severe("insertTreeControl", "the oTreeModel not available!");
				return null;
			}

			XMultiPropertySet xTreeMPSet = (XMultiPropertySet) UnoRuntime.queryInterface( XMultiPropertySet.class, m_oTreeControlModel );
			if(xTreeMPSet == null ) {
				m_logger.severe("insertTreeControl", "no XMultiPropertySet");
				return null;
			}

			// Set the properties at the model - keep in mind to pass the
			// property names in alphabetical order!
			xTreeMPSet.setPropertyValues( new String[] {
					"BackgroundColor",
					"DataModel",
					"Editable",
					"Height", 
//			"Label", 
					"Name",
					"PositionX", 
					"PositionY", 
//					"RootDisplayed",
					"SelectionType",
					"ShowsRootHandles",
					"Step",
					"TabIndex",
					"Width"
					},
					new Object[] {
					new Integer( ControlDims.DLG_CERT_TREE_BACKG_COLOR ),
					m_oTreeDataModel, //where the DataModel is attached
					new Boolean( true ),
					new Integer( _nHeight ),
//			_sLabel,
					_sName,
					new Integer( _nPosX ),
					new Integer( _nPosY ),
//					new Boolean( false ), //RootDisplayed, but does not function...
					new Integer(com.sun.star.view.SelectionType.SINGLE_value),
					new Boolean( true ),
					new Integer( 0 ),//Step
					new Short( (short)_nStep ), //TabIndex
					new Integer( _nWidth )					
					} );

//			Utilities.showProperties(m_oTreeControlModel, xTreeMPSet);
			// add the control model to the NameContainer of the dialog model
			m_xDlgModelNameContainer.insertByName( _sName, m_oTreeControlModel );
			XControl xTreeControl = m_xDlgContainer.getControl( _sName );

			xTree = (XTreeControl) UnoRuntime.queryInterface( XTreeControl.class, xTreeControl );
			xTree.addSelectionChangeListener(_xActionListener);

			//////////////////////////
/*			XWindow xTFWindow = (XWindow) UnoRuntime.queryInterface( XWindow.class,
					xTreeControl );
			xTFWindow.addFocusListener( this );
			xTFWindow.addKeyListener( this );*/
			
		} catch (com.sun.star.uno.Exception ex) {
			m_logger.severe("insertTreeControl", ex);
		}
		return xTree;
	}

	private void insertDisplayLinesOfText() {
		//now inserts the fixed text lines over the above mentioned element
		for(int i = 0; i < CertifTreeDlgDims.m_nMAXIMUM_FIELDS; i++) {
			insertFixedText(this,
					CertifTreeDlgDims.TEXT_0X(),
					CertifTreeDlgDims.TEXT_L0Y()+ControlDims.RSC_CD_FIXEDTEXT_HEIGHT*i,
					CertifTreeDlgDims.dsWidth()-CertifTreeDlgDims.TEXT_0X()-ControlDims.RSC_SP_DLG_INNERBORDER_RIGHT,
					-1,
					"l "+i, // a dummy text
					sEmptyTextLine+i
					);
		}
	}

	private void removeTreeNodeHelper(XMutableTreeNode _aStartNode) {
//		m_logger.entering("removeTreeNodeHelper "+m_nNestedLevel);
		//get the XTreeNode interface
		//child index
		Object oObj = new Object();
		int childIndex = _aStartNode.getChildCount();
		try {
			while(childIndex > 0) {
				childIndex--;
				oObj = _aStartNode.getChildAt(childIndex);
				XMutableTreeNode aNode = (XMutableTreeNode)UnoRuntime.queryInterface(XMutableTreeNode.class, oObj);
				m_xTreeControl.collapseNode(aNode);
				if(aNode.getChildCount() == 0) {
					//node with no child, remove it
					//FIXME: first remove the child node data
					// for now, only cleared
					Object oTreeNodeObject  = aNode.getDataValue();
					if(oTreeNodeObject != null) {
						if(oTreeNodeObject instanceof TreeElement) {
							TreeElement aCurrentNode = (TreeElement)oTreeNodeObject;
							aCurrentNode.dispose();
						}
						else
							m_logger.warning("Wrong class type in tree control node data: "+oTreeNodeObject.getClass().getName());
					}
					
					aNode.setDataValue(null);
					m_xTreeControl.collapseNode(aNode);
					//then remove it
					_aStartNode.removeChildByIndex(childIndex);
				}
				else
					removeTreeNodeHelper(aNode); //recursive call
				childIndex = _aStartNode.getChildCount();
			}
		} catch (java.lang.Exception e) {
			m_logger.severe("Index: "+childIndex,e);
		}
	}

	protected void removeAllTreeNodes() {
//first remove all selection from the tree control
		m_xTreeControl.clearSelection();
		removeTreeNodeHelper(m_aTreeRootNode);		
	}
	
	private XMutableTreeNode addMultiLineToTreeRootHelper(BaseGeneralNodeTreeElement aSSCDnode) {		
		//connect it to the right dialog pane
		aSSCDnode.setBackgroundControl(m_xDlgContainer.getControl( m_sTextLinesBackground ));
		for(int i=0; i < CertifTreeDlgDims.m_nMAXIMUM_FIELDS; i++ ) {
			aSSCDnode.setAControlLine(m_xDlgContainer.getControl( sEmptyTextLine+i ), i);
		}
		//add it to the tree root node
		XMutableTreeNode xaCNode = m_xTreeDataModel.createNode(aSSCDnode.getNodeName(), true);
		if(aSSCDnode.getNodeGraphic() != null)
			xaCNode.setNodeGraphicURL(aSSCDnode.getNodeGraphic());

		xaCNode.setDataValue(aSSCDnode);
		try {
			m_aTreeRootNode.appendChild(xaCNode);			
			m_xTreeControl.expandNode(m_aTreeRootNode);			
		} catch (IllegalArgumentException e) {
			m_logger.severe("addMultiLineToTreeRootHelper", e);
		} catch (ExpandVetoException e) {
			m_logger.severe("addMultiLineToTreeRootHelper", e);
		}
		return xaCNode;
	}

	protected XMutableTreeNode addSSCDToTreeRootHelper(XOX_SSCDevice _aSSCDev) {		
		//add the device to the dialog, a single node, with the device as a description
		SSCDTreeElement aSSCDnode = new SSCDTreeElement(m_xContext,m_xMCF);
		aSSCDnode.initialize();
		aSSCDnode.setSSCDDATA(_aSSCDev);
		return  addMultiLineToTreeRootHelper(aSSCDnode);
	}

	////// methods to manage the certificate display
	protected void addQualifiedCertificateToTree(XMutableTreeNode _aParentNode, XOX_QualifiedCertificate _aCertif) {
		//instantiate a certificate node
		CertificateTreeElement aNewNode = new CertificateTreeElement(m_xContext,m_xMCF);
		aNewNode.setCertificateData(_aCertif);
		
		//connect it to the right dialog pane
		aNewNode.setBackgroundControl(m_xDlgContainer.getControl( m_sTextLinesBackground ));
		for(int i=0; i < CertifTreeDlgDims.m_nMAXIMUM_FIELDS; i++ ) {
			aNewNode.setAControlLine(m_xDlgContainer.getControl( sEmptyTextLine+i ), i);
		}
		//create a new node to be used for this element
		XMutableTreeNode xaCNode = m_xTreeDataModel.createNode(aNewNode.getNodeName(), true);

		aNewNode.setNodeGraphic(m_sCertificateValidityGraphicName[_aCertif.getCertificateGraficStateValue()]);
		if(aNewNode.getNodeGraphic() != null)
			xaCNode.setNodeGraphicURL(aNewNode.getNodeGraphic());

		//link to our data
		xaCNode.setDataValue(aNewNode);
		//add it to the parent node
		try {
			_aParentNode.appendChild(xaCNode);			
			m_xTreeControl.expandNode(_aParentNode);
		} catch (IllegalArgumentException e) {
			m_logger.severe("addQualifiedCertificateToTree", e);
		} catch (ExpandVetoException e) {
			// TODO Auto-generated catch block
			m_logger.severe("addQualifiedCertificateToTree", e);
		}
		//now add the rest of the data
		//add the version
		addVariablePitchTreeElement(xaCNode, TreeNodeType.VERSION, m_sLabelVersion, _aCertif.getVersion());		
		//add the serial number
		addVariablePitchTreeElement(xaCNode,TreeNodeType.SERIAL_NUMBER,m_sLabelSerialNumer,_aCertif.getSerialNumber());
		//add the issuer full description		
		addVariablePitchTreeElement(xaCNode,TreeNodeType.ISSUER,m_sLabelIssuer,_aCertif.getIssuerName());
		//add the not valid before
		addVariablePitchTreeElement(xaCNode,TreeNodeType.VALID_FROM,m_sLabelNotValidBefore,_aCertif.getNotValidBefore());
		//add the not valid after
		addVariablePitchTreeElement(xaCNode,TreeNodeType.VALID_TO,m_sLabelNotValidAfter,_aCertif.getNotValidAfter());
		//add the subject
		addVariablePitchTreeElement(xaCNode,TreeNodeType.SUBJECT,m_sLabelSubject,_aCertif.getSubjectName());
		//add the subject signature algorithm
		addVariablePitchTreeElement(xaCNode,TreeNodeType.SUBJECT_ALGORITHM,m_sLabelSubjAlgor,_aCertif.getSubjectPublicKeyAlgorithm());
		//add the subject public key (multiline, fixed pitch)
		addFixedPitchTreeElement(xaCNode,TreeNodeType.PUBLIC_KEY,m_sLabelSubjectPublicKey,_aCertif.getSubjectPublicKeyValue());
		//add the thumbprint signature algorithm
		addVariablePitchTreeElement(xaCNode,TreeNodeType.SIGNATURE_ALGORITHM,m_sLabelThumbAlgor,_aCertif.getSignatureAlgorithm());
		//add the SHA1 thumbprint 
		addFixedPitchTreeElement(xaCNode,TreeNodeType.THUMBPRINT_SHA1,m_sLabelThumbSHA1,_aCertif.getSHA1Thumbprint());
		//add the MDA5 thumbprint
		addFixedPitchTreeElement(xaCNode,TreeNodeType.THUMBPRINT_MD5,m_sLabelThumbMDA5,_aCertif.getMD5Thumbprint());

		//add the critical extensions
		try {
			String[] aCtritExt = _aCertif.getCriticalCertificateExtensionOIDs();
			if(aCtritExt != null) {
				//then there are extension marked critical
				//add the main node
				XMutableTreeNode xNode = addEmptyDataTreeElement(xaCNode,
							TreeNodeType.EXTENSIONS_CRITICAL,m_sLabelCritExtension);
				for(int i=0; i<aCtritExt.length;i++) {
					addVariablePitchTreeElement(xNode,TreeNodeType.EXTENSIONS_CRITICAL,
							_aCertif.getCertificateExtensionName(aCtritExt[i]),
							_aCertif.getCertificateExtensionStringValue(aCtritExt[i]));
				}
			}
		} catch (Exception e) {
			m_logger.severe("addQualifiedCertificateToTree", e);
		}

		//add the non critical extensions
		try {
			String[] aNotCtritExt = _aCertif.getNotCriticalCertificateExtensionOIDs();
			if(aNotCtritExt != null) {
			//then there are extension NOT marked critical
			//add the main node
				XMutableTreeNode xNode = addEmptyDataTreeElement(xaCNode,
							TreeNodeType.EXTENSIONS_NON_CRITICAL,m_sLabelNotCritExtension);
				for(int i=0; i<aNotCtritExt.length;i++) {
					if(aNotCtritExt[i].equalsIgnoreCase("2.5.29.14") ||
							aNotCtritExt[i].equalsIgnoreCase("2.5.29.35")) 
						addFixedPitchTreeElement(xNode,TreeNodeType.EXTENSIONS_NON_CRITICAL,
								_aCertif.getCertificateExtensionName(aNotCtritExt[i]),
								_aCertif.getCertificateExtensionStringValue(aNotCtritExt[i]));
						
					else
					addVariablePitchTreeElement(xNode,TreeNodeType.EXTENSIONS_NON_CRITICAL,
							_aCertif.getCertificateExtensionName(aNotCtritExt[i]),
							_aCertif.getCertificateExtensionStringValue(aNotCtritExt[i]));
				}
			}
		} catch (Exception e) {
			m_logger.severe("addQualifiedCertificateToTree", e);
		}
		//add the certificate path
		XMutableTreeNode xNode = addEmptyDataTreeElement(xaCNode,
				TreeNodeType.CERTIFICATION_PATH,m_sLabelCertPath);
	}

	//test function, remove when ready!
	public void addOneSignature() {
		//create a fake certificate description
		SignatureTreeElement aCert = new SignatureTreeElement(m_xContext, m_xMCF);
		aCert.initialize();
		addOneFakeCertificateToTreeRootHelper(aCert);
	}
	protected XMutableTreeNode addOneFakeCertificateToTreeRootHelper(BaseCertificateTreeElement aCert) {		
		//connect it to the right dialog pane
		aCert.setBackgroundControl(m_xDlgContainer.getControl( m_sTextLinesBackground ));
		for(int i=0; i < CertifTreeDlgDims.m_nMAXIMUM_FIELDS; i++ ) {
			aCert.setAControlLine(m_xDlgContainer.getControl( sEmptyTextLine+i ), i);
		}
		//add it to the tree root node
		XMutableTreeNode xaCNode = m_xTreeDataModel.createNode(aCert.getNodeName(), true);
		if(aCert.getNodeGraphic() != null)
			xaCNode.setNodeGraphicURL(aCert.getNodeGraphic());

		xaCNode.setDataValue(aCert);
		try {
			m_aTreeRootNode.appendChild(xaCNode);			
			m_xTreeControl.expandNode(m_aTreeRootNode);			
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			m_logger.severe("addOneCertificate", e);
		} catch (ExpandVetoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return xaCNode;
	}

	private XMutableTreeNode addMultilineTreeElementHelper(XMutableTreeNode _Node, MultilineTreeElementBase _aElm, String _sLabel) {
		XMutableTreeNode xaCNode = m_xTreeDataModel.createNode(_sLabel, true);
		if(_aElm.getNodeGraphic() != null)
			xaCNode.setNodeGraphicURL(_aElm.getNodeGraphic());

		xaCNode.setDataValue(_aElm);
		try {
			//add it to the tree node
			_Node.appendChild(xaCNode);			
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			m_logger.severe("addMultilineTreeElementHelper", e);
		}
		return xaCNode;		
	}

	private XMutableTreeNode addFixedPitchTreeElement(XMutableTreeNode _Node, TreeNodeType _aType, String _sLabel, String _sContents) {
		FixedFontPitchTreeElement aElem = new FixedFontPitchTreeElement(m_xContext, m_xMCF, _sContents, m_xDlgContainer.getControl( m_sMultilineText ));
		aElem.setNodeType(_aType);
		return addMultilineTreeElementHelper(_Node, aElem, _sLabel);
	}

	private XMutableTreeNode addVariablePitchTreeElement(XMutableTreeNode _Node, TreeNodeType _aType, String _sLabel, String _sContents) {
		MultilineTreeElementBase aElem = new MultilineTreeElementBase(m_xContext, m_xMCF, _sContents, m_xDlgContainer.getControl( m_sMultilineText ));
		aElem.setNodeType(_aType);
		return addMultilineTreeElementHelper(_Node, aElem, _sLabel);
	}

	private XMutableTreeNode addEmptyDataTreeElement(XMutableTreeNode _Node, TreeNodeType _aType, String _sLabel) {
		XMutableTreeNode xaCNode = m_xTreeDataModel.createNode(_sLabel, true);
		xaCNode.setDataValue(null);
		try {
			//add it to the tree node
			_Node.appendChild(xaCNode);			
		} catch (IllegalArgumentException e) {
			m_logger.severe("addEmptyDataTreeElement", e);
		}
		return xaCNode;		
	}

	////////////////////////////////////
	/// methods to manage the UI

	// next five methods MUST be implemented by subclasses to
	// manage the buttons behavior 

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.ooo.ui.IDialogCertTreeBase#addButtonPressed()
	 */
	@Override
	public void addButtonPressed() {
		// TODO Auto-generated method stub
		m_logger.log("addButtonPressed");		
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.ooo.ui.IDialogCertTreeBase#removeButtonPressed()
	 */
	@Override
	public void removeButtonPressed() {
		// TODO Auto-generated method stub
		m_logger.log("removeButtonPressed");
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.ooo.ui.IDialogCertTreeBase#reportButtonPressed()
	 */
	@Override
	public void reportButtonPressed() {
		// TODO Auto-generated method stub
		m_logger.log("reportButtonPressed");
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.ooo.ui.IDialogCertTreeBase#selectButtonPressed()
	 */
	@Override
	public void selectButtonPressed() {
		// TODO Auto-generated method stub
		m_logger.log("selectButtonPressed");
	}

	/* (non-Javadoc)
	 * @see it.plio.ext.oxsit.ooo.ui.IDialogCertTreeBase#verifyButtonPressed()
	 */
	@Override
	public void verifyButtonPressed() {
		// TODO Auto-generated method stub
		m_logger.log("verifyButtonPressed");		
	}

	/* (non-Javadoc)
	 * @see com.sun.star.awt.XActionListener#actionPerformed(com.sun.star.awt.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent rEvent) {
		// TODO Auto-generated method stub
		try {
			// get the control that has fired the event,
			XControl xControl = (XControl) UnoRuntime.queryInterface(XControl.class,
					rEvent.Source);
			XControlModel xControlModel = xControl.getModel();
			XPropertySet xPSet = (XPropertySet) UnoRuntime.queryInterface(
					XPropertySet.class, xControlModel);
			String sName = (String) xPSet.getPropertyValue("Name");
			// just in case the listener has been added to several controls,
			// we make sure we refer to the right one
			m_logger.log("actionPerformed","action: "+sName);
			if (sName.equals(m_sAddBtn)) {
				addButtonPressed();
			} else if (sName.equals(m_sSelectBtn)) {
				selectButtonPressed();
			} else if (sName.equals(m_sReportBtn)) {
				reportButtonPressed();
			} else if (sName.equals(m_sVerifyBtn)) {
				verifyButtonPressed();
			} else if (sName.equals(m_sRemoveBtn)) {
				removeButtonPressed();
			}
			else {
				m_logger.warning("actionPerformed","Activated, unimplemented: " + sName);
			}
		} catch (com.sun.star.uno.Exception ex) {
			/*
			 * perform individual exception handling here. Possible exception
			 * types are: com.sun.star.lang.WrappedTargetException,
			 * com.sun.star.beans.UnknownPropertyException,
			 * com.sun.star.uno.Exception
			 */
			m_logger.severe("actionPerformed", ex);
		}
	}

	private void disableNamedControl(String sTheName) {
		XControl xTFControl = m_xDlgContainer.getControl( sTheName );
		if(xTFControl != null){
			XWindow xaWNode = (XWindow)UnoRuntime.queryInterface( XWindow.class, xTFControl );
			if(xaWNode != null )
				xaWNode.setVisible(false);
		}
	}

	protected void disableAllNamedControls() {
		disableNamedControl(m_sMultilineText);
		disableNamedControl(m_sTextLinesBackground);
		for(int i = 0; i < CertifTreeDlgDims.m_nMAXIMUM_FIELDS; i++)
			disableNamedControl(sEmptyTextLine+i);
	}

	/* (non-Javadoc)
	 * @see com.sun.star.view.XSelectionChangeListener#selectionChanged(com.sun.star.lang.EventObject)
	 */
	@Override
	public void selectionChanged( com.sun.star.lang.EventObject _eventObject ) {
		Object oObject = m_xTreeControl.getSelection();
// check if it's a node		
		XMutableTreeNode xaENode = (XMutableTreeNode)UnoRuntime.queryInterface( XMutableTreeNode.class, 
				oObject );
		//disable the previous Node (which still is the current one)
		if(m_aTheCurrentlySelectedTreeNode != null) {
			//disable it, that is un-display it
			Object oTreeNodeObject  = m_aTheCurrentlySelectedTreeNode.getDataValue();
			if(oTreeNodeObject != null) {
				if(oTreeNodeObject instanceof TreeElement) {
					TreeElement aCurrentNode = (TreeElement)oTreeNodeObject;
					aCurrentNode.EnableDisplay(false);
				}
				else
					m_logger.warning("Wrong class type in tree control node data: "+oTreeNodeObject.getClass().getName());
			}
		}
		else {// old node null, disable all all the display elements
			disableAllNamedControls();
		}
		//...and set the new old node
		m_aTheCurrentlySelectedTreeNode = xaENode;

		if(xaENode != null) {
			checkButtonsEnable(xaENode.getDataValue());
		}
	}	

	/**
	 * 
	 * @param oTreeNodeObject this is the TreeElement present in the tree element data field
	 * 
	 * (non-Javadoc)
	 * @see it.plio.ext.oxsit.ooo.ui.IDialogCertTreeBase#checkButtonsEnable()
	 */
	public void checkButtonsEnable(Object oTreeNodeObject) {
	}

	/* (non-Javadoc)
	 * @see com.sun.star.awt.XKeyListener#keyPressed(com.sun.star.awt.KeyEvent)
	 */
	@Override
	public void keyPressed(KeyEvent arg0) {
		// TODO Auto-generated method stub
/*		m_aLoggerDialog.entering("keyPressed on subclass"+arg0.KeyCode);*/
	}

	/* (non-Javadoc)
	 * @see com.sun.star.awt.XKeyListener#keyReleased(com.sun.star.awt.KeyEvent)
	 * 
	 */
	@Override
	public void keyReleased(KeyEvent arg0) {
		// TODO Auto-generated method stub
//		m_aLoggerDialog.entering("keyReleased, on subclass! "+arg0.KeyCode);
		
		//if arg0.KeyCode = 773 (key F6), set focus to certificate tree element
		if(arg0.KeyCode == com.sun.star.awt.Key.F6) {
			XWindow xTFWindow = (XWindow) UnoRuntime.queryInterface( XWindow.class,
					m_xTreeControl );
			xTFWindow.setFocus();
		}
	}

	/* (non-Javadoc)
	 * @see com.sun.star.awt.XFocusListener#focusGained(com.sun.star.awt.FocusEvent)
	 */
	@Override
	public void focusGained(FocusEvent arg0) {
//		m_aLoggerDialog.entering("focusGained, on subclass!");
	}

	/* (non-Javadoc)
	 * @see com.sun.star.awt.XFocusListener#focusGained(com.sun.star.awt.FocusEvent)
	 */
	@Override
	public void focusLost(FocusEvent arg0) {
//		m_aLoggerDialog.entering("focusLost, on subclass!");
	}
}
