package cz.it4i.fiji.datastore.zarr;

import java.util.Map;
import org.janelia.saalfeldlab.n5.zarr.ZarrCompressor;
public class ZarrHelpers {
    public static boolean IsCompresionSupported(String myCompression)
    {
        Map<String, Class<? extends ZarrCompressor>> registry = ZarrCompressor.registry;
        boolean isSupported = registry.containsKey(myCompression);
        return isSupported;
    }
}
