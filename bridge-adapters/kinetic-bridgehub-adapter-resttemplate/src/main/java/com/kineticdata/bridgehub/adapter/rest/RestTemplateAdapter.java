package com.kineticdata.bridgehub.adapter.rest;

import com.kineticdata.bridgehub.adapter.BridgeAdapter;
import com.kineticdata.bridgehub.adapter.BridgeError;
import com.kineticdata.bridgehub.adapter.BridgeRequest;
import com.kineticdata.bridgehub.adapter.BridgeUtils;
import com.kineticdata.bridgehub.adapter.Count;
import com.kineticdata.bridgehub.adapter.Record;
import com.kineticdata.bridgehub.adapter.RecordList;
import com.kineticdata.commons.v1.config.ConfigurableProperty;
import com.kineticdata.commons.v1.config.ConfigurablePropertyMap;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.LoggerFactory;

public class RestTemplateAdapter implements BridgeAdapter {
    /*----------------------------------------------------------------------------------------------
     * PROPERTIES
     *--------------------------------------------------------------------------------------------*/

    /** Defines the adapter display name */
    public static final String NAME = "Rest Template Bridge";

    /** Defines the logger */
    protected static final org.slf4j.Logger logger = LoggerFactory.getLogger(RestTemplateAdapter.class);

    /** Adapter version constant. */
    public static String VERSION = "1.0.0";

    private String username;
    private String password;
    private String restEndpoint;

    /** Defines the collection of property names for the adapter */
    public static class Properties {
        public static final String USERNAME = "Username";
        public static final String PASSWORD = "Password";
    }

    private final ConfigurablePropertyMap properties = new ConfigurablePropertyMap(
        new ConfigurableProperty(Properties.USERNAME).setIsRequired(true),
        new ConfigurableProperty(Properties.PASSWORD).setIsRequired(true).setIsSensitive(true)
    );

    /**
     * Structures that are valid to use in the bridge
     */
    public static final List<String> VALID_STRUCTURES = Arrays.asList(new String[] {
        "FirstStructure","SecondStructure"
    });

    /*---------------------------------------------------------------------------------------------
     * SETUP METHODS
     *-------------------------------------------------------------------------------------------*/

