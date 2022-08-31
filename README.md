# Djomo :: Data Models for Java Objects

a small, fast and extensible java library for reading and writing JSON and performing data transformations

## What is a Model?

A model is a logical structure composed of *objects with fields* and *lists with items*, with the terminal elements in the structure composed of primitives like strings, numbers and booleans.  The concept of a model maps directly onto the structure of a JSON document when serialized.  In java, a model might be represented using

* Java collections (lists and maps)
* Java Beans (field getters and setters)
* Builder & Immutable objects (builder pattern e.g. Lombok @Value @Builder)
* Java records (jdk 16+ immutable object)
* Java temporal objects (java.time)
* Java enums
* Strings
* Numbers
* Booleans

Djomo represents these and related concepts with a set of interfaces and concrete implementations. The class `Models` is used as a runtime lookup/cache/context for model implementations; it makes use of ModelFactory implementations to lazy-load models on demand, and offers a `Resolver` extension interface for plugging in logic to decide on a concrete type to use when parsing to an abstract model or interface.

The two primary interfaces available for working with data are `Visitor`, for walking an existing object structure in order to analyze or serialize it, and `Parser`, for materializing an object structure from some other representation.  A high-level `Json` utility class has convenient methods for parsing or serializing data using string form, using readers and writers or streams of binary UTF-8 encoded text.  

The behavior of parse and visit operations can be completely customized by extending the `FilterVisitor` and `FilterParser` base classes, which allow you to intercept the recursive calls through a Visitor or Parser.   Common use cases for filtering might be limiting the set of fields serialized for an object, renaming object fields, injecting computed field values, excluding null values, checking for circular references, limiting collection sizes, transforming one data type to another, dereferencing data pointers or applying path-based filters to specific locations in a model.  The Model API comes with a number of base classes to support filtering, as shown in the examples below.  This programming approach means that transformations can be made in a feed-forward and just-in-time fashion, avoiding extra data copies, mutations or the overhead of processing and holding a complete transformed data model in memory.  Built-in models are also provided to support Streams and Futures, for compatibility with arbitrary streaming and asynchronous data sources.


### Djomo basics

The two core classes you will use to start with are `com.bigcloud.djomo.Models` and `com.bigcloud.djomo.Json`.  Each are concrete types; Models is a heavyweight and preferably long-lived object that acts as a pull-through cache for Model implementations.  Json is a lighter class that provides convenient methods for parsing and serializing, but it must construct a Models instance internally if you don't pass one in.

First let's serialize and parse regular collections; while in this example `Map.of(...)` does not produce an ordered map, `Json.fromString(...)` does by default return a LinkedHashMap that maintains the field order of the source json.

```
import com.bigcloud.djomo.Json;
Json json = new Json();
Map data = Map.of("name", "John Doe", "age", 42, "aliases", List.of("Johnny", "Jawn"));
String str = json.toString(data);
System.out.println(str); 
	// {"age":42,"aliases":["Johnny","Jawn"],"name":"John Doe"}
Object round = json.fromString(str);
assertEquals(round, data);
```

Visiting models during serialization always relies on the actual type of the objects encountered when walking the model; during parsing the target model must be passed in to get anything other than LinkedHashMaps, Lists, Strings and primitives back.  The target model can be specified as a class:

```
public class MyBean{
	public String name;
	public int age;
	public List<String> aliases;
}

Json json = new Json();
String str = "{\"age\":42,\"aliases\":[\"Johnny\",\"Jawn\"],\"name\":\"John Doe\"}";
MyBean mb = json.fromString(str, MyBean.class);
String round = json.toString(mb);
assertEquals(round, str);
```

You can also specify the target model using generic collection definitions using the `StaticType` abstract class.  In this example, we would normally get a List of Integers parsing this json, but using StaticType we declare we want Doubles instead.

```
import com.bigcloud.djomo.StaticType;

Json json = new Json();
String str = "[1,2,3,4]";
List<Double> data = json.fromString(str, new StaticType<List<Double>>() {});
assertEquals(data.get(0).getClass(), Double.class);
```

While that approach is clean and compiler friendly when you know the concrete target type when you're writing the code, there may be times you want to be more flexible.  In this case we can use the ModelType helper

```
import com.bigcloud.djomo.ModelType;

Json json = new Json();
String str = "[1,2,3,4]";
Object data = json.fromString(str, ModelType.of(List.class, Double.class));
String round = json.toString(data);
System.out.println(round); 
	// [1.0,2.0,3.0,4.0]
```

You can also parse into an existing object to perform a merge of the models.  This can be used to support partial updates with list merging.  Using the MyBean class above:

