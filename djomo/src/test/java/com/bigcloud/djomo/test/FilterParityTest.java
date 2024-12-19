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
package com.bigcloud.djomo.test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Locale;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.Resolver;
import com.bigcloud.djomo.annotation.Order;
import com.bigcloud.djomo.annotation.Parse;
import com.bigcloud.djomo.annotation.Resolve;
import com.bigcloud.djomo.annotation.Visit;
import com.bigcloud.djomo.api.Field;
import com.bigcloud.djomo.api.Model;
import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.ObjectModel;
import com.bigcloud.djomo.api.Parser;
import com.bigcloud.djomo.base.BaseParserFilter;
import com.bigcloud.djomo.base.BaseVisitorFilter;
import com.bigcloud.djomo.filter.parsers.LimitParser;
import com.bigcloud.djomo.filter.parsers.RenameParser;
import com.bigcloud.djomo.filter.visitors.LimitVisitor;
import com.bigcloud.djomo.filter.visitors.RenameVisitor;

import lombok.Builder;
import lombok.Data;

public class FilterParityTest {
	Models Models = new Models();

	@Test
	public void testParity() throws IOException {
		Json json = Json.builder().models(Models).scan(ThingImpl.class).build();
		ParityModel firstModel = ParityModel.builder().realName("First Thing").altName("For Now").build();
		String str1 = json.toString(firstModel);
		Assert.assertEquals(str1, "{\"altName\":\"For Now\",\"realName\":\"First Thing\"}");
		String str2 = json.toString(firstModel, new ParityModelVisitorFilter());
		Assert.assertEquals(str2, "{\"alt-name\":\"For Now\",\"real-name\":\"First Thing\"}");
		ParityModel firstRound = json.fromString(str1, ParityModel.class);
		ParityModel firstRoundB = json.fromString(str2, ParityModel.class, new ParityModelParserFilter());
		Assert.assertEquals(firstRound, firstRoundB);
		json = Json.builder().models(Models).scan(ParityModel.class).build();
		String str3 = json.toString(firstModel);
		Assert.assertEquals(str3, "{\"alt-name\":\"For Now\",\"real-name\":\"First Thing\"}");
		ParityModel secondRound = json.fromString(str3, ParityModel.class);
		Assert.assertEquals(secondRound, firstModel);
	}

	@Data
	@Builder
	@Parse(ParityModelParserFilter.class)
	@Visit(ParityModelVisitorFilter.class)
	public static class ParityModel {
		String realName;
		String altName;
	}

	public static class ParityModelParserFilter extends RenameParser{

		public ParityModelParserFilter() {
			super(ParityModel.class, "real-name", "realName", "alt-name", "altName");
		}

	}

	public static class ParityModelVisitorFilter extends RenameVisitor {

		public ParityModelVisitorFilter() {
			super(ParityModel.class, "realName", "real-name", "altName", "alt-name");
		}

	}
	
