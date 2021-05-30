package pdomextractor;

// インポート宣言
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;


public class localXmlWriter {
	// メソッド定義
	static public Document createXMLDocument(String root) throws ParserConfigurationException {
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder = factory.newDocumentBuilder();

	    DOMImplementation dom = builder.getDOMImplementation();
	    return dom.createDocument("", root, null);
	}

	// メソッド定義
	static public String createXMLString(Document document) throws TransformerException {
	    StringWriter writer = new StringWriter();
	    TransformerFactory factory = TransformerFactory.newInstance(); 
	    Transformer transformer = factory.newTransformer(); 

	    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
	    transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "2");

	    transformer.transform(new DOMSource(document), new StreamResult(writer)); 
	    return writer.toString();
	}
}
