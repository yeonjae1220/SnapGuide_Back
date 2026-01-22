package yeonjae.snapguide.service.guideSerivce;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.controller.guideController.guideDto.GuideCreateTestDto;
import yeonjae.snapguide.controller.guideController.guideDto.GuideResponseDto;
import yeonjae.snapguide.domain.guide.Guide;
//import yeonjae.snapguide.domain.guide.GuideDistanceDto;
import yeonjae.snapguide.domain.like.GuideLike;
import yeonjae.snapguide.domain.location.Location;
import yeonjae.snapguide.domain.media.Media;
import yeonjae.snapguide.domain.media.MediaDto;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.domain.member.dto.MemberDto;
import yeonjae.snapguide.repository.guideLikeRepository.GuideLikeRepository;
import yeonjae.snapguide.repository.guideRepository.GuideRepository;
import yeonjae.snapguide.repository.locationRepository.GeoUtil;
import yeonjae.snapguide.repository.locationRepository.LocationRepository;
import yeonjae.snapguide.repository.mediaRepository.MediaRepository;
import yeonjae.snapguide.repository.memberRepository.MemberRepository;
import yeonjae.snapguide.service.fileStorageService.FileStorageService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
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

    private final FileStorageService fileStorageService;

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
    @CacheEvict(value = "nearbyGuides", allEntries = true)
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

    public void linkMediaToGuide(Long guideId, List<Long> mediaIds) {
        Guide guide = guideRepository.findById(guideId)
                .orElseThrow(() -> new EntityNotFoundException("Guide not found"));
        List<Media> mediaList = mediaRepository.findAllById(mediaIds);

        for (Media media : mediaList) {
            guide.assignGuide(media); // 양방향 관계 저장
            media.assignMedia(guide); // media ← guide 연결
        }
    }

    public List<GuideResponseDto> getMyGuides(Long memberId) {
        return guideRepository.findAllByMemberId(memberId);
    }

    @CacheEvict(value = "nearbyGuides", allEntries = true)
    public GuideResponseDto updateTip(Long guideId, String newTip, @AuthenticationPrincipal UserDetails userDetails) {
        Guide guide = guideRepository.findById(guideId)
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
        return GuideResponseDto.builder()
                .id(guide.getId())
                .tip(guide.getTip())
                .author(MemberDto.fromEntity(guide.getAuthor())) // NOTE : 이거 Member 조회에 성능 많이 먹는지 확인
                // NOTE : 아래 두개는 일단 보류.. locationName은 딱히 중요하지 않은 것 같고, media도 필요한가? 리소스만 쓰는거 같음
                .locationName("") //
                .media(guide.getMediaList().stream()
                        .map(media -> new MediaDto(media.getMediaName(), media.getMediaUrl()))
                        .toList())
                .build();
    }

    /**
     * Guide의 S3 미디어 파일들을 삭제합니다. (MemberService에서도 재사용)
     * @param guide 삭제할 미디어 파일들을 가진 Guide 엔티티
     */
    public void deleteGuideMediaFiles(Guide guide) {
        deleteMediaFilesFromStorage(guide);
    }

    /**
     * S3에서 Guide의 모든 미디어 파일 삭제 (private 헬퍼 메서드)
     * @param guide 삭제할 미디어 파일들을 가진 Guide 엔티티
     */
    private void deleteMediaFilesFromStorage(Guide guide) {
        log.info("Starting S3 file deletion for guide: {}", guide.getId());

        for (Media media : guide.getMediaList()) {
            try {
                fileStorageService.deleteFile(media.getOriginalKey());
                fileStorageService.deleteFile(media.getWebKey());
                fileStorageService.deleteFile(media.getThumbnailKey());
                log.info("S3 파일 삭제 성공: {}", media.getMediaName());
            } catch (IOException e) {
                log.error("S3 파일 삭제 실패: {}", media.getMediaUrl(), e);
                throw new RuntimeException("S3 파일 삭제 중 오류가 발생했습니다.");
            }
        }
    }

    @CacheEvict(value = "nearbyGuides", allEntries = true)
    public void deleteGuide(Long guideId, @AuthenticationPrincipal UserDetails userDetails) {
        Guide guide = guideRepository.findById(guideId)
                .orElseThrow(() -> new IllegalArgumentException("Guide not found"));

        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("사용자 정보를 찾을 수 없습니다."));

        if (!guide.getAuthor().getId().equals(member.getId())) {
            throw new AccessDeniedException("본인의 가이드만 삭제할 수 있습니다.");
        }

        log.info("Starting deletion for guide: {}", guideId);

        // S3 파일 삭제 (추출한 메서드 호출)
        deleteMediaFilesFromStorage(guide);

        // DB에서 Guide 삭제
        guideRepository.delete(guide);
        log.info("Guide deletion successful for: {}", guideId);
    }

    @Cacheable(
            value = "nearbyGuides",
            key = "T(Math).round(#lat * 100) / 100.0 + ':' + T(Math).round(#lng * 100) / 100.0 + ':' + #radius",
            unless = "#result.isEmpty()"
    )
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

        List<Guide> guides = guideRepository.findByLocationIdInWithFetch(locationIds);
        log.info("📘 Guide 수: {} (Fetch Join 적용)", guides.size());
        guides.forEach(g ->
                log.info("    ▸ Guide ID = {}, Tip = {}, Location ID = {}",
                        g.getId(),
                        g.getTip(),
                        g.getLocation().getId())
        );

