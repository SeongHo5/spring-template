package ho.seong.cho.aws.s3;

import java.time.Duration;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/** S3 파일 처리 관련 서비스 인터페이스 */
public interface MyS3Client {

  /**
   * 업로드된 객체 목록을 조회한다.
   *
   * @param directoryPath 조회할 디렉토리 경로({@code items/founds} 형식)
   * @return 지정한 디렉토리에 저장된 객체 목록
   * @throws IllegalArgumentException 디렉토리 경로가 유효하지 않은 경우
   */
  List<String> getListObjects(String directoryPath);

  /**
   * S3 객체에 접근할 수 있는 서명된 URL을 생성한다.
   *
   * @param key S3 객체 키
   * @param expiration URL의 유효 시간
   * @return 생성된 서명된 URL
   */
  String generatePresignedUrl(String key, Duration expiration);

  /**
   * 주어진 키에 해당하는 S3 객체가 존재하는지 확인한다.
   *
   * @param key 확인할 S3 객체 키
   * @return 객체가 존재하면 {@code true}, 그렇지 않으면 {@code false}
   */
  boolean exists(String key);

  /**
   * {@code MultipartFile}을 업로드한다.
   *
   * @param directoryPath 업로드할 디렉토리 경로
   * @param file 업로드할 파일
   * @param fileName 업로드할 파일의 이름 (확장자 제외 / 확장자는 파일에서 추출)
   * @return 저장된 파일의 URL
   * @throws IllegalArgumentException 디렉토리 경로가 유효하지 않은 경우
   */
  String upload(String directoryPath, String fileName, MultipartFile file);

  /**
   * {@code MultipartFile}을 업로드한다.
   *
   * @param directoryPath 업로드할 디렉토리 경로
   * @param file 업로드할 파일
   * @return 저장된 파일의 URL
   * @throws IllegalArgumentException 디렉토리 경로가 유효하지 않은 경우
   * @apiNote 이 메서드는 {@link System#currentTimeMillis()} + 확장자로 파일명을 생성한다.
   */
  String upload(String directoryPath, MultipartFile file);

  /**
   * 저장된 파일을 삭제한다.
   *
   * @param key 삭제할 파일의 키(파일명을 포함한 <strong>**버킷 내 전체 경로**</strong>)
   * @throws IllegalArgumentException 키가 유효하지 않은 경우
   */
  void deleteByKey(String key);

  /**
   * 저장된 파일을 삭제한다.
   *
   * @param url 삭제할 파일의 URL
   */
  void deleteByUrl(String url);
}
