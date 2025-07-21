package ho.seong.cho.infra.aws.sns;

public interface MySnsClient {

  /**
   * 모든 사용자에게 메세지를 발송한다.
   *
   * @param subject 메시지 제목
   * @param message 메시지 내용
   * @return 발송된 메시지의 고유 ID
   */
  String broadcast(String subject, String message);

  /**
   * 특정 사용자에게 등록된 모든 디바이스에 메시지를 발송한다.
   *
   * @param userId 사용자 ID
   * @param subject 메시지 제목
   * @param message 메시지 내용
   * @return 발송된 메시지의 고유 ID 목록(","로 구분)
   */
  //  String unicast(Long userId, String subject, String message);
}
