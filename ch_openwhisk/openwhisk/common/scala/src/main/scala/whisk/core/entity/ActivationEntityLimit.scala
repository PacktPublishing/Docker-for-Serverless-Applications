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

package whisk.core.entity

import whisk.core.entity.size.SizeInt

/**
 * ActivationEntityLimit defines the limits on the input/output payloads for actions
 * and triggers. This refers to the invoke-time parameters for actions or the trigger-time
 * parameters for triggers.
 */
protected[core] object ActivationEntityLimit {
  protected[core] val MAX_ACTIVATION_ENTITY_LIMIT = 1.MB
}
