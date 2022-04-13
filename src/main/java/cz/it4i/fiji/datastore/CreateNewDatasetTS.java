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
import static cz.it4i.fiji.datastore.DatasetFilesystemHandler.INITIAL_VERSION;
import static cz.it4i.fiji.datastore.base.Factories.constructViewSetup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.N5Writer;

import bdv.export.ExportMipmapInfo;
import bdv.img.hdf5.MipmapInfo;
import bdv.img.hdf5.Util;
import bdv.img.n5.N5ImageLoader;
import cz.it4i.fiji.datastore.core.ViewTransformDTO;
import lombok.Builder;
import lombok.NonNull;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.registration.ViewTransform;
import mpicbg.spim.data.registration.ViewTransformAffine;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.Illumination;
import mpicbg.spim.data.sequence.SequenceDescription;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.ViewSetup;
import mpicbg.spim.data.sequence.VoxelDimensions;


public class CreateNewDatasetTS {

	public static void createN5Structure(N5Writer n5, DataType voxelType,
		long[] dimensions, Compression compression, MipmapInfo mipmapInfo,
		SequenceDescription sequenceDescription, final List<Integer> setupIds,
		final List<Integer> timepointIds) throws IOException
	{
		try (AttributeSetter attrSet = new AttributeSetter(n5)) {
			final int[][] resolutions = Util.castToInts(mipmapInfo.getResolutions());
			final int[][] subdivisions = mipmapInfo.getSubdivisions();
			int n = 3;
			// write Mipmap descriptions
			for (final int setupId : setupIds) {
				final String pathName = getPathName(setupId);
				final int[][] downsamplingFactors = resolutions;
				n5.createGroup(pathName);
				attrSet.setAttribute(pathName, DOWNSAMPLING_FACTORS_KEY,
					downsamplingFactors);
				attrSet.setAttribute(pathName, DATA_TYPE_KEY, voxelType);
			}
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
					attrSet.setAttribute(pathName, MULTI_SCALE_KEY, true);
					final VoxelDimensions voxelSize = sequenceDescription.getViewSetups()
						.get(setupId).getVoxelSize();
					if (voxelSize != null) {
						final double[] resolution = new double[voxelSize.numDimensions()];
						voxelSize.dimensions(resolution);
						attrSet.setAttribute(pathName, RESOLUTION_KEY, resolution);
					}
					for (int l = 0; l < resolutions.length; ++l)
						attrSet.setAttribute(getPathName(setupId, timepointId, l),
							DOWNSAMPLING_FACTORS_KEY, resolutions[l]);
				}
			}
		}
	}

	private static final String MULTI_SCALE_KEY = "multiScale";
	private static final String RESOLUTION_KEY = "resolution";

	public void run(DatasetHandler handler, N5Description dsc) throws IOException,
		SpimDataException
	{
		SpimData data = createNew(handler, dsc);
		handler.saveSpimData(data);
	}

	private SpimData createNew(DatasetHandler datasetHandler,
		N5Description description)
		throws IOException
	{
		SpimData result = new SPIMDataProducer(description).spimData;
		createN5Structure(datasetHandler.getWriter(INITIAL_VERSION),
			description.voxelType, description.dimensions, description.compression,
			description.exportMipmapInfo, result.getSequenceDescription());
		return result;
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
		
		private final AffineTransform3D[] transforms;

		@NonNull
		private final Compression compression;

		@NonNull
		private final ExportMipmapInfo exportMipmapInfo;

		private final Collection<cz.it4i.fiji.datastore.core.ViewRegistrationDTO> viewRegistrations;


	}

	private static class SPIMDataProducer {

		final Collection<TimePoint> timepointsCol;
		final Collection<ViewSetup> viewSetups;
		final SpimData spimData;
		final Map<Integer, Map<Integer, ViewSetup>> perAngleAndChannelViewSetup =
			new HashMap<>();

		SPIMDataProducer(N5Description description) {
			timepointsCol = IntStream.range(0, description.timepoints)
				.<TimePoint> mapToObj(
				TimePoint::new).collect(Collectors.toList());

			viewSetups = generateViewSetups(
				description.dimensions, description.voxelDimensions,
				description.channels, description.angles);

			final SequenceDescription tempSequenceDescription =
				new SequenceDescription(new TimePoints(timepointsCol), viewSetups,
					null);
			
			final SequenceDescription sequenceDescription = new SequenceDescription(
				new TimePoints(timepointsCol), viewSetups, new N5ImageLoader(new File(
					""), tempSequenceDescription));
			
			spimData = new SpimData(new File(""), sequenceDescription,
				new ViewRegistrations(generateViewRegistrations(description)));
		}

		private Collection<ViewRegistration> generateViewRegistrations(
			N5Description description)
		{
			if (description.viewRegistrations != null) {
				return description.viewRegistrations.stream().map(vr -> convert(vr))
					.collect(Collectors.toList());
			}
			return generateViewRegistrations(description.transforms);
		}

		private Collection<ViewRegistration> generateViewRegistrations(
			AffineTransform3D[] transforms)
		{
			Collection<ViewRegistration> result = new LinkedList<>();
			for (ViewSetup viewSetup : viewSetups) {
				for (TimePoint timePoint : timepointsCol) {
					result.add(new ViewRegistration(timePoint.getId(), viewSetup.getId(),
						transforms[viewSetup.getAngle().getId()]));
				}
			}
			return result;
		}

		private Collection<ViewSetup> generateViewSetups(long[] dimensions,
			VoxelDimensions voxelDimensions, int channels, int angles)
		{
			Collection<ViewSetup> result = new LinkedList<>();
			Illumination illumination = new Illumination(0);
			int setupId = 0;
			for (int channel = 0; channel < channels; channel++) {
				for (int angle = 0; angle < angles; angle++) {
					Angle angleObj = new Angle(angle);
					Channel channelObj = new Channel(channel);
					ViewSetup vs = constructViewSetup(setupId, dimensions,
						voxelDimensions, channelObj, angleObj, illumination);
					result.add(vs);
					putViewSetup(angle, channel, vs);
					setupId++;
				}
			}
			return result;
		}

		private void putViewSetup(int angle, int channel, ViewSetup vs) {
			perAngleAndChannelViewSetup.computeIfAbsent(angle, $ -> new HashMap<>())
				.put(channel, vs);
		}

		private ViewRegistration convert(
			cz.it4i.fiji.datastore.core.ViewRegistrationDTO vr)
		{
			ArrayList<ViewTransform> transforms = vr.getTransformations().stream()
				.map(tr -> convert(tr)).collect(Collectors.toCollection(
					ArrayList::new));
			ViewSetup vs = perAngleAndChannelViewSetup.getOrDefault(vr.getAngle(),
				Collections.emptyMap()).get(vr.getChannel());

			return new ViewRegistration(vr.getTime(), vs.getId(), transforms);
		}

		private static ViewTransform convert(ViewTransformDTO tr) {
			AffineTransform3D affineTransform3D = new AffineTransform3D();
			affineTransform3D.set(tr.getRowPackedMatrix());
			return new ViewTransformAffine(tr.getName(), affineTransform3D);
		}

	}

	private static void createN5Structure(N5Writer n5, DataType voxelType,
		long[] dimensions, Compression compression, MipmapInfo mipmapInfo,
		SequenceDescription sequenceDescription) throws IOException
	{
		final List<Integer> setupIds = sequenceDescription.getViewSetupsOrdered()
			.stream().map(BasicViewSetup::getId).collect(Collectors.toList());
		final List<Integer> timepointIds = sequenceDescription.getTimePoints()
			.getTimePointsOrdered().stream().map(TimePoint::getId).collect(Collectors
				.toList());
		createN5Structure(n5, voxelType, dimensions, compression, mipmapInfo,
			sequenceDescription, setupIds, timepointIds);
	}


}
