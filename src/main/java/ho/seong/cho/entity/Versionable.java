package ho.seong.cho.entity;

import org.springframework.http.HttpHeaders;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** HTTP 캐싱을 위한 {@link HttpHeaders#ETAG} / {@link HttpHeaders#LAST_MODIFIED} 값을 제공하는 인터페이스 */
public interface Versionable {

  /** Constant for GMT Timezone */
  static final ZoneId GMT_ZONE = ZoneId.of("GMT");

  /** RFC 1123 Date Time Formatter */
  static final DateTimeFormatter RFC_1123_FORMAT = DateTimeFormatter.RFC_1123_DATE_TIME;

  /**
   * 객체의 {@code ETag} 값을 반환한다.
   *
   * @return {@code ETag} 값 (e.g., "ahsnm12cjk88")
   */
  String getETag();

  /**
   * 객체의 {@code Last-Modified} 값을 RFC 1123 형태의 문자열로 반환한다.
   *
   * @return 객체의 {@code Last-Modified} (e.g., "Mon, 10 Mar 2025 12:30:00 GMT")
   */
  String getLastModified();
}
