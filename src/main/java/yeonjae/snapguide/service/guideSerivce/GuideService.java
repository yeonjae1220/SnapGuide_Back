package yeonjae.snapguide.service.guideSerivce;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.controller.guideController.guideDto.GuideCreateTestDto;
import yeonjae.snapguide.controller.guideController.guideDto.GuideResponseDto;
import yeonjae.snapguide.domain.guide.Guide;
import yeonjae.snapguide.domain.guide.GuideMapper;
import yeonjae.snapguide.domain.guide.event.GuideDeletedEvent;
import yeonjae.snapguide.domain.location.Location;
import yeonjae.snapguide.domain.media.Media;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.repository.guideLikeRepository.GuideLikeRepository;
import yeonjae.snapguide.repository.guideRepository.GuideRepository;
import yeonjae.snapguide.repository.locationRepository.GeoUtil;
import yeonjae.snapguide.repository.locationRepository.LocationRepository;
import yeonjae.snapguide.repository.mediaRepository.MediaRepository;
import yeonjae.snapguide.repository.memberRepository.MemberRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GuideService {
    private final GuideRepository guideRepository;
    private final MemberRepository memberRepository;
    private final LocationRepository locationRepository;
    private final MediaRepository mediaRepository;
    private final GuideLikeRepository guideLikeRepository;
    private final GuideLikeService guideLikeService;
    private final ApplicationEventPublisher eventPublisher;
    private final RegionAggregateService regionAggregateService;

    /*
    가이드 생성하고
    팁 넣고
    location ,member 연결해야함
    사진 정보가 없으니 Location을 뽑아서 넣는건 불가.
    1. 사용자가 guide생성시 선택해서 넣기 => 매개변수로 받기
    2. media저장할 때 location이 존재한다면 연관관계 넣어주기. => 알단 이걸로
    3. 그냥 연관관계 끊고 media통해서 획득하기
    빌더 or 생성자

    media들의 위치 정보가 다른것들이 있다면?

    TODO : 추후 위치 정보 여러개 저장할 수 있게
    list<Long>으로 Location 위치 쭉 뽑아와서 저장해두면 될듯..?

    media들은 얘 작성하고, 아이디들 받아와서 얘한테 연결 시켜 주는게 좋을듯?

     */
    /**
     * Guide 생성 + Media 연결 통합 메서드 (권장)
     * DB 조회 최소화: Media 엔티티를 직접 받아서 처리
     */
    @Caching(evict = {
            @CacheEvict(value = "nearbyGuides", allEntries = true),
            @CacheEvict(value = "regionAggregate", allEntries = true)
    })
    public Long createGuideWithMedia(Member author, String tip, List<Media> mediaList, boolean locationPublic) {
        // 1. 전달받은 Media 엔티티는 이전 트랜잭션에서 저장 후 detached 상태임.
        //    현재 트랜잭션의 영속성 컨텍스트에서 재조회하여 managed 상태로 만든다.
        //    (Guide.mediaList의 CascadeType.ALL이 detached 엔티티에 PERSIST를 시도하는 문제 방지)
        List<Long> mediaIds = mediaList.stream().map(Media::getId).collect(Collectors.toList());
        List<Media> managedMedia = mediaIds.isEmpty() ? List.of() : mediaRepository.findAllById(mediaIds);

        // 2. 첫 번째 Media의 Location 사용 (없으면 null)
        Location location = managedMedia.stream()
                .map(Media::getLocation)
                .filter(loc -> loc != null)
                .findFirst()
                .orElse(null);

        // 3. Guide 생성
        Guide guide = Guide.builder()
                .tip(tip)
                .author(author)
                .location(location)
                .locationPublic(locationPublic)
                .build();

        guideRepository.save(guide);

        // 4. Media 연결 (managed 상태의 엔티티 사용)
        if (!managedMedia.isEmpty()) {
            linkMediaToGuide(guide, managedMedia);
        }

        log.info("[Guide] Created guide {} with {} media", guide.getId(), managedMedia.size());
        return guide.getId();
    }

    /**
     * @deprecated DTO 기반 생성은 추가 DB 조회 필요. createGuideWithMedia() 사용 권장
     */
    @Deprecated
    @Caching(evict = {
            @CacheEvict(value = "nearbyGuides", allEntries = true),
            @CacheEvict(value = "regionAggregate", allEntries = true)
    })
    public Long createGuide(GuideCreateTestDto guideCreateTestDto) {
        Member author = memberRepository.findById(guideCreateTestDto.getMemberId())
                .orElseThrow(() -> new EntityNotFoundException("member not found"));

        Location location = null;
        if (guideCreateTestDto.getLocationId() != null) {
            location = locationRepository.findById(guideCreateTestDto.getLocationId())
                    .orElseThrow(() -> new EntityNotFoundException("location not found"));
        }

        Guide guide = Guide.builder()
                .tip(guideCreateTestDto.getTip())
                .author(author)
                .location(location) // null도 가능
                .build();

        guideRepository.save(guide);
        return guide.getId();
    }

    /**
     * Media 엔티티를 Guide에 직접 연결 (ID 조회 없이)
     */
    public void linkMediaToGuide(Guide guide, List<Media> mediaList) {
        for (Media media : mediaList) {
            guide.assignGuide(media);
            media.assignMedia(guide);
        }
    }

    /**
     * @deprecated ID 기반 연결은 추가 DB 조회 필요. linkMediaToGuide(Guide, List<Media>) 사용 권장
     */
    @Deprecated
    public void linkMediaToGuide(Long guideId, List<Long> mediaIds) {
        Guide guide = guideRepository.findById(guideId)
                .orElseThrow(() -> new EntityNotFoundException("Guide not found"));
        List<Media> mediaList = mediaRepository.findAllById(mediaIds);

        linkMediaToGuide(guide, mediaList);
    }

    public List<GuideResponseDto> getMyGuides(Long memberId) {
        return guideRepository.findAllByMemberId(memberId);
    }

    @Caching(evict = {
            @CacheEvict(value = "nearbyGuides", allEntries = true),
            @CacheEvict(value = "regionAggregate", allEntries = true)
    })
    public GuideResponseDto updateTip(Long guideId, String newTip, @AuthenticationPrincipal UserDetails userDetails) {
        Guide guide = guideRepository.findByIdWithFetch(guideId)
                .orElseThrow(() -> new IllegalArgumentException("Guide not found"));

        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("사용자 정보를 찾을 수 없습니다."));

        if (!guide.getAuthor().getId().equals(member.getId())) {
            throw new AccessDeniedException("본인의 가이드만 수정할 수 있습니다.");
        }
        guide.updateTip(newTip);
//        guideRepository.save(guide); // 변경감지로 자동 반영되지만 save로 명시해도 OK
        // DTO로 변환해서 반환
//        return new GuideResponseDto(guide.getId(), guide.getTip());
        return GuideMapper.toResponseDto(guide, guideLikeService.hasUserLiked(member, guide));
    }

    /**
     * Guide 미디어 파일 삭제 이벤트 발행. (MemberService에서도 재사용)
     * 실제 파일 삭제는 DB 커밋 후 MediaFileCleanupListener가 처리.
     */
    public void deleteGuideMediaFiles(Guide guide) {
        publishMediaDeleteEvent(guide);
    }

    private void publishMediaDeleteEvent(Guide guide) {
        List<GuideDeletedEvent.MediaFileInfo> fileInfos = guide.getMediaList().stream()
                .map(m -> new GuideDeletedEvent.MediaFileInfo(
                        m.getOriginalKey(), m.getWebKey(), m.getThumbnailKey()))
                .toList();

        if (!fileInfos.isEmpty()) {
            log.info("[Guide] 파일 삭제 이벤트 발행: guide={}, 파일={}건", guide.getId(), fileInfos.size());
            eventPublisher.publishEvent(new GuideDeletedEvent(fileInfos));
        }
    }

    @Caching(evict = {
            @CacheEvict(value = "nearbyGuides", allEntries = true),
            @CacheEvict(value = "regionAggregate", allEntries = true)
    })
    public void deleteGuide(Long guideId, @AuthenticationPrincipal UserDetails userDetails) {
        Guide guide = guideRepository.findByIdWithFetch(guideId)
                .orElseThrow(() -> new IllegalArgumentException("Guide not found"));

        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("사용자 정보를 찾을 수 없습니다."));

        if (!guide.getAuthor().getId().equals(member.getId())) {
            throw new AccessDeniedException("본인의 가이드만 삭제할 수 있습니다.");
        }

        log.info("Starting deletion for guide: {}", guideId);

        // 파일 삭제 이벤트 발행 (DB 커밋 후 MediaFileCleanupListener가 처리)
        publishMediaDeleteEvent(guide);

        // DB에서 Guide 삭제 (Cascade로 연관 엔티티 자동 삭제)
        guideRepository.delete(guide);
        log.info("Guide deletion successful for: {}", guideId);
    }

    @Cacheable(
            value = "nearbyGuides",
            key = "T(Math).round(#lat * 100) / 100.0 + ':' + T(Math).round(#lng * 100) / 100.0 + ':' + #radius",
            unless = "#result.isEmpty()"
    )
    @Transactional(readOnly = true)
    public List<GuideResponseDto> findGuidesNear(double lat, double lng, double radius) { // km
        log.info("📍 [findGuidesNear] 요청 위치: lat = {}, lng = {}, radius = {} km", lat, lng, radius);

        // Bounding Box 계산 (km → degree)
        double[] box = GeoUtil.getBoundingBox(lat, lng, radius);
        double minLat = box[0], maxLat = box[1];
        double minLng = box[2], maxLng = box[3];

        // ST_DWithin용 radius를 degree로 변환 (geometry 타입이므로)
        double radiusInDegrees = GeoUtil.kmToDegrees(lat, radius);
        log.info("🔄 [findGuidesNear] radius 변환: {} km → {} degrees", radius, radiusInDegrees);

        List<Location> locations = locationRepository.findNearbyOptimized(lat, lng, radiusInDegrees,
                minLat, minLng, maxLat, maxLng);

        log.info("📌 [findNearbyOptimized] 반환된 Location 수: {}", locations.size());
        // NOTE : 과도한 로깅으로 오버헤드 발생하여 주석 처리 함
//        locations.forEach(loc ->
//                log.info("    ▸ Location ID = {}, 이름 = {}, 좌표 = {}",
//                        loc.getId(),
//                        loc.getLocationName(),
//                        loc.getCoordinate())
//        );

        // 위치 ID를 기준으로 가이드 찾기
        List<Long> locationIds = locations.stream()
                .map(Location::getId)
                .toList();

        log.info("🧭 조회할 Location ID 목록: {}", locationIds);

        if (locationIds.isEmpty()) {
            log.info("✅ 최종 반환 GuideDto 수: 0");
            return List.of();
        }

        List<Guide> guides = guideRepository.findByLocationIdInWithFetch(locationIds);
        log.info("📘 Guide 수: {} (Fetch Join 적용)", guides.size());
        guides.forEach(g ->
                log.debug("    ▸ Guide ID = {}, Tip = {}, Location ID = {}",
                        g.getId(),
                        g.getTip(),
                        g.getLocation() != null ? g.getLocation().getId() : null)
        );

//        List<GuideResponseDto> result = guides.stream()
//                .map(GuideResponseDto::fromEntity)
//                .toList();

        // 3. guides 목록을 DTO로 변환합니다.
        List<GuideResponseDto> result = guides.stream()
                .map(GuideMapper::toResponseDto)
                .toList();

        log.info("✅ 최종 반환 GuideDto 수: {}", result.size());
        return result;
    }

    // 게시글 상세 조회 (사용자 좋아요 정보 포함)
    @Transactional(readOnly = true)
    public GuideResponseDto findGuideById(Long guideId, UserDetails userDetails) {
        Guide guide = guideRepository.findByIdWithFetch(guideId)
                .orElseThrow(() -> new IllegalArgumentException("Guide not found"));

        boolean userHasLiked = false;
        if (userDetails != null) {
            Member member = memberRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("사용자 정보를 찾을 수 없습니다."));
            userHasLiked = guideLikeService.hasUserLiked(member, guide);
        }

        return GuideResponseDto.of(guide, userHasLiked);
    }


//    public List<GuideDistanceDto> distanceGuide(double lat, double lng) {
//
//    }


}
