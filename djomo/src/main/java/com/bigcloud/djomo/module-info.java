/*******************************************************************************
 * Copyright 2022 Alex Vigdor
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
/**
 * <p>
 * Core classes, interfaces and annotations used in parsing and writing Json documents.
 * </p>
 * 
 * @author Alex Vigdor
 *
 * @uses com.bigcloud.djomo.api.ModelFactory
 */
module com.bigcloud.djomo {
	exports com.bigcloud.djomo;
	exports com.bigcloud.djomo.api;
	exports com.bigcloud.djomo.api.parsers;
	exports com.bigcloud.djomo.api.visitors;
	exports com.bigcloud.djomo.annotation;
	exports com.bigcloud.djomo.base;
	exports com.bigcloud.djomo.io;
	exports com.bigcloud.djomo.json;
	exports com.bigcloud.djomo.filter;
	exports com.bigcloud.djomo.filter.parsers;
	exports com.bigcloud.djomo.filter.visitors;
	exports com.bigcloud.djomo.error;
	exports com.bigcloud.djomo.poly;
	exports com.bigcloud.djomo.list;
	exports com.bigcloud.djomo.object;
	exports com.bigcloud.djomo.simple;
	exports com.bigcloud.djomo.internal to com.bigcloud.djomo.test;

	uses com.bigcloud.djomo.api.ModelFactory;
}