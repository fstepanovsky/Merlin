package cz.mzk.osdd.merlin.models;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by Jakub Kremlacek on 30.3.17.
 */
public class ExportPack {
    public final String uuid;

    private boolean hasKrameriusExport = false;
    private boolean hasImageExport = false;

    private Path krameriusExportPath;
    private Path imageExportPath;

    public ExportPack(String uuid) {
        this.uuid = uuid;
    }

    public boolean hasKrameriusExport() {
        return hasKrameriusExport;
    }

    public boolean hasImageExport() {
        return hasImageExport;
    }

    public Path getKrameriusExportPath() {
        return Paths.get(krameriusExportPath.toString());
    }

    public Path getImageExportPath() {
        return Paths.get(imageExportPath.toString());
    }

    public void setHasKrameriusExport(String path) {

        hasKrameriusExport = true;
        krameriusExportPath = Paths.get(path);
    }

    public void setHasImageExport(String path) {

        hasImageExport = true;
        imageExportPath = Paths.get(path);
    }

    public boolean isValid() {
        return hasKrameriusExport && hasImageExport;
    }
}
