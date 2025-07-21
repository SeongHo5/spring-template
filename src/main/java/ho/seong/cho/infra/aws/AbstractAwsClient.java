package ho.seong.cho.infra.aws;

import ho.seong.cho.infra.aws.config.AwsProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public abstract class AbstractAwsClient {

  protected static final Logger log = LoggerFactory.getLogger(AbstractAwsClient.class);

  protected final AwsProperties awsProperties;
}
