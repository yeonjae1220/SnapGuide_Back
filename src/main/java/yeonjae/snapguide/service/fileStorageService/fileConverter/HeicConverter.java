package yeonjae.snapguide.service.fileStorageService.fileConverter;

import lombok.extern.slf4j.Slf4j;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.im4java.process.Pipe;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

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

    /**
     * HEIC InputStream을 받아 JPG byte 배열로 변환하는 메서드 (메모리 내 처리)
     *
     * @param inputStream 원본 HEIC 파일의 InputStream
     * @return 변환된 JPG 데이터의 byte[]
     * @throws IOException 변환 실패 시 예외
     */
    public byte[] convertToJpgBytes(InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream jpgOutputStream = new ByteArrayOutputStream()) {
            // 1. ImageMagick 작업 설정 (표준 입력 -> 표준 출력)
            // 입력으로 '-'를 주면 표준 입력을 사용하겠다는 의미
            // 출력으로 'jpg:-'를 주면 JPG 포맷으로 표준 출력을 사용하겠다는 의미
            IMOperation op = new IMOperation();
            op.addImage("-");       // stdin
            op.addImage("jpg:-");   // stdout

            // 2. 입력 스트림(stdin)과 출력 스트림(stdout)을 위한 파이프 생성
            Pipe inputPipe = new Pipe(inputStream, null);
            Pipe outputPipe = new Pipe(null, jpgOutputStream);

            // 3. ConvertCmd 객체 생성 및 스트림 설정
            ConvertCmd convert = new ConvertCmd();
            convert.setInputProvider(inputPipe);
            convert.setOutputConsumer(outputPipe);

            // 4. 변환 실행
            convert.run(op);

            // 5. 결과 byte[] 반환
            return jpgOutputStream.toByteArray();

        } catch (Exception e) {
            log.warn("[HeicConverter] HEIC 스트림 변환 실패: {}", e.getMessage());
            throw new IOException("HEIC Stream → JPG byte[] 변환 실패: " + e.getMessage(), e);
        }
    }
}
