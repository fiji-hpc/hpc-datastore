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
package cz.it4i.fiji.datastore;

import static bdv.img.n5.BdvN5Format.DATA_TYPE_KEY;
import static bdv.img.n5.BdvN5Format.DOWNSAMPLING_FACTORS_KEY;
import static bdv.img.n5.BdvN5Format.getPathName;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.cache.queue.BlockingFetchQueues;
import net.imglib2.cache.queue.FetcherThreads;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.LoadingStrategy;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileByteArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileDoubleArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileFloatArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileIntArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileLongArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.cell.CellImg;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.VolatileByteType;
import net.imglib2.type.volatiles.VolatileDoubleType;
import net.imglib2.type.volatiles.VolatileFloatType;
import net.imglib2.type.volatiles.VolatileIntType;
import net.imglib2.type.volatiles.VolatileLongType;
import net.imglib2.type.volatiles.VolatileShortType;
import net.imglib2.type.volatiles.VolatileUnsignedByteType;
import net.imglib2.type.volatiles.VolatileUnsignedIntType;
import net.imglib2.type.volatiles.VolatileUnsignedLongType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;
import net.imglib2.util.Cast;
import net.imglib2.view.Views;

import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;

import bdv.AbstractViewerSetupImgLoader;
import bdv.ViewerImgLoader;
import bdv.cache.CacheControl;
import bdv.img.cache.SimpleCacheArrayLoader;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.util.ConstantRandomAccessible;
import bdv.util.MipmapTransforms;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import mpicbg.spim.data.sequence.MultiResolutionImgLoader;
import mpicbg.spim.data.sequence.MultiResolutionSetupImgLoader;
import mpicbg.spim.data.sequence.VoxelDimensions;

