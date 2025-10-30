package ho.seong.cho.validation.impl;

import jakarta.validation.ConstraintValidatorContext;

public final class ValueOfEnumValidator extends AbstractValueOfEnumValidator<String> {

  @Override
  public boolean isValidInternal(String value, ConstraintValidatorContext context) {
    if (value.isBlank()) {
      return false;
    }
    if (!this.allowed.contains(value)) {
      super.addAllowedValuesViolation(context);
      return false;
    }
    return true;
  }
}
