/**
 * 
 */
package com.InfoExchangeHub.Connectors;

import static org.junit.Assert.*;

import org.junit.Test;

import PDQSupplier.src.org.hl7.v3.PRPAIN201306UV02;

/**
 * @author A. Sute
 *
 */
public class PDQQueryManagerTest
{
	private static String PDQManagerEndpointURI = "http://129.6.24.79:9090";

	/**
	 * Test method for {@link com.InfoExchangeHub.Connectors.PDQQueryManager#QueryPatientDemographics(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testITI47ConsumerQueryPatientAddress_NoOtherIDsScopingOrganization()
	{
		PDQQueryManager pdqQueryManager = null;
		try
		{
			pdqQueryManager = new PDQQueryManager(PDQManagerEndpointURI);
			PRPAIN201306UV02 pdqQueryResponse = pdqQueryManager.QueryPatientDemographics(null,
					null,
					null,
					null,
					null,
					null,
					"1905 Romrog Way",
					"ROCK SPRINGS",
					"WY",
					"82901",
					null,
					null,
					null);
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link com.InfoExchangeHub.Connectors.PDQQueryManager#QueryPatientDemographics(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testITI47ConsumerQueryPatientAddress_OtherIDsScopingOrganization()
	{
		PDQQueryManager pdqQueryManager = null;
		try
		{
			pdqQueryManager = new PDQQueryManager(PDQManagerEndpointURI);
			PRPAIN201306UV02 pdqQueryResponse = pdqQueryManager.QueryPatientDemographics(null,
					null,
					null,
					null,
					null,
					null,
					"1905 Romrog Way",
					"ROCK SPRINGS",
					"WY",
					"82901",
					null,
					null,
					"2.16.840.1.113883.3.72.5.9.1");
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link com.InfoExchangeHub.Connectors.PDQQueryManager#QueryPatientDemographics(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testITI47ConsumerQueryPatientNameAdministrativeSex_NoOtherIDsScopingOrganization()
	{
		PDQQueryManager pdqQueryManager = null;
		try
		{
			pdqQueryManager = new PDQQueryManager(PDQManagerEndpointURI);
			PRPAIN201306UV02 pdqQueryResponse = pdqQueryManager.QueryPatientDemographics(null,
					"GREGORYX",
					null,
					null,
					"F",
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null);
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link com.InfoExchangeHub.Connectors.PDQQueryManager#QueryPatientDemographics(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testITI47ConsumerQueryPatientNameAdministrativeSex_OtherIDsScopingOrganization()
	{
		PDQQueryManager pdqQueryManager = null;
		try
		{
			pdqQueryManager = new PDQQueryManager(PDQManagerEndpointURI);
			PRPAIN201306UV02 pdqQueryResponse = pdqQueryManager.QueryPatientDemographics(null,
					"GREGORYX",
					null,
					null,
					"F",
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					"2.16.840.1.113883.3.72.5.9.1");
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link com.InfoExchangeHub.Connectors.PDQQueryManager#QueryPatientDemographics(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testITI47ConsumerQueryPatientNameDOB_NoOtherIDsScopingOrganization()
	{
		PDQQueryManager pdqQueryManager = null;
		try
		{
			pdqQueryManager = new PDQQueryManager(PDQManagerEndpointURI);
			PRPAIN201306UV02 pdqQueryResponse = pdqQueryManager.QueryPatientDemographics(null,
					"GREGORYX",
					null,
					"10/15/1929",
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null);
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link com.InfoExchangeHub.Connectors.PDQQueryManager#QueryPatientDemographics(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testITI47ConsumerQueryPatientNameDOB_OtherIDsScopingOrganization()
	{
		PDQQueryManager pdqQueryManager = null;
		try
		{
			pdqQueryManager = new PDQQueryManager(PDQManagerEndpointURI);
			PRPAIN201306UV02 pdqQueryResponse = pdqQueryManager.QueryPatientDemographics(null,
					"GREGORYX",
					null,
					"10/15/1929",
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					"2.16.840.1.113883.3.72.5.9.1",
					"2.16.840.1.113883.3.72.5.9.1");
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link com.InfoExchangeHub.Connectors.PDQQueryManager#QueryPatientDemographics(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testITI47ConsumerQueryPatientPatientId_NoOtherIDsScopingOrganization()
	{
		PDQQueryManager pdqQueryManager = null;
		try
		{
			pdqQueryManager = new PDQQueryManager(PDQManagerEndpointURI);
			PRPAIN201306UV02 pdqQueryResponse = pdqQueryManager.QueryPatientDemographics(null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					"HJ-361",
					"2.16.840.1.113883.3.72.5.9.1",
					null);
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link com.InfoExchangeHub.Connectors.PDQQueryManager#QueryPatientDemographics(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testITI47ConsumerQueryPatientPatientId_OtherIDsScopingOrganization()
	{
		PDQQueryManager pdqQueryManager = null;
		try
		{
			pdqQueryManager = new PDQQueryManager(PDQManagerEndpointURI);
			PRPAIN201306UV02 pdqQueryResponse = pdqQueryManager.QueryPatientDemographics(null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					"HJ-361",
					"2.16.840.1.113883.3.72.5.9.1",
					"2.16.840.1.113883.3.72.5.9.1");
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link com.InfoExchangeHub.Connectors.PDQQueryManager#QueryPatientDemographics(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testITI47ConsumerQueryContinuationOption()
	{
		PDQQueryManager pdqQueryManager = null;
		try
		{
			// Per the NIST server's instructions for this test, send a valid query message to query about all patients named WILXLIS and ask for incremental
			//   response (limited to 1 record)... 
			pdqQueryManager = new PDQQueryManager(PDQManagerEndpointURI);
			PRPAIN201306UV02 pdqQueryResponse = pdqQueryManager.QueryPatientDemographics(null,
					"WILXLIS",
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					1);
			
			// Now send the PDQ query continuation message (QUQI_IN000003UV01), requesting one additional record...
			pdqQueryResponse = pdqQueryManager.QueryContinue(pdqQueryResponse,
					1);
			
			// Now send a PDQ query cancel message (QUQI_IN000003UV01)
			pdqQueryManager.QueryCancel(pdqQueryResponse);
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}

}
