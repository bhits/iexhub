/**
 * 
 */
package org.iexhub.services;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Properties;
//import org.apache.log4j.Logger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.iexhub.exceptions.UnexpectedServerException;
import org.junit.*;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;

/**
 * @author A. Sute
 *
 */
public class PatientResourceTest
{
	private static String propertiesFile = "/temp/IExHub.properties";
	private static String iExHubDomainOid = "2.16.840.1.113883.3.72.5.9.1";
	private static String iExHubAssigningAuthority = "ISO";
	private static int fhirClientSocketTimeout = 5000;
	private static String serverBaseUrl = "http://localhost:8080/iexhub/services";

	
	/**
	 * Test method for {@link org.iexhub.services.JaxRsPatientRestProvider#find(@IdParam final IdDt id)}.
	 */
	@Test
	public void testFindPatient()
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

		try
		{
			Logger logger = LoggerFactory.getLogger(PatientResourceTest.class);
			FhirContext ctxt = new FhirContext();
			ctxt.getRestfulClientFactory().setSocketTimeout(PatientResourceTest.fhirClientSocketTimeout);

			LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
			loggingInterceptor.setLogRequestSummary(true);
			loggingInterceptor.setLogRequestBody(true);
			loggingInterceptor.setLogger(logger);
			
			IGenericClient client = ctxt.newRestfulGenericClient(serverBaseUrl);
			client.registerInterceptor(loggingInterceptor);							// Required only for logging
			Patient retVal = client.read(Patient.class,
					"HJ-361");
			assertTrue("Error - unexpected return value for testFindPatient",
					retVal != null);
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link org.iexhub.services.JaxRsPatientRestProvider#search(@IdParam final IdDt id)}.
	 */
	@Test
	public void testSearchPatient()
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

		try
		{
			Logger logger = LoggerFactory.getLogger(PatientResourceTest.class);
			FhirContext ctxt = new FhirContext();
			ctxt.getRestfulClientFactory().setSocketTimeout(PatientResourceTest.fhirClientSocketTimeout);

			LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
			loggingInterceptor.setLogRequestSummary(true);
			loggingInterceptor.setLogRequestBody(true);
			loggingInterceptor.setLogger(logger);
			
			IGenericClient client = ctxt.newRestfulGenericClient(serverBaseUrl);
			client.registerInterceptor(loggingInterceptor);							// Required only for logging
			
			// FAMILY and GIVEN values are required for all queries.  Optionally, the following can also be specified:
			//
			//			Patient.GENDER
			//			Patient.BIRTHDATE
			//			Patient.ADDRESS
			//			Patient.ADDRESS_CITY
			//			Patient.ADDRESS_STATE
			//			Patient.ADDRESS_POSTALCODE
			ca.uhn.fhir.model.dstu2.resource.Bundle response = client.search()
				      .forResource(Patient.class)
				      .where(Patient.FAMILY.matches().values("HINOJOXS"))
				      .and(Patient.GIVEN.matches().values("JOYCE"))
				      .returnBundle(ca.uhn.fhir.model.dstu2.resource.Bundle.class)
				      .execute();
			assertTrue("Error - unexpected return value for testFindPatient",
					response != null);
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link com.InfoExchangeHub.Services.JaxRsPatientRestProvider#create(Patient patient, String theConditional)}.
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
			
			IGenericClient client = ctxt.newRestfulGenericClient(serverBaseUrl);
			client.registerInterceptor(loggingInterceptor);							// Required only for logging
			MethodOutcome outcome = client.create().resource(pat).execute();			
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
			
			IGenericClient client = ctxt.newRestfulGenericClient(serverBaseUrl);
			client.registerInterceptor(loggingInterceptor);							// Required only for logging
			MethodOutcome outcome = client.create().resource(pat).execute();			
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}
}
