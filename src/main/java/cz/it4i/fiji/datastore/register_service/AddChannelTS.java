/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.register_service;

import static cz.it4i.fiji.datastore.core.MipmapInfoAssembler.createExportMipmapInfo;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.imglib2.realtransform.AffineTransform3D;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.N5Writer;

import bdv.img.hdf5.MipmapInfo;
import cz.it4i.fiji.datastore.ApplicationConfiguration;
import cz.it4i.fiji.datastore.CreateNewDatasetTS;
import cz.it4i.fiji.datastore.DatasetHandler;
import cz.it4i.fiji.datastore.base.Factories;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.Illumination;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.ViewSetup;

public class AddChannelTS {

	private ApplicationConfiguration _configuration;

	public AddChannelTS(ApplicationConfiguration configuration) {
		this._configuration = configuration;
	}

	public void run(Dataset dataset, int channels, Compression compression)
		throws SpimDataException,
		IOException
	{
		DatasetHandler datasetHandler = _configuration.getDatasetHandler(dataset
			.getUuid());
		SpimData spimData = datasetHandler.getSpimData();
		for (DatasetVersion version : dataset.getDatasetVersion()) {

			Collection<ViewSetup> addedViewSetups =
				new LinkedList<>();
			createViewSetups(dataset, spimData, channels, addedViewSetups);
			createViewRegistrations(spimData, addedViewSetups);
			createN5Structure(datasetHandler.getWriter(version.getValue()), dataset,
				spimData, compression, addedViewSetups);
			datasetHandler.saveSpimData(spimData);
		}
	}

	private void createN5Structure(N5Writer writer, Dataset dataset,
		SpimData spimData, Compression compression,
		Collection<ViewSetup> addedViewSetups) throws IOException
	{
		MipmapInfo mi = createExportMipmapInfo(DatasetAssembler
			.createDatatransferObject(dataset.getResolutionLevel()));
		CreateNewDatasetTS.createN5Structure(writer, DataType.fromString(dataset
			.getVoxelType()), dataset.getDimensions(), compression, mi, spimData
				.getSequenceDescription(), addedViewSetups.stream().map(vs -> vs
					.getId()).collect(Collectors.toList()), spimData
						.getSequenceDescription().getTimePoints().getTimePointsOrdered()
						.stream().map(tp -> tp.getId()).collect(Collectors.toList()));
	}

	private void createViewSetups(Dataset dataset, SpimData spimData,
		int channels,
		Collection<ViewSetup> addedViewSetups)
	{
		int firstEmptyChannelId = spimData.getSequenceDescription()
			.getAllChannelsOrdered().stream().map(ch -> ch.getId() + 1).reduce((first,
				second) -> second).orElse(0);
		int setupID = spimData.getSequenceDescription().getViewSetupsOrdered()
			.stream().map(vs -> vs.getId() + 1).reduce((first, second) -> second)
			.orElse(0);
		List<Channel> channelList = IntStream.range(firstEmptyChannelId,
			firstEmptyChannelId + channels).mapToObj(id -> new Channel(id)).collect(
				Collectors.toList());

		FinalVoxelDimensions fvd = new FinalVoxelDimensions(dataset.getVoxelUnit(),
			dataset.getVoxelResolution());
		for (Channel channel : channelList) {
			for (Angle angle : spimData.getSequenceDescription()
				.getAllAnglesOrdered())
			{
				for (Illumination illumination : spimData.getSequenceDescription()
					.getAllIlluminationsOrdered())
				{
					ViewSetup vs = Factories.constructViewSetup(setupID, dataset
						.getDimensions(), fvd, channel, angle, illumination);
					@SuppressWarnings("unchecked")
					Map<Integer, ViewSetup> vss = (Map<Integer, ViewSetup>) spimData
						.getSequenceDescription().getViewSetups();
					vss.put(vs.getId(), vs);
					spimData.getSequenceDescription().getViewSetupsOrdered().add(vs);
					addedViewSetups.add(vs);
					setupID++;
				}
			}
		}
	}

	private void createViewRegistrations(SpimData spimData,
		Collection<ViewSetup> addedViewSetups)
	{
		Map<Angle,AffineTransform3D> transforms = new HashMap<>();
		for (ViewRegistration vr : spimData.getViewRegistrations()
			.getViewRegistrationsOrdered())
		{
			ViewSetup vs = spimData.getSequenceDescription().getViewSetups().get(vr.getViewSetupId());
			transforms.put(vs.getAngle(), vr.getModel());
		}
		for (ViewSetup vs : addedViewSetups) {
			for (TimePoint tp : spimData.getSequenceDescription().getTimePoints()
				.getTimePointsOrdered())
			{
				ViewRegistration vr = new ViewRegistration(tp.getId(), vs.getId(),
					transforms.get(vs.getAngle()));
				spimData.getViewRegistrations().getViewRegistrations().put(vr, vr);
			}
		}
	}
}
