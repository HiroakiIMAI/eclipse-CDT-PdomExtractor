package pdomextractor;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.cdt.core.dom.IName;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;

public class PDEVizPartsClass {
	
	public IASTNode iastNode;
	public String pdeNodeType;
	public String nodeText;
	public String inlineComment;
	public String blockComment;
	public ArrayList<IASTComment> inlineComments;
	public ArrayList<IASTComment> blockComments;
//	public HashMap<String, String> dataAccessMap;
	public HashMap<String, PDEVizDataAccessExpression> dataAccessMap;
	 
	PDEVizPartsClass( IASTNode node, IDocument doc, IASTComment[] comments, int lNum_CmSt, int lNum_CmEd )
	{

		// �����o�ϐ��̏�����
		inlineComment  = "";
		blockComment   = "";
		inlineComments = new ArrayList<IASTComment>();
		blockComments  = new ArrayList<IASTComment>();
//		dataAccessMap = new HashMap<String, String>();
		dataAccessMap = new HashMap<String, PDEVizDataAccessExpression>();
//		createDataAccessHashMap(doc, node);
		
		try 
		{
			//----------------------------------------
			// StatementNode��ێ�����
			//----------------------------------------
			this.iastNode = node;
			this.pdeNodeType = node.getClass().getSimpleName();
			
			//----------------------------------------
			// �R�����g�𒊏o����
			//----------------------------------------
			// �m�[�h�̍s�ԍ����擾����B
			int nodeLine = node.getFileLocation().getStartingLineNumber();
			// �R�����g���s�A�����Ă��邩�𔻒f���邽�߂̊�s��nodeLine�ŏ���������
			int refLine = nodeLine;
			
			// Comments���~���Ő��`�T������
			for( int i=comments.length-1; i>=0; i-- )
			{
				// �R�����g�s�ԍ����擾����
				int cmntStLine = comments[i].getFileLocation().getStartingLineNumber();
				int cmntEdLine = comments[i].getFileLocation().getEndingLineNumber();
				//---------------------------------------------------
				//�@�s�ԍ����J�����g�m�[�h�ƈ�v����R�����g��inlineComments�ɉ�����
				//---------------------------------------------------
				if( cmntStLine == nodeLine )
				{
					this.inlineComments.add( comments[i] );
				}
				//---------------------------------------------------
				// �R�����g�̍s�Ɗ�s(�����l��nodeLine)������s
				// �������̓R�����g�s����s�̒��O�̍s�ł���ꍇ�A
				//�@(���A�����w�肳�ꂽ�R�����g���o�s�͈͈ȓ��̏ꍇ)
				// �R�����g��blockComments�ɉ����A��s���R�����g�s�ōX�V����B
				//---------------------------------------------------
				else if(( (refLine - cmntEdLine) <= 1 )
				&& ( cmntStLine > lNum_CmSt )
				&& ( cmntStLine < nodeLine )
				)
				{
//					//-----------------------------------------------
//					// �O��ǉ��R�����g�s����1�s�ȏ�󂢂Ă���ꍇ�A
//					// �O��܂ł̃R�����g�͎��m�[�h�̃R�����g�Ȃ̂Ŏ̂Ă�B
//					//-----------------------------------------------
//					// �O��ǉ��R�����g�����݂���ꍇ
//					if( this.blockComments.size() > 0)
//					{
//						// �O��R�����g�̊J�n�s���擾
//						int prvCmtIdx = this.blockComments.size()-1;
//						int prvCmtSt = this.blockComments.get(prvCmtIdx).getFileLocation().getStartingLineNumber();
//						// �P�s�ȏ�󂢂Ă���?
//						if( (prvCmtSt - cmntEdLine) > 1 )
//						{
//							this.blockComments.clear();
//						}
//					}
					
					// �R�����g���u���b�N�R�����g�ɒǉ�
					this.blockComments.add( comments[i] );
					
					// �R�����g�s����s�����Ⴂ�ꍇ�A
					// �m�[�h�s����̃R�����g��ǂݐi�߂Ă���̂ŁA
					// ��s���R�����g�s�ōX�V����
					if( cmntStLine < refLine )
					{
						refLine = cmntStLine;
					}
				}
			}
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		//---------------------------------------------------
		// inlineComments��String��A���A�ێ�����
		//---------------------------------------------------
		for( int i=this.inlineComments.size()-1; i>=0; i-- )
		{
			try 
			{
				// �R�����g�������A��
				this.inlineComment += new String( this.inlineComments.get(i).getComment() );
			} 
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		
		//---------------------------------------------------
		// blockComments��String��A���A�ێ�����
		//---------------------------------------------------
		for( int i=this.blockComments.size()-1; i>=0; i-- )
		{
			try
			{
				// �R�����g�������A��
				this.blockComment += new String( this.blockComments.get(i).getComment() );

				//�@�u���b�N�R�����g�������R�����g����\�������Ă���
				// ���O�̃R�����g�ƍs�ԍ����قȂ�ꍇ�́A�R�����g������ɉ��s��}������B
				if( i>1 )
				{
					int line    = this.blockComments.get(i  ).getFileLocation().getStartingLineNumber();
					int linePrv = this.blockComments.get(i-1).getFileLocation().getStartingLineNumber();
					if( line != linePrv )
					{
						this.blockComment += "\n";
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
		//----------------------------------------
		// nodeText�𒊏o����
		//----------------------------------------
		// �q�v�f�̒���Compound���܂܂�邩���`�F�b�N����B
		boolean flag_haveCompound = false;
		int offsetCompSt = 0;
		for( IASTNode child : node.getChildren() )
		{
			// �q�v�f��Compound�ł���ꍇ
			if (child instanceof IASTCompoundStatement )
			{
				flag_haveCompound = true;
				offsetCompSt = child.getFileLocation().getNodeOffset();
			}
			// �q�v�f��Compound�łȂ��ꍇ
			else
			{
				// �������Ȃ�
			}
		}
		
		
		//-----------------------
		// Try to extract NodeText
		//-----------------------
		try
		{
			IASTFileLocation nodeLoc = node.getFileLocation();
			TextSelection selNodeTxt;
			
			if( node instanceof IASTIfStatement )
			{
				IASTIfStatement ifNode = (IASTIfStatement) node;
				nodeLoc = ifNode.getConditionExpression().getFileLocation();
				selNodeTxt = 
						new TextSelection(
						doc, 
						nodeLoc.getNodeOffset(), 
						nodeLoc.getNodeLength()
						);
				
				nodeText = "if (" + selNodeTxt.getText() + ")";
			}
			else
			// �q�v�f��Compound���܂܂��ꍇ�́A
			// inlineComment���n�܂�܂ł�nodeText�Ƃ���B
			// (�����_�ł́A�����s�ɂ킽��if���Ȃǂ̐��m�Ȓ��o�͒��߂�)
			if( flag_haveCompound )
			{
//				int extLength = nodeLoc.getNodeLength(); 
//				if( inlineComments.size() != 0 )
//				{
//					IASTFileLocation inlineCommentLoc = inlineComments.get( inlineComments.size()-1 ).getFileLocation();
//					extLength = inlineCommentLoc.getNodeOffset() - nodeLoc.getNodeOffset();
//				}
				int extLength = offsetCompSt - nodeLoc.getNodeOffset();
				selNodeTxt = 
						new TextSelection(
						doc, 
						nodeLoc.getNodeOffset(), 
						extLength
						);
				nodeText = new String( selNodeTxt.getText() );
			}
			// �q�v�f��Compound���܂܂�Ȃ��ꍇ�́A
			// �P����nodeText���쐬����B
			else
			{
				selNodeTxt = 
						new TextSelection(
						doc, 
						nodeLoc.getNodeOffset(), 
						nodeLoc.getNodeLength()
						);
				nodeText = new String( selNodeTxt.getText() );
			}		
		}
		//-----------------------
		// Catch failure to extract NodeText
		//-----------------------
		catch ( Exception e )
		{
			nodeText = "Couldn't get nodeText";
		}
		
	}
	
	
	//----------------------------------------
	// dataAccessMap���쐬����
	//----------------------------------------
	HashMap<String, PDEVizDataAccessExpression> createDataAccessHashMap( 
			IDocument doc,
			IASTNode node
//			HashMap<String, String> dataAccessMap 
			)
	{
		for( IASTNode child : node.getChildren() )
		{
			if( child instanceof IASTBinaryExpression )
			{
				registBinExpToDataAccessMap(doc, dataAccessMap, (IASTBinaryExpression)child);
			}
		}
		
		// �q�v�f�̂��߂̍ċA�Ăяo��
		for( IASTNode child : node.getChildren() )
		{
			if( !(node instanceof IASTCompoundStatement) )
			{
				dataAccessMap = createDataAccessHashMap(doc, child);
			}
		}
		
		return dataAccessMap;
	}
	
	
	HashMap<String, PDEVizDataAccessExpression> registBinExpToDataAccessMap( 
			IDocument doc,
			HashMap<String, PDEVizDataAccessExpression> dataAccessMap, 
			IASTBinaryExpression binExp
			)
	{
		IASTNode left = binExp.getChildren()[0];
		IASTNode right = binExp.getChildren()[1];
		//dataAccessMap = registToDataAccessMap(dataAccessMap, right, "R");
		
		//---------------------------------------------------------
		// ����n���Z�q�ł���ꍇ
		//	���Ӓl��Write�œo�^���A�E�Ӓl�ɑ΂��čċA�Ăяo������
		//---------------------------------------------------------
		if( (binExp.getOperator() >= IASTBinaryExpression.op_assign )
		&&	(binExp.getOperator() <= IASTBinaryExpression.op_binaryOrAssign )
		)
		{
			// ���Ӓl��Write�A�N�Z�X�Ƃ��ă}�b�v�ɓo�^����
			dataAccessMap = registToDataAccessMap(doc,dataAccessMap, left, "W");
			
			// �E�Ӓl��BinaryExpression�łȂ���ΉE�Ӓl��Read�A�N�Z�X�Œǉ�����
			if( !(right instanceof IASTBinaryExpression) )
			{
				dataAccessMap = registToDataAccessMap(doc,dataAccessMap, right, "R");
			}
			// �E�Ӓl��BinaryExpression�Ȃ�΍ċA�Ăяo������
			else
			{
				dataAccessMap = registBinExpToDataAccessMap(doc, dataAccessMap, (IASTBinaryExpression)right);
			}
		}
		//----------------------------------------------------------
		// ����n���Z�q�łȂ��ꍇ
		//	�E��������ʂƂ��ăl�X�g�����̂ŉE���D��œo�^���A�������ɑ΂��čċA�Ăяo������
		//----------------------------------------------------------
		else
		{
			//�@�E������Read�œo�^����
			dataAccessMap = registToDataAccessMap(doc, dataAccessMap, right, "R");
			
			// ��������BinaryExpression�łȂ���΍�������Read�A�N�Z�X�Œǉ�����
			if( !(left instanceof IASTBinaryExpression) )
			{
				dataAccessMap = registToDataAccessMap(doc,dataAccessMap, left, "R");
			}
			// ��������BinaryExpression�Ȃ�΍ċA�Ăяo������
			else
			{
				dataAccessMap = registBinExpToDataAccessMap(doc, dataAccessMap, (IASTBinaryExpression)left);
			}
		}
		
		return dataAccessMap;
	}

	HashMap<String, PDEVizDataAccessExpression> registToDataAccessMap( 
			IDocument doc, 
			HashMap<String, PDEVizDataAccessExpression> dataAccessMap, 
			IASTNode node, 
			String strFg )
	{
		// ���e�����̎Q�Ƃ͓o�^���Ȃ�
		if( node instanceof IASTLiteralExpression )
		{
			return dataAccessMap;
		}
		
		// �Q�ƕ������key�Ƃ��Ď��o��
		IASTFileLocation nodeLoc = node.getFileLocation();
		TextSelection selNodeTxt = new TextSelection(
			doc, 
			nodeLoc.getNodeOffset(), 
			nodeLoc.getNodeLength()
			);
		String key = selNodeTxt.getText();
		
		//-----------------------------------
		// key�ɑ΂���R, W, RW �̒l���Z�b�g����
		//-----------------------------------
		// ����key�̏ꍇ
		if( dataAccessMap.containsValue( key ))
		{
			PDEVizDataAccessExpression daExp = dataAccessMap.get(key);
			// �A�N�Z�X�^�C�v���������e�ƈႤ�Ȃ�R/W�A�N�Z�X�ɏ㏑������
			if(daExp.accessType != strFg)
			{
				daExp.accessType = "RW";
				dataAccessMap.put( key, daExp );
			}
		}
		// �V�Kkey�̏ꍇ
		else
		{
			PDEVizDataAccessExpression daExp = new PDEVizDataAccessExpression( node, strFg );
			dataAccessMap.put( key, daExp );
		}
		
		return dataAccessMap;
	}
	
//	String printDataAccessHashMapToString()
//	{
//		String str = "";
//		for(  String key : _dataAccessMap.keySet() )
//		{
//			str += _dataAccessMap.get(key).toString() + " : " + key  + "\n";
//		}		
//		return str;
//	}
	
	
	
//	HashMap<String, String> createDataAccessHashMap2( 
//			IDocument doc, 
//			IASTNode node, 
//			HashMap<String, String> dataAccessMap 
//			)
//	{
//		for( IASTNode child : node.getChildren() )
//		{
//			if( child instanceof IASTIdExpression )
//			{
//				if( ((IASTIdExpression)child).getExpressionType() ==  )
//				registExpressionsToDataAccessMap(doc, dataAccessMap, (IASTIdExpression)child);
//			}
//		}
//		
//		// �q�v�f�̂��߂̍ċA�Ăяo��
//		for( IASTNode child : node.getChildren() )
//		{
//			dataAccessMap = createDataAccessHashMap2(doc, child, dataAccessMap);
//		}
//		
//		return dataAccessMap;
//	}
	
//	void registExpressionsToDataAccessMap( 
//			IDocument doc,
//			HashMap<String, String> dataAccessMap,
//			IASTIdExpression idExpNode
//			)
//	{
//		
//	}	
}
