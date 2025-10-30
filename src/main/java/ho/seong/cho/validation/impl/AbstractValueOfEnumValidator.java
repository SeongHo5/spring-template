package ho.seong.cho.validation.impl;

import ho.seong.cho.validation.CustomConstraintValidator;
import ho.seong.cho.validation.annotation.ValueOfEnum;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractValueOfEnumValidator<T>
    extends CustomConstraintValidator<ValueOfEnum, T> {

  protected Set<String> allowed;

  @Override
  public final void initialize(ValueOfEnum constraintAnnotation) {
    this.required = constraintAnnotation.required();
    this.allowed =
        Stream.of(constraintAnnotation.type().getEnumConstants())
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());
  }

  protected final void addAllowedValuesViolation(ConstraintValidatorContext context) {
    super.addCustomViolationMessage(
        context, String.format("must be one of %s", this.allowed), null);
  }
}
