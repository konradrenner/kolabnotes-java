/**
 * 
 */
package org.kore.kolab.notes.v3;


/**
 * Will be thrown when a problem occurs by parsing or writing. To get the original exception, get the cause of this
 * exception
 * 
 * @author Konrad Renner
 * 
 */
public class KolabParseException
		extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public KolabParseException(Throwable cause) {
		super(cause);
	}
}