	@Test
	public void testMixedFilters() throws IOException {
		Thing testData = thing("root",
				thing("branch1",
						thing("subbranch1c","red",
								thing("subsub", thing("subsubsub"))),
						thing("subbranch2",
								thing("subsub2"),
								thing("subsubbranch3", "green",
										thing("subsubsub3"),
										thing("subsubsub3b", thing("1", thing("11"), thing("12")), thing("2", "blue", thing("21"), thing("22")), thing("3"), thing("4")),
										thing("subsubsub3c"),
										thing("subsubsub3d"),
										thing("subsubsub3e")
									)
								),
						thing("subbranch3"),
						thing("subbranch4")
						));
		Json json = Json.builder().models(Models).scan(ThingImpl.class).build();
		Locale orig = Locale.getDefault();
		Locale.setDefault(Locale.US);
		String output = json.toString(testData);
		//System.out.println(output);
		Assert.assertEquals(output, "{\"id\":\"root\",\"contents\":[{\"id\":\"branch1\",\"contents\":[{\"id\":\"subbranch1c\",\"color\":\"red\",\"contents\":[{\"id\":\"subsub\",\"subcontents\":[{\"id\":\"subsubsub\",\"contents\":[]}]}]},{\"id\":\"subbranch2\",\"contents\":[{\"id\":\"subsub2\",\"contents\":[]},{\"id\":\"subsubbranch3\",\"color\":\"green\",\"contents\":[{\"id\":\"subsubsub3\",\"subcontents\":[]},{\"id\":\"subsubsub3b\",\"subcontents\":[{\"id\":\"1\",\"contents\":[{\"id\":\"11\",\"contents\":[]},{\"id\":\"12\",\"contents\":[]}]},{\"id\":\"2\",\"color\":\"blue\",\"contents\":[{\"id\":\"21\",\"subcontents\":[]},{\"id\":\"22\",\"subcontents\":[]}]},{\"id\":\"3\",\"contents\":[]}]},{\"id\":\"subsubsub3c\",\"subcontents\":[]}]}]},{\"id\":\"subbranch3\",\"contents\":[]}]}]}");
		Thing round = json.fromString(output, Thing.class);
		String rj = json.toString(round);
		Assert.assertEquals(rj,"{\"id\":\"root\",\"contents\":[{\"id\":\"branch1\",\"contents\":[{\"id\":\"subbranch1c\",\"color\":\"red\",\"contents\":[{\"id\":\"subsub\",\"subcontents\":[{\"id\":\"subsubsub\",\"contents\":[]}]}]},{\"id\":\"subbranch2\",\"contents\":[{\"id\":\"subsub2\",\"contents\":[]},{\"id\":\"subsubbranch3\",\"color\":\"green\",\"contents\":[{\"id\":\"subsubsub3\",\"subcontents\":[]},{\"id\":\"subsubsub3b\",\"subcontents\":[{\"id\":\"1\",\"contents\":[{\"id\":\"11\",\"contents\":[]},{\"id\":\"12\",\"contents\":[]}]},{\"id\":\"2\",\"color\":\"blue\",\"contents\":[{\"id\":\"21\",\"subcontents\":[]},{\"id\":\"22\",\"subcontents\":[]}]}]}]}]}]}]}");
		Locale.setDefault(Locale.UK);
		output = json.toString(testData);
		Assert.assertEquals(output, "{\"id\":\"root\",\"contents\":[{\"id\":\"branch1\",\"contents\":[{\"id\":\"subbranch1c\",\"colour\":\"red\",\"contents\":[{\"id\":\"subsub\",\"subcontents\":[{\"id\":\"subsubsub\",\"contents\":[]}]}]},{\"id\":\"subbranch2\",\"contents\":[{\"id\":\"subsub2\",\"contents\":[]},{\"id\":\"subsubbranch3\",\"colour\":\"green\",\"contents\":[{\"id\":\"subsubsub3\",\"subcontents\":[]},{\"id\":\"subsubsub3b\",\"subcontents\":[{\"id\":\"1\",\"contents\":[{\"id\":\"11\",\"contents\":[]},{\"id\":\"12\",\"contents\":[]}]},{\"id\":\"2\",\"colour\":\"blue\",\"contents\":[{\"id\":\"21\",\"subcontents\":[]},{\"id\":\"22\",\"subcontents\":[]}]},{\"id\":\"3\",\"contents\":[]}]},{\"id\":\"subsubsub3c\",\"subcontents\":[]}]}]},{\"id\":\"subbranch3\",\"contents\":[]}]}]}");
		round = json.fromString(output, Thing.class);
		rj = json.toString(round);
		Assert.assertEquals(rj,"{\"id\":\"root\",\"contents\":[{\"id\":\"branch1\",\"contents\":[{\"id\":\"subbranch1c\",\"colour\":\"red\",\"contents\":[{\"id\":\"subsub\",\"subcontents\":[{\"id\":\"subsubsub\",\"contents\":[]}]}]},{\"id\":\"subbranch2\",\"contents\":[{\"id\":\"subsub2\",\"contents\":[]},{\"id\":\"subsubbranch3\",\"colour\":\"green\",\"contents\":[{\"id\":\"subsubsub3\",\"subcontents\":[]},{\"id\":\"subsubsub3b\",\"subcontents\":[{\"id\":\"1\",\"contents\":[{\"id\":\"11\",\"contents\":[]},{\"id\":\"12\",\"contents\":[]}]},{\"id\":\"2\",\"colour\":\"blue\",\"contents\":[{\"id\":\"21\",\"subcontents\":[]},{\"id\":\"22\",\"subcontents\":[]}]}]}]}]}]}]}");
		Locale.setDefault(orig);
	}

