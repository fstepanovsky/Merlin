package cz.mzk.osdd.merlin.models;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Created by Jakub Kremlacek on 3.4.17.
 */
public class Title {
    private static final String UNKNOWN_BASE_NAME = "mrln";

    private static final String FILE_K4_SUFFIX = ".xml";
    private static final String FILE_IMAGE_SUFFIX = ".jp2";

    private static final String IMAGESERVER_REQUIRED_DIR_PERMS = "rwxrwxr-x";
    private static final String IMAGESERVER_REQUIRED_FILE_PERMS = "rwxr-xr--";

    private final String OUTPUT_PACK_PATH;
    private final boolean LOUD;

    private final Path LOCATION;

    private String parentUUID;
    private String sysno = null;
    private String base = null;

    private final Map<String, ExportPack> packs = new HashMap<>();
    private final Map<String, ExportPack> nonPagePacks = new HashMap<>();
    //private List<String> nonPageFOXMLs = null;

    public Title(Path location, File[] files, String outputPackPath, boolean loud) throws IllegalArgumentException, ParserConfigurationException, SAXException, IOException {
        this.OUTPUT_PACK_PATH = outputPackPath;
        this.LOUD = loud;

        this.LOCATION = location;

        checkFiles(files);
    }

    private void checkFiles(File[] files) throws IllegalArgumentException, IOException, SAXException, ParserConfigurationException {
        for (File f : files) {
            String uuid;

            if (f.getName().endsWith(FILE_IMAGE_SUFFIX)) {
                uuid = f.getName().substring(0, f.getName().length() - FILE_IMAGE_SUFFIX.length());

                if (packs.containsKey(uuid)) {
                    packs.get(uuid).setHasImageExport(f.getPath());
                } else {
                    ExportPack p = new ExportPack(uuid);
                    p.setHasImageExport(f.getPath());

                    packs.put(uuid, p);
                }

            } else if (f.getName().endsWith(FILE_K4_SUFFIX)) {
                uuid = f.getName().substring(0, f.getName().length() - FILE_K4_SUFFIX.length());

                if (packs.containsKey(uuid)) {
                    packs.get(uuid).setHasKrameriusExport(f.getPath());
                } else {
                    ExportPack p = new ExportPack(uuid);
                    p.setHasKrameriusExport(f.getPath());

                    packs.put(uuid, p);
                }
            } else if (f.getName().equals("proarc_export_status.log")) {
                //continue;
            } else {
                throw new IllegalArgumentException("Unknown file type : " + f.getName());
            }
        }

        List<String> uuidsToRemove = new LinkedList<>();

        for (ExportPack pack : packs.values()) {
            if (!isPage(pack)) {
                uuidsToRemove.add(pack.uuid);

                parentUUID = pack.uuid;

                if (pack.hasImageExport()) throw new IllegalArgumentException("UUID: " + pack.uuid + " contains image data for non data FOXML type!");
                continue;
            }

            if (!pack.hasImageExport() || !pack.hasKrameriusExport()) {
                String msg = !pack.hasKrameriusExport() ? "FOXML" : "Image";

                throw new IllegalArgumentException("Part " + pack.uuid + " is not valid. Missing " + msg + " part.");
            }
        }

        for (String uuid : uuidsToRemove) {
            nonPagePacks.put(uuid, packs.get(uuid));
            packs.remove(uuid);
        }

        //nonPageFOXMLs = uuidsToRemove;

        //Test UUID format
        UUID uuid = UUID.fromString(parentUUID);

        if (LOUD) {
            System.out.println("Title " + uuid + " filecheck completed.");
        }
    }

    private boolean isPage(ExportPack pack) throws ParserConfigurationException, IOException, SAXException {
        Document doc = Utils.getDocumentFromFile(pack.getKrameriusExportPath().toFile());

        NodeList elements = doc.getElementsByTagName("datastream");

        for (int i = 0; i < elements.getLength(); i++) {
            if (((Element) elements.item(i)).getAttribute("ID").equals("IMG_FULL")) return true;
        }

        return false;
    }

    private static void reportMissingPart(ExportPack pack, String missingPart) {
        System.err.println("Pack " + pack.uuid + missingPart + ". Terminating.");
    }

