package cz.mzk.osdd.merlin;

import com.sun.javaws.exceptions.InvalidArgumentException;
import cz.mzk.osdd.merlin.models.AppState;
import cz.mzk.osdd.merlin.models.Title;
import org.xml.sax.SAXException;

import javax.print.attribute.standard.Severity;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Jakub Kremlacek on 30.3.17.
 */
public class ExportProcessor {
    private static final boolean DEBUG_MODE = true;

    private static final String IN = "in";
    private static final String OUT = "out";

    private final Path IN_PATH;
    private final Path OUT_PATH;

    private List<Title> titles = new LinkedList<>();

    public ExportProcessor(String path) {
        IN_PATH = Paths.get(path).resolve(IN);
        OUT_PATH = Paths.get(path).resolve(OUT);
    }

    public ExportProcessor(String in, String out) {
        IN_PATH = Paths.get(in);
        OUT_PATH = Paths.get(out);
    }


    public int run() {
        try {
            switch (checkDirectories()) {
                case CREATED_DIRECTORIES:
                    System.out.println(Severity.REPORT.getName() + " Created mandatory IO directories. Rerun app to start processing.");
                    break;
                case FINE:
                    processDirectory();
                    break;
            }

            processTitles();

        } catch (InvalidArgumentException e) {
            e.printStackTrace();
            return -1;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return -1;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        } catch (SAXException e) {
            e.printStackTrace();
            return -1;
        }

        return 0;
    }

    public List<Title> getTitlesDebug() throws IllegalAccessException {
        if (DEBUG_MODE) return titles;
        else reportAccessingProtectedMethod();

        return null;
    }

    public void processDirectoryDebug() throws InvalidArgumentException, IllegalAccessException, IOException, SAXException, ParserConfigurationException {
        if (DEBUG_MODE) processDirectory();
        else reportAccessingProtectedMethod();
    }

    private void processTitles() throws ParserConfigurationException, SAXException, IOException {
        for (Title title : titles) {
             title.processTitle(OUT_PATH);
        }
    }

    private void processDirectory() throws InvalidArgumentException, ParserConfigurationException, SAXException, IOException {
        processDirectory(IN_PATH);
    }

    private void processDirectory(Path directory) throws InvalidArgumentException, IOException, SAXException, ParserConfigurationException {
        File[] subdirs = directory.toFile().listFiles(File::isDirectory);
        File[] files = directory.toFile().listFiles(File::isFile);

        if (files.length > 0 && subdirs.length > 0) throw new InvalidArgumentException(new String[]{"Input datastructure malformed, each title must be stored in separate directory"});

        for (File subdir : subdirs) processDirectory(subdir.toPath());

        if (files.length > 0) titles.add(new Title(directory, files));

    }

    private AppState checkDirectories() throws InvalidArgumentException {
        boolean preparation = false;

        if (!IN_PATH.toFile().exists()) {
            IN_PATH.toFile().mkdir();
            preparation = true;
        }

        if (!OUT_PATH.toFile().exists()) {
            OUT_PATH.toFile().mkdir();
            preparation = true;
        }

        if (preparation) return AppState.CREATED_DIRECTORIES;

        if (IN_PATH.toFile().isFile()) throw new InvalidArgumentException(new String[]{"Input directory exists as file!"});
        if (OUT_PATH.toFile().isFile()) throw new InvalidArgumentException(new String[]{"Output directory exists as file!"});

        return AppState.FINE;
    }

    private void reportAccessingProtectedMethod() throws IllegalAccessException {
        throw new IllegalAccessException("Application must be run in Debug Mode for disabling direct access protection!");
    }
}
