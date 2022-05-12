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
import cz.it4i.fiji.datastore.ApplicationConfiguration;
import cz.it4i.fiji.datastore.DatasetHandler;
import cz.it4i.fiji.datastore.core.Version;
import cz.it4i.fiji.datastore.register_service.Dataset;
import cz.it4i.fiji.datastore.register_service.DatasetRepository;
import cz.it4i.fiji.datastore.register_service.DatasetVersion;
import mpicbg.spim.data.SpimData;
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
import mpicbg.spim.data.sequence.ViewSetup;

@ApplicationScoped
public final class GetSpimDataMinimalTS {

	@Inject
	DatasetRepository repository;

	@Inject
	ApplicationConfiguration configuration;

	SpimDataMinimal run(String uuid, String versionStr)
		throws SpimDataException
	{
		try {
			switch (versionStr) {
				case "all":
					return getAllVersionInOneDataset(uuid);
				default:
					return getOneVersionInOneDataset(uuid, versionStr);
			}
		}
		catch (RuntimeException exc) {
			if (exc.getCause() instanceof SpimDataException) {
				throw (SpimDataException) exc.getCause();
			}
			throw exc;
		}
	}

	private SpimDataMinimal getAllVersionInOneDataset(String uuid)
		throws SpimDataException
	{
		Dataset dataset = repository.findByUUID(uuid);
		Map<Integer, BasicViewSetup> setups = new HashMap<>();
		Map<ViewId, ViewRegistration> viewRegistrations = new HashMap<>();
		Collection<ViewId> missingViews = new LinkedList<>();

		DatasetHandler handler = configuration.getDatasetHandler(uuid);
		SpimDataMinimal spimDataMinimal;
		spimDataMinimal = SpimDataMapper.asSpimDataMinimal(handler.getSpimData());
		SequenceDescriptionMinimal seqMinimal = spimDataMinimal
			.getSequenceDescription();
		BasicImgLoader basicImgLoader = seqMinimal.getImgLoader();
		TimePoints timePoints = seqMinimal.getTimePoints();

		File path = spimDataMinimal.getBasePath();

		for (DatasetVersion version : dataset.getDatasetVersion().stream().sorted(
			comparingInt(DatasetVersion::getValue)).collect(toList()))
		{
			Version processedVersion = new Version(version.getValue());

			processVersion(processedVersion, setups, viewRegistrations, missingViews,
				spimDataMinimal, seqMinimal);
		}
		processVersion(new Version(Version.MIXED_LATEST_VERSION_NAME), setups,
			viewRegistrations, missingViews, spimDataMinimal, seqMinimal);

		SequenceDescriptionMinimal sd = new SequenceDescriptionMinimal(timePoints,
			setups, basicImgLoader, new MissingViews(missingViews));

		ViewRegistrations vr = new ViewRegistrations(viewRegistrations);
		SpimDataMinimal result = new SpimDataMinimal(path, sd, vr);
		return result;
	}

	public void processVersion(Version processedVersion,
		Map<Integer, BasicViewSetup> setups,
		Map<ViewId, ViewRegistration> viewRegistrations,
		Collection<ViewId> missingViews, SpimDataMinimal spimDataMinimal,
		SequenceDescriptionMinimal seqMinimal)
	{
		Map<ViewRegistration, ViewRegistration> origViewregistrationPerNewOne =
			new HashMap<>();
		Map<Integer, Integer> origSetupIdPerNewOne = new HashMap<>();




		for (BasicViewSetup viewSetup : seqMinimal.getViewSetupsOrdered()) {
			BasicViewSetup newViewSetup = new BasicViewSetup(setups.size(),
				getViewSetupName(viewSetup, processedVersion), viewSetup.getSize(),
				viewSetup.getVoxelSize());

			for (Entry<String, Entity> entries : viewSetup.getAttributes()
				.entrySet())
			{
				newViewSetup.setAttribute(entries.getValue());
			}
			newViewSetup.setAttribute(processedVersion);
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

	private static String getViewSetupName(BasicViewSetup viewSetup,
		Version version)
	{
		return String.format("Angle: %d, Channel: %d, Version: %s", viewSetup
			.getAttribute(Angle.class).getId(), viewSetup.getAttribute(Channel.class)
				.getId(), version.getName());
	}



	private SpimDataMinimal getOneVersionInOneDataset(String uuid,
		String versionStr) throws SpimDataException
	{
		repository.findByUUID(uuid);

		try {
			final int version = stringToIntVersion(versionStr);
			// only for check that version exists
			repository.findByUUIDVersion(uuid, version);
			SpimData spimdata = configuration.getDatasetHandler(uuid).getSpimData(
				version);
			for (ViewSetup vs : spimdata.getSequenceDescription()
				.getViewSetupsOrdered())
			{
				vs.setAttribute(new Version(versionStr));
			}
			return SpimDataMapper.asSpimDataMinimal(spimdata);
		}
		catch (NumberFormatException exc) {
			throw new NotFoundException(String.format("Dataset %s has no version %ds",
				uuid, versionStr));
		}


	}

}
