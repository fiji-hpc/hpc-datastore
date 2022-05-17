/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2022 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.register_service;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class DatasetAlreadyInsertedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	@Getter
	final private String uuid;

}
