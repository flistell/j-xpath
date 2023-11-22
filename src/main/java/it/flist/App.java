package it.flist;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Command(name = "App", version = "App 1.0-SNAPSHOT", mixinStandardHelpOptions = true)
public class App implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(App.class.getCanonicalName());

    @Parameters(index = "0")
    String xpathExpr;

    @Parameters(index = "1")
    String filename;

    @Option(names = {"-N", "--no-namespace-aware"}, description = "Disable Namespace awareness (default: true)")
    boolean noNSAware = false;


    private static Set<String> getAllNamespaces(Element element) {
        Set<String> namespaces = new HashSet<>();
        collectNamespaces(element, namespaces);
        return namespaces;
    }

    private static void collectNamespaces(Node node, Set<String> namespaces) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            String namespaceURI = element.getNamespaceURI();
            if (namespaceURI != null && !namespaceURI.isEmpty()) {
                namespaces.add(namespaceURI);
            }

            // Recursively process child elements
            NodeList children = element.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                collectNamespaces(children.item(i), namespaces);
            }
        }
    }

    /**
     * Recursively renames the namespace of a node.
     * @param node the starting node.
     * @param namespace the new namespace. Supplying <tt>null</tt> removes the namespace.
     */
    public static void renameNamespaceRecursive(Node node, String namespace) {
        Document document = node.getOwnerDocument();
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            document.renameNode(node, namespace, node.getNodeName());
        }
        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); ++i) {
            renameNamespaceRecursive(list.item(i), namespace);
        }
    }

    @Override
    public void run() {
        Document doc = null;
        DocumentBuilder db;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xpath = xpf.newXPath();
        XPathExpression expression = null;
        File file = new File(filename);
        NodeList resultNodes;

        try {
            dbf.setNamespaceAware(!noNSAware);
            db = dbf.newDocumentBuilder();
            doc = db.parse(file);
            String rootNS = doc.getDocumentElement().getNamespaceURI();
            LOGGER.finest("Doc root: " + doc.getDocumentElement().getNodeName());
            LOGGER.finest("Doc namespace: " + rootNS);
            Set<String> namespaces = getAllNamespaces(doc.getDocumentElement());
            for (String ns : namespaces){
                LOGGER.finest("Doc namespace: " + ns);
            }
            renameNamespaceRecursive(doc.getDocumentElement(), "");
            LOGGER.finest("XPath xpf: " + xpf);
            LOGGER.finest("XPath xpathobj: " + xpath);
            LOGGER.finest("XPath doc: " + doc);
            LOGGER.finest("XPath String: " + xpathExpr);
            expression = xpath.compile(xpathExpr);
            //LOGGER.finest("XPath Expression: " + expression.toString());
            //resultString = (String) xpath.evaluate(xpathExpr, doc, XPathConstants.STRING);
            //LOGGER.finest("XPath Val: '" + resultString + "'");
            resultNodes = (NodeList) expression.evaluate(doc, XPathConstants.NODESET);
            LOGGER.finest("XPath Nodes: " + resultNodes.getLength());
            LOGGER.finest("XPath Nodes: " + resultNodes);
            // Use a Transformer for output
            Document outXml = db.newDocument();
            Element outRoot = outXml.createElementNS(rootNS, "xpath_result");
            outRoot.setAttribute("source", filename);
            outRoot.setAttribute("xpath_expression", xpathExpr);
            outXml.appendChild(outRoot);
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();

            for(int n = 0; n < resultNodes.getLength(); n++){
                Node node = outXml.importNode(resultNodes.item(n), true);
                LOGGER.finest("Node namespace: " + resultNodes.item(n).getNamespaceURI());
                LOGGER.finest("Node: " + resultNodes.item(n).toString());
                outRoot.appendChild(node);
            }
            DOMSource source = new DOMSource(outXml);
            StreamResult result = new StreamResult(System.out);
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(source, result);
        } catch (XPathExpressionException e) {
            LOGGER.severe("Error: no xpath expression compiled.");
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            LOGGER.severe("Error: parsing xml file.");
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } catch (TransformerException e) {
            LOGGER.severe("Error: building output.");
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            e.printStackTrace();
        }
        /*
         try {
            // Use a Transformer for output
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(System.out);
            transformer.transform(source, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        */
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new App()).execute(args));
    }

}
