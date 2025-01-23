package ho.seong.cho.aws.sns;

import ho.seong.cho.aws.AbstractAwsClient;
import ho.seong.cho.aws.config.AwsProperties;
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
      PublishRequest request =
          PublishRequest.builder()
              .topicArn(this.awsProperties.sns().topicArn())
              .subject(subject)
              .message(message)
              .build();
      return this.snsClient.publish(request).messageId();
    } catch (SnsException e) {
      throw new RuntimeException(e);
    }
  }
}
