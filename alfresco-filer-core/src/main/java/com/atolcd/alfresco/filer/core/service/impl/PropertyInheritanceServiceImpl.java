package com.atolcd.alfresco.filer.core.service.impl;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.dictionary.DictionaryDAO;
import org.alfresco.repo.dictionary.DictionaryListener;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.springframework.beans.factory.InitializingBean;

import com.atolcd.alfresco.filer.core.model.FilerException;
import com.atolcd.alfresco.filer.core.model.PropertyInheritance;
import com.atolcd.alfresco.filer.core.model.PropertyInheritancePayload;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.model.RepositoryNodeDifference;
import com.atolcd.alfresco.filer.core.service.FilerModelService;
import com.atolcd.alfresco.filer.core.service.PropertyInheritanceService;

import edu.umd.cs.findbugs.annotations.Nullable;

public class PropertyInheritanceServiceImpl implements PropertyInheritanceService, InitializingBean, DictionaryListener {

  private final FilerModelService filerModelService;
  private final NodeService nodeService;
  private final DictionaryService dictionaryService;
  private final DictionaryDAO dictionaryDAO;

  @Nullable
  private Collection<QName> inheritedAspects;
  @Nullable
  private Map<QName, QName> inheritedProperties;

  public PropertyInheritanceServiceImpl(final FilerModelService filerModelService, final NodeService nodeService,
      final DictionaryService dictionaryService, final DictionaryDAO dictionaryDAO) {
    this.filerModelService = filerModelService;
    this.nodeService = nodeService;
    this.dictionaryService = dictionaryService;
    this.dictionaryDAO = dictionaryDAO;
  }

  @Override
  public void afterPropertiesSet() {
    dictionaryDAO.registerListener(this);
  }

  @Override
  public void afterDictionaryInit() {
    inheritedAspects = dictionaryService.getSubAspects(filerModelService.getPropertyInheritanceAspect(), true);
    inheritedProperties = getProperties(inheritedAspects);
  }

  private Map<QName, QName> getProperties(final Collection<QName> aspects) {
    Map<QName, QName> result = new HashMap<>();
    for (QName aspect : aspects) {
      Map<QName, QName> properties = dictionaryService.getAspect(aspect).getProperties().keySet().stream()
          .collect(Collectors.toMap(Function.identity(), prop -> aspect));
      result.putAll(properties);
    }
    return result;
  }

