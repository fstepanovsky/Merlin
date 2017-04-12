package cz.mzk.osdd.merlin.models;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.nio.file.Path;

/**
 * Wrapper for Fedora document XML, simplifies fixing datastream content locations
 *
 * Created by Jakub Kremlacek on 6.4.17.
 */
public class Foxml {
    public static final String IMAGESERVER_LOCATION = "http://imageserver.mzk.cz";

    public static final String DATASTREAM_FILENAME_IMG_FULL = "big.jpg";
    public static final String DATASTREAM_FILENAME_IMG_PREVIEW = "preview.jpg";
    public static final String DATASTREAM_FILENAME_IMG_THUMB = "thumb.jpg";

    public static final String DATASTREAM_IMG_FULL = "IMG_FULL.0";
    public static final String DATASTREAM_IMG_PREVIEW = "IMG_PREVIEW.0";
    public static final String DATASTREAM_IMG_THUMB = "IMG_THUMB.0";

    private Document doc;
    private String imagePath;
    private String base;
    private String uuid;

    /**
     * Creates foxml wrapper
     *
     * @param doc JavaDom document root of loaded XML file
     * @param imagePath sysno representing path on imageserver
     * @param base aleph base of record
     * @param uuid uuid of processed record
     */
    public Foxml(Document doc, String imagePath, String base, String uuid) {
        if (doc == null || imagePath == null || base == null || uuid == null)
            throw new NullPointerException("Paramaters cannot be null.");

        this.doc = doc;
        this.imagePath = imagePath;
        this.base = base;
        this.uuid = uuid;
    }

    /**
     * Modifies selected datastream data content from local binaryContent to contentLocation on imageserver
     *
     * @param datastream datastream to be edited
     * @throws IllegalArgumentException when unsupported datastream is selected or foxml is not valid
     */
    public void processDatastream(String datastream) throws IllegalArgumentException {

        if (datastream == null) {
            throw new IllegalArgumentException("Datastream cannot be null.");
        }

        if (
                !(
                datastream.equals(DATASTREAM_IMG_FULL) ||
                datastream.equals(DATASTREAM_IMG_PREVIEW) ||
                datastream.equals(DATASTREAM_IMG_THUMB)
                ))
        {
            throw new IllegalArgumentException("Unsupported datastream: " + datastream + ".");
        }

        Element img = Utils.filterDatastreamFromDocument(doc, datastream);

        if (img == null) {
            throw new IllegalArgumentException("missing " + datastream);
        }

        removeBinaryContent(img);

        String selectedImageType = null;

        switch (datastream) {
            case DATASTREAM_IMG_FULL:
                selectedImageType = DATASTREAM_FILENAME_IMG_FULL;
                break;
            case DATASTREAM_IMG_PREVIEW:
                selectedImageType = DATASTREAM_FILENAME_IMG_PREVIEW;
                break;
            case DATASTREAM_IMG_THUMB:
                selectedImageType = DATASTREAM_FILENAME_IMG_THUMB;
                break;
        }

        setContentLocation(img, selectedImageType);
    }

    /**
     * Adds kramerius4:tiles-url and modifies kramerius:file in RDF:rdf element
     */
    public void processRDF() {
        NodeList rdfL = doc.getElementsByTagName("rdf:Description");

        if (rdfL.getLength() != 1) throw new IllegalArgumentException("not containing single rdf:Description");

        NodeList rdfChildren = rdfL.item(0).getChildNodes();

        for (int i = 0; i < rdfChildren.getLength(); i++) {
            if (rdfChildren.item(i).getNodeName().equals("kramerius:file")) {
                rdfChildren.item(i).setTextContent(uuid + ".jp2");
            }
        }

        NodeList ktuL = doc.getElementsByTagName("kramerius4:tiles-url");
        Element ktu;

        if (ktuL.getLength() == 1) {
            ktu = (Element) ktuL.item(0);
            ktu.setTextContent(getImagePath() + uuid);
        } else if (ktuL.getLength() == 0) {
            ktu = doc.createElement("kramerius4:tiles-url");
            ktu.setTextContent(getImagePath() + uuid);
            rdfL.item(0).appendChild(ktu);
            ((Element) rdfL.item(0).getParentNode()).setAttribute("xmlns:kramerius4", "http://www.nsdl.org/ontologies/relationships#");
        } else {
            throw new IllegalArgumentException("not containing single or none kramerius4:tiles-url");
        }
    }

    private String getImagePath() {
        return IMAGESERVER_LOCATION + "/" +
                base + "/" +
                imagePath.substring(0, 3) + "/" +
                imagePath.substring(3, 6) + "/" +
                imagePath.substring(6, imagePath.length()) + "/";
    }

    private void setContentLocation(Element img, String imageName) {
        NodeList clL = img.getElementsByTagName("contentLocation");
        Element cL;

        if (clL.getLength() == 1) {
            cL = (Element) clL.item(0);
            cL.setAttribute("TYPE", "URL");
            cL.setAttribute("REF",   getImagePath() + uuid + "/" + imageName);
        } else if (clL.getLength() == 0) {
            cL = doc.createElement("contentLocation");
            cL.setAttribute("TYPE", "URL");
            cL.setAttribute("REF", getImagePath() + uuid + "/" + imageName);

            img.appendChild(cL);
        } else {
            throw new IllegalArgumentException("not containing single or none contentLocation in " + img.getTagName());
        }
    }

    private void removeBinaryContent(Element e) {

        NodeList imgFullChildren = e.getChildNodes();

        Element bC = null;

        for (int i = 0; i < imgFullChildren.getLength(); i++){
            if (imgFullChildren.item(i).getNodeName().equals("binaryContent")) {
                if (bC == null) {
                    bC = (Element) imgFullChildren.item(i);
                } else {
                    throw new IllegalArgumentException("contains multiple binaryContent in " + e.getTagName());
                }
            }
        }

        if (bC == null) {
            System.err.println("Warning: Element " + e.getTagName() + "does not contain binaryContent element");
            return;
        }

        e.removeChild(bC);

        e.removeAttribute("SIZE");
    }

    public void save(Path toKramerius) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult streamResult =  new StreamResult(toKramerius.resolve(uuid + ".xml").toFile());
        transformer.transform(source, streamResult);
    }

}
