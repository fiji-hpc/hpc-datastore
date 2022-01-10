/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2022 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.register_service;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Writer;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SkippingScaleN5Writer implements N5Writer {


	private final N5Writer delegate;

	private final int skippedLevels;

	@Override
	public <T> void setAttribute(String pathName, String key, T attribute)
		throws IOException
	{
		delegate.setAttribute(rewritePath(pathName), key, attribute);
	}

	@Override
	public void setAttributes(String pathName, Map<String, ?> attributes)
		throws IOException
	{
		delegate.setAttributes(rewritePath(pathName), attributes);
	}

	@Override
	public void setDatasetAttributes(String pathName,
		DatasetAttributes datasetAttributes) throws IOException
	{
		delegate.setDatasetAttributes(rewritePath(pathName), datasetAttributes);
	}

	@Override
	public void createGroup(String pathName) throws IOException {
		delegate.createGroup(rewritePath(pathName));
	}

	@Override
	public boolean remove(String pathName) throws IOException {
		return delegate.remove(rewritePath(pathName));
	}

	@Override
	public boolean remove() throws IOException {
		return delegate.remove();
	}

	@Override
	public void createDataset(String pathName,
		DatasetAttributes datasetAttributes) throws IOException
	{
		delegate.createDataset(rewritePath(pathName), datasetAttributes);
	}

	@Override
	public void createDataset(String pathName, long[] dimensions, int[] blockSize,
		DataType dataType, Compression compression) throws IOException
	{
		delegate.createDataset(rewritePath(pathName), dimensions, blockSize,
			dataType,
			compression);
	}

	@Override
	public Version getVersion() throws IOException {
		return delegate.getVersion();
	}

	@Override
	public <T> void writeBlock(String pathName,
		DatasetAttributes datasetAttributes, DataBlock<T> dataBlock)
		throws IOException
	{
		delegate.writeBlock(rewritePath(pathName), datasetAttributes, dataBlock);
	}

	@Override
	public boolean deleteBlock(String pathName, long[] gridPosition)
		throws IOException
	{
		return delegate.deleteBlock(rewritePath(pathName), gridPosition);
	}

	@Override
	public <T> T getAttribute(String pathName, String key, Class<T> clazz)
		throws IOException
	{
		return delegate.getAttribute(rewritePath(pathName), key, clazz);
	}

	@Override
	public DatasetAttributes getDatasetAttributes(String pathName)
		throws IOException
	{
		return delegate.getDatasetAttributes(rewritePath(pathName));
	}

	@Override
	public DataBlock<?> readBlock(String pathName,
		DatasetAttributes datasetAttributes, long[] gridPosition) throws IOException
	{
		return delegate.readBlock(rewritePath(pathName), datasetAttributes,
			gridPosition);
	}

	@Override
	public boolean exists(String pathName) {
		return delegate.exists(rewritePath(pathName));
	}

	@Override
	public boolean datasetExists(String pathName) throws IOException {
		return delegate.datasetExists(rewritePath(pathName));
	}

	@Override
	public String[] list(String pathName) throws IOException {
		return delegate.list(rewritePath(pathName));
	}

	@Override
	public Map<String, Class<?>> listAttributes(String pathName)
		throws IOException
	{
		return delegate.listAttributes(rewritePath(pathName));
	}

	private String rewritePath(String path) {
		Pattern pattern = Pattern.compile("s(\\d+)$");
		Matcher m = pattern.matcher(path);
		m.find();
		int levelId = Integer.parseInt(m.group(1));
		return m.replaceAll("s" + (levelId + skippedLevels));
	}
}
