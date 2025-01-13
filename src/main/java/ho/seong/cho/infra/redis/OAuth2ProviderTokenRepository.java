package ho.seong.cho.infra.redis;

import ho.seong.cho.oauth.data.token.OAuth2ProviderToken;
import org.springframework.data.repository.CrudRepository;

public interface OAuth2ProviderTokenRepository
    extends CrudRepository<OAuth2ProviderToken, String> {}
