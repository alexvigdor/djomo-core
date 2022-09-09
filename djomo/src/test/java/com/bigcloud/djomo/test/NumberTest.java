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

import org.testng.Assert;
import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;

public class NumberTest {
	@Test
	public void testDoubles() throws IOException {
		Json json = new Json();
		for (int sign = 0; sign < 2; sign++) {
			for (int exp = 0; exp < 2048; exp++) {
				for (int mantissaBits = 0; mantissaBits < 52; mantissaBits++) {
					for (int evenOdd = 0; evenOdd < 2; evenOdd++) {
						long rawval = sign;
						rawval = (rawval << 11) | exp;
						long mantissa = 0;
						for (int i = 0; i < mantissaBits; i++) {
							mantissa = (mantissa << 1) + 1;
						}
						mantissa = (mantissa << 1) + evenOdd;
						rawval = (rawval << 52) | mantissa;
						Double d = Double.longBitsToDouble(rawval);
						String ds = json.toString(d);
						Double round = Double.valueOf(ds);
						Assert.assertEquals(round, d);
						Double parsed = json.fromString(ds, Double.class);
						Assert.assertEquals(parsed, d);
					}
				}
			}
		}
	}

	@Test
	public void testFloats() throws IOException {
		Json json = new Json();
		for (int sign = 0; sign < 2; sign++) {
			for (int exp = 0; exp < 256; exp++) {
				for (int mantissaBits = 0; mantissaBits < 23; mantissaBits++) {
					for (int evenOdd = 0; evenOdd < 2; evenOdd++) {
						int rawval = sign;
						rawval = (rawval << 8) | exp;
						int mantissa = 0;
						for (int i = 0; i < mantissaBits - 1; i++) {
							mantissa = (mantissa << 1) + 1;
						}
						mantissa = (mantissa << 1) + evenOdd;
						rawval = (rawval << 23) | mantissa;
						Float f = Float.intBitsToFloat(rawval);
						String ds = json.toString(f);
						Float round = Float.valueOf(ds);
						Assert.assertEquals(round, f);
						Float parsed = json.fromString(ds, Float.class);
						Assert.assertEquals(parsed, f);
					}
				}
			}
		}
	}
}
