/**
 * 
 */
package com.InfoExchangeHub.Connectors;

import static org.junit.Assert.*;

import java.util.UUID;

import org.junit.Test;

import PDQSupplier.src.org.hl7.v3.PRPAIN201306UV02;

/**
 * @author A. Sute
 *
 */
public class PDQQueryManagerTest
{
	private static final String PDQManagerEndpointURI = null;
	private static final String PDQManagerTLSEndpointURI = null;

	/**
	 * Test method for {@link com.InfoExchangeHub.Connectors.PDQQueryManager#queryPatientDemographics(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testITI47ConsumerQueryPatientAddress_NoOtherIDsScopingOrganization()
	{
		PDQQueryManager pdqQueryManager = null;
		try
		{
			pdqQueryManager = new PDQQueryManager(PDQManagerEndpointURI);
			PRPAIN201306UV02 pdqQueryResponse = pdqQueryManager.queryPatientDemographics(null,
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
	 * Test method for {@link com.InfoExchangeHub.Connectors.PDQQueryManager#queryPatientDemographics(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testITI47ConsumerQueryPatientNameAddress_NoOtherIDsScopingOrganization_TLS()
	{
		PDQQueryManager pdqQueryManager = null;
		try
		{
			pdqQueryManager = new PDQQueryManager(null,
					true);
			PRPAIN201306UV02 pdqQueryResponse = pdqQueryManager.queryPatientDemographics(null,
					"MOORE",
					null,
					null,
					null,
					null,
					"10 PINETREE",
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
	 * Test method for {@link com.InfoExchangeHub.Connectors.PDQQueryManager#queryPatientDemographics(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testITI47ConsumerQueryPatientAddress_OtherIDsScopingOrganization()
	{
		PDQQueryManager pdqQueryManager = null;
		try
		{
			pdqQueryManager = new PDQQueryManager(PDQManagerEndpointURI);
			PRPAIN201306UV02 pdqQueryResponse = pdqQueryManager.queryPatientDemographics(null,
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
	 * Test method for {@link com.InfoExchangeHub.Connectors.PDQQueryManager#queryPatientDemographics(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testITI47ConsumerQueryPatientNameAdministrativeSex_NoOtherIDsScopingOrganization()
	{
		PDQQueryManager pdqQueryManager = null;
		try
		{
			pdqQueryManager = new PDQQueryManager(/*PDQManagerEndpointURI*/ null);
			PRPAIN201306UV02 pdqQueryResponse = pdqQueryManager.queryPatientDemographics("CHIP",
					"MOORE",
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
					null);
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link com.InfoExchangeHub.Connectors.PDQQueryManager#queryPatientDemographics(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testITI47ConsumerQueryPatientNameAdministrativeSex_NoOtherIDsScopingOrganization_TLS()
	{
		PDQQueryManager pdqQueryManager = null;
		try
		{
			pdqQueryManager = new PDQQueryManager(null,
					true);
			PRPAIN201306UV02 pdqQueryResponse = pdqQueryManager.queryPatientDemographics("CHIP",
					"MOORE",
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
					null);
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link com.InfoExchangeHub.Connectors.PDQQueryManager#queryPatientDemographics(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testITI47ConsumerQueryPatientNameAdministrativeSex_OtherIDsScopingOrganization()
	{
		PDQQueryManager pdqQueryManager = null;
		try
		{
			pdqQueryManager = new PDQQueryManager(PDQManagerEndpointURI);
			PRPAIN201306UV02 pdqQueryResponse = pdqQueryManager.queryPatientDemographics(null,
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
	 * Test method for {@link com.InfoExchangeHub.Connectors.PDQQueryManager#queryPatientDemographics(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testITI47ConsumerQueryPatientNameDOB_NoOtherIDsScopingOrganization()
	{
		PDQQueryManager pdqQueryManager = null;
		try
		{
			pdqQueryManager = new PDQQueryManager(PDQManagerEndpointURI);
			PRPAIN201306UV02 pdqQueryResponse = pdqQueryManager.queryPatientDemographics(null,
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
	 * Test method for {@link com.InfoExchangeHub.Connectors.PDQQueryManager#queryPatientDemographics(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testITI47ConsumerQueryPatientNameDOB_NoOtherIDsScopingOrganization_TLS()
	{
		PDQQueryManager pdqQueryManager = null;
		try
		{
			pdqQueryManager = new PDQQueryManager(null,
					true);
			PRPAIN201306UV02 pdqQueryResponse = pdqQueryManager.queryPatientDemographics(null,
					"MOORE",
					null,
					"7/6/1951",
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
	 * Test method for {@link com.InfoExchangeHub.Connectors.PDQQueryManager#queryPatientDemographics(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testITI47ConsumerQueryPatientNameDOB_OtherIDsScopingOrganization()
	{
		PDQQueryManager pdqQueryManager = null;
		try
		{
			pdqQueryManager = new PDQQueryManager(PDQManagerEndpointURI);
			PRPAIN201306UV02 pdqQueryResponse = pdqQueryManager.queryPatientDemographics(null,
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
	 * Test method for {@link com.InfoExchangeHub.Connectors.PDQQueryManager#queryPatientDemographics(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testITI47ConsumerQueryPatientPatientId_NoOtherIDsScopingOrganization()
	{
		PDQQueryManager pdqQueryManager = null;
		try
		{
			pdqQueryManager = new PDQQueryManager(PDQManagerEndpointURI);
			PRPAIN201306UV02 pdqQueryResponse = pdqQueryManager.queryPatientDemographics(null,
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
	 * Test method for {@link com.InfoExchangeHub.Connectors.PDQQueryManager#queryPatientDemographics(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testITI47ConsumerQueryPatientPatientId_OtherIDsScopingOrganization()
	{
		PDQQueryManager pdqQueryManager = null;
		try
		{
			pdqQueryManager = new PDQQueryManager(PDQManagerEndpointURI);
			PRPAIN201306UV02 pdqQueryResponse = pdqQueryManager.queryPatientDemographics(null,
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
	 * Test method for {@link com.InfoExchangeHub.Connectors.PDQQueryManager#queryPatientDemographics(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testITI47ConsumerQueryPatientPatientId_OtherIDsScopingOrganization_TLS()
	{
		PDQQueryManager pdqQueryManager = null;
		try
		{
			pdqQueryManager = new PDQQueryManager(PDQManagerTLSEndpointURI,
					true);
			PRPAIN201306UV02 pdqQueryResponse = pdqQueryManager.queryPatientDemographics(null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					"IHERED-993^^^&1.3.6.1.4.1.21367.13.20.1000&ISO",
					null,
					"1.3.6.1.4.1.21367.13.20.1000");
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link com.InfoExchangeHub.Connectors.PDQQueryManager#queryPatientDemographics(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
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
			String queryId = UUID.randomUUID().toString();
			PRPAIN201306UV02 pdqQueryResponse = pdqQueryManager.queryPatientDemographics(null,
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
					1,
					queryId);
			
			// Now send the PDQ query continuation message (QUQI_IN000003UV01), requesting one additional record...
			pdqQueryResponse = pdqQueryManager.queryContinue(pdqQueryResponse,
					1,
					queryId);
			
			// Now send a PDQ query cancel message (QUQI_IN000003UV01)
//			pdqQueryManager.queryCancel(pdqQueryResponse,
//					queryId);
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link com.InfoExchangeHub.Connectors.PDQQueryManager#queryPatientDemographics(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testITI47ConsumerQueryContinuationOptionTLS()
	{
		PDQQueryManager pdqQueryManager = null;
		try
		{
			pdqQueryManager = new PDQQueryManager(null,
					true);
			String queryId = UUID.randomUUID().toString();
			PRPAIN201306UV02 pdqQueryResponse = pdqQueryManager.queryPatientDemographics(null,
					null,
					null,
					null,
					"M",
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					2,
					queryId);
			
			// Now send the PDQ query continuation message (QUQI_IN000003UV01), requesting five additional records...
			pdqQueryResponse = pdqQueryManager.queryContinue(pdqQueryResponse,
					2,
					queryId);

			// Now send the PDQ query continuation message (QUQI_IN000003UV01), requesting five additional records...
//			pdqQueryResponse = pdqQueryManager.queryContinue(pdqQueryResponse,
//					5);

			// Now send the PDQ query continuation message (QUQI_IN000003UV01), requesting five additional records...
//			pdqQueryResponse = pdqQueryManager.queryContinue(pdqQueryResponse,
//					5);

			// Now send a PDQ query cancel message (QUQI_IN000003UV01)
//			pdqQueryManager.queryCancel(pdqQueryResponse);
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}

}
