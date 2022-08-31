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
package com.bigcloud.djomo.poly;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.bigcloud.djomo.Resolver;

/**
 * Default concrete type mappings for common JDK interfaces
 * 
 * @author Alex Vigdor
 *
 */
public class DefaultResolverModelFactory extends ResolverModelFactory {
	public DefaultResolverModelFactory() {
		super(
				new Resolver.Substitute<>(SortedMap.class, TreeMap.class),
				new Resolver.Substitute<>(Map.class, LinkedHashMap.class),
				new Resolver.Substitute<>(SortedSet.class, TreeSet.class),
				new Resolver.Substitute<>(Set.class, LinkedHashSet.class),
				new Resolver.Substitute<>(Queue.class, ArrayDeque.class),
				new Resolver.Substitute<>(Deque.class, ArrayDeque.class),
				new Resolver.Substitute<>(List.class, ArrayList.class),
				new Resolver.Substitute<>(Collection.class, ArrayList.class));
	}
}
