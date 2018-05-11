# swagger-java-client

## Requirements

Building the API client library requires [Maven](https://maven.apache.org/) to be installed.

## Installation

To install the API client library to your local Maven repository, simply execute:

```shell
mvn install
```

To deploy it to a remote Maven repository instead, configure the settings of the repository and execute:

```shell
mvn deploy
```

Refer to the [official documentation](https://maven.apache.org/plugins/maven-deploy-plugin/usage.html) for more information.

### Maven users

Add this dependency to your project's POM:

```xml
<dependency>
    <groupId>io.swagger</groupId>
    <artifactId>swagger-java-client</artifactId>
    <version>1.0.0</version>
    <scope>compile</scope>
</dependency>
```

### Gradle users

Add this dependency to your project's build file:

```groovy
compile "io.swagger:swagger-java-client:1.0.0"
```

### Others

At first generate the JAR by executing:

    mvn package

Then manually install the following JARs:

* target/swagger-java-client-1.0.0.jar
* target/lib/*.jar

## Getting Started

Please follow the [installation](#installation) instruction and execute the following Java code:

```java

import io.swagger.client.*;
import io.swagger.client.auth.*;
import io.swagger.client.model.*;
import io.swagger.client.api.ActionsApi;

import java.io.File;
import java.util.*;

public class ActionsApiExample {

    public static void main(String[] args) {
        
        ActionsApi apiInstance = new ActionsApi();
        String namespace = "namespace_example"; // String | The entity namespace
        String actionName = "actionName_example"; // String | Name of action
        try {
            apiInstance.deleteAction(namespace, actionName);
        } catch (ApiException e) {
            System.err.println("Exception when calling ActionsApi#deleteAction");
            e.printStackTrace();
        }
    }
}

```

## Documentation for API Endpoints

All URIs are relative to *https://localhost/api/v1*

Class | Method | HTTP request | Description
------------ | ------------- | ------------- | -------------
*ActionsApi* | [**deleteAction**](docs/ActionsApi.md#deleteAction) | **DELETE** /namespaces/{namespace}/actions/{actionName} | Delete an action
*ActionsApi* | [**getActionByName**](docs/ActionsApi.md#getActionByName) | **GET** /namespaces/{namespace}/actions/{actionName} | Get action information
*ActionsApi* | [**getAllActions**](docs/ActionsApi.md#getAllActions) | **GET** /namespaces/{namespace}/actions | Get all actions
*ActionsApi* | [**invokeAction**](docs/ActionsApi.md#invokeAction) | **POST** /namespaces/{namespace}/actions/{actionName} | Invoke an action
*ActionsApi* | [**updateAction**](docs/ActionsApi.md#updateAction) | **PUT** /namespaces/{namespace}/actions/{actionName} | Create or update an action
*ActivationsApi* | [**getActivationById**](docs/ActivationsApi.md#getActivationById) | **GET** /namespaces/{namespace}/activations/{activationid} | Get activation information
*ActivationsApi* | [**getActivations**](docs/ActivationsApi.md#getActivations) | **GET** /namespaces/{namespace}/activations | Get activation ids
*ActivationsApi* | [**namespacesNamespaceActivationsActivationidLogsGet**](docs/ActivationsApi.md#namespacesNamespaceActivationsActivationidLogsGet) | **GET** /namespaces/{namespace}/activations/{activationid}/logs | Get the logs for an activation
*ActivationsApi* | [**namespacesNamespaceActivationsActivationidResultGet**](docs/ActivationsApi.md#namespacesNamespaceActivationsActivationidResultGet) | **GET** /namespaces/{namespace}/activations/{activationid}/result | Get the result of an activation
*NamespacesApi* | [**getAllEntitiesInNamespace**](docs/NamespacesApi.md#getAllEntitiesInNamespace) | **GET** /namespaces/{namespace} | Get all entities in a namespace
*NamespacesApi* | [**getAllNamespaces**](docs/NamespacesApi.md#getAllNamespaces) | **GET** /namespaces | Get all namespaces for authenticated user
*PackagesApi* | [**deletePackage**](docs/PackagesApi.md#deletePackage) | **DELETE** /namespaces/{namespace}/packages/{packageName} | Delete a package
*PackagesApi* | [**getAlPackages**](docs/PackagesApi.md#getAlPackages) | **GET** /namespaces/{namespace}/packages | Get all packages
*PackagesApi* | [**getPackageByName**](docs/PackagesApi.md#getPackageByName) | **GET** /namespaces/{namespace}/packages/{packageName} | Get package information
*PackagesApi* | [**updatePackage**](docs/PackagesApi.md#updatePackage) | **PUT** /namespaces/{namespace}/packages/{packageName} | Create or update a package
*RulesApi* | [**deleteRule**](docs/RulesApi.md#deleteRule) | **DELETE** /namespaces/{namespace}/rules/{ruleName} | Delete a rule
*RulesApi* | [**getAllRules**](docs/RulesApi.md#getAllRules) | **GET** /namespaces/{namespace}/rules | Get all rules
*RulesApi* | [**getRuleByName**](docs/RulesApi.md#getRuleByName) | **GET** /namespaces/{namespace}/rules/{ruleName} | Get rule information
*RulesApi* | [**setState**](docs/RulesApi.md#setState) | **POST** /namespaces/{namespace}/rules/{ruleName} | Enable or disable a rule
*RulesApi* | [**updateRule**](docs/RulesApi.md#updateRule) | **PUT** /namespaces/{namespace}/rules/{ruleName} | Update a rule
*TriggersApi* | [**deleteTrigger**](docs/TriggersApi.md#deleteTrigger) | **DELETE** /namespaces/{namespace}/triggers/{triggerName} | Delete a trigger
*TriggersApi* | [**fireTrigger**](docs/TriggersApi.md#fireTrigger) | **POST** /namespaces/{namespace}/triggers/{triggerName} | Fire a trigger
*TriggersApi* | [**getAllTriggers**](docs/TriggersApi.md#getAllTriggers) | **GET** /namespaces/{namespace}/triggers | Get all triggers
*TriggersApi* | [**getTriggerByName**](docs/TriggersApi.md#getTriggerByName) | **GET** /namespaces/{namespace}/triggers/{triggerName} | Get trigger information
*TriggersApi* | [**updateTrigger**](docs/TriggersApi.md#updateTrigger) | **PUT** /namespaces/{namespace}/triggers/{triggerName} | Update a trigger


## Documentation for Models

 - [Action](docs/Action.md)
 - [ActionExec](docs/ActionExec.md)
 - [ActionLimits](docs/ActionLimits.md)
 - [ActionPayload](docs/ActionPayload.md)
 - [ActionPut](docs/ActionPut.md)
 - [Activation](docs/Activation.md)
 - [ActivationIds](docs/ActivationIds.md)
 - [ActivationInfo](docs/ActivationInfo.md)
 - [ActivationInfoResult](docs/ActivationInfoResult.md)
 - [ActivationLogs](docs/ActivationLogs.md)
 - [ActivationResult](docs/ActivationResult.md)
 - [ActivationStderr](docs/ActivationStderr.md)
 - [EntityBrief](docs/EntityBrief.md)
 - [ErrorMessage](docs/ErrorMessage.md)
 - [InlineResponse200](docs/InlineResponse200.md)
 - [ItemId](docs/ItemId.md)
 - [KeyValue](docs/KeyValue.md)
 - [ModelPackage](docs/ModelPackage.md)
 - [PackageBinding](docs/PackageBinding.md)
 - [PackagePut](docs/PackagePut.md)
 - [Provider](docs/Provider.md)
 - [ProviderAction](docs/ProviderAction.md)
 - [ProviderBinding](docs/ProviderBinding.md)
 - [ProviderTrigger](docs/ProviderTrigger.md)
 - [Rule](docs/Rule.md)
 - [RulePut](docs/RulePut.md)
 - [Trigger](docs/Trigger.md)
 - [TriggerLimits](docs/TriggerLimits.md)
 - [TriggerPayload](docs/TriggerPayload.md)
 - [TriggerPut](docs/TriggerPut.md)


## Documentation for Authorization

All endpoints do not require authorization.
Authentication schemes defined for the API:

## Recommendation

It's recommended to create an instance of `ApiClient` per thread in a multithreaded environment to avoid any potential issues.

## Author



