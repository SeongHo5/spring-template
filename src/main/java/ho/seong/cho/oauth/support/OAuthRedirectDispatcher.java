package ho.seong.cho.oauth.support;

import ho.seong.cho.entity.dto.BaseResponse;
import ho.seong.cho.oauth.OAuth2TemplateFactory;
import ho.seong.cho.oauth.data.enums.OAuth2ProviderType;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriTemplate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/oauth2/redirect")
public class OAuthRedirectDispatcher {

  private static final UriTemplate REDIRECT_URI =
      new UriTemplate("{clientUrl}/login?oAuthId={oAuthId}");

  private static final BiConsumer<OAuth2ProviderType, String> handleProviderState =
      (providerType, state) -> {
        if (providerType == OAuth2ProviderType.NAVER) {
          NaverOAuth2StateHolder.set(state);
        }
      };

  private final OAuth2TemplateFactory templateFactory;

  @RequestMapping(
      path = "/{provider}",
      method = {RequestMethod.GET, RequestMethod.POST})
  public ResponseEntity<BaseResponse<Void>> handleOAuth2Callback(
      @PathVariable String provider,
      @RequestParam String code,
      @RequestParam(required = false) String state) {
    final OAuth2ProviderType providerType = OAuth2ProviderType.from(provider);

    handleProviderState.accept(providerType, state);
    final String oAuthId =
        this.templateFactory.getByProviderType(providerType).issueToken(code).getId();

    return ResponseEntity.status(HttpStatus.FOUND)
        .location(REDIRECT_URI.expand("https://replacement.to-production.com", oAuthId))
        .build();
  }
}
