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
package com.bigcloud.djomo.simple;

import java.io.IOException;
import java.util.Base64;

import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Printer;
import com.bigcloud.djomo.base.BaseSimpleModel;
import com.bigcloud.djomo.io.Buffer;

public class ByteArrayModel extends BaseSimpleModel<byte[]> {

	public ByteArrayModel(ModelContext context) {
		super(byte[].class, context);
	}

	@Override
	public void print(byte[] obj, Printer printer) {
		byte[] bytes = Base64.getEncoder().encode(obj);
		var len = bytes.length;
		char[] chars = new char[len+2];
		chars[0]='"';
		for(int i=0; i < len; i++) {
			chars[i+1] = (char) bytes[i];
		}
		chars[len+1]='"';
		printer.raw(chars, 0, len+2);
	}

	@Override
	public byte[] parse(Buffer input, Buffer overflow) throws IOException {
		return Base64.getDecoder().decode(parseString(input, overflow));
	}

	@Override
	public byte[] convert(Object o) {
		if (o == null) {
			return null;
		}
		if (o.getClass() == getType()) {
			return (byte[]) o;
		}
		return Base64.getDecoder().decode(o.toString());
	}

}
