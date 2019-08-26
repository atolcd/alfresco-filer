package com.atolcd.alfresco.filer.core.test.domain.service.impl;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.site.SiteService;

import com.atolcd.alfresco.filer.core.service.impl.FilerBuilder;
import com.atolcd.alfresco.filer.core.service.impl.FilerFolderBuilder;
import com.atolcd.alfresco.filer.core.service.impl.FilerFolderTypeBuilder;
import com.atolcd.alfresco.filer.core.test.domain.content.model.FilerTestConstants;
import com.atolcd.alfresco.filer.core.test.domain.service.FilerTestActionService;
import com.atolcd.alfresco.filer.core.util.FilerNodeUtils;

public class FilerTestActionServiceImpl implements FilerTestActionService {

  @Override
  public FilerFolderTypeBuilder departmentFolder(final FilerBuilder builder) {
    return builder.root(FilerNodeUtils::getSiteNodeRef)
        .folder()
            .named().with(SiteService.DOCUMENT_LIBRARY).get()
        .folder(FilerTestConstants.Department.FolderType.NAME)
            .named().withProperty(FilerTestConstants.Department.Aspect.PROP_NAME);
  }

  @Override
  public FilerFolderBuilder dateSegmentation(final FilerFolderBuilder builder) {
    return builder
        // Segmentation on the imported aspect date or default to creation date
        .condition(node -> node.getProperties().containsKey(FilerTestConstants.ImportedAspect.PROP_DATE))
          .folder().asSegment()
            .named().withPropertyDate(FilerTestConstants.ImportedAspect.PROP_DATE, "yyyy").getOrCreate()
          .folder().asSegment()
            .named().withPropertyDate(FilerTestConstants.ImportedAspect.PROP_DATE, "MM").getOrCreate()
        .conditionReverse()
          .folder().asSegment()
            .named().withPropertyDate(ContentModel.PROP_CREATED, "yyyy").getOrCreate()
          .folder().asSegment()
            .named().withPropertyDate(ContentModel.PROP_CREATED, "MM").getOrCreate()
        .conditionEnd();
  }
}
