/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.register_service;

import static net.imglib2.cache.img.ReadOnlyCachedCellImgOptions.options;

import com.google.common.base.Strings;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transactional;
import javax.transaction.UserTransaction;
import javax.ws.rs.NotFoundException;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;

import org.janelia.saalfeldlab.n5.Bzip2Compression;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.Lz4Compression;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.XzCompression;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

import bdv.export.ExportMipmapInfo;
import bdv.export.ExportScalePyramid;
import bdv.export.ExportScalePyramid.AfterEachPlane;
import bdv.export.ExportScalePyramid.LoopbackHeuristic;
import cz.it4i.fiji.datastore.ApplicationConfiguration;
import cz.it4i.fiji.datastore.CreateNewDatasetTS;
import cz.it4i.fiji.datastore.CreateNewDatasetTS.N5Description;
import cz.it4i.fiji.datastore.CreateNewDatasetTS.N5Description.N5DescriptionBuilder;
import cz.it4i.fiji.datastore.DatasetHandler;
import cz.it4i.fiji.datastore.DatasetServerImpl;
import cz.it4i.fiji.datastore.N5Access;
import cz.it4i.fiji.datastore.core.DatasetDTO;
import cz.it4i.fiji.datastore.core.MipmapInfoAssembler;
import cz.it4i.fiji.datastore.management.DataServerManager;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;

@Log4j2
@Default
@RequestScoped
public class DatasetRegisterServiceImpl {

	public static final int[] IDENTITY_RESOLUTION = new int[] { 1, 1, 1 };

	@Inject
	ApplicationConfiguration configuration;

	@Inject
	DataServerManager dataServerManager;

	@Inject
	DatasetRepository datasetDAO;

	@Inject
	UserTransaction transaction;

	@Inject
	WriteToVersionListener writeToVersionListener;

	private Map<String, Compression> name2compression = null;

	public void addExistingDataset(String uuid) throws IOException,
		SpimDataException, SystemException, NotSupportedException
	{
		DatasetHandler handler = configuration.getDatasetHandler(uuid);
		Collection<Integer> versions = handler.getAllVersions();
		int firstVersion = Collections.min(versions);
		final SpimData spimData = handler.getSpimData(firstVersion);
		final N5Reader reader = handler.getWriter(firstVersion);
		final String label = handler.getLabel();
		transaction.begin();
		boolean trxActive = true;
		try {
			try {
				datasetDAO.findByUUID(uuid);
				throw new DatasetAlreadyInsertedException(uuid);
			}
			catch (NotFoundException e) {
				// ignore this because it is correct behaviour
			}
			final Dataset dataset = DatasetAssembler.createDomainObject(uuid,
				versions, reader, spimData, label);
			log.info("Adding dataset: {}", dataset);
			datasetDAO.persist(dataset);
			trxActive = false;
			transaction.commit();
		}
		catch (RollbackException | HeuristicMixedException
				| HeuristicRollbackException
				| SystemException exc)
		{
			log.error("commit", exc);
		}
		finally {
			if (trxActive) {
				transaction.rollback();
			}
		}
	}

	public UUID createEmptyDataset(DatasetDTO datasetDTO) throws IOException,
		SpimDataException, NotSupportedException, SystemException
	{
		UUID result = UUID.randomUUID();
		new CreateNewDatasetTS().run(configuration.getDatasetHandler(result
			.toString()), convert(datasetDTO));
		transaction.begin();
		boolean trxActive = true;
		try {
			Dataset dataset = DatasetAssembler.createDomainObject(datasetDTO);
			dataset.setUuid(result.toString());
			dataset.setDatasetVersion(new LinkedList<>());
			dataset.getDatasetVersion().add(DatasetVersion.builder().value(0)
				.build());
			datasetDAO.persist(dataset);
			trxActive = false;
			transaction.commit();
		}
		catch (SecurityException | IllegalStateException | RollbackException
				| HeuristicMixedException | HeuristicRollbackException exc)
		{
			log.error("commit", exc);
		}
		finally {
			if (trxActive) {
				transaction.rollback();
			}
		}
		return result;
	}

