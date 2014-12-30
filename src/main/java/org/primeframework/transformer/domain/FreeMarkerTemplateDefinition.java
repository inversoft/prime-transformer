package org.primeframework.transformer.domain;

import freemarker.template.Template;

/**
 * @author Daniel DeGroff
 */
public class FreeMarkerTemplateDefinition {

  public boolean bodyRequired = true;
  public boolean closingTagRequired = true;
  public boolean preFormattedBody;

  public String fileName;
  public Template template;

  public FreeMarkerTemplateDefinition(Template template) {
    this.template = template;
  }

  public FreeMarkerTemplateDefinition(String fileName) {
    this.fileName = fileName;
  }

  public FreeMarkerTemplateDefinition(String fileName, boolean bodyRequired, boolean closingTagRequired, boolean preFormattedBody) {
    this.fileName = fileName;
    this.bodyRequired = bodyRequired;
    this.closingTagRequired = closingTagRequired;
    this.preFormattedBody = preFormattedBody;
  }
}