    @Override
    public void initialize() throws BridgeError {
        this.username = properties.getValue(Properties.USERNAME);
        this.password = properties.getValue(Properties.PASSWORD);
        this.restEndpoint = "http://restendpoint.com/sitelocation";
        testAuthenticationValues(this.restEndpoint, this.username, this.password);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public void setProperties(Map<String,String> parameters) {
        properties.setValues(parameters);
    }

    @Override
    public ConfigurablePropertyMap getProperties() {
        return properties;
    }

    /*---------------------------------------------------------------------------------------------
     * IMPLEMENTATION METHODS
     *-------------------------------------------------------------------------------------------*/

    @Override
    public Count count(BridgeRequest request) throws BridgeError {
        // Check if the inputted structure is valid. If it isn't, throw a BridgeError.
        if (!VALID_STRUCTURES.contains(request.getStructure())) {
            throw new BridgeError("Invalid Structure: '"+request.getStructure()+"' is not a valid structure");
        }

        // Parse the query and exchange out any parameters with their parameter values.
        // ie. change the query username=<%=parameter["Username"]%> to username=test.user
        // where parameter["Username"]=test.user
        RestTemplateQualificationParser parser = new RestTrainingQualificationParser();
        String query = parser.parse(request.getQuery(),request.getParameters());

        // Build up the url that you will use to retrieve the source data. Use the query variable
        // instead of request.getQuery() to get a query without parameter placeholders.
        String url = this.restEndpoint+"/path/to/count/endpoint?query="+escapeQuery(query);

        // Initialize the HTTP Client, Response, and Get objects.
        HttpClient client = HttpClients.createDefault();
        HttpResponse response;
        HttpGet get = new HttpGet(url);

        // Append the authentication to the call. This example uses Basic Authentication but other
        // types can be added as HTTP GET or POST headers as well.
        get = addBasicAuthenticationHeader(get, this.username, this.password);

        // Make the call to the REST source to retrieve data and convert the response from an
        // HttpEntity object into a Java string so more response parsing can be done.
        String output = "";
        try {
            response = client.execute(get);
            HttpEntity entity = response.getEntity();
            output = EntityUtils.toString(entity);
            logger.trace("Request response code: "+response.getStatusLine().getStatusCode());
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new BridgeError("Unable to make a connection to the REST Service");
        }
        logger.trace("REST Service Count - Raw Output: "+output);

        // Parse the Response String into a JSON Object
        JSONObject json = (JSONObject)JSONValue.parse(output);
        // Get the count value from the response object
        Object countObj = json.get("count");
        // Assuming that the countObj is a string, parse it to an integer
        Integer count = Integer.valueOf((String)countObj);

        // Create and return a Count object.
        return new Count(count);
    }

    @Override
    public Record retrieve(BridgeRequest request) throws BridgeError {
        // Check if the inputted structure is valid. If it isn't, throw a BridgeError.
        if (!VALID_STRUCTURES.contains(request.getStructure())) {
            throw new BridgeError("Invalid Structure: '"+request.getStructure()+"' is not a valid structure");
        }

        // Parse the query and exchange out any parameters with their parameter values.
        // ie. change the query username=<%=parameter["Username"]%> to username=test.user
        // where parameter["Username"]=test.user
        RestTemplateQualificationParser parser = new RestTrainingQualificationParser();
        String query = parser.parse(request.getQuery(),request.getParameters());

        // Build up the url that you will use to retrieve the source data. Use the query variable
        // instead of request.getQuery() to get a query without parameter placeholders. If the query
        // contains an id that needs to be on the url path, retrieve it from the query and then
        // append it to the url as a path object.
        String recordId = null;
        // Retrieve a recordId from the query. Assumes the id is passed in the form of id=1234abcd
        // where the recordId is equal to 1234abcd
        if (request.getQuery().matches("[Ii][Dd]=.*?(?:$|&)")) {
            Pattern p = Pattern.compile("[Ii][Dd]=(.*?)(?:$|&)");
            Matcher m = p.matcher(query);
            if (m.find()) recordId = m.group(1);
        }
        String url = this.restEndpoint+"/path/to/retrieve/record/"+recordId;

        // Initialize the HTTP Client, Response, and Get objects.
        HttpClient client = HttpClients.createDefault();
        HttpResponse response;
        HttpGet get = new HttpGet(url);

        // Append the authentication to the call. This example uses Basic Authentication but other
        // types can be added as HTTP GET or POST headers as well.
        get = addBasicAuthenticationHeader(get, this.username, this.password);

        // Make the call to the REST source to retrieve data and convert the response from an
        // HttpEntity object into a Java string so more response parsing can be done.
        String output = "";
        try {
            response = client.execute(get);
            HttpEntity entity = response.getEntity();
            output = EntityUtils.toString(entity);
            logger.trace("Request response code: "+response.getStatusLine().getStatusCode());
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new BridgeError("Unable to make a connection to the REST Service");
        }
        logger.trace("Rest Service Retrieve - Raw Output: "+output);

        // Parse the Response String into a JSON Object
        JSONObject json = (JSONObject)JSONValue.parse(output);
        // Retrieve the record that was returned
        JSONObject record = (JSONObject)json.get("record");
        // Create and return a Record object.
        return new Record(record, request.getFields());
    }

    @Override
    public RecordList search(BridgeRequest request) throws BridgeError {
        // Check if the inputted structure is valid. If it isn't, throw a BridgeError.
        if (!VALID_STRUCTURES.contains(request.getStructure())) {
            throw new BridgeError("Invalid Structure: '"+request.getStructure()+"' is not a valid structure");
        }

        // Parse the query and exchange out any parameters with their parameter values.
        // ie. change the query username=<%=parameter["Username"]%> to username=test.user
        // where parameter["Username"]=test.user
        RestTemplateQualificationParser parser = new RestTrainingQualificationParser();
        String query = parser.parse(request.getQuery(),request.getParameters());

        // Build up the url that you will use to retrieve the source data. Use the query variable
        // instead of request.getQuery() to get a query without parameter placeholders.
        String url = this.restEndpoint+"/path/to/search/records?"+escapeQuery(query);

        // Initialize the HTTP Client, Response, and Get objects.
        HttpClient client = HttpClients.createDefault();
        HttpResponse response;
        HttpGet get = new HttpGet(url);

        // Append the authentication to the call. This example uses Basic Authentication but other
        // types can be added as HTTP GET or POST headers as well.
        get = addBasicAuthenticationHeader(get, this.username, this.password);

        // Make the call to the REST source to retrieve data and convert the response from an
        // HttpEntity object into a Java string so more response parsing can be done.
        String output = "";
        try {
            response = client.execute(get);
            HttpEntity entity = response.getEntity();
            output = EntityUtils.toString(entity);
            logger.trace("Request response code: "+response.getStatusLine().getStatusCode());
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new BridgeError("Unable to make a connection to the REST Service");
        }
        logger.trace("Rest Service Search - Raw Output: "+output);

        // Option #1: JSON Parsing

        // Parse the Response String into a JSON Object
        JSONObject json = (JSONObject)JSONValue.parse(output);
        // Get an array of objects from the parsed json
        JSONArray records = (JSONArray)json.get("records");
        // Create a list of Record objects from the returned JSON Array
        List<Record> recordList = new ArrayList<Record>();
        for (Object o : records) {
            // Convert the standard java object to a JSONObject
            JSONObject jsonObject = (JSONObject)o;
            // Create a record based on that JSONObject and add it to the list of records
            recordList.add(new Record(jsonObject));
        }
        // Create the metadata that needs to be returned.
        Map<String,String> metadata = new LinkedHashMap<String,String>();
        metadata.put("count",String.valueOf(records.size()));
        metadata.put("size", String.valueOf(records.size()));

        return new RecordList(request.getFields(), recordList, metadata);
    }

    /*----------------------------------------------------------------------------------------------
     * PRIVATE HELPER METHODS
     *--------------------------------------------------------------------------------------------*/

    private void testAuthenticationValues(String restEndpoint, String username, String password) throws BridgeError {
        logger.debug("Testing the authentication credentials");
        HttpGet get = new HttpGet(String.format("%s/path/to/authentication/check",restEndpoint));
        get = addBasicAuthenticationHeader(get, this.username, this.password);

        HttpClient client = HttpClients.createDefault();
        HttpResponse response;
        try {
            response = client.execute(get);
            HttpEntity entity = response.getEntity();
            EntityUtils.consume(entity);
            if (response.getStatusLine().getStatusCode() == 401) {
                throw new BridgeError("Unauthorized: The inputted Username/Password combination is not valid.");
            }
        }
        catch (IOException e) {
            logger.error(e.getMessage());
            throw new BridgeError("Unable to make a connection to properly to the Rest Service.");
        }
    }

    private HttpGet addBasicAuthenticationHeader(HttpGet get, String username, String password) {
        String creds = username + ":" + password;
        byte[] basicAuthBytes = Base64.encodeBase64(creds.getBytes());
        get.setHeader("Authorization", "Basic " + new String(basicAuthBytes));

        return get;
    }

    private HttpGet addTokenAuthenticationHeader(HttpGet get, String token) {
        get.setHeader("Authorization","Bearer " + token );

        return get;
    }

    // Escape query helper method that is used to escape queries that have spaces
    // and other characters that need escaping to form a complete URL
    private String escapeQuery(String query) {
        String[] qSplit = query.split("&");
        for (int i=0;i<qSplit.length;i++) {
            String qPart = qSplit[i];
            String[] keyValuePair = qPart.split("=");
            String key = keyValuePair[0].trim().replaceAll(" ","+");
            String value = keyValuePair.length > 1 ? StringUtils.join(Arrays.copyOfRange(keyValuePair, 1, keyValuePair.length),"=") : "";
            qSplit[i] = key+"="+URLEncoder.encode(value);
        }
        return StringUtils.join(qSplit,"&");
    }


}