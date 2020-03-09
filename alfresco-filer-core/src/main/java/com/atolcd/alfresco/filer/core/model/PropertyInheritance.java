package com.atolcd.alfresco.filer.core.model;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.alfresco.service.namespace.QName;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public class PropertyInheritance {

  @CheckForNull
  private Set<QName> mandatoryAspects; // Always apply
  @CheckForNull
  private Set<QName> optionalAspects; // Ignore if absent in the payload but always apply if present

  public PropertyInheritance() {
    // Use by default, so that in can be used instead of a null value
  }

  private PropertyInheritance(final Set<QName> mandatoryAspects, final Set<QName> optionalAspects) {
    this();
    this.mandatoryAspects = Optional.ofNullable(mandatoryAspects).map(HashSet::new).orElse(null);
    this.optionalAspects = Optional.ofNullable(optionalAspects).map(HashSet::new).orElse(null);
  }

  public PropertyInheritance(final PropertyInheritance inheritance) {
    this(inheritance.mandatoryAspects, inheritance.optionalAspects);
  }

  @Override
  public String toString() {
    return MessageFormat.format("'{'mandatory={0}, optional={1}'}'", mandatoryAspects, optionalAspects);
  }

  public Set<QName> getMandatoryAspects() {
    mandatoryAspects = Optional.ofNullable(mandatoryAspects).orElseGet(HashSet::new);
    return mandatoryAspects;
  }

  public Set<QName> getOptionalAspects() {
    optionalAspects = Optional.ofNullable(optionalAspects).orElseGet(HashSet::new);
    return optionalAspects;
  }
}
