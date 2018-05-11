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

-- A fake oauth provider for testing
local cjson = require "cjson"
local _M = {}
function _M.process (red, token)
  local result
  if token == "test" then
    local goodResult = [[
      {
       "email":"test@test.com"
      }
    ]]
    red:set('oauth:providers:mock:tokens:test', goodResult)
    return cjson.encode(goodResult)
  else
    return nil
  end
end

return _M
