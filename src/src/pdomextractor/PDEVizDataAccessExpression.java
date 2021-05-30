package pdomextractor;

import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;

public class PDEVizDataAccessExpression {
	public IASTNode 		expNode;
	public String			accessType;
	// アクセス先データの定義情報
	public IASTNode 		expDeclineNode;
	public IASTNode			expSpcfyNode;
	public String			declineText;
	
	PDEVizDataAccessExpression( IASTNode node, String strFg )
	{
		expNode = node;
		accessType = strFg;
		
		//----------------------------
		// expNodeの宣言箇所を取得する
		//----------------------------
		// bindingの取得
		IASTName iastName = LocalUtil.GetLowestName((IASTExpression)node);
		IBinding iBinding = iastName.resolveBinding();
		// binding -> Nameの宣言
		IASTName nameNodeOfDef = node.getTranslationUnit().getDeclarationsInAST(iBinding)[0];
		
		// 宣言文全体を取得する
		IASTNode nameDefParent = nameNodeOfDef.getParent();
		IASTNode smpDecNode;
		IASTNode decSpcfyNode;
		// 通常の宣言の場合
		if( nameDefParent instanceof IASTDeclarator )
		{
			smpDecNode = nameDefParent.getParent();
			// [0]:Specifier 
			// [1]:Declarator と仮定する
			decSpcfyNode = smpDecNode.getChildren()[0];
		}
		// typedef の場合
		else if( nameDefParent instanceof IASTCompositeTypeSpecifier )
		{
			smpDecNode = nameDefParent.getParent();
			// [0]:CASTCompositeTypeSpecifier(無名構造体)
			// [1]:Declarator(名称) と仮定する
			decSpcfyNode = smpDecNode.getChildren()[1];
		}
		else
		{
			smpDecNode = null;
			decSpcfyNode = null;
		}
		
		expDeclineNode = smpDecNode;
		expSpcfyNode = decSpcfyNode;
		
		// 宣言文を文字列として保持
		IDocument docDecline = 	LocalUtil.GetIDocumentFrmPath(expDeclineNode.getFileLocation().getFileName());
		ITextSelection sel = new TextSelection(
				docDecline, 
				expDeclineNode.getFileLocation().getNodeOffset(), 
				expDeclineNode.getFileLocation().getNodeLength()
				);
		declineText = sel.getText();
	}
	
}
