import cz.mzk.osdd.merlin.models.Foxml;
import cz.mzk.osdd.merlin.models.Utils;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

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

    @Test
    public void processInvalidDatastream() throws ParserConfigurationException, SAXException, IOException {
        Document d = getTestingDocument();
        Foxml f = new Foxml(d, " ", "MZK01", "aaaaaaa1-1111-1111-1111-2f890f11f9d5");

        assertThrows(IllegalArgumentException.class, () -> f.processDatastream("BIBLIO_MODS"));
    }

    @Test
    public void processImgFull() throws ParserConfigurationException, SAXException, IOException {
        Document d = getTestingDocument();

        Foxml f = new Foxml(d, TESTING_IMAGE_LOCATION, TESTING_FOXML_BASE, TESTING_FOXML_UUID);

        f.processDatastream(Foxml.DATASTREAM_IMG_FULL);

        assertEquals(0, Utils.filterDatastreamFromDocument(d, Foxml.DATASTREAM_IMG_FULL).getElementsByTagName("binaryContent").getLength());
        assertEquals(1, Utils.filterDatastreamFromDocument(d, Foxml.DATASTREAM_IMG_FULL).getElementsByTagName("contentLocation").getLength());

        assertEquals(
                Foxml.IMAGESERVER_LOCATION + "/" + TESTING_FOXML_BASE + "/111/222/333/" + TESTING_FOXML_UUID + "/big.jpg",
                ((Element) (Utils.filterDatastreamFromDocument(d, Foxml.DATASTREAM_IMG_FULL).getElementsByTagName("contentLocation")).item(0)).getAttribute("REF"));
    }

    @Test
    public void processImgPreview() {

    }

    @Test
    public void processImgThumb() {

    }
}