```
Json json = new Json();
String str1 = "{\"name\":\"First\",\"age\":1,\"aliases\":[\"uno\"]}";
String str2 = "{\"name\":\"Second\",\"aliases\":[\"dos\"]}";
MyBean mb = json.fromString(str1, MyBean.class);
mb = json.fromString(str2, mb);
System.out.println(json.toString(mb)); 
	// {"age":1,"aliases":["uno","dos"],"name":"Second"}
```

#### Resolver: Parsing to interfaces and abstract classes

If you need to perform polymorphic deserialization, for example due to an interface in the model for which an implementation must be chosen, you can use a Resolver to implement the custom logic.  In the simplest case, you might just need to substitute a concrete implementation of the interface.  We can create a subclass of Resolver.Substitute with concrete types and add it the Models at build time.

```
import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.Resolver;

public interface Person {
	String getName();
	List<Person> getFriends();
}

@Value
@Builder
public class PersonImpl implements Person {
	String name;
	List<Person> friends;
}

public class PersonResolver extends Resolver.Substitute<Person, PersonImpl> {
}

Models models = Models.builder().resolver(new PersonResolver()).build();
Json json = new Json(models);
String str = "{\"name\":\"Jim\",\"friends\":[{\"name\":\"John\"}]}";
Person person = json.fromString(str, Person.class);
assertEquals(person.getFriends().get(0).getName(), "John");
```
 
It is also possible to permanently attach a Resolver to a class or interface under your control using the @Resolve annotation, in which case you do not need to explicitly configure the resolver as above.

```
@Resolve(PersonResolver.class)
public interface Person {
	String getName();
	List<Person> getFriends();
}
Json json = new Json();
String str = "{\"name\":\"Jim\",\"friends\":[{\"name\":\"John\"}]}";
Person person = json.fromString(str, Person.class);
assertEquals(person.getFriends().get(0).getName(), "John");
```

In other cases you might need to parse and analyze a JSON structure to determine the right implementation to choose.  In this example we have a Resolver that first parses the JSON to a Map, and uses the value of a field to decide which implementation to choose.  It then converts the parsed Map into the selected model.

```
public interface CarPart {
	String getType();
}

public class Wheel implements CarPart {
	@Override
	public String getType() {
		return "wheel";
	}
}

public class Windshield implements CarPart {
	@Override
	public String getType() {
		return "windshield";
	}
}

public static class CarPartResolver extends Resolver<CarPart> {
	Model<Map> mapModel;
	Model<Wheel> wheelModel;
	Model<Windshield> windshieldModel;

	@Override
	public void init(Models models, Type[] args) {
		mapModel = models.mapModel;
		wheelModel = models.get(Wheel.class);
		windshieldModel = models.get(Windshield.class);
	}

	@Override
	public CarPart resolve(Parser parser) {
		var data = parser.parse(mapModel);
		var type = data.get("type");
		Model<? extends CarPart> model;
		if ("wheel".equals(type)) {
			model = wheelModel;
		} else if ("windshield".equals(type)) {
			model = windshieldModel;
		} else {
			throw new IllegalArgumentException("Unknown part " + type);
		}
		return model.convert(data);
	}
}

Json json = new Json(Models.builder().resolver(new CarPartResolver()).build());
String str = "[{\"type\":\"wheel\"},{\"type\":\"windshield\"}]";
List<CarPart> parts = json.fromString(str, new StaticType<List<CarPart>>() {});
assertEquals(parts.get(0).getClass(), Wheel.class);
assertEquals(parts.get(1).getClass(), Windshield.class);
```

#### Filters: customizing the parser and visitor

Now let's have a look at the more general purpose extension mechanism, filters.  Filters allow you to modify the behavior of Visitors and Parsers; to warmup, let's use a plain Visitor just to log what's happening while we visit a model.

```
import com.bigcloud.djomo.base.BaseVisitor;
List data = List.of(1, Map.of("a", "b", "c", "d"), 2);
new BaseVisitor() {
	@Override
	public void visit(Object o) {
		System.out.println("Start visit " + o);
		super.visit(o);
		System.out.println("End visit " + o);
	}
}.visit(data);

/* prints out
Start visit [1, {c=d, a=b}, 2]
Start visit 1
End visit 1
Start visit {c=d, a=b}
Start visit d
End visit d
Start visit b
End visit b
End visit {c=d, a=b}
Start visit 2
End visit 2
End visit [1, {c=d, a=b}, 2]
*/
```

