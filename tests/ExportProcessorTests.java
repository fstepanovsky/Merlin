import com.sun.javaws.exceptions.InvalidArgumentException;
import cz.mzk.osdd.merlin.ExportProcessor;
import cz.mzk.osdd.merlin.models.ExportPack;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Created by Jakub Kremlacek on 3.4.17.
 */
public class ExportProcessorTests {

    private static final String uuid_1 = "0000-fb87";
    private static final String uuid_2 = "1230-ac87";

    @Test
    public void exportProcessorNullTest() {
        assertThrows(NullPointerException.class, () -> new ExportProcessor(null));
    }

    @Test
    public void processPacksValidateInputEmptyInputTest() {
        String inputPath = "./tests/input/validateInput/empty";

        ExportProcessor processor = new ExportProcessor(inputPath);

        assertEquals(0, processor.run());

        try {
            assertEquals(0, processor.getTitlesDebug().size());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Only pages must be in packs for each title, since their parents don't contain any information to be changed
     */
    @Test
    public void processPacksValidateInputPackCountTest() {
        String inputPath = "./tests/input/validateInput/count";

        ExportProcessor processor = new ExportProcessor(inputPath);

        Map<String, ExportPack> expected = new HashMap<>();

        expected.put(uuid_1, new ExportPack(uuid_1));

        assertEquals(0, processor.run());

        try {
            assertEquals(expected.size(), processor.getTitlesDebug().get(0).getPackCount());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void processPacksValidateInputInvalidInputCountTest() {
        String inputPath = "./tests/input/validateInput/incomplete";

        ExportProcessor processor = new ExportProcessor(inputPath);

        assertThrows(InvalidArgumentException.class, () -> processor.processDirectoryDebug());
    }
}
