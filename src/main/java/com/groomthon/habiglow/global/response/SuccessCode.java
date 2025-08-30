package com.groomthon.habiglow.global.response;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SuccessCode {
<<<<<<< HEAD
	ApiSuccessCode value();
=======
	MemberSuccessCode value();
>>>>>>> 803266e19157d4b2789d0538cff2c8d75a3a0abd
}