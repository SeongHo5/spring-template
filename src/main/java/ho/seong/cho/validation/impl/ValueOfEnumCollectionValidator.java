package ho.seong.cho.validation.impl;

import jakarta.validation.ConstraintValidatorContext;
import java.util.Collection;

public final class ValueOfEnumCollectionValidator
    extends AbstractValueOfEnumValidator<Collection<String>> {

  @Override
  protected boolean isValidInternal(Collection<String> values, ConstraintValidatorContext context) {
    if (values == null || values.isEmpty()) {
      return false;
    }
    if (!this.allowed.containsAll(values)) {
      super.addAllowedValuesViolation(context);
      return false;
    }
    return true;
  }
}
