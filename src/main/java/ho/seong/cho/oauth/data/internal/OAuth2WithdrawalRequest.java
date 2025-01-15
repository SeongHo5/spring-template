package ho.seong.cho.oauth.data.internal;

public record OAuth2WithdrawalRequest(String accessToken) {

  public static OAuth2WithdrawalRequest from(String accessToken) {
    return new OAuth2WithdrawalRequest(accessToken);
  }
}
