/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2022 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.register_service;

import java.io.PrintStream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;

import bdv.export.ProgressWriter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LoggerProgressWriter implements ProgressWriter {

	private final Logger logger;

	private final String subject;

	@Override
	public PrintStream out() {
		return IoBuilder.forLogger(logger).setLevel(Level.INFO).buildPrintStream();
	}

	@Override
	public PrintStream err() {
		return IoBuilder.forLogger(logger).setLevel(Level.ERROR).buildPrintStream();
	}

	@Override
	public void setProgress(double completionRatio) {

		logger.info("{} progress: {} % complete", subject, completionRatio * 100);
	}

}
