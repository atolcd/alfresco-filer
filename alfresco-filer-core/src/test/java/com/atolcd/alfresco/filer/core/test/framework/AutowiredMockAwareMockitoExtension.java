package com.atolcd.alfresco.filer.core.test.framework;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;
import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Ensure correct use of mock/spy objects injected by Spring
 *
 * <p>
 * Calling {@link Mockito#spy(Object)} on those {@linkplain Autowired autowired}
 * mocks make them known of Mockito, without other effect.<br>
 * That way, Mockito will check for unnecessary stubbing of the injected mocks.
 * </p>
 *
 * <p>
 * The injected mocks are reseted after each test.
 * </p>
 */
public class AutowiredMockAwareMockitoExtension extends MockitoExtension {

  private List<Object> autowiredMocks;

  @Override
  public void beforeEach(final ExtensionContext context) {
    // Do this before MockitoExtension because it may have initialized beans annotated with @Spy
    autowiredMocks = getAutowiredMocks(context.getRequiredTestInstance());
    // Perform Mockito initialization
    super.beforeEach(context);
    // Make autowired mocks known to Mockito
    autowiredMocks.forEach(instance -> Mockito.spy(instance));
  }

  private static List<Object> getAutowiredMocks(final Object testInstance) {
    List<Field> fields = AnnotationUtils.findAnnotatedFields(testInstance.getClass(),
        Autowired.class, field -> MockUtil.isMock(getFieldValue(testInstance, field)));
    return fields.stream().map(field -> getFieldValue(testInstance, field)).collect(Collectors.toList());
  }

  private static Object getFieldValue(final Object testInstance, final Field field) {
    try {
      field.setAccessible(true);
      return field.get(testInstance);
    } catch (IllegalArgumentException | IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void afterEach(final ExtensionContext context) {
    try {
      super.afterEach(context);
    } finally {
      // Reset autowired mocks even if an exception occurred so that following tests are not affected
      autowiredMocks.forEach(spy -> Mockito.reset(spy));
    }
  }
}
