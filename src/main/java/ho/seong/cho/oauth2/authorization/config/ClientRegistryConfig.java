package ho.seong.cho.oauth2.authorization.config;

import ho.seong.cho.oauth2.authorization.client.DbRegistredClientRepository;
import ho.seong.cho.oauth2.authorization.client.Oauth2ClientInfoRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

@Configuration
public class ClientRegistryConfig {

  @Bean
  public RegisteredClientRepository registeredClientRepository(
      Oauth2ClientInfoRepository repository) {
    return new DbRegistredClientRepository(repository);
  }
}
