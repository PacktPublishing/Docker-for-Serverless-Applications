/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package whisk.core.controller

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

import org.apache.kafka.common.errors.RecordTooLargeException

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpMethod
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.RequestContext
import akka.http.scaladsl.server.RouteResult
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.sprayJsonMarshaller
import akka.http.scaladsl.unmarshalling._

import spray.json._
import spray.json.DefaultJsonProtocol._

import whisk.common.TransactionId
import whisk.core.WhiskConfig
import whisk.core.controller.actions.PostActionActivation
import whisk.core.database.CacheChangeNotification
import whisk.core.database.NoDocumentException
import whisk.core.entitlement._
import whisk.core.entity._
import whisk.core.entity.types.ActivationStore
import whisk.core.entity.types.EntityStore
import whisk.http.ErrorResponse.terminate
import whisk.http.Messages
import whisk.http.Messages._
import whisk.core.entitlement.Resource
import whisk.core.entitlement.Collection

/**
 * A singleton object which defines the properties that must be present in a configuration
 * in order to implement the actions API.
 */
object WhiskActionsApi {
  def requiredProperties = Map(WhiskConfig.actionSequenceMaxLimit -> null)

  /**
   * Max duration to wait for a blocking activation.
   * This is the default timeout on a POST request.
   */
  protected[core] val maxWaitForBlockingActivation = 60 seconds
}

/** A trait implementing the actions API. */
trait WhiskActionsApi extends WhiskCollectionAPI with PostActionActivation with ReferencedEntities {
  services: WhiskServices =>

  protected override val collection = Collection(Collection.ACTIONS)

  /** An actor system for timed based futures. */
  protected implicit val actorSystem: ActorSystem

  /** Database service to CRUD actions. */
  protected val entityStore: EntityStore

  /** Notification service for cache invalidation. */
  protected implicit val cacheChangeNotification: Some[CacheChangeNotification]

  /** Database service to get activations. */
  protected val activationStore: ActivationStore

  /** Entity normalizer to JSON object. */
  import RestApiCommons.emptyEntityToJsObject

  /** JSON response formatter. */
  import RestApiCommons.jsonDefaultResponsePrinter

