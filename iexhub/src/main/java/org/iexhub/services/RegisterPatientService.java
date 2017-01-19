/*******************************************************************************
 * Copyright (c) 2015, 2016 Substance Abuse and Mental Health Services Administration (SAMHSA)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Eversolve, LLC - initial IExHub implementation for Health Information Exchange (HIE) integration
 *     Anthony Sute, Ioana Singureanu
 *******************************************************************************/
package org.iexhub.services;

import PIXManager.org.hl7.v3.MCCIIN000002UV01;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.iexhub.config.IExHubConfig;
import org.iexhub.connectors.PIXManager;
import org.iexhub.exceptions.UnexpectedServerException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.File;

/**
 * @author A. Sute
 *
 */
@Deprecated
@Path("/RegisterPatient")
public class RegisterPatientService
{
	private static boolean TestMode = false;
	private static String PIXManagerEndpointURI = "http://129.6.24.79:9090";
	private final String testOutputPath;

	public RegisterPatientService() {
		this.testOutputPath = IExHubConfig.getProperty("TestOutputPath");
		assert StringUtils.isNotBlank(this.testOutputPath) : "'TestOutputPath' property must be configured";
	}

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
						enterpriseMRN);
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
				retVal = FileUtils.readFileToString(new File(this.testOutputPath + "/sampleJson.txt"));
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