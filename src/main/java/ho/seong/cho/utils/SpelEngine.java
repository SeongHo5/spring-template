package ho.seong.cho.utils;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class SpelEngine {

  private static final ExpressionParser PARSER = new SpelExpressionParser();

  private SpelEngine() {}

  /**
   * SpEL을 이용하여 동적으로 값을 가져온다.
   *
   * @param expression SpEL 표현식
   * @param parameterNames 파라미터 이름
   * @param args 파라미터 값
   * @return 동적으로 가져온 값
   */
  public static Object getDynamicValue(String expression, String[] parameterNames, Object[] args) {
    ExpressionParser parser = new SpelExpressionParser();
    EvaluationContext context = new StandardEvaluationContext();

    for (int i = 0; i < parameterNames.length; i++) {
      context.setVariable(parameterNames[i], args[i]);
    }

    return parser.parseExpression(preprocess(expression)).getValue(context);
  }

  /**
   * SpEL을 이용해 표현식을 평가한다.
   *
   * @param expression SpEL 표현식
   * @param parameterNames 파라미터 이름
   * @param args 파라미터 값
   * @return 평가 결과
   */
  public static Boolean evaluate(
      String expression, String[] parameterNames, Object... args) {
    EvaluationContext context = new StandardEvaluationContext();

    for (int i = 0; i < parameterNames.length; i++) {
      context.setVariable(parameterNames[i], args[i]);
    }

    return PARSER.parseExpression(preprocess(expression)).getValue(context, Boolean.class);
  }

  private static String preprocess(String keyExpression) {
    return Arrays.stream(keyExpression.split("(?=#)"))
        .map(token -> token.startsWith("#") ? token : "\"" + token + "\"")
        .collect(Collectors.joining(" + "));
  }
}
