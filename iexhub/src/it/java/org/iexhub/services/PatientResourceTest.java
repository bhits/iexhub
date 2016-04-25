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
package org.iexhub.services;

import static org.junit.Assert.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
//import org.apache.log4j.Logger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.iexhub.exceptions.UnexpectedServerException;
import org.junit.*;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.composite.HumanNameDt;
import ca.uhn.fhir.model.dstu2.resource.Organization;
import ca.uhn.fhir.model.dstu2.resource.Organization.Contact;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.resource.Contract;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;

/**
 * FHIR Patient Resource Test
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
					"HJ-361%5E2.16.840.1.113883.3.72.5.9.1");
//		"d80383d0-f561-11e5-83b6-00155dc95705%5E2.16.840.1.113883.4.357");
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
			//			Patient.IDENTIFIER
			//			Patient.TELECOM
			ca.uhn.fhir.model.dstu2.resource.Bundle response = client.search()
				      .forResource(Patient.class)
//				      .where(Patient.FAMILY.matches().values("HINOJOXS"))
//				      .and(Patient.GIVEN.matches().values("JOYCE"))
				      .where(Patient.FAMILY.matches().values("SMITH"))
				      .and(Patient.GIVEN.matches().values("ANDREW"))
//				      .and(Patient.BIRTHDATE.exactly().day("1967-12-14"))					// MUST specify as YYYY-MM-DD
//				      .and(Patient.TELECOM.exactly().identifier("tel:706-750-4736"))		// MUST specify as "tel:XXX-XXX-XXXX" if phone number
				      .returnBundle(ca.uhn.fhir.model.dstu2.resource.Bundle.class)
				      .execute();
			assertTrue("Error - unexpected return value for testSearchPatient",
					response != null);
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link org.iexhub.services.JaxRsPatientRestProvider#create(Patient patient, String theConditional)}.
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
//			pat.addName().addFamily("HINOJOXS").addGiven("JOYCE");
			pat.addName().addFamily("SMITH").addGiven("ANDREW");
			
			// SSN
			pat.addIdentifier().setSystem("2.16.840.1.113883.4.1").setValue("123-45-6789");
			
			pat.setGender(AdministrativeGenderEnum.MALE);
			Calendar dobCalendar = Calendar.getInstance();
			dobCalendar.set(1978,
					11,
					8);
			DateDt dob = new DateDt();
			dob.setValue(dobCalendar.getTime());
			pat.setBirthDate(dob);
			pat.addTelecom().setValue("tel:408-555-1010");

			// Provider organization...
			Organization organization = new Organization();
			IdDt orgId = new IdDt();
			orgId.setValue("urn:oid:2.16.840.1.113883.6.1");
			organization.setId(orgId);
			organization.setName("Provider Organization");
			organization.addAddress().addLine("1 Main Street").setCity("Cupertino").setState("CA").setPostalCode("95014");
			Contact organizationContact = new Contact();
			organizationContact.addTelecom().setValue("tel:408-555-1212");
			HumanNameDt contactName = new HumanNameDt();
			contactName.addFamily().setValue("JONES");
			contactName.addGiven().setValue("MARTHA");
			organizationContact.setName(contactName);
			List<Contact> contacts = new ArrayList<Contact>();
			contacts.add(organizationContact);
			organization.setContact(contacts);
			pat.addCareProvider().setReference("#urn:oid:2.16.840.1.113883.6.1");
			pat.getContained().getContainedResources().add(organization);
			
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
