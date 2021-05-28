package cz.mzk.osdd.merlin.models;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;

/**
 * Created by Jakub Kremlacek on 4.4.17.
 */
public class Utils {

    public static final int RETRY_COUNT = 3;
    public static final String[] ALEPH_BASES = {"mzk01", "mzk03"};
    public static final String DOC_NOT_FOUND_EXCEPTION_MSG = "Could not find document with specified signature";

    public static Document getDocumentFromURL(String url) throws IOException, SAXException, ParserConfigurationException {
        if (url == null) return null;

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();

        return db.parse(new URL(url).openStream());
    }

    /**
     * request for MARC record from Aleph XServer with retrieval of Sysno and Base
     *
     * @param mods
     * @return
     */
    public static Pair<String, String> getSysnoWithBaseFromAleph(Mods mods) throws IOException {
        String sysno;
        String base = null;

        if (mods == null) return null;

        List<Document> infoDocs = getResponseFromAleph(mods, RETRY_COUNT);

        if (infoDocs.isEmpty()) {
            throw new IllegalStateException(DOC_NOT_FOUND_EXCEPTION_MSG);
        }

        for (Document infoDoc : infoDocs) {
            String set_number = infoDoc.getElementsByTagName("set_number").item(0).getTextContent();
            String no_entries = infoDoc.getElementsByTagName("no_entries").item(0).getTextContent();

            int counter = 0;

            Document doc;

            do {
                try {
                    doc = getDocumentFromURL("http://aleph.mzk.cz/X?op=present&set_no=" + set_number + "&set_entry=" + no_entries + "&format=marc");
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }

                counter++;
            } while (
                    counter < RETRY_COUNT &&
                            doc.getElementsByTagName("doc_number").getLength() < 0
            );

            if (counter == RETRY_COUNT) throw new IOException("Could not get sysno from Aleph.");

            //getById does not work, therefore we have to do it hard way
            //= doc.getElementById("008").getTextContent();

            String marc008 = null;
            NodeList fixFields = doc.getElementsByTagName("fixfield");

            for (int i = 0; i < fixFields.getLength(); i++) {
                Element fixField = (Element) fixFields.item(i);

                String id = fixField.getAttribute("id");

                if (id.equals("008")) {
                    marc008 = fixField.getTextContent();
                }
            }

            //check if year matches due to possible year ambiguity
            if (marc008 == null || !marc008.contains(mods.getDateIssued())) {
                System.out.println("Could not load year from document: " + mods.getPhysicalLocation());
                continue;
            }

            sysno = doc.getElementsByTagName("doc_number").item(0).getTextContent();

            NodeList field = doc.getElementsByTagName("subfield");

            for (int i = 0; i < field.getLength(); i++) {
                String label = ((Element) field.item(i)).getAttribute("label");

                if (label.equals("l")) {
                    base = field.item(i).getTextContent();
                }
            }

            if (sysno == null || !base.startsWith("MZK0")) return null;

            return Pair.create(sysno, base);
        }

        return null;
    }

