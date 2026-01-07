package ho.seong.cho.oauth2.authorization.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwkConfig {

  @Bean
  public JWKSource<SecurityContext> jwkSource() throws NoSuchAlgorithmException {
    KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
    g.initialize(2048);
    KeyPair kp = g.generateKeyPair();
    RSAKey rsa =
        new RSAKey.Builder((RSAPublicKey) kp.getPublic())
            .privateKey(kp.getPrivate())
            .keyID("your-key-id")
            .build();
    JWKSet jwkSet = new JWKSet(rsa);
    return (jwkSelector, context) -> jwkSelector.select(jwkSet);
  }
}
