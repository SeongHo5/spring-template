package ho.seong.cho.validation.impl;

import ho.seong.cho.validation.CustomConstraintValidator;
import ho.seong.cho.validation.annotation.ValueOfEnum;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ValueOfEnumCollectionValidator
    extends CustomConstraintValidator<ValueOfEnum, Collection<String>> {

  private Set<String> acceptedValues;

  @Override
  public void initialize(ValueOfEnum constraintAnnotation) {
    this.acceptedValues =
        Stream.of(constraintAnnotation.type().getEnumConstants())
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());
    this.isRequired = constraintAnnotation.required();
  }

  @Override
  protected boolean isValidInternal(Collection<String> values, ConstraintValidatorContext context) {
    if (values == null || values.isEmpty()) {
      return false;
    }
    return this.acceptedValues.containsAll(values);
  }
}
