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
	// �A�N�Z�X��f�[�^�̒�`���
	public IASTNode 		expDeclineNode;
	public IASTNode			expSpcfyNode;
	public String			declineText;
	
	PDEVizDataAccessExpression( IASTNode node, String strFg )
	{
		expNode = node;
		accessType = strFg;
		
		//----------------------------
		// expNode�̐錾�ӏ����擾����
		//----------------------------
		// binding�̎擾
		IASTName iastName = LocalUtil.GetLowestName((IASTExpression)node);
		IBinding iBinding = iastName.resolveBinding();
		// binding -> Name�̐錾
		IASTName nameNodeOfDef = node.getTranslationUnit().getDeclarationsInAST(iBinding)[0];
		
		// �錾���S�̂��擾����
		IASTNode nameDefParent = nameNodeOfDef.getParent();
		IASTNode smpDecNode;
		IASTNode decSpcfyNode;
		// �ʏ�̐錾�̏ꍇ
		if( nameDefParent instanceof IASTDeclarator )
		{
			smpDecNode = nameDefParent.getParent();
			// [0]:Specifier 
			// [1]:Declarator �Ɖ��肷��
			decSpcfyNode = smpDecNode.getChildren()[0];
		}
		// typedef �̏ꍇ
		else if( nameDefParent instanceof IASTCompositeTypeSpecifier )
		{
			smpDecNode = nameDefParent.getParent();
			// [0]:CASTCompositeTypeSpecifier(�����\����)
			// [1]:Declarator(����) �Ɖ��肷��
			decSpcfyNode = smpDecNode.getChildren()[1];
		}
		else
		{
			smpDecNode = null;
			decSpcfyNode = null;
		}
		
		expDeclineNode = smpDecNode;
		expSpcfyNode = decSpcfyNode;
		
		// �錾���𕶎���Ƃ��ĕێ�
		IDocument docDecline = 	LocalUtil.GetIDocumentFrmPath(expDeclineNode.getFileLocation().getFileName());
		ITextSelection sel = new TextSelection(
				docDecline, 
				expDeclineNode.getFileLocation().getNodeOffset(), 
				expDeclineNode.getFileLocation().getNodeLength()
				);
		declineText = sel.getText();
	}
	
}
