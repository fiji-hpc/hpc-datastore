
package cz.it4i.fiji.datastore.zarr;

import static cz.it4i.fiji.datastore.DatasetPathRoutines.getDataDirectory;
import static cz.it4i.fiji.datastore.DatasetPathRoutines.getDatasetVersionDirectory;

import java.io.IOException;
import java.nio.file.Path;

import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;

import cz.it4i.fiji.datastore.DatasetFilesystemHandler;

public class DatasetFileSystemHandlerZarr extends DatasetFilesystemHandler {

	private final Path pathOfDataset;

	public DatasetFileSystemHandlerZarr(String auuid, Path path) {
		super(auuid, path);
		pathOfDataset = path;
	}

	static private Path getDataPath(Path pathOfDataset, int latestVersion) {
		return getDataDirectory(getDatasetVersionDirectory(pathOfDataset,
			latestVersion));
	}

	@Override
	public N5Writer getWriter(int versionNumber) throws IOException {
		Path result = getDataPath(pathOfDataset, versionNumber);
		return new N5ZarrWriter( result.toString() );
	}
}
