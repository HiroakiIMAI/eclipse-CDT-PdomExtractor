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
	 * @brief ツールバーボタン押下時メインアクション
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
		
		// 解析対象のデバッグ出力
		System.out.print(
			"actSrcPath = " + domRsrc.srcPath.toString() + "\n"
			+ "actCPrjName = " + CMdl.prj.getElementName() + "\n"
			);

		//-------------------------------
		// PdomExtractorのメイン処理を実施する
		//-------------------------------
		PDEVizTreeClass vizTree = new PDEVizTreeClass( astTrsUnit, doc ,astTrsUnit.getComments() );
		System.out.println( "PdomExtracted.xml" );
		vizTree.printXml( "PdomExtracted.xml" );

		// sequence to get TranslationUnit 
		//<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
		
		
		//------------------------------------------------------------------------
		// request.txt に記載されたファイル一覧に対して同様の処理を実施する。
		//------------------------------------------------------------------------
		ArrayList<String> requests = GerRequestFileLines();
		for( String reqFileName : requests )
		{
			//-------------------------------
			// get resources requested
			//-------------------------------
			// 一般化Eclipseソースファイル情報の取得
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
			// C/C++特化ソースファイル情報の取得
			CModelObjects reqCMdl = new CModelObjects(workspaceRoot, reqDomRsrc.prj, reqDomRsrc.srcFile);
			IASTTranslationUnit reqTrsUnitAST	= reqCMdl.GetAST();

			// 解析対象のデバッグ出力
			System.out.print(
				"reqSrcPath = " + reqDomRsrc.srcPath.toString() + "\n"
				+ "reqCPrjName = " + reqCMdl.prj.getElementName() + "\n"
				);
			
			//-------------------------------
			// PdomExtractorのメイン処理を実施する
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
	 * [機能概要]
	 * Eclipse の  一般的なファイルリソースを扱うオブジェクトをまとめたクラス
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
	 * [機能概要]
	 * Eclipse の C/C++ に特化したドキュメントオブジェクトモデルをまとめたクラス
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
	 * @brief ストリーム→文字列変換処理
	 * 
	 * [機能概要]
	 * InputStreamを受けて、エンコードを考慮してStringに変換する。
	 * 対応文字コードはS-JISとUTF-8。
	 * 
	 ***************************************************************************************/
	public static String GetEncodedString_FromInputStream(InputStream in) throws IOException {
		// バイトストリームを用意する
		byte[] b = new byte[1024];
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int len;
		// 引数で受けたストリームの全行をバイトストリームに渡す
		while ((len = in.read(b)) != -1) {
		    out.write(b, 0, len);
		}
		
		// バイトストリームの内容を各文字コードのエンコード済みStringに変換する
		byte[] bytes = out.toByteArray();
		System.out.println( "size:" + bytes.length );
		String str_u = new String( bytes, "UTF-8" );
		String str_s = new String( bytes, "Shift_JIS" );
		
		//------------------------------------------------------------------
		// エンコード済StringをByte列に復元し、元の引数ストリームから得たバイト列に一致する場合、
		// エンコードに用いた文字コードが適合すると判断し、エンコード済Stringを返す。
		//------------------------------------------------------------------
		// UTF-8 適合チェック
		if( Arrays.equals( bytes, str_u.getBytes("UTF-8") ) )
		{
			System.out.println( " *** UTF-8 File Detected!!! *** " );
			return str_u;
		}
		// S-JIS 適合チェック
		if( Arrays.equals( bytes, str_s.getBytes("Shift_JIS") ) )
		{
			System.out.println( " *** Shift_JIS File Detected!!! *** " );
			return str_s;
		}
		return out.toString();
	}
	
	
	
	/** ************************************************************************************
	 * @brief request.txt 読出し処理
	 * 
	 * [機能概要]
	 * request.txtを開き、各行をセットしたString配列を返す。
	 * 
	 ***************************************************************************************/
	public ArrayList<String> GerRequestFileLines() {
		ArrayList<String> retList = new ArrayList<String>();
		File requestFile = new File("request.txt");
		
		// ファイルの存在チェック
		if (!requestFile.exists())
		{
			// エラー処理
			System.out.println( "request.txt is not exist." );
		}
		else
		{
			// ファイルが存在する場合
			BufferedReader reader = null;
			// バッファ読出し処理
			try 
			{
				// readerオブジェクトの生成
				reader = new BufferedReader(
							new InputStreamReader(
								new FileInputStream(requestFile), 
								"UTF-8"
							)
						);
				
				// 全行読出し処理
				String line;
				while((line = reader.readLine()) != null) 
				{
					// ファイルから読みだした文字列をArrayListに詰める
					retList.add(line);
				}
			} 
			// バッファ操作の例外処理
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


