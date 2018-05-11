# PackagesApi

All URIs are relative to *https://localhost/api/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**deletePackage**](PackagesApi.md#deletePackage) | **DELETE** /namespaces/{namespace}/packages/{packageName} | Delete a package
[**getAlPackages**](PackagesApi.md#getAlPackages) | **GET** /namespaces/{namespace}/packages | Get all packages
[**getPackageByName**](PackagesApi.md#getPackageByName) | **GET** /namespaces/{namespace}/packages/{packageName} | Get package information
[**updatePackage**](PackagesApi.md#updatePackage) | **PUT** /namespaces/{namespace}/packages/{packageName} | Create or update a package


<a name="deletePackage"></a>
# **deletePackage**
> deletePackage(namespace, packageName)

Delete a package

Delete a package

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.PackagesApi;


PackagesApi apiInstance = new PackagesApi();
String namespace = "namespace_example"; // String | The entity namespace
String packageName = "packageName_example"; // String | Name of package
try {
    apiInstance.deletePackage(namespace, packageName);
} catch (ApiException e) {
    System.err.println("Exception when calling PackagesApi#deletePackage");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **namespace** | **String**| The entity namespace |
 **packageName** | **String**| Name of package |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="getAlPackages"></a>
# **getAlPackages**
> List&lt;EntityBrief&gt; getAlPackages(namespace, _public, limit, skip)

Get all packages

Get all packages

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.PackagesApi;


PackagesApi apiInstance = new PackagesApi();
String namespace = "namespace_example"; // String | The entity namespace
Boolean _public = true; // Boolean | Include publicly shared entitles in the result.
Integer limit = 56; // Integer | Number of entities to include in the result.
Integer skip = 56; // Integer | Number of entities to skip in the result.
try {
    List<EntityBrief> result = apiInstance.getAlPackages(namespace, _public, limit, skip);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling PackagesApi#getAlPackages");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **namespace** | **String**| The entity namespace |
 **_public** | **Boolean**| Include publicly shared entitles in the result. | [optional]
 **limit** | **Integer**| Number of entities to include in the result. | [optional]
 **skip** | **Integer**| Number of entities to skip in the result. | [optional]

### Return type

[**List&lt;EntityBrief&gt;**](EntityBrief.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="getPackageByName"></a>
# **getPackageByName**
> ModelPackage getPackageByName(namespace, packageName)

Get package information

Get package information.

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.PackagesApi;


PackagesApi apiInstance = new PackagesApi();
String namespace = "namespace_example"; // String | The entity namespace
String packageName = "packageName_example"; // String | Name of package to fetch
try {
    ModelPackage result = apiInstance.getPackageByName(namespace, packageName);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling PackagesApi#getPackageByName");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **namespace** | **String**| The entity namespace |
 **packageName** | **String**| Name of package to fetch |

### Return type

[**ModelPackage**](ModelPackage.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="updatePackage"></a>
# **updatePackage**
> ItemId updatePackage(namespace, packageName, _package, overwrite)

Create or update a package

Create or update a package

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.PackagesApi;


PackagesApi apiInstance = new PackagesApi();
String namespace = "namespace_example"; // String | The entity namespace
String packageName = "packageName_example"; // String | Name of package
PackagePut _package = new PackagePut(); // PackagePut | The package being updated
String overwrite = "overwrite_example"; // String | Overwrite item if it exists. Default is false.
try {
    ItemId result = apiInstance.updatePackage(namespace, packageName, _package, overwrite);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling PackagesApi#updatePackage");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **namespace** | **String**| The entity namespace |
 **packageName** | **String**| Name of package |
 **_package** | [**PackagePut**](PackagePut.md)| The package being updated |
 **overwrite** | **String**| Overwrite item if it exists. Default is false. | [optional] [enum: true, false]

### Return type

[**ItemId**](ItemId.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

