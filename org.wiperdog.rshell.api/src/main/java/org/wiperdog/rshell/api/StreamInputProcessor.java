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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.Charset;

public class StreamInputProcessor implements Runnable {
	private static final int BUFSIZE = 1024;
	private final InputStreamReader reader;
	private Thread me;
	private boolean bRun = false;
	private VolatileDataWriter writer;
	
	public StreamInputProcessor(InputStream is, File tmpfile, int maxsize) {
		
		reader = new InputStreamReader(is);
		writer = new VolatileDataWriter(tmpfile, maxsize);
	}
	
	public StreamInputProcessor(InputStream is, File tmpfile, int maxsize, Charset cs) {
		reader = new InputStreamReader(is, cs);
		writer = new VolatileDataWriter(tmpfile, maxsize);
	}
	
	private void setThread(Thread t) {
		this.me = t;
	}
	
	public void run() {
		bRun = true;
		char [] cbuf = new char[BUFSIZE];
		while (bRun) {
			try {
				int sizeread = reader.read(cbuf);
				if (sizeread > 0) {
					writer.write(cbuf, 0, sizeread);
				} else {
					break;
				}
			} catch (IOException e) {
			}
		}
	}

	public void stop() {
		bRun = false;
		me.interrupt();
		try {
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void clear() {
		writer.clear();
	}

	public void writeTo(Writer to) {
		writer.WriteTo(to);
	}
	
	public static StreamInputProcessor start(InputStream is, String tmpfilepath, int maxsize) {
		File tmpfile = null;
		if (tmpfilepath != null && tmpfilepath.length() > 0) {
			tmpfile = new File(tmpfilepath);
		}
		StreamInputProcessor processor = new StreamInputProcessor(is, tmpfile, maxsize);
		Thread t = new Thread(processor);
		processor.setThread(t);
		t.start();
		
		return processor;
	}
}