The log output above shows the recursive nature of the call stack, where all the objects beneath a given node are visited before the parent node visit is complete.  This makes it easy to use local variables on the call stack to maintain filter state, which can be very efficient.

A FilterVisitor allows you to wire additional logic into another visitor - now let's use a FilterVisitor to log our progress as we serialize the object to json.  This produces the exact same output as above; instead of extending BaseVisitor however we have extended FilterVisitor, to add our custom logic to an existing visitor (in this case the json writer).  The FilterVisitor base class implements Visitor with a default pass through for all methods, so you can just extend the method(s) of interest.

```
import com.bigcloud.djomo.filter.FilterVisitor;
json.toString(data, new FilterVisitor() {
	@Override
	public void visit(Object o) {
		System.out.println("Start visit " + o);
		super.visit(o);
		System.out.println("End visit " + o);
	}
});
```

A ParserFilter allows similar customization of a parser, using a simple FilterParser base class that by default passes through all parse methods:

```
import com.bigcloud.djomo.filter.FilterParser;
String str = "[1,{\"c\":\"d\",\"a\":\"b\"},2]";
json.fromString(str, new FilterParser() {
	@Override
	public <T> T parse(Model<T> model) {
		System.out.println("Parsing model " + model.getType());
		T t = parser.parse(model);
		System.out.println("Done Parsing model " + model.getType() + " " + t);
		return t;
	}
});
/* prints out
Parsing model class java.lang.Object
Parsing model class java.lang.Object
Done Parsing model class java.lang.Object 1
Parsing model class java.lang.Object
Parsing model class java.lang.Object
Done Parsing model class java.lang.Object d
Parsing model class java.lang.Object
Done Parsing model class java.lang.Object b
Done Parsing model class java.lang.Object {c=d, a=b}
Parsing model class java.lang.Object
Done Parsing model class java.lang.Object 2
Done Parsing model class java.lang.Object [1, {c=d, a=b}, 2]
*/
```

There are common filters you might find the occasion to use, such as the CircularReferenceVisitor that detects if the same instance is already in the visit stack and blocks it to prevent infinite looping.

```
import com.bigcloud.djomo.filter.CircularReferenceVisitor;
Map self = new HashMap();
self.put("a", "b");
self.put("self", self);
self.put("related", List.of("other", self));
try {
	String json = Json.toString(self);
}
catch(StackOverflowError e) {
	System.out.println(e);
		//java.lang.StackOverflowError
}
String json = Json.toString(self, new CircularReferenceVisitor());
System.out.println(json);
	// {"a":"b","related":["other"]}
```

Or the OmitNull filter that allows more compact serialization by omitting null valued fields and list items

```
import com.bigcloud.djomo.filter.OmitNullVisitor;
Map data = new HashMap();
data.put("a", "b");
data.put("c", null);
data.put("d", Arrays.asList(1, null, 2));
String str = json.toString(data);
System.out.println(str);
	// {"a":"b","c":null,"d":[1,null,2]}
str = json.toString(data, new OmitNullVisitor());
System.out.println(str);
	// {"a":"b","d":[1,2]}
```

The examples so far show how to pass a concrete filter into a parse or visit operation; the Model API also comes with a set of annotations that can be used to define filters, and you can use the annotation processor to read and apply those filters.  A variation on the prior example, we use the class MyFilter to organize one or more visitors that we have defined with the @Visit or @Parse annotation:

```
import com.bigcloud.djomo.annotation.Visit;

@Visit(OmitNullVisitor.class)
public class MyFilter{}

String str = json.toString(data, json.getAnnotationProcessor().visitorFilters(MyFilter.class));
System.out.println(str);
	// {"a":"b","d":[1,2]}
```


it is also possible to install filters at the time the Json is being built, so that all calls through that Json object will invoke those filters.  We could repeat the previous example with a permanently attached filter:

```
json = Json.builder().visit(new OmitNullVisitor()).build();
str = json.toString(data);
System.out.println(str);
	// {"a":"b","d":[1,2]}
```

Or we can use the scan function of the json builder to pick up @Visit or @Parse annotations

```
json = Json.builder().scan(MyFilter.class).build();
str = json.toString(data);
System.out.println(str);
	// {"a":"b","d":[1,2]}
```

A trio of common filters support common operations on fields during either visit or parse; field renaming, field including and field excluding.  These can be combined with a `type` to narrow the effect to specific classes.

