package yeonjae.snapguide.service.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.controller.admin.dto.*;
import yeonjae.snapguide.domain.location.Location;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.exception.CustomException;
import yeonjae.snapguide.exception.ErrorCode;
import yeonjae.snapguide.repository.CommentRepository;
import yeonjae.snapguide.repository.guideRepository.GuideRepository;
import yeonjae.snapguide.repository.locationRepository.LocationRepository;
import yeonjae.snapguide.repository.memberRepository.MemberRepository;
import yeonjae.snapguide.service.ReverseGeocodingService;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminService {

    private final MemberRepository memberRepository;
    private final GuideRepository guideRepository;
    private final LocationRepository locationRepository;
    private final CommentRepository commentRepository;
    private final ReverseGeocodingService reverseGeocodingService;

    public Page<AdminMemberResponse> getMembers(Pageable pageable) {
        return memberRepository.findAll(pageable)
                .map(AdminMemberResponse::of);
    }

    @Transactional
    public AdminMemberResponse updateMemberRole(Long memberId, AdminRoleUpdateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        member.updateAuthority(request.getAuthority());
        return AdminMemberResponse.of(member);
    }

    public AdminStatsResponse getStats() {
        return AdminStatsResponse.builder()
                .totalMembers(memberRepository.count())
                .totalGuides(guideRepository.count())
                .build();
    }

    public Page<AdminGuideResponse> getGuides(Pageable pageable) {
        return guideRepository.findAll(pageable)
                .map(AdminGuideResponse::of);
    }

    @Transactional
    public void deleteGuide(Long id) {
        if (!guideRepository.existsById(id)) {
            throw new CustomException(ErrorCode.GUIDE_NOT_FOUND);
        }
        guideRepository.deleteById(id);
    }

    public Page<AdminLocationResponse> getLocations(Pageable pageable) {
        return locationRepository.findAll(pageable)
                .map(AdminLocationResponse::of);
    }

    @Transactional
    public void deleteLocation(Long id) {
        if (!locationRepository.existsById(id)) {
            throw new CustomException(ErrorCode.LOCATION_NOT_FOUND);
        }
        locationRepository.deleteById(id);
    }

    public Page<AdminCommentResponse> getComments(Pageable pageable) {
        return commentRepository.findAll(pageable)
                .map(AdminCommentResponse::of);
    }

    @Transactional
    public void deleteComment(Long id) {
        if (!commentRepository.existsById(id)) {
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUND);
        }
        commentRepository.deleteById(id);
    }

    // 좌표만 있고 행정구역 정보 없는 Location 레코드 일괄 재지오코딩
    public int migrateCoordinateOnlyLocations() {
        List<Location> stale = locationRepository.findCoordinateOnlyLocations();
        log.info("[Admin] migrateCoordinateOnlyLocations: {} records to process", stale.size());

        int success = 0;
        for (Location loc : stale) {
            try {
                double lat = loc.getCoordinate().getY();
                double lng = loc.getCoordinate().getX();
                Location fresh = reverseGeocodingService.reverseGeocode(lat, lng).block();
                if (fresh != null) {
                    Location updated = loc.toBuilder()
                            .locationName(fresh.getLocationName())
                            .formattedAddress(fresh.getFormattedAddress())
                            .country(fresh.getCountry())
                            .countryCode(fresh.getCountryCode())
                            .region(fresh.getRegion())
                            .city(fresh.getCity())
                            .subRegion(fresh.getSubRegion())
                            .district(fresh.getDistrict())
                            .street(fresh.getStreet())
                            .streetNumber(fresh.getStreetNumber())
                            .buildingName(fresh.getBuildingName())
                            .subPremise(fresh.getSubPremise())
                            .postalCode(fresh.getPostalCode())
                            .rawJson(fresh.getRawJson())
                            .provider(fresh.getProvider())
                            .build();
                    locationRepository.save(updated);
                    success++;
                    log.info("[Admin] Updated location id={} → {}", loc.getId(), updated.getLocationName());
                }
                Thread.sleep(200); // Google API 레이트 리밋 방지
            } catch (Exception e) {
                log.warn("[Admin] Failed to update location id={}: {}", loc.getId(), e.getMessage());
            }
        }
        log.info("[Admin] migrateCoordinateOnlyLocations done: {}/{} updated", success, stale.size());
        return success;
    }
}
