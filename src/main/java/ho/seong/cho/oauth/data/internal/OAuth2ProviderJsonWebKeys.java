package ho.seong.cho.oauth.data.internal;

import java.util.List;

/**
 * JWT 공개키 응답 DTO
 *
 * @param keys JWT 공개키 목록
 */
public record OAuth2ProviderJsonWebKeys(List<OAuth2ProviderJsonWebKey> keys) {}
