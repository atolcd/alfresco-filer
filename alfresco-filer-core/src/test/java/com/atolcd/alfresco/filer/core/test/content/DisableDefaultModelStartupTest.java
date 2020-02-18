package com.atolcd.alfresco.filer.core.test.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.atolcd.alfresco.filer.core.test.framework.PostgreSQLExtension;

public class DisableDefaultModelStartupTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(DisableDefaultModelStartupTest.class);

  private final JupiterTestEngine engine = new JupiterTestEngine();

  @Test
  public void modelDisabled() {
    Events testEvents = executeTestsForClass(DisableDefaultModelStartupConfiguration.class).testEvents();

    logTestFailures(testEvents);

    assertThat(testEvents.started().count()).isEqualTo(1);
    assertThat(testEvents.succeeded().count()).isEqualTo(0);
    assertThat(testEvents.skipped().count()).isEqualTo(0);
    assertThat(testEvents.aborted().count()).isEqualTo(0);
    assertThat(testEvents.failed().count()).isEqualTo(1);

    Throwable error = testEvents.finished().failed().stream().findFirst()
        .map(event -> event.getPayload(TestExecutionResult.class))
        .map(Optional::get) // Event of type FINISHED cannot have empty payload
        .map(TestExecutionResult::getThrowable)
        .map(Optional::get) // Throwable is always present on failed test
        .get();
    assertThat(error.getCause()).isInstanceOf(BeanCreationException.class);
    assertThat(error.getCause()).hasMessageContaining("Class {http://www.atolcd.com/model/filer/1.0}fileable "
        + "has not been defined in the data dictionary");
  }

  @ExtendWith(PostgreSQLExtension.class)
  @ExtendWith(SpringExtension.class)
  @ContextConfiguration({
    "classpath:alfresco/application-context.xml",
    "classpath:context/security-context.xml",
    "classpath:alfresco/module/filer/disable/model-context.xml"
  })
  public static class DisableDefaultModelStartupConfiguration {

    @Test
    public void check() {
      // no-op - context is loaded by SpringExtension
    }
  }

  private EngineExecutionResults executeTestsForClass(final Class<?> testClass) {
    DiscoverySelector selectors = selectClass(testClass);
    LauncherDiscoveryRequest request = request().selectors(selectors).build();
    return EngineTestKit.execute(this.engine, request);
  }

  private static void logTestFailures(final Events testEvents) {
    testEvents.finished().failed().stream()
        .map(event -> event.getPayload(TestExecutionResult.class))
        .map(Optional::get) // Event of type FINISHED cannot have empty payload
        .map(TestExecutionResult::getThrowable)
        .map(Optional::get) // Throwable is always present on failed test
        // TODO wait for https://github.com/pmd/pmd/issues/2255
        .forEach(thrown -> LOGGER.error("Extension test error", thrown)); // NOPMD InvalidLogMessageFormat false positive
  }
}
