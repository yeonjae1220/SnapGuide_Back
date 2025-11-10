package yeonjae.snapguide.controller.locationController;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import yeonjae.snapguide.controller.locationController.locationDto.LocationRequestDto;
import yeonjae.snapguide.domain.location.Location;
import yeonjae.snapguide.service.locationSerivce.LocationServiceGeoImpl;
import yeonjae.snapguide.service.util.googleMapApiDto.PlaceAutocompleteService;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * LocationController 단위 테스트
 * 위치 저장 및 Google Places API 프록시 기능 테스트
 */
@WebMvcTest(LocationController.class)
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LocationServiceGeoImpl locationServiceGeoImpl;

    @MockBean
    private PlaceAutocompleteService placeAutocompleteService;

    private LocationRequestDto locationRequest;
    private Location mockLocation;

    @BeforeEach
    void setUp() {
        locationRequest = new LocationRequestDto();
        locationRequest.setLatitude(37.5665);
        locationRequest.setLongitude(126.9780);

        mockLocation = Location.builder()
                .id(1L)
                .locationName("Seoul City Hall")
                .build();
    }

    @Test
    @DisplayName("POST /location/api/upload - 위치 저장 성공")
    void saveLocation_Success() throws Exception {
        // given
        given(locationServiceGeoImpl.saveLocation(anyDouble(), anyDouble()))
                .willReturn(mockLocation);

        // when & then
        mockMvc.perform(post("/location/api/upload")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(locationRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("위치 저장 완료"));

        verify(locationServiceGeoImpl, times(1)).saveLocation(37.5665, 126.9780);
    }

    @Test
    @DisplayName("GET /location/api/places/autocomplete - 장소 자동완성 성공")
    void getPlaceAutocomplete_Success() throws Exception {
        // given
        String input = "Seoul";
        String mockResponse = "{\"predictions\": [{\"description\": \"Seoul, South Korea\"}], \"status\": \"OK\"}";

        given(placeAutocompleteService.getPlaceAutocomplete(input))
                .willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/location/api/places/autocomplete")
                        .param("input", input))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(mockResponse));

        verify(placeAutocompleteService, times(1)).getPlaceAutocomplete(input);
    }

    @Test
    @DisplayName("GET /location/api/places/autocomplete - API 호출 실패 시 안전한 응답")
    void getPlaceAutocomplete_ApiFailure_ReturnsSafeResponse() throws Exception {
        // given
        String input = "Seoul";
        String safeResponse = "{\"predictions\": [], \"status\": \"REQUEST_DENIED\"}";

        given(placeAutocompleteService.getPlaceAutocomplete(input))
                .willThrow(new RuntimeException("Google API Error"));

        // when & then
        mockMvc.perform(get("/location/api/places/autocomplete")
                        .param("input", input))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(safeResponse));

        verify(placeAutocompleteService, times(1)).getPlaceAutocomplete(input);
    }

    @Test
    @DisplayName("GET /location/api/places/details - 장소 상세 조회 성공")
    void getPlaceDetails_Success() throws Exception {
        // given
        String placeId = "ChIJwULG5WSODTkRbw2SigKRvcM";
        String mockResponse = "{\"result\": {\"name\": \"Seoul\", \"formatted_address\": \"Seoul, South Korea\"}, \"status\": \"OK\"}";

        given(placeAutocompleteService.getPlaceDetails(placeId))
                .willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/location/api/places/details")
                        .param("placeId", placeId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(mockResponse));

        verify(placeAutocompleteService, times(1)).getPlaceDetails(placeId);
    }

    @Test
    @DisplayName("GET /location/api/places/details - API 호출 실패 시 안전한 응답")
    void getPlaceDetails_ApiFailure_ReturnsSafeResponse() throws Exception {
        // given
        String placeId = "invalid-place-id";
        String safeResponse = "{\"result\": {}, \"status\": \"REQUEST_DENIED\"}";

        given(placeAutocompleteService.getPlaceDetails(placeId))
                .willThrow(new RuntimeException("Google API Error"));

        // when & then
        mockMvc.perform(get("/location/api/places/details")
                        .param("placeId", placeId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(safeResponse));

        verify(placeAutocompleteService, times(1)).getPlaceDetails(placeId);
    }

    @Test
    @DisplayName("GET /location/api/places/autocomplete - 빈 input으로 검색")
    void getPlaceAutocomplete_EmptyInput() throws Exception {
        // given
        String input = "";
        String mockResponse = "{\"predictions\": [], \"status\": \"ZERO_RESULTS\"}";

        given(placeAutocompleteService.getPlaceAutocomplete(input))
                .willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/location/api/places/autocomplete")
                        .param("input", input))
                .andDo(print())
                .andExpect(status().isOk());

        verify(placeAutocompleteService, times(1)).getPlaceAutocomplete(input);
    }
}
