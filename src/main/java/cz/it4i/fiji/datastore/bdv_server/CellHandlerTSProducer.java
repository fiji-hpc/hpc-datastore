/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/

package cz.it4i.fiji.datastore.bdv_server;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import cz.it4i.fiji.datastore.ApplicationConfiguration;
import cz.it4i.fiji.datastore.register_service.Dataset;
import cz.it4i.fiji.datastore.register_service.DatasetRepository;
import mpicbg.spim.data.SpimDataException;

@ApplicationScoped
class CellHandlerTSProducer {

	@Inject
	DatasetRepository repository;

	@Inject
	ApplicationConfiguration configuration;

	CellHandlerTS produce(String baseURL, String uuid, int version) {
		Dataset dataset = repository.findByUUID(uuid);

		// only for check that version exists
		repository.findByUUIDVersion(uuid, version);

		try {
			return new CellHandlerTS(configuration.getDatasetHandler(uuid), dataset,
				baseURL, version, uuid + "_version-" + version, System.getProperty("java.io.tmpdir"));
		}
		catch (SpimDataException | IOException exc) {
			throw new RuntimeException(exc);
		}
	}

}
