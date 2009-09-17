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

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;

import org.judison.sio.annot.WriteAttr;

public class SWriter implements Closeable {

	private OutputStream stream;
	private byte[] buf = new byte[8];

	public SWriter(OutputStream stream) {
		this.stream = stream;
	}

	public void writeByte(byte data) throws IOException {
		stream.write(data);
	}

	public void writeByte(int data) throws IOException {
		stream.write(data);
	}

	public void writeShort(short data) throws IOException {
		buf[0] = (byte)((data >> 8) & 0xff);
		buf[1] = (byte)((data >> 0) & 0xff);
		stream.write(buf, 0, 2);
	}

	public void writeShort(int data) throws IOException {
		writeShort((short)data);
	}

	public void writeInt(int data) throws IOException {
		buf[0] = (byte)((data >> 24) & 0xff);
		buf[1] = (byte)((data >> 16) & 0xff);
		buf[2] = (byte)((data >> 8) & 0xff);
		buf[3] = (byte)((data >> 0) & 0xff);
		stream.write(buf, 0, 4);
	}

	public void writeLong(long data) throws IOException {
		buf[0] = (byte)((data >> 56) & 0xff);
		buf[1] = (byte)((data >> 48) & 0xff);
		buf[2] = (byte)((data >> 40) & 0xff);
		buf[3] = (byte)((data >> 32) & 0xff);
		buf[4] = (byte)((data >> 24) & 0xff);
		buf[5] = (byte)((data >> 16) & 0xff);
		buf[6] = (byte)((data >> 8) & 0xff);
		buf[7] = (byte)((data >> 0) & 0xff);
		stream.write(buf, 0, 8);
	}

	public void writeFloat(float data) throws IOException {
		writeInt(Float.floatToRawIntBits(data));
	}

	public void writeDouble(double data) throws IOException {
		writeLong(Double.doubleToRawLongBits(data));
	}

	public void writeBoolean(boolean data) throws IOException {
		stream.write(data ? 1 : 0);
	}

	public void writeChar(char data) throws IOException {
		buf[0] = (byte)((data >> 8) & 0xff);
		buf[1] = (byte)((data >> 0) & 0xff);
		stream.write(buf, 0, 2);
	}

	public void writeString(String data) throws IOException {
		if (data == null) {
			writeInt(-1);
			return;
		}
		writeInt(data.length());
		for (char ch: data.toCharArray()) {
			buf[0] = (byte)((ch >> 8) & 0xff);
			buf[1] = (byte)((ch >> 0) & 0xff);
			stream.write(buf, 0, 2);
		}
	}

	public void writeEnum(Enum<?> data) throws IOException {
		writeString(data.name());
	}

	public void writeByteArray(byte[] data) throws IOException {
		if (data == null)
			writeInt(-1);
		else if (data.length == 0)
			writeInt(0);
		else {
			writeInt(data.length);
			stream.write(data);
		}
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

	public void writeObject(SWriteable object) throws IOException {
		for (Method m: object.getClass().getMethods()) {
			if (m.isAnnotationPresent(WriteAttr.class)) {
				try {
					Class<?> ret = m.getReturnType();
					String name = m.getAnnotation(WriteAttr.class).value();
					if (ret == null || m.getParameterTypes().length != 0)
						throw new RuntimeException("Invalid method for @WriteAttr: " + m.getName());
					else if (ret.equals(Byte.TYPE) || ret.equals(Byte.class)) {
						Byte v = (Byte)m.invoke(object);
						if (v == null) {
							writeByte(ATTR_ID_NULL);
							writeString(name);
						} else {
							writeByte(ATTR_ID_BYTE);
							writeString(name);
							writeByte(v);
						}
					} else if (ret.equals(Short.TYPE) || ret.equals(Short.class)) {
						Short v = (Short)m.invoke(object);
						if (v == null) {
							writeByte(ATTR_ID_NULL);
							writeString(name);
						} else {
							writeByte(ATTR_ID_SHORT);
							writeString(name);
							writeShort(v);
						}
					} else if (ret.equals(Integer.TYPE) || ret.equals(Integer.class)) {
						Integer v = (Integer)m.invoke(object);
						if (v == null) {
							writeByte(ATTR_ID_NULL);
							writeString(name);
						} else {
							writeByte(ATTR_ID_INT);
							writeString(name);
							writeInt(v);
						}
					} else if (ret.equals(Long.TYPE) || ret.equals(Long.class)) {
						Long v = (Long)m.invoke(object);
						if (v == null) {
							writeByte(ATTR_ID_NULL);
							writeString(name);
						} else {
							writeByte(ATTR_ID_LONG);
							writeString(name);
							writeLong(v);
						}
					} else if (ret.equals(Float.TYPE) || ret.equals(Float.class)) {
						Float v = (Float)m.invoke(object);
						if (v == null) {
							writeByte(ATTR_ID_NULL);
							writeString(name);
						} else {
							writeByte(ATTR_ID_FLOAT);
							writeString(name);
							writeFloat(v);
						}
					} else if (ret.equals(Double.TYPE) || ret.equals(Double.class)) {
						Double v = (Double)m.invoke(object);
						if (v == null) {
							writeByte(ATTR_ID_NULL);
							writeString(name);
						} else {
							writeByte(ATTR_ID_DOUBLE);
							writeString(name);
							writeDouble(v);
						}
					} else if (ret.equals(Boolean.TYPE) || ret.equals(Boolean.class)) {
						Boolean v = (Boolean)m.invoke(object);
						if (v == null) {
							writeByte(ATTR_ID_NULL);
							writeString(name);
						} else {
							writeByte(ATTR_ID_BOOLEAN);
							writeString(name);
							writeBoolean(v);
						}
					} else if (ret.equals(Character.TYPE) || ret.equals(Character.class)) {
						Character v = (Character)m.invoke(object);
						if (v == null) {
							writeByte(ATTR_ID_NULL);
							writeString(name);
						} else {
							writeByte(ATTR_ID_CHAR);
							writeString(name);
							writeChar(v);
						}
					} else if (ret.equals(String.class)) {
						String v = (String)m.invoke(object);
						if (v == null) {
							writeByte(ATTR_ID_NULL);
							writeString(name);
						} else {
							writeByte(ATTR_ID_STRING);
							writeString(name);
							writeString(v);
						}
					} else if (ret.isEnum()) {
						Enum<?> v = (Enum<?>)m.invoke(object);
						if (v == null) {
							writeByte(ATTR_ID_NULL);
							writeString(name);
						} else {
							writeByte(ATTR_ID_ENUM);
							writeString(name);
							writeEnum(v);
						}
					} else
						throw new RuntimeException("Invalid return type for @WriteAttr in " + object.getClass().getName() + "." + m.getName());
				} catch (IOException e) {
					throw e;
				} catch (Throwable e) {
					throw new RuntimeException("Error writing " + m.getAnnotation(WriteAttr.class).value(), e);
				}
			}
		}
		// Custom
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		SWriter sub = new SWriter(os);
		object.customWrite(sub);
		if (os.size() > 0) {
			writeByte(ATTR_ID_CUSTOM);
			writeByteArray(os.toByteArray());
		}
		writeByte(ATTR_ID_END);
	}

	@Override
	public void close() throws IOException {
		stream.close();
	}
}
