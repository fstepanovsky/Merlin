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

    private static final String FILE_K4_SUFFIX = ".xml";
    private static final String FILE_IMAGE_SUFFIX = ".NDK_USER";

    private static final String IMAGESERVER_REQUIRED_DIR_PERMS = "rwxrwxr-x";
    private static final String IMAGESERVER_REQUIRED_FILE_PERMS = "rwxr-xr--";

    public final String OUTPUT_PACK_PATH;
    public final boolean LOUD;

    public final Path location;

    private String parentUUID;
    private String sysno = null;
    private String base = null;

    private Map<String, ExportPack> packs = new HashMap<>();
    private Map<String, ExportPack> nonPagePacks = new HashMap<>();
    //private List<String> nonPageFOXMLs = null;

    public Title(Path location, File[] files, String outputPackPath, boolean loud) throws IllegalArgumentException, ParserConfigurationException, SAXException, IOException {
        this.OUTPUT_PACK_PATH = outputPackPath;
        this.LOUD = loud;

        this.location = location;

        checkFiles(files);
    }

    public int getPackCount() {
        return packs.size();
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
                continue;
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
            System.out.println("Title " + uuid.toString() + " filecheck completed.");
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
        Pair<String, String> sb = Utils.getSysnoWithBaseFromAleph(Utils.getSignatureFromRootObject(this.location));

        if (sb == null) {
            System.err.println("Could not receive Sysno and Base from Aleph. Skipping title at: " + location);
            return;
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

        checkPermissions(outFoxml, true, true, true);

        Path imsDirectory =
                (outImageserver == null ?
                        outRoot.resolve(OUTPUT_PACK_PATH).resolve("imageserver") :
                        outImageserver
                ).resolve(base).resolve(sysno.substring(0, 3)).resolve(sysno.substring(3, 6)).resolve(sysno.substring(6, sysno.length()));

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
                checkPermissions(imagePath, false, false, false);

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

//        for (String foxml : nonPageFOXMLs) {
//            try {
//                Path targetPath = outFoxml.resolve(foxml + ".xml");
//
//                Files.copy(location.resolve(foxml + ".xml"), targetPath);
//
//                checkPermissions(targetPath, true, true, true);
//            } catch (FileAlreadyExistsException e) {
//                System.out.println("Warning: File: " + foxml + ".xml already exists. Skipping!");
//            }
//        }

        if (k4Credentials != null && k4address != null) {
            Utils.requestKrameriusImport(parentUUID, k4address, k4Credentials);
        }

        if (alephDirectory != null) {
            Utils.prepareAlephUpdateRecord(parentUUID, sysno, base, alephDirectory);
        }

        if (LOUD) System.out.println("Title " + parentUUID + " processed.");
    }

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
        checkPermissions(subPath, true, true, false);
        subPath = subPath.resolve(sysno.substring(0, 3));
        checkPermissions(subPath, true, true, false);
        subPath = subPath.resolve(sysno.substring(3, 6));
        checkPermissions(subPath, true, true, false);
        subPath = subPath.resolve(sysno.substring(6, sysno.length()));
        checkPermissions(subPath, true, true, false);
    }

    private void checkPermissions(Path path, boolean groupWrite, boolean othersExecute, boolean othersWrite) throws IOException {
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



