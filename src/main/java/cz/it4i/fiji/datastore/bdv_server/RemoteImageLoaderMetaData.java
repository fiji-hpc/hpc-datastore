/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.bdv_server;


/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2020 BigDataViewer developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.janelia.saalfeldlab.n5.DataType;

import bdv.img.hdf5.DimsAndExistence;
import bdv.img.hdf5.MipmapInfo;
import bdv.img.hdf5.ViewLevelId;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.TimePoint;

class RemoteImageLoaderMetaData
{
	/**
	 * The highest occurring timepoint id + 1. This is the maximum number of
	 * timepoints that could possibly exist.
	 */
	protected int maxNumTimepoints;

	/**
	 * The highest occurring setup id + 1. This is the maximum number of setups
	 * that could possibly exist.
	 */
	protected int maxNumSetups;

	/**
	 * The maximum number of mipmap levels occuring in all setups.
	 */
	protected int maxNumLevels;

	/**
	 * Description of available mipmap levels for each {@link BasicViewSetup}.
	 * Contains for each mipmap level, the subsampling factors and subdivision
	 * block sizes. The {@link HashMap} key is the setup id.
	 */
	protected final Map<Integer, MipmapInfo> perSetupMipmapInfo;

	/**
	 * Maps {@link ViewLevelId} (timepoint, setup, level) to
	 * {@link DimsAndExistence}. Every entry represents the existence and
	 * dimensions of one image.
	 */
	protected final Map<ViewLevelId, DimsAndExistence> dimsAndExistence;

	@SuppressWarnings("unused")
	private final DataType dataType;

	public RemoteImageLoaderMetaData(
		final AbstractSequenceDescription<?, ?, ?> sequenceDescription,
		Map<Integer, MipmapInfo> aPerSetupMipmapInfo,
		Function<ViewLevelId, DimsAndExistence> dims, DataType aDataType)
	{
		perSetupMipmapInfo = new HashMap<>(aPerSetupMipmapInfo);
		dimsAndExistence = new HashMap<>();
		dataType = aDataType;

		final List< TimePoint > timepoints = sequenceDescription.getTimePoints().getTimePointsOrdered();
		maxNumTimepoints = timepoints.get( timepoints.size() - 1 ).getId() + 1;

		final List< ? extends BasicViewSetup > setups = sequenceDescription.getViewSetupsOrdered();
		maxNumSetups = setups.get( setups.size() - 1 ).getId() + 1;

		maxNumLevels = 0;

		for ( final BasicViewSetup setup : setups )
		{
			final int setupId = setup.getId();
			final MipmapInfo info = perSetupMipmapInfo.get(setupId);

			final int numLevels = info.getNumLevels();
			if ( numLevels > maxNumLevels )
				maxNumLevels = numLevels;

			for ( final TimePoint timepoint : timepoints )
			{
				final int timepointId = timepoint.getId();
				for ( int level = 0; level < numLevels; ++level )
				{
					final ViewLevelId id = new ViewLevelId( timepointId, setupId, level );
					dimsAndExistence.put(id, dims.apply(id));
				}
			}
		}
	}

	/**
	 * Create an map from {@link ViewLevelId} (timepoint, setup, level) to
	 * int[]. Every entry is the dimensions in cells (instead of pixels) of one
	 * image.
	 */
	protected HashMap< ViewLevelId, int[] > createCellsDimensions()
	{
		final HashMap< ViewLevelId, int[] > cellsDimensions = new HashMap<>();
		for ( final Entry< ViewLevelId, DimsAndExistence > entry : dimsAndExistence.entrySet() )
		{
			final ViewLevelId id = entry.getKey();
			final long[] imageDimensions = entry.getValue().getDimensions();
			final int[] cellSize = perSetupMipmapInfo.get( id.getViewSetupId() ).getSubdivisions()[ id.getLevel() ];
			final int[] dims = new int[] {
					( int ) ( imageDimensions[ 0 ] + cellSize[ 0 ] - 1 ) / cellSize[ 0 ],
					( int ) ( imageDimensions[ 1 ] + cellSize[ 1 ] - 1 ) / cellSize[ 1 ],
					( int ) ( imageDimensions[ 2 ] + cellSize[ 2 ] - 1 ) / cellSize[ 2 ] };
			cellsDimensions.put( id, dims );
		}
		return cellsDimensions;
	}
}

