package ho.seong.cho.oauth2.authorization.client;

import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DbRegistredClientRepository implements RegisteredClientRepository {

  private final Oauth2ClientInfoRepository repository;

  @Override
  public void save(RegisteredClient registeredClient) {
    // TODO: id 기준으로 기등록 여부에 따라 save / update
    this.repository.save(RegisteredClientMapper.toEntity(registeredClient));
  }

  @Nullable @Override
  public RegisteredClient findById(String id) {
    return this.repository
        .findById(id)
        .map(RegisteredClientMapper::toRegisteredClient)
        .orElse(null);
  }

  @Nullable @Override
  public RegisteredClient findByClientId(String clientId) {
    return this.repository
        .findByClientId(clientId)
        .map(RegisteredClientMapper::toRegisteredClient)
        .orElse(null);
  }
}
