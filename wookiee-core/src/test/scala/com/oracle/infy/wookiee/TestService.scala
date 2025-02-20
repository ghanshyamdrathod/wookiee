/*
 * Copyright (c) 2022 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oracle.infy.wookiee

import com.oracle.infy.wookiee.health.{ComponentState, HealthComponent}
import com.oracle.infy.wookiee.service.Service
import com.oracle.infy.wookiee.service.messages.{GetMetaDetails, Ready}
import com.oracle.infy.wookiee.service.meta.{ServiceMetaData, ServiceMetaDetails}

import scala.concurrent.Future

class TestService extends Service {
  var metaData: Option[ServiceMetaData] = None

  override def checkHealth: Future[HealthComponent] = {
    val comp = HealthComponent("testservice", ComponentState.NORMAL, "test")
    comp.addComponent(HealthComponent("childcomponent", ComponentState.DEGRADED, "test"))
    Future[HealthComponent] {
      comp
    }
  }

  // Define the receive function
  override def serviceReceive: Receive = {
    case Ready =>
      sender() ! Ready
      log.info("I am now ready: " + self.path)
    case Ready(meta) =>
      metaData = Some(meta)
      log.info("I am now ready, meta data set: " + self.path)
    case TestClass("foo", _) =>
      TestService.gotMessage = true
      sender() ! "gotit"
    case GetMetaDetails => sender() ! ServiceMetaDetails(supportsHttp = false)
  }

}

object TestService {
  var gotMessage: Boolean = false
}
