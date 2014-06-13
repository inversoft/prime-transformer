[#ftl/]
[#-- @ftlvariable name="body" type="java.lang.String" --]
[#-- @ftlvariable name="attribute" type="java.lang.String" --]
[#-- @ftlvariable name="attributes" type="java.util.Map<java.lang.String, java.lang.String>" --]
[#import "_macros.ftl" as macros/]
[#if attribute?ends_with("%") || attribute?ends_with("px")]
  <span [#if attribute??] style="font-size: ${attribute}" [/#if] [@macros.attributes attributes /]>${body}</span>
[#else]
  <span [#if attribute??] style="font-size: ${attribute}%" [/#if] [@macros.attributes attributes /]>${body}</span>
[/#if]
