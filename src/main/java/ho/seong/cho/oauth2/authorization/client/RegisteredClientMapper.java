package ho.seong.cho.oauth2.authorization.client;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

final class RegisteredClientMapper {

  private RegisteredClientMapper() {}

  // -------------------------
  // Entity -> RegisteredClient
  // -------------------------
  public static RegisteredClient toRegisteredClient(Oauth2RegisteredClient e) {
    Objects.requireNonNull(e, "entity");

    RegisteredClient.Builder b =
        RegisteredClient.withId(requireNonBlank(e.getId(), "id"))
            .clientId(requireNonBlank(e.getClientId(), "clientId"))
            .clientName(requireNonBlank(e.getClientName(), "clientName"));

    Instant issuedAt = e.getClientIdIssuedAt();
    if (issuedAt != null) {
      b.clientIdIssuedAt(issuedAt);
    }

    if (e.getClientSecret() != null) {
      b.clientSecret(e.getClientSecret());
    }

    Instant secretExpiresAt = e.getClientSecretExpiresAt();
    if (secretExpiresAt != null) {
      b.clientSecretExpiresAt(secretExpiresAt);
    }

    // client auth methods
    Set<String> cam = defaultEmpty(e.getClientAuthenticationMethods());
    cam.forEach(v -> b.clientAuthenticationMethod(new ClientAuthenticationMethod(v)));

    // grant types
    Set<String> grants = defaultEmpty(e.getAuthorizationGrantTypes());
    grants.forEach(v -> b.authorizationGrantType(new AuthorizationGrantType(v)));

    // redirect uris
    defaultEmpty(e.getRedirectUris()).forEach(b::redirectUri);

    // post logout redirect uris
    defaultEmpty(e.getPostLogoutRedirectUris()).forEach(b::postLogoutRedirectUri);

    // scopes
    defaultEmpty(e.getScopes()).forEach(b::scope);

    // settings (Map -> settings)
    Map<String, Object> clientSettingsMap = defaultEmptyMap(e.getClientSettings());
    Map<String, Object> tokenSettingsMap = defaultEmptyMap(e.getTokenSettings());

    b.clientSettings(ClientSettings.withSettings(clientSettingsMap).build());
    b.tokenSettings(TokenSettings.withSettings(tokenSettingsMap).build());

    return b.build();
  }

  // -------------------------
  // RegisteredClient -> Entity
  // -------------------------
  public static Oauth2RegisteredClient toEntity(RegisteredClient rc) {
    Objects.requireNonNull(rc, "registeredClient");

    return Oauth2RegisteredClient.builder()
        .id(rc.getId())
        .clientId(rc.getClientId())
        .clientIdIssuedAt(rc.getClientIdIssuedAt())
        .clientSecret(rc.getClientSecret())
        .clientSecretExpiresAt(rc.getClientSecretExpiresAt())
        .clientName(rc.getClientName())
        .clientAuthenticationMethods(
            rc.getClientAuthenticationMethods().stream()
                .map(ClientAuthenticationMethod::getValue)
                .collect(Collectors.toCollection(LinkedHashSet::new)))
        .authorizationGrantTypes(
            rc.getAuthorizationGrantTypes().stream()
                .map(AuthorizationGrantType::getValue)
                .collect(Collectors.toCollection(LinkedHashSet::new)))
        .redirectUris(new LinkedHashSet<>(rc.getRedirectUris()))
        .postLogoutRedirectUris(new LinkedHashSet<>(rc.getPostLogoutRedirectUris()))
        .scopes(new LinkedHashSet<>(rc.getScopes()))
        .clientSettings(new LinkedHashMap<>(rc.getClientSettings().getSettings()))
        .tokenSettings(new LinkedHashMap<>(rc.getTokenSettings().getSettings()))
        .build();
  }

  private static <T> Set<T> defaultEmpty(Set<T> v) {
    return v == null ? Collections.emptySet() : v;
  }

  private static Map<String, Object> defaultEmptyMap(Map<String, Object> v) {
    return v == null ? Collections.emptyMap() : v;
  }

  private static Map<String, Object> copySettings(Map<String, Object> source) {
    return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
  }

  private static String requireNonBlank(String v, String field) {
    if (v == null || v.isBlank()) {
      throw new IllegalArgumentException(field + " is blank");
    }
    return v;
  }
}
