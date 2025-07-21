package ho.seong.cho.infra.aws.ses;

import ho.seong.cho.infra.aws.AbstractAwsClient;
import ho.seong.cho.infra.aws.config.AwsProperties;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
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
                  message ->
                      message
                          .subject(s -> s.charset(DEFAULT_CHARSET).data(subject))
                          .body(
                              body ->
                                  body.html(html -> html.charset(DEFAULT_CHARSET).data(content))
                                      .text(
                                          text ->
                                              text.charset(DEFAULT_CHARSET)
                                                  .data(stripHtml(content)))))
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

  private static String stripHtml(String html) {
    return html == null ? null : Jsoup.parse(html).text();
  }
}
