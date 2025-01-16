package ho.seong.cho.oauth.support;

import ho.seong.cho.exception.custom.AuthenticationException;
import ho.seong.cho.infra.redis.OAuth2ProviderTokenRepository;
import ho.seong.cho.oauth.OAuth2Template;
import ho.seong.cho.oauth.OAuth2TemplateFactory;
import ho.seong.cho.oauth.data.enums.OAuth2ProviderType;
import ho.seong.cho.oauth.data.token.OAuth2ProviderToken;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class OAuth2TemplateFactoryImpl implements OAuth2TemplateFactory {

  private final Map<OAuth2ProviderType, OAuth2Template> templateMap;
  private final OAuth2ProviderTokenRepository providerTokenRepository;

  public OAuth2TemplateFactoryImpl(
      List<OAuth2Template> templates, OAuth2ProviderTokenRepository providerTokenRepository) {
    final Map<OAuth2ProviderType, OAuth2Template> enumTemplateMap =
        templates.stream()
            .collect(
                Collectors.toMap(
                    OAuth2Template::getProviderType,
                    Function.identity(),
                    (existing, replacement) -> existing,
                    () -> new EnumMap<>(OAuth2ProviderType.class)));

    this.templateMap = Collections.unmodifiableMap(enumTemplateMap);
    this.providerTokenRepository = providerTokenRepository;
  }

  @Override
  public OAuth2Template getByProviderType(final OAuth2ProviderType providerType) {
    return this.templateMap.get(providerType);
  }

  @Override
  public OAuth2Template getByOAuthId(final String oAuthId) {
    return this.providerTokenRepository
        .findById(oAuthId)
        .map(OAuth2ProviderToken::getProviderType)
        .map(this::getByProviderType)
        .orElseThrow(AuthenticationException::new);
  }
}
