package com.atolcd.alfresco.filer.core.test.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(LibraryRoleExtension.class)
public @interface TestLibraryRole {

  /**
   * Role name
   */
  String value() default "";
}