	@Transactional
	public void addChannels(String uuid, int channels) throws SpimDataException,
		IOException
	{
		try {
			Dataset dataset = getDataset(uuid);
			log.debug("add {} channel for dataset with path ", channels);
			new AddChannelTS(configuration).run(dataset, channels,
				getCompressionMapping().get(
				Strings.nullToEmpty(dataset.getCompression().toUpperCase())));
			dataset.setChannels(dataset.getChannels() + channels);
			datasetDAO.persist(dataset);

		}
		catch (SecurityException | IllegalStateException exc)
		{
			log.error("commit", exc);
		}

	}

	@Transactional
	public void deleteDataset(String uuid) {
		Dataset dataset = getDataset(uuid);
		log.debug("dataset with UUID {} is deleted", uuid);
		DatasetHandler dfs = configuration.getDatasetHandler(uuid);
		datasetDAO.delete(dataset);
		dfs.deleteDataset();

	}

	public void deleteVersions(String uuid, List<Integer> versionList)
		throws IOException
	{
		DatasetHandler dfs = configuration.getDatasetHandler(uuid);
		for (Integer version : versionList) {
			dfs.deleteVersion(version);
		}
	}

	public DatasetDTO query(String uuid) throws SpimDataException {
		final Dataset dataset = getDataset(uuid);
		final SpimData spimData = configuration.getDatasetHandler(uuid)
			.getSpimData();
		final DatasetDTO result = DatasetAssembler.createDatatransferObject(dataset,
			spimData.getSequenceDescription().getTimePoints());
		return result;
	}

	public String getCommonMetadata(String uuid) {
		Dataset dataset = getDataset(uuid);
		return Strings.nullToEmpty(dataset.getMetadata());
	}

	public <T extends RealType<T> & NativeType<T>> void rebuild(String uuid,
		int version, int time, int channel,
		int angle) throws IOException, SpimDataException
	{
		
		Dataset dataset = getDataset(uuid);
		DatasetHandler dh = configuration.getDatasetHandler(uuid);

		N5Access n5Access = new N5Access(dh.getSpimData(), dh.getWriter(version),
			Collections.singletonList(dataset.getSortedResolutionLevels().get(0)
				.getResolutions()), OperationMode.READ_WRITE);

		final N5Writer writer = n5Access.getWriter();
		final RandomAccessibleInterval<T> img = N5Utils.open(writer, n5Access
			.getViewSetupTimepoint(time, channel, angle).getPath(
				IDENTITY_RESOLUTION));
		final T type = img.randomAccess().get();
		N5Access.ViewSetupTimepoint vst = n5Access.getViewSetupTimepoint(time,
			channel, angle);
		
		
		
		

		final N5DatasetIO<T> io = new N5DatasetIO<>(new SkippingScaleN5Writer(
			writer, 1), vst.getCompression(), vst.getViewSetup().getId(), time, type);
		//needs load class before call ExportScalePyramid.writeScalePyramid to avoid NuSuchMethodError
		options().getClass().getMethods();
		
		ExportMipmapInfo emi = createExportMipmapInfo(dataset
			.getSortedResolutionLevels().stream().skip(1).collect(Collectors
				.toList()));
		
		int numThreads = numThreads();
		final ExecutorService executorService = Executors.newFixedThreadPool(
			numThreads());
		try {
			final boolean isVirtual = false;
			final Runnable clearCache = () -> {};
			ExportScalePyramid.writeScalePyramid(img, type, emi, io, executorService,
				numThreads, createLoopbackHeuristic(isVirtual), createAfterEachPlane(
					isVirtual, clearCache), new LoggerProgressWriter(log, "Rebuild"));
		} finally {
			executorService.shutdown();
		}
	}



	@Transactional
	public void setCommonMetadata(String uuid, String commonMetadata) {
		Dataset dataset = getDataset(uuid);
		dataset.setMetadata(commonMetadata);
		datasetDAO.persist(dataset);
	}

	public URI start(String uuid, int[] r, String version, OperationMode mode,
		Long timeout) throws IOException
	{

		Dataset dataset = getDataset(uuid);
		if (null == dataset.getBlockDimension(r)) {
			throw new NotFoundException("Dataset with UUID=" + uuid +
				" has not resolution [" + IntStream.of(r).mapToObj(i -> "" + i).collect(
					Collectors.joining(",")) + "]");
		}
		int resolvedVersion = resolveVersion(dataset, version, mode);
		if (mode.allowsWrite()) {
			writeToVersionListener.writingToVersion(uuid, resolvedVersion);
		}
		return dataServerManager.startDataServer(dataset.getUuid(), r,
			resolvedVersion, version.equals("mixedLatest"), mode, timeout);
	}

