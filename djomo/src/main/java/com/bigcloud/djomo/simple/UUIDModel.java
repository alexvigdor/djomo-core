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

import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.UUID;

import com.bigcloud.djomo.api.ModelContext;
import com.bigcloud.djomo.api.Printer;
import com.bigcloud.djomo.base.BaseSimpleModel;
import com.bigcloud.djomo.io.Buffer;

public class UUIDModel extends BaseSimpleModel<UUID> {
	private static final char[] hexchars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	public UUIDModel(Type type, ModelContext context) {
		super(type, context);
	}

	@Override
	public void print(UUID obj, Printer printer) {
		char[] ucs = new char[38];
		long val = obj.getMostSignificantBits();
		var hc = hexchars;
		for (int i = 0; i < 38; i++) {
			switch (i) {
			case 0:
			case 37:
				ucs[i] = '"';
				break;
			case 19:
				val = obj.getLeastSignificantBits();
			case 9:
			case 14:
			case 24:
				ucs[i] = '-';
				break;
			default:
				int b = (int) (val >>> 60);
				ucs[i] = hc[b];
				val = val << 4;
			}
		}
		ucs[0] = '"';
		ucs[37] = '"';
		printer.raw(ucs, 0, 38);
	}

	@Override
	public UUID parse(Buffer input, Buffer overflow) throws IOException {
		int rp = input.readPosition;
		char[] rb = input.buffer;
		int len = input.writePosition - rp;
		if (len > 36) {
			len = 36;
		}
		long msb = 0;
		long lsb = 0;
		int upos = 0;
		do {
			for (int i = 0; i < len; i++) {
				char ch = rb[rp + i];
				switch (upos++) {
				case 18:
					msb = lsb;
					lsb = 0;
				case 8:
				case 13:
				case 23:
					if (ch != '-') {
						throw new IllegalArgumentException("Illegal UUID format " + input.describe());
					}
					break;
				default:
					lsb = lsb << 4;
					switch (ch) {
					case '0':
						break;
					case '1':
						lsb += 1;
						break;
					case '2':
						lsb += 2;
						break;
					case '3':
						lsb += 3;
						break;
					case '4':
						lsb += 4;
						break;
					case '5':
						lsb += 5;
						break;
					case '6':
						lsb += 6;
						break;
					case '7':
						lsb += 7;
						break;
					case '8':
						lsb += 8;
						break;
					case '9':
						lsb += 9;
						break;
					case 'A':
					case 'a':
						lsb += 10;
						break;
					case 'B':
					case 'b':
						lsb += 11;
						break;
					case 'C':
					case 'c':
						lsb += 12;
						break;
					case 'D':
					case 'd':
						lsb += 13;
						break;
					case 'E':
					case 'e':
						lsb += 14;
						break;
					case 'F':
					case 'f':
						lsb += 15;
						break;
					default:
						throw new IllegalArgumentException("Illegal hex char " + ch);
					}
				}
			}
			input.readPosition = rp + len;
			if(upos == 36) {
				break;
			}
			if (!input.refill()) {
				throw new EOFException();
			}
			rp = input.readPosition;
			len = input.writePosition - rp;
			if (len > (36 - upos)) {
				len = 36 - upos;
			}
		} while (true);
		return new UUID(msb, lsb);
	}

	@Override
	public UUID convert(Object o) {
		if (o instanceof UUID) {
			return (UUID) o;
		}
		if (o == null) {
			return null;
		}
		return UUID.fromString(o.toString());
	}

}
