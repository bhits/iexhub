/*******************************************************************************
 * Copyright (c) 2016 Substance Abuse and Mental Health Services Administration (SAMHSA)
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
 *     Eversolve, LLC - initial IExHub implementation
 *******************************************************************************/
/**
 * 
 */
package org.iexhub.connectors;

import static org.junit.Assert.*;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.*;

import PIXManager.org.hl7.v3.MCCIIN000002UV01;
import PIXManager.org.hl7.v3.PRPAIN201310UV02;

/**
 * @author A. Sute
 *
 */
public class PIXManagerTest
{
	private static final String PIXManagerEndpointURI = "http://129.6.24.79:9090";

	/**
	 * Test method for {@link org.iexhub.connectors.PIXManager#patientRegistryGetIdentifiers(java.lang.String, java.lang.String, boolean)}.
	 */
	@Test
	public void testPatientRegistryGetIdentifiersWithDataSource()
	{
		// ITI-45-Consumer message
		PIXManager pixManager = null;
		try
		{
			pixManager = new PIXManager(PIXManagerEndpointURI);
			
			PRPAIN201310UV02 pixResponse = pixManager.patientRegistryGetIdentifiers("PIXL1",
					"2.16.840.1.113883.3.72.5.9.1",
					true);
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link org.iexhub.connectors.PIXManager#patientRegistryGetIdentifiers(java.lang.String, java.lang.String, boolean)}.
	 */
	@Test
	public void testPatientRegistryGetIdentifiersWithDataSourceTLS()
	{
		// ITI-45-Consumer message
		PIXManager pixManager = null;
		try
		{
			pixManager = new PIXManager(/*PIXManagerEndpointURI*/ null,
					true);
			
			PRPAIN201310UV02 pixResponse = pixManager.patientRegistryGetIdentifiers("IHEFACILITY-998",
					"1.3.6.1.4.1.21367.3000.1.6",
					true);
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link org.iexhub.connectors.PIXManager#patientRegistryGetIdentifiers(java.lang.String, java.lang.String, boolean)}.
	 */
	@Test
	public void testPatientRegistryGetIdentifiersNoDataSource()
	{
		// ITI-45-Consumer message
		PIXManager pixManager = null;
		try
		{
			pixManager = new PIXManager(PIXManagerEndpointURI);
			
			PRPAIN201310UV02 pixResponse = pixManager.patientRegistryGetIdentifiers("PIXL1",
					"2.16.840.1.113883.3.72.5.9.1",
					false);
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link org.iexhub.connectors.PIXManager#patientRegistryGetIdentifiers(java.lang.String, java.lang.String, boolean)}.
	 */
	@Test
	public void testPatientRegistryGetIdentifiersNoDataSourceTLS()
	{
		// ITI-45-Consumer message
		PIXManager pixManager = null;
		try
		{
			pixManager = new PIXManager(/*PIXManagerEndpointURI*/ null,
					true);
			
			PRPAIN201310UV02 pixResponse = pixManager.patientRegistryGetIdentifiers("1455985023678",
					"1.3.6.1.4.1.21367.13.20.200",
					false);
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link org.iexhub.connectors.PIXManager#registerPatient(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testRegisterPatient()
	{
		// ITI-44-Source-Feed message
		PIXManager pixManager = null;
		try
		{
			pixManager = new PIXManager(PIXManagerEndpointURI);
			
			MCCIIN000002UV01 pixRegistrationResponse = pixManager.registerPatient("ALAN",
					"ALPHA",
					null,
					"12/8/1978",
					"M",
					"PIX");
	
//			assertTrue("Error - unexpected return value for RegisterPatient message",
//					pixRegistrationResponse.toString());
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}
	
	/**
	 * Test method for {@link org.iexhub.connectors.PIXManager#registerPatient(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testRegisterPatientTLS()
	{
		// ITI-44-Source-Feed message
		PIXManager pixManager = null;
		try
		{
			pixManager = new PIXManager(/*PIXManagerEndpointURI*/ null,
					true);
			
			DateTime oidTimeValue = DateTime.now(DateTimeZone.UTC);
			MCCIIN000002UV01 pixRegistrationResponse = pixManager.registerPatient("SALLY",
					"SHARE",
					null,
					"5/1/1977",
					"F",
					String.valueOf(oidTimeValue.getMillis()));

//			MCCIIN000002UV01 pixRegistrationResponse = pixManager.registerPatient("ALICE",
//					"MAIDEN",
//					null,
//					"5/5/1955",
//					"M",
//					String.valueOf(oidTimeValue.getMillis()));

//			MCCIIN000002UV01 pixRegistrationResponse = pixManager.registerPatient("ALYSSA",
//					"EVERSOLVE",
//					null,
//					"5/5/1955",
//					"M",
//					String.valueOf(oidTimeValue.getMillis()));

//			MCCIIN000002UV01 pixRegistrationResponse = pixManager.registerPatient("ALICE",
//					"EVERSOLVE",
//					null,
//					"5/5/1955",
//					"M",
//					"1453993451564");

//			assertTrue("Error - unexpected return value for RegisterPatient message",
//					pixRegistrationResponse.toString());
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}
}
