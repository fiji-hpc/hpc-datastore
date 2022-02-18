/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2022 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.bdv_server;

import static cz.it4i.fiji.datastore.core.Version.stringToIntVersion;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import cz.it4i.fiji.datastore.DatasetPathRoutines;
import cz.it4i.fiji.datastore.core.Version;
import cz.it4i.fiji.datastore.register_service.Dataset;
import cz.it4i.fiji.datastore.register_service.DatasetRepository;
import cz.it4i.fiji.datastore.register_service.DatasetVersion;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.MissingViews;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.ViewId;

@ApplicationScoped
public final class GetSpimDataMinimalTS {

	@Inject
	DatasetRepository repository;

	SpimDataMinimal run(XmlIoSpimDataMinimal io, String uuid, String versionStr)
		throws SpimDataException
	{
		try {
			switch (versionStr) {
				case "all":
					return getAllVersionInOneDataset(io, uuid);
				default:
					return getOneVersionInOneDataset(io, uuid, versionStr);
			}
		}
		catch (RuntimeException exc) {
			if (exc.getCause() instanceof SpimDataException) {
				throw (SpimDataException) exc.getCause();
			}
			throw exc;
		}
	}

	private SpimDataMinimal getAllVersionInOneDataset(XmlIoSpimDataMinimal io,
		String uuid)
	{
		Dataset dataset = repository.findByUUID(uuid);
		Map<Integer, BasicViewSetup> setups = new HashMap<>();
		Map<ViewId, ViewRegistration> viewRegistrations = new HashMap<>();
		Collection<ViewId> missingViews = new LinkedList<>();
		File path = null;
		TimePoints timePoints = null;
		BasicImgLoader basicImgLoader = null;
		for (DatasetVersion version : dataset.getDatasetVersion().stream().sorted(
			comparingInt(DatasetVersion::getValue)).collect(toList()))
		{
			SpimDataMinimal spimDataMinimal = loadSpimData(io, dataset, version);

			Map<ViewRegistration, ViewRegistration> origViewregistrationPerNewOne =
				new HashMap<>();
			Map<Integer, Integer> origSetupIdPerNewOne = new HashMap<>();

			SequenceDescriptionMinimal seqMinimal = spimDataMinimal
				.getSequenceDescription();
			if (timePoints == null) {
				timePoints = seqMinimal.getTimePoints();
			}
			if (basicImgLoader == null) {
				basicImgLoader = seqMinimal.getImgLoader();
			}
			if (path == null) {
				path = spimDataMinimal.getBasePath();
			}

			for (BasicViewSetup viewSetup : seqMinimal.getViewSetupsOrdered()) {
				BasicViewSetup newViewSetup = new BasicViewSetup(setups.size(),
					getViewSetupName(viewSetup, version), viewSetup.getSize(),
					viewSetup.getVoxelSize());

				for (Entry<String, Entity> entries : viewSetup.getAttributes()
					.entrySet())
				{
					newViewSetup.setAttribute(entries.getValue());
				}
				newViewSetup.setAttribute(new Version(version.getValue()));
				setups.put(newViewSetup.getId(), newViewSetup);
				origSetupIdPerNewOne.put(viewSetup.getId(), newViewSetup.getId());
			}
			for (ViewRegistration origViewRegistration : spimDataMinimal
				.getViewRegistrations().getViewRegistrationsOrdered())
			{
				ViewRegistration viewRegistration = new ViewRegistration(
					origViewRegistration.getTimePointId(), origSetupIdPerNewOne.get(
						origViewRegistration.getViewSetupId()), new ArrayList<>(
							origViewRegistration.getTransformList()));
				viewRegistrations.put(viewRegistration, viewRegistration);
				origViewregistrationPerNewOne.put(origViewRegistration,
					viewRegistration);
			}

			MissingViews origMissingViews = spimDataMinimal.getSequenceDescription()
				.getMissingViews();
			if (origMissingViews != null) {
				missingViews.addAll(origMissingViews.getMissingViews().stream().map(
					oldViewId -> origViewregistrationPerNewOne.get(oldViewId)).collect(
						toList()));
			}

		}

		SequenceDescriptionMinimal sd = new SequenceDescriptionMinimal(timePoints,
			setups, basicImgLoader, new MissingViews(missingViews));

		ViewRegistrations vr = new ViewRegistrations(viewRegistrations);
		SpimDataMinimal result = new SpimDataMinimal(path, sd, vr);
		return result;
	}

	private static String getViewSetupName(BasicViewSetup viewSetup,
		DatasetVersion version)
	{
		return String.format("Angle: %d, Channel: %d, Version: %d", viewSetup
			.getAttribute(Angle.class).getId(), viewSetup.getAttribute(Channel.class)
				.getId(), version.getValue());
	}

	private SpimDataMinimal loadSpimData(XmlIoSpimDataMinimal io, Dataset dataset,
		DatasetVersion ds)
	{
		java.nio.file.Path xmlPath = DatasetPathRoutines.getXMLPath(Paths.get(
			dataset.getPath()), ds.getValue());

		try {
			return io.load(xmlPath.toString());
		}
		catch (SpimDataException exc) {
			throw new RuntimeException(exc);
		}
	}

	private SpimDataMinimal getOneVersionInOneDataset(XmlIoSpimDataMinimal io,
		String uuid, String versionStr)
	{
		Dataset dataset = repository.findByUUID(uuid);

		try {
			final int version = stringToIntVersion(versionStr);
			// only for check that version exists
			return loadSpimDataMinimal(io, uuid, dataset, version);
		}
		catch (NumberFormatException exc) {
			throw new NotFoundException(String.format("Dataset %s has no version %ds",
				uuid, versionStr));
		}

	}

	private SpimDataMinimal loadSpimDataMinimal(XmlIoSpimDataMinimal io,
		String uuid, Dataset dataset, final int version)
	{
		DatasetVersion ds = repository.findByUUIDVersion(uuid, version);

		return loadSpimData(io, dataset, ds);
	}

}
