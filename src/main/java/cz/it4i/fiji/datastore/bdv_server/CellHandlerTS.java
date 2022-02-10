package cz.it4i.fiji.datastore.bdv_server;

import com.google.gson.GsonBuilder;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;

import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.LoaderCache;
import net.imglib2.cache.ref.SoftRefLoaderCache;
import net.imglib2.img.cell.Cell;
import net.imglib2.realtransform.AffineTransform3D;

import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import bdv.BigDataViewer;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.img.hdf5.Hdf5ImageLoader;
import bdv.img.n5.BdvN5Format;
import bdv.img.remote.AffineTransform3DJsonSerializer;
import bdv.img.remote.RemoteImageLoader;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import cz.it4i.fiji.datastore.DatasetFilesystemHandler;
import cz.it4i.fiji.datastore.core.DatasetDTO;
import cz.it4i.fiji.datastore.core.HPCDatastoreImageLoaderMetaData;
import cz.it4i.fiji.datastore.register_service.Dataset;
import cz.it4i.fiji.datastore.register_service.DatasetAssembler;
import lombok.extern.log4j.Log4j2;
import mpicbg.spim.data.SpimDataException;

@Log4j2
public class CellHandlerTS 
{

	public static final int THUMBNAIL_WIDTH = 100;

	public static final int THUMBNAIL_HEIGHT = 100;

	/**
	 * Key for a cell identified by timepoint, setup, level, and index
	 * (flattened spatial coordinate).
	 */
	static class Key
	{
		private final int timepoint;

		private final int setup;

		private final int level;

		private final long index;

		private final String[] parts;

		/**
		 * Create a Key for the specified cell. Note that {@code cellDims} and
		 * {@code cellMin} are not used for {@code hashcode()/equals()}.
		 *
		 * @param timepoint
		 *            timepoint coordinate of the cell
		 * @param setup
		 *            setup coordinate of the cell
		 * @param level
		 *            level coordinate of the cell
		 * @param index
		 *            index of the cell (flattened spatial coordinate of the
		 *            cell)
		 */
		Key(final int timepoint, final int setup, final int level, final long index,
			final String[] parts)
		{
			this.timepoint = timepoint;
			this.setup = setup;
			this.level = level;
			this.index = index;
			this.parts = parts;

			int value = Long.hashCode( index );
			value = 31 * value + level;
			value = 31 * value + setup;
			value = 31 * value + timepoint;
			hashcode = value;
		}

		@Override
		public boolean equals( final Object other )
		{
			if ( this == other )
				return true;
			if ( !( other instanceof VolatileGlobalCellCache.Key ) )
				return false;
			final Key that = ( Key ) other;
			return ( this.index == that.index ) && ( this.timepoint == that.timepoint ) && ( this.setup == that.setup ) && ( this.level == that.level );
		}

		final int hashcode;

		@Override
		public int hashCode()
		{
			return hashcode;
		}
	}

	private final CacheLoader< Key, Cell< ? > > loader;

	private final LoaderCache< Key, Cell< ? > > cache;


	/**
	 * Full path of the dataset xml file this {@link CellHandlerTS} is serving,
	 * without the ".xml" suffix.
	 */
	private final String baseFilename;


	/**
	 * Cached dataset XML to be send to and opened by {@link BigDataViewer}
	 * clients.
	 */
	private final String datasetXmlString;

	/**
	 * Cached JSON representation of the {@link HPCDatastoreImageLoaderMetaData} to be
	 * send to clients.
	 */
	private final String metadataJson;

	/**
	 * Cached dataset.settings XML to be send to clients. May be null if no
	 * settings file exists for the dataset.
	 */
	private final String settingsXmlString;

	/**
	 * Full path to thumbnail png.
	 */
	private final String thumbnailFilename;


