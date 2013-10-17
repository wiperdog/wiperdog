package org.wiperdog.installer.internal;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * Handler for XML errors.
 * At this point, the handler simply prints stack traces.
 * @author nguyenvannghia
 *
 */
public class XMLErrorHandler implements ErrorHandler {

	/** Prints an error message in response to parser warnings.
	 */
	public void warning(SAXParseException e) {
		e.printStackTrace();
	}

	/** Prints an error message in response to parser errors.
	 */
	public void error(SAXParseException e) {
		e.printStackTrace();
	}

	/** Prints an error message in response to parser fatal errors.
	 */
	public void fatalError(SAXParseException e) {
		e.printStackTrace();
	}

}
