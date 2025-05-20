package yeonjae.snapguide.controller.mediaController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import yeonjae.snapguide.service.mediaSerivce.MediaService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/media")
@Slf4j
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadPhoto(@RequestParam("file") MultipartFile file) throws IOException {
        Long mediaId = mediaService.saveMedia(file);
        return ResponseEntity.ok("Saved: " + mediaId);
    }

    /**
     * NOTE : 클라이언트 측에서 <input type="file" name="file" multiple> 이런식으로 넣어야할지?
     */
    @PostMapping("/api/upload")
    public List<Long> uploadMedia(@RequestParam MultipartFile[] files) throws IOException {
        return Arrays.stream(files)
                .map(file -> {
                    try {
                        return mediaService.saveMedia(file);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }
}
