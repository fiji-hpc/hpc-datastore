/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/

package cz.it4i.fiji.datastore;

import static cz.it4i.fiji.datastore.DatasetPathRoutines.getXMLPath;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.N5Writer;

import cz.it4i.fiji.datastore.register_service.OperationMode;
import mpicbg.spim.data.SpimDataException;

@Default
@ApplicationScoped
public class DatasetServerImpl implements Closeable, Serializable {


	private static final long serialVersionUID = -2060288635563742563L;

	private static final Set<OperationMode> READING_MODES = EnumSet.of(
		OperationMode.READ, OperationMode.READ_WRITE);

	private static final Set<OperationMode> WRITING_MODES = EnumSet.of(
		OperationMode.WRITE, OperationMode.READ_WRITE);

	N5Access n5Access;

	@Inject
	ApplicationConfiguration configuration;

	String uuid;

	private int version;

	private boolean mixedVersion;

	private OperationMode mode;

	private List<int[]> resolutionLevels;

	private DatasetFilesystemHandler datasetFilesystemHandler;

	public synchronized void init(String aUuid, List<int[]> resolutions,
		int aVersion, boolean aMixedVersion, OperationMode aMode)
		throws SpimDataException, IOException
	{
		uuid = aUuid;
		version = aVersion;
		mixedVersion = aMixedVersion;
		mode = aMode;
		resolutionLevels = resolutions;
		datasetFilesystemHandler = new DatasetFilesystemHandler(uuid.toString(),
			configuration
				.getDatasetPath(uuid.toString()));
		initN5Access();
	}

	@Override
	public synchronized void close() {
		uuid = null;
		n5Access = null;
	}

	public DataBlock<?> read(long[] gridPosition, int time, int channel,
		int angle) throws IOException
	{
		if (!READING_MODES.contains(mode)) {
			throw new IllegalStateException("Cannot read in mode: " + mode);
		}
		return n5Access.read(gridPosition, time, channel, angle);
	}

	public void write(long[] gridPosition, int time, int channel, int angle,
		InputStream inputStream) throws IOException
	{
		if (!WRITING_MODES.contains(mode)) {
			throw new IllegalStateException("Cannot write in mode: " + mode);
		}
		n5Access.write(gridPosition, time, channel, angle, inputStream);
	}


	public DataType getType(int time, int channel, int angle) {
		return n5Access.getType(time, channel, angle);
	}

	private void initN5Access() throws SpimDataException, IOException {
		n5Access = new N5Access(getXMLPath(configuration.getDatasetPath(uuid
			.toString()),
			datasetFilesystemHandler.getLatestVersion()), createN5Writer(),
			resolutionLevels, mode);
	}



	private N5Writer createN5Writer() throws IOException {
		if (mixedVersion) {
			if (mode.allowsWrite()) {
				throw new IllegalArgumentException("Write is not possible for mixed version");
			}
			return datasetFilesystemHandler.constructChainOfWriters(version);
		}
		return datasetFilesystemHandler.getWriter(version);

	}

	private void readObject(java.io.ObjectInputStream in) throws IOException,
		ClassNotFoundException
	{
		in.defaultReadObject();
		try {
			initN5Access();
		}
		catch (SpimDataException exc) {
			throw new IOException(exc);
		}
	}

	public static void versionNotFound(int version) {
		throw new NotFoundException("version " + version + " not found");
	}
}
