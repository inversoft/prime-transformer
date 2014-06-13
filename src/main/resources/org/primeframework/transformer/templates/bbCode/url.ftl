[#ftl/]
[#-- @ftlvariable name="body" type="java.lang.String" --]
[#-- @ftlvariable name="attribute" type="java.lang.String" --]
[#-- @ftlvariable name="attributes" type="java.util.Map<java.lang.String, java.lang.String>" --]
[#import "_macros.ftl" as macros/]
<a [@macros.attributes attributes/] [#if attribute??] href="${attribute}" [#else] href="${body}" [/#if]>${body}</a>