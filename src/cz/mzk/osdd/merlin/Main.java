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
        ExportProcessor processor = null;

        processor = new ExportProcessor("./");

        processor.run();
    }


}
