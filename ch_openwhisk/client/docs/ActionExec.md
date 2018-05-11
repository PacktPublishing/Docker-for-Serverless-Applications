
# ActionExec

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**kind** | [**KindEnum**](#KindEnum) | the type of action | 
**code** | **String** | The code to execute when kind is not &#39;blackbox&#39; |  [optional]
**image** | **String** | container image name when kind is &#39;blackbox&#39; |  [optional]
**init** | **String** | optional zipfile reference when code kind is &#39;nodejs&#39; |  [optional]


<a name="KindEnum"></a>
## Enum: KindEnum
Name | Value
---- | -----
NODEJS_6 | &quot;nodejs:6&quot;
NODEJS_8 | &quot;nodejs:8&quot;
PYTHON_2 | &quot;python:2&quot;
PYTHON_3 | &quot;python:3&quot;
SWIFT_3 | &quot;swift:3&quot;
SWIFT_3_1_1 | &quot;swift:3.1.1&quot;
JAVA | &quot;java&quot;
BLACKBOX | &quot;blackbox&quot;



