package com.bigcloud.djomo.test;

import java.io.IOException;
import java.io.InputStream;

import org.testng.annotations.Test;

import com.bigcloud.djomo.Json;
import com.bigcloud.djomo.error.ModelException;

public class BufferOverflowTest {
	@Test(expectedExceptions = ModelException.class)
	public void endlessStringTest() throws IOException {
		InputStream endlessStringStream = new InputStream() {
			boolean first = true;

			@Override
			public int read() throws IOException {
				if (first) {
					first = false;
					return '"';
				}
				return 'a';
			}

		};
		Json json = new Json();
		json.read(endlessStringStream);
	}
}
