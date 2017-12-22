package cz.mzk.osdd.merlin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

/**
 *
 * @author Jakub Kremlacek
 */
public class AppConfig {
    private File inputImage = null;
    private File inputK4 = null;
    private File output = null;
    private File krameriusPath = null;
    private File imageserverPath = null;
    private boolean directOutput = false;
    private String krameriusAddress = null;
    private String krameriusCredentials = null;
    private File alephDir = null;
    private File configFile = null;

    public AppConfig(String[] args) {
        processArgs(args);
    }

    private void processArgs(String[] args) {

        if (args == null) {
            throw new NullPointerException("Args cannot be null");
        }

        int pos = 0;

        while (pos < args.length) {

            switch (args[pos]) {
                case CMDAttribute.INPUT_IMAGE:

                    inputImage = new File(args[pos+1]);

                    if (!inputImage.exists() || inputImage.isFile()) throw new IllegalArgumentException("Input image directory does not exist.");

                    pos = pos + 2;
                    break;

                case CMDAttribute.INPUT_K4:

                    inputK4 = new File(args[pos+1]);

                    if (!inputK4.exists() || inputK4.isFile()) throw new IllegalArgumentException("Input image directory does not exist.");

                    pos = pos + 2;
                    break;

                case CMDAttribute.OUTPUT:
                    output = new File(args[pos+1]);

                    pos = pos + 2;
                    break;

                case CMDAttribute.DIRECT_OUTPUT:
                    directOutput = true;

                    pos = pos + 1;
                    break;

                case CMDAttribute.OUTPUT_IMAGESERVER:
                    imageserverPath = new File(args[pos+1]);

                    if (!imageserverPath.exists() || imageserverPath.isFile()) throw new IllegalArgumentException("Imageserver directory does not exist.");

                    pos = pos + 2;
                    break;

                case CMDAttribute.OUTPUT_KRAMERIUS:
                    krameriusPath = new File(args[pos + 1]);

                    if (!krameriusPath.exists() || krameriusPath.isFile())
                        throw new IllegalArgumentException("Kramerius directory does not exist.");

                    pos = pos + 2;
                    break;

                case CMDAttribute.K4_ADDRESS:
                    krameriusAddress = args[pos + 1];

                    pos = pos + 2;
                    break;

                case CMDAttribute.K4_CREDENTIALS:
                    krameriusCredentials = args[pos + 1];

                    pos = pos + 2;
                    break;

                case CMDAttribute.ALEPH_DIR:
                    alephDir = new File(args[pos + 1]);

                    pos = pos + 2;
                    break;

                case CMDAttribute.CONFIG:
                    configFile = new File(args[pos + 1]);

                    pos = pos + 2;
                    break;

                default:
                    throw new IllegalArgumentException("Invalid argument type: " + args[pos]);
            }
        }

        if (configFile != null) {
            try {
                loadConfig();
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not read config file at: " + configFile.getAbsolutePath());
            }
        }

        if (inputImage == null) throw new IllegalArgumentException("inputImage not set");
        if (inputK4 == null) throw new IllegalArgumentException("inputK4 not set");
        if (output == null && !directOutput) throw new IllegalArgumentException("output not set, use: " + CMDAttribute.OUTPUT + " or " + CMDAttribute.DIRECT_OUTPUT);

        if (alephDir != null && (!alephDir.exists() || !alephDir.isDirectory())) {
            throw new IllegalArgumentException("aleph directory: " + alephDir + " must exist");
        }

        //note that unused input attributes are simply discarded

        if (directOutput) {
            if (krameriusCredentials != null && !krameriusCredentials.contains(":")) {
                throw new IllegalArgumentException("krameriusCredentials must be set correctly");
            }

            if (krameriusAddress != null && krameriusCredentials == null || krameriusAddress == null && krameriusCredentials != null) {
                throw new IllegalArgumentException("krameriusAddress and krameriusCredentials must be set when remote import call is used");
            }

            if (krameriusAddress != null) {
                try {
                    URL url = new URL(krameriusAddress);
                    URLConnection conn = url.openConnection();
                    conn.connect();
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException("malformed kramerius URL");
                } catch (IOException e) {
                    throw new IllegalArgumentException("connection to the kramerius cannot be established");
                }
            }

            if (alephDir != null && (!alephDir.exists() || !alephDir.isDirectory())) {
                throw new IllegalArgumentException("aleph directory: " + alephDir + " must exist");
            }

            if (krameriusPath == null) {
                throw new IllegalArgumentException("path to kramerius must be specified");
            }

            if (imageserverPath == null) {
                throw new IllegalArgumentException("path to imageserver must be specified");
            }

        } else {
            if (output == null) throw new IllegalArgumentException("output not set, use: " + CMDAttribute.OUTPUT + " or " + CMDAttribute.DIRECT_OUTPUT);
        }
    }

    private void loadConfig() throws IOException {
        FileInputStream fileInput = new FileInputStream(configFile);

        Properties prop = new Properties();
        prop.load(fileInput);

        fileInput.close();

        alephDir = loadFileProperty(prop, "aleph.directory", alephDir);

        imageserverPath = loadFileProperty(prop, "imageserver.directory", imageserverPath);

        krameriusPath = loadFileProperty(prop, "kramerius.directory", krameriusPath);
        krameriusAddress = loadStringProperty(prop, "kramerius.address", krameriusAddress);
        krameriusCredentials = loadStringProperty(prop, "kramerius.credentials", krameriusCredentials);
    }

    private String loadStringProperty(Properties prop, String propertyName, String prevVal) {
        String val = prop.getProperty(propertyName);

        if (val != null) {
            return val;
        } else {
            return prevVal;
        }
    }

    private File loadFileProperty(Properties prop, String propertyName, File prevVal) {
        String val = prop.getProperty(propertyName);

        if (val != null) {
            return new File(val);
        } else {
            return prevVal;
        }
    }

    public File getInputImage() {
        return inputImage;
    }

    public File getInputK4() {
        return inputK4;
    }

    public File getOutput() {
        return output;
    }

    public File getKrameriusPath() {
        return krameriusPath;
    }

    public File getImageserverPath() {
        return imageserverPath;
    }

    public boolean isDirectOutput() {
        return directOutput;
    }

    public String getKrameriusAddress() {
        return krameriusAddress;
    }

    public String getKrameriusCredentials() {
        return krameriusCredentials;
    }

    public File getAlephDir() {
        return alephDir;
    }
}