	public URI start(String uuid, List<int[]> resolutions, Long timeout)
		throws IOException
	{
		Dataset dataset = getDataset(uuid);
		// called only for checking that all resolutions exists
		getNonIdentityResolutions(dataset, resolutions);
		mergeVersions(dataset);
		writeToVersionListener.writeToAllVersions(uuid);
		return dataServerManager.startDataServer(dataset.getUuid(), resolutions,
			timeout);

	}

	private void mergeVersions(Dataset dataset) throws IOException {
		DatasetHandler dfh = configuration.getDatasetHandler(dataset.getUuid());
		Collection<Integer> versions = dfh.getAllVersions();
		int minVersion = Collections.min(versions);
		for (int ver : versions.stream().filter(ver -> ver > minVersion).collect(
			Collectors.toList()))
		{
			dfh.deleteVersion(ver);
		}
		dfh.makeAsInitialVersion(minVersion);

	}

	/*private void checkeResolutions(List<ResolutionLevel> levels) {
		int[] previousResolution = null;
		for (ResolutionLevel level : levels) {
			int[] resolution = level.getResolutions();
			if (previousResolution == null) {
				previousResolution = resolution;
				continue;
			}
			for (int dim = 0; dim < previousResolution.length; dim++) {
				if (resolution[dim] % previousResolution[dim] != 0) {
					throw new UnsupportedOperationException(String.format(
						"Cannot rescale from resolution %s to resolution %s", toString(
							previousResolution), toString(resolution)));
				}
			}
			previousResolution= resolution;
		}
	
	}*/

	private List<cz.it4i.fiji.datastore.register_service.ResolutionLevel>
		getNonIdentityResolutions(Dataset dataset, List<int[]> resolutions)
	{
		return resolutions.stream().map(res -> getNonIdentityResolution(dataset,
			res)).collect(Collectors.toList());
	}

	private cz.it4i.fiji.datastore.register_service.ResolutionLevel
		getNonIdentityResolution(Dataset dataset, int[] res)
	{
		if (Arrays.equals(IDENTITY_RESOLUTION, res)) {
			throw new IllegalArgumentException("Resolution [1,1,1] cannot be used.");
		}
		cz.it4i.fiji.datastore.register_service.ResolutionLevel result = dataset
			.getResolutionLevel(res);
		if (result == null) {
			throw new NotFoundException("Resolution " + Arrays.toString(res) +
				" not found in dataset " + dataset.getUuid());
		}
		return result;
	}

	private N5Description convert(DatasetDTO dataset) {
// @formatter:off
		N5DescriptionBuilder result = N5Description.builder()
				.voxelType(DataType.valueOf(dataset.getVoxelType().toUpperCase()))
				.dimensions(dataset.getDimensions())
				.voxelDimensions(new FinalVoxelDimensions(dataset.getVoxelUnit(), dataset.getVoxelResolution()))
				.channels(dataset.getChannels())
				.angles(dataset.getAngles())
				.transforms(createTransfoms(dataset.getAngles(), dataset.getTransformations()))
				.compression(createCompression(dataset.getCompression()))
				.exportMipmapInfo(MipmapInfoAssembler.createExportMipmapInfo(dataset))
				.viewRegistrations(dataset.getViewRegistrations());
//	@formatter:on
		if (dataset.getTimepointIds() != null && !dataset.getTimepointIds()
			.isEmpty())
		{
			result.timepointIds(dataset.getTimepointIds());
		}
		else {
			result.timepoints(dataset.getTimepoints());
		}
		return result.build();
	}

	private AffineTransform3D[] createTransfoms(int angles,
		double[][] transformations)
	{
		AffineTransform3D[] result = new AffineTransform3D[angles];
		for (int i = 0; i < angles; i++) {
			result[i] = new AffineTransform3D();
			if (transformations != null && i < transformations.length &&
				transformations[i] != null)
			{
				result[i].set(transformations[i]);
			}
		}
		return result;
	}

