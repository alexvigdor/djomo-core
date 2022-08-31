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
package com.bigcloud.djomo.list;

import java.util.stream.Stream;

import com.bigcloud.djomo.api.ListMaker;
import com.bigcloud.djomo.base.BaseMaker;

public class StreamMaker<T extends Stream<I>, I> extends BaseMaker<T, StreamModel<T, I>> implements ListMaker<T, I> {
	private final T start;
	private final Stream.Builder<I> builder;
	
	public StreamMaker(StreamModel<T, I> model) {
		this(model, null);
	}
	
	public StreamMaker(StreamModel<T, I> model, T start) {
		super(model);
		this.builder = Stream.builder();
		this.start = start;
	}

	@Override
	public T make() {
		if (start == null) {
			return (T) builder.build();
		}
		return (T) Stream.concat(start, builder.build());
	}

	@Override
	public void item(I item) {
		builder.accept(item);
	}

}
