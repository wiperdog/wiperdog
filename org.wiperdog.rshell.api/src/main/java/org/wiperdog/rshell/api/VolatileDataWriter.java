/*
 *  Copyright 2013 Insight technology,inc. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wiperdog.rshell.api;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class VolatileDataWriter extends Writer {
	private static final int BUFFSIZE = 4096;
	private File tmpfile;
	private Writer delegate;
	private int maxsize;
	private int currentsize = 0;
	
	public VolatileDataWriter(File tmpfile, int maxsize) {
		this.tmpfile = tmpfile;
		this.maxsize = maxsize;
		if (tmpfile == null) {
			delegate = new CharArrayWriter();
		} else {
			try {
				delegate = new FileWriter(tmpfile);
			} catch (IOException e) {
			}
		}
	}
	
	@Override
	public void close() throws IOException {
		if (delegate == null) {
			throw new IOException("no delegating writer exist");
		}
		if (tmpfile != null) {
			((FileWriter)delegate).close();
		}
	}

	@Override
	public void flush() throws IOException {
		if (delegate == null) {
			throw new IOException("no delegating writer exist");
		}
		if (tmpfile != null) {
			((FileWriter)delegate).flush();
		}
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		int lentowrite = len;
		if (currentsize < maxsize) {
			lentowrite = (maxsize - currentsize > len ? len : maxsize - currentsize);
		} else {
			return;
		}
		delegate.write(cbuf, off, lentowrite);
		currentsize += lentowrite;
	}

	public void clear() {
		if (tmpfile != null) {
			try {
				((FileWriter)delegate).close();
				tmpfile.delete();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			((CharArrayWriter)delegate).close();
		}
	}
	
	private void writeFileTo(Writer to) {
		FileReader reader = null;
		try {
			reader = new FileReader(tmpfile);
			char [] buff = new char[BUFFSIZE];
			int rsize;
			while ((rsize = reader.read(buff)) > 0) {
				to.write(buff, 0, rsize);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (reader != null) {
			try {
				reader.close();
			} catch (IOException e) {
			}
		}
	}
	
	public void WriteTo(Writer to) {
		if (tmpfile != null) {
			writeFileTo(to);
		} else {
			if (delegate instanceof CharArrayWriter) {
				try {
					((CharArrayWriter)delegate).writeTo(to);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
