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
package com.bigcloud.djomo.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.bigcloud.djomo.filter.FilterVisitor;
/**
 * Define and configure a visitor filter; can be loaded in a Json.Builder using the scan method, or
 * used on a jax-rs endpoint to control serialization.  For lower-level use, the AnnotationProcessor 
 * acquired from a Json can be used to scan a class and materialize FilterVisitors from Visit annotations found there.
 * 
 * @author Alex Vigdor
 *
 */
@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Repeatable(Visits.class)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Visit {
	Class<? extends FilterVisitor> value();
	String[] path() default {};
	String[] arg() default {};
	Class<? extends Object> type() default Object.class;
}
