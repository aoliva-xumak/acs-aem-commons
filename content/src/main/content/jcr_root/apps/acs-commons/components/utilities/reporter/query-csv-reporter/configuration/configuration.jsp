<%--
  ~ #%L
  ~ ACS AEM Commons Bundle
  ~ %%
  ~ Copyright (C) 2013 Adobe
  ~ %%
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~      http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  ~ #L%
  --%>
<%@include file="/libs/foundation/global.jsp"%><%
%><%@page session="false" contentType="text/html" pageEncoding="utf-8" %><%

    /* Package Definition */
    final String queryLanguage = properties.get("queryLanguage", "xpath");
    final String relPath = properties.get("relPath", "");
    final String query = properties.get("query", "");

    final String fileName = properties.get("fileName", "query report");
    final String[] propertyColumns = properties.get("propertyColumns", new String[0]);

%>

<h3>Report Definition</h3>
<ul>
    <li>File name: <%= xssAPI.encodeForHTML(fileName) %></li>
    <li>Property Columns: 
        <ul>
            <% for(String property:propertyColumns) {%> 
            <li><%= xssAPI.encodeForHTML(property) %></li> 
            <%}%>
        </ul>
    </li>


</ul>

<h3>Query</h3>
<p>Query Language: <%= xssAPI.encodeForHTML(queryLanguage) %></p>
<p>Query: <%= xssAPI.encodeForHTML(query) %></p>
<p>Relative Path: <%= xssAPI.encodeForHTML(relPath) %></p>

<%-- Common Form (Preview / Create Package) used for submittins Packager requests --%>
<%-- Requires this configuration component have a sling:resourceSuperType of the ACS AEM Commons Packager --%>
<cq:include script="partials/form.jsp"/>
