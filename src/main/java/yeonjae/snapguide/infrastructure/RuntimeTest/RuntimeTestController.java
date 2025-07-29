package yeonjae.snapguide.infrastructure.RuntimeTest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yeonjae.snapguide.domain.location.Location;
import yeonjae.snapguide.infrastructure.aop.TimeTrace;
import yeonjae.snapguide.repository.locationRepository.LocationRepository;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/test")
@Slf4j
public class RuntimeTestController {
    private final RuntimeTestService runtimeTestService;

    @GetMapping("/locationCoordinateSearchTest")
    public void testExactCoordinateSearch() {
        runtimeTestService.testExactCoordinateSearch();
    }


    @GetMapping("/locationSquareSearchTest")
    public void testSquareSearch() {
        runtimeTestService.testSquareSearch();
    }

    @GetMapping("/locationRadiusSearchTest")
    public void testRadiusSearch() {
        runtimeTestService.testRadiusSearch();
    }

    @GetMapping("/locationOptimizedSearchTest")
    public void testOptimizedSearch() {
        runtimeTestService.testOptimizedSearch();
    }
}