    /**
     * Loads information from foxml stored in directory param (root is defined by having same uuid in name as directory)
     *
     * @param directory directory containing k4 ProArc export
     * @return export signature
     */
    public static Mods getModsFromRootObject(Path directory) {

        String rootName = directory.toFile().getName();

        if (rootName.startsWith("k4_")) {
            rootName = rootName.substring(3);
        }

        if (rootName.lastIndexOf('_') != -1) {
            rootName = rootName.substring(0, rootName.lastIndexOf('_'));
        }

        File root = directory.resolve(rootName + ".xml").toFile();

        Document doc;

        try {
            doc = getDocumentFromFile(root);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return Mods.loadModsFromDoc(doc, rootName);
    }

    /**
     * Loads DOM object from File
     *
     * @param file xml file to be loaded
     * @return DOM xml document of file
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
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
            if (((Element) nl.item(i)).getAttribute("ID").startsWith(id)) return (Element) nl.item(i);
        }

        return null;
    }

    private static List<Document> getResponseFromAleph(Mods mods, int retryCount) {
        List<Document> docs = new LinkedList<>();

        for (String alephBase : ALEPH_BASES) {
            Document doc = getResponseFromAleph(alephBase, mods, retryCount);

            if (doc != null) {
                docs.add(doc);
            }
        }

        return docs;
    }

    private static Document getResponseFromAleph(String base, Mods mods,  int retryCount) {
        int counter = 0;
        Document doc;

        do {
            try {
                doc = getDocumentFromURL("http://aleph.mzk.cz/X?base=" + base + "&op=find&request=sig=" + mods.getPhysicalLocation());
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

            counter++;
        } while (
            counter < retryCount &&
                doc.getElementsByTagName("set_number").getLength() < 1 &&
                doc.getElementsByTagName("no_entries").getLength() < 1
        );

        if (counter == retryCount) return null;

        return doc;
    }

    /**
     * Moves contents of the second directory into the first.
     *
     * @param dir1 target directory
     * @param dir2 source directory which will be moved to dir1
     */
    public static void mergeTwoDirectories(File dir1, File dir2){
        String targetDirPath = dir1.getAbsolutePath();
        File[] files = dir2.listFiles();
        for (File file : files) {
            file.renameTo(new File(targetDirPath+File.separator+file.getName()));
            System.out.println(file.getName() + " is moved!");
        }
    }

    /**
     * Sends HTTP request to kramerius for starting import of specified uuid
     *
     * Note that Kramerius must have ProArc export accessible at
     * /opt/app-root/src/.kramerius4/import/ProArc/
     *
     * If request fails, IllegalStateException is thrown.
     *
     * @param parentUUID file name / uuid of export
     * @param k4address http adress of Kramerius client
     * @param k4credentials login credentials of Kramerius client
     * @throws IOException
     */
    public static void requestKrameriusImport(String parentUUID, String k4address, String k4credentials) throws IOException {
        String query = k4address + "/search/api/v4.6/processes/?def=parametrizedimport";
        String json =
                "{" +
                        "\"mapping\":" +
                        "{" +
                        "\"importDirectory\":\"/opt/app-root/src/.kramerius4/import/ProArc/" + parentUUID + "\","+
                        "\"startIndexer\":\"true\"," +
                        "\"updateExisting\":\"false\"" +
                        "}" +
                        "}";

        URL url = new URL(query);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        String encoded = Base64.getEncoder().encodeToString((k4credentials).getBytes(StandardCharsets.UTF_8));
        conn.setRequestProperty("Authorization", "Basic "+encoded);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");

        OutputStream os = conn.getOutputStream();
        os.write(json.getBytes("UTF-8"));
        os.close();

        int status = conn.getResponseCode();
        InputStream in;

        if (status < HTTP_BAD_REQUEST) {
            // read the response
            in = new BufferedInputStream(conn.getInputStream());
            String result = org.apache.commons.io.IOUtils.toString(in, "UTF-8");
            System.out.println("Kramerius response:");
            System.out.println(result);

            in.close();
            conn.disconnect();
        } else {
            // read the response
            in = new BufferedInputStream(conn.getErrorStream());
            String result = org.apache.commons.io.IOUtils.toString(in, "UTF-8");

            in.close();
            conn.disconnect();

            throw new IllegalArgumentException("Requesting Kramerius import failed, server response was : " + result);
        }

        try {
            String result = org.apache.commons.io.IOUtils.toString(in, "UTF-8");
            System.out.println("Kramerius response:");
            System.out.println(result);
        } catch (Exception ignored) {
            //response does not have to be processed in any way
        }

        in.close();
        conn.disconnect();
    }

    /**
     * Creates aleph update csv file into specified directory.
     * This file is then processed by other applications
     *
     * @param parentUUID uuid of record to be updated (digitalniknihovna.cz link will be added)
     * @param sysno sysno of record in Aleph
     * @param base base of record in Aleph
     * @param alephDirectory storage of update CSV records
     * @throws IOException
     */
    public static void prepareAlephUpdateRecord(String parentUUID, String sysno, String base, File alephDirectory) throws IOException {

        File csvFile = alephDirectory.toPath().resolve(parentUUID + ".csv").toFile();

        FileUtils.writeStringToFile(
                csvFile,
                base + " @ " + sysno + " @ http://www.digitalniknihovna.cz/mzk/view/uuid:" + parentUUID,
                Charset.defaultCharset()
        );

        csvFile.setReadable(true, false);
        csvFile.setWritable(true, false);
    }
}
