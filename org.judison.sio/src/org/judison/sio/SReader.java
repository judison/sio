/*
 * Copyright (c) 2009, Judison Oliveira Gil Filho <judison@gmail.com>
 * 
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package org.judison.sio;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import org.judison.sio.annot.ReadAttr;

public class SReader implements Closeable {

	private InputStream stream;
	private byte[] buf = new byte[8];

	public SReader(InputStream stream) {
		this.stream = stream;
	}

	public SReader(byte[] data) {
		this.stream = new ByteArrayInputStream(data);
	}

	public byte readByte() throws IOException {
		if (stream.read(buf, 0, 1) != 1)
			throw new EOFException();
		return buf[0];
	}

	public short readShort() throws IOException {
		if (stream.read(buf, 0, 2) != 2)
			throw new EOFException();
		return (short)(//
		(0xff & buf[0]) << 8 | //
		(0xff & buf[1]) << 0);
	}

	public int readInt() throws IOException {
		if (stream.read(buf, 0, 4) != 4)
			throw new EOFException();
		return (//
		/*    */(0xff & buf[0]) << 24 | //
				(0xff & buf[1]) << 16 | //
				(0xff & buf[2]) << 8 | //
		/*    */(0xff & buf[3]) << 0);
	}

	public long readLong() throws IOException {
		if (stream.read(buf, 0, 8) != 8)
			throw new EOFException();
		return (//
		/*    */(long)(0xff & buf[0]) << 56 | //
				(long)(0xff & buf[1]) << 48 | //
				(long)(0xff & buf[2]) << 40 | //
				(long)(0xff & buf[3]) << 32 | //
				(long)(0xff & buf[4]) << 24 | //
				(long)(0xff & buf[5]) << 16 | //
				(long)(0xff & buf[6]) << 8 | //
		/*    */(long)(0xff & buf[7]) << 0);
	}

	public float readFloat() throws IOException {
		return Float.intBitsToFloat(readInt());
	}

	public double readDouble() throws IOException {
		return Double.longBitsToDouble(readLong());
	}

	public boolean readBoolean() throws IOException {
		return readByte() == 1;
	}

	public char readChar() throws IOException {
		if (stream.read(buf, 0, 2) != 2)
			throw new EOFException();
		return (char)(//
		(0xff & buf[0]) << 8 | //
		(0xff & buf[1]) << 0);
	}

	public String readString() throws IOException {
		int len = readInt();
		if (len == -1)
			return null;
		if (len == 0)
			return "";
		char[] str = new char[len];
		for (int i = 0; i < len; i++) {
			if (stream.read(buf, 0, 2) != 2)
				throw new EOFException();
			str[i] = (char)((0xff & buf[0]) << 8 | (0xff & buf[1]) << 0);
		}
		return new String(str);
	}

	public <T extends Enum<T>> T readEnum(Class<T> enumClass) throws IOException {
		return Enum.valueOf(enumClass, readString());
	}

	public byte[] readByteArray() throws IOException {
		int size = readInt();
		if (size == -1)
			return null;
		if (size == 0)
			return new byte[0];
		byte[] data = new byte[size];
		stream.read(data, 0, size);
		return data;
	}

	private static final int ATTR_ID_END = 0;
	private static final int ATTR_ID_NULL = 1;
	private static final int ATTR_ID_BYTE = 2;
	private static final int ATTR_ID_SHORT = 3;
	private static final int ATTR_ID_INT = 4;
	private static final int ATTR_ID_LONG = 5;
	private static final int ATTR_ID_FLOAT = 6;
	private static final int ATTR_ID_DOUBLE = 7;
	private static final int ATTR_ID_BOOLEAN = 8;
	private static final int ATTR_ID_CHAR = 9;
	private static final int ATTR_ID_STRING = 10;
	private static final int ATTR_ID_ENUM = 11;
	//...
	private static final int ATTR_ID_CUSTOM = 50;

	@SuppressWarnings("unchecked")
	public void readObject(SReadable object) throws IOException {
		byte id = readByte();
		while (id != ATTR_ID_END) {
			if (id < ATTR_ID_CUSTOM) {
				String name = readString();
				for (Method m: object.getClass().getMethods()) {
					if (m.isAnnotationPresent(ReadAttr.class) && m.getAnnotation(ReadAttr.class).value().equals(name)) {
						try {
							Class<?>[] types = m.getParameterTypes();
							if (types.length != 1)
								throw new RuntimeException("Invalid method for @ReadAttr: " + object.getClass().getName() + "." + m.getName());
							Class<?> type = types[0];
							switch (id) {
								case ATTR_ID_NULL:
									m.invoke(object, new Object[] { null });
									break;
								case ATTR_ID_BYTE:
									if (!type.equals(Byte.class) && !type.equals(Byte.TYPE))
										throw new RuntimeException("Invalid type to call @ReadAttr: " + object.getClass().getName() + "." + m.getName());
									m.invoke(object, readByte());
									break;
								case ATTR_ID_SHORT:
									if (!type.equals(Short.class) && !type.equals(Short.TYPE))
										throw new RuntimeException("Invalid type to call @ReadAttr: " + object.getClass().getName() + "." + m.getName());
									m.invoke(object, readShort());
									break;
								case ATTR_ID_INT:
									if (!type.equals(Integer.class) && !type.equals(Integer.TYPE))
										throw new RuntimeException("Invalid type to call @ReadAttr: " + object.getClass().getName() + "." + m.getName());
									m.invoke(object, readInt());
									break;
								case ATTR_ID_LONG:
									if (!type.equals(Long.class) && !type.equals(Long.TYPE))
										throw new RuntimeException("Invalid type to call @ReadAttr: " + object.getClass().getName() + "." + m.getName());
									m.invoke(object, readLong());
									break;
								case ATTR_ID_FLOAT:
									if (!type.equals(Float.class) && !type.equals(Float.TYPE))
										throw new RuntimeException("Invalid type to call @ReadAttr: " + object.getClass().getName() + "." + m.getName());
									m.invoke(object, readFloat());
									break;
								case ATTR_ID_DOUBLE:
									if (!type.equals(Double.class) && !type.equals(Double.TYPE))
										throw new RuntimeException("Invalid type to call @ReadAttr: " + object.getClass().getName() + "." + m.getName());
									m.invoke(object, readDouble());
									break;
								case ATTR_ID_BOOLEAN:
									if (!type.equals(Boolean.class) && !type.equals(Boolean.TYPE))
										throw new RuntimeException("Invalid type to call @ReadAttr: " + object.getClass().getName() + "." + m.getName());
									m.invoke(object, readBoolean());
									break;
								case ATTR_ID_CHAR:
									if (!type.equals(Character.class) && !type.equals(Character.TYPE))
										throw new RuntimeException("Invalid type to call @ReadAttr: " + object.getClass().getName() + "." + m.getName());
									m.invoke(object, readChar());
									break;
								case ATTR_ID_STRING:
									if (!type.equals(String.class))
										throw new RuntimeException("Invalid type to call @ReadAttr: " + object.getClass().getName() + "." + m.getName());
									m.invoke(object, readString());
									break;
								case ATTR_ID_ENUM:
									if (!type.isEnum())
										throw new RuntimeException("Invalid type to call @ReadAttr: " + object.getClass().getName() + "." + m.getName());
									m.invoke(object, readEnum((Class<? extends Enum>)type));
									break;
							}
						} catch (IOException e) {
							throw e;
						} catch (Throwable e) {
							throw new RuntimeException("Error reading " + m.getAnnotation(ReadAttr.class).value(), e);
						}
					}
				}
			} else if (id == ATTR_ID_CUSTOM) {
				byte[] data = readByteArray();
				ByteArrayInputStream is = new ByteArrayInputStream(data);
				SReader sub = new SReader(is);
				object.customRead(sub);
			}
			id = readByte();
		}
	}

	@Override
	public void close() throws IOException {
		stream.close();
	}
}