  /**
   * Handles operations on action resources, which encompass these cases:
   *
   * 1. ns/foo     -> subject must be authorized for one of { action(ns, *), action(ns, foo) },
   *                  resource resolves to { action(ns, foo) }
   *
   * 2. ns/bar/foo -> where bar is a package
   *                  subject must be authorized for one of { package(ns, *), package(ns, bar), action(ns.bar, foo) }
   *                  resource resolves to { action(ns.bar, foo) }
   *
   * 3. ns/baz/foo -> where baz is a binding to ns'.bar
   *                  subject must be authorized for one of { package(ns, *), package(ns, baz) }
   *                  *and* one of { package(ns', *), package(ns', bar), action(ns'.bar, foo) }
   *                  resource resolves to { action(ns'.bar, foo) }
   *
   * Note that package(ns, xyz) == action(ns.xyz, *) and if subject has rights to package(ns, xyz)
   * then they also have rights to action(ns.xyz, *) since sharing is done at the package level and
   * is not more granular; hence a check on action(ns.xyz, abc) is eschewed.
   *
   * Only list is supported for these resources:
   *
   * 4. ns/bar/    -> where bar is a package
   *                  subject must be authorized for one of { package(ns, *), package(ns, bar) }
   *                  resource resolves to { action(ns.bar, *) }
   *
   * 5. ns/baz/    -> where baz is a binding to ns'.bar
   *                  subject must be authorized for one of { package(ns, *), package(ns, baz) }
   *                  *and* one of { package(ns', *), package(ns', bar) }
   *                  resource resolves to { action(ns.bar, *) }
   */
  protected override def innerRoutes(user: Identity, ns: EntityPath)(implicit transid: TransactionId) = {
    (entityPrefix & entityOps & requestMethod) { (segment, m) =>
      entityname(segment) { outername =>
        pathEnd {
          // matched /namespace/collection/name
          // this is an action in default package, authorize and dispatch
          authorizeAndDispatch(m, user, Resource(ns, collection, Some(outername)))
        } ~ (get & pathSingleSlash) {
          // matched GET /namespace/collection/package-name/
          // list all actions in package iff subject is entitled to READ package
          val resource = Resource(ns, Collection(Collection.PACKAGES), Some(outername))
          onComplete(entitlementProvider.check(user, Privilege.READ, resource)) {
            case Success(_) => listPackageActions(user, FullyQualifiedEntityName(ns, EntityName(outername)))
            case Failure(f) => super.handleEntitlementFailure(f)
          }
        } ~ (entityPrefix & pathEnd) { segment =>
          entityname(segment) { innername =>
            // matched /namespace/collection/package-name/action-name
            // this is an action in a named package
            val packageDocId = FullyQualifiedEntityName(ns, EntityName(outername)).toDocId
            val packageResource = Resource(ns.addPath(EntityName(outername)), collection, Some(innername))

            val right = collection.determineRight(m, Some(innername))
            onComplete(entitlementProvider.check(user, right, packageResource)) {
              case Success(_) =>
                getEntity(WhiskPackage, entityStore, packageDocId, Some {
                  if (right == Privilege.READ || right == Privilege.ACTIVATE) {
                    // need to merge package with action, hence authorize subject for package
                    // access (if binding, then subject must be authorized for both the binding
                    // and the referenced package)
                    //
                    // NOTE: it is an error if either the package or the action does not exist,
                    // the former manifests as unauthorized and the latter as not found
                    mergeActionWithPackageAndDispatch(m, user, EntityName(innername)) _
                  } else {
                    // these packaged action operations do not need merging with the package,
                    // but may not be permitted if this is a binding, or if the subject does
                    // not have PUT and DELETE rights to the package itself
                    (wp: WhiskPackage) =>
                      wp.binding map { _ =>
                        terminate(BadRequest, Messages.notAllowedOnBinding)
                      } getOrElse {
                        val actionResource = Resource(wp.fullPath, collection, Some(innername))
                        dispatchOp(user, right, actionResource)
                      }
                  }
                })
              case Failure(f) => super.handleEntitlementFailure(f)
            }
          }
        }
      }
    }
  }

  /**
   * Creates or updates action if it already exists. The PUT content is deserialized into a WhiskActionPut
   * which is a subset of WhiskAction (it eschews the namespace and entity name since the former is derived
   * from the authenticated user and the latter is derived from the URI). The WhiskActionPut is merged with
   * the existing WhiskAction in the datastore, overriding old values with new values that are defined.
   * Any values not defined in the PUT content are replaced with old values.
   *
   * Responses are one of (Code, Message)
   * - 200 WhiskAction as JSON
   * - 400 Bad Request
   * - 409 Conflict
   * - 500 Internal Server Error
   */
  override def create(user: Identity, entityName: FullyQualifiedEntityName)(implicit transid: TransactionId) = {
    parameter('overwrite ? false) { overwrite =>
      entity(as[WhiskActionPut]) { content =>
        val request = content.resolve(user.namespace)

        onComplete(entitleReferencedEntities(user, Privilege.READ, request.exec)) {
          case Success(_) =>
            putEntity(WhiskAction, entityStore, entityName.toDocId, overwrite, update(user, request) _, () => {
              make(user, entityName, request)
            })
          case Failure(f) =>
            super.handleEntitlementFailure(f)
        }
      }
    }
  }

