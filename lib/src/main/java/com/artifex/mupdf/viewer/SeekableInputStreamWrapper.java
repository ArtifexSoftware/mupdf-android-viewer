package com.artifex.mupdf.viewer;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;

import com.artifex.mupdf.fitz.SeekableInputStream;

public class SeekableInputStreamWrapper implements SeekableInputStream
{
	// Max file size supported if the size is unknown
	protected final int MAX_SIZE = (128<<20);

	protected InputStream is;
	protected long p, length;

	public SeekableInputStreamWrapper(InputStream raw, long n) throws IOException {
		int size = n < 0 ? MAX_SIZE : (int)n + 1;
		if (!raw.markSupported()) {
			is = new BufferedInputStream(raw, size);
		} else {
			is = raw;
		}
		is.mark(size);
		length = n;
		p = 0;
	}

	public long seek(long offset, int whence) throws IOException {
		switch (whence) {
		case SEEK_SET:
			p = offset;
			break;
		case SEEK_CUR:
			p = p + offset;
			break;
		case SEEK_END:
			if (length < 0) {
				is.reset();
				length = is.skip(MAX_SIZE);
				if (length < 0)
					throw new IOException("could not find length of stream");
			}
			p = length + offset;
			break;
		}
		is.reset();
		is.skip(p);
		return p;
	}

	public long position() throws IOException {
		return p;
	}

	public int read(byte[] buf) throws IOException {
		int n = is.read(buf);
		if (n > 0)
			p += n;
		return n;
	}
}
