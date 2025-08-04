package yeonjae.snapguide.repository.locationRepository;

import yeonjae.snapguide.domain.location.Location;

import java.util.List;

public interface LocationRepositoryCustom {
    public List<Location> findLocationByCoordinate(Double lat, Double lng);

    public List<Location> findWithinSquare(double lat, double lng, double radiusKm);

    public List<Location> findWithinRadius(double targetLat, double targetLng, double radiusInKm);

    public List<Location> findNearby(double lat, double lon, double radiusKm);
}
