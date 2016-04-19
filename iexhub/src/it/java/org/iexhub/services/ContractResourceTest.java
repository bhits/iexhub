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
 * FHIR Contract Resource Test
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
import ca.uhn.fhir.model.dstu2.resource.Contract;
import ca.uhn.fhir.model.dstu2.resource.Organization;
import ca.uhn.fhir.model.dstu2.resource.Practitioner;
import ca.uhn.fhir.model.dstu2.resource.SearchParameter;
import ca.uhn.fhir.model.dstu2.resource.Organization.Contact;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.valueset.AddressTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.dstu2.valueset.ContactPointSystemEnum;
import ca.uhn.fhir.model.dstu2.valueset.ContactPointUseEnum;
import ca.uhn.fhir.model.dstu2.valueset.ContractTypeCodesEnum;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;

/**
 * @author A. Sute
 *
 */
public class ContractResourceTest
{
	private static String propertiesFile = "/temp/IExHub.properties";
	private static String uriPrefix = "urn:oid";
	private static String iExHubDomainOid = "2.16.840.1.113883.3.72.5.9.1";
	private static String iExHubAssigningAuthority = "ISO";
	private static int fhirClientSocketTimeout = 5000;
	private static String serverBaseUrl = "http://localhost:8080/iexhub/services";
	private static String localPatientId = "4d6e1717-99f8-4e96-bf88-be7cea8f5820";
	private static String providerOrganizationId = "2.16.840.1.113883.6.1";
	private static String practitionerId = "db653797-5ad7-41bf-8abf-14588da7f7e8";
	private static String versionId = "1"; // default version
	//FHIR objects used to create a Consent
	private static Patient testPatient = new Patient();
	private static Organization providerOrganization = new Organization();
	private static Contact organizationContact = new Contact();
	private static Practitioner testPractitioner = new Practitioner();
	{
		//create the testPatient resource to be embedded into a contract
		testPatient.setId(new IdDt("Patient", localPatientId,versionId));
		testPatient.addName().addFamily("Patient_family").addGiven("Patient_given_name");
		// set SSN value using coding system 2.16.840.1.113883.4.1
		testPatient.addIdentifier().setSystem(uriPrefix+"2.16.840.1.113883.4.1").setValue("123-45-6789");
		// set local patient id
		testPatient.addIdentifier().setSystem(uriPrefix+iExHubDomainOid).setValue(localPatientId);
		testPatient.setGender(AdministrativeGenderEnum.MALE);
		testPatient.setBirthDate(new DateDt("1966-10-22"));
		testPatient.addAddress().addLine("Patient Address Line").setCity("City").setState("NY").setPostalCode("12345").setType(AddressTypeEnum.POSTAL);
		testPatient.addTelecom().setUse(ContactPointUseEnum.HOME).setSystem(ContactPointSystemEnum.PHONE).setValue("555-1212");
		// Provider organization...		
		//set id to be used to reference the providerOrganization as an inline resource 
		providerOrganization.setId(new IdDt(providerOrganizationId));
		providerOrganization.addIdentifier().setSystem("NPI uri").setValue("NPI of organization");
		providerOrganization.setName("Provider Organization");
		providerOrganization.addAddress().addLine("1 Main Street").setCity("Cupertino").setState("CA").setPostalCode("95014");
		//contact
		organizationContact.addTelecom().setSystem(ContactPointSystemEnum.PHONE).setValue("tel:408-555-1212");
		HumanNameDt contactName = new HumanNameDt();		
		contactName.addFamily().setValue("JONES");
		contactName.addGiven().setValue("MARTHA");
		organizationContact.setName(contactName);
		List<Contact> contacts = new ArrayList<Contact>();
		contacts.add(organizationContact);
		providerOrganization.setContact(contacts);
		//set reference using "#" prefix
		testPatient.addCareProvider().setReference("#"+providerOrganizationId);
		//add resource 
		testPatient.getContained().getContainedResources().add(providerOrganization);
		//intended recipient	
		testPractitioner.setId(new IdDt("Practitioner",practitionerId, versionId));
		testPractitioner.addIdentifier().setSystem("NPI uri").setValue("NPI");
		testPractitioner.getName().addFamily("Practitioner Last Name").addGiven("Practitioner Given Name").addSuffix("MD");
		testPractitioner.addAddress().addLine("Practitioner Address Line").setCity("City").setState("NY").setPostalCode("98765");
		testPractitioner.addTelecom().setSystem(ContactPointSystemEnum.PHONE).setValue("212-555-1212");
	
	}
	
