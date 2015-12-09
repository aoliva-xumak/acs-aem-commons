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
package com.adobe.acs.commons.reporting;

import com.adobe.acs.commons.util.ParameterUtil;
import com.adobe.acs.commons.util.TypeUtil;
import com.adobe.cq.commerce.pim.common.Csv;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * ACS AEM Commons - Report Util methods
 * Utility class for creating report files.
 */
public class ReportUtil {

    private static final String QUERY_BUILDER = "queryBuilder";

    private static final String LIST = "list";

    private static final String JSON_EXCEPTION_MSG =
            "{\"status\": \"error\", \"msg\": \"Error creating JSON response.\"}";

    private static final Logger log = LoggerFactory.getLogger(ReportUtil.class);

    private ReportUtil() {
    }

    /**
     * Find all the resources  that will be added to the file.
     *
     * @param resourceResolver the resource resolver to find the resources
     * @param language         the Query language
     * @param statement        the Query statement
     * @param relPath          the relative path to resolve against query result nodes for package resources
     * @param queryBuilder     the Query Builder to be used for executing the queries.
     * @return a unique set of paths to include in the package
     * @throws RepositoryException
     */
    public static List<Resource> findQueryResources(final ResourceResolver resourceResolver,
                                                    final String language,
                                                    final String statement,
                                                    final String relPath,
                                                    final QueryBuilder queryBuilder) throws RepositoryException {

        final List<Resource> resources = new ArrayList<Resource>();
        /* Handles the case when is configured to use the Query Builder to find the resources */
        if (language.equals(QUERY_BUILDER)) {
            final String[] lines = StringUtils.split(statement, '\n');
            final Map<String, String> params = ParameterUtil.toMap(lines, "=", false, null, true);

            // ensure all results are returned
            params.put("p.limit", "-1");

            final com.day.cq.search.Query query = queryBuilder.createQuery(PredicateGroup.create(params), resourceResolver.adaptTo(Session.class));
            final List<Hit> hits = query.getResult().getHits();
            for (final Hit hit : hits) {
                resources.add(hit.getResource());
            }
        /* When is configured to use a list, it will retrieve the resources
           for the paths configured  and the relative path */
        } else if (language.equals(LIST)) {
            if (StringUtils.isNotBlank(statement)) {
                final String[] lines = statement.split("[,;\\s\\n\\t]+");

                for (String line : lines) {
                    if (StringUtils.isNotBlank(line)) {
                        final Resource resource = resourceResolver.getResource(line);
                        /* Add the resource of the path configured on the Relative path property */
                        final Resource relativeAwareResource = getRelativeAwareResource(resource, relPath);

                        if (relativeAwareResource != null) {
                            resources.add(relativeAwareResource);
                        }
                    }
                }
            }
        } else {
            /* In case another type of Query like JCR-SQL, JCR-SQL2 or XPATH is configured */
            Iterator<Resource> resourceIterator = resourceResolver.findResources(statement, language);

            while (resourceIterator.hasNext()) {
                final Resource resource = resourceIterator.next();
                /* Add the resource of the path configured on the Relative path property */
                final Resource relativeAwareResource = getRelativeAwareResource(resource, relPath);

                if (relativeAwareResource != null) {
                    resources.add(relativeAwareResource);
                }
            }
        }

        return resources;
    }