//        List<GuideResponseDto> result = guides.stream()
//                .map(GuideResponseDto::fromEntity)
//                .toList();

        // 3. guides 목록을 DTO로 변환합니다.
        List<GuideResponseDto> result = guides.stream()
                .map(guide -> {
                    // 미디어 리스트 변환
                    List<MediaDto> mediaDto = guide.getMediaList()
                            .stream()
                            .map(MediaDto::fromEntity)
                            .toList();

                    // 5. Builder를 사용해 모든 정보를 담아 DTO를 생성합니다.
                    return GuideResponseDto.builder()
                            .id(guide.getId())
                            .tip(guide.getTip())
                            .author(MemberDto.fromEntity(guide.getAuthor())) // 이 부분이 id만 가져오는게 아니라 객체 전체 가져오는거라 LazyLoading
                            .locationName(guide.getLocation().getLocationName())
                            .media(mediaDto)
                            .likeCount(guide.getLikeCount())
                            .build();
                })
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

        // 인증된 사용자인 경우에만 좋아요 정보 조회
        if (userDetails != null) {
            Member member = memberRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("사용자 정보를 찾을 수 없습니다."));
            userHasLiked = guideLikeRepository.findByMemberAndGuide(member, guide).isPresent();
        }

        return GuideResponseDto.of(guide, userHasLiked);
    }



    @Transactional
    public boolean toggleLike(Long guideId, @AuthenticationPrincipal UserDetails userDetails) {
        // 인증되지 않은 사용자 체크
        if (userDetails == null) {
            throw new IllegalArgumentException("로그인이 필요한 서비스입니다.");
        }

        Guide guide = findGuide(guideId);
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("사용자 정보를 찾을 수 없습니다."));

        Optional<GuideLike> like = guideLikeRepository.findByMemberAndGuide(member, guide);

        if (like.isPresent()) {
            // 좋아요가 이미 존재하면 -> 좋아요 취소
            guideLikeRepository.delete(like.get());
            guide.decreaseLikeCount();
            return false; // 좋아요 취소됨
        } else {
            // 좋아요가 없으면 -> 좋아요 추가
            guideLikeRepository.save(new GuideLike(member, guide));
            guide.increaseLikeCount();
            return true; // 좋아요 추가됨
        }
    }

    // 중복 코드를 줄이기 위한 private 메서드
    private Guide findGuide(Long guideId) {
        return guideRepository.findById(guideId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + guideId));
    }


//    public List<GuideDistanceDto> distanceGuide(double lat, double lng) {
//
//    }


}
