package ho.seong.cho.oauth.data.internal;

import java.util.List;

/**
 * OAuth2 Provider JWT 공개키 응답 DTO
 *
 * @param keys JWT 공개키 목록
 */
public record OAuth2ProviderJsonWebKeys(List<JsonWebKey> keys) {

  /**
   * JWT Web Key
   *
   * @param alg 토큰 암호화에 사용된 알고리즘
   * @param e RSA 공개 지수
   * @param kid 키 식별자(Auth Key ID)
   * @param kty 키 타입(RSA)
   * @param n RSA 모듈러스
   * @param use 키 사용 용도(Signature)
   */
  public record JsonWebKey(String alg, String e, String kid, String kty, String n, String use) {}
}
