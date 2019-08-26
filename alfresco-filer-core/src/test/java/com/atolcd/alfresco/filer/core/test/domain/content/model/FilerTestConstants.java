package com.atolcd.alfresco.filer.core.test.domain.content.model;

import org.alfresco.service.namespace.QName;

public final class FilerTestConstants {

  public static final String NAMESPACE_URI = "http://www.atolcd.com/model/filer/test/1.0";
  public static final QName MODEL_NAME = QName.createQName(NAMESPACE_URI, "model");

  public static final class Department {

    public static final class FolderType {
      public static final QName NAME = QName.createQName(NAMESPACE_URI, "departmentFolder");

      private FolderType() {}
    }

    public static final class DocumentType {
      public static final QName NAME = QName.createQName(NAMESPACE_URI, "departmentDocument");

      private DocumentType() {}
    }

    public static final class Aspect { //NOPMD - name: not a utility class
      public static final QName NAME = QName.createQName(NAMESPACE_URI, "department");
      public static final QName PROP_NAME = QName.createQName(NAMESPACE_URI, "departmentName");

      private Aspect() {}
    }

    private Department() {}
  }

  public static final class SpecialDocumentType {
    public static final QName NAME = QName.createQName(NAMESPACE_URI, "specialDocument");

    private SpecialDocumentType() {}
  }

  public static final class ImportedAspect {
    public static final QName NAME = QName.createQName(NAMESPACE_URI, "imported");
    public static final QName PROP_DATE = QName.createQName(NAMESPACE_URI, "importedDate");

    private ImportedAspect() {}
  }

  private FilerTestConstants() {}
}
