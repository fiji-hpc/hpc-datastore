/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.register_service;

import java.util.Arrays;
import java.util.List;

import bdv.export.ExportMipmapInfo;
import cz.it4i.fiji.datastore.register_service.DatasetDTO.ResolutionLevel;
import lombok.NonNull;

public class MipmapInfoAssembler {

	public static @NonNull ExportMipmapInfo createExportMipmapInfo(
		DatasetDTO datasetDTO)
	{
		return createExportMipmapInfo(datasetDTO.getResolutionLevels());
	}

	public static ExportMipmapInfo createExportMipmapInfo(
		ResolutionLevel[] resolutionLevels)
	{
		int[][] resolutions = new int[resolutionLevels.length][];
		int[][] subdivisions = new int[resolutionLevels.length][];
		for (int i = 0; i < resolutionLevels.length; i++) {
			resolutions[i] = Arrays.copyOf(resolutionLevels[i].resolutions,
				resolutionLevels[i].resolutions.length);
			subdivisions[i] = Arrays.copyOf(resolutionLevels[i].blockDimensions,
				resolutionLevels[i].blockDimensions.length);
		}
		return new ExportMipmapInfo(resolutions, subdivisions);
	}

	public static ExportMipmapInfo createExportMipmapInfo(
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
}
