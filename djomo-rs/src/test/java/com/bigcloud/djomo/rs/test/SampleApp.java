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
package com.bigcloud.djomo.rs.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.bigcloud.djomo.annotation.Parse;
import com.bigcloud.djomo.annotation.Visit;
import com.bigcloud.djomo.filter.ExcludeVisitor;
import com.bigcloud.djomo.filter.FieldVisitorFunction;
import com.bigcloud.djomo.filter.FilterVisitor;
import com.bigcloud.djomo.filter.IncludeVisitor;
import com.bigcloud.djomo.filter.LimitVisitor;
import com.bigcloud.djomo.filter.MultiFilterVisitor;
import com.bigcloud.djomo.filter.OmitNullVisitor;
import com.bigcloud.djomo.filter.TypeParserTransform;
import com.bigcloud.djomo.filter.TypeVisitor;
import com.bigcloud.djomo.filter.TypeVisitorTransform;
import com.bigcloud.djomo.rs.Indent;
import com.bigcloud.djomo.Models;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@Path("sample")
public class SampleApp {
	private static final ThreadLocal<Collection<String>> dynamicExcludes = new ThreadLocal<>();
	@GET
	@Path("thing1")
	@Produces("application/json")
	public Response getThing1() {
		return Response.ok(new Thing<String>("thing1", List.of("a", "b", "c"))).build();
	}

	@POST
	@Path("echo")
	@Consumes("application/json")
	@Produces("application/json")
	public Thing echo(@Parse(value=ProduceUpper.class,path="name") Thing body) {
		return body;
	}

	@POST
	@Path("complex")
	@Consumes("application/json")
	@Produces("application/json")
	public Thing complex(Thing<Map<Integer,List<Integer>>> body) {
		return body;
	}

	@Indent("  ")
	interface Pretty {}

	@POST
	@Path("flatten")
	@Consumes("application/json")
	@Produces("application/json")
	@Visit(ThingFlattener.class)
	@Visit(OmitNullVisitor.class)
	public Response flatten(@Parse(ThingUnflattener.class) Thing body, @QueryParam("pretty") boolean pretty) {
		var response = Response.ok();
		if (pretty) {
			response.entity(body, Pretty.class.getAnnotations());
		} else {
			response.entity(body);
		}
		return response.build();
	}
	
	@POST
	@Path("limit")
	@Consumes("application/json")
	@Produces("application/json")
	@Visit(value=LimitVisitor.class, arg="5")
	public Thing limit(@Parse(ThingUnflattener.class) Thing body) {
		return body;
	}

	@POST
	@Path("foobar")
	@Consumes("application/json")
	@Produces("application/json")
	@Visit(value=ExcludeVisitor.class, path="**.foo.bar")
	public Map foobar(Map body) {
		return body;
	}
	
	@GET
	@Path("pretty")
	@Produces("application/json")
	@Indent
	public Thing pretty() {
		return new Thing("pretty", Arrays.asList("x","y","z"));
	}

	@GET
	@Path("canupper")
	@Produces("application/json")
	@Visit(OmitNullVisitor.class)
	public Response canUpper(@QueryParam("toUpper") boolean toUpper) {
		try {
			var entity = new Thing<Object>("thing2", Arrays.asList("a", null, "b", "c"));
			var builder = Response.ok();
			if (toUpper) {
				builder.entity(entity, UpperTransform.class.getAnnotations());
			} else {
				builder.entity(entity);
			}
			return builder.build();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@GET
	@Path("partial")
	@Produces("application/json")
	@Visit(DynamicExcludeFilter.class)
	public Doc partial(@QueryParam("exclude") List<String> excludes) {
		dynamicExcludes.set(excludes);
		return new Doc("Title", "Author", "Body");
	}
	
	@POST
	@Path("explode")
	@Consumes("application/json")
	@Produces("application/json")
	@Visit(ThingNameExploder.class)
	public Thing explode(Thing body) {
		return body;
	}

	@POST
	@Path("label")
	@Consumes("application/json")
	@Produces("application/json")
	@Visit(value=Labeller.class,path="elements[*]",type=String.class)
	@Visit(value=Labeller.class,path="elements[*]",type=Double.class)
	public Thing label(Thing body) {
		return body;
	}

	@POST
	@Path("readlabel")
	@Consumes("application/json")
	@Produces("application/json")
	public Thing readlabel(
			@Parse(value = ParseLabeller.class, type = Number.class)
			Thing<Number> body) {
		return body;
	}

	@POST
	@Path("sensitive")
	@Consumes("application/json")
	@Produces("application/json")
	@Visit(ThingNameExploder.class)
	public Sensitive sensitive(Sensitive body) {
		return body;
	}
	
	@Visit(value=ToUpper.class, path={"elements[0]", "elements[2]", "elements[4]"})
	private class UpperTransform {
	}

	public static class DynamicExcludeFilter extends ExcludeVisitor{

		@Override
		public boolean exclude(String fieldName) {
			return dynamicExcludes.get().contains(fieldName);
		}
		
	}

	public static class Labeller extends TypeVisitorTransform<Object>{

		@Override
		public Object transform(Object obj) {
			return "("+obj.getClass().getSimpleName()+") "+obj;
		}

	}
	
	public static class ParseLabeller extends TypeParserTransform<Object, Object> {

		@Override
		public Object transform(Object obj) {
			return "("+obj.getClass().getSimpleName()+") "+obj;
		}
		
	}

	public static class ThingNameExploder extends MultiFilterVisitor {

		public ThingNameExploder(Models models) {
			super(new IncludeVisitor<>(models.get(Thing.class), "name"),
					new FieldVisitorFunction<Thing, String>("name", (String n) -> {
						return n.chars().boxed().map(Character::toString).collect(Collectors.toList());
					}) {});
		}
	}
}
