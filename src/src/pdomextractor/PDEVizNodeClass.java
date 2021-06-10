package pdomextractor;

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.ExpansionOverlapsBoundaryException;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.internal.ui.editor.CDocumentProvider;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.internal.resources.File;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;

public class PDEVizNodeClass {
	// �\���Ώۃm�[�h
	public IASTNode node;
	// �\���Ώۃm�[�h���璊�o�����\���f�[�^��ێ�����\���N���X
	public PDEVizPartsClass vizParts;
	// �\���Ώێq�m�[�h�̃��X�g
	public ArrayList<PDEVizNodeClass> children;
	
	PDEVizNodeClass()
	{
		children = new ArrayList<PDEVizNodeClass>();
	}

	static ArrayList<PDEVizNodeClass> createPDEVizNodeTree( IASTNode topNode, IDocument doc ,IASTComment[] comments )
	{
		ArrayList<PDEVizNodeClass> vizTree = new ArrayList<PDEVizNodeClass>();
		
		// topNode�̎q�v�f�����ɕ\���m�[�h�����邽�߂̃��[�v
		IASTNode[] children = topNode.getChildren();
		for( int j=0; j<children.length; j++ )
		{
			
			//----------------------------------------------------------------------------------
			// Try to convert child item to pdevNode
			//----------------------------------------------------------------------------------
			try
			{
				int i = j-1; // prev
				int k = j+1; // next
							
				//----------------------------------------------------------------------------------
				// node�Ɋ֘A����R�����g�̒��o�͈͍s�����擾����
				//----------------------------------------------------------------------------------
				int lNum_prvNd = -1;
				int lNum_nxtNd = -1;
				// ����ȏꍇ�̏���
				// �擪statement�̏ꍇ
				if( j == 0 )
				{
					// �e�m�[�h(Compound��z�肷��)�̊J�n�s���Z�b�g����
					lNum_prvNd = children[j].getParent().getFileLocation().getStartingLineNumber();
				}
				// ����ȏꍇ�̏���
				// �ŏIstatement�̏ꍇ
				if( j == children.length -1 )
				{
					// ���g�̏I���s���Z�b�g����
					lNum_nxtNd = children[j].getFileLocation().getEndingLineNumber();
				}
				// ����ȊO
				// ����ȏꍇ�ɓ��Ă͂܂�Ȃ������̂ŁA���Ostatement�̏I���s���Z�b�g����
				if( lNum_prvNd == -1 ){	lNum_prvNd = children[i].getFileLocation().getEndingLineNumber();}
				// ����ȏꍇ�ɓ��Ă͂܂�Ȃ������̂ŁA����statement�̊J�n�s���Z�b�g����
				if( lNum_nxtNd == -1 ){ lNum_nxtNd = children[k].getFileLocation().getStartingLineNumber();}
				
				//----------------------------------------------------------------------------------
				// �\���m�[�h�@PDEVizNodeClass�@�C���X�^���X���쐬
				//----------------------------------------------------------------------------------
				IASTNode iastNode = children[j];
				
				//----------------------------------------------------------------------------------
				// �m�[�h��ʂɂ����ʏ���
				//----------------------------------------------------------------------------------
				// if Statement �������ꍇ�A if, if-else, if-elseif �̍\���ɑΉ����邽�߂ɓ��ʏ��������{����
				// 
				// [Eclipse CDT AST�d�l]
				// if-else�\���̏ꍇ�AAST����else�߂�Statement�m�[�h�������Ȃ��B
				// �����if�߂�2��Compound�������A2�ڂ�Compound��else�̓��e��ێ�����
				// 
				// if-elseif �\���̏ꍇ�AAST���ł�IfStatement��������IfStatement�������A
				// ���ꂪelseif�߂�\������B����āAelseif�̓x�ɁAAST�̊K�w��1�[���Ȃ�B
				// 
				// [PDE�̏o�͎d�l]
				// if-else�\���̏ꍇ�Aelse�߂�Statement��if�Ɠ����K�w�ɏo�͂���B
				// Statement�m�[�h�̓��e�͂Ƃ肠�����K���ɍ���Ă���
				// 
				// if-elseif�\���̏ꍇ�Aelseif�߂�Statement��if�Ɠ����K�w�ɏo�͂���B
				// AST���ifStatement�̒����ɂ���elseif������ifStatement��elseif�ł������t������
				// �擪��ifStatement�Ɠ����K�w�Ɉړ�������B
				//----------------------------------------------------------------------------------
				if( iastNode instanceof IASTIfStatement )
				{
					IASTIfStatement ifNode = (IASTIfStatement)iastNode;
					
					// if�߂�\������PDE�m�[�h�𐶐�����
					PDEVizNodeClass pdeIfNode = new PDEVizNodeClass();
					pdeIfNode.node = ifNode;							// node�͂Ƃ肠����ifNode��ݒ肵�Ă���
					pdeIfNode.vizParts = new PDEVizPartsClass(ifNode, doc, comments, lNum_prvNd, lNum_nxtNd);

					// if�߂�Compound���m�[�h�����邽�߂̍ċA����
					pdeIfNode.children = createPDEVizNodeTree(ifNode.getThenClause(), doc, comments);

					// if�߂�PDE�m�[�h���C���X�^���X�����X�g��topNode���C���[��PDE�m�[�h���X�g�ɒǉ�
					vizTree.add( pdeIfNode );
					
					// else, else if �̑��݃`�F�b�N
					if( null != ifNode.getElseClause() )
					{
						IASTStatement elseClause = ifNode.getElseClause();
						if( elseClause instanceof IASTIfStatement )
						{
							TreatElseIfNode( vizTree, (IASTIfStatement)elseClause, doc, comments );
						}
						else
						{	
							TreatElseClause(vizTree, (IASTCompoundStatement)elseClause, doc, comments);
						}
					}
				}
				//----------------------------------------------------------------------------------
				// ���̑��̈�ʓI��node�̏ꍇ
				//----------------------------------------------------------------------------------
				else
				{
					PDEVizNodeClass curNode = new PDEVizNodeClass();
					curNode.node = iastNode;
					curNode.vizParts = new PDEVizPartsClass(iastNode, doc, comments, lNum_prvNd, lNum_nxtNd);
					
					//----------------------------------------------------------------------------------
					// �q�m�[�h�ւ̍ċA����
					//----------------------------------------------------------------------------------
					// topNode������node���g��CompoundStatement�ł���ꍇ�A
					// ���̎q�v�f�Ɋ܂܂��Statement�Q��visTree�̕��}�Ƃ��ēo�^�������̂ŁA
					// CompoundStatement��topNode�Ƃ��čċA�Ăяo������B
					if( iastNode instanceof IASTCompoundStatement )
					{
						System.out.println( "recurrent :C" );
						curNode.children = createPDEVizNodeTree(iastNode, doc, comments);
						
					}
					// topNode������node���g��CompoundStatement�łȂ��ꍇ
					else
					{
						// statement���q�v�f��CompoundStatement�����ꍇ�A
						// CompoundStatement�ȉ��Ɋ܂܂��Statement�Q��visTree�̕��}�Ƃ��ēo�^�������̂ŁA
						// createPDEVisTree()���ċA�Ăяo������B
						for( IASTNode statementChid : iastNode.getChildren() )
						{
							if( statementChid instanceof IASTCompoundStatement )
							{
								System.out.println( "recurrent :N" );
								curNode.children = createPDEVizNodeTree(statementChid, doc, comments);
							}
						}
					}
					
					//----------------------------------------------------------------------------------
					// �\���m�[�h�@PDEVizNodeClass�@�C���X�^���X�����X�g�ɕێ�
					//----------------------------------------------------------------------------------
					vizTree.add( curNode );
				}
			}
			//----------------------------------------------------------------------------------
			// Catch failure to convert child item to pdevNode
			//----------------------------------------------------------------------------------
			catch ( Exception e )
			{
				e.printStackTrace();
			}
		}

		return vizTree;
	}
	

