/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.register_service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Transient;

import cz.it4i.fiji.datastore.BaseEntity;
import cz.it4i.fiji.datastore.zarr.fileTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Entity(name = "Dataset")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Dataset extends BaseEntity {

 private static final long serialVersionUID = 4257740020931057972L;

	@Getter
	@Setter
	private String uuid;

	@Getter
	@ElementCollection(targetClass = ResolutionLevel.class)
	private Collection<ResolutionLevel> resolutionLevel;
	
	@ToString.Exclude
	@Transient
	private Map<String, ResolutionLevel> resolutionLevelIndex;


	@Getter
	@Setter
	@Transient
	private Collection<DatasetVersion> datasetVersion;
	
	@ToString.Exclude
	@Getter
	@Setter
	@Transient
	private Collection<ViewSetup> viewSetup;
	
//TODO  voxelType is a org.janelia.saalfeldlab.n5.DataType
	@Getter
	@Setter
	private String voxelType;

	
	@Getter
	@Setter
	private String metadata;
	
	@Getter
	@Setter
	private long[] dimensions;
	
	@Getter
	@Setter
	private int timepoints;
	
	@Getter
	@Setter
	private int channels;
	
	@Getter
	@Setter
	private int angles;
	
	@Getter
	@Setter
	private String voxelUnit;
	
	@Getter
	@Setter
	private double[] voxelResolution;
	
	@Getter
	@Setter
	private Resolution timepointResolution;
	
	@Getter
	@Setter
	private Resolution channelResolution;
	
	@Getter
	@Setter
	private Resolution angleResolution;
	
	@Getter
	@Setter
	private String compression;

	@Getter
	@Setter
	@Transient
	private String label;

	@Getter
	@Setter
	private String datasetType;

	public ResolutionLevel getResolutionLevel(int[] resolution) {
		return getResolutionLevelIndex().get(Arrays.toString(resolution));
	}

	public List<ResolutionLevel> getSortedResolutionLevels () {
		return getResolutionLevel().stream().sorted(Comparator.comparingInt(
			Dataset::area)).collect(Collectors.toList());
	}

	public int[] getBlockDimension(int[] resolution) {
		ResolutionLevel rl = getResolutionLevel(resolution);
		if (rl == null) {
			return null;
		}
		return rl.getBlockDimensions();
	}

	private Map<String, ResolutionLevel> getResolutionLevelIndex() {
		if (resolutionLevelIndex == null) {
			resolutionLevelIndex = resolutionLevel.stream().collect(Collectors
				.<ResolutionLevel, String, ResolutionLevel> toMap(rl -> Arrays.toString(
					rl.getResolutions()), rl -> rl));
		}
		return resolutionLevelIndex;
	}

	private static int area(ResolutionLevel rl) {
		if (rl.getResolutions().length == 0) {
			return 0;
		}
		int acc = 1;
		for (int i : rl.getResolutions()) {
			acc *= i;
		}
		return acc;
	}
}
