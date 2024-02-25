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
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.channels.Channel;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.api.VisitorFilterFactory;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
/**
 * Support writing java objects out to a UTF-8 encoded Json stream
 * 
 * @author Alex Vigdor
 *
 * @param <T> the type of object to be written to Json
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JsonBodyWriter<T> extends JsonContext implements MessageBodyWriter<T> {

	public JsonBodyWriter() {
		super();
	}

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		if (Reader.class.isAssignableFrom(type)
				|| InputStream.class.isAssignableFrom(type)
				|| Channel.class.isAssignableFrom(type)) {
			return false;
		}
		if (mediaType.getSubtype().contains("json")) {
			return true;
		}
		return false;
	}

	@Override
	public void writeTo(T t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
			throws IOException, WebApplicationException {
		// filters by annotation
		String indent = null;
		for (var a : annotations) {
			if (a instanceof Indent ind) {
				indent = ind.value();
			}
		}
		// System.out.println("Writing "+type.getName()+" with "+filters);
		Json json = getJson(type);
		VisitorFilterFactory[] filters = json.getAnnotationProcessor().visitorFilters(annotations);
		if (filters.length == 0) {
			if (indent == null) {
				json.write(t, entityStream);
			} else {
				json.write(t, entityStream, indent);
			}
		} else {
			if (indent == null) {
				json.write(t, entityStream, filters);
			} else {
				json.write(t, entityStream, indent, filters);
			}
		}
	}

}
