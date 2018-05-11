# RulesApi

All URIs are relative to *https://localhost/api/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**deleteRule**](RulesApi.md#deleteRule) | **DELETE** /namespaces/{namespace}/rules/{ruleName} | Delete a rule
[**getAllRules**](RulesApi.md#getAllRules) | **GET** /namespaces/{namespace}/rules | Get all rules
[**getRuleByName**](RulesApi.md#getRuleByName) | **GET** /namespaces/{namespace}/rules/{ruleName} | Get rule information
[**setState**](RulesApi.md#setState) | **POST** /namespaces/{namespace}/rules/{ruleName} | Enable or disable a rule
[**updateRule**](RulesApi.md#updateRule) | **PUT** /namespaces/{namespace}/rules/{ruleName} | Update a rule


<a name="deleteRule"></a>
# **deleteRule**
> deleteRule(namespace, ruleName)

Delete a rule

Delete a rule

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.RulesApi;


RulesApi apiInstance = new RulesApi();
String namespace = "namespace_example"; // String | The entity namespace
String ruleName = "ruleName_example"; // String | Name of rule to delete
try {
    apiInstance.deleteRule(namespace, ruleName);
} catch (ApiException e) {
    System.err.println("Exception when calling RulesApi#deleteRule");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **namespace** | **String**| The entity namespace |
 **ruleName** | **String**| Name of rule to delete |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="getAllRules"></a>
# **getAllRules**
> List&lt;EntityBrief&gt; getAllRules(namespace, limit, skip)

Get all rules

Get all rules

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.RulesApi;


RulesApi apiInstance = new RulesApi();
String namespace = "namespace_example"; // String | The entity namespace
Integer limit = 56; // Integer | Number of entities to include in the result.
Integer skip = 56; // Integer | Number of entities to skip in the result.
try {
    List<EntityBrief> result = apiInstance.getAllRules(namespace, limit, skip);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling RulesApi#getAllRules");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **namespace** | **String**| The entity namespace |
 **limit** | **Integer**| Number of entities to include in the result. | [optional]
 **skip** | **Integer**| Number of entities to skip in the result. | [optional]

### Return type

[**List&lt;EntityBrief&gt;**](EntityBrief.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="getRuleByName"></a>
# **getRuleByName**
> Rule getRuleByName(namespace, ruleName)

Get rule information

Get rule information

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.RulesApi;


RulesApi apiInstance = new RulesApi();
String namespace = "namespace_example"; // String | The entity namespace
String ruleName = "ruleName_example"; // String | Name of rule to fetch
try {
    Rule result = apiInstance.getRuleByName(namespace, ruleName);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling RulesApi#getRuleByName");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **namespace** | **String**| The entity namespace |
 **ruleName** | **String**| Name of rule to fetch |

### Return type

[**Rule**](Rule.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="setState"></a>
# **setState**
> ItemId setState(namespace, ruleName, state)

Enable or disable a rule

Enable or disable a rule

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.RulesApi;


RulesApi apiInstance = new RulesApi();
String namespace = "namespace_example"; // String | The entity namespace
String ruleName = "ruleName_example"; // String | Name of rule to update
String state = "state_example"; // String | Set state to enable or disable
try {
    ItemId result = apiInstance.setState(namespace, ruleName, state);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling RulesApi#setState");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **namespace** | **String**| The entity namespace |
 **ruleName** | **String**| Name of rule to update |
 **state** | **String**| Set state to enable or disable | [enum: disabled, enabled]

### Return type

[**ItemId**](ItemId.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="updateRule"></a>
# **updateRule**
> ItemId updateRule(namespace, ruleName, rule, overwrite)

Update a rule

Update a rule

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.RulesApi;


RulesApi apiInstance = new RulesApi();
String namespace = "namespace_example"; // String | The entity namespace
String ruleName = "ruleName_example"; // String | Name of rule to update
RulePut rule = new RulePut(); // RulePut | The rule being updated
String overwrite = "overwrite_example"; // String | Overwrite item if it exists. Default is false.
try {
    ItemId result = apiInstance.updateRule(namespace, ruleName, rule, overwrite);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling RulesApi#updateRule");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **namespace** | **String**| The entity namespace |
 **ruleName** | **String**| Name of rule to update |
 **rule** | [**RulePut**](RulePut.md)| The rule being updated |
 **overwrite** | **String**| Overwrite item if it exists. Default is false. | [optional] [enum: true, false]

### Return type

[**ItemId**](ItemId.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

