/**
 * 
 */
package com.InfoExchangeHub.Services;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Properties;
import java.util.UUID;






//import org.apache.log4j.Logger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.*;

import com.InfoExchangeHub.Exceptions.UnexpectedServerException;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.dstu2.composite.IdentifierDt;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;

/**
 * @author A. Sute
 *
 */
public class PatientResourceTest
{
	private static final String PIXManagerEndpointURI = null;
	private static String propertiesFile = "/temp/IExHub.properties";
	private static String iExHubDomainOid = "2.16.840.1.113883.3.72.5.9.1";
	private static String iExHubAssigningAuthority = "ISO";
	private static int fhirClientSocketTimeout = 5000;
	
	/**
	 * Test method for {@link com.InfoExchangeHub.Connectors.PIXManager#registerPatient(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testRegisterPatient()
	{
		Properties props = null;
		try
		{
			props = new Properties();
			props.load(new FileInputStream(propertiesFile));
			PatientResourceTest.iExHubDomainOid = (props.getProperty("IExHubDomainOID") == null) ? PatientResourceTest.iExHubDomainOid
					: props.getProperty("IExHubDomainOID");
			PatientResourceTest.iExHubAssigningAuthority = (props.getProperty("IExHubAssigningAuthority") == null) ? PatientResourceTest.iExHubAssigningAuthority
					: props.getProperty("IExHubAssigningAuthority");
			PatientResourceTest.fhirClientSocketTimeout = (props.getProperty("FHIRClientSocketTimeoutInMs") == null) ? PatientResourceTest.fhirClientSocketTimeout
					: Integer.parseInt(props.getProperty("FHIRClientSocketTimeoutInMs"));
		}
		catch (IOException e)
		{
			throw new UnexpectedServerException("Error encountered loading properties file, "
					+ propertiesFile
					+ ", "
					+ e.getMessage());
		}

		// ITI-44-Source-Feed message
		try
		{
			Patient pat = new Patient();
			pat.addName().addFamily("ALPHA").addGiven("ALAN");
//			pat.addIdentifier().setSystem(PatientResourceTest.iExHubDomainOid).setValue(UUID.randomUUID().toString());
			pat.addIdentifier().setValue("PIX");
			pat.setGender(AdministrativeGenderEnum.MALE);
			Calendar dobCalendar = Calendar.getInstance();
			dobCalendar.set(1978,
					11,
					8);
			DateDt dob = new DateDt();
			dob.setValue(dobCalendar.getTime());
			pat.setBirthDate(dob);

			Logger logger = LoggerFactory.getLogger(PatientResourceTest.class);
			FhirContext ctxt = new FhirContext();
			ctxt.getRestfulClientFactory().setSocketTimeout(PatientResourceTest.fhirClientSocketTimeout);

			LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
			loggingInterceptor.setLogRequestSummary(true);
			loggingInterceptor.setLogRequestBody(true);
			loggingInterceptor.setLogger(logger);
			
			String serverBaseUrl = "http://localhost:8080/InfoExchangeHub/InfoExchangeHubServices";
			IGenericClient client = ctxt.newRestfulGenericClient(serverBaseUrl);
			client.registerInterceptor(loggingInterceptor);
			MethodOutcome outcome = client.create().resource(pat).execute();
			
//			assertTrue("Error - unexpected return value for RegisterPatient message",
//					pixRegistrationResponse.toString());
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}
	
	/**
	 * Test method for {@link com.InfoExchangeHub.Connectors.PIXManager#registerPatient(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testRegisterPatientTLS()
	{
		Properties props = null;
		try
		{
			props = new Properties();
			props.load(new FileInputStream(propertiesFile));
			PatientResourceTest.iExHubDomainOid = (props.getProperty("IExHubDomainOID") == null) ? PatientResourceTest.iExHubDomainOid
					: props.getProperty("IExHubDomainOID");
			PatientResourceTest.iExHubAssigningAuthority = (props.getProperty("IExHubAssigningAuthority") == null) ? PatientResourceTest.iExHubAssigningAuthority
					: props.getProperty("IExHubAssigningAuthority");
		}
		catch (IOException e)
		{
			throw new UnexpectedServerException("Error encountered loading properties file, "
					+ propertiesFile
					+ ", "
					+ e.getMessage());
		}

		// ITI-44-Source-Feed message
		try
		{
			Patient pat = new Patient();
			pat.addName().addFamily("WALTERS").addGiven("WILLIAM").addGiven("A");
			pat.addIdentifier().setSystem(PatientResourceTest.iExHubDomainOid).setValue(UUID.randomUUID().toString());
			pat.setGender(AdministrativeGenderEnum.MALE);
			
			FhirContext ctxt = new FhirContext();
			String serverBaseUrl = "http://localhost:8080/InfoExchangeHub/InfoExchangeHubServices";
			IGenericClient client = ctxt.newRestfulGenericClient(serverBaseUrl);
			MethodOutcome outcome = client.create().resource(pat).execute();
			
			DateTime oidTimeValue = DateTime.now(DateTimeZone.UTC);
//			MCCIIN000002UV01 pixRegistrationResponse = pixManager.registerPatient("WILLIAM",
//					"WALTERS",
//					null,
//					"5/5/1955",
//					"M",
//					String.valueOf(oidTimeValue.getMillis()));

//			assertTrue("Error - unexpected return value for RegisterPatient message",
//					pixRegistrationResponse.toString());
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}
}
