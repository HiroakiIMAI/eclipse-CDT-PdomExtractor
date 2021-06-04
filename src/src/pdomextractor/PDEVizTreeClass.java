package pdomextractor;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

import javax.management.RuntimeErrorException;
import javax.security.auth.login.LoginException;
import javax.xml.transform.TransformerException;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;

public class PDEVizTreeClass {
	
	IASTTranslationUnit trsUnit;
	IDocument document;
	ArrayList<PDEVizNodeClass> pdevTreeRoots;
	//HashMap<String, String> dataAccessMapOfTree;
	
	PDEVizTreeClass( IASTNode topNode, IDocument doc ,IASTComment[] comments )
	{
		// topNode�̃t�@�C�������擾
		String fileName_topNode = topNode.getFileLocation().getFileName();
		
		trsUnit = topNode.getTranslationUnit();
		document = doc;
		pdevTreeRoots = new ArrayList<PDEVizNodeClass>();
//		_dataAccessMap = new HashMap<String, String>();
//		_dataAccessMap = createDataAccessHashMap( topNode, _dataAccessMap );
		
		// topNode��TranslationUnit�łȂ��ꍇ�͏����𔲂���
		if( !(topNode instanceof IASTTranslationUnit) )
		{
			throw new IllegalArgumentException();
		}
		
		//--------------------------------------------------------------------------
		// comment����A���g�ƈقȂ�t�@�C��(�C���N���[�h�t�@�C��)�̃R�����g����菜�����z����쐬����
		//--------------------------------------------------------------------------
		ArrayList<IASTComment> arrList_comment = new ArrayList<IASTComment>();
		for( IASTComment com : comments )
		{
			try
			{
				if( com.getFileLocation().getFileName() == topNode.getFileLocation().getFileName() )
				{
					arrList_comment.add(com);
				}
			}
			catch(Exception e)
			{
				// com.getFileLocation().getFileName() �� null pointer exception ��f���̂�
				// ���߂����Ď��̃R�����g�����o��
			}
		}
		IASTComment[] commentsScreaned = arrList_comment.toArray( new IASTComment[arrList_comment.size()] );

	
		//--------------------------------------------------------------------------
		// �q�v�f�T�����[�v
		//--------------------------------------------------------------------------
		for( IASTNode node : topNode.getChildren() )
		{
			try
			{
				// topNode�ƈقȂ�t�@�C��(include�t�@�C��)�ɋL�ڂ��ꂽ�v�f�͏����o���Ȃ�
				if( node.getFileLocation().getFileName() == fileName_topNode )
				{
					
					//------------------------------------------------------------------
					// �ϐ���\���̐錾�������PDEVisTree�Ƃ��č\�z����
					//------------------------------------------------------------------
					if(node instanceof IASTSimpleDeclaration )
					{
						
					}

					
					//------------------------------------------------------------------
					// �֐��錾������Γ����̋L�q��PDEVisTree�Ƃ��č\�z����
					//------------------------------------------------------------------
					// node��FunctionDefinition�Ȃ�Ύq�v�f��CompoundStatemnet��T��
					if(node instanceof IASTFunctionDefinition )
					{
						IASTFunctionDefinition fncDef = (IASTFunctionDefinition) node;
						

						//------------------------------------------------------------------
						// FunctionDefinition���g�ƒ�����CompoundStatemane��\��Tree�Ƃ���root���X�g�ɉ�����B
						//------------------------------------------------------------------
						// ���g��node��
						PDEVizNodeClass pdevNode = new PDEVizNodeClass();
						pdevNode.node = node;
						pdevNode.vizParts = new PDEVizPartsClass(fncDef, doc, commentsScreaned, 0, fncDef.getFileLocation().getStartingLineNumber());
						
						
						// �z����Compound��Tree��
						for( IASTNode fncDefChild : fncDef.getChildren() )
						{
							if(fncDefChild instanceof IASTCompoundStatement)
							{
								IASTCompoundStatement compound = (IASTCompoundStatement) fncDefChild;
								// FunctionDefinition������CompoundStatemane��\��Tree�ɉ�����B
								pdevNode.children = PDEVizNodeClass.createPDEVizNodeTree( compound, document, commentsScreaned );
								
							}
						}
						this.pdevTreeRoots.add(pdevNode);
					}
				}
			}
			catch(Exception e)
			{
				// node.getFileLocation().getFileName() �� null pointer exception ��f���̂�
				// ���߂����Ď���node����������
			}
		}
	}
	
