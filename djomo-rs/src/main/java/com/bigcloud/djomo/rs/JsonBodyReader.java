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
package com.bigcloud.djomo.rs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.channels.Channel;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.filter.FilterParser;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
/**
 * Support parsing of UTF-8 encoded JSON streams into java objects
 * 
 * @author Alex Vigdor
 *
 * @param <T> the type of object to be parsed from Json
 */
@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JsonBodyReader<T> extends JsonContext implements MessageBodyReader<T> {

	public JsonBodyReader() {
		super();
	}

	@Override
	public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		if (Writer.class.isAssignableFrom(type)
				|| OutputStream.class.isAssignableFrom(type)
				|| Channel.class.isAssignableFrom(type)) {
			return false;
		}
		if (mediaType.getSubtype().contains("json")) {
			return true;
		}
		return false;
	}

	@Override
	public T readFrom(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
			throws IOException, WebApplicationException {
		// filters by annotation
		Json json = getJson(type);
		FilterParser[] filters = json.getAnnotationProcessor().parserFilters(annotations);
		if (filters.length == 0) {
			return (T) json.read(entityStream, genericType == null ? type : genericType);
		}
		return (T) json.read(entityStream, genericType == null ? type : genericType, filters);
	}

}
