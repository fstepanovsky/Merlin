package cz.mzk.osdd.merlin;

/**
 * modifies FOXML produced from ProArc v3.3 to work with MZK Imageserver and Kramerius and prepares images into their respective places at Imageserver
 *
 * does not check duplicate exports(ending with _X)
 *
 * @author Jakub Kremlacek
 */

public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.print(Messages.DEFAULT_INFO);
            return;
        }

        ExportProcessor processor = null;

        if (args.length < 3) {
            if (args.length == 1) processor = new ExportProcessor(args[0]);
            if (args.length == 2) processor = new ExportProcessor(args[0], args[1]);

            System.exit(processor.runBatch());
        } else {
            processor = processCommandLine(args);

            System.exit(processor.runSingle());
        }
    }

    public static ExportProcessor processCommandLine(String[] args) {
        AppConfig config = new AppConfig(args);

        return ExportProcessor.createExportProcessor(config);
    }
}
