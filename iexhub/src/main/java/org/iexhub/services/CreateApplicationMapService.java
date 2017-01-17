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
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.iexhub.config.IExHubConfig;
import org.iexhub.exceptions.MapFileCreationException;
import org.iexhub.exceptions.URLToMapFileCopyException;
import org.iexhub.exceptions.UnexpectedServerException;

/**
 * @author A. Sute
 *
 */
@Deprecated
@Path("/CreateApplicationMap")
public class CreateApplicationMapService
{
	private final String testOutputPath;

	public CreateApplicationMapService(){
		this.testOutputPath =  IExHubConfig.getProperty("TestOutputPath");
		assert StringUtils.isNotBlank(this.testOutputPath) : "'TestOutputPath' property must be configured";
	}

	@POST
	@Produces("application/xml")
	public Response createApplicationMap(@QueryParam("applicationId") String applicationId,
			@QueryParam("referentIndex") String referentIndex,
			@QueryParam("mappingFile") String mappingFile)
	{
		try
		{
			File destinationRootDir = new File(testOutputPath);
			
			// mappingFile may be either the XML mapping itself or a URI pointing to a file containing the XML mapping.  To test
			//   for the URI, try to construct a URI object...
			try
			{
				URL test = new URL(mappingFile);
				
				// We have a URI - copy the file...
				try
				{
					FileUtils.copyURLToFile(test, new File(destinationRootDir, applicationId + "Map.xmi"));
				}
				catch (IOException ex)
				{
					throw new URLToMapFileCopyException("Error encountered while copying URL to map file, info="
							+ ex.getMessage());
				}
			}
			catch (MalformedURLException ex)
			{
				// Mapping is in the mappingFile parameter - serialize it to a file...
				File destinationFile = new File(destinationRootDir, applicationId + "Map.xmi");
				
				try
				{
					Files.write(Paths.get(destinationFile.getAbsolutePath()), mappingFile.getBytes());
				}
				catch (IOException ex2)
				{
					throw new MapFileCreationException("Error encountered while creating map file, info="
							+ ex.getMessage());
				}
			}
		}
		catch (URLToMapFileCopyException |
				MapFileCreationException ex)
		{
			throw ex;
		}
		catch (Exception ex)
		{
			throw new UnexpectedServerException("Error - " + ex.getMessage());
		}
		
		return Response.status(Response.Status.OK).build();
	}
}