    /**
     * Returns a Map containing the path of the resource as keys and a property/value map of the resource based
     * on the list of properties provided.
     * @param resourceList List that contains the resources to be processed to get the property values.
     * @param propertyList list of properties to search on each resource listed to get their values.
     * @return a Map containing a nested map with the properties and its values of each of the listed resources.
     * @throws RepositoryException
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    public static Map<String,Map<String,String>> getResourceProperties(List<Resource> resourceList,
                                                                       String[] propertyList)
            throws RepositoryException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Map<String, Map<String,String>> resourcePropertiesMap = new HashMap<String, Map<String,String>>();
        for(Resource resource:resourceList) {
            ValueMap resourceProperties = resource.adaptTo(ValueMap.class);
            /* checking for the resourceType of the resource for checking where it has to get the property */
            Map<String, String> propertyMap = new HashMap<String, String>();
            for(String property:propertyList) {

                String propValue = getPropertyValue(resourceProperties, property);
                propertyMap.put(property, propValue);
            }
            resourcePropertiesMap.put(resource.getPath(), propertyMap);
        }

        return resourcePropertiesMap;
    }

    /**
     * Returns the string representation of the values of the given property, in the case of having multiple values,
     * gives a comma-separated list of them enclosed on quotes (""), if the property is not found or empty it retuns
     * an empty string.
     * @param properties ValueMap containing the properties and values of a resource.
     * @param property the property that has to be searched to get the value.
     * @return String representation of the property value, empty string if the value is not found.
     * @throws RepositoryException
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    private static String getPropertyValue(ValueMap properties, String property) throws RepositoryException,
            IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        String valueString = "";
        if(properties.containsKey(property)) {
            Object  propertyObj = properties.get(property);
            if(propertyObj != null) {
                valueString = TypeUtil.toString(propertyObj);
            }
        }
        /* if its and array string value replace the braces with quotes */
        if(valueString.startsWith("[") && valueString.endsWith("]")) {
            valueString = valueString.replace("[", "\"");
            valueString = valueString.replace("]", "\"");
        }
        return valueString;
    }

    /**
     * Generates the CSV report file and writes it on the response.
     * @param writer response writer that will be used for the output of the file.
     * @param resourceMap map with the resources and its properties.
     * @param propertyColumns an array with the properties of the resources that will be presented
     * on individual columns.
     * @throws RepositoryException
     * @throws IOException
     */
    public static void generateCsvFile(Writer writer, Map<String, Map<String,String>> resourceMap,
                                       String[] propertyColumns) throws RepositoryException, IOException {
        Csv csvWriter = new Csv();
        csvWriter.writeInit(writer);
        if(!resourceMap.isEmpty()) {
		       /* Building table columns */
            ArrayList<String> columns = new ArrayList<String>();
            columns.add("Path");
            if(propertyColumns.length > 0){
                for(String column:propertyColumns) {
                    columns.add(column);
                }
            }
            csvWriter.writeRow(columns.toArray(new String[columns.size()]));

	            /* Adding entries */
            for(Map.Entry<String,Map<String,String>> entry:resourceMap.entrySet()) {
                ArrayList<String> entryValues = new ArrayList<String>();
	                /* Adding the path at the start of entry */
                entryValues.add(entry.getKey());

                Map<String,String> propertyMap = entry.getValue();
                if(!propertyMap.isEmpty()){
	                    /* Ensuring the property values will go in the correct order */
                    for(int index=0; index < propertyColumns.length; index++) {
                        String property = propertyColumns[index];
                        String propValue = propertyMap.get(property);
                        entryValues.add(propValue);
                    }
                }
                csvWriter.writeRow(entryValues.toArray(new String[entryValues.size()]));
            }

        } else {
            csvWriter.writeRow("Could not generate report.");
        }
		    /* Closing CSV writer */
        csvWriter.close();
    }


    /**
     * Returns the JSON to return reporting what the packager definition will include for filterSet roots.
     *
     * @param resourceList the resources that are on the list provided
     * @return a string representation of JSON to write to response
     * @throws JSONException
     */
    public static String getPreviewJSON(final List<Resource> resourceList) throws JSONException {
        final JSONObject json = new JSONObject();

        json.put("status", "preview");
        json.put("resourceList", new JSONArray());

        for (final Resource currentResource: resourceList) {
            final JSONObject tmp = new JSONObject();
            tmp.put("resourcePath", currentResource.getPath());

            json.accumulate("resourceList", tmp);
        }

        return json.toString();
    }

    /**
     * Returns the JSON to return in the event of an unsuccessful packaging.
     *
     * @param msg the error message to display
     * @return a string representation of JSON to write to response
     */
    public static String getErrorJSON(final String msg) {
        final JSONObject json = new JSONObject();
        try {
            json.put("status", "error");
            json.put("msg", msg);
            return json.toString();
        } catch (JSONException e) {
            log.error("Error creating JSON Error response message: {}", e.getMessage());
            return JSON_EXCEPTION_MSG;
        }
    }

    /**
     * Get the relative resource of the given resource if it resolves otherwise
     * the provided resource.
     *
     * @param resource         the resource
     * @param relPath          the relative path to resolve against the resource
     * @return the relative resource if it resolves otherwise the resource
     */
    public static Resource getRelativeAwareResource(final Resource resource, final String relPath) {
        if (resource != null) {
            if (StringUtils.isNotBlank(relPath)) {
                final Resource relResource = resource.getChild(relPath);

                if (relResource != null) {
                    return relResource;
                }
            }
        }

        return resource;
    }


}
