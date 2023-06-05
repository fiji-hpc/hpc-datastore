/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.register_service;

import static bdv.img.n5.BdvN5Format.getPathName;
import static java.util.stream.Collectors.toList;

import com.google.common.collect.Streams;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;

import bdv.img.n5.BdvN5Format;
import cz.it4i.fiji.datastore.core.DatasetDTO;
import cz.it4i.fiji.datastore.register_service.Dataset.DatasetBuilder;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.sequence.SequenceDescription;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.VoxelDimensions;



public class DatasetAssembler {

	private static DatasetTypeConverter converter = new DatasetTypeConverter();
	
	public static Dataset createDomainObject(DatasetDTO dto) {
		// @formatter:off
		return Dataset
				.builder()
				.voxelType(dto.getVoxelType())
				.dimensions(dto.getDimensions())
				.voxelUnit(dto.getVoxelUnit())
				.voxelResolution(dto.getVoxelResolution())
				.timepoints(dto.getTimepoints()).timepointResolution(createDomainObject(dto.getTimepointResolution()))
				.channels(dto.getChannels()).channelResolution(createDomainObject(dto.getChannelResolution()))
				.angles(dto.getAngles()).angleResolution(createDomainObject(dto.getAngleResolution()))
				.compression(dto.getCompression())
				.datasetType( converter.convertToEntityAttribute( dto.getDatasetType() ) )
				.resolutionLevel(createDomainObject(dto.getResolutionLevels()))
				.label(dto.getLabel())
				.build();
	// @formatter:on
	}

	public static Dataset createDomainObject(String uuid,
		Collection<Integer> versions, N5Reader reader, SpimData spimData,
		String label)
		throws IOException
	{
		final SequenceDescription sequenceDescription = spimData
			.getSequenceDescription();

		DatasetBuilder result = Dataset.builder();
		final mpicbg.spim.data.sequence.ViewSetup viewSetup = sequenceDescription
			.getViewSetupsOrdered().stream().findFirst().get();
		final int setupId = viewSetup.getId();
		final int timepointId = sequenceDescription.getTimePoints()
			.getTimePointsOrdered().stream().findFirst().get().getId();
		final VoxelDimensions voxelDimensions = viewSetup.getVoxelSize();
		

		final String path2Timepoint = getPathName(setupId, timepointId);
		List<Integer> levelIDs = Arrays.stream(reader.list(path2Timepoint)).map(
			lev -> lev.substring(1)).map(Integer::valueOf).sorted().collect(toList());

		String voxelType = null;
		String compression = null;
		long[] dimensions = null;
		final Collection<ResolutionLevel> levels = new LinkedList<>();
		for (int levelId : levelIDs) {
			final String path2Level = getPathName(setupId, timepointId, levelId);
			final DatasetAttributes attributes = reader.getDatasetAttributes(
				path2Level);
			if (voxelType == null) {
				voxelType = attributes.getDataType().toString();
			}

			if (compression == null) {
				compression = attributes.getCompression().getType();
			}

			if (dimensions == null) {
				dimensions = attributes.getDimensions();
			}
			
			final int[] resolution = reader.getAttribute(path2Level,
				BdvN5Format.DOWNSAMPLING_FACTORS_KEY, int[].class);
			ResolutionLevel level = new ResolutionLevel(resolution, attributes
				.getBlockSize());
			levels.add(level);
		}

		final double[] voxelResolutions = new double[voxelDimensions
			.numDimensions()];
		voxelDimensions.dimensions(voxelResolutions);

		result.angles(sequenceDescription.getAllAnglesOrdered().size());
		result.channels(sequenceDescription.getAllChannelsOrdered().size());
		result.timepoints(sequenceDescription.getTimePoints().size());
		result.uuid(uuid);
		result.label(label);
		result.voxelType(voxelType);
		result.voxelUnit(voxelDimensions.unit());
		result.voxelResolution(voxelResolutions);
		result.compression(compression);
		result.dimensions(dimensions);
		result.resolutionLevel(levels);
		result.datasetVersion(versions.stream().map(DatasetVersion::new).collect(
			Collectors.toList()));
		return result.build();
	}

	private static Resolution createDomainObject(DatasetDTO.Resolution dto) {
		if (dto == null) {
			return null;
		}
		return new Resolution(dto.getValue(), dto.getUnit());
	}
	
	
	private static Collection<ResolutionLevel> createDomainObject(
		DatasetDTO.ResolutionLevel[] resolutionLevels)
	{

		Stream<ResolutionLevel> resLevels = Arrays.asList(resolutionLevels)
			.stream().map(dto -> new ResolutionLevel(dto.getResolutions(), dto
				.getBlockDimensions()));
		
		return Streams.zip(IntStream.iterate(1, i -> i + 1).mapToObj(i -> Integer
			.valueOf(i)), resLevels, (i, r) -> {
				r.setLevelId(i);
				return r;
			}).collect(Collectors.toList());
	}

	public static DatasetDTO createDatatransferObject(Dataset dataset,
		TimePoints timepoints)
	{
		
		// @formatter:off
		return DatasetDTO
				.builder()
				.uuid(dataset.getUuid())
				.voxelType(dataset.getVoxelType())
				.dimensions(dataset.getDimensions())
				.voxelUnit(dataset.getVoxelUnit())
				.voxelResolution(dataset.getVoxelResolution())
				.timepoints(dataset.getTimepoints())
				.timepointIds(timepoints.getTimePointsOrdered().stream().map(TimePoint::getId).sorted().collect(toList()))
				.timepointResolution(createDatatransferObject(dataset.getTimepointResolution()))
				.channels(dataset.getChannels()).channelResolution(createDatatransferObject(dataset.getChannelResolution()))
				.angles(dataset.getAngles()).angleResolution(createDatatransferObject(dataset.getAngleResolution()))
				.compression(dataset.getCompression())
				.datasetType( converter.convertToDatabaseColumn( dataset.getDatasetType() ) )
				.resolutionLevels(createDatatransferObject(dataset.getResolutionLevel()))
				.versions(dataset.getDatasetVersion().stream().map(DatasetVersion::getValue).collect(toList()))
				.label(dataset.getLabel())
				.build();
	// @formatter:on
	}

	public static
		cz.it4i.fiji.datastore.core.DatasetDTO.ResolutionLevel[]
		createDatatransferObject(
		Collection<ResolutionLevel> resolutionLevel)
	{

		cz.it4i.fiji.datastore.core.DatasetDTO.ResolutionLevel[] result =
			new cz.it4i.fiji.datastore.core.DatasetDTO.ResolutionLevel[resolutionLevel
				.size()];
		int i = 0;
		for (ResolutionLevel rl : resolutionLevel) {
			result[i++] =
				new cz.it4i.fiji.datastore.core.DatasetDTO.ResolutionLevel(
					rl.getResolutions(), rl.getBlockDimensions());
		}
		return result;
	}

	private static cz.it4i.fiji.datastore.core.DatasetDTO.Resolution
		createDatatransferObject(Resolution resolution)
	{
		if (resolution == null) {
			return null;
		}
		return new DatasetDTO.Resolution(resolution.getValue(), resolution
			.getUnit());
	}

}