  /**
   * Invokes action if it exists. The POST content is deserialized into a Payload and posted
   * to the loadbalancer.
   *
   * Responses are one of (Code, Message)
   * - 200 Activation as JSON if blocking or just the result JSON iff '&result=true'
   * - 202 ActivationId as JSON (this is issued on non-blocking activation or blocking activation that times out)
   * - 404 Not Found
   * - 502 Bad Gateway
   * - 500 Internal Server Error
   */
  override def activate(user: Identity, entityName: FullyQualifiedEntityName, env: Option[Parameters])(
    implicit transid: TransactionId) = {
    parameter(
      'blocking ? false,
      'result ? false,
      'timeout.as[FiniteDuration] ? WhiskActionsApi.maxWaitForBlockingActivation) { (blocking, result, waitOverride) =>
      entity(as[Option[JsObject]]) { payload =>
        getEntity(WhiskActionMetaData, entityStore, entityName.toDocId, Some {
          act: WhiskActionMetaData =>
            // resolve the action --- special case for sequences that may contain components with '_' as default package
            val action = act.resolve(user.namespace)
            onComplete(entitleReferencedEntitiesMetaData(user, Privilege.ACTIVATE, Some(action.exec))) {
              case Success(_) =>
                val actionWithMergedParams = env.map(action.inherit(_)) getOrElse action
                val waitForResponse = if (blocking) Some(waitOverride) else None
                onComplete(invokeAction(user, actionWithMergedParams, payload, waitForResponse, cause = None)) {
                  case Success(Left(activationId)) =>
                    // non-blocking invoke or blocking invoke which got queued instead
                    complete(Accepted, activationId.toJsObject)
                  case Success(Right(activation)) =>
                    val response = if (result) activation.resultAsJson else activation.toExtendedJson

                    if (activation.response.isSuccess) {
                      complete(OK, response)
                    } else if (activation.response.isApplicationError) {
                      // actions that result is ApplicationError status are considered a 'success'
                      // and will have an 'error' property in the result - the HTTP status is OK
                      // and clients must check the response status if it exists
                      // NOTE: response status will not exist in the JSON object if ?result == true
                      // and instead clients must check if 'error' is in the JSON
                      // PRESERVING OLD BEHAVIOR and will address defect in separate change
                      complete(BadGateway, response)
                    } else if (activation.response.isContainerError) {
                      complete(BadGateway, response)
                    } else {
                      complete(InternalServerError, response)
                    }
                  case Failure(t: RecordTooLargeException) =>
                    logging.info(this, s"[POST] action payload was too large")
                    terminate(RequestEntityTooLarge)
                  case Failure(RejectRequest(code, message)) =>
                    logging.info(this, s"[POST] action rejected with code $code: $message")
                    terminate(code, message)
                  case Failure(t: Throwable) =>
                    logging.error(this, s"[POST] action activation failed: ${t.getMessage}")
                    terminate(InternalServerError)
                }

              case Failure(f) =>
                super.handleEntitlementFailure(f)
            }
        })
      }
    }
  }

  /**
   * Deletes action.
   *
   * Responses are one of (Code, Message)
   * - 200 WhiskAction as JSON
   * - 404 Not Found
   * - 409 Conflict
   * - 500 Internal Server Error
   */
  override def remove(user: Identity, entityName: FullyQualifiedEntityName)(implicit transid: TransactionId) = {
    deleteEntity(WhiskAction, entityStore, entityName.toDocId, (a: WhiskAction) => Future.successful({}))
  }

  /**
   * Gets action. The action name is prefixed with the namespace to create the primary index key.
   *
   * Responses are one of (Code, Message)
   * - 200 WhiskAction has JSON
   * - 404 Not Found
   * - 500 Internal Server Error
   */
  override def fetch(user: Identity, entityName: FullyQualifiedEntityName, env: Option[Parameters])(
    implicit transid: TransactionId) = {
    getEntity(WhiskAction, entityStore, entityName.toDocId, Some { action: WhiskAction =>
      val mergedAction = env map { action inherit _ } getOrElse action
      complete(OK, mergedAction)
    })
  }

