package ho.seong.cho.web.hook;

import ho.seong.cho.infra.client.http.DiscordClient;
import ho.seong.cho.utils.MySpelParser;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** {@link AdminAlert} 어노테이션이 붙은 메소드의 실행 결과에 따라 관리자에게 알림을 보내는 Advice */
@Aspect
@Component
@Profile("prod")
@RequiredArgsConstructor
public class AdminAlertAdvice {

  public static final String MESSAGE_PREFIX = "[관리자 알림] ";

  private final DiscordClient discordClient;
  private final WebhookProperties webhookProperties;

  @Around("@annotation(adminAlert)")
  public Object handleNotification(ProceedingJoinPoint joinPoint, AdminAlert adminAlert)
      throws Throwable {
    Object methodResult = joinPoint.proceed();

    if (shouldNotify(joinPoint, adminAlert, methodResult)) {
      var discordProperties = this.webhookProperties.discord();
      var message = MESSAGE_PREFIX.concat(adminAlert.message());
      this.discordClient.send(
          discordProperties.serverId(), discordProperties.token(), new WebhookRequest(message));
    }

    return methodResult;
  }

  private static boolean shouldNotify(
      ProceedingJoinPoint joinPoint, AdminAlert requireAdminNotify, Object methodResult) {
    var condition = requireAdminNotify.condition();
    return StringUtils.isBlank(condition) || evaluateCondition(condition, joinPoint, methodResult);
  }

  private static boolean evaluateCondition(
      String expression, ProceedingJoinPoint joinPoint, Object methodResult) {
    MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
    return MySpelParser.evaluateExpression(
        expression, methodSignature.getParameterNames(), methodResult);
  }
}
