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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
/**
 * Use on a field, getter or setter to exclude it from a model, so that it will not be visited or parsed
 * 
 * @author Alex Vigdor
 *
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@Inherited
public @interface Ignore {

}
