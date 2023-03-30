package cz.it4i.fiji.datastore.zarr;

import cz.it4i.fiji.datastore.DatasetFilesystemHandler;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;

import java.io.IOException;
import java.nio.file.Path;

import static cz.it4i.fiji.datastore.DatasetPathRoutines.getDataDirectory;
import static cz.it4i.fiji.datastore.DatasetPathRoutines.getDatasetVersionDirectory;

public class DatasetFileSystemHandlerZarr extends DatasetFilesystemHandler {
    private final Path pathOfDataset;
    public DatasetFileSystemHandlerZarr(String auuid, Path path) {
        super(auuid, path);
        pathOfDataset=path;
    }
    static private Path getDataPath(Path pathOfDataset, int latestVersion) {
        return getDataDirectory(getDatasetVersionDirectory(pathOfDataset,
                latestVersion));
    }
    @Override
    public N5Writer getWriter(int versionNumber) throws IOException {
        Path result =getDataPath(pathOfDataset, versionNumber);
        return new N5ZarrWriter(result.toString());
    }
}
