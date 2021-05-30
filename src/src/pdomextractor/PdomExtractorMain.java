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
import org.eclipse.cdt.internal.ui.editor.CEditor;
import org.eclipse.cdt.internal.ui.text.CWordFinder;
import org.eclipse.cdt.ui.ICEditor;
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


import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.Node;

public class PdomExtractorMain implements IWorkbenchWindowActionDelegate {

	public void run(IAction action) {
		
		//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
		// sequence to get TranslationUnit 
		ITranslationUnit 	actTrsUnit;
		//-------------------------------
		// get active editor
		//-------------------------------
		IWorkbenchPage wrkBnchWndPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IEditorPart actEditor = wrkBnchWndPage.getActiveEditor();
		ITextEditor actTxtEditor = (ITextEditor)actEditor;
		IDocument doc = actTxtEditor.getDocumentProvider().getDocument(actTxtEditor.getEditorInput());
		//-------------------------------
		// get resources related to active editor
		//-------------------------------
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IPath		actSrcPath	= new Path( actEditor.getTitleToolTip() );
		IFile		actSrcFile	= workspaceRoot.getFile( actSrcPath );
		IProject	actPrj		= actSrcFile.getProject();
		//-------------------------------
		// get CModel of active resources
		//-------------------------------
		try{
			ICModel				cModel		= CoreModel.create(workspaceRoot);
			ICProject			actCPrj		= cModel.getCProject( actPrj.getName() );
			ICElement			actCSrcElem = actCPrj.findElement( actSrcFile.getProjectRelativePath() );  // this apparently throws exception for non-source files instead of returning null
			if( !( actCSrcElem instanceof ITranslationUnit) )
			{
				MessageDialog.openInformation(
				        null,
				        "error",
				        "this source is not an instance of TranslationUnit"
				        );
				return;
			}
			actTrsUnit	= (ITranslationUnit)actCSrcElem;  // not clear whether we need to check for null or instanceof

			MessageDialog.openInformation(
			        null,
			        "createCProject",
			        "actEditorToolTip = " + actEditor.getTitleToolTip() + "\n"
			        + "actSrcPath = " + actSrcPath.toString() + "\n"
			        + "actCPrjName = " + actCPrj.getElementName() + "\n"
			        + "actTrsUnit = " + actTrsUnit.getElementName() + "\n"
			        );
			
			//-------------------------------
			// list up nodes
			//->>>---------------------------
			try{
				IASTTranslationUnit astTrsUnit = actTrsUnit.getAST();
				
				System.out.println( "start" );
				// デバッグ出力
				File AstPrintFile = new File( "AstPrint.txt" );								// 出力ファイル生成
				PrintStream AstPrintStream = new PrintStream( AstPrintFile );				// ファイルへのストリームを生成
				//ASTPrinter.print(astTrsUnit, AstPrintStream);  //System.out);				//　ストリームにASTNodeを書き出す
				AstPrintStream.close();
				
				// PdomExtractorのメイン処理を実施する
				PDEVizTreeClass vizTree = new PDEVizTreeClass( astTrsUnit, doc ,astTrsUnit.getComments() );
				// PdomExtractorの解析結果をxmlに書き出す
				System.out.println( "xml" );
				vizTree.printXml( "PdomExtracted.xml" );
			}catch( Exception e )
			{
				
			}
			//-<<<---------------------------
		} 
		catch( CModelException e ){
			MessageDialog.openInformation(
			        null,
			        "exception in creating CModel",
			        "selected source is not a CElement"
			        );
		}
		// sequence to get TranslationUnit 
		//<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
		
		
		
		
		// TODO Auto-generated method stub
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
//						+ "└" 
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


