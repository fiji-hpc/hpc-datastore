/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2022 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.bdv_server;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;

import bdv.spimdata.SpimDataMinimal;
import cz.it4i.fiji.datastore.ApplicationConfiguration;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ThumbnailProviderTS {

	private static final int THUMBNAIL_WIDTH = 100;

	private static final int THUMBNAIL_HEIGHT = 100;

	/**
	 * Full path to thumbnail png.
	 */
	private final String thumbnailFilename;

	public ThumbnailProviderTS(SpimDataMinimal spimData, String datasetName,
		final String thumbnailsDirectory)
	{
		thumbnailFilename = createThumbnail(spimData, datasetName,
			thumbnailsDirectory);
	}

	public void runForThumbnail(final HttpServletResponse response)
		throws IOException
	{
		provideThumbnail(response);
	}

	private void provideThumbnail(final HttpServletResponse response)
		throws IOException
	{
		final Path path = Paths.get(thumbnailFilename);
		if (Files.exists(path)) {
			final byte[] imageData = Files.readAllBytes(path);
			if (imageData != null) {
				response.setContentType("image/png");
				response.setContentLength(imageData.length);
				response.setStatus(HttpServletResponse.SC_OK);

				try (final OutputStream os = response.getOutputStream()) {
					os.write(imageData);
				}
			}
		}
	}

	/**
	 * Create PNG thumbnail file named "{@code <baseFilename>.png}".
	 */
	private static String createThumbnail(final SpimDataMinimal spimData,
		final String datasetName, final String thumbnailsDirectory)
	{
		final String baseFilename = ApplicationConfiguration.BASE_NAME;
		final String thumbnailFileName = Paths.get(thumbnailsDirectory).resolve(
			datasetName + ".png").toAbsolutePath().toString();
		final File thumbnailFile = new File(thumbnailFileName);
		if (!thumbnailFile.isFile()) // do not recreate thumbnail if it already
																	// exists
		{
			final BufferedImage bi = makeThumbnail(spimData, baseFilename,
				THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
			try {
				ImageIO.write(bi, "png", thumbnailFile);
			}
			catch (final IOException e) {
				log.warn("Could not create thumbnail png for dataset \"" +
					baseFilename + "\"");
				log.warn(e.getMessage());
			}
		}
		return thumbnailFileName;
	}

	private static BufferedImage makeThumbnail(SpimDataMinimal spimData,
		String baseFilename2, int thumbnailWidth, int thumbnailHeight)
	{

		return ThumbnailGenerator.makeThumbnail(spimData, baseFilename2,
			thumbnailWidth, thumbnailHeight);
	}
}
