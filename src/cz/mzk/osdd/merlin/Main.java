package cz.mzk.osdd.merlin;

import java.io.File;

/**
 * modifies FOXML produced from ProArc v3.3 to work with MZK Imageserver and Kramerius and prepares images into their respective places at Imageserver
 *
 * does not check duplicate exports(ending with _X)
 *
 * @author Jakub Kremlacek
 */

public class Main {

    public static final String defaultPath = "";
    public static final String INPUT_IMAGE = "-iI";
    public static final String INPUT_K4 = "-iK";
    public static final String OUTPUT = "-o";

    public static void main(String[] args) {
        ExportProcessor processor = null;

        if (args.length < 3) {
            if (args.length == 1) processor = new ExportProcessor(args[0]);
            if (args.length == 2) processor = new ExportProcessor(args[0], args[1]);
            if (args.length == 0) processor = new ExportProcessor(defaultPath);

            System.exit(processor.runBatch());
        } else {
            processor = processCommandLine(args);

            System.exit(processor.runSingle());
        }
    }

    public static ExportProcessor processCommandLine(String[] args) {
        if (args.length != 6) throw new IllegalArgumentException("Invalid argument count: " + args.length + " expected 6.");

        int pos = 0;

        File inputImage = null;
        File inputK4 = null;
        File output = null;

        while (pos < args.length) {
            if (args[pos].equals(INPUT_IMAGE)) {
                inputImage = new File(args[pos+1]);

                if (!inputImage.exists() || inputImage.isFile()) throw new IllegalArgumentException("Input image directory does not exist.");

                pos = pos + 2;
            } else if (args[pos].equals(INPUT_K4)) {
                inputK4 = new File(args[pos+1]);

                if (!inputK4.exists() || inputK4.isFile()) throw new IllegalArgumentException("Input image directory does not exist.");

                pos = pos + 2;
            } else if (args[pos].equals(OUTPUT)) {
                output = new File(args[pos+1]);

                pos = pos + 2;
            } else {
                throw new IllegalArgumentException("Invalid argument type: " + args[pos]);
            }
        }

        if (inputImage == null) throw new IllegalArgumentException("inputImage not set");
        if (inputK4 == null) throw new IllegalArgumentException("inputK4 not set");
        if (output == null) throw new IllegalArgumentException("output not set");

        return new ExportProcessor(inputImage.getAbsolutePath(), inputK4.getAbsolutePath(), output.getAbsolutePath());
    }
}
