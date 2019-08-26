package com.atolcd.alfresco.filer.core.test.util;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(PostgreSQLExtension.class)
@ExtendWith(SpringExtension.class)
@ContextConfiguration({
  "classpath:alfresco/application-context.xml"
})
public interface ApplicationContextAwareTest {}
