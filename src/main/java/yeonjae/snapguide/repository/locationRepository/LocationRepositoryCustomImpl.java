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

    // 소숫점 비교용
    private static final double EPSILON = 1e-6;

    // 좌표 탐색
    @Override
    public List<Location> findLocationByCoordinate(Double lat, Double lng) {
        QLocation location = QLocation.location;

        return jpaQueryFactory
                .selectFrom(location)
                .where(
                        location.latitude.between(lat - EPSILON, lat + EPSILON),
                        location.longitude.between(lng - EPSILON, lng + EPSILON)
                )
                .fetch();
    }
    // 바운딩 박스
    @Override
    public List<Location> findWithinSquare(double lat, double lng, double radiusKm) {
        QLocation location = QLocation.location;

        double[] box = GeoUtil.getBoundingBox(lat, lng, radiusKm);
        double minLat = box[0], maxLat = box[1];
        double minLng = box[2], maxLng = box[3];

        return jpaQueryFactory
                .selectFrom(location)
                .where(
                        location.latitude.between(minLat, maxLat),
                        location.longitude.between(minLng, maxLng)
                )
                .fetch();
    }
    // 하버사인 공식
    @Override
    public List<Location> findWithinRadius(double targetLat, double targetLng, double radiusInKm) {
        QLocation location = QLocation.location;
        final double R = 6371;

        // acos 내부 수식 (모든 파라미터 명시)
        NumberExpression<Double> acosInput = Expressions.numberTemplate(Double.class,
                "LEAST(1.0, GREATEST(-1.0, cos(radians({0})) * cos(radians({1})) * cos(radians({2}) - radians({3})) + sin(radians({0})) * sin(radians({1}))))",
                targetLat,
                location.latitude,
                targetLng,
                location.longitude
        );

        // 거리 계산식
        NumberExpression<Double> distance = Expressions.numberTemplate(Double.class,
                "{0} * acos({1})",
                R,
                acosInput
        );

        // 거리 비교
        BooleanExpression withinRadius = distance.loe(radiusInKm);

        return jpaQueryFactory.selectFrom(location)
                .where(withinRadius)
                .fetch();
    }


    // 하버 사인 + 바운딩 박스
    @Override
    public List<Location> findNearby(double lat, double lon, double radiusKm) {
        double[] box = GeoUtil.getBoundingBox(lat, lon, radiusKm);
        double minLat = box[0], maxLat = box[1], minLon = box[2], maxLon = box[3];

        QLocation location = QLocation.location;

        return jpaQueryFactory.selectFrom(location)
                .where(location.latitude.between(minLat, maxLat)
                        .and(location.longitude.between(minLon, maxLon)))
                .fetch()
                .stream()
                .filter(loc -> GeoUtil.haversine(lat, lon, loc.getLatitude(), loc.getLongitude()) <= radiusKm)
                .toList();
    }



}
