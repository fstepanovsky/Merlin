package cz.mzk.osdd.merlin;

/**
 * modifies FOXML produced from ProArc v3.3 to work with MZK Imageserver and Kramerius and prepares images into their respective places at Imageserver
 *
 * does not check duplicate exports(ending with _X)
 *
 * @author Jakub Kremlacek
 */

public class Main {

    public static final String defaultPath = "./";

    public static void main(String[] args) {
        ExportProcessor processor = null;

        if (args.length == 1) processor = new ExportProcessor(args[0]);
        if (args.length == 2) processor = new ExportProcessor(args[0], args[1]);
        if (args.length == 0) processor = new ExportProcessor(defaultPath);

        processor.run();
    }


}
