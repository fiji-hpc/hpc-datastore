package cz.it4i.fiji.datastore.zarr;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.of;

import java.util.Map;
import java.util.Optional;

import cz.it4i.fiji.datastore.register_service.DatasetType;

public enum DatasetTypeEnum implements DatasetType
{
	N5, ZARR;

	private static final Map< String, DatasetType > stringToEnum = of( values() ).collect( toMap( k -> k.toString().toLowerCase(), e -> e ) );

	// Returns Operation for string, if any
	public static Optional< DatasetType > fromString( String symbol )
	{
		return Optional.ofNullable( stringToEnum.get( symbol.toLowerCase() ) );
	}

}
