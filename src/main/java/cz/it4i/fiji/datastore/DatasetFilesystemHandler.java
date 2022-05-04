/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;


import static cz.it4i.fiji.datastore.DatasetPathRoutines.getDataDirectory;
import static cz.it4i.fiji.datastore.DatasetPathRoutines.getDatasetVersionDirectory;
import static cz.it4i.fiji.datastore.DatasetPathRoutines.getXMLFile;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Objects;

import javax.ws.rs.NotFoundException;

import org.apache.commons.io.FileUtils;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;

import bdv.img.n5.N5ImageLoader;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;

public class DatasetFilesystemHandler implements DatasetHandler {

	private final Path pathOfDataset;

	private final String uuid;

	public DatasetFilesystemHandler(String auuid, Path path) {
		pathOfDataset = path;
		uuid = auuid;
	}

	public DatasetFilesystemHandler(String auuid, String path) {
		this(auuid, Paths.get(path));
	}

	@Override
	public SpimData getSpimData() throws SpimDataException {
		return getSpimData(INITIAL_VERSION);
	}

	@Override
	public SpimData getSpimData(int version) throws SpimDataException {

		try {
			int versionForReading = version < 0 ? getLatestVersion() : version;
			return SetN5LoaderToSpimData.$(loadFromXML(getXMLFile(
				getDatasetVersionDirectory(pathOfDataset, versionForReading))),
				seq -> new cz.it4i.fiji.datastore.N5ImageLoader(() -> getReader(
					version), seq.getViewSetupsOrdered()), new File(""));

		}
		catch (IOException exc) {
			throw new SpimDataException(exc);
		}

	}

	@Override
	public void saveSpimData(SpimData data, int version)
		throws SpimDataException
	{
		data = setN5ImageLoadeToSpimData(data, version);
		new XmlIoSpimData().save(data, getXMLFile(getDatasetVersionDirectory(
			pathOfDataset, version)).toString());
	}

	@Override
	public int createNewVersion() throws IOException {
		int latestVersion = getLatestVersion();
		int newVersion = latestVersion + 1;
		createNewVersion(getDatasetVersionDirectory(pathOfDataset, latestVersion), getDatasetVersionDirectory(
			pathOfDataset, newVersion));
		return newVersion;
	}

	@Override
	public Collection<Integer> getAllVersions() throws IOException {
		Collection<Integer> result = new LinkedList<>();
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(pathOfDataset)) {
			for (Path p : (Iterable<Path>) (() -> ds.iterator())) {
				if (!isBlockFileDirOrVersion(p.toFile())) {
					continue;
				}
				Integer temp = Integer.valueOf(p.getFileName().toString());
				result.add(temp);
			}
		}
		return result;
	}

	@Override
	public N5Writer getWriter(int versionNumber) throws IOException {
		Path result = getDataPath(pathOfDataset, versionNumber);
		return new N5FSWriter(result.toString());
	}

	@Override
	public int getLatestVersion() throws IOException {
		return Collections.max(getAllVersions());
	}

	@Override
	public void makeAsInitialVersion(int version) throws IOException {
		Path versionPath = getDatasetVersionDirectory(pathOfDataset, version);
		Path initialVersionPath = getDatasetVersionDirectory(pathOfDataset,
			INITIAL_VERSION);
		Files.move(versionPath, initialVersionPath, StandardCopyOption.ATOMIC_MOVE);
	}

	@Override
	public void deleteVersion(int version) throws IOException {
		Path versionPath = getDatasetVersionDirectory(pathOfDataset, version);
		if(!Files.exists(versionPath)) {
			throw new NotFoundException("Dataset with uuid=" + uuid +
				" does not have version " + version);
		}
		// At least one version should remain
		if (getAllVersions().size() == 1) {
			throw new IllegalStateException("Version " + version +
				" is the last version in dataset " + uuid);
		}
		FileUtils.deleteDirectory(versionPath.toFile());
	}
	
	@Override
	public String getUUID() {
		return uuid;
	}

	@Override
	public void deleteDataset() {
		try {
			FileUtils.deleteDirectory(pathOfDataset.toFile());
		}
		catch (IOException exc) {
			throw new UncheckedExecutionException(exc);
		}
	}

	@Override
	public String getLabel() {
		try {
			return Files.list(pathOfDataset).filter(Files::isRegularFile).findFirst()
				.map(Path::getFileName).map(Path::toString).orElse(null);
		}
		catch (IOException exc) {
			throw new UncheckedIOException(exc);
		}
	}

	@Override
	public void setLabel(String label) {
		String oldLabel = getLabel();
		if (Objects.equals(oldLabel, label)) {
			return;
		}
		if (!Strings.nullToEmpty(oldLabel).isBlank()) {
			try {
				Files.deleteIfExists(pathOfDataset.resolve(oldLabel));
			}
			catch (IOException exc) {
				throw new UncheckedIOException(exc);
			}
		}

		if (!Strings.nullToEmpty(label).isBlank()) {
			try {
				Files.createFile(pathOfDataset.resolve(label));
			}
			catch (IOException exc) {
				throw new UncheckedIOException(exc);
			}
		}

	}

	private N5Reader getReader(int version) {
		try {
			if (version >= 0) {
				return getWriter(version);
			}
			return constructChainOfWriters();
		}
		catch (IOException exc) {
			throw new UncheckedIOException(exc);
		}
	}

	private SpimData setN5ImageLoadeToSpimData(SpimData spimData, int version) {
		File dataDirectory = DatasetPathRoutines.getDataDirectory(
			getDatasetVersionDirectory(pathOfDataset, version)).toFile();
		return SetN5LoaderToSpimData.$(spimData, seq -> new N5ImageLoader(
			dataDirectory, seq), dataDirectory);
	}

	private void createNewVersion(Path src, Path dst) throws IOException {
		FileUtils.copyDirectory(src.toFile(), dst.toFile(),
			DatasetFilesystemHandler::isNotBlockFileOrDir);
	}

	static private Path getDataPath(Path pathOfDataset, int latestVersion) {
		return getDataDirectory(getDatasetVersionDirectory(pathOfDataset,
			latestVersion));
	}

	private static boolean isBlockFileDirOrVersion(File file) {
		return WHOLE_NUMBER_PATTERN.matcher(file.getName().toString()).matches();
	}

	private static boolean isNotBlockFileOrDir(File file) {
		return !isBlockFileDirOrVersion(file);
	}

	private static SpimData loadFromXML(Path path) throws SpimDataException {
		return new XmlIoSpimData().load(path.toString());
	}
}
