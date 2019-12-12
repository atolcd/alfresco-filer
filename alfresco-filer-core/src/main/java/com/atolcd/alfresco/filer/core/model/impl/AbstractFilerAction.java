package com.atolcd.alfresco.filer.core.model.impl;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;

import com.atolcd.alfresco.filer.core.model.FilerAction;
import com.atolcd.alfresco.filer.core.model.FilerException;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.service.FilerRegistry;
import com.atolcd.alfresco.filer.core.service.FilerService;
import com.atolcd.alfresco.filer.core.service.impl.FilerBuilder;

public abstract class AbstractFilerAction implements FilerAction, InitializingBean, BeanNameAware {

  private static final Comparator<FilerAction> NATURAL_ORDER_COMPARATOR =
      Comparator.comparing(FilerAction::getOrder).thenComparing(FilerAction::getName);

  private FilerRegistry filerRegistry;
  private FilerService filerService;
  private String name;

  @Override
  public void afterPropertiesSet() {
    filerRegistry.registerAction(this);
  }

  @Override
  public final void execute(final RepositoryNode node) {
    FilerBuilder builder = new FilerBuilder(filerService, node);
    execute(builder);
  }

  protected abstract void execute(FilerBuilder builder);

  protected void deny(final FilerBuilder builder, final Predicate<RepositoryNode> check) {
    if (check.test(builder.getNode())) {
      throw new FilerException("Filer action DENIED: " + name);
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getOrder() {  // NOPMD - 0 is the default value
    return 0;
  }

  @Override
  public boolean equals(final Object object) {
    if (object == null) {
      return false;
    }
    if (object == this) {
      return true;
    }
    if (object instanceof AbstractFilerAction) {
      AbstractFilerAction other = (AbstractFilerAction) object;
      return Objects.equals(getOrder(), other.getOrder())
          && Objects.equals(getName(), other.getName());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getOrder(), name);
  }

  @Override
  public int compareTo(final FilerAction other) {
    return NATURAL_ORDER_COMPARATOR.compare(this, other);
  }

  @Override
  public String toString() {
    return Stream.of(name, getOrder()).map(String::valueOf).collect(Collectors.joining(", "));
  }

  public void setFilerRegistry(final FilerRegistry filerRegistry) {
    this.filerRegistry = filerRegistry;
  }

  public void setFilerService(final FilerService filerService) {
    this.filerService = filerService;
  }

  @Override
  public void setBeanName(final String beanName) {
    this.name = beanName;
  }
}
