package ho.seong.cho.aws.ses;

import ho.seong.cho.aws.AbstractAwsClient;
import ho.seong.cho.aws.config.AwsProperties;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SesException;

@Component
@Slf4j
public class MySesClientImpl extends AbstractAwsClient implements MySesClient {

  private static final String DEFAULT_CHARSET = StandardCharsets.UTF_8.name();

  private final SesClient sesClient;

  public MySesClientImpl(AwsProperties awsProperties, SesClient sesClient) {
    super(awsProperties);
    this.sesClient = sesClient;
  }

  @Override
  public void send(final String to, final String subject, final String content) {
    try {
      var request =
          SendEmailRequest.builder()
              .source(this.awsProperties.ses().from())
              .destination(d -> d.toAddresses(to))
              .message(
                  m ->
                      m.subject(s -> s.charset(DEFAULT_CHARSET).data(subject))
                          .body(b -> b.html(h -> h.charset(DEFAULT_CHARSET).data(content))))
              .build();
      this.sesClient.sendEmail(request);
    } catch (SesException ex) {
      log.error(
          "SesException occurred while sending email. [Subject: {}] / Reason: {}",
          subject,
          ex.getMessage());
      throw new RuntimeException(ex);
    }
  }
}
