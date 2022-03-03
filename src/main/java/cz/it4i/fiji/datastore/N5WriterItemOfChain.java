/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2022 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import java.io.IOException;

import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Writer;

import lombok.AllArgsConstructor;
import lombok.experimental.Delegate;

@AllArgsConstructor
public class N5WriterItemOfChain implements N5Writer {

	@Delegate(excludes = { ExcludeReadWriteMethod.class })
	private final N5Writer innerWriter;

	private final N5WriterItemOfChain next;

	@Override
	public DataBlock<?> readBlock(String pathName,
		DatasetAttributes datasetAttributes, long[] gridPosition)
		throws IOException
	{
		DataBlock<?> result = innerWriter.readBlock(pathName, datasetAttributes,
			gridPosition);
		if (result != null) {
			return result;
		}

		if (next != null) {
			return next.readBlock(pathName, datasetAttributes, gridPosition);
		}
		return null;
	}

	@Override
	public <T> void writeBlock(String pathName,
		DatasetAttributes datasetAttributes, DataBlock<T> dataBlock)
		throws IOException
	{
		throw new UnsupportedOperationException(
			"Writting mode is not supported for version mixedLatest");
	}

	private interface ExcludeReadWriteMethod {

		public DataBlock<?> readBlock(final String pathName,
			final DatasetAttributes datasetAttributes, final long[] gridPosition)
			throws IOException;

		public <T> void writeBlock(final String pathName,
			final DatasetAttributes datasetAttributes, final DataBlock<T> dataBlock)
			throws IOException;

	}

}