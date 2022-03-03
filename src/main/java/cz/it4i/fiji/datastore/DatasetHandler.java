package cz.it4i.fiji.datastore;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.janelia.saalfeldlab.n5.N5Writer;

import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;

public interface DatasetHandler {

	static final int INITIAL_VERSION = 0;

	static final Pattern WHOLE_NUMBER_PATTERN = Pattern.compile("\\d+");

	SpimData getSpimData() throws SpimDataException;

	default void saveSpimData(SpimData data) throws SpimDataException {
		Collection<Integer> versions = new LinkedList<>();
		if (versions.isEmpty()) {
			versions = Collections.singleton(INITIAL_VERSION);
		}
		for (Integer version : versions) {
			saveSpimData(data, version);
		}
	}

	void saveSpimData(SpimData data, int version) throws SpimDataException;

	int createNewVersion() throws IOException;

	Collection<Integer> getAllVersions() throws IOException;

	default public N5Writer getWriter() throws IOException {
		return getWriter(getLatestVersion());
	}

	default public N5Writer getWriter(String version) throws IOException {
		if (!WHOLE_NUMBER_PATTERN.matcher(version).matches()) {
			return null;
		}
		return getWriter(Integer.parseInt(version));
	}

	N5Writer getWriter(int versionNumber) throws IOException;

	default int getLatestVersion() throws IOException {
		return Collections.max(getAllVersions());
	}

	void makeAsInitialVersion(int version) throws IOException;

	void deleteVersion(int version) throws IOException;

	default N5Writer constructChainOfWriters() throws IOException {
		return constructChainOfWriters(getLatestVersion());
	}

	default N5Writer constructChainOfWriters(int version) throws IOException {

		N5WriterItemOfChain result = null;
		List<Integer> versions = new LinkedList<>(this.getAllVersions());
		Collections.sort(versions);
		for (Integer i : versions) {
			if (i > version) {
				continue;
			}
			result = new N5WriterItemOfChain(this.getWriter(i), result);
		}
		return result;
	}

	String getUUID();

	void deleteDataset();

	String getLabel();

	void setLabel(String label);

}
