package pdomextractor;

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.ExpansionOverlapsBoundaryException;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
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
							
				IASTNode iastNode = children[j];
				
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
				PDEVizNodeClass curNode = new PDEVizNodeClass();
				curNode.node = iastNode;
				curNode.vizParts = new PDEVizPartsClass(iastNode, doc, comments, lNum_prvNd, lNum_nxtNd);
				
				//----------------------------------------------------------------------------------
				// �q�m�[�h�ւ̍ċA����
				//----------------------------------------------------------------------------------
				// statement���q�v�f��CompoundStatement�����ꍇ�A
				// CompoundStatement�ȉ��Ɋ܂܂��Statement�Q��visTree�̕��}�Ƃ��ēo�^�������̂ŁA
				// createPDEVisTree()���ċA�Ăяo������B
				for( IASTNode statementChid : iastNode.getChildren() )
				{
					if( statementChid instanceof IASTCompoundStatement )
					{
						curNode.children = createPDEVizNodeTree(statementChid, doc, comments);
					}
				}
				
				//----------------------------------------------------------------------------------
				// �\���m�[�h�@PDEVizNodeClass�@�C���X�^���X�����X�g�ɕێ�
				//----------------------------------------------------------------------------------
				vizTree.add( curNode );
			}
			//----------------------------------------------------------------------------------
			// Catch failure to convert child item to pdevNode
			//----------------------------------------------------------------------------------
			catch ( Exception e )
			{
				
			}
		}

		return vizTree;
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
		if ( this.node != null ){	
			elem.setAttribute("astNodeType", this.node.getClass().getSimpleName() );	
		}else{
			// �������Ȃ�
		}
		
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