	/**
	 * Test method for {@link org.iexhub.services.JaxRsContractRestProvider#find(@IdParam final IdDt id)}.
	 */
	@Test
	public void testFindContract()
	{
		Properties props = null;
		try
		{
			props = new Properties();
			props.load(new FileInputStream(propertiesFile));
			ContractResourceTest.iExHubDomainOid = (props.getProperty("IExHubDomainOID") == null) ? ContractResourceTest.iExHubDomainOid
					: props.getProperty("IExHubDomainOID");
			ContractResourceTest.iExHubAssigningAuthority = (props.getProperty("IExHubAssigningAuthority") == null) ? ContractResourceTest.iExHubAssigningAuthority
					: props.getProperty("IExHubAssigningAuthority");
			ContractResourceTest.fhirClientSocketTimeout = (props.getProperty("FHIRClientSocketTimeoutInMs") == null) ? ContractResourceTest.fhirClientSocketTimeout
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
			Logger logger = LoggerFactory.getLogger(ContractResourceTest.class);
			FhirContext ctxt = new FhirContext();
			ctxt.getRestfulClientFactory().setSocketTimeout(ContractResourceTest.fhirClientSocketTimeout);

			LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
			loggingInterceptor.setLogRequestSummary(true);
			loggingInterceptor.setLogRequestBody(true);
			loggingInterceptor.setLogger(logger);
			
			IGenericClient client = ctxt.newRestfulGenericClient(serverBaseUrl);
			client.registerInterceptor(loggingInterceptor);							// Required only for logging
			Contract retVal = client.read(Contract.class,
					"HJ-361%5E2.16.840.1.113883.3.72.5.9.1");
//		"d80383d0-f561-11e5-83b6-00155dc95705%5E2.16.840.1.113883.4.357");
			assertTrue("Error - unexpected return value for testFindContract",
					retVal != null);
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link org.iexhub.services.JaxRsContractRestProvider#search(@IdParam final IdDt id)}.
	 */
	@Test
	public void testSearchContract()
	{
		Properties props = null;
		try
		{
			props = new Properties();
			props.load(new FileInputStream(propertiesFile));
			ContractResourceTest.iExHubDomainOid = (props.getProperty("IExHubDomainOID") == null) ? ContractResourceTest.iExHubDomainOid
					: props.getProperty("IExHubDomainOID");
			ContractResourceTest.iExHubAssigningAuthority = (props.getProperty("IExHubAssigningAuthority") == null) ? ContractResourceTest.iExHubAssigningAuthority
					: props.getProperty("IExHubAssigningAuthority");
			ContractResourceTest.fhirClientSocketTimeout = (props.getProperty("FHIRClientSocketTimeoutInMs") == null) ? ContractResourceTest.fhirClientSocketTimeout
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
			Logger logger = LoggerFactory.getLogger(ContractResourceTest.class);
			FhirContext ctxt = new FhirContext();
			ctxt.getRestfulClientFactory().setSocketTimeout(ContractResourceTest.fhirClientSocketTimeout);

			LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
			loggingInterceptor.setLogRequestSummary(true);
			loggingInterceptor.setLogRequestBody(true);
			loggingInterceptor.setLogger(logger);
			
			IGenericClient client = ctxt.newRestfulGenericClient(serverBaseUrl);
			client.registerInterceptor(loggingInterceptor);		
			
			ca.uhn.fhir.model.dstu2.resource.Bundle response = client.search()
					.forResource(Contract.class)
					//.where(Contract.)
					.returnBundle(ca.uhn.fhir.model.dstu2.resource.Bundle.class).execute();
				
		assertTrue("Error - unexpected return value for testSearchContract",
					response != null);
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link org.iexhub.services.JaxRsContractRestProvider#create(Patient patient, String theConditional)}.
	 * Basic Consent Content 
	 */
	@Test
	public void testCreateContract()
	{
		Properties props = null;
		try
		{
			props = new Properties();
			props.load(new FileInputStream(propertiesFile));
			ContractResourceTest.iExHubDomainOid = (props.getProperty("IExHubDomainOID") == null) ? ContractResourceTest.iExHubDomainOid
					: props.getProperty("IExHubDomainOID");
			ContractResourceTest.iExHubAssigningAuthority = (props.getProperty("IExHubAssigningAuthority") == null) ? ContractResourceTest.iExHubAssigningAuthority
					: props.getProperty("IExHubAssigningAuthority");
			ContractResourceTest.fhirClientSocketTimeout = (props.getProperty("FHIRClientSocketTimeoutInMs") == null) ? ContractResourceTest.fhirClientSocketTimeout
					: Integer.parseInt(props.getProperty("FHIRClientSocketTimeoutInMs"));
		}
		catch (IOException e)
		{
			throw new UnexpectedServerException("Error encountered loading properties file, "
					+ propertiesFile
					+ ", "
					+ e.getMessage());
		}

		// ITI-41 ProvideAndRegisterDocumentSet using Contract service 
		try
		{
			Contract contract = new Contract();
			contract.getIdentifier().setSystem(uriPrefix+iExHubDomainOid).setValue("consent GUID");
			contract.getType().setValueAsEnum(ContractTypeCodesEnum.DISCLOSURE);			
			DateTimeDt issuedDateTime = new DateTimeDt();
			issuedDateTime.setValue(Calendar.getInstance().getTime());
			contract.setIssued(issuedDateTime);
			//specify the covered entity making the disclosure
			contract.addAuthority().setReference("#"+providerOrganizationId);
			contract.getContained().getContainedResources().add(providerOrganization);
			//specify the provider who authored the data
			
			//specify the 
			//contract.getSubject().add();	
			contract.getContained().getContainedResources().add(testPatient);
			Logger logger = LoggerFactory.getLogger(PatientResourceTest.class);
			FhirContext ctxt = new FhirContext();
			ctxt.getRestfulClientFactory().setSocketTimeout(ContractResourceTest.fhirClientSocketTimeout);

			LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
			loggingInterceptor.setLogRequestSummary(true);
			loggingInterceptor.setLogRequestBody(true);
			loggingInterceptor.setLogger(logger);
			
			IGenericClient client = ctxt.newRestfulGenericClient(serverBaseUrl);
			client.registerInterceptor(loggingInterceptor);							// Required only for logging
			MethodOutcome outcome = client.create().resource(contract).execute();			
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}	
}
