package com.atolcd.alfresco.filer.core.service.impl;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.alfresco.model.ContentModel;
import org.alfresco.service.namespace.QName;

import com.atolcd.alfresco.filer.core.model.FilerException;
import com.atolcd.alfresco.filer.core.model.FilerFolderContext;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;

public class FilerNameBuilder<T> {

  private final T builder;
  private final FilerFolderContext context;

  private String filerName;

  public FilerNameBuilder(final T builder, final FilerFolderContext context) {
    this.builder = builder;
    this.context = context;
  }

  public String getName() {
    return filerName;
  }

  public T with(final String name) {
    if (context.isEnabled()) {
      filerName = name;
    }
    return builder;
  }

  public T with(final Date date, final String dateFormat) {
    return with(() -> {
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
    return with(node -> {
      return nodeNameFormatter.get();
    });
  }

  public T with(final Function<RepositoryNode, String> nodeNameFormatter) {
    String name = null;
    if (context.isEnabled()) {
      name = nodeNameFormatter.apply(context.getNode());
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

  private <C> C getProperty(final QName propertyName, final Class<C> clazz) {
    C value = context.getNode().getProperty(propertyName, clazz);
    if (value == null && context.isEnabled()) {
      throw new FilerException("Could not get property '" + propertyName + "' for node: " + context.getNode());
    }
    return value;
  }
}