    /**
     * @param outRoot used on pack export - stores locally for further processing
     * @param outKramerius used on direct export - stores foxml into kramerius drive
     * @param outImageserver used on direct export - stores jp2 into imageserver drive
     * @param alephDirectory used on direct export - stores files for updating aleph
     * @param k4Credentials used on direct export - request credentials
     * @param k4address used on direct exprot - address of k4 remote API
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IllegalArgumentException
     */
    public void processTitle(Path outRoot, Path outKramerius, Path outImageserver, File alephDirectory, String k4Credentials, String k4address) throws IOException, ParserConfigurationException, SAXException, IllegalArgumentException {

        if (LOUD) System.out.println("Started processing title " + parentUUID);

        if (LOUD) System.out.println("Receiving Sysno from Aleph");

        // workaround to still process doc even if not known by Aleph
        Pair<String, String> sb = null;
        try {
            sb = Utils.getSysnoWithBaseFromAleph(Utils.getModsFromRootObject(this.LOCATION));
        } catch (IllegalStateException e) {
            if (e.getMessage().equals(Utils.DOC_NOT_FOUND_EXCEPTION_MSG)) {
                sb = null;
            }
        }
        if (sb == null) {
            //throw new IllegalStateException("Could not receive Sysno and Base from Aleph for item: " + this.LOCATION);
            sb = new Pair<>(parentUUID.replaceAll("-", ""), UNKNOWN_BASE_NAME);
        } else if (LOUD){
            System.out.println("Received Sysno from Aleph");
        }

        sysno = sb.first;
        base = sb.second.toLowerCase();

        Path outFoxml =
                (outKramerius == null ?
                        outRoot.resolve(OUTPUT_PACK_PATH).resolve("kramerius") :
                        outKramerius
                ).resolve(parentUUID);

        if (!outFoxml.toFile().exists()) outFoxml.toFile().mkdirs();

        checkPermissions(outFoxml, true, true);

        Path imsDirectory;
        if (!base.equals(UNKNOWN_BASE_NAME)) {
            imsDirectory =
                    (outImageserver == null ?
                            outRoot.resolve(OUTPUT_PACK_PATH).resolve("imageserver") :
                            outImageserver
                    ).resolve(base).resolve(sysno.substring(0, 3)).resolve(sysno.substring(3, 6)).resolve(sysno.substring(6));
        } else { // when using fallback base sysno contains uuid which we want preserve
            imsDirectory =
                    (outImageserver == null ?
                            outRoot.resolve(OUTPUT_PACK_PATH).resolve("imageserver") :
                            outImageserver
                    ).resolve(base).resolve(sysno.substring(0, 3)).resolve(sysno.substring(3, 6)).resolve(sysno);
        }
        if (!imsDirectory.toFile().exists()) {
            createImageserverPath(outRoot, outImageserver, imsDirectory);
        }

        //page packs
        for (ExportPack pack : packs.values()) {

            Path imagePath = imsDirectory.resolve(pack.uuid + ".jp2");

            Foxml f = new Foxml(Utils.getDocumentFromFile(pack.getKrameriusExportPath().toFile()), sysno, base, pack.uuid);

            try {
                f.removeFedoraURIFromRoot();
                f.processDatastream(Foxml.DATASTREAM_IMG_FULL);
                f.processDatastream(Foxml.DATASTREAM_IMG_THUMB);
                f.processDatastream(Foxml.DATASTREAM_IMG_PREVIEW);
                f.processDatastream(Foxml.DATASTREAM_ALTO);
                f.processDatastream(Foxml.DATASTREAM_OCR);
                f.processDatastream(Foxml.DATASTREAM_BIBLIO_MODS);
                f.processDatastream(Foxml.DATASTREAM_DC);
                f.processDatastream(Foxml.DATASTREAM_RELS_EXT);
                f.processRDF();
                f.save(outFoxml);
            } catch (IllegalArgumentException e) {
                reportMissingPart(pack, e.getMessage());
                return;
            } catch (TransformerException e) {
                System.err.println("Cannot save " + pack.uuid + ".xml. Terminating.");
                e.printStackTrace();
                return;
            }

            try {
                Files.copy(pack.getImageExportPath(), imagePath);
                Files.setPosixFilePermissions(
                        imagePath,
                        PosixFilePermissions.fromString(
                                IMAGESERVER_REQUIRED_FILE_PERMS
                        ));
                checkPermissions(imagePath, false, false);

            } catch (FileAlreadyExistsException e) {
                System.out.println("Warning: Image: " + imagePath.getFileName() + " already exists at target destination: " + pack.getImageExportPath() + " Skipping.");
            }
        }

        //nonPagePacks
        for (ExportPack pack : nonPagePacks.values()) {
            Foxml f = new Foxml(Utils.getDocumentFromFile(pack.getKrameriusExportPath().toFile()), sysno, base, pack.uuid);

            try {
                f.removeFedoraURIFromRoot();
                f.processDatastream(Foxml.DATASTREAM_BIBLIO_MODS);
                f.processDatastream(Foxml.DATASTREAM_RELS_EXT);
                f.processDatastream(Foxml.DATASTREAM_DC);
                f.save(outFoxml);
            } catch (IllegalArgumentException e) {
                reportMissingPart(pack, e.getMessage());
                return;
            } catch (TransformerException e) {
                System.err.println("Cannot save " + pack.uuid + ".xml. Terminating.");
                e.printStackTrace();
                return;
            }
        }

        if (k4Credentials != null && k4address != null) {
            Utils.requestKrameriusImport(parentUUID, k4address, k4Credentials);
        }

        if (alephDirectory != null) {
            if (!base.equals(UNKNOWN_BASE_NAME)) {
                Utils.prepareAlephUpdateRecord(parentUUID, sysno, base, alephDirectory);
            }
        }

        if (LOUD) System.out.println("Title " + parentUUID + " processed.");
    }

