--
-- Licensed to the Apache Software Foundation (ASF) under one or more
-- contributor license agreements.  See the NOTICE file distributed with
-- this work for additional information regarding copyright ownership.
-- The ASF licenses this file to You under the Apache License, Version 2.0
-- (the "License"); you may not use this file except in compliance with
-- the License.  You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

--- @module clientSecret
-- Check a subscription with a client id and a hashed secret
local _M = {}

local dataStore = require "lib/dataStore"
local utils = require "lib/utils"
local request = require "lib/request"

local REDIS_HOST = os.getenv("REDIS_HOST")
local REDIS_PORT = os.getenv("REDIS_PORT")
local REDIS_PASS = os.getenv("REDIS_PASS")

--- Process function main entry point for the security block.
--  Takes 2 headers and decides if the request should be allowed based on a hashed secret key
--@param dataStore the datastore object
--@param securityObj the security object loaded from nginx.conf
--@return a string representation of what this looks like in redis :clientsecret:clientid:hashed secret
function process(dataStore, securityObj)
  local result = processWithHashFunction(dataStore, securityObj, utils.hash)
end

--- In order to properly test this functionallity, I use this function to do all of the business logic with injected dependencies
-- Takes 2 headers and decides if the request should be allowed based on a hashed secret key
-- @param dataStore the datastore object
-- @param securityObj the security configuration for the tenant/resource/api we are verifying
-- @param hashFunction the function used to perform the hash of the api secret
function processWithHashFunction(dataStore, securityObj, hashFunction)
  -- pull the configuration from nginx
  local tenant = ngx.var.tenant
  local gatewayPath = ngx.var.gatewayPath
  local apiId = ngx.var.apiId
  local scope = securityObj.scope
  local queryString = ngx.req.get_uri_args()
  local location = (securityObj.location == nil) and 'header' or securityObj.location
  local clientId = nil
  local clientSecret = nil

  -- allow support for custom names in query or header
  local clientIdName = (securityObj.idFieldName == nil) and 'X-Client-ID' or securityObj.idFieldName
  if location == "header" then
    clientId = ngx.var[utils.concatStrings({'http_', clientIdName}):gsub("-", "_")]
  end
  if location == "query" then
    clientId = queryString[clientIdName]
  end
-- if they didn't supply whatever name this is configured to require, error out
  if clientId == nil or clientId == '' then
    request.err(401, clientIdName .. " required")
    return false
  end

-- allow support for custom names in query or header
  local clientSecretName = (securityObj.secretFieldName == nil) and 'X-Client-Secret' or securityObj.secretFieldName
  _G.clientSecretName = clientSecretName:lower()
  if location == "header" then
    clientSecret = ngx.var[utils.concatStrings({'http_', clientSecretName}):gsub("-","_")]
  end
  if location == "query" then
    clientSecret = queryString[clientSecretName]
  end
-- if they didn't supply whatever name this is configured to require, error out
  if clientSecret == nil or clientSecret == '' then
    request.err(401, clientSecretName .. " required")
    return false
  end

-- hash the secret
  local result = validate(dataStore, tenant, gatewayPath, apiId, scope, clientId, hashFunction(clientSecret))
  if result == nil then
    request.err(401, "Secret mismatch or not subscribed to this api.")
  end
  ngx.var.apiKey = clientId
  return result
end

--- Validate that the subscription exists in the dataStore
-- @param dataStore the datastore object
-- @param tenant the tenantId we are checking for
-- @param gatewayPath the possible resource we are checking for
-- @param apiId if we are checking for an api
-- @param scope which values should we be using to find the location of the secret
-- @param clientId the subscribed client id
-- @param clientSecret the hashed client secret
function validate(dataStore, tenant, gatewayPath, apiId, scope, clientId, clientSecret)
-- Open connection to redis or use one from connection pool
  local k
  if scope == 'tenant' then
    k = utils.concatStrings({'subscriptions:tenant:', tenant})
  elseif scope == 'resource' then
    k = utils.concatStrings({'subscriptions:tenant:', tenant, ':resource:', gatewayPath})
  elseif scope == 'api' then
    k = utils.concatStrings({'subscriptions:tenant:', tenant, ':api:', apiId})
  end
  -- using the same key location in redis, just using :clientsecret: instead of :key:
  k = utils.concatStrings({k, ':clientsecret:', clientId, ':', clientSecret})
  if dataStore:exists(k) == 1 then
    return k
  else
    return nil
  end
end
_M.processWithHashFunction = processWithHashFunction
_M.process = process
return _M
