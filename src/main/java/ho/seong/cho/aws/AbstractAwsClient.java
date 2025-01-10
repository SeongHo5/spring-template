package ho.seong.cho.aws;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractAwsClient {

  protected final AwsProperties awsProperties;
}
