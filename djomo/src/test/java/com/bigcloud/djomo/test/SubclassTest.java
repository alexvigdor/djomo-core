/*******************************************************************************
 * Copyright 2025 Alex Vigdor
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
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;

import lombok.Builder;
import lombok.Value;

public class SubclassTest {

	public static interface Media {
		public String getId();

		public String getName();
	}

	@Value
	@Builder
	public static class Video implements Media {
		String name;
		String id;
		Duration duration;
	}

	public static record MediaRecord(Media media, Date date) {
	};

	public static record MediaListRecord(Date date, Media... media) {
	};

	public static record MediaLookup(Map<String, Media> media, Date date) {
	};

	Json json = new Json();

	private Video makeVideo() {
		return Video.builder().name("Citizen Kane").id("123").duration(Duration.ofHours(2)).build();
	}

	@Test
	public void testMedia() throws IOException {
		Media media = makeVideo();
		String str = json.toString(media);
		Assert.assertEquals(str, "{\"duration\":\"PT2H\",\"id\":\"123\",\"name\":\"Citizen Kane\"}");
	}

	@Test
	public void testVideo() {
		Video media = makeVideo();
		String str = json.toString(media);
		Assert.assertEquals(str, "{\"duration\":\"PT2H\",\"id\":\"123\",\"name\":\"Citizen Kane\"}");
	}

	@Test
	public void testMediaField() throws IOException {
		MediaRecord mediaRecord = new MediaRecord(makeVideo(), new Date(123456789l));
		String str = json.toString(mediaRecord);
		Assert.assertEquals(str,
				"{\"date\":123456789,\"media\":{\"duration\":\"PT2H\",\"id\":\"123\",\"name\":\"Citizen Kane\"}}");
	}

	@Test
	public void testMediaList() throws IOException {
		MediaListRecord mediaListRecord = new MediaListRecord(new Date(123456789l), makeVideo());
		String str = json.toString(mediaListRecord);
		Assert.assertEquals(str,
				"{\"date\":123456789,\"media\":[{\"duration\":\"PT2H\",\"id\":\"123\",\"name\":\"Citizen Kane\"}]}");
	}

	@Test
	public void testMediaLookup() throws IOException {
		MediaLookup mediaLookup = new MediaLookup(Map.of("abc", makeVideo()), new Date(123456789l));
		String str = json.toString(mediaLookup);
		Assert.assertEquals(str,
				"{\"date\":123456789,\"media\":{\"abc\":{\"duration\":\"PT2H\",\"id\":\"123\",\"name\":\"Citizen Kane\"}}}");
	}
}