  /**
   * Gets all actions in a path.
   *
   * Responses are one of (Code, Message)
   * - 200 [] or [WhiskAction as JSON]
   * - 500 Internal Server Error
   */
  override def list(user: Identity, namespace: EntityPath, excludePrivate: Boolean)(implicit transid: TransactionId) = {
    // for consistency, all the collections should support the same list API
    // but because supporting docs on actions is difficult, the API does not
    // offer an option to fetch entities with full docs yet.
    //
    // the complication with actions is that providing docs on actions in
    // package bindings is cannot be do readily done with a couchdb view
    // and would require finding all bindings in namespace and
    // joining the actions explicitly here.
    val docs = false
    parameter('skip ? 0, 'limit ? collection.listLimit, 'count ? false) { (skip, limit, count) =>
      listEntities {
        WhiskAction.listCollectionInNamespace(entityStore, namespace, skip, limit, docs) map { list =>
          val actions = list.fold((js) => js, (as) => as.map(WhiskAction.serdes.write(_)))
          FilterEntityList.filter(actions, excludePrivate)
        }
      }
    }
  }

  /** Replaces default namespaces in a vector of components from a sequence with appropriate namespace. */
  private def resolveDefaultNamespace(components: Vector[FullyQualifiedEntityName],
                                      user: Identity): Vector[FullyQualifiedEntityName] = {
    // if components are part of the default namespace, they contain `_`; replace it!
    val resolvedComponents = components map { c =>
      FullyQualifiedEntityName(c.path.resolveNamespace(user.namespace), c.name)
    }
    resolvedComponents
  }

  /** Replaces default namespaces in an action sequence with appropriate namespace. */
  private def resolveDefaultNamespace(seq: SequenceExec, user: Identity): SequenceExec = {
    // if components are part of the default namespace, they contain `_`; replace it!
    val resolvedComponents = resolveDefaultNamespace(seq.components, user)
    new SequenceExec(resolvedComponents)
  }

  /**
   * Creates a WhiskAction instance from the PUT request.
   */
  private def makeWhiskAction(content: WhiskActionPut, entityName: FullyQualifiedEntityName)(
    implicit transid: TransactionId) = {
    val exec = content.exec.get
    val limits = content.limits map { l =>
      ActionLimits(l.timeout getOrElse TimeLimit(), l.memory getOrElse MemoryLimit(), l.logs getOrElse LogLimit())
    } getOrElse ActionLimits()
    // This is temporary while we are making sequencing directly supported in the controller.
    // The parameter override allows this to work with Pipecode.code. Any parameters other
    // than the action sequence itself are discarded and have no effect.
    // Note: While changing the implementation of sequences, components now store the fully qualified entity names
    // (which loses the leading "/"). Adding it back while both versions of the code are in place.
    val parameters = exec match {
      case seq: SequenceExec =>
        Parameters("_actions", JsArray(seq.components map { _.qualifiedNameWithLeadingSlash.toJson }))
      case _ => content.parameters getOrElse Parameters()
    }

    WhiskAction(
      entityName.path,
      entityName.name,
      exec,
      parameters,
      limits,
      content.version getOrElse SemVer(),
      content.publish getOrElse false,
      (content.annotations getOrElse Parameters()) ++ execAnnotation(exec))
  }

  /** For a sequence action, gather referenced entities and authorize access. */
  private def entitleReferencedEntities(user: Identity, right: Privilege, exec: Option[Exec])(
    implicit transid: TransactionId) = {
    exec match {
      case Some(seq: SequenceExec) =>
        logging.info(this, "checking if sequence components are accessible")
        entitlementProvider.check(user, right, referencedEntities(seq))
      case _ => Future.successful(true)
    }
  }

  private def entitleReferencedEntitiesMetaData(user: Identity, right: Privilege, exec: Option[ExecMetaDataBase])(
    implicit transid: TransactionId) = {
    exec match {
      case Some(seq: SequenceExecMetaData) =>
        logging.info(this, "checking if sequence components are accessible")
        entitlementProvider.check(user, right, referencedEntities(seq))
      case _ => Future.successful(true)
    }
  }

