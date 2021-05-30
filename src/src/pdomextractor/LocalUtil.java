package pdomextractor;

import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTName;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;

final public class LocalUtil {
	
	static IDocument GetIDocumentFrmPath( String strPath )
	{
		Path path = new Path (strPath);
		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		try {
			manager.connect( path , null);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ITextFileBuffer buffer = manager.getTextFileBuffer(path);
		return buffer.getDocument();
	}
	
	static IASTName GetLowestName( IASTExpression expNode )
	{
		IASTName name = new CASTName();
		
		if( expNode instanceof IASTIdExpression )
		{			
			name = (IASTName)((IASTIdExpression)expNode).getChildren()[0];
//			for( IASTNode child :((IASTIdExpression)expNode).getChildren() )
//			{
//				if( child instanceof IASTName )
//				{
//					name = (IASTName)child;
//					break;
//				}
//			}
		}
		else if (expNode instanceof IASTFieldReference)
		{
			name = (IASTName)((IASTFieldReference)expNode).getChildren()[1];
		}
		else
		{
			name = null;
		}
		
		return name;
	}
}
