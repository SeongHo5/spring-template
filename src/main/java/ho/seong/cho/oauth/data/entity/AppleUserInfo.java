package ho.seong.cho.oauth.data.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import ho.seong.cho.oauth.data.enums.OAuth2ProviderType;
import io.jsonwebtoken.Claims;
import java.util.Set;
import lombok.Builder;

/**
 * Apple Open Connect ID 토큰(JWT)
 *
 * @param issuer Issuer(Apple - {@code https://appleid.apple.com})
 * @param subject Subject(사용자 고유 ID)
 * @param audience Audience(Client ID)
 * @param issuedAt 발급 시간(Unix timestamp)
 * @param expirationTime 만료 시간(Unix timestamp)
 * @param email 사용자의 이메일 주소
 * @param emailVerified 이메일 주소 확인 여부
 */
@Builder
public record AppleUserInfo(
    @JsonProperty("iss") String issuer,
    @JsonProperty("sub") String subject,
    @JsonProperty("aud") Set<String> audience,
    @JsonProperty("iat") Long issuedAt,
    @JsonProperty("exp") Long expirationTime,
    String email,
    @JsonProperty("email_verified") Boolean emailVerified)
    implements OAuth2UserInfo {

  @Override
  public String getId() {
    return this.subject;
  }

  @Override
  public String getEmail() {
    return this.email;
  }

  @Override
  public OAuth2ProviderType getProvider() {
    return OAuth2ProviderType.APPLE;
  }

  public static AppleUserInfo fromJwt(Claims body) {
    return AppleUserInfo.builder()
        .issuer(body.getIssuer())
        .subject(body.getSubject())
        .audience(body.getAudience())
        .issuedAt(body.getIssuedAt().getTime())
        .expirationTime(body.getExpiration().getTime())
        .email(body.get("email", String.class))
        .emailVerified(body.get("email_verified", Boolean.class))
        .build();
  }
}
