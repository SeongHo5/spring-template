package ho.seong.cho.oauth2.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class Oauth2ClientConfig {

  @Bean
  public OAuth2AuthorizedClientManager authorizedClientManager(
      ClientRegistrationRepository registrationRepository,
      OAuth2AuthorizedClientRepository authorizedClientRepository) {
    var provider =
        OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().refreshToken().build();

    var manager =
        new DefaultOAuth2AuthorizedClientManager(
            registrationRepository, authorizedClientRepository);
    manager.setAuthorizedClientProvider(provider);
    return manager;
  }

  @Bean
  public WebClient webClient(OAuth2AuthorizedClientManager manager) {
    var oauth2ExchangeFunction = new ServletOAuth2AuthorizedClientExchangeFilterFunction(manager);
    return WebClient.builder().filter(oauth2ExchangeFunction).build();
  }
}
