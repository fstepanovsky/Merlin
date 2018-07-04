package cz.mzk.osdd.merlin.models;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Base64;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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

    public static final String DATASTREAM_OCR = "TEXT_OCR.";
    public static final String DATASTREAM_ALTO = "ALTO.";

    public static final String DATASTREAM_BIBLIO_MODS = "BIBLIO_MODS.";
    public static final String DATASTREAM_DC = "DC.";
    public static final String DATASTREAM_RELS_EXT = "RELS-EXT.";


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

    public void removeFedoraURIFromRoot() {
        removeFedoraURI((Element) doc.getElementsByTagName("digitalObject").item(0));
    }

    private void removeFedoraURI(Element element) {
        element.removeAttribute("FEDORA_URI");
    }

    /**
     * Modifies selected datastream data content from local binaryContent to contentLocation on imageserver
     *
     * @param datastream datastream to be edited
     * @throws IllegalArgumentException when unsupported datastream is selected or foxml is not valid
     */
    public void processDatastream(String datastream) throws IllegalArgumentException, IOException {

        if (datastream == null) {
            throw new IllegalArgumentException("Datastream cannot be null.");
        }

        Element thisElement = Utils.filterDatastreamFromDocument(doc, datastream);

        if (thisElement == null) {
            if (!(datastream.equals(DATASTREAM_ALTO) || datastream.equals(DATASTREAM_OCR))) {
                throw new IllegalStateException("Missing datastream " + datastream);
            } else {
                return;
            }
        }

        Element parent = (Element) thisElement.getParentNode();
        removeFedoraURI(parent);

        if (
                datastream.equals(DATASTREAM_IMG_FULL) ||
                datastream.equals(DATASTREAM_IMG_PREVIEW) ||
                datastream.equals(DATASTREAM_IMG_THUMB))
        {
            processImgDatastream(datastream);
        } else if (
                datastream.equals(DATASTREAM_ALTO) ||
                datastream.equals(DATASTREAM_OCR))
        {
            processOcrDatastream(datastream);
        }

    }

    private void processImgDatastream(String datastream) {
        if (
            !(
                datastream.equals(DATASTREAM_IMG_FULL) ||
                datastream.equals(DATASTREAM_IMG_PREVIEW) ||
                datastream.equals(DATASTREAM_IMG_THUMB)))
        {
            throw new IllegalArgumentException("Unsupported img datastream: " + datastream + ".");
        }

        Element img = Utils.filterDatastreamFromDocument(doc, datastream);

        if (img == null) {
            throw new IllegalArgumentException("missing " + datastream);
        }

        removeBinaryContent(img);
        setControlGroup((Element) img.getParentNode(), "E");

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

    private void processOcrDatastream(String datastream) throws IOException {
        if (!(
                datastream.equals(DATASTREAM_ALTO) ||
                datastream.equals(DATASTREAM_OCR)
            ))
        {
            throw new IllegalArgumentException("Unsupported datastream: " + datastream + ".");
        }

        Element ocr = Utils.filterDatastreamFromDocument(doc, datastream);

        if (ocr == null) {
            throw new IllegalArgumentException("missing " + datastream);
        }

        removeBinaryContent(ocr);

        String content = removeContentLocation(ocr);

        if (content != null) {
            Element binaryContent = doc.createElement("binaryContent");
            binaryContent.setTextContent(content);

            ocr.appendChild(binaryContent);
        }
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

    private void setControlGroup(Element img, String e) {
        img.setAttribute("CONTROL_GROUP", e);
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
            System.err.println("Warning: Element " + e.getTagName() + " does not contain binaryContent element");
            return;
        }

        e.removeChild(bC);

        e.removeAttribute("SIZE");
    }

    private String removeContentLocation(Element e) throws IOException {

        // fedora does not have connection to proarc fedora
        Element cL = (Element) e.getElementsByTagName("contentLocation").item(0);

        cL.setAttribute("TYPE", "URL");

        String refAttrVal = cL.getAttribute("REF");
        refAttrVal = refAttrVal.replaceAll("localhost:8080", "proarc.staff.mzk.cz:1993");

        String content = getBase64Encoded(refAttrVal);

        NodeList imgFullChildren = e.getChildNodes();

        Element bC = null;

        for (int i = 0; i < imgFullChildren.getLength(); i++){
            if (imgFullChildren.item(i).getNodeName().equals("contentLocation")) {
                if (bC == null) {
                    bC = (Element) imgFullChildren.item(i);
                } else {
                    throw new IllegalArgumentException("contains multiple contentLocation in " + e.getTagName());
                }
            }
        }

        if (bC == null) {
            System.err.println("Warning: Element " + e.getTagName() + " does not contain contentLocation element");
            return null;
        }

        e.removeChild(bC);

        return content;
    }

    public String getBase64Encoded(String imageURL) throws IOException {
        java.net.URL url = new java.net.URL(imageURL);
        InputStream is = url.openStream();
        byte[] bytes = org.apache.commons.io.IOUtils.toByteArray(is);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public void save(Path toKramerius) throws TransformerException {
        File krameriusFoxml = toKramerius.resolve(uuid + ".xml").toFile();

        krameriusFoxml.setWritable(true, false);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult streamResult =  new StreamResult(toKramerius.resolve(uuid + ".xml").toFile());
        transformer.transform(source, streamResult);
    }

}
