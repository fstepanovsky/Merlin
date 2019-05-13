package cz.mzk.osdd.merlin.models;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * @author kremlacek
 */
public class Mods {

    private String dateIssued;
    private String physicalLocation;

    private Mods() {}

    public Mods(String dateIssued, String physicalLocation) {

        //this should be performed when initializing info container class not here
        if (physicalLocation.contains(" ")) {
            physicalLocation = physicalLocation.replaceAll(" ", "%20");
        }

        this.dateIssued = dateIssued;
        this.physicalLocation = physicalLocation;
    }

    public String getDateIssued() {
        return dateIssued;
    }

    public String getPhysicalLocation() {
        return physicalLocation;
    }

    public static Mods loadModsFromDoc(Document doc, String uuid) {
        Mods mods = new Mods();

        //signature
        NodeList msl = doc.getElementsByTagName("mods:shelfLocator");

        if (msl.getLength() < 1) {
            System.err.println("Signature not found within parent: " + uuid);
            return null;
        }

        String physicalLocation = msl.item(0).getTextContent();

        //year
        NodeList mdi = doc.getElementsByTagName("mods:dateIssued");

        if (mdi.getLength() < 1) {
            System.err.println("Date issued not found witin parent: " + uuid);
        }

        //text content can contain non numerical characters
        Pattern p = Pattern.compile("[0-9]{4}");
        Matcher m = p.matcher(mdi.item(0).getTextContent());

        String dateIssued;

        if (m.find()) {
            dateIssued = m.group(0);
        } else {
            throw new IllegalStateException("Date issued does not contain single year within parent: " + uuid);
        }

        return new Mods(dateIssued, physicalLocation);
    }
}
