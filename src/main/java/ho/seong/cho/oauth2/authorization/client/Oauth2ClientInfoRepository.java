package ho.seong.cho.oauth2.authorization.client;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Oauth2ClientInfoRepository extends JpaRepository<Oauth2ClientInfo, String> {

  Optional<Oauth2ClientInfo> findByClientId(String clientId);
}
