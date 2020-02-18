package com.atolcd.alfresco.filer.core.service.impl;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.alfresco.model.ContentModel;
import org.alfresco.service.namespace.QName;

import com.atolcd.alfresco.filer.core.model.FilerException;
import com.atolcd.alfresco.filer.core.model.FilerFolderContext;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public class FilerNameBuilder<T> {

  private final T builder;
  private final FilerFolderContext context;

  @CheckForNull
  private String filerName;

  public FilerNameBuilder(final T builder, final FilerFolderContext context) {
    this.builder = builder;
    this.context = context;
  }

  public Optional<String> getName() {
    return Optional.ofNullable(filerName);
  }

  public T with(final @CheckForNull String name) {
    if (context.isEnabled()) {
      filerName = name;
    }
    return builder;
  }

  public T with(@CheckForNull final Date date, final String dateFormat) {
    return date == null ? with((String) null) : with(() -> {
      DateFormat formatter = new SimpleDateFormat(dateFormat);
      return formatter.format(date);
    });
  }

  public T with(final String pattern, final QName... properties) {
    return with(() -> {
      List<Serializable> values = new ArrayList<>();
      for (QName property : properties) {
        Serializable value = getProperty(property, Serializable.class);
        values.add(value);
      }
      return MessageFormat.format(pattern, values.toArray());
    });
  }

  public T with(final Supplier<String> nodeNameFormatter) {
    return with(node -> nodeNameFormatter.get());
  }

  public T with(final Function<RepositoryNode, String> nodeNameFormatter) {
    return withContext(filerFolderContext -> nodeNameFormatter.apply(filerFolderContext.getNode()));
  }

  public T withContext(final Function<FilerFolderContext, String> nodeNameFormatter) {
    String name = null;
    if (context.isEnabled()) {
      name = nodeNameFormatter.apply(context);
    }
    return with(name);
  }

  public T withPropertyName() {
    return withProperty(ContentModel.PROP_NAME);
  }

  public T withProperty(final QName propertyName) {
    String name = getProperty(propertyName, String.class);
    return with(name);
  }

  public T withPropertyDate(final QName propertyName, final String dateFormat) {
    Date date = getProperty(propertyName, Date.class);
    return with(date, dateFormat);
  }

  @CheckForNull
  private <C> C getProperty(final QName propertyName, final Class<C> propertyType) {
    Optional<C> value = context.getNode().getProperty(propertyName, propertyType);
    if (!value.isPresent() && context.isEnabled()) {
      throw new FilerException("Could not get property '" + propertyName + "' for node: " + context.getNode());
    }
    return value.orElse(null);
  }
}
