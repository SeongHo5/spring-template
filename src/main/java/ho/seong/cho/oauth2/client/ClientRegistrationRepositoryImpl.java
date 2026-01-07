package ho.seong.cho.oauth2.client;

import ho.seong.cho.oauth2.authorization.client.Oauth2ClientInfoMapper;
import ho.seong.cho.oauth2.authorization.client.Oauth2ClientInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ClientRegistrationRepositoryImpl implements ClientRegistrationRepository {

  private final Oauth2ClientInfoRepository delegate;

  @Override
  public ClientRegistration findByRegistrationId(String registrationId) {
    return this.delegate
        .findById(registrationId)
        .map(Oauth2ClientInfoMapper::toClientRegistration)
        .orElse(null);
  }
}