	/** ************************************************************************************
	 * @brief if-else �m�[�h��else������
	 * 
	 ***************************************************************************************/
	static void TreatElseClause( ArrayList<PDEVizNodeClass> vizTree, IASTCompoundStatement elseClause, IDocument doc ,IASTComment[] comments )
	{
		// if-else �\���ւ̑Ή�
		System.out.println( "else Detected" );
		IASTFileLocation nodeLoc = elseClause.getFileLocation();
		TextSelection selNodeTxt = 
				new TextSelection(
				doc, 
				nodeLoc.getNodeOffset(), 
				nodeLoc.getNodeLength()
				);
		System.out.println( selNodeTxt.toString() );
		PDEVizNodeClass pdeElseNode = new PDEVizNodeClass();
		pdeElseNode.vizParts = new PDEVizPartsClass(elseClause, doc, comments, 0, 0);
		pdeElseNode.vizParts.blockComment = "";
		pdeElseNode.vizParts.inlineComment = "";
		pdeElseNode.vizParts.nodeText = "else";
		pdeElseNode.vizParts.pdeNodeType = "PdeElseStatement";
		
		// else�߂�Compound���m�[�h�����邽�߂̍ċA����
		pdeElseNode.children = createPDEVizNodeTree(elseClause, doc, comments);
		
		// else�߂�PDE�m�[�h���C���X�^���X�����X�g��topNode���C���[��PDE�m�[�h���X�g�ɒǉ�
		vizTree.add( pdeElseNode );
	}
	