  /** Creates a WhiskAction from PUT content, generating default values where necessary. */
  private def make(user: Identity, entityName: FullyQualifiedEntityName, content: WhiskActionPut)(
    implicit transid: TransactionId) = {
    content.exec map {
      case seq: SequenceExec =>
        // check that the sequence conforms to max length and no recursion rules
        checkSequenceActionLimits(entityName, seq.components) map { _ =>
          makeWhiskAction(content.replace(seq), entityName)
        }
      case supportedExec if !supportedExec.deprecated =>
        Future successful makeWhiskAction(content, entityName)
      case deprecatedExec =>
        Future failed RejectRequest(BadRequest, runtimeDeprecated(deprecatedExec))

    } getOrElse Future.failed(RejectRequest(BadRequest, "exec undefined"))
  }

  /** Updates a WhiskAction from PUT content, merging old action where necessary. */
  private def update(user: Identity, content: WhiskActionPut)(action: WhiskAction)(implicit transid: TransactionId) = {
    content.exec map {
      case seq: SequenceExec =>
        // check that the sequence conforms to max length and no recursion rules
        checkSequenceActionLimits(FullyQualifiedEntityName(action.namespace, action.name), seq.components) map { _ =>
          updateWhiskAction(content.replace(seq), action)
        }
      case supportedExec if !supportedExec.deprecated =>
        Future successful updateWhiskAction(content, action)
      case deprecatedExec =>
        Future failed RejectRequest(BadRequest, runtimeDeprecated(deprecatedExec))
    } getOrElse {
      if (!action.exec.deprecated) {
        Future successful updateWhiskAction(content, action)
      } else {
        Future failed RejectRequest(BadRequest, runtimeDeprecated(action.exec))
      }
    }
  }

  /**
   * Updates a WhiskAction instance from the PUT request.
   */
  private def updateWhiskAction(content: WhiskActionPut, action: WhiskAction)(implicit transid: TransactionId) = {
    val limits = content.limits map { l =>
      ActionLimits(
        l.timeout getOrElse action.limits.timeout,
        l.memory getOrElse action.limits.memory,
        l.logs getOrElse action.limits.logs)
    } getOrElse action.limits

    // This is temporary while we are making sequencing directly supported in the controller.
    // Actions that are updated with a sequence will have their parameter property overridden.
    // Actions that are updated with non-sequence actions will either set the parameter property according to
    // the content provided, or if that is not defined, and iff the previous version of the action was not a
    // sequence, inherit previous parameters. This is because sequence parameters are special and should not
    // leak to non-sequence actions.
    // If updating an action but not specifying a new exec type, then preserve the previous parameters if the
    // existing type of the action is a sequence (regardless of what parameters may be defined in the content)
    // otherwise, parameters are inferred from the content or previous values.
    // Note: While changing the implementation of sequences, components now store the fully qualified entity names
    // (which loses the leading "/"). Adding it back while both versions of the code are in place. This will disappear completely
    // once the version of sequences with "pipe.js" is removed.
    val parameters = content.exec map {
      case seq: SequenceExec =>
        Parameters("_actions", JsArray(seq.components map { c =>
          JsString("/" + c.toString)
        }))
      case _ =>
        content.parameters getOrElse {
          action.exec match {
            case seq: SequenceExec => Parameters()
            case _                 => action.parameters
          }
        }
    } getOrElse {
      action.exec match {
        case seq: SequenceExec => action.parameters // discard content.parameters
        case _                 => content.parameters getOrElse action.parameters
      }
    }

    val exec = content.exec getOrElse action.exec

    WhiskAction(
      action.namespace,
      action.name,
      exec,
      parameters,
      limits,
      content.version getOrElse action.version.upPatch,
      content.publish getOrElse action.publish,
      (content.annotations getOrElse action.annotations) ++ execAnnotation(exec))
      .revision[WhiskAction](action.docinfo.rev)
  }