    /**
     * Create directory structure for imageserver
     *
     * Creation processes assume you have 9 digits
     * sysno from Aleph (recognized using base) which is
     * split into 3 dirs by 3 digits
     * example sysno: 000329646, base: mzk01
     * dir structure: mzk01/000/329/646
     * or the base is set to UNKNOWN_BASE_NAME.
     *
     * If UNKNOWN_BASE_NAME is present in base
     * it is assumed that sysno contains root
     * title uuid from which is created 2 dirs
     * named by 3 chars each and 3rd dir is
     * whole uuid without hyphens for example
     * uuid: 2f30448f-78c5-4d41-981c-b4c0eae300a2, base: mrln
     * dir structure: mrln/2f3/044/2f30448f78c54d41981cb4c0eae300a2
     * @param outRoot
     * @param outImageserver if null outRoot + OUTPUT_PACK_PATH + "imageserver" is used instead
     * @param imsDirectory
     * @throws IOException
     */
    private void createImageserverPath(Path outRoot, Path outImageserver, Path imsDirectory) throws IOException {
        Files.createDirectories(
                imsDirectory,
                PosixFilePermissions.asFileAttribute(
                        PosixFilePermissions.fromString(
                                IMAGESERVER_REQUIRED_DIR_PERMS
                        )));

        //createDirectories is not trustworthy under docker
        Path subPath = (outImageserver == null ?
                outRoot.resolve(OUTPUT_PACK_PATH).resolve("imageserver") :
                outImageserver
        );

        subPath = subPath.resolve(base);
        checkPermissions(subPath, true, true);
        subPath = subPath.resolve(sysno.substring(0, 3));
        checkPermissions(subPath, true, true);
        subPath = subPath.resolve(sysno.substring(3, 6));
        checkPermissions(subPath, true, true);
        if (!base.equals(UNKNOWN_BASE_NAME)) {
            subPath = subPath.resolve(sysno.substring(6));
        } else {
            subPath = subPath.resolve(sysno);
        }
        checkPermissions(subPath, true, true);
    }

    private void checkPermissions(Path path, boolean groupWrite, boolean othersExecute) throws IOException {
        checkPermission(path, PosixFilePermission.GROUP_EXECUTE);
        if (groupWrite) {
            checkPermission(path, PosixFilePermission.GROUP_WRITE);
        }
        checkPermission(path, PosixFilePermission.OTHERS_READ);
        if (othersExecute) {
            checkPermission(path, PosixFilePermission.OTHERS_EXECUTE);
        }

    }

    private void checkPermission(Path path, PosixFilePermission perm) throws IOException {
        PosixFileAttributes attrs = Files.getFileAttributeView(path, PosixFileAttributeView.class).readAttributes();

        if (!attrs.permissions().contains(perm)) {
            Set<PosixFilePermission> perms = attrs.permissions();
            perms.add(perm);
            Files.setPosixFilePermissions(path, perms);

            attrs = Files.getFileAttributeView(path, PosixFileAttributeView.class).readAttributes();

            if (!attrs.permissions().contains(perm)) {
                throw new IllegalArgumentException("Unable to set group write permission");
            }
        }
    }
}



