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
import java.util.EnumMap;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ComplexModel {
	public enum Direction{NORTH, SOUTH, EAST, WEST};
	private Direction direction;
	private EnumMap<Direction, List<Long>> history;
	private List<ImmutableModel> models;
	private Map<String, Map<String, Object>> children;
}