```
import com.bigcloud.djomo.filter.ExcludeParser;
import com.bigcloud.djomo.filter.ExcludeVisitor;
import com.bigcloud.djomo.filter.IncludeVisitor;
import com.bigcloud.djomo.filter.RenameParser;
import com.bigcloud.djomo.filter.RenameVisitor;

public record Gadget(String name, Gear gear, Gauge gauge) {}

public record Gear(String name, Double value, Double radius) {}

public record Gauge(String name, Double value, Double max) {}

@Visit(value = IncludeVisitor.class, type = Gauge.class, arg = { "name", "value" })
@Visit(value = ExcludeVisitor.class, type = Gear.class, arg = "value")
@Visit(value = RenameVisitor.class, arg = { "name", "n" })
@Visit(OmitNullVisitor.class)
@Parse(value = RenameParser.class, arg = { "n", "name" })
@Parse(value = ExcludeParser.class, type = Gauge.class, arg = "value")
public class GadgetFilters {}

Json json = Json.builder().scan(GadgetFilters.class).build();
Gadget gadget = new Gadget("gizmo", new Gear("spur", 13.7, 8.0), new Gauge("pressure", 15.0, 20.0));
String str = json.toString(gadget);
System.out.println(str);
	// {"gauge":{"n":"pressure","value":15.0},"gear":{"n":"spur","radius":8.0},"n":"gizmo"}
Gadget roundTrip = json.fromString(str, Gadget.class);
System.out.println(json.toString(roundTrip));
	// {"gauge":{"n":"pressure"},"gear":{"n":"spur","radius":8.0},"n":"gizmo"}
```

You might want to customize a parse or visit operation for a specific field in other ways, like transforming or limiting the data.  Several base classes are provided to support these use cases - in this example, we have an object with two fields, and we use field filters to limit the size of a list in one field, and reverse a string value in another field.

```
import com.bigcloud.djomo.filter.FieldParser;
import com.bigcloud.djomo.filter.FieldParserFunction;
import com.bigcloud.djomo.filter.FieldVisitor;
import com.bigcloud.djomo.filter.FieldVisitorFunction;
import com.bigcloud.djomo.filter.LimitParser;
import com.bigcloud.djomo.filter.LimitVisitor;

public static record Feed(String name, List<String> ids) {}
	
Feed feed = new Feed("Hello World", List.of("1", "2", "3", "4", "5", "6", "7"));
String str = json.toString(feed, 
	new FieldVisitor<Feed>("ids", new LimitVisitor(5)) {}, 
	new FieldVisitorFunction<Feed, String>("name", s -> new StringBuilder(s).reverse().toString()) {});
System.out.println(str);
	// {"ids":["1","2","3","4","5"],"name":"dlroW olleH"}
Feed roundTrip = json.fromString(str, Feed.class,
	new FieldParser<Feed>("ids", new LimitParser(3)) {},
	new FieldParserFunction<Feed, String, String>("name", s -> new StringBuilder(s).reverse().toString()) {});
System.out.println(json.toString(roundTrip));
	// {"ids":["1","2","3"],"name":"Hello World"}
```

You can also use filters to perform structural modifications to a data type, e.g. to serialize a java object with fields down to a plain string and back.  Supporting a full round trip of structural transformations requires symmetric parser and visitor filters; in this example we create those symmetric filters as classes, and attach them using annotations to a single interface to group them together.

```
@Value
@Builder
public class Contact {
	String firstName;
	String lastName;
}

@Parse(ContactParser.class)
@Visit(ContactVisitor.class)
public interface ContactFilters {}

public class ContactVisitor extends TypeVisitorTransform<Contact> {
	@Override
	public Object transform(Contact contact) {
		String fullName = contact.getFirstName();
		if (contact.getLastName() != null) {
			fullName = fullName + " " + contact.getLastName();
		}
		return fullName;
	}
}

public class ContactParser extends TypeParserTransform<String, Contact> {
	@Override
	public Contact transform(String fullName) {
		String[] nameParts = fullName.split("\\s+", 2);
		Contact.ContactBuilder builder = Contact.builder();
		builder.firstName(nameParts[0]);
		if (nameParts.length > 1) {
			builder.lastName(nameParts[1]);
		}
		return builder.build();
	}
}

Json json = new Json();
Contact contact = new Contact("John", "Von Doe");
String str = json.toString(contact);
System.out.println(str);
	// {"firstName":"John","lastName":"Von Doe"}
Contact roundTrip = json.fromString(str, Contact.class);
assertEquals(roundTrip, contact);
json = Json.builder()
			.scan(ContactFilters.class)
			.build();
str = json.toString(contact);
System.out.println(str);
	// "John Von Doe"
roundTrip = json.fromString(str, Contact.class);
assertEquals(roundTrip, contact);

```

