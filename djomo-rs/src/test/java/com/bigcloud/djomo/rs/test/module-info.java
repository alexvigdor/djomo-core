/*******************************************************************************
 * Copyright 2025 Alex Vigdor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
module com.bigcloud.djomo.rs.test {
	requires com.bigcloud.djomo.rs;
	requires jakarta.ws.rs;
	requires com.bigcloud.djomo;
	requires org.testng;
	requires org.glassfish.jersey.core.client;
	requires org.glassfish.jersey.core.server;
	requires org.glassfish.jersey.tests.framework.core;

	exports com.bigcloud.djomo.rs.test;
}