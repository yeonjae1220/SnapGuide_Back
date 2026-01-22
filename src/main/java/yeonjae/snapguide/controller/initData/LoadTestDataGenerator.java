package yeonjae.snapguide.controller.initData;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * k6 부하테스트용 대량 테스트 데이터를 CSV 파일로 생성하는 유틸리티
 *
 * 실행 방법:
 * 1. IDE에서 main() 메서드 직접 실행
 * 2. 생성된 CSV를 PostgreSQL COPY 명령어로 DB에 삽입
 * 3. k6/data/ 디렉토리의 CSV를 k6 테스트에서 활용
 *
 * 생성되는 데이터:
 * - 회원: 10,000명
 * - 카메라 모델: 50종
 * - 가이드: 50,000개
 * - 미디어: 150,000개 (가이드당 평균 3장)
 * - 메타데이터: 150,000개 (미디어와 1:1)
 * - 댓글: 100,000개
 * - Location: 기존 DB 데이터 재사용 (koreaTourSpot.json) -> 기존 db에 저장되어 있던 정보 사용하고 싶은데
 */
@Slf4j
public class LoadTestDataGenerator {

    // 생성할 데이터 규모 설정
    private static final int MEMBER_COUNT = 10000;
    private static final int CAMERA_MODEL_COUNT = 50;
    private static final int GUIDE_COUNT = 50000;
    private static final int AVG_MEDIA_PER_GUIDE = 3; // 가이드당 평균 미디어 개수
    private static final int COMMENT_COUNT = 100000;

    // 출력 경로
    private static final String OUTPUT_DIR = "src/main/resources/load-test-data";
    private static final String K6_DATA_DIR = "k6/data";
    private static final String LOCATION_IDS_FILE = "src/main/resources/load-test-data/location_ids.csv";

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Random random = new Random(42); // 고정 시드로 재현 가능

    // DB에서 가져온 실제 Location ID 목록
    private static List<Integer> availableLocationIds = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        log.info("🚀 부하테스트용 CSV 데이터 생성 시작...");

        long startTime = System.currentTimeMillis();

        // 디렉토리 생성
        createDirectories();

        // Location ID 로드 (DB에서 추출한 실제 ID 사용)
        loadLocationIds();

        log.info("📊 생성 규모:");
        log.info("   - 회원: {}명", MEMBER_COUNT);
        log.info("   - 카메라 모델: {}종", CAMERA_MODEL_COUNT);
        log.info("   - 가이드: {}개", GUIDE_COUNT);
        log.info("   - 미디어: 약 {}개", GUIDE_COUNT * AVG_MEDIA_PER_GUIDE);
        log.info("   - 댓글: {}개", COMMENT_COUNT);
        log.info("   - 사용 가능한 Location: {}개", availableLocationIds.size());

        // CSV 파일 생성
        List<MemberCsv> members = generateMembers();
        List<CameraModelCsv> cameraModels = generateCameraModels();
        List<GuideCsv> guides = generateGuides(members);
        List<MediaCsv> medias = generateMedias(guides);
        List<MediaMetaDataCsv> metadatas = generateMediaMetaDatas(cameraModels, medias);
        List<CommentCsv> comments = generateComments(members, guides);

        // CSV 파일 쓰기
        writeMembersCsv(members);
        writeCameraModelsCsv(cameraModels);
        writeGuidesCsv(guides);
        writeMediasCsv(medias);
        writeMediaMetaDatasCsv(metadatas);
        writeCommentsCsv(comments);

        // member_authority 테이블용 CSV (ElementCollection)
        writeMemberAuthorityCsv(members);

