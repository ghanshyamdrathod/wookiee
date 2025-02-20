/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.oracle.infy.wookiee.health

import akka.actor.Actor
import akka.pattern._
import akka.util.Timeout
import com.oracle.infy.wookiee.HarnessConstants
import com.oracle.infy.wookiee.logging.ActorLoggingAdapter
import com.oracle.infy.wookiee.service.messages.CheckHealth
import com.oracle.infy.wookiee.utils.ConfigUtil
import org.joda.time.DateTime

import java.util.jar
import java.util.jar.Attributes.Name
import java.util.jar.{Attributes, JarFile}
import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

object HealthCheckProvider {

  /**
    * Rollup the overall status and critical alerts for each component
    * @param checks List of status objects for each component and service
    * @return Parent status that is only NOMRAL if all children were NORMAL
    */
  def rollupHealth(checks: Seq[HealthComponent]): ComponentHealth = {

    // Identify all components in the hierarchy in a DEGRADED or CRITICAL state
    def getAlertComponents(root: HealthComponent): Seq[ComponentHealth] = {
      if (root.state == ComponentState.CRITICAL || root.state == ComponentState.DEGRADED) {
        val rootCompHealth = ComponentHealth(root.state, s"${root.name}[${root.state}] - ${root.details}")
        rootCompHealth +: root.components.flatMap(getAlertComponents)
      } else {
        root.components.flatMap(getAlertComponents)
      }
    }

    val componentsToAlert = checks.flatMap(getAlertComponents)

    if (componentsToAlert.isEmpty) {
      ComponentHealth(ComponentState.NORMAL, "Thunderbirds are GO")
    } else {
      val status =
        if (componentsToAlert.exists(c => c.state == ComponentState.CRITICAL)) ComponentState.CRITICAL
        else ComponentState.DEGRADED
      val details = componentsToAlert.map(_.details)
      ComponentHealth(status, details.mkString("; "))
    }
  }

}

trait HealthCheckProvider {
  this: Actor with ActorLoggingAdapter =>
  val upTime: DateTime = DateTime.now

  implicit val timeout: Timeout =
    ConfigUtil.getDefaultTimeout(
      context.system.settings.config,
      HarnessConstants.KeyDefaultTimeout,
      Timeout(15.seconds)
    )

  val scalaVersion: String = util.Properties.versionString
  val file: String = getClass.getProtectionDomain.getCodeSource.getLocation.getFile

  val manifest: jar.Manifest = file match {
    case _ if file.endsWith(".jar") =>
      new JarFile(file).getManifest
    case _ =>
      val man = new java.util.jar.Manifest()
      man.getMainAttributes.put(Name.IMPLEMENTATION_TITLE, "Oracle Wookiee Service")
      man.getMainAttributes.put(Name.IMPLEMENTATION_VERSION, "develop-SNAPSHOT")
      man.getMainAttributes.put(new Attributes.Name("Implementation-Build"), "N/A")
      man
  }

  val application: String = manifest.getMainAttributes.getValue(Name.IMPLEMENTATION_TITLE)
  val version: String = manifest.getMainAttributes.getValue(Name.IMPLEMENTATION_VERSION)

  /**
    * Run the health checks and return the current system state
    * @return
    */
  def runChecks: Future[ApplicationHealth] = {

    import context.dispatcher

    // Ask for the health of each component
    val future = (context.actorSelection(HarnessConstants.ActorPrefix) ? CheckHealth).mapTo[Seq[HealthComponent]]
    val p = Promise[ApplicationHealth]()

    future.onComplete({
      case Success(checks) =>
        val overallHealth = HealthCheckProvider.rollupHealth(checks)
        p success ApplicationHealth(application, version, upTime, overallHealth.state, overallHealth.details, checks)
      case Failure(e) =>
        log.error("An error occurred while fetching the health request results", e)
        p success ApplicationHealth(application, version, upTime, ComponentState.CRITICAL, e.getMessage, Nil)
    })

    p.future
  }
}
