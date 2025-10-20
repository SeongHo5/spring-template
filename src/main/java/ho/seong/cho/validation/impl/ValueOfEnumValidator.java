package ho.seong.cho.validation.impl;

import jakarta.validation.ConstraintValidatorContext;

public class ValueOfEnumValidator extends AbstractValueOfEnumValidator<String> {

  @Override
  public boolean isValidInternal(String value, ConstraintValidatorContext context) {
    if (value.isBlank()) {
      return false;
    }
    return this.acceptedValues.contains(value);
  }
}
