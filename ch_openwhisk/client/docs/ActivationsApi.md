# ActivationsApi

All URIs are relative to *https://localhost/api/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getActivationById**](ActivationsApi.md#getActivationById) | **GET** /namespaces/{namespace}/activations/{activationid} | Get activation information
[**getActivations**](ActivationsApi.md#getActivations) | **GET** /namespaces/{namespace}/activations | Get activation ids
[**namespacesNamespaceActivationsActivationidLogsGet**](ActivationsApi.md#namespacesNamespaceActivationsActivationidLogsGet) | **GET** /namespaces/{namespace}/activations/{activationid}/logs | Get the logs for an activation
[**namespacesNamespaceActivationsActivationidResultGet**](ActivationsApi.md#namespacesNamespaceActivationsActivationidResultGet) | **GET** /namespaces/{namespace}/activations/{activationid}/result | Get the result of an activation


<a name="getActivationById"></a>
# **getActivationById**
> Activation getActivationById(namespace, activationid)

Get activation information

Get activation information.

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.ActivationsApi;


ActivationsApi apiInstance = new ActivationsApi();
String namespace = "namespace_example"; // String | The entity namespace
String activationid = "activationid_example"; // String | Name of activation to fetch
try {
    Activation result = apiInstance.getActivationById(namespace, activationid);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ActivationsApi#getActivationById");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **namespace** | **String**| The entity namespace |
 **activationid** | **String**| Name of activation to fetch |

### Return type

[**Activation**](Activation.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="getActivations"></a>
# **getActivations**
> List&lt;EntityBrief&gt; getActivations(namespace, name, limit, skip, since, upto, docs)

Get activation ids

Get activation ids.

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.ActivationsApi;


ActivationsApi apiInstance = new ActivationsApi();
String namespace = "namespace_example"; // String | The entity namespace
String name = "name_example"; // String | Name of item
Integer limit = 56; // Integer | Number of entities to include in the result.
Integer skip = 56; // Integer | Number of entities to skip in the result.
Integer since = 56; // Integer | Only include entities later than this timestamp (measured in milliseconds since Thu, 01 Jan 1970)
Integer upto = 56; // Integer | Only include entities earlier than this timestamp (measured in milliseconds since Thu, 01 Jan 1970)
Boolean docs = true; // Boolean | Whether to include full entity description.
try {
    List<EntityBrief> result = apiInstance.getActivations(namespace, name, limit, skip, since, upto, docs);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ActivationsApi#getActivations");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **namespace** | **String**| The entity namespace |
 **name** | **String**| Name of item | [optional]
 **limit** | **Integer**| Number of entities to include in the result. | [optional]
 **skip** | **Integer**| Number of entities to skip in the result. | [optional]
 **since** | **Integer**| Only include entities later than this timestamp (measured in milliseconds since Thu, 01 Jan 1970) | [optional]
 **upto** | **Integer**| Only include entities earlier than this timestamp (measured in milliseconds since Thu, 01 Jan 1970) | [optional]
 **docs** | **Boolean**| Whether to include full entity description. | [optional]

### Return type

[**List&lt;EntityBrief&gt;**](EntityBrief.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="namespacesNamespaceActivationsActivationidLogsGet"></a>
# **namespacesNamespaceActivationsActivationidLogsGet**
> ActivationLogs namespacesNamespaceActivationsActivationidLogsGet(namespace, activationid)

Get the logs for an activation

Get activation logs information.

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.ActivationsApi;


ActivationsApi apiInstance = new ActivationsApi();
String namespace = "namespace_example"; // String | The entity namespace
String activationid = "activationid_example"; // String | Name of activation to fetch
try {
    ActivationLogs result = apiInstance.namespacesNamespaceActivationsActivationidLogsGet(namespace, activationid);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ActivationsApi#namespacesNamespaceActivationsActivationidLogsGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **namespace** | **String**| The entity namespace |
 **activationid** | **String**| Name of activation to fetch |

### Return type

[**ActivationLogs**](ActivationLogs.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="namespacesNamespaceActivationsActivationidResultGet"></a>
# **namespacesNamespaceActivationsActivationidResultGet**
> ActivationResult namespacesNamespaceActivationsActivationidResultGet(namespace, activationid)

Get the result of an activation

Get activation result.

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.ActivationsApi;


ActivationsApi apiInstance = new ActivationsApi();
String namespace = "namespace_example"; // String | The entity namespace
String activationid = "activationid_example"; // String | Name of activation to fetch
try {
    ActivationResult result = apiInstance.namespacesNamespaceActivationsActivationidResultGet(namespace, activationid);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ActivationsApi#namespacesNamespaceActivationsActivationidResultGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **namespace** | **String**| The entity namespace |
 **activationid** | **String**| Name of activation to fetch |

### Return type

[**ActivationResult**](ActivationResult.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

