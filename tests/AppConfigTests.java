import cz.mzk.osdd.merlin.AppConfig;
import java.io.File;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Jakub Kremlacek
 */
public class AppConfigTests {

    @Test
    public void initTest() {
        String[] args = {"-iI", "./", "-iK", "./", "-oD","-c", "tests/config/configTest.properties"};

        AppConfig ac = new AppConfig(args);

        assertEquals(new File("tests/input").toString(), ac.getAlephDir().toString());
        assertEquals(new File("tests").toString(), ac.getKrameriusPath().toString());
        assertEquals(new File("tests/input/foxml").toString(), ac.getImageserverPath().toString());
        assertEquals("kramerius:kramerius", ac.getKrameriusCredentials());
        assertEquals("http://test.cz", ac.getKrameriusAddress());
    }
}
