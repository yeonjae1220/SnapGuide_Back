package yeonjae.snapguide.service.guideSerivce;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.controller.guideController.guideDto.RegionClusterDto;
import yeonjae.snapguide.repository.guideRepository.GuideAggregateRow;
import yeonjae.snapguide.repository.guideRepository.GuideRepository;
import yeonjae.snapguide.repository.guideRepository.MediaThumbnailRow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 지도 축소 시 국가/대륙 단위 가이드 집계.
 * 결과는 Redis에 캐싱되며 가이드 CRUD 시 nearbyGuides와 함께 무효화된다.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RegionAggregateService {

    private final GuideRepository guideRepository;

    @Cacheable(value = "regionAggregate", key = "#level.name()")
    public List<RegionClusterDto> aggregate(AggregateLevel level) {
        log.info("[RegionAggregate] level={}", level);

        List<GuideAggregateRow> rows = guideRepository.findAllForAggregation();

        // --- grouping ---
        // key → {sumLat, sumLng, count, topGuideId (max likeCount), topLikeCount}
        record GroupAccum(double sumLat, double sumLng, long count, Long topGuideId, int topLikeCount, String label) {}
        Map<String, GroupAccum> groups = new HashMap<>();

        for (GuideAggregateRow row : rows) {
            if (row.getLat() == null || row.getLng() == null) continue;

            String key;
            String label;
            if (level == AggregateLevel.COUNTRY) {
                key   = row.getCountryCode();
                label = row.getCountry() != null ? row.getCountry() : row.getCountryCode();
            } else {
                key   = ContinentResolver.continentCode(row.getCountryCode());
                label = ContinentResolver.continentName(key);
            }

            int like = row.getLikeCount() != null ? row.getLikeCount() : 0;
            GroupAccum prev = groups.get(key);
            if (prev == null) {
                groups.put(key, new GroupAccum(row.getLat(), row.getLng(), 1, row.getGuideId(), like, label));
            } else {
                Long topId = like > prev.topLikeCount() ? row.getGuideId() : prev.topGuideId();
                int topLike = Math.max(like, prev.topLikeCount());
                groups.put(key, new GroupAccum(
                        prev.sumLat() + row.getLat(),
                        prev.sumLng() + row.getLng(),
                        prev.count() + 1,
                        topId, topLike, prev.label()
                ));
            }
        }

        // --- thumbnail: one query for all top guide IDs ---
        List<Long> topGuideIds = groups.values().stream()
                .map(GroupAccum::topGuideId)
                .filter(id -> id != null)
                .toList();

        Map<Long, String> thumbnailByGuideId = new HashMap<>();
        if (!topGuideIds.isEmpty()) {
            List<MediaThumbnailRow> thumbRows = guideRepository.findFirstMediaUrlsByGuideIds(topGuideIds);
            for (MediaThumbnailRow r : thumbRows) {
                if (r.getGuideId() != null) {
                    thumbnailByGuideId.put(r.getGuideId(), r.getMediaUrl());
                }
            }
        }

        // --- build DTOs ---
        List<RegionClusterDto> result = new ArrayList<>();
        for (Map.Entry<String, GroupAccum> e : groups.entrySet()) {
            String key   = e.getKey();
            GroupAccum g = e.getValue();
            double lat = g.sumLat() / g.count();
            double lng = g.sumLng() / g.count();
            String thumb = g.topGuideId() != null ? thumbnailByGuideId.get(g.topGuideId()) : null;
            result.add(new RegionClusterDto(key, g.label(), lat, lng, g.count(), thumb));
        }

        log.info("[RegionAggregate] level={} → {} clusters", level, result.size());
        return result;
    }
}
