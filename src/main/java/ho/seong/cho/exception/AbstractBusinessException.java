package ho.seong.cho.exception;

import jakarta.annotation.Nullable;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

/** 애플리케이션 내에서 발생하는 모든 예외의 최상위 클래스 */
@Getter
public abstract class AbstractBusinessException extends RuntimeException {

  protected static final Logger log = LoggerFactory.getLogger(AbstractBusinessException.class);

  /** HTTP 응답 코드 */
  private final HttpStatus httpStatus;

  /** 예외 메시지 */
  private final String message;

  /** 예외 관리 코드 */
  private final Integer code;

  /** 예외 원인 */
  @Nullable private final Throwable cause;

  protected AbstractBusinessException(ApiExceptionType apiExceptionType, @Nullable Throwable cause) {
    super(apiExceptionType.getMessage(), cause);
    this.httpStatus = HttpStatus.resolve(apiExceptionType.getStatusCode());
    this.message = apiExceptionType.getMessage();
    this.code = apiExceptionType.getCode();
    this.cause = cause;
  }

  protected AbstractBusinessException(ApiExceptionType apiExceptionType, @Nullable String detailMessage, @Nullable Throwable cause) {
    super(detailMessage, cause);
    this.httpStatus = HttpStatus.resolve(apiExceptionType.getStatusCode());
    this.message = detailMessage;
    this.code = apiExceptionType.getCode();
    this.cause = cause;
  }

  protected AbstractBusinessException(ApiExceptionType apiExceptionType) {
    this(apiExceptionType, null);
  }

}
