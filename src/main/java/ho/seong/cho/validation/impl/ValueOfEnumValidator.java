package ho.seong.cho.validation.impl;

import ho.seong.cho.validation.CustomConstraintValidator;
import ho.seong.cho.validation.annotation.ValueOfEnum;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ValueOfEnumValidator extends CustomConstraintValidator<ValueOfEnum, String> {

  private Set<String> acceptedValues;

  @Override
  public void initialize(ValueOfEnum constraintAnnotation) {
    this.acceptedValues =
        Stream.of(constraintAnnotation.type().getEnumConstants())
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());
    this.required = constraintAnnotation.required();
  }

  @Override
  public boolean isValidInternal(String value, ConstraintValidatorContext context) {
    if (value.isBlank()) {
      return false;
    }
    return this.acceptedValues.contains(value);
  }
}
