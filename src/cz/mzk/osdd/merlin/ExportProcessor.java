package cz.mzk.osdd.merlin;

import cz.mzk.osdd.merlin.models.AppState;
import cz.mzk.osdd.merlin.models.Title;
import cz.mzk.osdd.merlin.models.Utils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import javax.print.attribute.standard.Severity;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 * Created by Jakub Kremlacek on 30.3.17.
 */
public class ExportProcessor {
    private static final boolean DEBUG_MODE = true;

    private final Path IMAGESERVER_PATH;
    private final Path KRAMERIUS_PATH;

    private final String KRAMERIUS_ADDRESS;
    private final String KRAMERIUS_CREDENTIALS;
    private final File ALEPH_DIRECTORY;

    private Path IN_IMG;
    private Path IN_FOXML;

    private Path IN_PATH;
    private Path OUT_PATH;

    private final boolean DIRECT_OUTPUT;

    private String outputPackPath = "proarcExport_" + ((new SimpleDateFormat("yyyy.MM.dd_HH:mm:ss")).format(new Date()));
    private static final boolean DETAILED_OUTPUT = true;

    private List<Title> titles = new LinkedList<>();

    public ExportProcessor(String path) {
        this(path, path);
    }

    //Batch processing - with direct output
    public ExportProcessor(String in, String out) {
        IN_PATH = Paths.get(in);
        OUT_PATH = Paths.get(out);

        DIRECT_OUTPUT = false;

        IMAGESERVER_PATH = null;
        KRAMERIUS_PATH = null;
        KRAMERIUS_CREDENTIALS = null;
        KRAMERIUS_ADDRESS = null;
        ALEPH_DIRECTORY = null;
    }

    //Single title processing - with direct output
    public ExportProcessor(String imageIn, String foxmlIn, String out, File alephDir) {
        IN_IMG = Paths.get(imageIn);
        IN_FOXML = Paths.get(foxmlIn);

        DIRECT_OUTPUT = false;

        IMAGESERVER_PATH = null;
        KRAMERIUS_PATH = null;
        KRAMERIUS_CREDENTIALS = null;
        KRAMERIUS_ADDRESS = null;
        ALEPH_DIRECTORY = alephDir;

        OUT_PATH = Paths.get(out);
        outputPackPath = out;

        new File(outputPackPath).mkdir();
    }

    //Single title processing - with remote output
    public ExportProcessor(
            String imageIn,
            String foxmlIn,
            boolean directOutput,
            String imageserverPath,
            String krameriusPath,
            String krameriusAddress,
            String krameriusCredentials,
            File alephDir) {
        IN_IMG = Paths.get(imageIn);
        IN_FOXML = Paths.get(foxmlIn);

        IMAGESERVER_PATH = new File(imageserverPath).toPath();
        KRAMERIUS_PATH = new File(krameriusPath).toPath();

        DIRECT_OUTPUT = directOutput;

        if (!IMAGESERVER_PATH.toFile().exists()) {
            throw new IllegalArgumentException("Invalid Imageserver path!");
        }

        if (!KRAMERIUS_PATH.toFile().exists()) {
            throw new IllegalArgumentException("Invalid Kramerius path!");
        }

        KRAMERIUS_ADDRESS = krameriusAddress;
        KRAMERIUS_CREDENTIALS = krameriusCredentials;
        ALEPH_DIRECTORY = alephDir;
    }

    public static ExportProcessor createExportProcessor(AppConfig config) {
        if (config.isDirectOutput()) {
            return new ExportProcessor(
                    config.getInputImage().getAbsolutePath(),
                    config.getInputK4().getAbsolutePath(),
                    config.isDirectOutput(),
                    config.getImageserverPath().getAbsolutePath(),
                    config.getKrameriusPath().getAbsolutePath(),
                    config.getKrameriusAddress(),
                    config.getKrameriusCredentials(),
                    config.getAlephDir()
            );
        } else {
            return new ExportProcessor(
                    config.getInputImage().getAbsolutePath(),
                    config.getInputK4().getAbsolutePath(),
                    config.getOutput().getAbsolutePath(),
                    config.getAlephDir()
            );
        }
    }

