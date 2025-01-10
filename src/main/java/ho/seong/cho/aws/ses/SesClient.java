package ho.seong.cho.aws.ses;

public interface SesClient {

  /**
   * 메일을 전송한다.
   *
   * @param to 수신자
   * @param subject 제목
   * @param content 내용
   */
  void send(final String to, final String subject, final String content);
}
