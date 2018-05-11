# TriggersApi

All URIs are relative to *https://localhost/api/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**deleteTrigger**](TriggersApi.md#deleteTrigger) | **DELETE** /namespaces/{namespace}/triggers/{triggerName} | Delete a trigger
[**fireTrigger**](TriggersApi.md#fireTrigger) | **POST** /namespaces/{namespace}/triggers/{triggerName} | Fire a trigger
[**getAllTriggers**](TriggersApi.md#getAllTriggers) | **GET** /namespaces/{namespace}/triggers | Get all triggers
[**getTriggerByName**](TriggersApi.md#getTriggerByName) | **GET** /namespaces/{namespace}/triggers/{triggerName} | Get trigger information
[**updateTrigger**](TriggersApi.md#updateTrigger) | **PUT** /namespaces/{namespace}/triggers/{triggerName} | Update a trigger


<a name="deleteTrigger"></a>
# **deleteTrigger**
> deleteTrigger(namespace, triggerName)

Delete a trigger

Delete a trigger

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.TriggersApi;


TriggersApi apiInstance = new TriggersApi();
String namespace = "namespace_example"; // String | The entity namespace
String triggerName = "triggerName_example"; // String | Name of trigger to delete
try {
    apiInstance.deleteTrigger(namespace, triggerName);
} catch (ApiException e) {
    System.err.println("Exception when calling TriggersApi#deleteTrigger");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **namespace** | **String**| The entity namespace |
 **triggerName** | **String**| Name of trigger to delete |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="fireTrigger"></a>
# **fireTrigger**
> ItemId fireTrigger(namespace, triggerName, payload)

Fire a trigger

Fire a trigger

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.TriggersApi;


TriggersApi apiInstance = new TriggersApi();
String namespace = "namespace_example"; // String | The entity namespace
String triggerName = "triggerName_example"; // String | Name of trigger being fired
KeyValue payload = new KeyValue(); // KeyValue | The trigger payload
try {
    ItemId result = apiInstance.fireTrigger(namespace, triggerName, payload);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling TriggersApi#fireTrigger");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **namespace** | **String**| The entity namespace |
 **triggerName** | **String**| Name of trigger being fired |
 **payload** | [**KeyValue**](KeyValue.md)| The trigger payload |

### Return type

[**ItemId**](ItemId.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="getAllTriggers"></a>
# **getAllTriggers**
> List&lt;EntityBrief&gt; getAllTriggers(namespace, limit, skip)

Get all triggers

Get all triggers

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.TriggersApi;


TriggersApi apiInstance = new TriggersApi();
String namespace = "namespace_example"; // String | The entity namespace
Integer limit = 56; // Integer | Number of entities to include in the result.
Integer skip = 56; // Integer | Number of entities to skip in the result.
try {
    List<EntityBrief> result = apiInstance.getAllTriggers(namespace, limit, skip);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling TriggersApi#getAllTriggers");
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

<a name="getTriggerByName"></a>
# **getTriggerByName**
> Trigger getTriggerByName(namespace, triggerName)

Get trigger information

Get trigger information

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.TriggersApi;


TriggersApi apiInstance = new TriggersApi();
String namespace = "namespace_example"; // String | The entity namespace
String triggerName = "triggerName_example"; // String | Name of trigger to fetch
try {
    Trigger result = apiInstance.getTriggerByName(namespace, triggerName);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling TriggersApi#getTriggerByName");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **namespace** | **String**| The entity namespace |
 **triggerName** | **String**| Name of trigger to fetch |

### Return type

[**Trigger**](Trigger.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="updateTrigger"></a>
# **updateTrigger**
> ItemId updateTrigger(namespace, triggerName, trigger, overwrite)

Update a trigger

Update a trigger

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.TriggersApi;


TriggersApi apiInstance = new TriggersApi();
String namespace = "namespace_example"; // String | The entity namespace
String triggerName = "triggerName_example"; // String | Name of trigger to update
TriggerPut trigger = new TriggerPut(); // TriggerPut | The trigger being updated
String overwrite = "overwrite_example"; // String | Overwrite item if it exists. Default is false.
try {
    ItemId result = apiInstance.updateTrigger(namespace, triggerName, trigger, overwrite);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling TriggersApi#updateTrigger");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **namespace** | **String**| The entity namespace |
 **triggerName** | **String**| Name of trigger to update |
 **trigger** | [**TriggerPut**](TriggerPut.md)| The trigger being updated |
 **overwrite** | **String**| Overwrite item if it exists. Default is false. | [optional] [enum: true, false]

### Return type

[**ItemId**](ItemId.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

