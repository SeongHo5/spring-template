package ho.seong.cho.aws.s3;

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
   * {@code MultipartFile}을 업로드한다.
   *
   * @param directoryPath 업로드할 디렉토리 경로
   * @param file 업로드할 파일
   * @return 저장된 파일의 URL
   * @throws IllegalArgumentException 디렉토리 경로가 유효하지 않은 경우
   */
  String upload(String directoryPath, MultipartFile file);

  /**
   * 저장된 파일을 삭제한다.
   *
   * @param key 삭제할 파일의 키(파일명을 포함한 <strong>**버킷 내 전체 경로**</strong>)
   * @throws IllegalArgumentException 키가 유효하지 않은 경우
   */
  void delete(String key);
}
