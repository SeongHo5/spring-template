package ho.seong.cho.aws;

import ho.seong.cho.aws.config.AwsProperties;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractAwsClient {

  protected final AwsProperties awsProperties;
}
