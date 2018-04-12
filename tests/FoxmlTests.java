import cz.mzk.osdd.merlin.models.Foxml;
import cz.mzk.osdd.merlin.models.Utils;
import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Created by Jakub Kremlacek on 7.4.17.
 */
public class FoxmlTests {

    public static final String TESTING_FOXML = "./tests/input/foxml/aaaaaaa1-1111-1111-1111-2f890f11f9d5.xml";
    public static final String TESTING_IMAGE_LOCATION = "111222333";
    public static final String TESTING_FOXML_UUID = "aaaaaaa1-1111-1111-1111-2f890f11f9d5";
    public static final String TESTING_FOXML_BASE = "MZK01";

    private Document getTestingDocument() throws IOException, SAXException, ParserConfigurationException {
        return Utils.getDocumentFromFile(new File(TESTING_FOXML));
    }

    //All datastreams are processable due to public fedora export which modifies all datastreams
//    @Test
//    public void processInvalidDatastream() throws ParserConfigurationException, SAXException, IOException {
//        Document d = getTestingDocument();
//        Foxml f = new Foxml(d, " ", "MZK01", "aaaaaaa1-1111-1111-1111-2f890f11f9d5");
//
//        assertThrows(IllegalArgumentException.class, () -> f.processDatastream("BIBLIOX_MODS"));
//    }

    @Test
    public void processNullDatastream() throws ParserConfigurationException, SAXException, IOException {

        Document d = getTestingDocument();
        Foxml f = new Foxml(d, " ", "MZK01", "aaaaaaa1-1111-1111-1111-2f890f11f9d5");

        assertThrows(IllegalArgumentException.class, () -> f.processDatastream(null));
    }

    @Test
    public void processImgFull() throws ParserConfigurationException, SAXException, IOException {
        processDatastream(Foxml.DATASTREAM_IMG_FULL, Foxml.DATASTREAM_FILENAME_IMG_FULL);
    }

    @Test
    public void processImgPreview() throws IOException, SAXException, ParserConfigurationException {
        processDatastream(Foxml.DATASTREAM_IMG_PREVIEW, Foxml.DATASTREAM_FILENAME_IMG_PREVIEW);
    }

    @Test
    public void processImgThumb() throws IOException, SAXException, ParserConfigurationException {
        processDatastream(Foxml.DATASTREAM_IMG_THUMB, Foxml.DATASTREAM_FILENAME_IMG_THUMB);
    }

    private void processDatastream(String ds, String dsFilename) throws ParserConfigurationException, SAXException, IOException {
        Document d = getTestingDocument();

        Foxml f = new Foxml(d, TESTING_IMAGE_LOCATION, TESTING_FOXML_BASE, TESTING_FOXML_UUID);

        f.processDatastream(ds);

        checkDatastream(d, ds);

        assertEquals(
                Foxml.IMAGESERVER_LOCATION + "/" + TESTING_FOXML_BASE + "/111/222/333/" + TESTING_FOXML_UUID + "/" + dsFilename,
                ((Element) (Utils.filterDatastreamFromDocument(d, ds).getElementsByTagName("contentLocation")).item(0)).getAttribute("REF"));
    }

    private void checkDatastream(Document d, String ds) {
        String[] others;

        switch (ds) {
            case Foxml.DATASTREAM_IMG_FULL:
                others = new String[] {Foxml.DATASTREAM_IMG_PREVIEW, Foxml.DATASTREAM_IMG_THUMB};
                break;
            case Foxml.DATASTREAM_IMG_PREVIEW:
                others = new String[] {Foxml.DATASTREAM_IMG_FULL, Foxml.DATASTREAM_IMG_THUMB};
                break;
            case Foxml.DATASTREAM_IMG_THUMB:
                others = new String[] {Foxml.DATASTREAM_IMG_FULL, Foxml.DATASTREAM_IMG_PREVIEW};
                break;
            default:
                throw new IllegalArgumentException("Unknown datastream: " + ds);
        }

        assertEquals(0, Utils.filterDatastreamFromDocument(d, ds).getElementsByTagName("binaryContent").getLength());
        assertEquals(1, Utils.filterDatastreamFromDocument(d, ds).getElementsByTagName("contentLocation").getLength());

        for (int i = 0; i < others.length; i++) {
            assertEquals(1, Utils.filterDatastreamFromDocument(d, others[i]).getElementsByTagName("binaryContent").getLength());
            assertEquals(0, Utils.filterDatastreamFromDocument(d, others[i]).getElementsByTagName("contentLocation").getLength());
        }
    }
}
