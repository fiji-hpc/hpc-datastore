/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/

package cz.it4i.fiji.datastore.bdv_server;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import cz.it4i.fiji.datastore.ApplicationConfiguration;
import cz.it4i.fiji.datastore.DatasetPathRoutines;
import cz.it4i.fiji.datastore.register_service.Dataset;
import cz.it4i.fiji.datastore.register_service.DatasetRepository;
import mpicbg.spim.data.SpimDataException;

@ApplicationScoped
class CellHandlerTSProducer {

	@Inject
	DatasetRepository repository;

	@Inject
	ApplicationConfiguration configuration;

	CellHandlerTS produce(String baseURL, UUID uuid, int version) {
		Dataset dataset = repository.findByUUID(uuid);

		// only for check that version exists
		repository.findByUUIDVersion(uuid, version);

		Path xmlPath = DatasetPathRoutines.getXMLPath(configuration.getDatasetPath(
			uuid), Math.max(version, 0));
		try {
			return new CellHandlerTS(dataset, baseURL, version, xmlPath.toRealPath()
				.toString(), uuid + "_version-" + version, "/tmp/datastore");
		}
		catch (SpimDataException | IOException exc) {
			throw new RuntimeException(exc);
		}
	}

}
