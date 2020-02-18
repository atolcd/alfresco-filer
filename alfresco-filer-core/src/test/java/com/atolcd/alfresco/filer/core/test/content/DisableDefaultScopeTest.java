package com.atolcd.alfresco.filer.core.test.content;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.atolcd.alfresco.filer.core.service.FilerRegistry;
import com.atolcd.alfresco.filer.core.test.framework.PostgreSQLExtension;
import com.atolcd.alfresco.filer.core.test.framework.TestApplicationContext;

public class DisableDefaultScopeTest {

  @Nested
  @TestApplicationContext
  public class EnabledDefaultScopeTest {

    @Autowired
    private FilerRegistry filerRegistry;

    @Test
    public void checkEnabled() {
      assertThat(filerRegistry.getScopeLoaders()).isNotEmpty();
    }
  }

  @Nested
  @ExtendWith(PostgreSQLExtension.class)
  @ExtendWith(SpringExtension.class)
  @ContextConfiguration({
    "classpath:alfresco/application-context.xml",
    "classpath:context/security-context.xml",
    "classpath:alfresco/module/filer/disable/scope-context.xml"
  })
  public class DisabledDefaultScopeTest {

    @Autowired
    private FilerRegistry filerRegistry;

    @Test
    public void checkDisabled() {
      assertThat(filerRegistry.getScopeLoaders()).isEmpty();
    }
  }
}
