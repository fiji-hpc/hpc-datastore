/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2023 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.register_service;

import static cz.it4i.fiji.datastore.zarr.DatasetTypeEnum.N5;
import static cz.it4i.fiji.datastore.zarr.DatasetTypeEnum.fromString;

import com.google.common.base.Strings;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import javax.ws.rs.NotSupportedException;

import cz.it4i.fiji.datastore.zarr.DatasetTypeEnum;

@Converter
public class DatasetTypeConverter implements AttributeConverter< DatasetType, String >
{

	@Override
	public String convertToDatabaseColumn( DatasetType attribute )
	{
		return attribute.toString();
	}

	@Override
	public DatasetType convertToEntityAttribute( String dbData )
	{
		if ( Strings.isNullOrEmpty( dbData ) )
		{ return N5; }
		return fromString( dbData.toString() ).orElseThrow( () -> new NotSupportedException( "DatasetType" + dbData + " not supported" ) );
	}


}
