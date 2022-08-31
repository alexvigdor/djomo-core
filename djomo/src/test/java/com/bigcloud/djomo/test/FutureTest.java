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

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.StaticType;

import org.testng.Assert;

public class FutureTest {

	@Test
	public void testFuture() throws Exception {
		Json json = new Json();
		ExecutorService executor = Executors.newCachedThreadPool();
		Results results = new Results(executor.submit(() -> new Result("abc", 123)),
				executor.submit(() -> new Result("def", 456)));
		String str = json.toString(results);
		Assert.assertEquals(str, "{\"firstPass\":{\"name\":\"abc\",\"value\":123},\"secondPass\":{\"name\":\"def\",\"value\":456}}");
		Results rt = json.fromString(str, Results.class);
		Assert.assertEquals(rt.firstPass.get(), results.firstPass.get());
		Map map = json.fromString(str, Map.class);
		Future<Result> fp2 = (Future<Result>) json.models().get(new StaticType<Future<Result>>() {}).convert(map.get("firstPass"));
		Assert.assertEquals(fp2.get(), results.firstPass.get());
		executor.shutdown();
	}

	public static record Results(Future<Result> firstPass, Future<Result> secondPass) {

	}

	public static record Result(String name, long value) {
	}
}
