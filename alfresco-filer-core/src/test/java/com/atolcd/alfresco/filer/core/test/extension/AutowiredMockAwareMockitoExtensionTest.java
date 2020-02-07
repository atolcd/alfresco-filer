package com.atolcd.alfresco.filer.core.test.extension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;
import org.mockito.Mockito;
import org.mockito.exceptions.misusing.UnnecessaryStubbingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.atolcd.alfresco.filer.core.test.framework.AutowiredMockAwareMockitoExtension;

public class AutowiredMockAwareMockitoExtensionTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(AutowiredMockAwareMockitoExtensionTest.class);

  private final JupiterTestEngine engine = new JupiterTestEngine();

  @Test
  public void necessaryStubbing() {
    Events testEvents = executeTestsForClass(NecessaryStubbingTestClass.class).tests();

    logTestFailures(testEvents);

    assertThat(testEvents.started().count()).isEqualTo(2);
    assertThat(testEvents.succeeded().count()).isEqualTo(2);
    assertThat(testEvents.skipped().count()).isEqualTo(0);
    assertThat(testEvents.aborted().count()).isEqualTo(0);
    assertThat(testEvents.failed().count()).isEqualTo(0);
  }

  @ExpectedExceptionAutowiredMockAwareMockitoExtensionAnnotation
  public static class NecessaryStubbingTestClass {

    @Autowired
    private Dummy dummy;

    @Test
    @Order(1)
    public void necessaryStubbing() {
      Mockito.doAnswer(invocation -> {
        return null;
      }).when(dummy).foo();

      dummy.foo();
    }

    @Test
    @Order(2)
    public void verifyStubbingIsResetted() {
      // no-op - Work is done by ExpectedExceptionAutowiredMockAwareMockitoExtension
    }
  }

  @Test
  public void unnecessaryStubbing() {
    Events testEvents = executeTestsForClass(UnnecessaryStubbingTestClass.class).tests();

    logTestFailures(testEvents);

    assertThat(testEvents.started().count()).isEqualTo(2);
    assertThat(testEvents.succeeded().count()).isEqualTo(2);
    assertThat(testEvents.skipped().count()).isEqualTo(0);
    assertThat(testEvents.aborted().count()).isEqualTo(0);
    assertThat(testEvents.failed().count()).isEqualTo(0);
  }

  @ExpectedExceptionAutowiredMockAwareMockitoExtensionAnnotation
  public static class UnnecessaryStubbingTestClass {

    @Autowired
    private Dummy dummy;

    @Test
    @Order(1)
    @ExpectedException(type = UnnecessaryStubbingException.class)
    public void unnecessaryStubbing() {
      Mockito.doAnswer(invocation -> {
        return null;
      }).when(dummy).foo();
    }

    @Test
    @Order(2)
    public void verifyUnnecessaryStubbingDoesNotLeakOnOtherTest() {
      // no-op - Work is done by ExpectedExceptionAutowiredMockAwareMockitoExtension
    }
  }

  @Test
  public void stubbing() {
    Events testEvents = executeTestsForClass(StubbingTestClass.class).tests();

    logTestFailures(testEvents);

    assertThat(testEvents.started().count()).isEqualTo(2);
    assertThat(testEvents.succeeded().count()).isEqualTo(2);
    assertThat(testEvents.skipped().count()).isEqualTo(0);
    assertThat(testEvents.aborted().count()).isEqualTo(0);
    assertThat(testEvents.failed().count()).isEqualTo(0);
  }

  @ExpectedExceptionAutowiredMockAwareMockitoExtensionAnnotation
  public static class StubbingTestClass {

    @Autowired
    private Dummy dummy;

    @Test
    @Order(1)
    public void stubbing() {
      Mockito.doAnswer(invocation -> {
        return true;
      }).when(dummy).bar();

      if (!dummy.bar()) {
        Assertions.fail();
      }
    }

    @Test
    @Order(2)
    public void verifyStubbingIsResetted() {
      if (dummy.bar()) {
        Assertions.fail();
      }
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

  /**
   * Expected exception catcher inspired by : {@link io.github.glytching.junit.extension.exception.ExpectedExceptionExtension}
   */
  public static class ExpectedExceptionAutowiredMockAwareMockitoExtension extends AutowiredMockAwareMockitoExtension {

    @Override
    public void afterEach(final ExtensionContext context) {
      Optional<ExpectedException> annotation = findAnnotation(context.getTestMethod(), ExpectedException.class);
      boolean catched = false;
      try {
        super.afterEach(context);
      } catch (Exception exception) { //NOPMD - Type is checked dynamically below
        catched = true;
        if (annotation.isPresent()) {
          if (!annotation.get().type().isAssignableFrom(exception.getClass())) {
            Assertions.fail("Exception of type " + annotation.get().type() + "expected", exception);
          }
        } else {
          // No exception expected, behave normally
          throw exception;
        }
      }
      if (annotation.isPresent() && !catched) {
        Assertions.fail("No exception was thrown. Expected exception was: " + annotation.get().type());
      }
    }
  }

  public static class Dummy {

    public void foo() {
      // no-op - Empty method to stub
    }

    public boolean bar() {
      return false;
    }
  }

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @TestMethodOrder(OrderAnnotation.class)
  @ExtendWith(ExpectedExceptionAutowiredMockAwareMockitoExtension.class)
  @ExtendWith(SpringExtension.class)
  @ContextConfiguration("classpath:context/test-mocking-context.xml")
  public @interface ExpectedExceptionAutowiredMockAwareMockitoExtensionAnnotation {}

  @Target({ ElementType.METHOD, ElementType.TYPE })
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  public @interface ExpectedException {

    Class<? extends Throwable> type();
  }
}
