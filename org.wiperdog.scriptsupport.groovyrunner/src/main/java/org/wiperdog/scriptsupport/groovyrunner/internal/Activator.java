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
package org.wiperdog.scriptsupport.groovyrunner.internal;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.tools.LoaderConfiguration;
import org.codehaus.groovy.tools.RootLoader;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
	Logger logger = Logger.getLogger(Activator.class);
	public static final String PROPKEY_LIBPATH = "groovyrunner.scriptlibs";
	public static final String PROPKEY_BOOTSCRIPT = "groovyrunner.bootscript";
	public static final String DEF_BOOTSCRIPT = "etc/boot.groovy";
	public static final String PROPKEY_HOME = "felix.home";
	public static final String DEF_LIBPATH = "lib/groovy/libs.common";
	
	private void bootGroovy(BundleContext context, File bootFile) {
		String libpath = System.getProperty(PROPKEY_LIBPATH);
		if (libpath == null) {
			libpath = System.getProperty(PROPKEY_HOME) + "/" + DEF_LIBPATH;
		}
		URL [] scriptpath = new URL[0];
		try {
			scriptpath = new URL [] { new File(libpath).toURL() };
		} catch (MalformedURLException e1) {
			logger.debug("", e1);
		}
		RootLoader loader = new RootLoader(scriptpath, this.getClass().getClassLoader());
		Binding binding = new Binding();
		binding.setProperty( "ctx", context );
		binding.setProperty( "logger", logger );
		GroovyShell shell = new GroovyShell(loader, binding);
		try {
//			Class<?> clsGroovyShell = loader.loadClass("groovy.lang.GroovyShell");
//			GroovyShell shell = (GroovyShell) clsGroovyShell.newInstance();
			shell.getContext().setProperty("ctx", context);
			shell.getContext().setProperty("logger", logger);
			shell.run(bootFile, new String [] {""} );
		} catch (CompilationFailedException e) {
			logger.info("boot script error", e);
		} catch (IOException e) {
			logger.info("boot script error", e);
		} catch (Throwable t) {
			logger.info("boot script error", t);
		}
	}
	
	public void start(BundleContext context) throws Exception {
		String bootFile = System.getProperty(PROPKEY_BOOTSCRIPT);
		if (bootFile == null) {
			bootFile = System.getProperty("felix.home") + "/" + DEF_BOOTSCRIPT;
		}
		File fBoot = new File(bootFile);
		if (fBoot.isFile()) {
			bootGroovy(context, fBoot);
		} else {
			logger.info("no groovy boot script exist(" + bootFile + ")");
		}
	}

	public void stop(BundleContext context) throws Exception {
	}

}
