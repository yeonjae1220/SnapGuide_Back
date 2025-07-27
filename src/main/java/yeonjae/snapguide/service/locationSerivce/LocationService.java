package yeonjae.snapguide.service.locationSerivce;

import yeonjae.snapguide.domain.location.Location;

import java.io.File;

public interface LocationService {
    public Location extractAndResolveLocation(File file);

    public Location saveLocation(Double lat, Double lng);

}
