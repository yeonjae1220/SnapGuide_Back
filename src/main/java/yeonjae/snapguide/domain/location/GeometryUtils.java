package yeonjae.snapguide.domain.location;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

public class GeometryUtils {
    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    public static Point createPoint(double lat, double lng) {
        return geometryFactory.createPoint(new Coordinate(lng, lat)); // ⚠️ lng 먼저!
    }
}