	/** ************************************************************************************
	 * @brief if-else if �m�[�h�̍ċA����
	 * 
	 ***************************************************************************************/
	static void TreatElseIfNode( ArrayList<PDEVizNodeClass> vizTree, IASTIfStatement elifNode, IDocument doc ,IASTComment[] comments )
	{
		// if-else if �\���ւ̑Ή�
		System.out.println( "else if Detected" );
		IASTFileLocation nodeLoc = elifNode.getFileLocation();
		TextSelection selNodeTxt = 
				new TextSelection(
				doc, 
				nodeLoc.getNodeOffset(), 
				nodeLoc.getNodeLength()
				);
		System.out.println( selNodeTxt.toString() );
		
		PDEVizNodeClass pdeElifNode = new PDEVizNodeClass();
		pdeElifNode.node = elifNode;
		pdeElifNode.vizParts = new PDEVizPartsClass(elifNode, doc, comments, 0, 0);
		pdeElifNode.vizParts.nodeText = "else " + pdeElifNode.vizParts.nodeText;
		pdeElifNode.vizParts.pdeNodeType = "PdeElifStatement";
		
		// elseif�߂�Compound���m�[�h�����邽�߂̍ċA����
		pdeElifNode.children = createPDEVizNodeTree(elifNode.getThenClause(), doc, comments);
		
		// elseif�߂�PDE�m�[�h���C���X�^���X��if�߂Ɠ����C���[��PDE�m�[�h���X�g�ɒǉ�
		vizTree.add( pdeElifNode );
		
		// elseif�߂ɍX��ElseClause������ꍇ�͍ċA��������B
		if( null != elifNode.getElseClause() )
		{
			IASTStatement elseClause = elifNode.getElseClause();
			// ����elseif�����ւ̍ċA
			if( elseClause instanceof IASTIfStatement )
			{
				TreatElseIfNode(vizTree, (IASTIfStatement)elseClause, doc, comments);
			}
			// ����else�����ւ̍ċA
			else
			{
				TreatElseClause(vizTree, (IASTCompoundStatement)elseClause, doc, comments);
			}
		}
	}
	
	
	/****************************************************************************/
	/* �\�����̕����񏑂��o�������@�q�K�w���ċA�����o��								*/
	/****************************************************************************/
	String printToStringRecursive( int layer )
	{
		String out = "";
		String prefix = "";
		
		// layer�̐[���ɉ�����prefix�Ƃ��ăC���f���g���쐬����
		for( int i=0; i<layer; i++ )
		{
			prefix += "  ";
		}
		
		// ���g�̕\�������Z�b�g����
		out += printToString_withPrefix( prefix );
		
		
		System.out.print("children:" + children.size() );
		// �q�v�f�̕\�������Z�b�g���邽�߂ɍċA�Ăяo�������{����
		for( PDEVizNodeClass childPdevNode : children )
		{
			System.out.print("print_child\n");
			out += childPdevNode.printToStringRecursive(layer + 1);
		}
		
		return out;
	}
	
	/****************************************************************************/
	/* �\�����̕����񏑂��o�������@�v���t�B�N�X�w�肠��									*/
	/****************************************************************************/
	String printToString_withPrefix( String prefix )
	{
		String out = "";
		out += prefix + vizParts.blockComment	+ "\n" ;
		out += prefix + vizParts.nodeText		+ "\n" ;
		out += prefix + vizParts.inlineComment	+ "\n" ;
		out += "\n";
		return out;
	}

	org.w3c.dom.Element printXmlElem( org.w3c.dom.Document xml, String rId, String nId )
	{
		// fncNode���쐬����
		org.w3c.dom.Element elem = xml.createElement( "fncNode" );
		elem.setAttribute( "NodeText", this.vizParts.nodeText);
		elem.setAttribute("rnID", rId + nId);
		elem.setAttribute("astNodeType", this.vizParts.pdeNodeType );
		
		// commentNode���쐬����
		elem.setAttribute("inlineComment", this.vizParts.inlineComment );
		elem.setAttribute("blockComment", this.vizParts.blockComment );
		
		
		//------------------------------------------------------------------------
		// CPP�Ή��ɂ������āA�f�[�^�A�N�Z�X�̉�͈͂�U��߂Ă���
		//------------------------------------------------------------------------
//		try
//		{
//			// dataAccessNode���쐬����
//			for( Map.Entry<String, PDEVizDataAccessExpression> dataAccessEntry 
//					: this.vizParts.dataAccessMap.entrySet() )
//			{
//				PDEVizDataAccessExpression daExp = dataAccessEntry.getValue();
//				org.w3c.dom.Element dataAccessNode = xml.createElement( "dataAccessNode" );
//				dataAccessNode.setAttribute( "accessType",	daExp.accessType	);
//				dataAccessNode.setAttribute( "dataDec",		daExp.declineText	);
//				dataAccessNode.setTextContent( dataAccessEntry.getKey() );
//				elem.appendChild( dataAccessNode );
//			}
//		}
//		catch(Exception e)
//		{
//			e.printStackTrace();
//		}

		// �ċA�Ăяo��
		int nNum = 0;
		for( PDEVizNodeClass childPdevNode : children )
		{
			String nnId = nId + nNum;
			elem.appendChild( childPdevNode.printXmlElem(xml, rId, nnId) );
			nNum++;
		}
		return elem;
	}

}
