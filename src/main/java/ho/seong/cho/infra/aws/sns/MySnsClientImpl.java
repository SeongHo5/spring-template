package ho.seong.cho.infra.aws.sns;

import ho.seong.cho.infra.aws.AbstractAwsClient;
import ho.seong.cho.infra.aws.config.AwsProperties;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.SnsException;

@Service
public class MySnsClientImpl extends AbstractAwsClient implements MySnsClient {

  private final SnsClient snsClient;

  public MySnsClientImpl(AwsProperties awsProperties, SnsClient snsClient) {
    super(awsProperties);
    this.snsClient = snsClient;
  }

  @Override
  public String broadcast(final String subject, final String message) {
    try {
      var request =
          PublishRequest.builder()
              .topicArn(this.awsProperties.sns().topicArn())
              .subject(subject)
              .message(message)
              .build();
      return this.snsClient.publish(request).messageId();
    } catch (SnsException ex) {
      throw new RuntimeException(ex);
    }
  }
}
