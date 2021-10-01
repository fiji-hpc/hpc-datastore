/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.base;

import net.imglib2.FinalDimensions;

import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.Illumination;
import mpicbg.spim.data.sequence.ViewSetup;
import mpicbg.spim.data.sequence.VoxelDimensions;

public final class Factories {

	private Factories() {}

	public static ViewSetup constructViewSetup(int setupId, long[] dimensions,
		VoxelDimensions voxelDimensions, Channel channelObj, Angle angleObj,
		Illumination illumination)
	{
		return new ViewSetup(setupId, "setup" + setupId, new FinalDimensions(
			dimensions), voxelDimensions, channelObj, angleObj, illumination);
	}
}
