package ho.seong.cho.oauth.data.enums;

import java.util.Arrays;

/** OAuth2 제공자 타입 */
public enum OAuth2ProviderType {
  APPLE,
  GOOGLE,
  KAKAO,
  NAVER,
  GITHUB,
  FACEBOOK;

  public static OAuth2ProviderType from(final String providerName) {
    return Arrays.stream(values())
        .filter(type -> type.name().equalsIgnoreCase(providerName))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("Unsupported OAuth2 provider: " + providerName));
  }
}