In the basic form, the annotations take as a value the class of the VisitorFilter or ParserFilter to be invoked.  You can also provide `type` and `path` elements to the annotations which will automatically combine the specified filter with a Type or Path filter to limit the scope in which the filter is applied.  In this example, we set up filters to exclude a field by name from a java class, only when it is found under a given path.  We also inject the simple class name as a field for some types.

```
import com.bigcloud.djomo.filter.ExcludeFilter;
import com.bigcloud.djomo.filter.InjectFilter;

public record Thing1(String name, String role) {}

public record Thing2(String name, String role) {}

@Visit(value = ExcludeFilter.class, path = "staff.**", type = Thing2.class, arg = "role")
@Visit(value = InjectType.class, type = Thing1.class)
@Visit(value = InjectType.class, type = Thing2.class)
public interface FilterThings {}

public class InjectType extends InjectFilter<Object> {
	public InjectType() {
		super("type");
	}

	@Override
	public Object value(Object obj) {
		return obj.getClass().getSimpleName();
	}
}

Map things = Map.of("head", new Thing1("Joe", "planning"),
		"officer", new Thing2("Jim", "executing"),
		"staff", List.of(new Thing1("Bob", "walking"),
				new Thing2("Bill", "talking"),
				new Thing1("Jane", "thinking"),
				new Thing2("Joan", "listening")));
Json json = Json.builder()
		.scan(FilterThings.class)
		.build();
String str = json.toString(things);
System.out.println(str);
	// {"head":{"type":"Thing1","name":"Joe","role":"planning"},"officer":{"type":"Thing2","name":"Jim","role":"executing"},"staff":[{"type":"Thing1","name":"Bob","role":"walking"},{"type":"Thing2","name":"Bill"},{"type":"Thing1","name":"Jane","role":"thinking"},{"type":"Thing2","name":"Joan"}]}

```

### Djomo JAX-RS Provider

This tiny module allows you to plug in Djomo as a provider of JSON reading and writing for JAX-RS applications.  It supports customization of Json and Model objects using a custom JAX-RS ContextResolver, and supports the use of @Visit and @Parse annotations from Djomo on any JSON producing endpoint or entity.  It also includes an @Indent annotation to allow for pretty-printing responses.

To use the the providers, you should register these two classes with your JAX-RS application

```
com.bigcloud.djomo.rs.JsonBodyReader.class
com.bigcloud.djomo.rs.JsonBodyWriter.class
```

To customize the Json or Models used by the application, implement a custom ContextResolver<Json> and it will automatically be invoked to supply Json objects for reading and writing.  In this example, we customize the Models to resolve references to the Map interface with a ConcurrentHashMap, instead of the usual LinkedHashMap.  We also add an OmitNullParser filter, since ConcurrentHashMaps don't accept null values.

```
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.Models;
import com.bigcloud.djomo.Resolver;
import com.bigcloud.djomo.filter.OmitNullParser;
import com.bigcloud.djomo.filter.TypeParser;

import jakarta.ws.rs.ext.ContextResolver;

public class JsonResolver implements ContextResolver<Json> {
	Json json = Json.builder()
			.models(Models.builder()
					.resolver(new Resolver.Substitute<Map, ConcurrentHashMap>() {})
					.build())
			.parse(new TypeParser<Map>(new OmitNullParser()) {})
			.build();

	@Override
	public Json getContext(Class<?> type) {
		return json;
	}
}

```

With no changes to your JAX-RS application code, the providers will automatically be invoked by the JAX-RS container to serialize responses and parse request bodies. You may apply @Visit and @Indent annotations on JAX-RS endpoint methods or response entities to customize how JSON is generated; request body parameters can carry the @Parse annotation to customize the parser.  In this way you can configure local transformations, as opposed to global transformations you may have specified in a custom Json ContextResolver as above.

In this example we have an endpoint whose input and output is a flattened representation of an object with a single string field and a list field; pretty printing is controlled by the caller.

```
public record Thing<T>(String name, List<T> elements) {}

import java.util.stream.Stream;
import com.bigcloud.djomo.filter.TypeVisitorTransform;

public class ThingFlattener extends TypeVisitorTransform<Thing> {

	@Override
	public Stream transform(Thing in) {
		return Stream.concat(Stream.of(in.name()), in.elements().stream());
	}
}

import java.util.List;
import com.bigcloud.djomo.filter.TypeParserTransform;

public class ThingUnflattener extends TypeParserTransform<List, Thing> {

	@Override
	public Thing transform(List list) {
		return new Thing(list.get(0).toString(), list.subList(1, list.size()));
	}
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
```
