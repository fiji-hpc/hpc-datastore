/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.register_service;

import com.google.common.collect.Streams;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import cz.it4i.fiji.datastore.core.DatasetDTO;



public class DatasetAssembler {

	
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
				.resolutionLevel(createDomainObject(dto.getResolutionLevels()))
				.build();
	// @formatter:on
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

	public static DatasetDTO createDatatransferObject(Dataset dataset)
	{
		
		// @formatter:off
		return DatasetDTO
				.builder()
				.uuid(dataset.getUuid())
				.voxelType(dataset.getVoxelType())
				.dimensions(dataset.getDimensions())
				.voxelUnit(dataset.getVoxelUnit())
				.voxelResolution(dataset.getVoxelResolution())
				.timepoints(dataset.getTimepoints()).timepointResolution(createDatatransferObject(dataset.getTimepointResolution()))
				.channels(dataset.getChannels()).channelResolution(createDatatransferObject(dataset.getChannelResolution()))
				.angles(dataset.getAngles()).angleResolution(createDatatransferObject(dataset.getAngleResolution()))
				.compression(dataset.getCompression())
				.resolutionLevels(createDatatransferObject(dataset.getResolutionLevel()))
				.versions(dataset.getDatasetVersion().stream().map(DatasetVersion::getValue).collect(Collectors.toList()))
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
