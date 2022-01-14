/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2022 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.bdv_server;

import java.io.IOException;
import java.io.Writer;

import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import bdv.ViewerImgLoader;
import bdv.img.remote.RemoteImageLoader;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import mpicbg.spim.data.SpimDataException;

class BuildRemoteDatasetXmlTS {

	/**
	 * Create a modified dataset XML by replacing the ImageLoader with an
	 * {@link RemoteImageLoader} pointing to the data we are serving.
	 */
	static void run(final XmlIoSpimDataMinimal io, final SpimDataMinimal spimData,
		ViewerImgLoader imgLoader, Writer writer) throws IOException,
		SpimDataException
	{
		final SpimDataMinimal s = new SpimDataMinimal(spimData, imgLoader);
		final Document doc = new Document(io.toXml(s, s.getBasePath()));
		final XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
		xout.output(doc, writer);
	}

}
