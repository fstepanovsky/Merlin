package cz.mzk.osdd.merlin.models;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

/**
 * Created by Jakub Kremlacek on 4.4.17.
 */
public class Utils {

    public static Document getDocumentFromURL(String url) throws IOException, SAXException, ParserConfigurationException {
        if (url == null) return null;

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new URL(url).openStream());

        return doc;
    }

    /**
     * request for MARC record from Aleph XServer with retrieval of Sysno and Base
     *
     * @param signature
     * @return
     */
    public static Pair<String, String> getSysnoWithBaseFromAleph(String signature) {
        Document doc;
        String sysno;
        String base = null;

        if (signature == null) return null;

        try {
            doc = getDocumentFromURL("http://aleph.mzk.cz/X?base=mzk01&op=find&request=sig=" + signature);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        String set_number = doc.getElementsByTagName("set_number").item(0).getTextContent();
        String no_entries = doc.getElementsByTagName("no_entries").item(0).getTextContent();

        try {
            doc = getDocumentFromURL("http://aleph.mzk.cz/X?op=present&set_no=" + set_number + "&set_entry=" + no_entries + "&format=marc");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        sysno = doc.getElementsByTagName("doc_number").item(0).getTextContent();

        NodeList field = doc.getElementsByTagName("subfield");

        for (int i = 0; i < field.getLength(); i++) {
            String label = ((Element) field.item(i)).getAttribute("label");

            if (label.equals("l")) {
                base = ((Element) field.item(i)).getTextContent();
            }
        }

        if (sysno == null || !base.startsWith("MZK0")) return null;

        return Pair.create(sysno, base);
    }

    public static String getSignatureFromRootObject(Path directory) {

        File root = directory.resolve(directory.toFile().getName() + ".xml").toFile();

        Document doc = null;

        try {
            doc = getDocumentFromFile(root);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        NodeList msl = doc.getElementsByTagName("mods:shelfLocator");

        if (msl.getLength() < 1) {
            System.err.println("Signature not found within parent (" + root.getName() + ") record");
            return null;
        }

        return msl.item(0).getTextContent();
    }

    public static Document getDocumentFromFile(File file) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

        return dBuilder.parse(file);
    }

    /**
     * getElementById is not working because of ID chars
     *
     * @param id to be filtered from document. Note that ID must be unique otherwise Element returned will be the first found.
     * @return element with specified ID
     */
    public static Element filterDatastreamFromDocument(Document doc, String id) {
        NodeList nl = doc.getElementsByTagName("datastreamVersion");

        for (int i = 0; i < nl.getLength(); i++) {
            if (((Element) nl.item(i)).getAttribute("ID").equals(id)) return (Element) nl.item(i);
        }

        return null;
    }
}
