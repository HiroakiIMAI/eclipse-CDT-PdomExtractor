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
		// topNodeのファイル名を取得
		String fileName_topNode = topNode.getFileLocation().getFileName();
		
		trsUnit = topNode.getTranslationUnit();
		document = doc;
		pdevTreeRoots = new ArrayList<PDEVizNodeClass>();
//		_dataAccessMap = new HashMap<String, String>();
//		_dataAccessMap = createDataAccessHashMap( topNode, _dataAccessMap );
		
		// topNodeがTranslationUnitでない場合は処理を抜ける
		if( !(topNode instanceof IASTTranslationUnit) )
		{
			throw new IllegalArgumentException();
		}
		
		//--------------------------------------------------------------------------
		// commentから、自身と異なるファイル(インクルードファイル)のコメントを取り除いた配列を作成する
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
				// com.getFileLocation().getFileName() が null pointer exception を吐くので
				// やり過ごして次のコメントを取り出す
			}
		}
		IASTComment[] commentsScreaned = arrList_comment.toArray( new IASTComment[arrList_comment.size()] );

	
		//--------------------------------------------------------------------------
		// 子要素探索ループ
		//--------------------------------------------------------------------------
		for( IASTNode node : topNode.getChildren() )
		{
			try
			{
				// topNodeと異なるファイル(includeファイル)に記載された要素は書き出さない
				if( node.getFileLocation().getFileName() == fileName_topNode )
				{
					
					//------------------------------------------------------------------
					// 変数や構造体宣言があればPDEVisTreeとして構築する
					//------------------------------------------------------------------
					if(node instanceof IASTSimpleDeclaration )
					{
						
					}

					
					//------------------------------------------------------------------
					// 関数宣言があれば内部の記述をPDEVisTreeとして構築する
					//------------------------------------------------------------------
					// nodeがFunctionDefinitionならば子要素にCompoundStatemnetを探す
					if(node instanceof IASTFunctionDefinition )
					{
						IASTFunctionDefinition fncDef = (IASTFunctionDefinition) node;
						

						//------------------------------------------------------------------
						// FunctionDefinition自身と直下のCompoundStatemaneを表示Treeとしてrootリストに加える。
						//------------------------------------------------------------------
						// 自身のnode化
						PDEVizNodeClass pdevNode = new PDEVizNodeClass();
						pdevNode.node = node;
						pdevNode.vizParts = new PDEVizPartsClass(fncDef, doc, commentsScreaned, 0, fncDef.getFileLocation().getStartingLineNumber());
						
						
						// 配下のCompoundのTree化
						for( IASTNode fncDefChild : fncDef.getChildren() )
						{
							if(fncDefChild instanceof IASTCompoundStatement)
							{
								IASTCompoundStatement compound = (IASTCompoundStatement) fncDefChild;
								// FunctionDefinition直下のCompoundStatemaneを表示Treeに加える。
								pdevNode.children = PDEVizNodeClass.createPDEVizNodeTree( compound, document, commentsScreaned );
								
							}
						}
						this.pdevTreeRoots.add(pdevNode);
					}
				}
			}
			catch(Exception e)
			{
				// node.getFileLocation().getFileName() が null pointer exception を吐くので
				// やり過ごして次のnodeを処理する
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
			// ファイルストリームを開く
			PrintStream ofs = new PrintStream( path, "UTF-8" );
			
			// xmlDomインスタンスを作成し、トップノードを登録する
			org.w3c.dom.Document xml = localXmlWriter.createXMLDocument( "pde" );
			org.w3c.dom.Element rootElem = xml.getDocumentElement();

			// 関数定義分だけループ
			int rNum = 0;
			for( PDEVizNodeClass pdevNode : pdevTreeRoots )
			{
				int nNum = 0;
				// 関数構造を再帰的にxmlに書き出す
				org.w3c.dom.Element elem = pdevNode.printXmlElem(xml, "r"+rNum, "n"+nNum);
				rootElem.appendChild( elem );
				rNum++;
			}
			
			// xml文字列をファイルストリームに出力する
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
//		// 再帰呼び出し
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
//		// 代入系演算子である場合
//		//	左辺値をWriteで登録し、右辺値に対して再帰呼び出しする
//		//---------------------------------------------------------
//		if( (binExp.getOperator() >= IASTBinaryExpression.op_assign )
//		&&	(binExp.getOperator() <= IASTBinaryExpression.op_binaryOrAssign )
//		)
//		{
//			// 左辺値をWriteアクセスとしてマップに登録する
//			dataAccessMap = registToDataAccessMap(dataAccessMap, left, "W");
//			
//			// 右辺値がBinaryExpressionでなければ右辺値をReadアクセスで追加する
//			if( !(right instanceof IASTBinaryExpression) )
//			{
//				dataAccessMap = registToDataAccessMap(dataAccessMap, right, "R=");
//			}
//			// 右辺値がBinaryExpressionならば再帰呼び出しする
//			else
//			{
//				dataAccessMap = registBinExpToDataAccessMap(dataAccessMap, (IASTBinaryExpression)right);
//			}
//		}
//		//----------------------------------------------------------
//		// 代入系演算子でない場合
//		//	右側項を上位としてネストされるので右方優先で登録し、左側項に対して再帰呼び出しする
//		//----------------------------------------------------------
//		else
//		{
//			//　右側項をReadで登録する
//			dataAccessMap = registToDataAccessMap(dataAccessMap, right, "Rr");
//			
//			// 左側項がBinaryExpressionでなければ左側項をReadアクセスで追加する
//			if( !(left instanceof IASTBinaryExpression) )
//			{
//				dataAccessMap = registToDataAccessMap(dataAccessMap, left, "Rl");
//			}
//			// 左側項がBinaryExpressionならば再帰呼び出しする
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
//		// リテラルの参照は登録しない
//		if( node instanceof IASTLiteralExpression )
//		{
//			return dataAccessMap;
//		}
//		
//		// 参照文字列をkeyとして取り出す
//		IASTFileLocation nodeLoc = node.getFileLocation();
//		TextSelection selNodeTxt = new TextSelection(
//			document, 
//			nodeLoc.getNodeOffset(), 
//			nodeLoc.getNodeLength()
//			);
//		String key = selNodeTxt.getText();
//		
//		//-----------------------------------
//		// keyに対してR, W, RW の値をセットする
//		//-----------------------------------
//		// 既存keyの場合
//		if( dataAccessMap.containsValue( key ))
//		{
//			// 既存内容と違うならR/Wアクセスと判断する
//			if(dataAccessMap.get(key) != strFg)
//			{
//				dataAccessMap.put( key, "RW" );
//			}
//		}
//		// 新規keyの場合
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