	CellHandlerTS(Dataset dataset, final String baseUrl, int version,
		final String xmlFilename,
		final String datasetName, final String thumbnailsDirectory)
		throws SpimDataException, IOException
	{
		final XmlIoSpimDataMinimal io = new XmlIoSpimDataMinimal();
		final SpimDataMinimal spimData = io.load( xmlFilename );
		DatasetFilesystemHandler tempDFH = new DatasetFilesystemHandler(dataset);
		final N5Writer writer = 0 <= version ? tempDFH.getWriter(version) : tempDFH
			.constructChainOfWriters();
		final Map<String, DatasetAttributes> perPathDatasetAttribute =
			new HashMap<>();

		loader = key -> {
			final int[] cellDims = new int[] {
					Integer.parseInt( key.parts[ 5 ] ),
					Integer.parseInt( key.parts[ 6 ] ),
					Integer.parseInt( key.parts[ 7 ] ) };
			final long[] cellMin = new long[] {
					Long.parseLong( key.parts[ 8 ] ),
					Long.parseLong( key.parts[ 9 ] ),
					Long.parseLong( key.parts[ 10 ] ) };
			DataBlock<?> block = readBlock(perPathDatasetAttribute, writer, key,
				cellMin, cellDims);
			return new Cell<>(cellDims, cellMin, block);

		};

		cache = new SoftRefLoaderCache<>();

		// dataSetURL property is used for providing the XML file by replace
		// SequenceDescription>ImageLoader>baseUrl
		baseFilename = xmlFilename.endsWith( ".xml" ) ? xmlFilename.substring( 0, xmlFilename.length() - ".xml".length() ) : xmlFilename;
		datasetXmlString = buildRemoteDatasetXML( io, spimData, baseUrl );
		metadataJson = buildMetadataJsonString(spimData, dataset);
		settingsXmlString = buildSettingsXML( baseFilename );
		thumbnailFilename = createThumbnail( spimData, baseFilename, datasetName, thumbnailsDirectory );
	}

	private DataBlock<?> readBlock(
		Map<String, DatasetAttributes> perPathDatasetAttribute, N5Writer writer,
		Key key, long[] cellMin,
		int[] cellDims) throws IOException
	{
		String path = BdvN5Format.getPathName(key.setup, key.timepoint, key.level);
		DatasetAttributes datasetAttributes = getDatasetAttributes(perPathDatasetAttribute, writer, path);
		long[] gridPosition = new long[cellMin.length];
		for (int i = 0; i < gridPosition.length; i++) {
			gridPosition[i] = cellMin[i] / cellDims[i];
		}
		DataBlock<?> result = writer.readBlock(path, datasetAttributes,
			gridPosition);

		return result;
	}

	public DatasetAttributes getDatasetAttributes(
		Map<String, DatasetAttributes> perPathDatasetAttribute, N5Writer writer,
		String path)
	{
		synchronized (perPathDatasetAttribute) {
			return perPathDatasetAttribute.computeIfAbsent(path, x -> {
				try {
					return writer.getDatasetAttributes(path);
				}
				catch (IOException exc) {
					throw new RuntimeException(exc);
				}
			});
		}
	}

	@SuppressWarnings("unchecked")
	public void runForCellOrInit(final HttpServletResponse response,
		final String cellString)
		throws IOException
	{
		final String[] parts = cellString.split("/");
		if (parts[0].equals("cell"))
		{
			final int index = Integer.parseInt( parts[ 1 ] );
			final int timepoint = Integer.parseInt( parts[ 2 ] );
			final int setup = Integer.parseInt( parts[ 3 ] );
			final int level = Integer.parseInt( parts[ 4 ] );
			final Key key = new Key( timepoint, setup, level, index, parts );
			// TODO - there should be another type
			short[] data;
			try
			{
				final Cell< ? > cell = cache.get( key, loader );
				DataBlock<short[]> dataBlock = (DataBlock<short[]>) cell.getData();
				if (dataBlock == null) {
					data = new short[0];
				}
				else {
					data = dataBlock.getData();
				}
			}
			catch ( ExecutionException e )
			{
				log.error("getData", e);
				data = new short[ 0 ];
			}
	
			final byte[] buf = new byte[ 2 * data.length ];
			for ( int i = 0, j = 0; i < data.length; i++ )
			{
				final short s = data[ i ];
				buf[ j++ ] = ( byte ) ( ( s >> 8 ) & 0xff );
				buf[ j++ ] = ( byte ) ( s & 0xff );
			}
	
			response.setContentType( "application/octet-stream" );
			response.setContentLength( buf.length );
			response.setStatus( HttpServletResponse.SC_OK );
			try (final OutputStream os = response.getOutputStream()) {
				os.write(buf);
			}
		}
		else if (parts[0].equals("init"))
		{
			respondWithString(response, "application/json", metadataJson);
		}
	}

	public void runForDataset(final HttpServletResponse response)
		throws IOException
	{
		respondWithString(response, "application/xml", datasetXmlString);
	}

