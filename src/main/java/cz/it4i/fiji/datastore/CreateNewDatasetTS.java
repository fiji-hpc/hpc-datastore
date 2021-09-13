/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import static bdv.img.n5.BdvN5Format.DATA_TYPE_KEY;
import static bdv.img.n5.BdvN5Format.DOWNSAMPLING_FACTORS_KEY;
import static bdv.img.n5.BdvN5Format.getPathName;
import static cz.it4i.fiji.datastore.DatasetPathRoutines.getXMLPath;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.imglib2.FinalDimensions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Writer;

import bdv.img.hdf5.MipmapInfo;
import bdv.img.hdf5.Util;
import bdv.img.n5.N5ImageLoader;
import lombok.Builder;
import lombok.NonNull;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.Illumination;
import mpicbg.spim.data.sequence.SequenceDescription;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.ViewSetup;
import mpicbg.spim.data.sequence.VoxelDimensions;


public class CreateNewDatasetTS {

	private static final String MULTI_SCALE_KEY = "multiScale";
	private static final String RESOLUTION_KEY = "resolution";

	public void run(Path path, N5Description dsc) throws IOException,
		SpimDataException
	{
		Path pathToXML = getXMLPath(path, DatasetFilesystemHandler.INITIAL_VERSION);

		Path pathToDir = DatasetPathRoutines.getDataPath(pathToXML);
		SpimData data = createNew(pathToDir, dsc.voxelType, dsc.dimensions,
			dsc.voxelDimensions, dsc.timepoints, dsc.channels, dsc.angles,
			dsc.compression, dsc.mipmapInfo);
		new XmlIoSpimData().save(data, pathToXML.toString());
	}

	private SpimData createNew(Path pathToDir, DataType voxelType,
		long[] dimensions, VoxelDimensions voxelDimensions, int timepoints,
		int channels, int angles, Compression compression, MipmapInfo mipmapInfo)
		throws IOException
	{

		final Collection<TimePoint> timepointsCol = IntStream.range(0, timepoints)
			.<TimePoint> mapToObj(TimePoint::new).collect(Collectors.toList());

		Collection<ViewSetup> viewSetups = generateViewSetups(dimensions,
			voxelDimensions, channels, angles);

		SequenceDescription sequenceDescription = new SequenceDescription(
			new TimePoints(timepointsCol), viewSetups, null);

		final List<Integer> setupIds = sequenceDescription.getViewSetupsOrdered()
			.stream().map(BasicViewSetup::getId).collect(Collectors.toList());
		N5Writer n5 = new N5FSWriter(pathToDir.toFile().getAbsolutePath());
		final int[][] resolutions = Util.castToInts(mipmapInfo.getResolutions());
		final int[][] subdivisions = mipmapInfo.getSubdivisions();
		int n = 3;
		// write Mipmap descriptions
		for (final int setupId : setupIds) {
			final String pathName = getPathName(setupId);
			final int[][] downsamplingFactors = resolutions;
			n5.createGroup(pathName);
			n5.setAttribute(pathName, DOWNSAMPLING_FACTORS_KEY, downsamplingFactors);
			n5.setAttribute(pathName, DATA_TYPE_KEY, voxelType);
		}
		final List<Integer> timepointIds = sequenceDescription.getTimePoints()
			.getTimePointsOrdered().stream().map(TimePoint::getId).collect(Collectors
				.toList());
		for (final int timepointId : timepointIds) {

			// assemble the viewsetups that are present in this timepoint
			for (final int setupId : setupIds) {
				final int numLevels = mipmapInfo.getNumLevels();
				for (int level = 0; level < numLevels; ++level) {
					long[] dims = new long[n];
					final int[] factor = resolutions[level];
					final long size = Intervals.numElements(factor);
					final boolean fullResolution = size == 1;
					System.arraycopy(dimensions, 0, dims, 0, n);
					if (!fullResolution) {
						for (int d = 0; d < n; ++d) {
							dims[d] = Math.max(dims[d] / factor[d], 1);
						}
					}
					n5.createDataset(getPathName(setupId, timepointId, level), dims,
						subdivisions[level], voxelType, compression);

				}

				// additional attributes for paintera compatibility
				final String pathName = getPathName(setupId, timepointId);
				n5.createGroup(pathName);
				n5.setAttribute(pathName, MULTI_SCALE_KEY, true);
				final VoxelDimensions voxelSize = sequenceDescription.getViewSetups()
					.get(setupId).getVoxelSize();
				if (voxelSize != null) {
					final double[] resolution = new double[voxelSize.numDimensions()];
					voxelSize.dimensions(resolution);
					n5.setAttribute(pathName, RESOLUTION_KEY, resolution);
				}
				for (int l = 0; l < resolutions.length; ++l)
					n5.setAttribute(getPathName(setupId, timepointId, l),
						DOWNSAMPLING_FACTORS_KEY, resolutions[l]);
			}
		}


		sequenceDescription = new SequenceDescription(new TimePoints(timepointsCol),
			viewSetups, new N5ImageLoader(pathToDir.toFile(), sequenceDescription));
		SpimData result = new SpimData(pathToDir.toFile(), sequenceDescription,
			new ViewRegistrations(generateViewRegistrations(timepointsCol,
				viewSetups, getTransform(mipmapInfo))));
		return result;
	}

	private Collection<ViewSetup> generateViewSetups(long[] dimensions,
		VoxelDimensions voxelDimensions, int channels, int angles)
	{
		Collection<ViewSetup> viewSetups = new LinkedList<>();
		Illumination illumination = new Illumination(0);
		int setupId = 0;
		for (int channel = 0; channel < channels; channel++) {
			for (int angle = 0; angle < angles; angle++) {
				Angle angleObj = new Angle(angle);
				Channel channelObj = new Channel(channel);
				ViewSetup vs = new ViewSetup(setupId, "setup" + setupId,
					new FinalDimensions(dimensions), voxelDimensions, channelObj,
					angleObj, illumination);
				viewSetups.add(vs);
				setupId++;
			}
		}
		return viewSetups;
	}

	private Collection<ViewRegistration> generateViewRegistrations(
		Collection<TimePoint> timepoints, Collection<ViewSetup> viewSetups,
		AffineTransform3D transform)
	{
		Collection<ViewRegistration> result = new LinkedList<>();
		for (ViewSetup viewSetup : viewSetups) {
			for (TimePoint timePoint : timepoints) {
				result.add(new ViewRegistration(timePoint.getId(), viewSetup.getId(),
					transform));
			}
		}
		return result;
	}

	static private AffineTransform3D getTransform(MipmapInfo mipmapInfo) {
		if (mipmapInfo.getTransforms() == null || mipmapInfo
			.getTransforms().length == 0)
		{
			return new AffineTransform3D();
		}
		return mipmapInfo.getTransforms()[0];
	}

	@Builder
	public static class N5Description {

		@NonNull
		private final DataType voxelType;

		@NonNull
		private final long[] dimensions;

		@NonNull
		private final VoxelDimensions voxelDimensions;

		@Builder.Default
		private final int timepoints = 1;

		@Builder.Default
		private final int channels = 1;

		@Builder.Default
		private final int angles = 1;

		@NonNull
		private final Compression compression;

		@NonNull
		private final MipmapInfo mipmapInfo;

	}


}
