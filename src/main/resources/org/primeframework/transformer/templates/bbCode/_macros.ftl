[#ftl/]
[#--
 Adds complex attributes in the following format:
   key1="value1" key2="value2" ...
 --]
[#macro attributes attributes]
  [#if attributes??] [#list attributes?keys as attr] ${attr}="${attributes[attr]}" [/#list] [/#if]
[/#macro]