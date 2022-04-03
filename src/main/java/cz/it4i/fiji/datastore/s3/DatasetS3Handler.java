
package cz.it4i.fiji.datastore.s3;

import static java.nio.charset.StandardCharsets.UTF_8;
import static mpicbg.spim.data.XmlKeys.SPIMDATA_TAG;

import com.google.common.base.Strings;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.janelia.saalfeldlab.n5.N5Writer;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import cz.it4i.fiji.datastore.DatasetHandler;
import lombok.extern.log4j.Log4j2;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.SpimDataIOException;
import mpicbg.spim.data.XmlIoSpimData;

@Log4j2
public class DatasetS3Handler implements DatasetHandler {

	public static final int INITIAL_VERSION = 0;
	private static final String LABEL_FILE = "label";

	private final String basePath;
	private final String uuid;
	private final S3Client s3Client;
	private final DatasetS3KeyRoutines keyRoutines;
	private final S3Settings settings;

	private String _label;

	public DatasetS3Handler(String auuid, String pathPrefix,
		S3Settings settings)
	{
		this.uuid = auuid;
		this.s3Client = new S3Client(settings);
		this.keyRoutines = new DatasetS3KeyRoutines(s3Client.getDelimiter());
		this.basePath = keyRoutines.resolve(pathPrefix, uuid);
		this.settings = settings;
	}

	@Override
	public SpimData getSpimData() throws SpimDataException {

		String path = getXMLPath(INITIAL_VERSION);
		log.debug("Loading spim data from {}", path);

		if (s3Client.fileExists(path.toString())) {
			try (final InputStream inputStream = s3Client.getInputStream(path))
			{

				final SAXBuilder sax = new SAXBuilder();
				Document doc;
				try {
					doc = sax.build(inputStream);
				}
				catch (final Exception e) {
					throw new SpimDataIOException(e);
				}
				final Element root = doc.getRootElement();

				if (root.getName() != SPIMDATA_TAG) throw new RuntimeException(
					"expected <" + SPIMDATA_TAG + "> root element. wrong file?");

				return new XmlIoSpimData().fromXml(root, new File(path));
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		log.error("File {} does not exist", path);
		return null;
	}

	@Override
	public void saveSpimData(SpimData spimData, int version)
		throws SpimDataException
	{
		final String xmlFileName = getXMLPath(version);
		final File xmlFileDirectory = new File(".");
		final Document doc = new Document(new XmlIoSpimData().toXml(spimData,
			xmlFileDirectory));
		final XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
		try (OutputStream os = s3Client.getOutputStream(xmlFileName)) {
			xout.output(doc, os);
		}
		catch (final IOException e) {
			throw new SpimDataIOException(e);
		}
	}

	@Override
	public int createNewVersion() throws IOException {
		int latestVersion = getLatestVersion();
		int newVersion = latestVersion + 1;
		createNewVersion(latestVersion, newVersion);
		return newVersion;
	}

	@Override
	public Collection<Integer> getAllVersions() throws IOException {
		return getAllVersions(basePath);
	}

	@Override
	public N5Writer getWriter(final int versionNumber) throws IOException {
		return new N5S3Writer(keyRoutines.getDataKey(keyRoutines
			.getDatasetVersionKey(basePath, versionNumber)), settings);
	}

	@Override
	public void makeAsInitialVersion(final int version) throws IOException {
		String srcVersionPrefix = getPrefixForVersion(version);
		String newVersionPrefix = getPrefixForVersion(INITIAL_VERSION);
		forAllItems(srcVersionPrefix, src -> {
			final String dst = src.replaceFirst(srcVersionPrefix, newVersionPrefix);
			s3Client.copy(src, dst);
			s3Client.deleteFile(src);
		});
	}

	@Override
	public void deleteVersion(final int version) throws IOException {
		deleteAllWithPrefix(getPrefixForVersion(version));
	}

	@Override
	public String getUUID() {
		return uuid;
	}

	@Override
	public void deleteDataset() {
		deleteAllWithPrefix(basePath);
	}

	@Override
	public String getLabel() {
		if (_label == null) {
			String labelKey = keyRoutines.resolve(basePath, LABEL_FILE);
			if (!s3Client.fileExists(labelKey)) {
				_label = "";
			}
			else {
				try (BufferedReader br = new BufferedReader(new InputStreamReader(
					s3Client.getInputStream(labelKey), UTF_8)))
				{
					_label = br.readLine();
				}
				catch (IOException exc) {
					log.error("getLabel", exc);
				}
			}
		}
		return _label;
	}

	@Override
	public void setLabel(String label) {

		_label = Strings.nullToEmpty(label);
		if (!_label.isEmpty()) {
			try (Writer writer = new OutputStreamWriter(s3Client.getOutputStream(
				keyRoutines.resolve(basePath, LABEL_FILE)), UTF_8))
			{
				writer.write(label);
			}
			catch (IOException exc) {
				log.error("setLabel", exc);
			}
		}
		else {
			s3Client.deleteFile(keyRoutines.resolve(basePath, LABEL_FILE));
		}

	}

	private void createNewVersion(int srcVersion, int newVersion) {
		String srcVersionPrefix = getPrefixForVersion(srcVersion);
		String newVersionPrefix = getPrefixForVersion(newVersion);
		forAllItems(srcVersionPrefix, key -> {
			if (isBlockKey(key)) {
				return;
			}
			final String dst = key.replaceFirst(srcVersionPrefix, newVersionPrefix);
			s3Client.copy(key, dst);
		});
	}

	private void deleteAllWithPrefix(final String prefix) {
		forAllItems(prefix, key -> {
			s3Client.deleteFile(key);
		});
	}

	private String getPrefixForVersion(int version) {
		return keyRoutines.resolve(basePath, Integer.toString(version));
	}

	private void forAllItems(String prefix, Consumer<String> keyConsumer) {
		s3Client.streamObjects(prefix).forEach(keyConsumer);
	}


	private Collection<Integer> getAllVersions(final String path) {
		return s3Client.getSubkeys(s3Client.streamFolderObjects(path)).map(
			Integer::valueOf).collect(Collectors.toList());
	}

	private boolean isBlockKey(String src) {
		int lastOccurenceOfDelimiter = src.lastIndexOf(s3Client.getDelimiter());
		if (lastOccurenceOfDelimiter != -1) {
			src = src.substring(lastOccurenceOfDelimiter + 1, src.length());
		}
		return WHOLE_NUMBER_PATTERN.matcher(src).matches();
	}

	private String getXMLPath(int version) {
		String datasetKey = keyRoutines.getDatasetVersionKey(basePath, version);
		return keyRoutines.getXMLKey(datasetKey);
	}

}
