package ho.seong.cho.security;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface MyUserDetailsService extends UserDetailsService {

  /**
   * 사용자 인증 정보로 인증 객체 {@link Authentication}을 생성한다.
   *
   * @param userDetails 사용자 정보
   * @return 인증 객체 {@link Authentication}
   */
  Authentication createAuthentication(UserDetails userDetails);

  /**
   * JWT 토큰으로부터 인증 객체 {@link Authentication}을 생성한다.
   *
   * @param claims JWT 토큰의 클레임 정보
   * @return 인증 객체 {@link Authentication}
   */
  Authentication createAuthentication(Claims claims);

  /**
   * 인증 객체로부터 (단일) 권한 정보를 추출한다.
   *
   * @param authentication 인증 객체
   * @return 권한 정보
   */
  String extractAuthority(Authentication authentication);
}
