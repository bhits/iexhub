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
/**
 * 
 */
package org.iexhub.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.composite.HumanNameDt;
import ca.uhn.fhir.model.dstu2.composite.IdentifierDt;
import ca.uhn.fhir.model.dstu2.resource.Organization;
import ca.uhn.fhir.model.dstu2.resource.Organization.Contact;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.dstu2.valueset.IdentifierUseEnum;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import org.apache.commons.io.FileUtils;
import org.iexhub.exceptions.UnexpectedServerException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

//import org.apache.log4j.Logger;

/**
 * FHIR Patient Resource Test
 * 
 * @author A. Sute
 *
 */
public class PatientResourceTest {
	private static String propertiesFile = "/temp/IExHub.properties";
	private static String iExHubDomainOid = "2.16.840.1.113883.3.72.5.9.1";
	private static String pidAssigningAuthority = "ISO";
	private static int fhirClientSocketTimeout = 5000;
	private static String serverBaseUrl = "http://localhost:8080/iexhub/services";
	private static String uriPrefix = "urn:oid:";
	private static String testResourcesPath = "src/test/resources/";

	/**
	 * Test method for
	 * {@link org.iexhub.services.JaxRsPatientRestProvider\#find(@IdParam final
	 * IdDt id)}.
	 */
	@Test
	public void testFindPatient() {
		Properties props = null;
		try {
			props = new Properties();
			props.load(new FileInputStream(propertiesFile));
			PatientResourceTest.iExHubDomainOid = (props.getProperty("IExHubDomainOID") == null)
					? PatientResourceTest.iExHubDomainOid : props.getProperty("IExHubDomainOID");
			PatientResourceTest.pidAssigningAuthority = (props.getProperty("IExHubAssigningAuthority") == null)
					? PatientResourceTest.pidAssigningAuthority : props.getProperty("IExHubAssigningAuthority");
			PatientResourceTest.fhirClientSocketTimeout = (props.getProperty("FHIRClientSocketTimeoutInMs") == null)
					? PatientResourceTest.fhirClientSocketTimeout
					: Integer.parseInt(props.getProperty("FHIRClientSocketTimeoutInMs"));
		} catch (IOException e) {
			throw new UnexpectedServerException(
					"Error encountered loading properties file, " + propertiesFile + ", " + e.getMessage());
		}

		try {
			Logger logger = LoggerFactory.getLogger(PatientResourceTest.class);
			FhirContext ctxt = new FhirContext();
			ctxt.getRestfulClientFactory().setSocketTimeout(PatientResourceTest.fhirClientSocketTimeout);

			LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
			loggingInterceptor.setLogRequestSummary(true);
			loggingInterceptor.setLogRequestBody(true);
			loggingInterceptor.setLogger(logger);

			IGenericClient client = ctxt.newRestfulGenericClient(serverBaseUrl);
			client.registerInterceptor(loggingInterceptor); // Required only for
															// logging
			Patient retVal = client.read(Patient.class, "HJ-361%5E2.16.840.1.113883.3.72.5.9.1");
			// "d80383d0-f561-11e5-83b6-00155dc95705%5E2.16.840.1.113883.4.357");
			assertTrue("Error - unexpected return value for testFindPatient", retVal != null);
		} catch (Exception e) {
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for
	 * {@link org.iexhub.services.JaxRsPatientRestProvider#search\(@IdParam
	 * final IdDt id)}.
	 */
	@Test
	public void testSearchPatient() {
		Properties props = null;
		try {
			props = new Properties();
			props.load(new FileInputStream(propertiesFile));
			PatientResourceTest.iExHubDomainOid = (props.getProperty("IExHubDomainOID") == null)
					? PatientResourceTest.iExHubDomainOid : props.getProperty("IExHubDomainOID");
			PatientResourceTest.pidAssigningAuthority = (props.getProperty("IExHubAssigningAuthority") == null)
					? PatientResourceTest.pidAssigningAuthority : props.getProperty("IExHubAssigningAuthority");
			PatientResourceTest.fhirClientSocketTimeout = (props.getProperty("FHIRClientSocketTimeoutInMs") == null)
					? PatientResourceTest.fhirClientSocketTimeout
					: Integer.parseInt(props.getProperty("FHIRClientSocketTimeoutInMs"));
		} catch (IOException e) {
			throw new UnexpectedServerException(
					"Error encountered loading properties file, " + propertiesFile + ", " + e.getMessage());
		}

		try {
			Logger logger = LoggerFactory.getLogger(PatientResourceTest.class);
			FhirContext ctxt = new FhirContext();
			ctxt.getRestfulClientFactory().setSocketTimeout(PatientResourceTest.fhirClientSocketTimeout);

			LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
			loggingInterceptor.setLogRequestSummary(true);
			loggingInterceptor.setLogRequestBody(true);
			loggingInterceptor.setLogger(logger);

			IGenericClient client = ctxt.newRestfulGenericClient(serverBaseUrl);
			client.registerInterceptor(loggingInterceptor); // Required only for
															// logging

			// FAMILY and GIVEN values are required for all queries. Optionally,
			// the following can also be specified:
			//
			// Patient.GENDER
			// Patient.BIRTHDATE
			// Patient.ADDRESS
			// Patient.ADDRESS_CITY
			// Patient.ADDRESS_STATE
			// Patient.ADDRESS_POSTALCODE
			// Patient.IDENTIFIER
			// Patient.TELECOM
			ca.uhn.fhir.model.dstu2.resource.Bundle response = client.search().forResource(Patient.class)
					// .where(Patient.FAMILY.matches().values("HINOJOXS"))
					// .and(Patient.GIVEN.matches().values("JOYCE"))
					.where(Patient.FAMILY.matches().values("SMITH")).and(Patient.GIVEN.matches().values("ANDREW"))
					// .and(Patient.BIRTHDATE.exactly().day("1967-12-14")) //
					// MUST specify as YYYY-MM-DD
					// .and(Patient.TELECOM.exactly().identifier("tel:706-750-4736"))
					// // MUST specify as "tel:XXX-XXX-XXXX" if phone number
					.returnBundle(ca.uhn.fhir.model.dstu2.resource.Bundle.class).execute();
			assertTrue("Error - unexpected return value for testSearchPatient", response != null);
		} catch (Exception e) {
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for
	 * {@link org.iexhub.services.JaxRsPatientRestProvider#create(Patient patient, String theConditional)}
	 * .
	 */
	@Test
	public void testRegisterPatient() {
		Properties props = null;
		try {
			props = new Properties();
			props.load(new FileInputStream(propertiesFile));
			PatientResourceTest.iExHubDomainOid = (props.getProperty("IExHubDomainOID") == null)
					? PatientResourceTest.iExHubDomainOid : props.getProperty("IExHubDomainOID");
			PatientResourceTest.pidAssigningAuthority = (props.getProperty("IExHubAssigningAuthority") == null)
					? PatientResourceTest.pidAssigningAuthority : props.getProperty("IExHubAssigningAuthority");
			PatientResourceTest.fhirClientSocketTimeout = (props.getProperty("FHIRClientSocketTimeoutInMs") == null)
					? PatientResourceTest.fhirClientSocketTimeout
					: Integer.parseInt(props.getProperty("FHIRClientSocketTimeoutInMs"));
		} catch (IOException e) {
			throw new UnexpectedServerException(
					"Error encountered loading properties file, " + propertiesFile + ", " + e.getMessage());
		}

		// ITI-44-Source-Feed message
		try {
			Patient patientResource = new Patient();
			// pat.addName().addFamily("HINOJOXS").addGiven("JOYCE");
			patientResource.addName().addFamily("SMITH").addGiven("BOB");
			// SSN
			patientResource.addIdentifier().setSystem(uriPrefix + "2.16.840.1.113883.4.1").setValue("321-54-9876");
			// MRN - the "official" identifier used to refer to this patient
			// identity
			// The server checks the "use" to determine that this is the official/trusted identifier supplied by the application
			patientResource.addIdentifier().setSystem(uriPrefix + iExHubDomainOid)
			        .setUse(IdentifierUseEnum.OFFICIAL)
					.setValue("MRN");
			//
			List<IdentifierDt> idList = patientResource.getIdentifier();
			// find the "official" identifier aka MRN
			IdentifierDt mrn = null; 
			if (idList != null) {
				Iterator<IdentifierDt> idIterator = idList.iterator();
				while (idIterator.hasNext()) {
					IdentifierDt id = idIterator.next();
					String use = id.getUse();
					
					if ((use!= null) && (use.equals(IdentifierUseEnum.OFFICIAL.getCode()))) {
						// this is the official identifier
						mrn = id;
						break;
					}
				}
			}
			//the server set the Patient.id to the official Patient.identifier if the PIX add was successful
			//patient.setId(mrn.getValue());
			assertTrue(mrn!=null);
			assertTrue(mrn.getValue().equals("MRN"));
			assertTrue(mrn.getSystem().equals(uriPrefix + iExHubDomainOid));
			patientResource.setGender(AdministrativeGenderEnum.MALE);
			Calendar dobCalendar = Calendar.getInstance();
			dobCalendar.set(1978, 11, 8);
			DateDt dob = new DateDt();
			dob.setValue(dobCalendar.getTime());
			patientResource.setBirthDate(dob);
			patientResource.addTelecom().setValue("tel:408-555-1010");
			// Provider organization...
			Organization organizationResource = new Organization();
			IdDt orgId = new IdDt();
			orgId.setValue("urn:oid:2.16.840.1.113883.6.1");
			organizationResource.setId(orgId);
			organizationResource.setName("Provider Organization");
			organizationResource.addAddress().addLine("1 Main Street").setCity("Cupertino").setState("CA")
					.setPostalCode("95014");
			Contact organizationContact = new Contact();
			organizationContact.addTelecom().setValue("tel:408-555-1212");
			HumanNameDt contactName = new HumanNameDt();
			contactName.addFamily().setValue("JONES");
			contactName.addGiven().setValue("MARTHA");
			organizationContact.setName(contactName);
			List<Contact> contacts = new ArrayList<Contact>();
			contacts.add(organizationContact);
			organizationResource.setContact(contacts);
			patientResource.addCareProvider().setReference("#urn:oid:2.16.840.1.113883.6.1");
			patientResource.getContained().getContainedResources().add(organizationResource);
			Logger logger = LoggerFactory.getLogger(PatientResourceTest.class);
			FhirContext ctxt = new FhirContext();
			// Set FHIR DSTU2
			FhirContext.forDstu2();
			ctxt.getRestfulClientFactory().setSocketTimeout(PatientResourceTest.fhirClientSocketTimeout);

			LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
			loggingInterceptor.setLogRequestSummary(true);
			loggingInterceptor.setLogRequestBody(true);
			loggingInterceptor.setLogger(logger);

			String xmlEncodedPatient = ctxt.newXmlParser().setPrettyPrint(true).encodeResourceToString(patientResource);
			try {
				FileUtils.writeStringToFile(new File(testResourcesPath + "/XML/" + "testRegisterPatient.xml"),
						xmlEncodedPatient);
			} catch (IOException e) {

				fail("Write Patient resource to XML:" + e.getMessage());
			}

			IGenericClient client = ctxt.newRestfulGenericClient(serverBaseUrl);
			client.registerInterceptor(loggingInterceptor); // Required only for
															// logging
			client.create().resource(patientResource).execute();
		} catch (Exception e) {
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for
	 * {@link org.iexhub.connectors.PIXManager\#registerPatient(java.lang.String,
	 * java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testRegisterPatientTLS() {
		Properties props = null;
		try {
			props = new Properties();
			props.load(new FileInputStream(propertiesFile));
			PatientResourceTest.iExHubDomainOid = (props.getProperty("IExHubDomainOID") == null)
					? PatientResourceTest.iExHubDomainOid : props.getProperty("IExHubDomainOID");
			PatientResourceTest.pidAssigningAuthority = (props.getProperty("IExHubAssigningAuthority") == null)
					? PatientResourceTest.pidAssigningAuthority : props.getProperty("IExHubAssigningAuthority");
		} catch (IOException e) {
			throw new UnexpectedServerException(
					"Error encountered loading properties file, " + propertiesFile + ", " + e.getMessage());
		}

		// ITI-44-Source-Feed message
		try {
			Patient patientResource = new Patient();
			patientResource.addName().addFamily("ALPHA").addGiven("ALAN");
			// SSN
			patientResource.addIdentifier().setSystem(uriPrefix + "2.16.840.1.113883.4.1").setValue("123-45-6789");
			// MRN - the "official" identifier used to refer to this patient
			// identity
			// The server checks the "use" to determine how to invoke the PIX
			// Add service
			patientResource.addIdentifier().setSystem(uriPrefix + iExHubDomainOid).setUse(IdentifierUseEnum.OFFICIAL)
					.setValue("PIX");
			patientResource.setGender(AdministrativeGenderEnum.MALE);
			Calendar dobCalendar = Calendar.getInstance();
			dobCalendar.set(1978, 11, 8);
			DateDt dob = new DateDt();
			dob.setValue(dobCalendar.getTime());
			patientResource.setBirthDate(dob);

			Logger logger = LoggerFactory.getLogger(PatientResourceTest.class);
			FhirContext ctxt = new FhirContext();
			ctxt.getRestfulClientFactory().setSocketTimeout(PatientResourceTest.fhirClientSocketTimeout);

			LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
			loggingInterceptor.setLogRequestSummary(true);
			loggingInterceptor.setLogRequestBody(true);
			loggingInterceptor.setLogger(logger);

			IGenericClient client = ctxt.newRestfulGenericClient(serverBaseUrl);
			client.registerInterceptor(loggingInterceptor); // Required only for
															// logging
			MethodOutcome outcome = client.create().resource(patientResource).execute();
		} catch (Exception e) {
			fail("Error - " + e.getMessage());
		}
	}
}
