/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2022 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.register_service;

import static bdv.img.n5.BdvN5Format.getPathName;
import static net.imglib2.cache.img.ReadOnlyCachedCellImgOptions.options;

import java.io.IOException;
import java.util.function.Function;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Cast;

import org.janelia.saalfeldlab.n5.ByteArrayDataBlock;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.DoubleArrayDataBlock;
import org.janelia.saalfeldlab.n5.FloatArrayDataBlock;
import org.janelia.saalfeldlab.n5.IntArrayDataBlock;
import org.janelia.saalfeldlab.n5.LongArrayDataBlock;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.ShortArrayDataBlock;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

import bdv.export.ExportScalePyramid;
import bdv.img.cache.SimpleCacheArrayLoader;
import bdv.img.n5.N5ImageLoader;

class N5Dataset {

	final String pathName;
	final DatasetAttributes attributes;

	public N5Dataset(final String pathName, final DatasetAttributes attributes) {
		this.pathName = pathName;
		this.attributes = attributes;
	}
}

class N5DatasetIO<T extends RealType<T> & NativeType<T>> implements
	ExportScalePyramid.DatasetIO<N5Dataset, T>
{

	private final N5Writer n5;
	private final Compression compression;
	private final int setupId;
	private final int timepointId;
	private final DataType dataType;
	private final T type;
	private final Function<ExportScalePyramid.Block<T>, DataBlock<?>> getDataBlock;

	public N5DatasetIO(final N5Writer n5, final Compression compression,
		final int setupId, final int timepointId, final T type)
	{
		this.n5 = n5;
		this.compression = compression;
		this.setupId = setupId;
		this.timepointId = timepointId;
		this.dataType = N5Utils.dataType(type);
		this.type = type;

		switch (dataType) {
			case UINT8:
				getDataBlock = b -> new ByteArrayDataBlock(b.getSize(), b
					.getGridPosition(), Cast.unchecked(b.getData().getStorageArray()));
				break;
			case UINT16:
				getDataBlock = b -> new ShortArrayDataBlock(b.getSize(), b
					.getGridPosition(), Cast.unchecked(b.getData().getStorageArray()));
				break;
			case UINT32:
				getDataBlock = b -> new IntArrayDataBlock(b.getSize(), b
					.getGridPosition(), Cast.unchecked(b.getData().getStorageArray()));
				break;
			case UINT64:
				getDataBlock = b -> new LongArrayDataBlock(b.getSize(), b
					.getGridPosition(), Cast.unchecked(b.getData().getStorageArray()));
				break;
			case INT8:
				getDataBlock = b -> new ByteArrayDataBlock(b.getSize(), b
					.getGridPosition(), Cast.unchecked(b.getData().getStorageArray()));
				break;
			case INT16:
				getDataBlock = b -> new ShortArrayDataBlock(b.getSize(), b
					.getGridPosition(), Cast.unchecked(b.getData().getStorageArray()));
				break;
			case INT32:
				getDataBlock = b -> new IntArrayDataBlock(b.getSize(), b
					.getGridPosition(), Cast.unchecked(b.getData().getStorageArray()));
				break;
			case INT64:
				getDataBlock = b -> new LongArrayDataBlock(b.getSize(), b
					.getGridPosition(), Cast.unchecked(b.getData().getStorageArray()));
				break;
			case FLOAT32:
				getDataBlock = b -> new FloatArrayDataBlock(b.getSize(), b
					.getGridPosition(), Cast.unchecked(b.getData().getStorageArray()));
				break;
			case FLOAT64:
				getDataBlock = b -> new DoubleArrayDataBlock(b.getSize(), b
					.getGridPosition(), Cast.unchecked(b.getData().getStorageArray()));
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	public N5Dataset createDataset(final int level, final long[] dimensions,
		final int[] blockSize) throws IOException
	{
		final String pathName = getPathName(setupId, timepointId, level);
		n5.createDataset(pathName, dimensions, blockSize, dataType, compression);
		final DatasetAttributes attributes = n5.getDatasetAttributes(pathName);
		return new N5Dataset(pathName, attributes);
	}

	@Override
	public void writeBlock(final N5Dataset dataset,
		final ExportScalePyramid.Block<T> dataBlock) throws IOException
	{
		n5.writeBlock(dataset.pathName, dataset.attributes, getDataBlock.apply(
			dataBlock));
	}

	@Override
	public void flush(final N5Dataset dataset) {}

	@Override
	public RandomAccessibleInterval<T> getImage(final int level)
		throws IOException
	{
		final String pathName = getPathName(setupId, timepointId, level);
		final DatasetAttributes attributes = n5.getDatasetAttributes(pathName);
		final long[] dimensions = attributes.getDimensions();
		final int[] cellDimensions = attributes.getBlockSize();
		final CellGrid grid = new CellGrid(dimensions, cellDimensions);
		final SimpleCacheArrayLoader<?> cacheArrayLoader = N5ImageLoader
			.createCacheArrayLoader(n5, pathName);
		return new ReadOnlyCachedCellImgFactory().createWithCacheLoader(dimensions,
			type, key -> {
				final int n = grid.numDimensions();
				final long[] cellMin = new long[n];
				final int[] cellDims = new int[n];
				final long[] cellGridPosition = new long[n];
				grid.getCellDimensions(key, cellMin, cellDims);
				grid.getCellGridPositionFlat(key, cellGridPosition);
				return new Cell<>(cellDims, cellMin, cacheArrayLoader.loadArray(
					cellGridPosition));
			}, options().cellDimensions(cellDimensions));
	}
}
