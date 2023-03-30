package cz.it4i.fiji.datastore.zarr;

import cz.it4i.fiji.datastore.DatasetFilesystemHandler;
import cz.it4i.fiji.datastore.DatasetHandler;
import cz.it4i.fiji.datastore.register_service.DatasetRepository;
import lombok.extern.log4j.Log4j2;

import java.nio.file.Path;

@Log4j2
public class HandlerFactory {
    public static DatasetHandler CreateHandlerByType(String type, String uuid, Path path) {
        return ResolveDataset(type,uuid,path);
    }
    public static DatasetHandler CreateHandlerByAtribute(String uuid, Path path) {
        DatasetRepository datasetDAO = new DatasetRepository();
        String type = datasetDAO.findTypebyUUID(uuid);
        //Old dataset doesnt have Dataset.datasetType so its old N5 dataset.So try to update it.
        if(type ==null) {
            try{
                datasetDAO.updateDatasetTypebyUUID(uuid,"N5");
                }
            catch (Exception e)
            {
                log.warn("Something went wrong with updating old dataset",e);
            }
            finally{
                type="N5";
            }
        }
        return ResolveDataset(type,uuid,path);
    }
    private static DatasetHandler ResolveDataset(String type,String uuid, Path path)
    {
        switch(type) {
            case "Zarr":
                return new DatasetFileSystemHandlerZarr(uuid,path);
            default:
                return new DatasetFilesystemHandler(uuid,path);
        }
    }
}
