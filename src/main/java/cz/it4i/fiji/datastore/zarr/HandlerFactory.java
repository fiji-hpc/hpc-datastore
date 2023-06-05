
package cz.it4i.fiji.datastore.zarr;

import static cz.it4i.fiji.datastore.zarr.DatasetTypeEnum.N5;
import static cz.it4i.fiji.datastore.zarr.DatasetTypeEnum.ZARR;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.BiFunction;

import cz.it4i.fiji.datastore.DatasetFilesystemHandler;
import cz.it4i.fiji.datastore.DatasetHandler;
import cz.it4i.fiji.datastore.register_service.DatasetType;

public class HandlerFactory {

	private final static Map<DatasetTypeEnum, BiFunction< String, Path, DatasetHandler >> FACTORIES 
			= Map.of( N5, DatasetFilesystemHandler::new, ZARR, DatasetFileSystemHandlerZarr::new );

	public static DatasetHandler createHandler( String uuid, Path path, DatasetType type )
	{
		return FACTORIES.get( type ).apply( uuid, path );
	}


}
