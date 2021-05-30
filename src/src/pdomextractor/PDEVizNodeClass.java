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
							
				IASTNode iastNode = children[j];
				
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
				PDEVizNodeClass curNode = new PDEVizNodeClass();
				curNode.node = iastNode;
				curNode.vizParts = new PDEVizPartsClass(iastNode, doc, comments, lNum_prvNd, lNum_nxtNd);
				
				//----------------------------------------------------------------------------------
				// 子ノードへの再帰処理
				//----------------------------------------------------------------------------------
				// statementが子要素にCompoundStatementを持つ場合、
				// CompoundStatement以下に含まれるStatement群をvisTreeの分枝として登録したいので、
				// createPDEVisTree()を再帰呼び出しする。
				for( IASTNode statementChid : iastNode.getChildren() )
				{
					if( statementChid instanceof IASTCompoundStatement )
					{
						curNode.children = createPDEVizNodeTree(statementChid, doc, comments);
					}
				}
				
				//----------------------------------------------------------------------------------
				// 表示ノード　PDEVizNodeClass　インスタンスをリストに保持
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
		if ( this.node != null ){	
			elem.setAttribute("astNodeType", this.node.getClass().getSimpleName() );	
		}else{
			// 何もしない
		}
		
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
