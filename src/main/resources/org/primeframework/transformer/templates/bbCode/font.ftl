[#ftl/]
[#-- @ftlvariable name="body" type="java.lang.String" --]
[#-- @ftlvariable name="attribute" type="java.lang.String" --]
[#-- @ftlvariable name="attributes" type="java.util.Map<java.lang.String, java.lang.String>" --]
<span [#if attribute??] style="font-family: ${attribute}" [/#if]>${body}</span>