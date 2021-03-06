package pdomextractor;

import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ISourceEntry;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.core.parser.util.ASTPrinter;
import org.eclipse.cdt.internal.core.model.CElement;
import org.eclipse.cdt.internal.ui.editor.CDocumentProvider;
import org.eclipse.cdt.internal.ui.editor.CEditor;
import org.eclipse.cdt.internal.ui.text.CWordFinder;
import org.eclipse.cdt.ui.ICEditor;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

// self added
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.texteditor.ITextEditor;
//import org.w3c.dom.Document;
















import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.scene.Node;

public class PdomExtractorMain implements IWorkbenchWindowActionDelegate {
	
	/** ************************************************************************************
	 * @brief ?c?[???o?[?{?^???????????C???A?N?V????
	 * 
	 ***************************************************************************************/
	public void run(IAction action) {
		
		//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
		// sequence to get TranslationUnit 
		//-------------------------------
		// get active editor
		//-------------------------------
		IWorkbenchPage wrkBnchWndPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IEditorPart actEditor = wrkBnchWndPage.getActiveEditor();
		ITextEditor actTxtEditor = (ITextEditor)actEditor;
		
		//-------------------------------
		// get Document related to active editor
		//-------------------------------
		IDocument doc = actTxtEditor.getDocumentProvider().getDocument(actTxtEditor.getEditorInput());
		
		//-------------------------------
		// get resources related to active editor
		//-------------------------------
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		EclipseDomResources domRsrc = new EclipseDomResources( workspaceRoot, actEditor.getTitleToolTip() );

		
		//-------------------------------
		// get CModel of active resources
		//-------------------------------
		CModelObjects CMdl = new CModelObjects( workspaceRoot, domRsrc.prj, domRsrc.srcFile );
		IASTTranslationUnit astTrsUnit = CMdl.GetAST();
		
		// ???????????f?o?b?O?o??
		System.out.print(
			"actSrcPath = " + domRsrc.srcPath.toString() + "\n"
			+ "actCPrjName = " + CMdl.prj.getElementName() + "\n"
			);

//		// AST???f?o?b?O?o??(EclipseCDT????)
//		try 
//		{
//			PrintStream ofs = new PrintStream( "AstPrint.txt", "UTF-8" );
//			ASTPrinter.print(astTrsUnit, ofs);
//			ofs.close();
//		}
//		catch (Exception e1) 
//		{
//			e1.printStackTrace();
//		}

		//-------------------------------
		// PdomExtractor?????C???????????{????
		//-------------------------------
		PDEVizTreeClass vizTree = new PDEVizTreeClass( astTrsUnit, doc ,astTrsUnit.getComments() );
		System.out.println( "PdomExtracted.xml" );
		vizTree.printXml( "PdomExtracted.xml" );

		// sequence to get TranslationUnit 
		//<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
		
		
		//------------------------------------------------------------------------
		// request.txt ???L?????????t?@?C?????????????????l???????????{?????B
		//------------------------------------------------------------------------
		ArrayList<String> requests = GerRequestFileLines();
		for( String reqFileName : requests )
		{
			//-------------------------------
			// get resources requested
			//-------------------------------
			// ??????Eclipse?\?[?X?t?@?C????????????
			EclipseDomResources reqDomRsrc = new EclipseDomResources( workspaceRoot, reqFileName );
			String str = new String();
			try 
			{
				str = GetEncodedString_FromInputStream( reqDomRsrc.srcFile.getContents() );
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
				return;
			}
			
			//-------------------------------
			// get Document
			//-------------------------------
			IDocument	reqDoc 	= new Document( str );
			
			//-------------------------------
			// get CModel of resources
			//-------------------------------
			// C/C++?????\?[?X?t?@?C????????????
			CModelObjects reqCMdl = new CModelObjects(workspaceRoot, reqDomRsrc.prj, reqDomRsrc.srcFile);
			IASTTranslationUnit reqTrsUnitAST	= reqCMdl.GetAST();

			// ???????????f?o?b?O?o??
			System.out.print(
				"reqSrcPath = " + reqDomRsrc.srcPath.toString() + "\n"
				+ "reqCPrjName = " + reqCMdl.prj.getElementName() + "\n"
				);
			
			//-------------------------------
			// PdomExtractor?????C???????????{????
			//-------------------------------
			PDEVizTreeClass reqVizTree = new PDEVizTreeClass( reqTrsUnitAST, reqDoc ,reqTrsUnitAST.getComments() );
			reqFileName = reqFileName.substring( reqFileName.lastIndexOf("/")+1 );
			System.out.println( "PdomExtracted_" + reqFileName + ".xml" );
			reqVizTree.printXml( "PdomExtracted_" + reqFileName + ".xml" );
		}
	}
	
	
	/** ************************************************************************************
	 * @brief Eclipse Document Resource Objects Class
	 * 
	 * [?@?\?T?v]
	 * Eclipse ??  ?????I???t?@?C?????\?[?X???????I?u?W?F?N?g???????????N???X
	 * 
	 ***************************************************************************************/
	class EclipseDomResources
	{
		IPath		srcPath;
		IFile		srcFile;
		IProject	prj;
		
