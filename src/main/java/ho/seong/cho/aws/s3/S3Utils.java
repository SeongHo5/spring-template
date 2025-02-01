package ho.seong.cho.aws.s3;

import static ho.seong.cho.aws.s3.MyS3ClientImpl.*;

import io.jsonwebtoken.lang.Assert;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

public final class S3Utils {

  private S3Utils() {}

  /**
   * 원본 파일 이름에서 확장자를 추출한다.
   *
   * @param file 파일
   * @return 파일 확장자
   */
  static String extractExtension(final MultipartFile file) {
    final String originalFilename =
        Optional.of(file)
            .map(MultipartFile::getOriginalFilename)
            .map(String::toLowerCase)
            .orElseThrow(RuntimeException::new);
    final int lastIndexOfDot = originalFilename.lastIndexOf(".");
    if (lastIndexOfDot == -1 || lastIndexOfDot == originalFilename.length() - 1) {
      throw new RuntimeException();
    }
    return originalFilename.substring(lastIndexOfDot);
  }

  /**
   * 디렉토리 경로의 유효성 검사를 수행한다.
   *
   * @param directoryPath 디렉토리 경로
   * @throws IllegalArgumentException {@code directoryPath}가 공백 또는 {@code null}이거나, 적절한 경로 형식이 아닌 경우
   */
  static void validatePath(final String directoryPath) {
    Assert.hasText(directoryPath, "directoryPath must not be null or empty");
    Assert.isTrue(DIRECTORY_PATH_PATTERN.matcher(directoryPath).matches(), "Invalid directoryPath");
  }
}