  /**
   * Lists actions in package or binding. The router authorized the subject for the package
   * (if binding, then authorized subject for both the binding and the references package)
   * and iff authorized, this method is reached to lists actions.
   *
   * Note that when listing actions in a binding, the namespace on the actions will be that
   * of the referenced packaged, not the binding.
   */
  private def listPackageActions(user: Identity, pkgName: FullyQualifiedEntityName)(implicit transid: TransactionId) = {
    // get the package to determine if it is a package or reference
    // (this will set the appropriate namespace), and then list actions
    // NOTE: these fetches are redundant with those from the authorization
    // and should hit the cache to ameliorate the cost; this can be improved
    // but requires communicating back from the authorization service the
    // resolved namespace
    getEntity(WhiskPackage, entityStore, pkgName.toDocId, Some { (wp: WhiskPackage) =>
      val pkgns = wp.binding map { b =>
        logging.info(this, s"list actions in package binding '${wp.name}' -> '$b'")
        b.namespace.addPath(b.name)
      } getOrElse {
        logging.info(this, s"list actions in package '${wp.name}'")
        pkgName.path.addPath(wp.name)
      }
      // list actions in resolved namespace
      // NOTE: excludePrivate is false since the subject is authorize to access
      // the package; in the future, may wish to exclude private actions in a
      // public package instead
      list(user, pkgns, excludePrivate = false)
    })
  }

  /**
   * Constructs a WhiskPackage that is a merger of a package with its packing binding (if any).
   * This resolves a reference versus an actual package and merge parameters as needed.
   * Once the package is resolved, the operation is dispatched to the action in the package
   * namespace.
   */
  private def mergeActionWithPackageAndDispatch(method: HttpMethod,
                                                user: Identity,
                                                action: EntityName,
                                                ref: Option[WhiskPackage] = None)(wp: WhiskPackage)(
    implicit transid: TransactionId): RequestContext => Future[RouteResult] = {
    wp.binding map {
      case b: Binding =>
        val docid = b.fullyQualifiedName.toDocId
        logging.info(this, s"fetching package '$docid' for reference")
        // already checked that subject is authorized for package and binding;
        // this fetch is redundant but should hit the cache to ameliorate cost
        getEntity(WhiskPackage, entityStore, docid, Some {
          mergeActionWithPackageAndDispatch(method, user, action, Some { wp }) _
        })
    } getOrElse {
      // a subject has implied rights to all resources in a package, so dispatch
      // operation without further entitlement checks
      val params = { ref map { _ inherit wp.parameters } getOrElse wp } parameters
      val ns = wp.namespace.addPath(wp.name) // the package namespace
      val resource = Resource(ns, collection, Some { action.asString }, Some { params })
      val right = collection.determineRight(method, resource.entity)
      logging.info(this, s"merged package parameters and rebased action to '$ns")
      dispatchOp(user, right, resource)
    }
  }

  /**
   * Checks that the sequence is not cyclic and that the number of atomic actions in the "inlined" sequence is lower than max allowed.
   *
   * @param sequenceAction is the action sequence to check
   * @param components the components of the sequence
   */
  private def checkSequenceActionLimits(
    sequenceAction: FullyQualifiedEntityName,
    components: Vector[FullyQualifiedEntityName])(implicit transid: TransactionId): Future[Unit] = {
    // first checks that current sequence length is allowed
    // then traverses all actions in the sequence, inlining any that are sequences
    val future = if (components.size > actionSequenceLimit) {
      Future.failed(TooManyActionsInSequence())
    } else if (components.size == 0) {
      Future.failed(NoComponentInSequence())
    } else {
      // resolve the action document id (if it's in a package/binding);
      // this assumes that entityStore is the same for actions and packages
      WhiskAction.resolveAction(entityStore, sequenceAction) flatMap { resolvedSeq =>
        val atomicActionCnt = countAtomicActionsAndCheckCycle(resolvedSeq, components)
        atomicActionCnt map { count =>
          logging.debug(this, s"sequence '$sequenceAction' atomic action count $count")
          if (count > actionSequenceLimit) {
            throw TooManyActionsInSequence()
          }
        }
      }
    }

    future recoverWith {
      case _: TooManyActionsInSequence => Future failed RejectRequest(BadRequest, sequenceIsTooLong)
      case _: NoComponentInSequence    => Future failed RejectRequest(BadRequest, sequenceNoComponent)
      case _: SequenceWithCycle        => Future failed RejectRequest(BadRequest, sequenceIsCyclic)
      case _: NoDocumentException      => Future failed RejectRequest(BadRequest, sequenceComponentNotFound)
    }
  }

