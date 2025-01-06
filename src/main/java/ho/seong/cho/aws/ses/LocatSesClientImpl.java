package ho.seong.cho.aws.ses;

import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SesException;

@Component
@Slf4j
@RequiredArgsConstructor
public class LocatSesClientImpl implements LocatSesClient {

  private final SesClient sesClient;

  @Value("${service.aws.ses.from}")
  private String from;

  @Override
  public void send(final String to, final String subject, final String content) {
    try {
      SendEmailRequest request =
          SendEmailRequest.builder()
              .source(this.from)
              .destination(d -> d.toAddresses(to))
              .message(
                  m ->
                      m.subject(s -> s.charset(StandardCharsets.UTF_8.name()).data(subject))
                          .body(
                              b ->
                                  b.html(
                                      h -> h.charset(StandardCharsets.UTF_8.name()).data(content))))
              .build();
      this.sesClient.sendEmail(request);
    } catch (SesException ex) {
      log.error(
          "SesException occurred while sending email. [Subject: {}] / Reason: {}",
          subject,
          ex.getMessage());
      throw new RuntimeException();
    }
  }
}
