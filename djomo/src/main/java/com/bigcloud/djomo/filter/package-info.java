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
 * {@link FilterVisitor} and {@link FilterParser} base classes, and a number of common and composable filter implementations.
 * </p><p>
 * Filters can be defined programmatically, or configured using {@link com.bigcloud.djomo.annotation.Visit} and 
 * {@link com.bigcloud.djomo.annotation.Parse} annotations.
 * </p><p>
 * When filters are loaded from annotations using the {@link com.bigcloud.djomo.base.AnnotationProcessor}, the filter instances
 * are constructed once based on the annotation parameters and cached; each time the filter is used for a parse or visit operation,
 * a clone of the original filter instance is made. This way filters can safely use fields to maintain processing state, but they 
 * must properly implement clone() to make sure that state remains truly isolated, for example deep-cloning wrapped filters.
 * </p><p>
 * Filters do not have to define a zero-argument constructor to be initialized by annotation; they may define certain constructor 
 * parameters that can be injected by the AnnotationProcessor.
 * </p>
 * <table>
 * <caption>Possible filter constructor parameter types that can be injected by AnnotationProcessor</caption>
 * <tr><td><b>Models</b></td><td>the active Models, e.g. to get a model to wrap with an IncludeModel</td><tr>
 * <tr><td><b>Model</b></td><td>if the annotation has a <b>type</b> parameter, the corresponding Model will be loaded and passed in</td><tr>
 * <tr><td><b>Class</b></td><td>if the annotation has a <b>type</b> parameter, that Class will be passed in</td><tr>
 * <tr><td><b>String</b></td><td>if the annotation has an <b>arg</b> parameter, the next argument is passed in.  Can be used multiple times when multiple args are expected.</td><tr>
 * <tr><td><b>String[]</b></td><td>if the annotation has an <b>arg</b> parameter, the remaining arguments are passed in.  Must be after other String arguments and cannot repeat.</td><tr>
 * </table>
 */
package com.bigcloud.djomo.filter;