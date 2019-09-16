package com.atolcd.alfresco.filer.core.test.domain.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.atolcd.alfresco.filer.core.test.framework.DocumentLibraryExtension;

public final class NodePathUtils {

  public static String nodePath(final String departmentName, final LocalDateTime date) {
    return DocumentLibraryExtension.getDocumentLibrary().childPath(
        departmentName,
        Integer.toString(date.getYear()),
        date.format(DateTimeFormatter.ofPattern("MM")));
  }

  private NodePathUtils() {}
}
