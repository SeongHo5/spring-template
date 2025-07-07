package ho.seong.cho.validation.impl;

import ho.seong.cho.validation.CustomConstraintValidator;
import ho.seong.cho.validation.annotation.Age;
import jakarta.validation.ConstraintValidatorContext;

public class AgeValidator extends CustomConstraintValidator<Age, Number> {

  private static final int MIN_AGE = 15;
  private static final int MAX_AGE = 70;

  @Override
  public void initialize(Age constraintAnnotation) {
    this.required = constraintAnnotation.required();
  }

  @Override
  public boolean isValidInternal(Number value, ConstraintValidatorContext context) {
    final int age = value.intValue();
    return age >= MIN_AGE && age <= MAX_AGE;
  }
}
