# ActionsApi

All URIs are relative to *https://localhost/api/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**deleteAction**](ActionsApi.md#deleteAction) | **DELETE** /namespaces/{namespace}/actions/{actionName} | Delete an action
[**getActionByName**](ActionsApi.md#getActionByName) | **GET** /namespaces/{namespace}/actions/{actionName} | Get action information
[**getAllActions**](ActionsApi.md#getAllActions) | **GET** /namespaces/{namespace}/actions | Get all actions
[**invokeAction**](ActionsApi.md#invokeAction) | **POST** /namespaces/{namespace}/actions/{actionName} | Invoke an action
[**updateAction**](ActionsApi.md#updateAction) | **PUT** /namespaces/{namespace}/actions/{actionName} | Create or update an action


<a name="deleteAction"></a>
# **deleteAction**
> deleteAction(namespace, actionName)

Delete an action

Delete an action

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.ActionsApi;


ActionsApi apiInstance = new ActionsApi();
String namespace = "namespace_example"; // String | The entity namespace
String actionName = "actionName_example"; // String | Name of action
try {
    apiInstance.deleteAction(namespace, actionName);
} catch (ApiException e) {
    System.err.println("Exception when calling ActionsApi#deleteAction");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **namespace** | **String**| The entity namespace |
 **actionName** | **String**| Name of action |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="getActionByName"></a>
# **getActionByName**
> Action getActionByName(namespace, actionName)

Get action information

Get action information.

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.ActionsApi;


ActionsApi apiInstance = new ActionsApi();
String namespace = "namespace_example"; // String | The entity namespace
String actionName = "actionName_example"; // String | Name of action to fetch
try {
    Action result = apiInstance.getActionByName(namespace, actionName);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ActionsApi#getActionByName");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **namespace** | **String**| The entity namespace |
 **actionName** | **String**| Name of action to fetch |

### Return type

[**Action**](Action.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="getAllActions"></a>
# **getAllActions**
> List&lt;EntityBrief&gt; getAllActions(namespace, limit, skip)

Get all actions

Get all actions

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.ActionsApi;


ActionsApi apiInstance = new ActionsApi();
String namespace = "namespace_example"; // String | The entity namespace
Integer limit = 56; // Integer | Number of entities to include in the result.
Integer skip = 56; // Integer | Number of entities to skip in the result.
try {
    List<EntityBrief> result = apiInstance.getAllActions(namespace, limit, skip);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ActionsApi#getAllActions");
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

<a name="invokeAction"></a>
# **invokeAction**
> Activation invokeAction(namespace, actionName, payload, blocking, result, timeout)

Invoke an action

Invoke an action

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.ActionsApi;


ActionsApi apiInstance = new ActionsApi();
String namespace = "namespace_example"; // String | The entity namespace
String actionName = "actionName_example"; // String | Name of action
KeyValue payload = new KeyValue(); // KeyValue | The parameters for the action being invoked
String blocking = "blocking_example"; // String | Blocking or non-blocking invocation. Default is non-blocking.
String result = "result_example"; // String | Return only the result of a blocking activation. Default is false.
Integer timeout = 56; // Integer | Wait no more than specified duration in milliseconds for a blocking response. Default value and max allowed timeout are 60000.
try {
    Activation result = apiInstance.invokeAction(namespace, actionName, payload, blocking, result, timeout);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ActionsApi#invokeAction");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **namespace** | **String**| The entity namespace |
 **actionName** | **String**| Name of action |
 **payload** | [**KeyValue**](KeyValue.md)| The parameters for the action being invoked |
 **blocking** | **String**| Blocking or non-blocking invocation. Default is non-blocking. | [optional] [enum: true, false]
 **result** | **String**| Return only the result of a blocking activation. Default is false. | [optional] [enum: true, false]
 **timeout** | **Integer**| Wait no more than specified duration in milliseconds for a blocking response. Default value and max allowed timeout are 60000. | [optional]

### Return type

[**Activation**](Activation.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="updateAction"></a>
# **updateAction**
> ItemId updateAction(namespace, actionName, action, overwrite)

Create or update an action

Create or update an action

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.ActionsApi;


ActionsApi apiInstance = new ActionsApi();
String namespace = "namespace_example"; // String | The entity namespace
String actionName = "actionName_example"; // String | Name of action
ActionPut action = new ActionPut(); // ActionPut | The action being updated
String overwrite = "overwrite_example"; // String | Overwrite item if it exists. Default is false.
try {
    ItemId result = apiInstance.updateAction(namespace, actionName, action, overwrite);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ActionsApi#updateAction");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **namespace** | **String**| The entity namespace |
 **actionName** | **String**| Name of action |
 **action** | [**ActionPut**](ActionPut.md)| The action being updated |
 **overwrite** | **String**| Overwrite item if it exists. Default is false. | [optional] [enum: true, false]

### Return type

[**ItemId**](ItemId.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

