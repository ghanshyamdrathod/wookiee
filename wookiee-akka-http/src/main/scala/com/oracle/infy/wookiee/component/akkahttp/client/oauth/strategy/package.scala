/*
 *  Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.oracle.infy.wookiee.component.akkahttp.client.oauth

package object strategy {
  implicit val authorizationCodeStrategy: AuthorizationCodeStrategy = new AuthorizationCodeStrategy
  implicit val clientCredentialsStrategy: ClientCredentialsStrategy = new ClientCredentialsStrategy
  implicit val implicitStrategy: ImplicitStrategy = new ImplicitStrategy
  implicit val passwordCredentialsStrategy: PasswordCredentialsStrategy = new PasswordCredentialsStrategy
  implicit val refreshTokenStrategy: RefreshTokenStrategy = new RefreshTokenStrategy
}
