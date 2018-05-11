# NamespacesApi

All URIs are relative to *https://localhost/api/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getAllEntitiesInNamespace**](NamespacesApi.md#getAllEntitiesInNamespace) | **GET** /namespaces/{namespace} | Get all entities in a namespace
[**getAllNamespaces**](NamespacesApi.md#getAllNamespaces) | **GET** /namespaces | Get all namespaces for authenticated user


<a name="getAllEntitiesInNamespace"></a>
# **getAllEntitiesInNamespace**
> InlineResponse200 getAllEntitiesInNamespace(namespace)

Get all entities in a namespace

Get all entities in a namespace

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.NamespacesApi;


NamespacesApi apiInstance = new NamespacesApi();
String namespace = "namespace_example"; // String | The namespace
try {
    InlineResponse200 result = apiInstance.getAllEntitiesInNamespace(namespace);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling NamespacesApi#getAllEntitiesInNamespace");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **namespace** | **String**| The namespace |

### Return type

[**InlineResponse200**](InlineResponse200.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="getAllNamespaces"></a>
# **getAllNamespaces**
> List&lt;String&gt; getAllNamespaces()

Get all namespaces for authenticated user

Get all namespaces for authenticated user

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.NamespacesApi;


NamespacesApi apiInstance = new NamespacesApi();
try {
    List<String> result = apiInstance.getAllNamespaces();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling NamespacesApi#getAllNamespaces");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

**List&lt;String&gt;**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

