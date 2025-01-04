package ho.seong.cho.exception.custom;

import ho.seong.cho.exception.AbstractBusinessException;
import ho.seong.cho.exception.ApiExceptionType;

/**
 * Entity가 존재하지 않을 때 발생하는 예외 <br>
 * <li>주어진 ID, Key 등 정보로 Entity 조회에 실패했을 때 등
 */
public class NoSuchEntityException extends AbstractBusinessException {

  public NoSuchEntityException(ApiExceptionType apiExceptionType) {
    super(apiExceptionType);
  }
}
