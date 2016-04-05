package org.iexhub.services;

import java.io.File;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;
import org.iexhub.connectors.PIXManager;
import org.iexhub.exceptions.*;

import PIXManager.org.hl7.v3.MCCIIN000002UV01;

/**
 * @author A. Sute
 *
 */

@Path("/RegisterPatient")
public class RegisterPatientService
{
	private static boolean TestMode = false;
	private static String PIXManagerEndpointURI = "http://129.6.24.79:9090";
	
	@GET
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	public Response registerPatient(@Context HttpHeaders headers)
	{
		String retVal = "";
		GetPatientDataResponse patientDataResponse = new GetPatientDataResponse();

		if (!TestMode)
		{
			PIXManager pixManager = null;
			try
			{
				pixManager = new PIXManager(PIXManagerEndpointURI);
			}
			catch (Exception e)
			{
				throw new UnexpectedServerException("Error - " + e.getMessage());
			}
	
			try
			{
				MultivaluedMap<String, String> headerParams = headers.getRequestHeaders();
				String ssoAuth = headerParams.getFirst("ssoauth");
				
				// Extract patient name, date of birth, gender, and ID info.  Expected format from the client is:
				//   "EnterpriseMasterRecordNumber={0}&LastName={1}&FirstName={2}&MiddleName={3}&DateOfBirth={4}&PatientGender={5}"
				String[] splitEMRN = ssoAuth.split("&LastName=");
				String enterpriseMRN = (splitEMRN[0].split("=").length == 2) ? splitEMRN[0].split("=")[1] : null;
				
				String[] parts = splitEMRN[1].split("&");
				String lastName = (parts[0].length() > 0) ? parts[0] : null;
				String firstName = (parts[1].split("=").length == 2) ? parts[1].split("=")[1] : null;
				String middleName = (parts[2].split("=").length == 2) ? parts[2].split("=")[1] : null;
				String dateOfBirth = (parts[3].split("=").length == 2) ? parts[3].split("=")[1] : null;
				String gender = (parts[4].split("=").length == 2) ? parts[4].split("=")[1] : null;

				// PIX patient registration...
				MCCIIN000002UV01 pixRegistrationResponse = pixManager.registerPatient(firstName,
						lastName,
						middleName,
						dateOfBirth,
						gender,
						enterpriseMRN,
						null);
			}
			catch (Exception e)
			{
				throw new UnexpectedServerException("Error - " + e.getMessage());
			}
		}
		else
		{
			// Return canned document for sprint #16 demo.  Sprint #17 will return JSON created by MDMI map (code below outside of this block).
			try
			{
				retVal = FileUtils.readFileToString(new File("test/sampleJson.txt"));
				return Response.status(Response.Status.OK).entity(retVal).type(MediaType.APPLICATION_XML).build();
			}
			catch (Exception e)
			{
				throw new UnexpectedServerException("Error - " + e.getMessage());
			}
		}
		
		return Response.status(Response.Status.OK).entity(patientDataResponse).type(MediaType.APPLICATION_XML).build();
	}
}