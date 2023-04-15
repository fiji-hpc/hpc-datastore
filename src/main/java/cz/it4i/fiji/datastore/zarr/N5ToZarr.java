package cz.it4i.fiji.datastore.zarr;

import cz.it4i.fiji.datastore.DatasetFilesystemHandler;

import cz.it4i.fiji.datastore.register_service.OperationMode;
import cz.it4i.fiji.datastore.N5Access;
import mpicbg.spim.data.SpimDataException;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import static cz.it4i.fiji.datastore.register_service.OperationMode.READ_WRITE;
/*
public class N5ToZarr {

    public void translate(String uuidN5,String pathN5,String uuidZarr,String pathZarr) throws IOException, SpimDataException {
        /*
        // vytvoření instance DatasetHandler pro N5
        final DatasetFilesystemHandler n5Handler = new DatasetFilesystemHandler(uuidN5, pathN5);
        final DatasetFileSystemHandlerZarr zarrHandler = new DatasetFileSystemHandlerZarr(uuidZarr, Path.of(pathZarr));
        //nacteni writeru
        N5Writer reader = n5Handler.getWriter();
        N5Writer writerZarr = zarrHandler.getWriter();
        N5Access n5=new N5Access(n5Handler.getSpimData(),reader, Collections.singletonList(dataset.getSortedResolutionLevels().get(0)
                .getResolutions()),OperationMode.READ_WRITE);
        N5Access zarr =new N5Access(zarrHandler.getSpimData(),writerZarr,Collections.singletonList(dataset.getSortedResolutionLevels().get(0)
                .getResolutions()),OperationMode.READ_WRITE);
        n5.read()

        DatasetAttributes attributes = reader.getDatasetAttributes(pathN5);
        DataType dataType = attributes.getDataType();
        long[] dimensions = attributes.getDimensions();


        // Nacteni dat po blocich a zapis do Zarr
        int[] blockDimensions = attributes.getBlockSize();
        long[] gridPosition = new long[dimensions.length];
        long[] blockSize = new long[dimensions.length];


        for (int i = 0; i < dimensions.length; i++) {
            gridPosition[i] = 0;
            blockSize[i] = blockDimensions[i];
        }

        final long[] numBlocks = N5Utils.numBlocks(dimensions, blockSize);
        for (long blockZ = 0; blockZ < numBlocks[2]; blockZ++) {
            gridPosition[2] = blockZ * blockSize[2];
            for (long blockY = 0; blockY < numBlocks[1]; blockY++) {
                gridPosition[1] = blockY * blockSize[1];
                for (long blockX = 0; blockX < numBlocks[0]; blockX++) {
                    gridPosition[0] = blockX * blockSize[0];

                    final Object data = reader.readBlock(pathN5, attributes, gridPosition);

                    long[] linearIndices = N5Utils.blockGridPositionToLinearIndex(gridPosition, dimensions, blockSize);
                    writerZarr.writeBlock(pathZarr, linearIndices, data);
                }
            }
        }
    }
}
*/