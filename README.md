# AtolCD Alfresco Filer

This is an Alfresco Content Services module to perform filer operations. It adds the ability to define rules to move
an incoming or an updated document into the desired folder structure based on its characteristics (mainly its type, aspects and metadata).
It offers a fluent API to define each level of the classification and whether they should be created on the fly.
It also allows to inherit specific metadata that are defined at any level of the folder hierarchy to ensure consistency and availability.

# Few things to notice

 * Runs with Alfresco Content Services 6.2 and JDK 11 (compatible with ACS 5.2 and JDK 8)
 * Standard JAR packaging and layout
 * AMP as an assembly
 * Tested with JUnit 5, Mockito 3 and PostgreSQL 10

# Building and testing

The project can be built and tested by running the following Maven command:
~~~
mvn -Pdelivery clean package
~~~

# Artifacts

The artifacts can be added to the dependency of your project in its pom.xml:
~~~
<dependency>
  <groupId>com.atolcd.alfresco.filer</groupId>
  <artifactId>alfresco-filer-core</artifactId>
  <version>0.1.0</version>
</dependency>
~~~

# Using the module

The core of the module is based on Alfresco Content Services policies to detect nodes to be filed and an engine to execute filer actions.

## Filer model

The filer defines 3 concepts:
* a **fileable**: a node (document or folder) that can be automatically filed,
* a **filer subscriber**: a container in which nodes are automatically filed,
* a **filer segment**: a folder that is part of a hierarchy in which a node is filed, it can be deleted automatically if empty

The filer engine uses policies to detect changes in the repository and trigger its own rule mechanism to adapt the node's classification:
* *onCreateChildAssociation* on a filer subscriber, to label the incoming node as fileable,
* *onAddAspect* on a fileable node, to trigger the initial classification,
* *onUpdatePreoperties* and *onMoveNode* on a fileable node, to check for updates that could change the node's classification,
* *onDeleteNode* on a fileable node, to remove a classification left empty.

## Filer action

A filer action is evaluated by the filer engine to determine whether it applies to the node to be filed and then it performs the selected action.

First, it is required to provide the conditions upon which a filer action will be executed.
The matching is actually performed in two passes to allow to quickly bypass classification if the node does not supports a filer action based on some general requirements such as the containing site, its aspects or type.
The second check allows for a more thorough inspection, including for example the properties of the node.
Finally, it is possible to define the action itself. This is indeed the actual classification, which would trigger the navigation or the creation of the folder structure.

Creating a filer action is done by implementing [`FilerAction`] or directly inheriting [`AbstractFilerAction`].

Let's take a simple example where a document that would contain a particular description with a department data (e.g. department: treasury;) created in 2019 would be filed into the "treasury/2019" path inside the site's document library.
Here is the corresponding implementation:
```java
public class DepartmentFilerAction extends AbstractFilerAction {

  @Override
  public boolean supportsActionResolution(final FilerEvent event) {
    return event.getNode().getAspects().contains(ContentModel.ASPECT_TITLED)
        && event.getNode().getType().equals(ContentModel.TYPE_CONTENT);
  }

  @Override
  public boolean supportsActionExecution(final RepositoryNode node) {
    return node.getProperty(ContentModel.PROP_DESCRIPTION, String.class).matches("department:.+;");
  }

  @Override
  protected void execute(final FilerBuilder builder) {
    builder.root(FilerNodeUtils::getSiteNodeRef)
        .folder()
            .named().with(SiteService.DOCUMENT_LIBRARY).get()
        .folder().asSegment()
            .named().with(node -> {
              Pattern regex = Pattern.compile("department:\\s*(.+);");
              Matcher matcher = regex.matcher(node.getProperty(ContentModel.PROP_DESCRIPTION, String.class));
              matcher.find();
              return matcher.group(1);
            }).getOrCreate()
        .folder().asSegment()
            .named().withPropertyDate(ContentModel.PROP_CREATED, "yyyy").getOrCreate()
        .updateAndMove();
  }
}
```
You can also look at [example actions] used in the tests and their corresponding [folder structure creation] put together in a dedicated service.

It is possible to create as many actions as needed. They are automatically registered by the [`FilerRegistry`] if they inherits from [`AbstractFilerAction`].
You just need to define the corresponding Spring bean:
```xml
  <bean id="you.name.it" parent="filer.action.base" class="your.implementation.XXXFilerAction"/>
```
You can also look at [example beans] used in the tests.

Actions are evaluated by the [`FilerService`] in order. They are first sorted by the explicit order defined in the action ([`FilerAction`] implements `Ordered`) and then alphabetically by bean name.
The first action that matches the conditions is selected and its classification is applied.

## Properties inheritance

Another characteristic of this module is the ability to define which properties should be inherited on the fileable node and also on the folder structure.
It uses a specific marker aspect to label which aspects should have their properties duplicated.
First, the properties of the inherited aspects are retrieved from the parent folder to supplement the node being filed.
Then, each level of the classification can define the number of properties they inherit.

For example, instead of using the description property, a custom aspect with a specific department label property can be set directly on the department folder.
In this case any document created in it could also have the property directly added on them to make a search on the department of documents easier.
The corresponding action implementation would look like this:
```java
  @Override
  protected void execute(final FilerBuilder builder) {
    builder.root(FilerNodeUtils::getSiteNodeRef)
        .folder()
            .named().with(SiteService.DOCUMENT_LIBRARY).get()
        .folder(MyModel.TYPE_DEPARTMENT).asSegment()
            .mandatoryPropertyInheritance(MyModel.ASPECT_DEPARTMENT)
            .named().withProperty(MyModel.PROP_DEPARTMENT_LABEL).getOrCreate()
        .folder().asSegment()
            .named().withPropertyDate(ContentModel.PROP_CREATED, "yyyy").getOrCreate()
        .updateAndMove();
  }
}
```

[example actions]: alfresco-filer-core/src/test/java/com/atolcd/alfresco/filer/core/test/domain/action
[folder structure creation]: alfresco-filer-core/src/test/java/com/atolcd/alfresco/filer/core/test/domain/service/impl/FilerTestActionServiceImpl.java
[example beans]: alfresco-filer-core/src/test/resources/context/test-action-context.xml

[`FilerAction`]: alfresco-filer-core/src/main/java/com/atolcd/alfresco/filer/core/model/FilerAction.java
[`AbstractFilerAction`]: alfresco-filer-core/src/main/java/com/atolcd/alfresco/filer/core/model/impl/AbstractFilerAction.java
[`FilerRegistry`]: alfresco-filer-core/src/main/java/com/atolcd/alfresco/filer/core/service/FilerRegistry.java
[`FilerService`]: alfresco-filer-core/src/main/java/com/atolcd/alfresco/filer/core/service/FilerService.java