	public void runForSettings(final HttpServletResponse response)
		throws IOException
	{
		if (settingsXmlString != null) {
			respondWithString(response, "application/xml", settingsXmlString);
		}
		else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "settings.xml");
		}
	}

	public void runForThumbnail(final HttpServletResponse response)
		throws IOException
	{
		provideThumbnail(response);
	}

	private void provideThumbnail(final HttpServletResponse response)
		throws IOException
	{
		final Path path = Paths.get( thumbnailFilename );
		if ( Files.exists( path ) )
		{
			final byte[] imageData = Files.readAllBytes(path);
			if ( imageData != null )
			{
				response.setContentType( "image/png" );
				response.setContentLength( imageData.length );
				response.setStatus( HttpServletResponse.SC_OK );

				try (final OutputStream os = response.getOutputStream()) {
					os.write(imageData);
				}
			}
		}
	}


	/**
	 * Create a JSON representation of the {@link HPCDatastoreImageLoaderMetaData}
	 * (image sizes and resolutions) provided by the given
	 * {@link Hdf5ImageLoader}.
	 * 
	 */
	private static String buildMetadataJsonString(SpimDataMinimal spimData,
		Dataset dataset)
	{
		DatasetDTO datasetDTO = DatasetAssembler.createDatatransferObject(dataset);
		final HPCDatastoreImageLoaderMetaData metadata =
			new HPCDatastoreImageLoaderMetaData(datasetDTO, spimData
				.getSequenceDescription(), DataType.fromString(dataset.getVoxelType()));
		final GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter( AffineTransform3D.class, new AffineTransform3DJsonSerializer() );
		gsonBuilder.enableComplexMapKeySerialization();
		return gsonBuilder.create().toJson( metadata );
	}

	private String buildRemoteDatasetXML(XmlIoSpimDataMinimal io,
		SpimDataMinimal spimData, String baseUrl) throws IOException,
		SpimDataException
	{
		StringWriter sw = new StringWriter();
		BuildRemoteDatasetXmlTS.run(io, spimData, new RemoteImageLoader(baseUrl,
			false), sw);
		return sw.toString();
	}



	/**
	 * Read {@code baseFilename.settings.xml} into a string if it exists.
	 *
	 * @return contents of {@code baseFilename.settings.xml} or {@code null} if
	 *         that file couldn't be read.
	 */
	private static String buildSettingsXML( final String baseFilename )
	{
		final String settings = baseFilename + ".settings.xml";
		if ( new File( settings ).exists() )
		{
			try
			{
				final SAXBuilder sax = new SAXBuilder();
				final Document doc = sax.build( settings );
				final XMLOutputter xout = new XMLOutputter( Format.getPrettyFormat() );
				final StringWriter sw = new StringWriter();
				xout.output( doc, sw );
				return sw.toString();
			}
			catch ( JDOMException | IOException e )
			{
				log.warn("Could not read settings file \"" + settings + "\"");
				log.warn(e.getMessage());
			}
		}
		return null;
	}

	/**
	 * Create PNG thumbnail file named "{@code <baseFilename>.png}".
	 */
	private static String createThumbnail( final SpimDataMinimal spimData, final String baseFilename, final String datasetName, final String thumbnailsDirectory )
	{
		final String thumbnailFileName = thumbnailsDirectory + "/" + datasetName + ".png";
		final File thumbnailFile = new File( thumbnailFileName );
		if ( !thumbnailFile.isFile() ) // do not recreate thumbnail if it already exists
		{
			final BufferedImage bi = makeThumbnail(spimData, baseFilename,
				THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
			try
			{
				ImageIO.write( bi, "png", thumbnailFile );
			}
			catch ( final IOException e )
			{
				log.warn("Could not create thumbnail png for dataset \"" +
					baseFilename + "\"");
				log.warn(e.getMessage());
			}
		}
		return thumbnailFileName;
	}

	@SuppressWarnings("unused")
	private static BufferedImage makeThumbnail(SpimDataMinimal spimData,
		String baseFilename2, int thumbnailWidth, int thumbnailHeight)
	{
		return new BufferedImage(thumbnailWidth, thumbnailHeight,
			BufferedImage.TYPE_USHORT_GRAY);
	}

	/**
	 * Handle request by sending a UTF-8 string.
	 */
	private static void respondWithString(final HttpServletResponse response,
		final String contentType, final String string) throws IOException
	{
		response.setContentType( contentType );
		response.setCharacterEncoding( "UTF-8" );
		response.setStatus( HttpServletResponse.SC_OK );

		try (final PrintWriter ow = response.getWriter()) {
			ow.write(string);
		}
	}
}
