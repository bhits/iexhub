/**
 * 
 */
package org.iexhub.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author A. Sute
 *
 */
public class InvalidSourceModelException extends WebApplicationException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InvalidSourceModelException(String message)
	{
		super(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(message).type(MediaType.APPLICATION_XML).build());
    }
}
