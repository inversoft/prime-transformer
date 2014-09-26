[#-- @ftlvariable name="body" type="java.lang.String" --]
[#-- @ftlvariable name="attribute" type="java.lang.String" --]
[#-- @ftlvariable name="attributes" type="java.util.Map<java.lang.String, java.lang.String>" --]
[#import "_macros.ftl" as macros/]
<span [@macros.attributes attributes/] style="text-decoration: line-through">${body}</span>