package ho.seong.cho.validation.impl;

import ho.seong.cho.validation.CustomConstraintValidator;
import ho.seong.cho.validation.annotation.ValueOfEnum;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractValueOfEnumValidator<T>
    extends CustomConstraintValidator<ValueOfEnum, T> {

  protected Set<String> acceptedValues;

  @Override
  public final void initialize(ValueOfEnum constraintAnnotation) {
    this.required = constraintAnnotation.required();
    this.acceptedValues =
        Stream.of(constraintAnnotation.type().getEnumConstants())
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());
  }
}
