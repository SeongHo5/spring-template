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
  public static RegisteredClient toRegisteredClient(Oauth2ClientInfo entity) {
    Objects.requireNonNull(entity, "entity");

    final var builder =
        RegisteredClient.withId(entity.getId())
            .clientId(entity.getClientId())
            .clientName(entity.getClientName());

    Long issuedAt = entity.getClientIdIssuedAt();
    if (issuedAt != null) {
      builder.clientIdIssuedAt(Instant.ofEpochSecond(issuedAt));
    }

    if (entity.getClientSecret() != null) {
      builder.clientSecret(entity.getClientSecret());
    }

    Long secretExpiresAt = entity.getClientSecretExpiresAt();
    if (secretExpiresAt != null) {
      builder.clientSecretExpiresAt(Instant.ofEpochSecond(secretExpiresAt));
    }

    // client auth methods
    Set<String> cam = defaultEmpty(entity.getClientAuthenticationMethods());
    cam.forEach(v -> builder.clientAuthenticationMethod(new ClientAuthenticationMethod(v)));
    // grant types
    Set<String> grants = defaultEmpty(entity.getAuthorizationGrantTypes());
    grants.forEach(v -> builder.authorizationGrantType(new AuthorizationGrantType(v)));
    // redirect uris
    defaultEmpty(entity.getRedirectUris()).forEach(builder::redirectUri);
    // post logout redirect uris
    defaultEmpty(entity.getPostLogoutRedirectUris()).forEach(builder::postLogoutRedirectUri);
    // scopes
    defaultEmpty(entity.getScopes()).forEach(builder::scope);
    // settings (Map -> settings)
    Map<String, Object> clientSettingsMap = defaultEmptyMap(entity.getClientSettings());
    Map<String, Object> tokenSettingsMap = defaultEmptyMap(entity.getTokenSettings());
    builder.clientSettings(ClientSettings.withSettings(clientSettingsMap).build());
    builder.tokenSettings(TokenSettings.withSettings(tokenSettingsMap).build());

    return builder.build();
  }

  // -------------------------
  // RegisteredClient -> Entity
  // -------------------------
  public static Oauth2ClientInfo toEntity(RegisteredClient rc) {
    Objects.requireNonNull(rc, "registeredClient");

    var builder = Oauth2ClientInfo.builder().id(rc.getId()).clientId(rc.getClientId());

    Instant issuedAt = rc.getClientIdIssuedAt();
    if (issuedAt != null) {
      builder.clientIdIssuedAt(issuedAt.getEpochSecond());
    }
    if (rc.getClientSecret() != null) {
      builder.clientSecret(rc.getClientSecret());
    }
    Instant secretExpiresAt = rc.getClientSecretExpiresAt();
    if (secretExpiresAt != null) {
      builder.clientSecretExpiresAt(secretExpiresAt.getEpochSecond());
    }

    return builder
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
}
