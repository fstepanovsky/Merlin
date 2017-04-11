import com.sun.javaws.exceptions.InvalidArgumentException;
import cz.mzk.osdd.merlin.models.Pair;
import cz.mzk.osdd.merlin.models.Utils;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.MalformedURLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Created by Jakub Kremlacek on 4.4.17.
 */
public class UtilsTests {

    @Test
    public void getDocumentFromURLTest() throws ParserConfigurationException, SAXException, IOException {
        assertEquals(true ,Utils.getDocumentFromURL("http://aleph.mzk.cz/X?base=mzk01").getElementsByTagName("login").getLength() == 1);
    }

    @Test
    public void getDocumentFromURLMalformedTest() {
        assertThrows(MalformedURLException.class, () -> Utils.getDocumentFromURL("mzk.cz"));
    }

    @Test
    public void getDocumentFromURLNotXML() {
        assertThrows(SAXException.class, () -> Utils.getDocumentFromURL("http://mzk.cz"));
    }

    @Test
    public void getDocumentFromURLNullTest() throws ParserConfigurationException, SAXException, IOException {
        assertEquals(null, Utils.getDocumentFromURL(null));
    }

    @Test
    public void getSysnoWithBaseFromAlephTest() throws InvalidArgumentException {
        assertEquals(Pair.create("001564911","MZK01"), Utils.getSysnoWithBaseFromAleph("Mpa-0174.951,3960-19"));
    }

    @Test
    public void getSysnoWithBaseFromAlephNullTest() throws InvalidArgumentException {
        assertEquals(null, Utils.getSysnoWithBaseFromAleph(null));
    }
}