	private @NonNull Compression createCompression(String compression) {
		return getCompressionMapping().getOrDefault(compression.toUpperCase(),
			new RawCompression());
	}

	private Dataset getDataset(String uuid) {
		Dataset dataset = datasetDAO.findByUUID(uuid);
		return dataset;
	}

	private Map<String, Compression> getCompressionMapping() {
		if (name2compression == null) {
			name2compression = new HashMap<>();
			name2compression.put("BZIP2", new Bzip2Compression());
			name2compression.put("GZIP", new GzipCompression());
			name2compression.put("LZ4", new Lz4Compression());
			name2compression.put("RAW", new RawCompression());
			name2compression.put("XZ", new XzCompression());
		}
		return name2compression;
	}

	private void illegalVersionAndModeCombination(String version,
		OperationMode mode)
	{
		throw new IllegalArgumentException("" + mode +
			" mode is not valid for version " + version);
	}

	private int resolveVersion(Dataset dataset, String version,
		OperationMode mode) throws IOException
	{
		DatasetHandler dfs = configuration.getDatasetHandler(dataset.getUuid());
		switch (version) {
			case "latest":
				return dfs.getLatestVersion();
			case "new":
				if (mode == OperationMode.READ) {
					illegalVersionAndModeCombination(version, mode);
				}
				return dfs.createNewVersion();
			case "mixedLatest":
				if (mode == OperationMode.WRITE) {
					illegalVersionAndModeCombination(version, mode);
				}
				return dfs.getLatestVersion();
			default:
				try {
					int result = Integer.parseInt(version);
					if (!dfs.getAllVersions().contains(result)) {
						DatasetServerImpl.versionNotFound(result);
					}
					return result;
				}
				catch (NumberFormatException e) {
					throw new IllegalArgumentException("version (" + version +
						") is not valid.");
				}

		}

	}

	/*private String toString(int[] resolution) {
		return "[" + IntStream.of(resolution).mapToObj(i -> Integer.toString(i))
			.collect(Collectors.joining(",")) +
			"]";
	}*/

	private static ExportMipmapInfo createExportMipmapInfo(
		List<cz.it4i.fiji.datastore.register_service.ResolutionLevel> resolutionLevels)
	{
		int[][] resolutions = new int[resolutionLevels.size()][];
		int[][] subdivisions = new int[resolutionLevels.size()][];
		int i = 0;
		for (cz.it4i.fiji.datastore.register_service.ResolutionLevel rl : resolutionLevels) {
			resolutions[i] = rl.getResolutions();
			subdivisions[i] = rl.getBlockDimensions();
			i++;
		}
		return new ExportMipmapInfo(resolutions, subdivisions);
	}

	private static int numThreads() {
		return Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
	}

	private static LoopbackHeuristic createLoopbackHeuristic(boolean isVirtual) {

		final long planeSizeInBytes = -1;
		final long maxMemory = Runtime.getRuntime().maxMemory();
		return new LoopbackHeuristic() {

			@Override
			public boolean decide(final RandomAccessibleInterval<?> originalImg,
				final int[] factorsToOriginalImg, final int previousLevel,
				final int[] factorsToPreviousLevel, final int[] chunkSize)
			{
				if (previousLevel < 0) return false;

				if (Intervals.numElements(factorsToOriginalImg) / Intervals.numElements(
					factorsToPreviousLevel) >= 8) return true;

				if (isVirtual) {
					final long requiredCacheSize = planeSizeInBytes *
						factorsToOriginalImg[2] * chunkSize[2];
					if (requiredCacheSize > maxMemory / 4) return true;
				}

				return false;
			}
		};

	}

	private static AfterEachPlane createAfterEachPlane(final boolean isVirtual,
		Runnable clearCache)
	{
		return new AfterEachPlane() {

			@SuppressWarnings("unused")
			@Override
			public void afterEachPlane(final boolean usedLoopBack) {
				if (!usedLoopBack && isVirtual) {
					final long free = Runtime.getRuntime().freeMemory();
					final long total = Runtime.getRuntime().totalMemory();
					final long max = Runtime.getRuntime().maxMemory();
					final long actuallyFree = max - total + free;

					if (actuallyFree < max / 2) clearCache.run();
				}
			}

		};
	}

}