        // k6 디렉토리로 복사
        copyToK6Directory();

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("✅ CSV 생성 완료! 소요 시간: {}ms", elapsed);
        log.info("📁 파일 위치: {}", OUTPUT_DIR);
        log.info("📁 k6 파일 위치: {}", K6_DATA_DIR);
        log.info("\n다음 단계:");
        log.info("  1. PostgreSQL 컨테이너에 CSV 파일 복사");
        log.info("  2. import.sql 실행하여 COPY 명령으로 데이터 로드");
        log.info("  3. k6 테스트 실행");
    }

    private static void createDirectories() throws IOException {
        Files.createDirectories(Paths.get(OUTPUT_DIR));
        Files.createDirectories(Paths.get(K6_DATA_DIR));
    }

    /**
     * DB에서 추출한 Location ID 목록 로드
     * location_ids.csv가 없으면 기본값(1~1000) 사용
     */
    private static void loadLocationIds() throws IOException {
        Path locationIdsPath = Paths.get(LOCATION_IDS_FILE);

        if (Files.exists(locationIdsPath)) {
            log.info("📍 Location ID 파일 로드 중: {}", LOCATION_IDS_FILE);
            List<String> lines = Files.readAllLines(locationIdsPath);

            // 첫 줄은 헤더이므로 skip
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (!line.isEmpty()) {
                    try {
                        availableLocationIds.add(Integer.parseInt(line));
                    } catch (NumberFormatException e) {
                        log.warn("⚠️  잘못된 Location ID: {}", line);
                    }
                }
            }

            log.info("✅ Location ID {}개 로드 완료", availableLocationIds.size());
        } else {
            // location_ids.csv가 없으면 기본값 사용 (1~1000)
            log.warn("⚠️  location_ids.csv가 없습니다. 기본값(1~1000) 사용");
            log.warn("   실제 DB의 Location ID를 사용하려면:");
            log.warn("   1. docker exec -it snapguide-postgres psql -U postgres -d snapguidedb");
            log.warn("   2. \\i /tmp/export-location-ids.sql");
            log.warn("   3. docker cp snapguide-postgres:/tmp/location_ids.csv {}", LOCATION_IDS_FILE);

            for (int i = 1; i <= 1000; i++) {
                availableLocationIds.add(i);
            }
        }

        if (availableLocationIds.isEmpty()) {
            throw new IllegalStateException("사용 가능한 Location ID가 없습니다!");
        }
    }

    /**
     * 회원 데이터 생성
     */
    private static List<MemberCsv> generateMembers() {
        log.info("👤 회원 데이터 생성 중...");
        List<MemberCsv> members = new ArrayList<>();
        String hashedPassword = passwordEncoder.encode("test1234");
        LocalDateTime now = LocalDateTime.now();

        for (int i = 1; i <= MEMBER_COUNT; i++) {
            members.add(new MemberCsv(
                i,
                "loadtest" + i + "@example.com",
                hashedPassword,
                "테스터" + i,
                "LOCAL",
                null,
                now.minusDays(random.nextInt(365)),
                now
            ));

            if (i % 1000 == 0) {
                log.info("  - {} / {} 생성 완료", i, MEMBER_COUNT);
            }
        }

        log.info("✅ 회원 {}명 생성 완료", members.size());
        return members;
    }

    /**
     * 카메라 모델 데이터 생성
     */
    private static List<CameraModelCsv> generateCameraModels() {
        log.info("📷 카메라 모델 데이터 생성 중...");
        List<CameraModelCsv> models = new ArrayList<>();

        String[][] cameraData = {
            {"Apple", "iPhone 14 Pro", "Built-in"},
            {"Apple", "iPhone 13", "Built-in"},
            {"Apple", "iPhone 12 Pro Max", "Built-in"},
            {"Samsung", "Galaxy S23 Ultra", "Built-in"},
            {"Samsung", "Galaxy S22", "Built-in"},
            {"Canon", "EOS R5", "RF 24-70mm f/2.8L"},
            {"Canon", "EOS 5D Mark IV", "EF 24-70mm f/2.8L II"},
            {"Nikon", "Z9", "NIKKOR Z 24-70mm f/2.8 S"},
            {"Nikon", "D850", "AF-S 24-70mm f/2.8E"},
            {"Sony", "A7R V", "FE 24-70mm f/2.8 GM II"},
            {"Sony", "A7 IV", "FE 24-70mm f/2.8 GM"},
            {"Fujifilm", "X-T5", "XF 23mm f/1.4"},
            {"Fujifilm", "X-H2S", "XF 16-55mm f/2.8"},
            {"Google", "Pixel 7 Pro", "Built-in"},
            {"Google", "Pixel 8", "Built-in"},
        };

        int id = 1;
        // 기본 카메라 모델들
        for (String[] data : cameraData) {
            models.add(new CameraModelCsv(id++, data[0], data[1], data[2]));
        }

        // 추가 랜덤 모델 (50개까지)
        while (models.size() < CAMERA_MODEL_COUNT) {
            String manufacturer = cameraData[random.nextInt(cameraData.length)][0];
            models.add(new CameraModelCsv(
                id++,
                manufacturer,
                manufacturer + " Model-" + id,
                "Standard Lens"
            ));
        }

        log.info("✅ 카메라 모델 {}종 생성 완료", models.size());
        return models;
    }

    /**
     * 가이드 데이터 생성
     */
    private static List<GuideCsv> generateGuides(List<MemberCsv> members) {
        log.info("📍 가이드 데이터 생성 중...");
        List<GuideCsv> guides = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        String[] tips = {
            "이곳은 정말 아름다운 곳입니다. 꼭 방문해보세요!",
            "야경이 멋진 장소예요. 저녁 시간에 가시는 걸 추천합니다.",
            "가족과 함께 가기 좋은 곳입니다.",
            "사진 찍기 좋은 포토존이 많아요.",
            "현지 음식이 정말 맛있었어요!",
            "주말에는 사람이 많으니 평일 방문을 추천합니다.",
            "입장료가 없어서 부담 없이 갈 수 있어요.",
            "주차 공간이 넉넉합니다.",
            "아이들이 정말 좋아했어요!",
            "조용하고 평화로운 분위기가 좋았습니다.",
            "일몰 시간에 가면 정말 환상적입니다.",
            "근처에 맛집도 많아서 좋아요.",
            "비 오는 날에도 즐길 수 있는 실내 공간이 있어요.",
            "반려동물 동반 가능한 곳입니다.",
            "무료 와이파이가 잘 터져요!"
        };

        for (int i = 1; i <= GUIDE_COUNT; i++) {
            MemberCsv randomMember = members.get(random.nextInt(members.size()));
            // 실제 존재하는 Location ID 중에서 랜덤 선택
            int locationId = availableLocationIds.get(random.nextInt(availableLocationIds.size()));
            String tip = tips[random.nextInt(tips.length)];
            int likeCount = random.nextInt(101);

            guides.add(new GuideCsv(
                i,
                tip,
                randomMember.id,
                locationId,
                likeCount,
                now.minusDays(random.nextInt(365)),
                now
            ));

            if (i % 5000 == 0) {
                log.info("  - {} / {} 생성 완료", i, GUIDE_COUNT);
            }
        }

        log.info("✅ 가이드 {}개 생성 완료", guides.size());
        return guides;
    }

    /**
     * 미디어 데이터 생성
     * 각 가이드당 1~5장의 미디어 생성
     */
    private static List<MediaCsv> generateMedias(List<GuideCsv> guides) {
        log.info("🖼️ 미디어 데이터 생성 중...");
        List<MediaCsv> medias = new ArrayList<>();
        int mediaId = 1;

        for (GuideCsv guide : guides) {
            // 가이드당 1~5장 랜덤
            int mediaCount = random.nextInt(5) + 1;

            for (int j = 0; j < mediaCount; j++) {
                int locationId = guide.locationId; // 가이드의 위치 재사용

                // 가상의 S3 경로 생성
                String basePath = String.format("/uploads/guide_%d/media_%d", guide.id, mediaId);
                String originalKey = basePath + "_original.jpg";
                String webKey = basePath + "_web.jpg";
                String thumbnailKey = basePath + "_thumb.jpg";

                medias.add(new MediaCsv(
                    mediaId,
                    "photo_" + mediaId + ".jpg",
                    webKey, // mediaUrl
                    originalKey,
                    webKey,
                    thumbnailKey,
                    random.nextInt(5000000) + 500000, // 500KB ~ 5.5MB
                    guide.id,
                    mediaId, // metadata_id (1:1 관계)
                    locationId
                ));

                mediaId++;
            }

            if (guide.id % 5000 == 0) {
                log.info("  - 가이드 {} / {} 처리 완료 (총 미디어: {}개)", guide.id, GUIDE_COUNT, medias.size());
            }
        }

        log.info("✅ 미디어 {}개 생성 완료", medias.size());
        return medias;
    }

    /**
     * 미디어 메타데이터 생성
     * 각 미디어와 1:1 관계
     */
    private static List<MediaMetaDataCsv> generateMediaMetaDatas(
        List<CameraModelCsv> cameraModels,
        List<MediaCsv> medias
    ) {
        log.info("📊 미디어 메타데이터 생성 중...");
        List<MediaMetaDataCsv> metadatas = new ArrayList<>();

        String[] shutterSpeeds = {"1/1000", "1/500", "1/250", "1/125", "1/60", "1/30"};
        String[] apertures = {"f/1.4", "f/1.8", "f/2.8", "f/4.0", "f/5.6", "f/8.0"};
        String[] whiteBalances = {"AUTO", "MANUAL"};
        String[] flashModes = {"NO_FLASH", "FLASH_FIRED", "AUTO_FLASH_FIRED", "UNKNOWN"};

        for (MediaCsv media : medias) {
            CameraModelCsv randomCamera = cameraModels.get(random.nextInt(cameraModels.size()));

            metadatas.add(new MediaMetaDataCsv(
                media.id, // metadata id = media id (1:1)
                randomCamera.id,
                random.nextInt(3200) + 100, // ISO 100~3200
                shutterSpeeds[random.nextInt(shutterSpeeds.length)],
                apertures[random.nextInt(apertures.length)],
                whiteBalances[random.nextInt(whiteBalances.length)],
                random.nextInt(200) + 24, // focal length 24~223mm
                "0.0", // exposure compensation
                flashModes[random.nextInt(flashModes.length)],
                0,
                1.0 + random.nextDouble() * 3.0, // zoom 1.0~4.0
                "0",
                LocalDateTime.now().minusDays(random.nextInt(365))
            ));

            if (metadatas.size() % 10000 == 0) {
                log.info("  - {} / {} 생성 완료", metadatas.size(), medias.size());
            }
        }

        log.info("✅ 미디어 메타데이터 {}개 생성 완료", metadatas.size());
        return metadatas;
    }

    /**
     * 댓글 데이터 생성
     */
    private static List<CommentCsv> generateComments(List<MemberCsv> members, List<GuideCsv> guides) {
        log.info("💬 댓글 데이터 생성 중...");
        List<CommentCsv> comments = new ArrayList<>();

        String[] commentTexts = {
            "정보 감사합니다!",
            "저도 다녀왔는데 정말 좋았어요.",
            "다음에 꼭 가보고 싶네요.",
            "사진이 정말 예쁘네요!",
            "유용한 정보 감사합니다.",
            "덕분에 좋은 여행 했습니다.",
            "추천해주셔서 감사해요!",
            "여기 정말 멋지죠!",
            "저도 이 장소 좋아합니다.",
            "가족과 함께 가면 좋을 것 같아요.",
            "날씨 좋을 때 가면 더 예쁠 것 같아요.",
            "주차는 어디에 했나요?",
            "입장료가 얼마인가요?",
            "영업시간 알 수 있을까요?",
            "근처 맛집 추천해주세요!"
        };

        for (int i = 1; i <= COMMENT_COUNT; i++) {
            MemberCsv randomMember = members.get(random.nextInt(members.size()));
            GuideCsv randomGuide = guides.get(random.nextInt(guides.size()));
            String commentText = commentTexts[random.nextInt(commentTexts.length)];

            comments.add(new CommentCsv(i, commentText, randomMember.id, randomGuide.id));

            if (i % 10000 == 0) {
                log.info("  - {} / {} 생성 완료", i, COMMENT_COUNT);
            }
        }

        log.info("✅ 댓글 {}개 생성 완료", comments.size());
        return comments;
    }

    // ========== CSV 파일 쓰기 메서드들 ==========

    private static void writeMembersCsv(List<MemberCsv> members) throws IOException {
        Path path = Paths.get(OUTPUT_DIR, "members.csv");
        try (FileWriter writer = new FileWriter(path.toFile())) {
            writer.write("id,email,password,nickname,provider,provider_id,created_at,updated_at\n");
            for (MemberCsv m : members) {
                writer.write(String.format("%d,%s,%s,%s,%s,%s,%s,%s\n",
                    m.id, m.email, m.password, m.nickname, m.provider,
                    nullToEmpty(m.providerId),
                    m.createdAt.format(DATE_FORMATTER),
                    m.updatedAt.format(DATE_FORMATTER)
                ));
            }
        }
        log.info("📄 members.csv: {} bytes", Files.size(path));
    }

    private static void writeMemberAuthorityCsv(List<MemberCsv> members) throws IOException {
        Path path = Paths.get(OUTPUT_DIR, "member_authority.csv");
        try (FileWriter writer = new FileWriter(path.toFile())) {
            writer.write("member_id,authority\n");
            for (MemberCsv m : members) {
                writer.write(String.format("%d,MEMBER\n", m.id));
            }
        }
        log.info("📄 member_authority.csv: {} bytes", Files.size(path));
    }

    private static void writeCameraModelsCsv(List<CameraModelCsv> models) throws IOException {
        Path path = Paths.get(OUTPUT_DIR, "camera_models.csv");
        try (FileWriter writer = new FileWriter(path.toFile())) {
            writer.write("id,manufacturer,model,lens\n");
            for (CameraModelCsv c : models) {
                writer.write(String.format("%d,%s,%s,%s\n",
                    c.id, c.manufacturer, escapeCsv(c.model), escapeCsv(c.lens)
                ));
            }
        }
        log.info("📄 camera_models.csv: {} bytes", Files.size(path));
    }

    private static void writeGuidesCsv(List<GuideCsv> guides) throws IOException {
        Path path = Paths.get(OUTPUT_DIR, "guides.csv");
        try (FileWriter writer = new FileWriter(path.toFile())) {
            writer.write("id,tip,member_id,location_id,like_count,created_at,updated_at\n");
            for (GuideCsv g : guides) {
                writer.write(String.format("%d,%s,%d,%d,%d,%s,%s\n",
                    g.id, escapeCsv(g.tip), g.memberId, g.locationId, g.likeCount,
                    g.createdAt.format(DATE_FORMATTER),
                    g.updatedAt.format(DATE_FORMATTER)
                ));
            }
        }
        log.info("📄 guides.csv: {} bytes", Files.size(path));
    }

    private static void writeMediasCsv(List<MediaCsv> medias) throws IOException {
        Path path = Paths.get(OUTPUT_DIR, "medias.csv");
        try (FileWriter writer = new FileWriter(path.toFile())) {
            writer.write("id,media_name,media_url,original_key,web_key,thumbnail_key,file_size,guide_id,media_metadata_id,location_id\n");
            for (MediaCsv m : medias) {
                writer.write(String.format("%d,%s,%s,%s,%s,%s,%d,%d,%d,%d\n",
                    m.id, m.mediaName, m.mediaUrl, m.originalKey, m.webKey, m.thumbnailKey,
                    m.fileSize, m.guideId, m.mediaMetadataId, m.locationId
                ));
            }
        }
        log.info("📄 medias.csv: {} bytes", Files.size(path));
    }

    private static void writeMediaMetaDatasCsv(List<MediaMetaDataCsv> metadatas) throws IOException {
        Path path = Paths.get(OUTPUT_DIR, "media_meta_data.csv");
        try (FileWriter writer = new FileWriter(path.toFile())) {
            writer.write("id,camera_model_id,iso,shutter_speed,aperture,white_balance,focal_length," +
                "exposure_compensation,flash_mode,flash_code,zoom_level,roll,time\n");
            for (MediaMetaDataCsv m : metadatas) {
                writer.write(String.format("%d,%d,%d,%s,%s,%s,%d,%s,%s,%d,%.2f,%s,%s\n",
                    m.id, m.cameraModelId, m.iso, m.shutterSpeed, m.aperture, m.whiteBalance,
                    m.focalLength, m.exposureCompensation, m.flashMode, m.flashCode,
                    m.zoomLevel, m.roll, m.time.format(DATE_FORMATTER)
                ));
            }
        }
        log.info("📄 media_meta_data.csv: {} bytes", Files.size(path));
    }

    private static void writeCommentsCsv(List<CommentCsv> comments) throws IOException {
        Path path = Paths.get(OUTPUT_DIR, "comments.csv");
        try (FileWriter writer = new FileWriter(path.toFile())) {
            writer.write("id,comment,member_id,guide_id\n");
            for (CommentCsv c : comments) {
                writer.write(String.format("%d,%s,%d,%d\n",
                    c.id, escapeCsv(c.comment), c.memberId, c.guideId
                ));
            }
        }
        log.info("📄 comments.csv: {} bytes", Files.size(path));
    }

    private static void copyToK6Directory() throws IOException {
        log.info("📦 k6 디렉토리로 파일 복사 중...");
        String[] files = {
            "members.csv", "guides.csv", "medias.csv", "comments.csv",
            "camera_models.csv", "media_meta_data.csv"
        };

        for (String file : files) {
            Path source = Paths.get(OUTPUT_DIR, file);
            Path target = Paths.get(K6_DATA_DIR, file);
            Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("✅ k6 디렉토리 복사 완료");
    }

    // 유틸리티 메서드
    private static String nullToEmpty(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // DTO 클래스들
    record MemberCsv(int id, String email, String password, String nickname,
                     String provider, String providerId,
                     LocalDateTime createdAt, LocalDateTime updatedAt) {}

    record CameraModelCsv(int id, String manufacturer, String model, String lens) {}

    record GuideCsv(int id, String tip, int memberId, int locationId, int likeCount,
                    LocalDateTime createdAt, LocalDateTime updatedAt) {}

    record MediaCsv(int id, String mediaName, String mediaUrl, String originalKey,
                    String webKey, String thumbnailKey, long fileSize,
                    int guideId, int mediaMetadataId, int locationId) {}

    record MediaMetaDataCsv(int id, int cameraModelId, int iso, String shutterSpeed,
                            String aperture, String whiteBalance, int focalLength,
                            String exposureCompensation, String flashMode, int flashCode,
                            double zoomLevel, String roll, LocalDateTime time) {}

    record CommentCsv(int id, String comment, int memberId, int guideId) {}
}
