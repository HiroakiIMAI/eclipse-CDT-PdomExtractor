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

		// メンバ変数の初期化
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
			// StatementNodeを保持する
			//----------------------------------------
			this.iastNode = node;
			this.pdeNodeType = node.getClass().getSimpleName();
			
			//----------------------------------------
			// コメントを抽出する
			//----------------------------------------
			// ノードの行番号を取得する。
			int nodeLine = node.getFileLocation().getStartingLineNumber();
			// コメントが行連続しているかを判断するための基準行をnodeLineで初期化する
			int refLine = nodeLine;
			
			// Commentsを降順で線形探索する
			for( int i=comments.length-1; i>=0; i-- )
			{
				// コメント行番号を取得する
				int cmntStLine = comments[i].getFileLocation().getStartingLineNumber();
				int cmntEdLine = comments[i].getFileLocation().getEndingLineNumber();
				//---------------------------------------------------
				//　行番号がカレントノードと一致するコメントはinlineCommentsに加える
				//---------------------------------------------------
				if( cmntStLine == nodeLine )
				{
					this.inlineComments.add( comments[i] );
				}
				//---------------------------------------------------
				// コメントの行と基準行(初期値はnodeLine)が同一行
				// もしくはコメント行が基準行の直前の行である場合、
				//　(かつ、引数指定されたコメント抽出行範囲以内の場合)
				// コメントをblockCommentsに加え、基準行をコメント行で更新する。
				//---------------------------------------------------
				else if(( (refLine - cmntEdLine) <= 1 )
				&& ( cmntStLine > lNum_CmSt )
				&& ( cmntStLine < nodeLine )
				)
				{
//					//-----------------------------------------------
//					// 前回追加コメント行から1行以上空いている場合、
//					// 前回までのコメントは次ノードのコメントなので捨てる。
//					//-----------------------------------------------
//					// 前回追加コメントが存在する場合
//					if( this.blockComments.size() > 0)
//					{
//						// 前回コメントの開始行を取得
//						int prvCmtIdx = this.blockComments.size()-1;
//						int prvCmtSt = this.blockComments.get(prvCmtIdx).getFileLocation().getStartingLineNumber();
//						// １行以上空いている?
//						if( (prvCmtSt - cmntEdLine) > 1 )
//						{
//							this.blockComments.clear();
//						}
//					}
					
					// コメントをブロックコメントに追加
					this.blockComments.add( comments[i] );
					
					// コメント行が基準行よりも若い場合、
					// ノード行直上のコメントを読み進めているので、
					// 基準行をコメント行で更新する
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
		// inlineCommentsのStringを連結、保持する
		//---------------------------------------------------
		for( int i=this.inlineComments.size()-1; i>=0; i-- )
		{
			try 
			{
				// コメント文字列を連結
				this.inlineComment += new String( this.inlineComments.get(i).getComment() );
			} 
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		
		//---------------------------------------------------
		// blockCommentsのStringを連結、保持する
		//---------------------------------------------------
		for( int i=this.blockComments.size()-1; i>=0; i-- )
		{
			try
			{
				// コメント文字列を連結
				this.blockComment += new String( this.blockComments.get(i).getComment() );

				//　ブロックコメントが複数コメントから構成されれていて
				// 直前のコメントと行番号が異なる場合は、コメント文字列に改行を挿入する。
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
		// nodeTextを抽出する
		//----------------------------------------
		// 子要素の中にCompoundが含まれるかをチェックする。
		boolean flag_haveCompound = false;
		int offsetCompSt = 0;
		for( IASTNode child : node.getChildren() )
		{
			// 子要素がCompoundである場合
			if (child instanceof IASTCompoundStatement )
			{
				flag_haveCompound = true;
				offsetCompSt = child.getFileLocation().getNodeOffset();
			}
			// 子要素がCompoundでない場合
			else
			{
				// 何もしない
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
			// 子要素にCompoundが含まれる場合は、
			// inlineCommentが始まるまでをnodeTextとする。
			// (現時点では、複数行にわたるif文などの正確な抽出は諦める)
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
			// 子要素にCompoundが含まれない場合は、
			// 単純にnodeTextを作成する。
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
	// dataAccessMapを作成する
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
		
		// 子要素のための再帰呼び出し
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
		// 代入系演算子である場合
		//	左辺値をWriteで登録し、右辺値に対して再帰呼び出しする
		//---------------------------------------------------------
		if( (binExp.getOperator() >= IASTBinaryExpression.op_assign )
		&&	(binExp.getOperator() <= IASTBinaryExpression.op_binaryOrAssign )
		)
		{
			// 左辺値をWriteアクセスとしてマップに登録する
			dataAccessMap = registToDataAccessMap(doc,dataAccessMap, left, "W");
			
			// 右辺値がBinaryExpressionでなければ右辺値をReadアクセスで追加する
			if( !(right instanceof IASTBinaryExpression) )
			{
				dataAccessMap = registToDataAccessMap(doc,dataAccessMap, right, "R");
			}
			// 右辺値がBinaryExpressionならば再帰呼び出しする
			else
			{
				dataAccessMap = registBinExpToDataAccessMap(doc, dataAccessMap, (IASTBinaryExpression)right);
			}
		}
		//----------------------------------------------------------
		// 代入系演算子でない場合
		//	右側項を上位としてネストされるので右方優先で登録し、左側項に対して再帰呼び出しする
		//----------------------------------------------------------
		else
		{
			//　右側項をReadで登録する
			dataAccessMap = registToDataAccessMap(doc, dataAccessMap, right, "R");
			
			// 左側項がBinaryExpressionでなければ左側項をReadアクセスで追加する
			if( !(left instanceof IASTBinaryExpression) )
			{
				dataAccessMap = registToDataAccessMap(doc,dataAccessMap, left, "R");
			}
			// 左側項がBinaryExpressionならば再帰呼び出しする
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
		// リテラルの参照は登録しない
		if( node instanceof IASTLiteralExpression )
		{
			return dataAccessMap;
		}
		
		// 参照文字列をkeyとして取り出す
		IASTFileLocation nodeLoc = node.getFileLocation();
		TextSelection selNodeTxt = new TextSelection(
			doc, 
			nodeLoc.getNodeOffset(), 
			nodeLoc.getNodeLength()
			);
		String key = selNodeTxt.getText();
		
		//-----------------------------------
		// keyに対してR, W, RW の値をセットする
		//-----------------------------------
		// 既存keyの場合
		if( dataAccessMap.containsValue( key ))
		{
			PDEVizDataAccessExpression daExp = dataAccessMap.get(key);
			// アクセスタイプが既存内容と違うならR/Wアクセスに上書きする
			if(daExp.accessType != strFg)
			{
				daExp.accessType = "RW";
				dataAccessMap.put( key, daExp );
			}
		}
		// 新規keyの場合
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
//		// 子要素のための再帰呼び出し
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
