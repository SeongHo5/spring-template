package ho.seong.cho.infra.aws.s3;

import static ho.seong.cho.infra.aws.s3.MyS3ClientImpl.DIRECTORY_PATH_PATTERN;

import io.jsonwebtoken.lang.Assert;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.tika.Tika;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

public final class S3Utils {

  private S3Utils() {}

  public static String createTimeBasedKey(String key) {
    return key + System.currentTimeMillis();
  }

  /**
   * 원본 파일 이름에서 확장자를 추출한다.
   *
   * @param file 파일
   * @return 파일 확장자
   */
  static String extractExtension(@NotNull MultipartFile file) {
    final var originalFileName = file.getOriginalFilename();
    if (originalFileName == null) {
      throw new RuntimeException();
    }
    final int lastIndexOfDot = originalFileName.lastIndexOf(".");
    if (lastIndexOfDot == -1 || lastIndexOfDot == originalFileName.length() - 1) {
      throw new RuntimeException();
    }
    return originalFileName.toLowerCase().substring(lastIndexOfDot);
  }

  /**
   * 주어진 URL에서 S3 객체 키를 추출한다.
   *
   * @param url 대상 URL
   * @return S3 객체 키 ({@code /}를 제외한 경로)
   */
  static String extractObjectKey(@NotNull String url) {
    try {
      return new URL(url).getPath().substring(1);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 파일의 MIME 타입을 판별한다.
   *
   * @param file 파일
   * @return MIME 타입
   */
  static String getMimeTypeFromStream(@NotNull MultipartFile file) {
    try (InputStream input = file.getInputStream()) {
      Tika tika = new Tika();
      final var mimeType = tika.detect(input);
      return mimeType != null ? mimeType : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
