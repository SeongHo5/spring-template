package ho.seong.cho.exception.custom;

import ho.seong.cho.exception.AbstractBusinessException;
import ho.seong.cho.exception.ApiExceptionType;
import lombok.Getter;

/**
 * 내부 로직 처리 중 예외 발생 시 던지는 예외<br>
 * 예외 메시지를 로깅하고, 클라이언트에게는 500 에러를 반환
 */
@Getter
public class InternalProcessingException extends AbstractBusinessException {

  public InternalProcessingException(Throwable cause) {
    super(ApiExceptionType.INTERNAL_SERVER_ERROR, cause);
  }
}
