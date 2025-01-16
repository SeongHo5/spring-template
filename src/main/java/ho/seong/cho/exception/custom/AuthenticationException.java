package ho.seong.cho.exception.custom;

import ho.seong.cho.exception.AbstractBusinessException;
import ho.seong.cho.exception.ApiExceptionType;

public class AuthenticationException extends AbstractBusinessException {

  public AuthenticationException(Throwable cause) {
    super(ApiExceptionType.UNAUTHORIZED, cause);
  }

  public AuthenticationException() {
    this(null);
  }
}
