package yeonjae.snapguide.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import yeonjae.snapguide.service.MediaService;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/media")
@Slf4j
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadPhoto(@RequestParam("file") MultipartFile file) throws IOException {
        String mediaId = mediaService.saveMedia(file);
        return ResponseEntity.ok("Saved: " + mediaId);
    }
}
