/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2015 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.adobe.acs.commons.reporting.impl;

import com.adobe.acs.commons.reporting.ReportUtil;
import com.day.cq.search.QueryBuilder;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ACS AEM Commons - Query CSV Reporter Servlet
 * Servlet end-point used to create Query-based Reports to CSV files based on the underlying resource's configuration.
 */
@SuppressWarnings("serial")
@SlingServlet(
        methods = { "POST" },
        resourceTypes = { "acs-commons/components/utilities/reporter/query-csv-reporter" },
        selectors = { "report" },
        extensions = { "json", "csv"}
)
public class QueryCsvReporterServletImpl extends SlingAllMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(QueryCsvReporterServletImpl.class);

    private static final String FILE_NAME = "fileName";

    private static final String PROPERTY_COLUMNS = "propertyColumns";

    private static final String DEFAULT_FILE_NAME = "query report";

    private static final String CSV_FILE_EXTENSION = ".csv";

    @Reference
    private QueryBuilder queryBuilder;

    @Override
    public final void doPost(final SlingHttpServletRequest request,
                             final SlingHttpServletResponse response) throws IOException {

        final ResourceResolver resourceResolver = request.getResourceResolver();
        final boolean preview = Boolean.parseBoolean(request.getParameter("preview"));

        log.debug("Preview mode: {}", preview);

        final ValueMap properties = this.getProperties(request);
        try {
            final List<Resource> reportResources = ReportUtil.findQueryResources(resourceResolver,
                    properties.get("queryLanguage", Query.JCR_SQL2),
                    properties.get("query", String.class),
                    properties.get("relPath", String.class),
                    queryBuilder);
            String[] propertyColumns = properties.get(PROPERTY_COLUMNS, new String[0]);
            Map<String, Map<String,String>> propertiesMap = ReportUtil
                    .getResourceProperties(reportResources,propertyColumns);

            if (preview) {
                // Handle preview mode
                response.getWriter().print(ReportUtil.getPreviewJSON(reportResources));
            } else {
                /* getting the file name from the resource properties */
                String fileName = properties.get(FILE_NAME, DEFAULT_FILE_NAME) + CSV_FILE_EXTENSION;
                response.setHeader("Content-Type", "text/csv");
                response.setHeader("Content-Disposition", "attachment; filename=\"" +
                        fileName + "\"");

                Writer writer = response.getWriter();
                /* Write the CSV file output to the response */
                ReportUtil.generateCsvFile(writer, propertiesMap, propertyColumns);
            }
        } catch (RepositoryException ex) {
            log.error("Repository error while creating Query Report", ex);
            response.getWriter().print(ReportUtil.getErrorJSON(ex.getMessage()));
        } catch (IOException ex) {
            log.error("IO error while creating Query Report", ex);
            response.getWriter().print(ReportUtil.getErrorJSON(ex.getMessage()));
        } catch (JSONException ex) {
            log.error("JSON error while creating Query Report file", ex);
            response.getWriter().print(ReportUtil.getErrorJSON(ex.getMessage()));
        } catch (InvocationTargetException ex) {
            log.error("Invocation target error while creating Query Report file", ex);
            response.getWriter().print(ReportUtil.getErrorJSON(ex.getMessage()));
        } catch (NoSuchMethodException ex) {
            log.error("No method error while creating Query Report file", ex);
            response.getWriter().print(ReportUtil.getErrorJSON(ex.getMessage()));
        } catch (IllegalAccessException ex) {
            log.error("Illegal access error while creating Query Report file", ex);
            response.getWriter().print(ReportUtil.getErrorJSON(ex.getMessage()));
        }
    }

    /**
     * Gets the properties saved to the Query Packager Page's jcr:content node.
     *
     * @param request the request obj
     * @return a ValueMap representing the properties
     */
    private ValueMap getProperties(final SlingHttpServletRequest request) {
        if (request.getResource().getChild("configuration") == null) {
            log.warn("Query Packager Configuration node could not be found for: {}", request.getResource());
            return new ValueMapDecorator(new HashMap<String, Object>());
        } else {
            return request.getResource().getChild("configuration").adaptTo(ValueMap.class);
        }
    }
}
