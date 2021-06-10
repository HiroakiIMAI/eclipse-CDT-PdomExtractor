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
	// 表示対象ノード
	public IASTNode node;
	// 表示対象ノードから抽出した表示データを保持する構造クラス
	public PDEVizPartsClass vizParts;
	// 表示対象子ノードのリスト
	public ArrayList<PDEVizNodeClass> children;
	
	PDEVizNodeClass()
	{
		children = new ArrayList<PDEVizNodeClass>();
	}

	static ArrayList<PDEVizNodeClass> createPDEVizNodeTree( IASTNode topNode, IDocument doc ,IASTComment[] comments )
	{
		ArrayList<PDEVizNodeClass> vizTree = new ArrayList<PDEVizNodeClass>();
		
		// topNodeの子要素を順に表示ノード化するためのループ
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
				// nodeに関連するコメントの抽出範囲行数を取得する
				//----------------------------------------------------------------------------------
				int lNum_prvNd = -1;
				int lNum_nxtNd = -1;
				// 特殊な場合の処理
				// 先頭statementの場合
				if( j == 0 )
				{
					// 親ノード(Compoundを想定する)の開始行をセットする
					lNum_prvNd = children[j].getParent().getFileLocation().getStartingLineNumber();
				}
				// 特殊な場合の処理
				// 最終statementの場合
				if( j == children.length -1 )
				{
					// 自身の終了行をセットする
					lNum_nxtNd = children[j].getFileLocation().getEndingLineNumber();
				}
				// それ以外
				// 特殊な場合に当てはまらなかったので、直前statementの終了行をセットする
				if( lNum_prvNd == -1 ){	lNum_prvNd = children[i].getFileLocation().getEndingLineNumber();}
				// 特殊な場合に当てはまらなかったので、直後statementの開始行をセットする
				if( lNum_nxtNd == -1 ){ lNum_nxtNd = children[k].getFileLocation().getStartingLineNumber();}
				
				//----------------------------------------------------------------------------------
				// 表示ノード　PDEVizNodeClass　インスタンスを作成
				//----------------------------------------------------------------------------------
				IASTNode iastNode = children[j];
				
				//----------------------------------------------------------------------------------
				// ノード種別による特別処理
				//----------------------------------------------------------------------------------
				// if Statement だった場合、 if, if-else, if-elseif の構造に対応するために特別処理を実施する
				// 
				// [Eclipse CDT AST仕様]
				// if-else構造の場合、AST内でelse節はStatementノードを持たない。
				// 代わりにif節が2つのCompoundを持ち、2つ目のCompoundがelseの内容を保持する
				// 
				// if-elseif 構造の場合、AST内ではIfStatementが直下にIfStatementを持ち、
				// これがelseif節を表現する。よって、elseifの度に、ASTの階層は1つ深くなる。
				// 
				// [PDEの出力仕様]
				// if-else構造の場合、else節のStatementをifと同じ階層に出力する。
				// Statementノードの内容はとりあえず適当に作っておく
				// 
				// if-elseif構造の場合、elseif節のStatementをifと同じ階層に出力する。
				// AST上でifStatementの直下にあるelseif相当のifStatementにelseifである情報を付加して
				// 先頭のifStatementと同じ階層に移動させる。
				//----------------------------------------------------------------------------------
				if( iastNode instanceof IASTIfStatement )
				{
					IASTIfStatement ifNode = (IASTIfStatement)iastNode;
					
					// if節を表現するPDEノードを生成する
					PDEVizNodeClass pdeIfNode = new PDEVizNodeClass();
					pdeIfNode.node = ifNode;							// nodeはとりあえずifNodeを設定しておく
					pdeIfNode.vizParts = new PDEVizPartsClass(ifNode, doc, comments, lNum_prvNd, lNum_nxtNd);

					// if節のCompoundをノード化するための再帰する
					pdeIfNode.children = createPDEVizNodeTree(ifNode.getThenClause(), doc, comments);

					// if節のPDEノードをインスタンスをリストにtopNodeレイヤーのPDEノードリストに追加
					vizTree.add( pdeIfNode );
					
					// else, else if の存在チェック
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
				// その他の一般的なnodeの場合
				//----------------------------------------------------------------------------------
				else
				{
					PDEVizNodeClass curNode = new PDEVizNodeClass();
					curNode.node = iastNode;
					curNode.vizParts = new PDEVizPartsClass(iastNode, doc, comments, lNum_prvNd, lNum_nxtNd);
					
					//----------------------------------------------------------------------------------
					// 子ノードへの再帰処理
					//----------------------------------------------------------------------------------
					// topNode直下のnode自身がCompoundStatementである場合、
					// その子要素に含まれるStatement群をvisTreeの分枝として登録したいので、
					// CompoundStatementをtopNodeとして再帰呼び出しする。
					if( iastNode instanceof IASTCompoundStatement )
					{
						System.out.println( "recurrent :C" );
						curNode.children = createPDEVizNodeTree(iastNode, doc, comments);
						
					}
					// topNode直下のnode自身がCompoundStatementでない場合
					else
					{
						// statementが子要素にCompoundStatementを持つ場合、
						// CompoundStatement以下に含まれるStatement群をvisTreeの分枝として登録したいので、
						// createPDEVisTree()を再帰呼び出しする。
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
					// 表示ノード　PDEVizNodeClass　インスタンスをリストに保持
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
	 * @brief if-else ノードのelse側処理
	 * 
	 ***************************************************************************************/
	static void TreatElseClause( ArrayList<PDEVizNodeClass> vizTree, IASTCompoundStatement elseClause, IDocument doc ,IASTComment[] comments )
	{
		// if-else 構造への対応
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
		
		// else節のCompoundをノード化するための再帰する
		pdeElseNode.children = createPDEVizNodeTree(elseClause, doc, comments);
		
		// else節のPDEノードをインスタンスをリストにtopNodeレイヤーのPDEノードリストに追加
		vizTree.add( pdeElseNode );
	}
	
	/** ************************************************************************************
	 * @brief if-else if ノードの再帰処理
	 * 
	 ***************************************************************************************/
	static void TreatElseIfNode( ArrayList<PDEVizNodeClass> vizTree, IASTIfStatement elifNode, IDocument doc ,IASTComment[] comments )
	{
		// if-else if 構造への対応
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
		
		// elseif節のCompoundをノード化するための再帰する
		pdeElifNode.children = createPDEVizNodeTree(elifNode.getThenClause(), doc, comments);
		
		// elseif節のPDEノードをインスタンスをif節と同レイヤーのPDEノードリストに追加
		vizTree.add( pdeElifNode );
		
		// elseif節に更にElseClauseがある場合は再帰処理する。
		if( null != elifNode.getElseClause() )
		{
			IASTStatement elseClause = elifNode.getElseClause();
			// 次のelseif処理への再帰
			if( elseClause instanceof IASTIfStatement )
			{
				TreatElseIfNode(vizTree, (IASTIfStatement)elseClause, doc, comments);
			}
			// 次のelse処理への再帰
			else
			{
				TreatElseClause(vizTree, (IASTCompoundStatement)elseClause, doc, comments);
			}
		}
	}
	
	
	/****************************************************************************/
	/* 表示情報の文字列書き出し処理　子階層を再帰書き出し								*/
	/****************************************************************************/
	String printToStringRecursive( int layer )
	{
		String out = "";
		String prefix = "";
		
		// layerの深さに応じてprefixとしてインデントを作成する
		for( int i=0; i<layer; i++ )
		{
			prefix += "  ";
		}
		
		// 自身の表示情報をセットする
		out += printToString_withPrefix( prefix );
		
		
		System.out.print("children:" + children.size() );
		// 子要素の表示情報をセットするために再帰呼び出しを実施する
		for( PDEVizNodeClass childPdevNode : children )
		{
			System.out.print("print_child\n");
			out += childPdevNode.printToStringRecursive(layer + 1);
		}
		
		return out;
	}
	
	/****************************************************************************/
	/* 表示情報の文字列書き出し処理　プレフィクス指定あり									*/
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
		// fncNodeを作成する
		org.w3c.dom.Element elem = xml.createElement( "fncNode" );
		elem.setAttribute( "NodeText", this.vizParts.nodeText);
		elem.setAttribute("rnID", rId + nId);
		elem.setAttribute("astNodeType", this.vizParts.pdeNodeType );
		
		// commentNodeを作成する
		elem.setAttribute("inlineComment", this.vizParts.inlineComment );
		elem.setAttribute("blockComment", this.vizParts.blockComment );
		
		
		//------------------------------------------------------------------------
		// CPP対応にあたって、データアクセスの解析は一旦やめておく
		//------------------------------------------------------------------------
//		try
//		{
//			// dataAccessNodeを作成する
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

		// 再帰呼び出し
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