    public int runBatch() {
        if (IN_PATH == null || OUT_PATH == null) {
            System.err.println("Wrong EP usage");
            return -1;
        }

        try {
            switch (checkDirectories()) {
                case CREATED_DIRECTORIES:
                    System.out.println(Severity.REPORT.getName() + " Created mandatory IO directories. Rerun app to start processing.");
                    break;
                case FINE:
                    mergeImageAndKrameriusDirectories();
                    processDirectory();
                    break;
            }

            System.out.println("Preparing export pack into: " + outputPackPath);

            processTitles(
                    DIRECT_OUTPUT ? KRAMERIUS_PATH : null,
                    DIRECT_OUTPUT ? IMAGESERVER_PATH : null
            );

        } catch (IllegalArgumentException e) {
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

    public int runSingle() {
        if (IN_IMG == null || IN_FOXML == null || (OUT_PATH == null && !DIRECT_OUTPUT)) {
            System.err.println("Wrong EP usage");
            return -1;
        }

        try {
            Utils.mergeTwoDirectories(IN_FOXML.toFile(), IN_IMG.toFile());

            processDirectory(IN_FOXML);
            processTitles(
                    DIRECT_OUTPUT ? KRAMERIUS_PATH : null,
                    DIRECT_OUTPUT ? IMAGESERVER_PATH : null
                    );
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }

        return 0;
    }

    private void mergeImageAndKrameriusDirectories() throws IOException {
        File[] subdirs = IN_PATH.toFile().listFiles(File::isDirectory);
        String dirName;

        for (File subdir : subdirs) {
            if (subdir.getName().startsWith("k4_")) {
                dirName = subdir.getName().substring("k4_".length());
            } else if (subdir.getName().endsWith(".NDK_USER")) {
                dirName = subdir.getName().substring(0, subdir.getName().length() - ".NDK_USER".length());
            } else {
                System.out.println("skipping " + subdir.getName());
                continue;
            }

            Path dirPath = IN_PATH.resolve(dirName);

            if (!dirPath.toFile().exists()) dirPath.toFile().mkdir();

            Utils.mergeTwoDirectories(dirPath.toFile(), IN_PATH.resolve(subdir.getName()).toFile());
        }
    }

    public List<Title> getTitlesDebug() throws IllegalAccessException {
        if (DEBUG_MODE) return titles;
        else reportAccessingProtectedMethod();

        return null;
    }

    public void processDirectoryDebug() throws IllegalArgumentException, IllegalAccessException, IOException, SAXException, ParserConfigurationException {
        if (DEBUG_MODE) processDirectory();
        else reportAccessingProtectedMethod();
    }

    private void processTitles(Path krameriusPath, Path imageserverPath) throws ParserConfigurationException, SAXException, IOException, IllegalArgumentException {
        for (Title title : titles) {
            title.processTitle(OUT_PATH, krameriusPath, imageserverPath, ALEPH_DIRECTORY, KRAMERIUS_CREDENTIALS, KRAMERIUS_ADDRESS);
        }
    }

    private void processDirectory() throws IllegalArgumentException, ParserConfigurationException, SAXException, IOException {
        processDirectory(IN_PATH);
    }

    private void processDirectory(Path directory) throws IllegalArgumentException, IOException, SAXException, ParserConfigurationException {
        File[] subdirs = directory.toFile().listFiles(File::isDirectory);
        File[] files = directory.toFile().listFiles(File::isFile);

        if (files.length > 0 && subdirs.length > 0) throw new IllegalArgumentException("Input datastructure malformed, each title must be stored in separate directory");

        for (File subdir : subdirs) processDirectory(subdir.toPath());

        if (files.length > 0) titles.add(new Title(directory, files, outputPackPath, DETAILED_OUTPUT));
    }

    private AppState checkDirectories() throws IllegalArgumentException {
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

        if (IN_PATH.toFile().isFile()) throw new IllegalArgumentException("Input directory exists as file!");
        if (OUT_PATH.toFile().isFile()) throw new IllegalArgumentException("Output directory exists as file!");

        return AppState.FINE;
    }

    private void reportAccessingProtectedMethod() throws IllegalAccessException {
        throw new IllegalAccessException("Application must be run in Debug Mode for disabling direct access protection!");
    }
}