		EclipseDomResources( IWorkspaceRoot wsRoot, String fileName )
		{
			srcPath	= new Path( fileName );
			srcFile	= wsRoot.getFile( srcPath );
			prj		= srcFile.getProject();
		}
	}
	
	
	
	/** ************************************************************************************
	 * @brief Eclipse C/C++ Document Model Objects Class
	 * 
	 * [?@?\?T?v]
	 * Eclipse ?? C/C++ ???????????h?L???????g?I?u?W?F?N?g???f?????????????N???X
	 * 
	 ***************************************************************************************/
	class CModelObjects
	{
		ICModel				model;
		ICProject			prj;
		ICElement			srcElem;
		
		CModelObjects( IWorkspaceRoot wsRoot, IProject iprj, IFile srcFile )
		{
			try
			{
				model		= CoreModel.create(wsRoot);
				prj			= model.getCProject( iprj.getName());
				srcElem		= prj.findElement( srcFile.getProjectRelativePath() );  // this apparently throws exception for non-source files instead of returning null
			}
			catch( Exception e )
			{
				e.printStackTrace();
			}
		}
		
		
		public IASTTranslationUnit GetAST()
		{
			if( !( srcElem instanceof ITranslationUnit) )
			{
				MessageDialog.openInformation(
				        null,
				        "error",
				        srcElem.getElementName() + " is not an instance of TranslationUnit"
				        );
			}
			
			try 
			{
				return ((ITranslationUnit)srcElem).getAST();
			} 
			catch (CoreException e)
			{
				e.printStackTrace();
				return null;
			}
		}
	}
	
	
	
	/** ************************************************************************************
	 * @brief ?X?g???[??????????????????
	 * 
	 * [?@?\?T?v]
	 * InputStream?????????A?G???R?[?h???l??????String???????????B
	 * ?????????R?[?h??S-JIS??UTF-8?B
	 * 
	 ***************************************************************************************/
	public static String GetEncodedString_FromInputStream(InputStream in) throws IOException {
		// ?o?C?g?X?g???[?????p??????
		byte[] b = new byte[1024];
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int len;
		// ?????????????X?g???[?????S?s???o?C?g?X?g???[?????n??
		while ((len = in.read(b)) != -1) {
		    out.write(b, 0, len);
		}
		
		// ?o?C?g?X?g???[???????e???e?????R?[?h???G???R?[?h????String??????????
		byte[] bytes = out.toByteArray();
		System.out.println( "size:" + bytes.length );
		String str_u = new String( bytes, "UTF-8" );
		String str_s = new String( bytes, "Shift_JIS" );
		
		//------------------------------------------------------------------
		// ?G???R?[?h??String??Byte???????????A?????????X?g???[???????????o?C?g???????v?????????A
		// ?G???R?[?h???p?????????R?[?h???K???????????f???A?G???R?[?h??String???????B
		//------------------------------------------------------------------
		// UTF-8 ?K???`?F?b?N
		if( Arrays.equals( bytes, str_u.getBytes("UTF-8") ) )
		{
			System.out.println( " *** UTF-8 File Detected!!! *** " );
			return str_u;
		}
		// S-JIS ?K???`?F?b?N
		if( Arrays.equals( bytes, str_s.getBytes("Shift_JIS") ) )
		{
			System.out.println( " *** Shift_JIS File Detected!!! *** " );
			return str_s;
		}
		return out.toString();
	}
	
	
	
