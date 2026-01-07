package ho.seong.cho.oauth2.authorization.client;

import ho.seong.cho.entity.converter.JsonMapConverter;
import ho.seong.cho.entity.converter.StringSetToCsvConverter;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import lombok.*;

@Entity
@Table(name = "tb_oauth2_registered_client")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Oauth2RegisteredClient {

  @Id
  @Column(length = 100)
  private String id;

  @Column(name = "client_id", length = 100, nullable = false, unique = true)
  private String clientId;

  @Column(name = "client_id_issued_at", nullable = false)
  private Instant clientIdIssuedAt;

  @Column(name = "client_secret", length = 200)
  private String clientSecret;

  @Column(name = "client_secret_expires_at")
  private Instant clientSecretExpiresAt;

  @Column(name = "client_name", length = 200, nullable = false)
  private String clientName;

  @Convert(converter = StringSetToCsvConverter.class)
  @Column(name = "client_authentication_methods", length = 1000, nullable = false)
  private Set<String> clientAuthenticationMethods;

  @Convert(converter = StringSetToCsvConverter.class)
  @Column(name = "authorization_grant_types", length = 1000, nullable = false)
  private Set<String> authorizationGrantTypes;

  @Convert(converter = StringSetToCsvConverter.class)
  @Column(name = "redirect_uris", length = 1000)
  private Set<String> redirectUris;

  @Convert(converter = StringSetToCsvConverter.class)
  @Column(name = "post_logout_redirect_uris", length = 1000)
  private Set<String> postLogoutRedirectUris;

  @Convert(converter = StringSetToCsvConverter.class)
  @Column(name = "scopes", length = 1000, nullable = false)
  private Set<String> scopes;

  @Convert(converter = JsonMapConverter.class)
  @Column(name = "client_settings", length = 2000, nullable = false)
  private Map<String, Object> clientSettings;

  @Convert(converter = JsonMapConverter.class)
  @Column(name = "token_settings", length = 2000, nullable = false)
  private Map<String, Object> tokenSettings;
}
