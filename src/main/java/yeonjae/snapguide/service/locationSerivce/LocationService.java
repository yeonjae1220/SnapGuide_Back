package yeonjae.snapguide.service.locationSerivce;

import yeonjae.snapguide.domain.location.Location;

import java.io.File;
import java.io.InputStream;

public interface LocationService {
//    public Location extractAndResolveLocation(File file);
    public Location extractAndResolveLocation(InputStream inputStream);

    public Location saveLocation(Double lat, Double lng);

}
