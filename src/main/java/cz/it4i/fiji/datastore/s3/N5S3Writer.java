
package cz.it4i.fiji.datastore.s3;

import static org.janelia.saalfeldlab.n5.GsonAttributesParser.insertAttributes;
import static org.janelia.saalfeldlab.n5.GsonAttributesParser.readAttributes;
import static org.janelia.saalfeldlab.n5.GsonAttributesParser.writeAttributes;

import com.google.gson.JsonElement;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.DefaultBlockWriter;
import org.janelia.saalfeldlab.n5.N5Writer;

import lombok.extern.log4j.Log4j2;

@Log4j2
class N5S3Writer extends N5S3Reader implements N5Writer {

	public N5S3Writer(String basePath, final S3Settings settings)
		throws IOException
	{
		super(basePath, settings);
		if (!VERSION.equals(getVersion())) {
			setAttribute("/", VERSION_KEY, VERSION.toString());
		}
	}

	@Override
	public void setAttributes(final String pathName,
		final Map<String, ?> attributes) throws IOException
	{
		final String path = keyRoutines.resolve(basePath, pathName);
		final HashMap<String, JsonElement> map = new HashMap<>();

		final String s3FileName = getAttributesPath(path);
		if (this.s3Client.fileExists(s3FileName)) {
			try (final Reader reader = new InputStreamReader(this.s3Client
				.getInputStream(s3FileName), StandardCharsets.UTF_8))
			{
				insertAttributes(map, readAttributes(reader, getGson()), gson);
			}
		}
		insertAttributes(map, attributes, gson);
		try (final Writer writer = new OutputStreamWriter(this.s3Client
			.getOutputStream(s3FileName), StandardCharsets.UTF_8))
		{

			writeAttributes(writer, map, gson);
		}
	}

	@Override
	public void createGroup(final String pathName) throws IOException {
		log.debug("Group doesn't need to be created for S3");
	}

	@Override
	public boolean remove(final String pathName) throws IOException {
		final String path = keyRoutines.resolve(basePath, pathName);
		final boolean result = this.s3Client.fileExists(path);
		this.s3Client.deleteFile(path);
		return result;
	}

	@Override
	public boolean remove() throws IOException {
		return remove("/");
	}

	@Override
	public <T> void writeBlock(final String pathName,
		final DatasetAttributes datasetAttributes, final DataBlock<T> dataBlock)
		throws IOException
	{
		final String path = getDataBlockPath(pathName, dataBlock.getGridPosition());
		try (OutputStream outputStream = this.s3Client.getOutputStream(path
			.toString()))
		{
			DefaultBlockWriter.writeBlock(outputStream, datasetAttributes, dataBlock);
		}
	}

	@Override
	public boolean deleteBlock(final String pathName, final long[] gridPosition)
		throws IOException
	{
		if (!s3Client.fileExists(pathName)) {
			return false;
		}
		s3Client.deleteFile(getDataBlockPath(pathName, gridPosition));
		return true;
	}
}

