package UMC_8th.With_Run.common.config.s3;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@Service
@Component
@Slf4j
public class S3Uploader {
    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public String upload(MultipartFile multipartFile, String dirName) throws IOException {
        File uploadFile;
        try {
            uploadFile = convert(multipartFile)
                    .orElseThrow(() -> new IOException("MultipartFile -> File 전환 실패: convert()에서 Optional.empty() 반환됨"));
        } catch (Exception e) {
            log.error("파일 업로드 실패: {}", e.getMessage(), e);
            throw new RuntimeException("파일 업로드 중 오류 발생", e);
        }

        return uploadFile(uploadFile, dirName);
    }

    private String uploadFile(File uploadFile, String dirName) {
        String fileName = dirName + "/" + UUID.randomUUID() + uploadFile.getName();
        String uploadImageUrl = putS3(uploadFile, fileName);

        removeNewFile(uploadFile);  // 로컬에 생성된 File 삭제 (MultipartFile -> File 전환 하며 로컬에 파일 생성됨)

        return uploadImageUrl;      // 업로드된 파일의 S3 URL 주소 반환
    }

    private String putS3(File uploadFile, String fileName) {
        amazonS3.putObject(
                new PutObjectRequest(bucket, fileName, uploadFile)
        );
        return amazonS3.getUrl(bucket, fileName).toString();
    }

    private void removeNewFile(File targetFile) {
        if (targetFile.delete()) {
            log.info("파일 삭제가 완료되었습니다.");
        } else {
            log.info("파일 삭제가 실패되었습니다.");
        }
    }

    private Optional<File> convert(MultipartFile files) throws IOException {
        // stack-over-flow code
        File convertFile = new File(System.getProperty("java.io.tmpdir") +
                System.getProperty("file.separator") +
                files.getOriginalFilename());

        if (convertFile.createNewFile()) {
            try (FileOutputStream fos = new FileOutputStream(convertFile)) {
                fos.write(files.getBytes());
            }
            return Optional.of(convertFile);
        }
        return Optional.empty();
    }

    //file 삭제

    public void fileDelete(String s3Key) {
        try {
            amazonS3.deleteObject(bucket, s3Key);
            log.info("✅ S3 파일 삭제 완료: {}", s3Key);
        } catch (AmazonServiceException e) {
            log.error("❌ S3 삭제 실패");
            log.error("에러 메시지: {}", e.getErrorMessage());
            log.error("에러 코드: {}", e.getErrorCode());
            log.error("상태 코드: {}", e.getStatusCode());
            log.error("요청 ID: {}", e.getRequestId());
            log.error("삭제 대상 key: {}", s3Key);
            throw e;
        }
    }

    // URL에서 S3 키 추출
    public String extractKeyFromUrl(String fileUrl) {
        // 예: https://with-run-bucket.s3.ap-northeast-2.amazonaws.com/profile/abc_말티즈2.jpg
        // → 결과: profile/abc_말티즈2.jpg (URL 디코딩 필요)
        try {
            URI uri = new URI(fileUrl);
            String path = uri.getPath();
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("유효하지 않은 S3 URL입니다: " + fileUrl);
        }
    }



}
