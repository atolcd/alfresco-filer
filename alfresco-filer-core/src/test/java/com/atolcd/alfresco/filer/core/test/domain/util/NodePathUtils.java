package com.atolcd.alfresco.filer.core.test.domain.util;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import com.atolcd.alfresco.filer.core.test.framework.LibraryExtension;

public final class NodePathUtils {

  public static Path nodePath(final Optional<String> departmentName, final LocalDateTime date) {
    return nodePath(departmentName.get(), date);
  }

  public static Path nodePath(final String departmentName, final LocalDateTime date) {
    return LibraryExtension.getLibrary().childPath(
        departmentName,
        Integer.toString(date.getYear()),
        date.format(DateTimeFormatter.ofPattern("MM")));
  }

  private NodePathUtils() {}
}
