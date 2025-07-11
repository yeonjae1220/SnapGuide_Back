package yeonjae.snapguide.service.fileStorageService.fileConverter;

import lombok.extern.slf4j.Slf4j;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;

import java.io.File;
import java.io.IOException;
@Slf4j
public class HeicConverter {
    /**
     * HEIC 파일을 JPG로 변환하는 메서드
     *
     * @param inputFile 원본 HEIC 파일
     * @param outputDir 변환된 JPG 저장 경로 (예: /converted/)
     * @return 변환된 JPG 파일
     * @throws Exception 변환 실패 시 예외
     */
    public File convertHeicToJpg(File inputFile, String outputDir) throws IOException {
        try {
            // 기본 이름 구성
            String baseName = inputFile.getName().replaceFirst("[.][^.]+$", "");
            File outputFile = new File(outputDir, baseName + ".jpg");

            // ImageMagick 명령어 구성
            IMOperation op = new IMOperation();
            op.addImage(inputFile.getAbsolutePath());
            op.addImage(outputFile.getAbsolutePath());

            ConvertCmd convert = new ConvertCmd();
            convert.run(op);

            return outputFile;

        } catch (Exception e) {
            log.warn("[HeicConverter] HEIC 변환 실패: {}", e.getMessage());
            throw new IOException("HEIC → JPG 변환 실패: " + e.getMessage(), e);
        }
    }
}