	@Resolve(ThingResolver.class)
	@Parse(value = RenameParser.class, arg = { "id", "iD" }, type=Thing.class)
	@Visit(value = RenameVisitor.class, arg = { "iD", "id" }, type=Thing.class)
	@Parse(value = RenameParser.class, arg = { "subcontents", "contents" }, path = "**.contents[*].subcontents", type=Thing.class)
	@Visit(value = ColorContentsVisitor.class)
	@Visit(ColorLocaleVisitor.class)
	@Parse(ColorLocaleParser.class)
	public static interface Thing {
		String getID();

		Thing[] getContents();
	}

	public static class ThingResolver extends Resolver<Thing> {
		Model<ColorThingImpl> colorThingModel;
		Model<ThingImpl> plainThingModel;

		@Override
		public Thing resolve(Parser parser) {
			Thing maxThing = colorThingModel.parse(parser);
			if (maxThing instanceof ColorThing) {
				if (((ColorThing) maxThing).getColor() == null) {
					return plainThingModel.convert(maxThing);
				}
			}
			return maxThing;
		}
		
		@Override
		public void init(ModelContext models, Type[] args) {
			colorThingModel = models.get(ColorThingImpl.class);
			plainThingModel = models.get(ThingImpl.class);
		}

	}

	public static class ColorLocaleVisitor extends BaseVisitorFilter {
		@Override
		public void visitObjectField(Object name) {
			String n = name.toString();
			if (n.equals("color")) {
				if (Locale.getDefault() == Locale.UK) {
					name = "colour";
				}
			}
			visitor.visitObjectField(name);
		}

	}

	public static class ColorLocaleParser extends BaseParserFilter {
		
		@Override
		public Field parseObjectField(ObjectModel model, CharSequence field) {
			if(field.equals("colour")) {
				field = "color";
			}
			return super.parseObjectField(model, field);
		}

	}
	
	public static class ColorContentsVisitor extends BaseVisitorFilter {
		Object parent;
		Object grandParent;

		@Override
		public <T> void visitObject(T model, ObjectModel<T> definition) {
			Object op = parent, og = grandParent;
			grandParent = op;
			parent = model;
			visitor.visitObject(model, definition);
			parent = op;
			grandParent = og;
		}

		@Override
		public void visitObjectField(Object name) {
			if (grandParent instanceof ColorThing && name.equals("contents")) {
				name = "subcontents";
			}
			visitor.visitObjectField(name);
		}

	}

	
	public static interface ColorThing extends Thing {
		String getColor();
	}

	@Visit(value = LimitVisitor.class, arg = "3")
	@Parse(value = LimitParser.class, arg = "2")
	@Order({ "iD", "color", "contents" })
	public static class ThingImpl implements Thing {
		String id;
		Thing[] contents;

		@Override
		public String getID() {
			return id;
		}

		@Override
		public Thing[] getContents() {
			return contents;
		}

		public void setID(String id) {
			this.id = id;
		}

		public void setContents(Thing[] contents) {
			this.contents = contents;
		}
	}

	public static class ColorThingImpl extends ThingImpl implements ColorThing {
		private String color;

		public String getColor() {
			return color;
		}

		public void setColor(String color) {
			this.color = color;
		}
	}

	public Thing thing(String id, Thing... contents) {
		ThingImpl t = new ThingImpl();
		t.setID(id);
		t.setContents(contents);
		return t;
	}

	public Thing thing(String id, String color, Thing... contents) {
		ColorThingImpl t = new ColorThingImpl();
		t.setID(id);
		t.setContents(contents);
		t.setColor(color);
		return t;
	}
}
