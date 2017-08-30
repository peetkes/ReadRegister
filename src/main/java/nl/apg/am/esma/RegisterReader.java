package nl.apg.am.esma;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

/**
 * Created by PeetKes on 29/08/2017.
 */
public class RegisterReader {
    static final String registerUri = "https://registers.esma.europa.eu/solr/%s/select";
    //?q=%s&wt=xml&indent=true&rows=%s";
    static final String query = "({!parent%20which=%27type_s:parent%27})"; //q=
    static final String format = "xml";     // wt=xml
    static final String indent = "true";    // indent=true
    static final int rows = 100;        // &rows=100";
    static final String register = "esma_registers_mifid_sha";

    public static void  main(String[] args) {
        Client client = Client.create();
        int cnt = getRegisterCount(client, register, query);
        System.out.println("Count = " + cnt);
        Map<String, String> response = readRegister(client, register, query, 0, rows);
        System.out.println("Response = " + response);
    }

    private static int getRegisterCount(Client client, String register, String query) {
        String uri = String.format(registerUri, register);
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            WebResource webResource = client
                    .resource(uri)
                    .queryParam("q",query)
                    .queryParam("indent","true")
                    .queryParam("wt","xml")
                    .queryParam("rows","0");

            ClientResponse response = webResource.accept("application/xml")
                    .get(ClientResponse.class);
            if (response.getStatus() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + response.getStatus());
            }

            String output = response.getEntity(String.class);
            Document doc = dBuilder.parse(new InputSource(new StringReader(output)));
            doc.getDocumentElement().normalize();
            XPath xPath =  XPathFactory.newInstance().newXPath();
            XPathExpression expr = xPath.compile("/response/result/@numFound");
            Number count = (Number)expr.evaluate(doc, XPathConstants.NUMBER);

            System.out.println("Server response : \n");
            System.out.println(output);
            return count.intValue();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static Map<String, String> readRegister(Client client, String register, String query, int start, int rows) {
        String uri = String.format(registerUri, register);
        Map<String, String> result = new HashMap<String, String>();
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            WebResource webResource = client
                    .resource(uri)
                    .queryParam("q",query)
                    .queryParam("indent","true")
                    .queryParam("wt","xml")
                    .queryParam("start",String.valueOf(start))
                    .queryParam("rows", String.valueOf(rows));

            ClientResponse response = webResource.accept("application/xml")
                    .get(ClientResponse.class);
            if (response.getStatus() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + response.getStatus());
            }

            String output = response.getEntity(String.class);
            Document doc = dBuilder.parse(new InputSource(new StringReader(output)));
            doc.getDocumentElement().normalize();
            XPath xPath =  XPathFactory.newInstance().newXPath();
            XPathExpression expr = xPath.compile("/response/result/doc");
            NodeList docs = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
            for(Node n: asList(docs)) {
                XPathExpression idExpr = xPath.compile("/str[@name='id']/text()");
                String id = (String)idExpr.evaluate(n, XPathConstants.STRING);
                StringWriter buf = new StringWriter();
                Transformer xform = TransformerFactory.newInstance().newTransformer();
                xform.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                xform.setOutputProperty(OutputKeys.INDENT, "yes");
                xform.transform(new DOMSource(n), new StreamResult(buf));
                System.out.println(buf.toString());
                result.put(id, buf.toString());
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private static void test() {
        Runnable task = () -> {
            String threadName = Thread.currentThread().getName();
            System.out.println("Hello " + threadName);
        };

        task.run();

        Thread thread = new Thread(task);
        thread.start();

        System.out.println("Done!");
    }

    public static List<Node> asList(NodeList n) {
        return n.getLength()==0?
                Collections.<Node>emptyList(): new NodeListWrapper(n);
    }
    static final class NodeListWrapper extends AbstractList<Node>
            implements RandomAccess {
        private final NodeList list;
        NodeListWrapper(NodeList l) {
            list=l;
        }
        public Node get(int index) {
            return list.item(index);
        }
        public int size() {
            return list.getLength();
        }
    }
}
