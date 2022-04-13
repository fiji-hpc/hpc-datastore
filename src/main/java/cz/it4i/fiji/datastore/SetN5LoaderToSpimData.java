/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2022 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import java.io.File;
import java.util.function.Function;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.sequence.ImgLoader;
import mpicbg.spim.data.sequence.SequenceDescription;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class SetN5LoaderToSpimData {

	static SpimData $(SpimData spimData,
		Function<SequenceDescription, ImgLoader> loaderFunction, File basePath)
	{
		final SequenceDescription tempSeq = spimData.getSequenceDescription();
		ImgLoader imgLoader = loaderFunction.apply(tempSeq);
		final SequenceDescription sequenceDescription = new SequenceDescription(
			tempSeq.getTimePoints(), tempSeq.getViewSetupsOrdered(), imgLoader,
			tempSeq.getMissingViews());
		return new SpimData(basePath, sequenceDescription, spimData
			.getViewRegistrations());

	}

}
