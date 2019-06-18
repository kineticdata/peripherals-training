# REST Template Adapter
A sample REST Bridgehub Adapter that can be used as a starting point for developing a new adapter to connect and return information from a REST Web Service.

## Explanation of Bridgehub Adapter parts
### Configuration Options
The ConfigurablePropertyMap defines the properties that need to be set by the user when a new instance of the Adapter is created in the Bridgehub console. Most commonly this would include properties like Username and Password while also allowing a user to select something configurable like an Instance or Region depending on the API that is being accessed.
### Initialize method
A method that runs once on Bridgehub startup or before invoking a count/retrieve/search method if a bridge is uninitialized. The initialization method should handle any of the following things (if they are implemented in your Adapter):
  - One time preloading operations (such as caching of forms/fields) 
  - Validating that configurable property values are valid
  - Loading configurable property values into instance variables to make them easier to access in other Bridge methods
### Count Method
**REST API Path:** /kinetic-bridgehub/app/api/v1/bridges/{slug}/count?structure={structure}&query={query}

A method that can be called by the API to return how many records a particular structure and query combination matches.

Returns a `Count(Integer count)` object that takes the number of records returned as a parameter (either as an integer or long)
### Retrieve Method
**REST API Path:** /kinetic-bridgehub/app/api/v1/bridges/{slug}/retrieve?structure={structure}&fields={fields}&query={query}

A method that can be called by the API to return a *single* record

Returns a `Record(Map<String,Object> record, List<String> fields)` object that takes a single record as a key(field)/value map and a list of fields that should be returned.
### Search Method
**REST API Path:** /kinetic-bridgehub/app/api/v1/bridges/{slug}/search?structure={structure}&fields={fields}&query={query}

A method that can be called by the API to return *multiple* records.

Returns a `Record(List<String> fields, Map<Record> records, Map<String,String> metadata)` where fields is a list of fields to return on each object, records is a list of `Record()` objects, and metadata is an optional key/value map of metadata fields (ie. count, size, pagination values, etc.).
## Explanation of API Input Values
### Structure
A structure corresponds to the *type* of data that should be returned by a Bridge call. For example, if a Bridge call is returning submission data to a user the structure could be "Submissions" to indicate clearly to that user what the data returned will be representing (very similar in concept to naming database table).
### Fields
The fields input is a comma separated list of fields that each record returned should contain.
### Query
The query should be something that the system providing the source data understands to filter out Records. In terms of Bridge Adapters, the query is just passed in as a string without any validation and it is the job of the Adapter author to either reform or pass along the query in the REST call to the data source.

