package ho.seong.cho.validation.impl;

import jakarta.validation.ConstraintValidatorContext;
import java.util.Collection;

public class ValueOfEnumCollectionValidator
    extends AbstractValueOfEnumValidator<Collection<String>> {

  @Override
  protected boolean isValidInternal(Collection<String> values, ConstraintValidatorContext context) {
    if (values == null || values.isEmpty()) {
      return false;
    }
    return this.acceptedValues.containsAll(values);
  }
}
