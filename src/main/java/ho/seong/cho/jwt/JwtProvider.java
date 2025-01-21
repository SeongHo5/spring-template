package ho.seong.cho.jwt;

import ho.seong.cho.users.User;
import io.jsonwebtoken.Claims;
import java.util.Optional;

public interface JwtProvider {

  /**
   * {@link User} 정보를 기반으로 토큰을 생성한다.
   *
   * @param user 사용자 정보
   * @return 토큰 발급 응답 DTO
   * @apiNote 갱신 토큰은 캐시 저장소에 저장됩니다.
   */
  JsonWebToken create(User user);

  /**
   * 접근 토큰을 갱신한다.
   *
   * @param oldAccessToken 이전 접근 토큰
   * @param refreshToken 갱신 토큰
   * @return 갱신된 토큰 발급 응답 DTO
   */
  JsonWebToken renew(String oldAccessToken, String refreshToken);

  /**
   * 토큰을 파싱하여 사용자 정보를 반환한다.
   *
   * @param token 접근 토큰
   * @return 사용자 정보, 변조/만료되었거나 유효하지 않은 토큰인 경우 {@link Optional#empty()}
   */
  Optional<Claims> parse(String token);
}
