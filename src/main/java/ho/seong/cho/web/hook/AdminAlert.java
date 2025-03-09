package ho.seong.cho.web.hook;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * 해당 메서드가 실행 결과에 따라 관리자가 알림을 받아야 함을 나타내는 어노테이션 <br>
 * 이 어노테이션을 사용하여 중요한 메서드 실행 시 관리자에게 알림을 보낼 수 있다.
 *
 * <pre>{@code
 * @AdminAlert(message = "중요 작업 A가 시작되었습니다.")
 * public void startSomeImportantJobs() {
 *      // do something
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminAlert {

  /**
   * 관리자에게 전달할 메시지
   *
   * <p>메세지 접두사는 '[관리자 알림]'으로 고정
   *
   * @return 메세지 (never <code>null</code>)
   */
  String message();

  /**
   * 알림 이벤트를 발생시킬 조건을 SpEL 표현식으로 작성한다.
   *
   * <pre>{@code
   *  // 메서드가 실행에 성공했을 때만 알림을 보내려는 경우
   *  @AdminAlert(message = "중요 작업 A가 시작되었습니다.", condition = "#result != null")
   *  public void startSomeImportantJobs() {
   *    // do something
   * }
   * }</pre>
   *
   * @apiNote condition을 작성하지 않으면, true로 처리하여 메서드 실행 결과에 상관없이 알림을 보냅니다.
   * @return SpEL 표현식
   * @see SpelExpressionParser
   */
  String condition() default "";
}