  /**
   * Counts the number of atomic actions in a sequence and checks for potential cycles. The latter is done
   * by inlining any sequence components that are themselves sequences and checking if there if a reference to
   * the given original sequence.
   *
   * @param origSequence the original sequence that is updated/created which generated the checks
   * @param components the components of the a sequence to check if they reference the original sequence
   * @return Future with the number of atomic actions in the current sequence or an appropriate error if there is a cycle or a non-existent action reference
   */
  private def countAtomicActionsAndCheckCycle(
    origSequence: FullyQualifiedEntityName,
    components: Vector[FullyQualifiedEntityName])(implicit transid: TransactionId): Future[Int] = {
    if (components.size > actionSequenceLimit) {
      Future.failed(TooManyActionsInSequence())
    } else {
      // resolve components wrt any package bindings
      val resolvedComponentsFutures = components map { c =>
        WhiskAction.resolveAction(entityStore, c)
      }
      // traverse the sequence structure by checking each of its components and do the following:
      // 1. check whether any action (sequence or not) referred by the sequence (directly or indirectly)
      //    is the same as the original sequence (aka origSequence)
      // 2. count the atomic actions each component has (by "inlining" all sequences)
      val actionCountsFutures = resolvedComponentsFutures map {
        _ flatMap { resolvedComponent =>
          // check whether this component is the same as origSequence
          // this can happen when updating an atomic action to become a sequence
          if (origSequence == resolvedComponent) {
            Future failed SequenceWithCycle()
          } else {
            // check whether component is a sequence or an atomic action
            // if the component does not exist, the future will fail with appropriate error
            WhiskAction.get(entityStore, resolvedComponent.toDocId) flatMap { wskComponent =>
              wskComponent.exec match {
                case SequenceExec(seqComponents) =>
                  // sequence action, count the number of atomic actions in this sequence
                  countAtomicActionsAndCheckCycle(origSequence, seqComponents)
                case _ => Future successful 1 // atomic action count is one
              }
            }
          }
        }
      }
      // collapse the futures in one future
      val actionCountsFuture = Future.sequence(actionCountsFutures)
      // sum up all individual action counts per component
      val totalActionCount = actionCountsFuture map { actionCounts =>
        actionCounts.foldLeft(0)(_ + _)
      }
      totalActionCount
    }
  }

  /**
   * Constructs an "exec" annotation. This is redundant with the exec kind
   * information available in WhiskAction but necessary for some clients which
   * fetch action lists but cannot determine action kinds without fetching them.
   * An alternative is to include the exec in the action list "view" but this
   * will require an API change. So using an annotation instead.
   */
  private def execAnnotation(exec: Exec): Parameters = {
    Parameters(WhiskAction.execFieldName, exec.kind)
  }

  /** Max atomic action count allowed for sequences. */
  private lazy val actionSequenceLimit = whiskConfig.actionSequenceLimit.toInt

  implicit val stringToFiniteDuration: Unmarshaller[String, FiniteDuration] = {
    Unmarshaller.strict[String, FiniteDuration] { value =>
      val max = WhiskActionsApi.maxWaitForBlockingActivation.toMillis

      Try { value.toInt } match {
        case Success(i) if i > 0 && i <= max => i.milliseconds
        case _ =>
          throw new IllegalArgumentException(Messages.invalidTimeout(WhiskActionsApi.maxWaitForBlockingActivation))
      }
    }
  }
}

private case class TooManyActionsInSequence() extends IllegalArgumentException
private case class NoComponentInSequence() extends IllegalArgumentException
private case class SequenceWithCycle() extends IllegalArgumentException
