package com.atolcd.alfresco.filer.core.test.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(PostgreSQLExtension.class)
@ExtendWith(SpringExtension.class)
@ContextConfiguration({
  "classpath:alfresco/application-context.xml",
  "classpath:context/test-service-context.xml",
  "classpath:context/test-model-context.xml",
  "classpath:context/test-action-context.xml"
})
public @interface ApplicationContextAwareTest {}
