/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.register_service;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.ws.rs.NotFoundException;

import cz.it4i.fiji.datastore.DatasetFilesystemHandler;

/*import org.apache.deltaspike.data.api.AbstractEntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.Repository;*/

//@Repository(forEntity = Dataset.class)

@Default
@ApplicationScoped
public class DatasetRepository implements PanacheRepository<Dataset>,
	Serializable
{

	private static final long serialVersionUID = 7503192760728646786L;

	public Dataset findByUUID(UUID uuid) {
		Optional<Dataset> result = find("from Dataset where uuid = :uuid",
			Parameters.with("uuid",
			uuid)).singleResultOptional();
		if (result.isEmpty()) {
			throw new NotFoundException("Dataset with UUID = " + uuid +
				" not found ");
		}

		DatasetFilesystemHandler dfh = new DatasetFilesystemHandler(null, result
			.get().getPath());
		try {
			result.get().setDatasetVersion( dfh.getAllVersions().stream().map(v -> new DatasetVersion(v, Paths.get(
				result.get().getPath()).resolve("" + v).toString())).collect(Collectors
					.toList()));
		}
		catch (IOException exc) {
			throw new RuntimeException(exc);
		}
		return result.get();
	}

	public DatasetVersion findByUUIDVersion(UUID uuid, int version) {
		Dataset dataset = findByUUID(uuid);
		Optional<DatasetVersion> result = dataset.getDatasetVersion().stream()
			.filter(v -> v.getValue() == version).findAny();
		if (result.isEmpty()) {
			throw new NotFoundException("Dataset with UUID = " + uuid +
				" has no version " + version);
		}
		return result.get();
	}
}
