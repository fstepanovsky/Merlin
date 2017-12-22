import cz.mzk.osdd.merlin.AppConfig;
import java.io.File;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Jakub Kremlacek
 */
public class AppConfigTests {

    @Test
    public void initWithConfigFileTest() {
        String[] args = {"-iI", "./", "-iK", "./", "-oD","-c", "tests/config/configTest.properties"};

        AppConfig ac = new AppConfig(args);

        assertEquals(new File("tests/input").toString(), ac.getAlephDir().toString());
        assertEquals(new File("tests").toString(), ac.getKrameriusPath().toString());
        assertEquals(new File("tests/input/foxml").toString(), ac.getImageserverPath().toString());
        assertEquals("kramerius:kramerius", ac.getKrameriusCredentials());
        assertEquals("http://test.cz", ac.getKrameriusAddress());
    }

    @Test
    public void initTest() {
        String[] args = {"-iI", "./", "-iK", "tests", "-oD"};

        AppConfig ac = new AppConfig(args);

        assertTrue(ac.isDirectOutput());
        assertEquals("tests",ac.getInputK4().toString());
        assertEquals(".", ac.getInputImage().toString());
    }

    @Test
    public void malformedCredentialsTest() {
        String[] args = {"-iI", "./", "-iK", "tests", "-oD", "-kL", "krameriuskramerius"};
        assertThrows(IllegalArgumentException.class, () -> new AppConfig(args));
    }

    @Test
    public void nullArgsTest() {
        assertThrows(NullPointerException.class, () -> new AppConfig(null));
    }

    @Test
    public void missingKrameriusAddressTest() {
        String[] args = {"-iI", "./", "-iK", "tests", "-oD", "-kL", "kramerius:kramerius"};
        assertThrows(IllegalArgumentException.class, () -> new AppConfig(args));
    }

    @Test
    public void missingKrameriusLoginTest() {
        String[] args = {"-iI", "./", "-iK", "tests", "-oD", "-kA", "http://kramerius.cz"};
        assertThrows(IllegalArgumentException.class, () -> new AppConfig(args));
    }

    @Test
    public void malformedKrameriusURLTest() {
        String[] args = {"-iI", "./", "-iK", "tests", "-oD", "-kL", "kramerius:kramerius", "-kA", "krameriuscz"};
        assertThrows(IllegalArgumentException.class, () -> new AppConfig(args));
    }

    @Test
    public void incompleteDirectOutputArgsTest() {
        String[] argsWithoutImageserver = {"-iI", "./", "-iK", "tests", "-oD", "-oK", "./"};

        assertThrows(IllegalArgumentException.class, () -> new AppConfig(argsWithoutImageserver));

        String[] argsWithoutKramerius = {"-iI", "./", "-iK", "tests", "-oD", "-oI", "tests"};

        assertThrows(IllegalArgumentException.class, () -> new AppConfig(argsWithoutKramerius));
    }
}