	void printToFile()
	{
		try{
			
			PrintStream ofs = new PrintStream( "./pdeOutTest.txt" );
			ofs.printf("Roots:%d\n",pdevTreeRoots.size() );
			ofs.printf( "--Root------------------------------------------\n" );
			for( PDEVizNodeClass pdevNode : pdevTreeRoots )
			{
				String strTmp = pdevNode.printToStringRecursive(0);
				ofs.printf( strTmp + "\n" );
			}
			
			ofs.flush();
			ofs.close();
			System.out.println( "end printToFile" );
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	void printXml( String path )
	{
		try {
			// �t�@�C���X�g���[�����J��
			PrintStream ofs = new PrintStream( path, "UTF-8" );
			
			// xmlDom�C���X�^���X���쐬���A�g�b�v�m�[�h��o�^����
			org.w3c.dom.Document xml = localXmlWriter.createXMLDocument( "pde" );
			org.w3c.dom.Element rootElem = xml.getDocumentElement();

			// �֐���`���������[�v
			int rNum = 0;
			for( PDEVizNodeClass pdevNode : pdevTreeRoots )
			{
				int nNum = 0;
				// �֐��\�����ċA�I��xml�ɏ����o��
				org.w3c.dom.Element elem = pdevNode.printXmlElem(xml, "r"+rNum, "n"+nNum);
				rootElem.appendChild( elem );
				rNum++;
			}
			
			// xml��������t�@�C���X�g���[���ɏo�͂���
			ofs.print( localXmlWriter.createXMLString( xml ) );
			ofs.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
//	HashMap<String, String> createDataAccessHashMap( IASTNode node, HashMap<String, String> dataAccessMap )
//	{
//		for( IASTNode child : node.getChildren() )
//		{
//			if( child instanceof IASTBinaryExpression )
//			{
//				registBinExpToDataAccessMap(dataAccessMap, (IASTBinaryExpression)child);
//			}
//		}
//		
//		// �ċA�Ăяo��
//		for( IASTNode child : node.getChildren() )
//		{
//			dataAccessMap = createDataAccessHashMap(child, dataAccessMap);
//		}
//		
//		return dataAccessMap;
//	}
//	
//	HashMap<String, String> registBinExpToDataAccessMap( 
//			HashMap<String, String> dataAccessMap, 
//			IASTBinaryExpression binExp
//			)
//	{
//		IASTNode left = binExp.getChildren()[0];
//		IASTNode right = binExp.getChildren()[1];
//		//dataAccessMap = registToDataAccessMap(dataAccessMap, right, "R");
//		
//		//---------------------------------------------------------
//		// ����n���Z�q�ł���ꍇ
//		//	���Ӓl��Write�œo�^���A�E�Ӓl�ɑ΂��čċA�Ăяo������
//		//---------------------------------------------------------
//		if( (binExp.getOperator() >= IASTBinaryExpression.op_assign )
//		&&	(binExp.getOperator() <= IASTBinaryExpression.op_binaryOrAssign )
//		)
//		{
//			// ���Ӓl��Write�A�N�Z�X�Ƃ��ă}�b�v�ɓo�^����
//			dataAccessMap = registToDataAccessMap(dataAccessMap, left, "W");
//			
//			// �E�Ӓl��BinaryExpression�łȂ���ΉE�Ӓl��Read�A�N�Z�X�Œǉ�����
//			if( !(right instanceof IASTBinaryExpression) )
//			{
//				dataAccessMap = registToDataAccessMap(dataAccessMap, right, "R=");
//			}
//			// �E�Ӓl��BinaryExpression�Ȃ�΍ċA�Ăяo������
//			else
//			{
//				dataAccessMap = registBinExpToDataAccessMap(dataAccessMap, (IASTBinaryExpression)right);
//			}
//		}
//		//----------------------------------------------------------
//		// ����n���Z�q�łȂ��ꍇ
//		//	�E��������ʂƂ��ăl�X�g�����̂ŉE���D��œo�^���A�������ɑ΂��čċA�Ăяo������
//		//----------------------------------------------------------
//		else
//		{
//			//�@�E������Read�œo�^����
//			dataAccessMap = registToDataAccessMap(dataAccessMap, right, "Rr");
//			
//			// ��������BinaryExpression�łȂ���΍�������Read�A�N�Z�X�Œǉ�����
//			if( !(left instanceof IASTBinaryExpression) )
//			{
//				dataAccessMap = registToDataAccessMap(dataAccessMap, left, "Rl");
//			}
//			// ��������BinaryExpression�Ȃ�΍ċA�Ăяo������
//			else
//			{
//				dataAccessMap = registBinExpToDataAccessMap(dataAccessMap, (IASTBinaryExpression)left);
//			}
//		}
//		
//		return dataAccessMap;
//	}
//	
//	
//	HashMap<String, String> registToDataAccessMap( HashMap<String, String> dataAccessMap, IASTNode node, String strFg )
//	{
//		// ���e�����̎Q�Ƃ͓o�^���Ȃ�
//		if( node instanceof IASTLiteralExpression )
//		{
//			return dataAccessMap;
//		}
//		
//		// �Q�ƕ������key�Ƃ��Ď��o��
//		IASTFileLocation nodeLoc = node.getFileLocation();
//		TextSelection selNodeTxt = new TextSelection(
//			document, 
//			nodeLoc.getNodeOffset(), 
//			nodeLoc.getNodeLength()
//			);
//		String key = selNodeTxt.getText();
//		
//		//-----------------------------------
//		// key�ɑ΂���R, W, RW �̒l���Z�b�g����
//		//-----------------------------------
//		// ����key�̏ꍇ
//		if( dataAccessMap.containsValue( key ))
//		{
//			// �������e�ƈႤ�Ȃ�R/W�A�N�Z�X�Ɣ��f����
//			if(dataAccessMap.get(key) != strFg)
//			{
//				dataAccessMap.put( key, "RW" );
//			}
//		}
//		// �V�Kkey�̏ꍇ
//		else
//		{
//			dataAccessMap.put( key, strFg );
//		}
//		
//		return dataAccessMap;
//	}
//	
//	String printDataAccessHashMapToString()
//	{
//		String str = "";
//		for(  String key : _dataAccessMap.keySet() )
//		{
//			str += _dataAccessMap.get(key).toString() + " : " + key  + "\n";
//		}		
//		return str;
//	}
}
