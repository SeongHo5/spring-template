package ho.seong.cho.aws.s3;

import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

public final class FileUtils {

  private FileUtils() {
    // Utility class
  }

  /**
   * 원본 파일 이름에서 확장자를 추출합니다.
   *
   * @param file 파일
   * @return 파일 확장자
   */
  public static String extractExtension(MultipartFile file) {
    final String originalFilename =
        Optional.of(file)
            .map(MultipartFile::getOriginalFilename)
            .map(String::toLowerCase)
            .orElseThrow(() -> new RuntimeException());
    final int lastIndexOfDot = originalFilename.lastIndexOf(".");
    if (lastIndexOfDot == -1 || lastIndexOfDot == originalFilename.length() - 1) {
      throw new RuntimeException();
    }
    return originalFilename.substring(lastIndexOfDot);
  }
}