public class N5ImageLoader implements ViewerImgLoader,
	MultiResolutionImgLoader
{



	/**
	 * Maps setup id to {@link SetupImgLoader}.
	 */
	private final Map<Integer, SetupImgLoader> setupImgLoaders = new HashMap<>();

	private Supplier<N5Reader> n5Supplier;

	private volatile boolean isOpen = false;
	private FetcherThreads fetchers;
	private VolatileGlobalCellCache cache;
	private N5Reader n5;

	private List<? extends BasicViewSetup> setups;

	public N5ImageLoader(final Supplier<N5Reader> n5Supplier,
		final List<? extends BasicViewSetup> setups)
	{
		this.n5Supplier = n5Supplier;
		this.setups = setups;
	}

	/**
	 * Clear the cache. Images that were obtained from this loader before
	 * {@link #close()} will stop working. Requesting images after
	 * {@link #close()} will cause the n5 to be reopened (with a new cache).
	 */
	public void close() {
		if (isOpen) {
			synchronized (this) {
				if (!isOpen) return;
				fetchers.shutdown();
				cache.clearCache();
				isOpen = false;
			}
		}
	}

	@Override
	public SetupImgLoader getSetupImgLoader(final int setupId) {
		open();
		return setupImgLoaders.get(setupId);
	}

	@Override
	public CacheControl getCacheControl() {
		open();
		return cache;
	}

	private void open() {
		if (!isOpen) {
			synchronized (this) {
				if (isOpen) return;
	
				try {
	
					this.n5 = n5Supplier.get();
					int maxNumLevels = 0;
					for (final BasicViewSetup setup : setups) {
						final int setupId = setup.getId();
						final SetupImgLoader setupImgLoader = createSetupImgLoader(setupId);
						setupImgLoaders.put(setupId, setupImgLoader);
						maxNumLevels = Math.max(maxNumLevels, setupImgLoader
							.numMipmapLevels());
					}
	
					final int numFetcherThreads = Math.max(1, Runtime.getRuntime()
						.availableProcessors());
					final BlockingFetchQueues<Callable<?>> queue =
						new BlockingFetchQueues<>(maxNumLevels, numFetcherThreads);
					fetchers = new FetcherThreads(queue, numFetcherThreads);
					cache = new VolatileGlobalCellCache(queue);
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
	
				isOpen = true;
			}
		}
	}

	private <T extends NativeType<T>, V extends Volatile<T> & NativeType<V>>
		SetupImgLoader<T, V> createSetupImgLoader(final int setupId)
			throws IOException
	{
		final String pathName = getPathName(setupId);
		final DataType dataType = n5.getAttribute(pathName, DATA_TYPE_KEY,
			DataType.class);
		switch (dataType) {
			case UINT8:
				return Cast.unchecked(new SetupImgLoader<>(setupId,
					new UnsignedByteType(), new VolatileUnsignedByteType()));
			case UINT16:
				return Cast.unchecked(new SetupImgLoader<>(setupId,
					new UnsignedShortType(), new VolatileUnsignedShortType()));
			case UINT32:
				return Cast.unchecked(new SetupImgLoader<>(setupId,
					new UnsignedIntType(), new VolatileUnsignedIntType()));
			case UINT64:
				return Cast.unchecked(new SetupImgLoader<>(setupId,
					new UnsignedLongType(), new VolatileUnsignedLongType()));
			case INT8:
				return Cast.unchecked(new SetupImgLoader<>(setupId, new ByteType(),
					new VolatileByteType()));
			case INT16:
				return Cast.unchecked(new SetupImgLoader<>(setupId, new ShortType(),
					new VolatileShortType()));
			case INT32:
				return Cast.unchecked(new SetupImgLoader<>(setupId, new IntType(),
					new VolatileIntType()));
			case INT64:
				return Cast.unchecked(new SetupImgLoader<>(setupId, new LongType(),
					new VolatileLongType()));
			case FLOAT32:
				return Cast.unchecked(new SetupImgLoader<>(setupId, new FloatType(),
					new VolatileFloatType()));
			case FLOAT64:
				return Cast.unchecked(new SetupImgLoader<>(setupId, new DoubleType(),
					new VolatileDoubleType()));
		}
		return null;
	}

	public class SetupImgLoader<T extends NativeType<T>, V extends Volatile<T> & NativeType<V>>
		extends AbstractViewerSetupImgLoader<T, V> implements
		MultiResolutionSetupImgLoader<T>
	{

		private final int setupId;

		private final double[][] mipmapResolutions;

		private final AffineTransform3D[] mipmapTransforms;

		public SetupImgLoader(final int setupId, final T type, final V volatileType)
			throws IOException
		{
			super(type, volatileType);
			this.setupId = setupId;
			final String pathName = getPathName(setupId);
			mipmapResolutions = n5.getAttribute(pathName, DOWNSAMPLING_FACTORS_KEY,
				double[][].class);
			mipmapTransforms = new AffineTransform3D[mipmapResolutions.length];
			for (int level = 0; level < mipmapResolutions.length; level++)
				mipmapTransforms[level] = MipmapTransforms.getMipmapTransformDefault(
					mipmapResolutions[level]);
		}

		@Override
		public RandomAccessibleInterval<V> getVolatileImage(final int timepointId,
			final int level, final ImgLoaderHint... hints)
		{
			return prepareCachedImage(timepointId, level, LoadingStrategy.BUDGETED,
				volatileType);
		}

		@Override
		public RandomAccessibleInterval<T> getImage(final int timepointId,
			final int level, final ImgLoaderHint... hints)
		{
			return prepareCachedImage(timepointId, level, LoadingStrategy.BLOCKING,
				type);
		}

		@Override
		public Dimensions getImageSize(final int timepointId, final int level) {
			try {
				final String pathName = getPathName(setupId, timepointId, level);
				final DatasetAttributes attributes = n5.getDatasetAttributes(pathName);
				return new FinalDimensions(attributes.getDimensions());
			}
			catch (Exception e) {
				return null;
			}
		}

		@Override
		public double[][] getMipmapResolutions() {
			return mipmapResolutions;
		}

		@Override
		public AffineTransform3D[] getMipmapTransforms() {
			return mipmapTransforms;
		}

		@Override
		public int numMipmapLevels() {
			return mipmapResolutions.length;
		}

		@Override
		public VoxelDimensions getVoxelSize(final int timepointId) {
			return null;
		}

		/**
		 * Create a {@link CellImg} backed by the cache.
		 */
		private <T2 extends NativeType<T2>> RandomAccessibleInterval<T2>
			prepareCachedImage(final int timepointId, final int level,
				final LoadingStrategy loadingStrategy, final T2 aType)
		{
			try {
				final String pathName = getPathName(setupId, timepointId, level);
				final DatasetAttributes attributes = n5.getDatasetAttributes(pathName);
				final long[] dimensions = attributes.getDimensions();
				final int[] cellDimensions = attributes.getBlockSize();
				final CellGrid grid = new CellGrid(dimensions, cellDimensions);

				final int priority = numMipmapLevels() - 1 - level;
				final CacheHints cacheHints = new CacheHints(loadingStrategy, priority,
					false);

				final SimpleCacheArrayLoader<?> loader = createCacheArrayLoader(n5,
					pathName);
				return cache.createImg(grid, timepointId, setupId, level, cacheHints,
					loader, aType);
			}
			catch (IOException e) {
				System.err.println(String.format(
					"image data for timepoint %d setup %d level %d could not be found.",
					timepointId, setupId, level));
				return Views.interval(new ConstantRandomAccessible<>(aType
					.createVariable(), 3), new FinalInterval(1, 1, 1));
			}
		}
	}

	private static class N5CacheArrayLoader<A> implements
		SimpleCacheArrayLoader<A>
	{

		private final N5Reader n5;
		private final String pathName;
		private final DatasetAttributes attributes;
		private final Function<Object, A> createArray;

		N5CacheArrayLoader(final N5Reader n5, final String pathName,
			final DatasetAttributes attributes,
			final Function<Object, A> createArray)
		{
			this.n5 = n5;
			this.pathName = pathName;
			this.attributes = attributes;
			this.createArray = createArray;
		}

		@Override
		public A loadArray(final long[] gridPosition) throws IOException {
			DataBlock<?> block = n5.readBlock(pathName, attributes, gridPosition);
			if (block == null) {
				block = attributes.getDataType().createDataBlock(attributes
					.getBlockSize(), gridPosition);
			}
			return createArray.apply(block != null ? block.getData() : null);
		}
	}

	public static SimpleCacheArrayLoader<?> createCacheArrayLoader(
		final N5Reader n5, final String pathName) throws IOException
	{
		final DatasetAttributes attributes = n5.getDatasetAttributes(pathName);
		switch (attributes.getDataType()) {
			case UINT8:
			case INT8:
				return new N5CacheArrayLoader<>(n5, pathName, attributes,
					data -> new VolatileByteArray(Cast.unchecked(data), data != null));
			case UINT16:
			case INT16:
				return new N5CacheArrayLoader<>(n5, pathName, attributes,
					data -> new VolatileShortArray(Cast.unchecked(data), data != null));
			case UINT32:
			case INT32:
				return new N5CacheArrayLoader<>(n5, pathName, attributes,
					data -> new VolatileIntArray(Cast.unchecked(data), data != null));
			case UINT64:
			case INT64:
				return new N5CacheArrayLoader<>(n5, pathName, attributes,
					data -> new VolatileLongArray(Cast.unchecked(data), data != null));
			case FLOAT32:
				return new N5CacheArrayLoader<>(n5, pathName, attributes,
					data -> new VolatileFloatArray(Cast.unchecked(data), data != null));
			case FLOAT64:
				return new N5CacheArrayLoader<>(n5, pathName, attributes,
					data -> new VolatileDoubleArray(Cast.unchecked(data), data != null));
			default:
				throw new IllegalArgumentException();
		}
	}
}
