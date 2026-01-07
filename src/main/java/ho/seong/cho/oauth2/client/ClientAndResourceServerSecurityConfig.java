package ho.seong.cho.oauth2.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class ClientAndResourceServerSecurityConfig {

  @Bean
  public SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
    return http.oauth2Login(Customizer.withDefaults())
        .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))
        .build();
  }
}
