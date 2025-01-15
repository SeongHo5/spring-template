package ho.seong.cho.oauth.data.internal;

/**
 * OAuth2 사용자 연결 해제 요청 DTO
 *
 * @param accessToken 연결 해제할 사용자의 유효한 접근 토큰
 */
public record OAuth2WithdrawalRequest(String accessToken) {

  public static OAuth2WithdrawalRequest from(String accessToken) {
    return new OAuth2WithdrawalRequest(accessToken);
  }
}
