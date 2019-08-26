package com.atolcd.alfresco.filer.core.test.domain.service;

import com.atolcd.alfresco.filer.core.service.impl.FilerBuilder;
import com.atolcd.alfresco.filer.core.service.impl.FilerFolderBuilder;
import com.atolcd.alfresco.filer.core.service.impl.FilerFolderTypeBuilder;

public interface FilerTestActionService {

  FilerFolderTypeBuilder departmentFolder(FilerBuilder builder);

  FilerFolderBuilder dateSegmentation(FilerFolderBuilder builder);
}
