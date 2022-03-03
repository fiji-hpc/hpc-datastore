/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2022 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.bdv_server;

import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.sequence.SequenceDescription;

final class SpimDataMapper {

	private SpimDataMapper() {}

	static SpimDataMinimal asSpimDataMinimal(SpimData spimData) {
		final SequenceDescription tempSeq = spimData.getSequenceDescription();
		final SequenceDescriptionMinimal sequenceDescription =
			new SequenceDescriptionMinimal(tempSeq.getTimePoints(), tempSeq
				.getViewSetups(), tempSeq.getImgLoader(), tempSeq.getMissingViews());
		return new SpimDataMinimal(spimData.getBasePath(), sequenceDescription,
			spimData.getViewRegistrations());
	}


}