	/** ************************************************************************************
	 * @brief request.txt ???o??????
	 * 
	 * [?@?\?T?v]
	 * request.txt???J???A?e?s???Z?b?g????String?z?????????B
	 * 
	 ***************************************************************************************/
	public ArrayList<String> GerRequestFileLines() {
		ArrayList<String> retList = new ArrayList<String>();
		File requestFile = new File("request.txt");
		
		// ?t?@?C?????????`?F?b?N
		if (!requestFile.exists())
		{
			// ?G???[????
			System.out.println( "request.txt is not exist." );
		}
		else
		{
			// ?t?@?C????????????????
			BufferedReader reader = null;
			// ?o?b?t?@???o??????
			try 
			{
				// reader?I?u?W?F?N?g??????
				reader = new BufferedReader(
							new InputStreamReader(
								new FileInputStream(requestFile), 
								"UTF-8"
							)
						);
				
				// ?S?s???o??????
				String line;
				while((line = reader.readLine()) != null) 
				{
					// ?t?@?C????????????????????????ArrayList???l????
					retList.add(line);
				}
			} 
			// ?o?b?t?@?????????O????
			catch (Exception e) 
			{
				
				System.out.println( "cannot open request.txt" );
				e.printStackTrace();
			} 
		}
		return retList;
	}


	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub

	}

	public void dispose() {
		// TODO Auto-generated method stub

	}

	public void init(IWorkbenchWindow window) {
		// TODO Auto-generated method stub

	}
	
//	IResource extractSelection(ISelection sel) {
//		if (!(sel instanceof IStructuredSelection))
//			return null;
//		IStructuredSelection ss = (IStructuredSelection) sel;
//		Object element = ss.getFirstElement();
//		if (element instanceof IResource)
//			return (IResource) element;
//		if (!(element instanceof IAdaptable))
//			return null;
//		IAdaptable adaptable = (IAdaptable)element;
//		Object adapter = adaptable.getAdapter(IResource.class);
//		return (IResource) adapter;
//	}
//	
//	String cvtNodesToString( IDocument doc, IASTNode[] nodes, int layer )
//	{
//		String str = "";
//		for( IASTNode node : nodes )
//		{
//			try{
//				//--------------------------------------
//				// description of node
//				//--------------------------------------
//				for( int i=0; i<layer; i++ )
//					str += "    ";
//				
//				int				offset			= node.getSyntax().getOffset();
//				
//				IASTComment[] comments =  node.getTranslationUnit().getComments();
//				
//				
//				
//				
//				String			strSrcPath		= node.getTranslationUnit().getFilePath();
//				IWorkspaceRoot 	workspaceRoot	= ResourcesPlugin.getWorkspace().getRoot();
//				IPath			srcPath			= new Path( strSrcPath );
//				IFile			srcFile			= workspaceRoot.getFile( srcPath );
//				
//				IRegion reg= CWordFinder.findWord(doc, node.getFileLocation().getNodeOffset());
//				ITextSelection selNodeTxt = new TextSelection(doc, reg.getOffset(), reg.getLength());
//				//ISearchQuery searchJob = null;
//				//searchJob = createQuery((ICElement)(node.getTranslationUnit().getParent()) , selNodeTxt);
//				
//				str += layer 
//						+ "??" 
//						+ node.getSyntax().getType() + ", "
//						+ node.getSyntax().toString() + ", "
//						+ node.getSyntax().getOffset() + ", "
//						+ node.getSyntax().getLength() + ", "
//						+ node.getFileLocation().getStartingLineNumber() + ", "
//						+ "\n";
//				
//				//--------------------------------------
//				// recursive description children nodes
//				//--------------------------------------
//				if( node.getChildren().length > 0 )
//				{
//					str += cvtNodesToString(doc, node.getChildren(), layer+1);
//				}
//				else
//				{
//					// do nothing
//				}
//					
//			}
//			catch(Exception e )
//			{
//				// do nothing
//				str += "exception\n";
//			}
//		}
//		return str;
//	}
	
	
	
}


