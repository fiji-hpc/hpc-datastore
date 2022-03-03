/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2022 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.janelia.saalfeldlab.n5.N5Writer;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AttributeSetter implements AutoCloseable {

	final private N5Writer n5Writer;

	final private Map<String, Map<String, Object>> asPathAttributes =
		new HashMap<>();

	public void setAttribute(String path, String key, Object value) {
		Map<String, Object> attributes = asPathAttributes.computeIfAbsent(path,
			$ -> new HashMap<>());
		attributes.put(key, value);
	}

	@Override
	public void close() throws IOException {
		for (Entry<String, Map<String, Object>> entry : asPathAttributes
			.entrySet())
		{
			n5Writer.setAttributes(entry.getKey(), entry.getValue());
		}

	}

}
