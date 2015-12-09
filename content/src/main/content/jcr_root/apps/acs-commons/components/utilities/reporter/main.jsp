<%--
  ~ #%L
  ~ ACS AEM Commons Bundle
  ~ %%
  ~ Copyright (C) 2014 Adobe
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
%><%@page session="false" contentType="text/html" pageEncoding="utf-8"
          import="com.adobe.acs.commons.util.TextUtil"%><%

    /* Page Properties */
    final String pageTitle = TextUtil.getFirstNonEmpty(
            currentPage.getPageTitle(),
            currentPage.getTitle(),
            currentPage.getName());
%>

<h1>AEM Reporter</h1>

<h2><%= xssAPI.encodeForHTML(pageTitle) %></h2>

<div class="notifications">
    <div class="notification preview hidden">
        <h2>Preview</h2>

        <p>The following resource paths will be added to the report file:</p>

        <ul class="filters"></ul>

        <p>If the above list is the expected result, please click on the "Download File" button below.</p>
    </div>

    <%-- <div class="notification success hidden">
        <h2>Success</h2>

        <p>A new ACL package has been created at: <a class="package-manager-link" target="_blank" x-cq-linkchecker="skip" href=""><span
                class="package-path"></span></a></p>

        <ul class="filters"></ul>

        <p>Go to the <a class="package-manager-link" target="_blank" x-cq-linkchecker="skip" href="">CRX Package manager</a> to build and
            download this package.</p>
    </div> --%>

    <div class="notification error hidden">
        <h2>Error</h2>

        <p>An error occurred while generating report.</p>

        <p class="msg"></p>

        <p>Please check the following</p>
        <ul>
            <li>Review your settings and query on this page</li>
        </ul>
    </div>
</div>

<%-- Custom impl of the packager configuration --%>
<cq:include path="configuration" resourceType="<%= component.getPath() + "/configuration" %>"/>

<%-- JavaScript supporting Form Submissions/Response behaviors --%>
<cq:includeClientLib js="acs-commons.utilities.reporter"/>
