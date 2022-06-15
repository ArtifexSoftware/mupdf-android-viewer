package com.artifex.mupdf.viewer;

import com.artifex.mupdf.fitz.SeekableInputStream;

import android.util.Log;
import android.content.ContentResolver;
import android.net.Uri;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ContentInputStream implements SeekableInputStream {
	private final String APP = "MuPDF";

	protected ContentResolver cr;
	protected Uri uri;
	protected InputStream is;
	protected long length, p;
	protected boolean mustReopenStream;

	public ContentInputStream(ContentResolver cr, Uri uri, long size) throws IOException {
		this.cr = cr;
		this.uri = uri;
		length = size;
		mustReopenStream = false;
		reopenStream();
	}

	public long seek(long offset, int whence) throws IOException {
		long newp = p;
		switch (whence) {
		case SEEK_SET:
			newp = offset;
			break;
		case SEEK_CUR:
			newp = p + offset;
			break;
		case SEEK_END:
			if (length < 0) {
				byte[] buf = new byte[16384];
				int k;
				while ((k = is.read(buf)) != -1)
					p += k;
				length = p;
			}
			newp = length + offset;
			break;
		}

		if (newp < p) {
			if (!mustReopenStream) {
				try {
					is.skip(newp - p);
				} catch (IOException x) {
					Log.i(APP, "Unable to skip backwards, reopening input stream");
					mustReopenStream = true;
				}
			}
			if (mustReopenStream) {
				reopenStream();
				is.skip(newp);
			}
		} else if (newp > p) {
			is.skip(newp - p);
		}
		return p = newp;
	}

	public long position() throws IOException {
		return p;
	}

	public int read(byte[] buf) throws IOException {
		int n = is.read(buf);
		if (n > 0)
			p += n;
		else if (n < 0 && length < 0)
			length = p;
		return n;
	}

	public void reopenStream() throws IOException {
		if (is != null)
		{
			is.close();
			is = null;
		}
		is = cr.openInputStream(uri);
		p = 0;
	}

}