  @Override
  public void computeAspectsAndProperties(final NodeRef nodeRef, final RepositoryNode result) {
    Set<QName> aspects = nodeService.getAspects(nodeRef);
    Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);
    // Get inherited aspects and properties
    Set<QName> inheritanceAspects = retainAspects(aspects, inheritedAspects);
    Map<QName, Serializable> inheritanceProperties = retainProperties(properties, inheritedProperties.keySet());
    // Get unset properties from inheritance aspects for removal on the resulting node
    Set<QName> unknownProperties = inheritanceProperties.keySet().stream()
        .map(property -> inheritedProperties.get(property)).distinct()
        .flatMap(aspect -> dictionaryService.getAspect(aspect).getProperties().keySet().stream())
        .filter(property -> !inheritanceProperties.containsKey(property))
        .collect(Collectors.toSet());
    // Update resulting node aspects and properties
    result.getAspects().addAll(inheritanceAspects);
    result.getProperties().putAll(inheritanceProperties);
    result.getProperties().keySet().removeAll(unknownProperties);
  }

  private static Set<QName> retainAspects(final Set<QName> from, final Collection<QName> without) {
    // Create a new Set
    Set<QName> result = new HashSet<>(from);
    result.retainAll(without);
    return result;
  }

  private static Map<QName, Serializable> retainProperties(final Map<QName, Serializable> properties,
      final Set<QName> matchingProperties) {
    // Create a new Map and retain only matching properties (property value can be null)
    Map<QName, Serializable> result = new HashMap<>(properties);
    result.keySet().removeIf(property -> !matchingProperties.contains(property));
    return result;
  }

  @Override
  public void setProperties(final NodeRef nodeRef, final RepositoryNode payload, final PropertyInheritance inheritance) {
    Set<QName> aspects = new HashSet<>();
    aspects.addAll(inheritance.getMandatoryAspects());
    // Retrieve all properties from aspects
    Map<QName, PropertyDefinition> propertyDefinitions = getPropertyDefinitions(aspects);
    // Check for mandatory properties in mandatory aspects that do not have a value
    Set<QName> unknownMandatoryProperties = propertyDefinitions.entrySet().stream()
        .filter(property -> !payload.getProperties().containsKey(property.getKey()))
        .filter(property -> property.getValue().isMandatory())
        .map(Entry::getKey).collect(Collectors.toSet());
    if (!unknownMandatoryProperties.isEmpty()) {
      throw new FilerException("Unknown mandatory property value for: " + unknownMandatoryProperties);
    }
    aspects.addAll(inheritance.getOptionalAspects());
    // Apply mandatory and optional aspects
    Set<QName> properties = getPropertyDefinitions(aspects).keySet();
    // Save inherited properties to repository (property value can be null)
    Map<QName, Serializable> propertyValues = new HashMap<>();
    propertyValues.putAll(payload.getProperties());
    propertyValues.keySet().removeIf(property -> !properties.contains(property));
    nodeService.addProperties(nodeRef, propertyValues);
  }

  private Map<QName, PropertyDefinition> getPropertyDefinitions(final Collection<QName> aspects) {
    return aspects.stream()
        .map(aspect -> dictionaryService.getAspect(aspect).getProperties())
        .flatMap(m -> m.entrySet().stream()).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  @Override
  public PropertyInheritancePayload getPayload(final RepositoryNodeDifference difference) {
    Set<QName> aspectsToAdd = new HashSet<>(difference.getAspectsToAdd());
    aspectsToAdd.retainAll(inheritedAspects);
    Map<QName, Map<QName, Serializable>> added = aspectsToAdd.stream()
        .collect(Collectors.toMap(Function.identity(), v -> new HashMap<>()));
    for (Entry<QName, Serializable> property : difference.getPropertiesToAdd().entrySet()) {
      QName aspect = inheritedProperties.get(property.getKey());
      if (aspect != null) {
        added.computeIfAbsent(aspect, k -> new HashMap<>()) // NOPMD - default instantiation required
            .put(property.getKey(), property.getValue());
      }
    }
    Set<QName> aspectsToRemove = new HashSet<>(difference.getAspectsToRemove());
    aspectsToRemove.retainAll(inheritedAspects);
    Map<QName, Set<QName>> removed = aspectsToRemove.stream()
        .collect(Collectors.toMap(Function.identity(), v -> new HashSet<>()));
    for (QName property : difference.getPropertiesToRemove()) {
      QName aspect = inheritedProperties.get(property);
      // If aspect is already marked for removal, ignore associated property
      if (aspect != null && !removed.containsKey(aspect)) {
        removed.computeIfAbsent(aspect, k -> new HashSet<>()) // NOPMD - default instantiation required
            .add(property);
      }
    }
    return new PropertyInheritancePayload(added, removed);
  }

  @Override
  public void setInheritance(final NodeRef root, final PropertyInheritancePayload payload) {
    if (!payload.isEmpty()) {
      setInheritanceImpl(root, payload);
    }
  }

  private void setInheritanceImpl(final NodeRef parent, final PropertyInheritancePayload payload) {
    Collection<ChildAssociationRef> children = nodeService.getChildAssocs(parent, ContentModel.ASSOC_CONTAINS,
        RegexQNamePattern.MATCH_ALL);
    for (ChildAssociationRef child : children) {
      NodeRef nodeRef = child.getChildRef();
      Set<QName> aspects = nodeService.getAspects(nodeRef);
      boolean cascadeChildren = true;
      if (aspects.contains(filerModelService.getFileableAspect())) {
        filerModelService.runWithoutFileableBehaviour(nodeRef, () -> {
          updateInheritance(nodeRef, payload);
        });
      } else if (aspects.contains(filerModelService.getSegmentAspect())) {
        updateInheritance(nodeRef, payload);
      } else {
        cascadeChildren = false;
      }
      if (cascadeChildren) {
        setInheritanceImpl(nodeRef, payload);
      }
    }
  }

  private void updateInheritance(final NodeRef nodeRef, final PropertyInheritancePayload payload) {
    // Add aspects and associated properties
    for (Entry<QName, Map<QName, Serializable>> aspect : payload.getAdded().entrySet()) {
      nodeService.addAspect(nodeRef, aspect.getKey(), aspect.getValue());
    }
    for (Entry<QName, Set<QName>> aspect : payload.getRemoved().entrySet()) {
      if (aspect.getValue().isEmpty()) {
        // Remove aspect
        nodeService.removeAspect(nodeRef, aspect.getKey());
      } else {
        // Remove properties
        aspect.getValue().stream().forEach(property -> nodeService.removeProperty(nodeRef, property));
      }
    }
  }

  @Override
  public void onDictionaryInit() { // NOPMD - default empty method, nothing to do on dictionary initialization
    // no op
  }

  @Override
  public void afterDictionaryDestroy() { // NOPMD - default empty method, nothing to do after dictionary deletion
    // no op
  }
}
