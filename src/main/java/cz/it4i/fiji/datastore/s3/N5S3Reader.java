
package cz.it4i.fiji.datastore.s3;

import com.google.common.collect.Maps;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.janelia.saalfeldlab.n5.AbstractGsonReader;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.DefaultBlockReader;
import org.janelia.saalfeldlab.n5.GsonAttributesParser;

class N5S3Reader extends AbstractGsonReader {

	protected static final String JSON_FILE = "attributes.json";

	protected final S3Client s3Client;

	protected final String basePath;

	protected final DatasetS3KeyRoutines keyRoutines;

	public N5S3Reader(final String basePath, final S3Settings settings,
		final GsonBuilder gsonBuilder)
		throws IOException
	{

		super(gsonBuilder);
		this.basePath = basePath;
		this.s3Client = new S3Client(settings);
		this.keyRoutines = new DatasetS3KeyRoutines(s3Client.getDelimiter());
		if (exists("/")) {
			final Version version = getVersion();
			if (!VERSION.isCompatible(version)) throw new IOException(
				"Incompatible version " + version + " (this is " + VERSION + ").");
		}
	}

	public N5S3Reader(final String basePath, final S3Settings settings)
		throws IOException
	{
		this(basePath, settings, new GsonBuilder());
	}

	@Override
	public String toString() {
		return String.format("%s[basePath=%s]", getClass().getSimpleName(),
			basePath);
	}

	public String getBasePath() {
		return this.basePath;
	}

	@Override
	public boolean exists(final String path) {
		final String fullPath = keyRoutines.resolve(basePath, path) + s3Client
			.getDelimiter();
		return this.s3Client.directoryExists(fullPath);
	}

	@Override
	public HashMap<String, JsonElement> getAttributes(final String pathName)
		throws IOException
	{
		final String path = keyRoutines.resolve(basePath, pathName);

		final String s3FileName = getAttributesPath(path);

		if (!this.s3Client.fileExists(s3FileName)) {
			return Maps.newHashMap();
		}

		final InputStream inputStream = this.s3Client.getInputStream(s3FileName);
		final Reader reader = Channels.newReader(Channels.newChannel(inputStream),
			StandardCharsets.UTF_8.name());

		return GsonAttributesParser.readAttributes(reader, getGson());
	}

	@Override
	public DataBlock<?> readBlock(final String pathName,
		final DatasetAttributes datasetAttributes, final long[] gridPosition)
		throws IOException
	{

		final String path = getDataBlockPath(pathName, gridPosition);
		if (!this.s3Client.fileExists(path.toString())) {
			return null;
		}
		try (final InputStream inputStream = this.s3Client.getInputStream(path)) {
			return DefaultBlockReader.readBlock(inputStream, datasetAttributes,
				gridPosition);
		}
	}


	@Override
	public String[] list(final String pathName) throws IOException {
		final String path = keyRoutines.resolve(basePath, pathName);
		return s3Client.getSubkeys(s3Client.streamFolderObjects(path)).toArray(
			String[]::new);
	}

	protected String getDataBlockPath(String pathName,
		final long[] gridPosition)
	{
		final String datasetPathName = keyRoutines.resolve(basePath, pathName);
		String delimiter = s3Client.getDelimiter();
		return keyRoutines.resolve(datasetPathName, Arrays.stream(gridPosition)
			.mapToObj(Long::toString).collect(Collectors.joining(delimiter)));
	}

	protected String getAttributesPath(final String pathName) {
		return keyRoutines.resolve(pathName, JSON_FILE);
	}
}
