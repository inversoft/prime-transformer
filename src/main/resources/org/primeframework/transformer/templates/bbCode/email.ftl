[#ftl/]
[#-- @ftlvariable name="body" type="java.lang.String" --]
[#-- @ftlvariable name="attribute" type="java.lang.String" --]
[#-- @ftlvariable name="attributes" type="java.util.Map<java.lang.String, java.lang.String>" --]
[#import "_macros.ftl" as macros/]
<a [#if attribute??] href="mailto:${attribute}" [#else] href="${body}" [/#if] [@macros.attributes attributes/]>${body}</a>