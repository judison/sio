package org.judison.sio;

import java.io.ByteArrayOutputStream;

public class ByteArraySWriter extends SWriter {

	public ByteArraySWriter() {
		super(new ByteArrayOutputStream());
	}

	public byte[] toByteArray() {
		return ((ByteArrayOutputStream)getStream()).toByteArray();
	}

}