## Modifying the REST Template Adapter to support a different REST Call
### Change the name of the adapter
The name of the adapter needs to be changed in the following filenames:
- src/main/java/com/kineticdata/bridgehub/adapter/rest/**RestTemplateAdapter.java**
  - RestTemplate should be replaced with a camelcase name describing the service the adapter is connecting to.
  - ie. For Kientic Core, the file changes to KineticCoreAdapter.java.
  - Also change the java class name at the top of the file (public class RestTemplateAdapter) to match the new file name
- src/main/java/com/kineticdata/bridgehub/adapter/rest/**RestTemplateQualificationParser.java**
  - Once again, replace RestTemplate with a camelcase name describing the adapter's service
  - ie. For Kientic Core, the file changes to KineticCoreQualificationParser.java.
  - Also change the java class name at the top of the file (public class RestTemplateAdapter) to match the new file name

The name needs to be changed in the following places inside RestTemplateAdapter.java:
- The NAME constant at the top of the adapter class
  - This is the name that will be displayed to users in the Bridgehub console.
  - Full statement should be: 
    - `public static final string NAME = "New Name here"`
- In the logger statement directly after the NAME constant
  - Change it to the same as the Java class name
  - For Kientic Core, this would be changed to:
    - `protected static final org.slf4j.Logger logger = LoggerFactory.getLogger(KineticCoreAdapter.class);`
- In each of count(), retrieve(), and search(), rename RestTemplateQualificationParser
  - Should be renamed to whatever you renamed the RestTemplateQualificationParser.java filename to.
  - For Kinetic Core, this would be changed to:
    - `KineticCoreQualificationParser parser = KienticCoreQualificationParser();`

### Adding/Removing Configurable Properties
- First add any new properties to `public static class Properties`
  - The constant (USERNAME) is what the Adapter uses and the string ("Username") is the label the user sees
  - A new property with a label of 'Kinetic Core Location' would look like:
    `public static final String KINETIC_CORE_LOCATION = "Kinetic Core Location";`
- Then add the property to the ConfigurablePropertyMap
  - Add the new property by adding `new ConfigurablePropertyMap(Properties.YOUR_PROPERTY_CONSTANT)` and setting any extra properties on that configurable property
    - `.setIsRequired(true)` makes a property required
    - `.setIsSensitive(true)` makes aproperty a sensitive (password) field input
    - `.setValue("")` sets the inputted string as a default value for the property
    - `.setDescription("")` shows the string as a description under the property input in the console
  - To add the new 'Kinetic Core Location' property as a required field:
    - `new ConfigurableProperty(Properties.KINETIC_CORE_LOCATION).setIsRequired(true))`

### Adding a new Structure to the list of valid structures
  - Add the structure name to the comma separated list of String inside the `VALID_STRUCTURES` array.
  ```
  public static final List<String> VALID_STRUCTURES = Arrays.asList(new String[] {
      "FirstStructure","SecondStructure","New Structure"
  });
  ```
### Changing the initialize() method
- Change `this.restEndpoint` to the base endpoint for the web service that you are going to use.
  - For Kinetic Core, the new rest endpoint would be 'http://localhost:8080/kinetic/space
  ```
  this.restEndpoint = "http://localhost:8080/kinetic/space;
  ```
- If you added any new configurable properties, consiering adding them as an instance variable in the initalize() method to make it easier to access in other Adapter methods.
  - Calling `this.property` is more convinient than calling `properties.getValue(Properties.PROPERTY)`.
  - Example of adding the new 'Kinetic Core Location' property as a instance variable:
  ```
  private String username;
  private String password;
  private String restEndpoint;
  // The new Kinetic Core Location property
  private String kineticCoreLocation;
  ```
  - Assigning a value to the the new kineticCoreLocation instance variable
  ```
  this.username = properties.getValue(Properties.USERNAME);
  this.password = properties.getValue(Properties.PASSWORD);
  this.kineticCoreLocation = properties.getValue(Properties.KINETIC_CORE_LOCATION);
  ```
- It is also a good idea to make a sample call to your web service in initialize() to validate that a user's inputted configurable properties work with the REST endpoint.
  - If you wish to validate the properties, modify testAuthenticationValues() to make an authentication check to the REST endpoint and throw a `new BridgeError("message")` if a 401 is returned.
  - If an error happens during initialize(), the bridge will remain uninitalized and will continue attempting initialization on each method call and after each property change until an error doesn't occur.
### Changing the count() method
- Replace the `String url =` variable with a path to a count endpoint for the Structure that should be retrieved
  - When building up the Url, make sure to also include the query that was passed in (either passed unchanged or modified to fit the REST API's expectations)
  ```
  String url = this.restEndpoint+"/app/api/v1/submissions/count?query=escapeQuery(query);
  ```
  - If the REST API service you are using doesn't have a count endpoint, just use an endpoint that returns all the records (likely also used for the search endpoint) that match the query and count the records that were returned.
- This example uses basic authentication for the REST API calls, but if you are using something other than that just replace or modify `get = addBasicAuthenticationHeader(get, this.username, this.password)` to add another authentication header to the HttpGet request.
- Once the URL and headers are set, the actual REST call and its outputs shouldn't need to change
  - The only thing that would be worth changing is the wording on the logging and error messages in that section (Changing from REST Service to something more descriptive of the service the call is being made to)
- Parsing the results will need to change based on the type and format of data that is being sent back from your REST API.
  - If you were able to call a service that has a specific count endpoint, parsing and returning that count number from the return object would look something like this:
  ```
  JSONObject json = (JSONObject)JSONValue.parse(output);
  Integer count = Integer.valueOf((String)json.get("count"));
  ```
  - If your REST API didn't have a count endpoint, parsing and counting all the returned records would look something like this:
  ```
  JSONObject json = (JSONObject)JSONValue.parse(output);
  JSONArray records = (JSONArray)json.get("records");
  Integer count = records.size();
  ```
### Changing the retrieve() method
- Replace the `String url =` variable with a path to retrieve a single record from the REST API.
  - If the record id needs to be in the url path instead of the query, the example shows how to pull a value from a user entered query and include it in the url path.
  - If an id doesn't need to be in the url and instead needs to be in the query, grab the url example from the `search()` method.
- Parsing the results will need to change based on the type and format of data that is being sent back from your REST API.
  - If you called an endpoint that is only returning a single record, the parsing of the record should look something like this:
  ```
  JSONObject json = (JSONObject)JSONValue.parse(output);
  JSONObject record = (JSONObject)json.get("record");
  ```
  - If you called an endpoint that can return multiple results but is filtered down to one record based on a query, the parsing should look something like this:
  ```
  JSONObject json = (JSONObject)JSONValue.parse(output);
  JSONArray records = (JSONArray)json.get("records");
  if (records.size() > 1) {
    throw new BridgeError("Multiple results matched an expected single match query");
  }
  JSONObject record = records.isEmpty() ? null : (JSONObject)records.get(0);
  ```
- If more than one record is returned from the REST API call, a BridgeError should be thrown.
### Changing the search() method
- Replace the `String url =` variable with a path to retrieve multiple records matching a query from the REST API you are using.
- Parse the results and create a new `Record()` object for each record that was returned and load it into a `List<Record>`. 
- Create a metadata map that contains count and size keys (at a minimum) that correspond to the size of the recordList if pagination is not included. If pagination is included by the bridge, other metadata values might be useful:
  - **count**: the total count of all records (not just the ones returned)
  - **size**: the amount of records on this page
  - **pageSize**: the amount of records allowed to be returned on the page
  - **pageNumber**: the page number that this call is on
  - **nextPageToken**: If not doing pageSize/pageNumber pagination, can include a nextPageToken that can be passed back into the adapter to return the next page