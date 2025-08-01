package yeonjae.snapguide.repository.locationRepository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import yeonjae.snapguide.domain.location.Location;
import yeonjae.snapguide.domain.location.QLocation;

import java.util.List;

@RequiredArgsConstructor
@Repository
public class LocationRepositoryCustomImpl implements LocationRepositoryCustom{

    private final EntityManager em;
    private final JPAQueryFactory jpaQueryFactory;

    private static final int SRID = 4326; // WGS 84
    private static final int METERS_PER_KM = 1000;

    // 좌표로 정확히 일치하는 지점 찾기 (소수점 비교 대신 Point 비교)
    @Override
    public List<Location> findLocationByCoordinate(Double lat, Double lng) {
        QLocation location = QLocation.location;

        String pointWKT = String.format("SRID=%d;POINT(%f %f)", SRID, lng, lat); // lng 먼저
        BooleanExpression equals = Expressions.booleanTemplate(
                "ST_Equals({0}, ST_GeomFromText({1}))",
                location.coordinate,
                pointWKT
        );

        return jpaQueryFactory.selectFrom(location)
                .where(equals)
                .fetch();
    }

    // 정사각형 박스 내 좌표 탐색 (PostGIS의 ST_MakeEnvelope 사용)
    @Override
    public List<Location> findWithinSquare(double lat, double lng, double radiusKm) {
        QLocation location = QLocation.location;

        // WKT Envelope: ST_MakeEnvelope(minX, minY, maxX, maxY, SRID)
        double[] box = GeoUtil.getBoundingBox(lat, lng, radiusKm);
        double minLat = box[0], maxLat = box[1];
        double minLng = box[2], maxLng = box[3];

        String envelope = String.format(
                "ST_MakeEnvelope(%f, %f, %f, %f, %d)",
                minLng, minLat, maxLng, maxLat, SRID
        );

        BooleanExpression within = Expressions.booleanTemplate(
                "ST_Contains(" + envelope + ", {0})",
                location.coordinate
        );

        return jpaQueryFactory.selectFrom(location)
                .where(within)
                .fetch();
    }

    // 주어진 반경(km) 안의 좌표 탐색 (ST_DWithin 사용)
    @Override
    public List<Location> findWithinRadius(double targetLat, double targetLng, double radiusKm) {
        QLocation location = QLocation.location;

//        String pointWKT = String.format("SRID=%d;POINT(%f %f)", SRID, targetLng, targetLat);

        BooleanExpression withinRadius = Expressions.booleanTemplate(
                "ST_DWithin({0}, ST_SetSRID(ST_MakePoint({1}, {2}), {3}), {4})",
                location.coordinate,
                targetLng,
                targetLat,
                SRID,
                radiusKm * METERS_PER_KM
        );

        return jpaQueryFactory.selectFrom(location)
                .where(withinRadius)
                .fetch();
    }

    // 반경 + 박스 필터 (ST_DWithin + ST_Envelope + toList optional 필터 제거 가능)
    @Override
    public List<Location> findNearby(double lat, double lon, double radiusKm) {
        QLocation location = QLocation.location;

        // POINT(lon lat) 형식만 남김
        String pointWKT = String.format("POINT(%f %f)", lon, lat);

        BooleanExpression withinRadius = Expressions.booleanTemplate(
                "ST_DWithin({0}, ST_SetSRID(ST_GeomFromText({1}), {2}), {3})",
                location.coordinate,
                pointWKT,
                SRID,
                radiusKm * METERS_PER_KM
        );

        return jpaQueryFactory.selectFrom(location)
                .where(withinRadius)
                .fetch();
    }


//    private final EntityManager em;
//    private final JPAQueryFactory jpaQueryFactory;
//
//    // 소숫점 비교용
//    private static final double EPSILON = 1e-6;
//
//    // 좌표 탐색
//    @Override
//    public List<Location> findLocationByCoordinate(Double lat, Double lng) {
//        QLocation location = QLocation.location;
//
//        return jpaQueryFactory
//                .selectFrom(location)
//                .where(
//                        location.latitude.between(lat - EPSILON, lat + EPSILON),
//                        location.longitude.between(lng - EPSILON, lng + EPSILON)
//                )
//                .fetch();
//    }
//    // 바운딩 박스
//    @Override
//    public List<Location> findWithinSquare(double lat, double lng, double radiusKm) {
//        QLocation location = QLocation.location;
//
//        double[] box = GeoUtil.getBoundingBox(lat, lng, radiusKm);
//        double minLat = box[0], maxLat = box[1];
//        double minLng = box[2], maxLng = box[3];
//
//        return jpaQueryFactory
//                .selectFrom(location)
//                .where(
//                        location.latitude.between(minLat, maxLat),
//                        location.longitude.between(minLng, maxLng)
//                )
//                .fetch();
//    }
//    // 하버사인 공식
//    @Override
//    public List<Location> findWithinRadius(double targetLat, double targetLng, double radiusInKm) {
//        QLocation location = QLocation.location;
//        final double R = 6371;
//
//        // acos 내부 수식 (모든 파라미터 명시)
//        NumberExpression<Double> acosInput = Expressions.numberTemplate(Double.class,
//                "LEAST(1.0, GREATEST(-1.0, cos(radians({0})) * cos(radians({1})) * cos(radians({2}) - radians({3})) + sin(radians({0})) * sin(radians({1}))))",
//                targetLat,
//                location.latitude,
//                targetLng,
//                location.longitude
//        );
//
//        // 거리 계산식
//        NumberExpression<Double> distance = Expressions.numberTemplate(Double.class,
//                "{0} * acos({1})",
//                R,
//                acosInput
//        );
//
//        // 거리 비교
//        BooleanExpression withinRadius = distance.loe(radiusInKm);
//
//        return jpaQueryFactory.selectFrom(location)
//                .where(withinRadius)
//                .fetch();
//    }
//
//
//    // 하버 사인 + 바운딩 박스
//    @Override
//    public List<Location> findNearby(double lat, double lon, double radiusKm) {
//        double[] box = GeoUtil.getBoundingBox(lat, lon, radiusKm);
//        double minLat = box[0], maxLat = box[1], minLon = box[2], maxLon = box[3];
//
//        QLocation location = QLocation.location;
//
//        return jpaQueryFactory.selectFrom(location)
//                .where(location.latitude.between(minLat, maxLat)
//                        .and(location.longitude.between(minLon, maxLon)))
//                .fetch()
//                .stream()
//                .filter(loc -> GeoUtil.haversine(lat, lon, loc.getLatitude(), loc.getLongitude()) <= radiusKm)
//                .toList();
//    }



}
