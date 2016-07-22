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

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.composite.*;
import ca.uhn.fhir.model.dstu2.resource.*;
import ca.uhn.fhir.model.dstu2.resource.Organization.Contact;
import ca.uhn.fhir.model.dstu2.valueset.*;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import org.apache.commons.io.FileUtils;
import org.iexhub.exceptions.UnexpectedServerException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

//import org.apache.log4j.Logger;

/**
 * Contract Resource Test - used to create, find, search Privacy Consents
 * 
 * @author A. Sute
 *
 */
public class ContractResourceTest {
	private static String testResourcesPath = "src/test/resources/";
	private static String propertiesFile = testResourcesPath+"/properties/IExHub.properties";
	private static Properties properties = new Properties();
	private static String uriPrefix = ""; //"urn:oid:";
	private static String iExHubDomainOid = "2.16.840.1.113883.3.72.5.9.1";
	private static String iExHubAssigningAuthority = "ISO";
	private static int fhirClientSocketTimeout = 5000;
	// IExHub FHIR server base
	private static String serverBaseUrl = "http://localhost:8080/iexhub/services";
	// FHIR objects used to create a Consent
	private static Patient testPatientResource = new Patient();
	private static Organization sourceOrganizationResource = new Organization();
	private static Contact organizationContactResource = new Contact();
	private static Practitioner sourcePractitionerResource = new Practitioner();
	private static Practitioner recipientPractitionerResource = new Practitioner();
	// FHIR resource identifiers for inline/embedded objects
	private static String consentId = "consentId";
	private static String patientId = "ffc486eff2b04b8"; /*"ffc486eff2b0999";*/ //"patientId";
	private static String sourceOrganizationId = "sourceOrgOID";
	private static String sourcePractitionerId = "sourcePractitionerNPI";
	private static String recipientPractitionerId = "recipientPractitionerNPI";
	//FHIr context singleton
	private static FhirContext ctxt = new FhirContext();
	static {
		try {
			properties.load(new FileInputStream(propertiesFile));
			ContractResourceTest.iExHubDomainOid = (properties.getProperty("IExHubDomainOID") == null)
					? ContractResourceTest.iExHubDomainOid : properties.getProperty("IExHubDomainOID");
			ContractResourceTest.iExHubAssigningAuthority = (properties.getProperty("IExHubAssigningAuthority") == null)
					? ContractResourceTest.iExHubAssigningAuthority
					: properties.getProperty("IExHubAssigningAuthority");
			ContractResourceTest.fhirClientSocketTimeout = (properties
					.getProperty("FHIRClientSocketTimeoutInMs") == null) ? ContractResourceTest.fhirClientSocketTimeout
							: Integer.parseInt(properties.getProperty("FHIRClientSocketTimeoutInMs"));
			ContractResourceTest.iExHubDomainOid = (properties.getProperty("IExHubDomainOID") == null) ? ContractResourceTest.iExHubDomainOid
					: properties.getProperty("IExHubDomainOID");
		} catch (IOException e) {
			throw new UnexpectedServerException(
					"Error encountered loading properties file, " + propertiesFile + ", " + e.getMessage());
		}

		ctxt.getRestfulClientFactory().setSocketTimeout(fhirClientSocketTimeout);

		// create the testPatient resource to be embedded into a contract
		testPatientResource.setId(new IdDt(patientId));
		testPatientResource.addName().addFamily("Patient Family Name").addGiven("Patient Given Name");
		// set SSN value using coding system 2.16.840.1.113883.4.1
//		testPatientResource.addIdentifier().setSystem(uriPrefix + "2.16.840.1.113883.4.1").setValue("123-45-6789");
//		testPatientResource.addIdentifier().setSystem(uriPrefix + "1.3.6.1.4.1.21367.2005.13.20.1000").setValue(patientId);
		// set local patient id
		testPatientResource.addIdentifier().setSystem(uriPrefix + iExHubDomainOid).setValue(patientId);
		testPatientResource.setGender(AdministrativeGenderEnum.FEMALE);
		testPatientResource.setBirthDate(new DateDt("1966-10-22"));
		testPatientResource.addAddress().addLine("Patient Address Line").setCity("City").setState("NY")
				.setPostalCode("12345").setType(AddressTypeEnum.POSTAL);
		testPatientResource.addTelecom().setUse(ContactPointUseEnum.HOME)
		.setSystem(ContactPointSystemEnum.PHONE).setValue("555-1212");
		testPatientResource.addTelecom().setUse(ContactPointUseEnum.HOME)
		.setSystem(ContactPointSystemEnum.EMAIL).setValue("patient@home.org");
		// Provider organization...
		// set id to be used to reference the providerOrganization as an inline
		// resource
		sourceOrganizationResource.setId(new IdDt(sourceOrganizationId));
		sourceOrganizationResource.addIdentifier().setSystem(uriPrefix + "2.16.840.1.113883.4.6").setValue("NPI of source organization");
		sourceOrganizationResource.setName("Provider Organization Name");
		sourceOrganizationResource.addAddress().addLine("1 Main Street").setCity("Cupertino").setState("CA")
				.setPostalCode("95014");
		// contact
		organizationContactResource.addTelecom().setUse(ContactPointUseEnum.WORK)
		.setSystem(ContactPointSystemEnum.PHONE).setValue("408-555-1212");
		organizationContactResource.addTelecom().setUse(ContactPointUseEnum.WORK)
		.setSystem(ContactPointSystemEnum.EMAIL)
		.setValue("contact@sourceorgnization.org");
		HumanNameDt contactName = new HumanNameDt();
		contactName.addFamily().setValue("Contact Family Name");
		contactName.addGiven().setValue("Contact Given Name");
		organizationContactResource.setName(contactName);
		List<Contact> contacts = new ArrayList<Contact>();
		contacts.add(organizationContactResource);
		sourceOrganizationResource.setContact(contacts);
		// set reference using "#" prefix
		testPatientResource.addCareProvider().setReference("#" + sourceOrganizationId);
		
		// authoring practitioner
		sourcePractitionerResource.setId(new IdDt(sourcePractitionerId));
		sourcePractitionerResource.addIdentifier().setSystem(uriPrefix + "2.16.840.1.113883.4.6").setValue("NPI");
		sourcePractitionerResource.getName().addFamily("Source Practitioner Last Name")
				.addGiven("Source Practitioner Given Name").addSuffix("MD");
		sourcePractitionerResource.addAddress().addLine("Source Practitioner Address Line").setCity("City")
				.setState("NY").setPostalCode("98765");
		sourcePractitionerResource.addTelecom().setSystem(ContactPointSystemEnum.PHONE).setValue("212-555-1212");
		sourcePractitionerResource.addTelecom().setUse(ContactPointUseEnum.WORK)
		.setSystem(ContactPointSystemEnum.EMAIL).setValue("contact@sourceorgnization.org");
		// recipient practitioner
		recipientPractitionerResource.setId(new IdDt(recipientPractitionerId));
		recipientPractitionerResource.addIdentifier().setSystem(uriPrefix + "2.16.840.1.113883.4.6").setValue("NPI");
		recipientPractitionerResource.getName().addFamily("Recipient Practitioner Last Name")
				.addGiven("Recipient Practitioner Given Name").addSuffix("MD").addPrefix("Ms.");
		recipientPractitionerResource.addAddress().addLine("Recipient Practitioner Address Line").setCity("City")
				.setState("NY").setPostalCode("98765");
		recipientPractitionerResource.addTelecom().setUse(ContactPointUseEnum.WORK)
		.setSystem(ContactPointSystemEnum.PHONE).setValue("212-000-1212");
		recipientPractitionerResource.addTelecom().setUse(ContactPointUseEnum.WORK)
		.setSystem(ContactPointSystemEnum.EMAIL).setValue("recipient@destination.org");
	}

	/**
	 * Test method for
	 * {@link org.iexhub.services.JaxRsContractRestProvider\#find(@IdParam final
	 * IdDt id)}.
	 */
	@Test
	public void testFindContract() {

		try {
			Logger logger = LoggerFactory.getLogger(ContractResourceTest.class);
			LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
			loggingInterceptor.setLogRequestSummary(true);
			loggingInterceptor.setLogRequestBody(true);
			loggingInterceptor.setLogger(logger);

			IGenericClient client = ctxt.newRestfulGenericClient(serverBaseUrl);
			client.registerInterceptor(loggingInterceptor); // Required only for
															// logging
			Contract retVal = client.read(Contract.class, iExHubDomainOid+"."+consentId);
			
			assertTrue("Error - unexpected return value for testFindContract", retVal != null);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Test method for
	 * {@link org.iexhub.services.JaxRsContractRestProvider#search\(@IdParam
	 * final IdDt id)}.
	 */
	@Test
	public void testSearchContract() {

		try {
			Logger logger = LoggerFactory.getLogger(ContractResourceTest.class);
			LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
			loggingInterceptor.setLogRequestSummary(true);
			loggingInterceptor.setLogRequestBody(true);
			loggingInterceptor.setLogger(logger);

			IGenericClient client = ctxt.newRestfulGenericClient(serverBaseUrl);
			client.registerInterceptor(loggingInterceptor);

			IdentifierDt searchParam = new IdentifierDt(iExHubDomainOid/*"1.3.6.1.4.1.21367.2005.13.20.1000"*/,
					patientId);
			ca.uhn.fhir.model.dstu2.resource.Bundle response = client
					.search()
					.forResource(Contract.class)
					.where(Patient.IDENTIFIER.exactly().identifier(searchParam))
					.returnBundle(ca.uhn.fhir.model.dstu2.resource.Bundle.class).execute();

			assertTrue("Error - unexpected return value for testSearchContract", response != null);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Test method for Basic Consent Content
	 * {@link org.iexhub.services.JaxRsContractRestProvider\#create(Patient patient, String theConditional)}
	 * 
	 * @author Ioana Singureanu
	 */
	@Test
	public void testCreateBasicConsent() {

		// Create a Privacy Consent as a Contract to be submitted as document
		// using ITI-41
		String currentTest = "BasicConsent";
		try {
			Logger logger = LoggerFactory.getLogger(ContractResourceTest.class);
			LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
			loggingInterceptor.setLogRequestSummary(true);
			loggingInterceptor.setLogRequestBody(true);
			loggingInterceptor.setLogger(logger);

			Contract contract = createBasicTestConsent();

			// Use the narrative generator
			// @TODO: add generator Thymeleaf templates
			// ctxt.setNarrativeGenerator(new  DefaultThymeleafNarrativeGenerator());

			String xmlEncodedGranularConsent = ctxt.newXmlParser().setPrettyPrint(true)
					.encodeResourceToString(contract);
			FileUtils.writeStringToFile(new File(testResourcesPath+"/XML/"+currentTest+".xml"), xmlEncodedGranularConsent);
			String jsonEncodedGranularConsent = ctxt.newJsonParser().setPrettyPrint(true)
					.encodeResourceToString(contract);
			FileUtils.writeStringToFile(new File(testResourcesPath+"/JSON/"+currentTest+".json"), jsonEncodedGranularConsent);

			// create FHIR client
			IGenericClient client = ctxt.newRestfulGenericClient(serverBaseUrl);
			client.registerInterceptor(loggingInterceptor);
			 
			//  invoke Contract service
			client.create().resource(contract).execute();
		} catch (Exception e) {
			fail( e.getMessage());
		}
	}

	/**
	 * Test method for Basic Consent Content update
	 * {@link org.iexhub.services.JaxRsContractRestProvider\#create(Patient patient, String theConditional)}
	 * 
	 * @author A. Sute
	 */
	@Test
	public void testUpdateBasicConsent() {

		// Create a Privacy Consent as a Contract to be submitted as document
		// using ITI-41
		String currentTest = "BasicConsentUpdate";
		try {
			Logger logger = LoggerFactory.getLogger(ContractResourceTest.class);
			LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
			loggingInterceptor.setLogRequestSummary(true);
			loggingInterceptor.setLogRequestBody(true);
			loggingInterceptor.setLogger(logger);

			Contract contract = createBasicTestConsent(UUID.fromString("819efe60-d1bb-47b7-b5d6-ab5fa073eef0"));

			// Use the narrative generator
			// @TODO: add generator Thymeleaf templates
			// ctxt.setNarrativeGenerator(new  DefaultThymeleafNarrativeGenerator());

			String xmlEncodedGranularConsent = ctxt.newXmlParser().setPrettyPrint(true)
					.encodeResourceToString(contract);
			FileUtils.writeStringToFile(new File(testResourcesPath+"/XML/"+currentTest+".xml"), xmlEncodedGranularConsent);
			String jsonEncodedGranularConsent = ctxt.newJsonParser().setPrettyPrint(true)
					.encodeResourceToString(contract);
			FileUtils.writeStringToFile(new File(testResourcesPath+"/JSON/"+currentTest+".json"), jsonEncodedGranularConsent);

			// create FHIR client
			IGenericClient client = ctxt.newRestfulGenericClient(serverBaseUrl);
			client.registerInterceptor(loggingInterceptor);
			
			//  invoke Contract service
			client.update().resource(contract).execute();
		} catch (Exception e) {
			fail( e.getMessage());
		}
	}

	/**
	 * Test method for Basic Granular Content
	 * {@link org.iexhub.services.JaxRsContractRestProvider\#create(Patient patient, String theConditional)}
	 * 
	 * @author Ioana Singureanu
	 */
	@Test
	public void testCreateGranularConsent() {

		String currentTest = "GranularConsent";
		// Create a Privacy Consent as a Contract to be submitted as document
		// using ITI-41
		try {
			Logger logger = LoggerFactory.getLogger(ContractResourceTest.class);
			LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
			loggingInterceptor.setLogRequestSummary(true);
			loggingInterceptor.setLogRequestBody(true);
			loggingInterceptor.setLogger(logger);

			Contract contract = createBasicTestConsent();
			// add granular preferences
			String includedDataListId = "includedListOfDataTypes";
			ListResource list = new ListResource();
			list.setId(new IdDt(includedDataListId));
			list.setTitle("List of included data types");
			//specifies how the list items are to be used 
			CodeableConceptDt includeCodeValue = new CodeableConceptDt("http://hl7.org/fhir/v3/SetOperator", "I");
			includeCodeValue.setText("Include");
			list.setCode(includeCodeValue);
			list.setStatus(ListStatusEnum.CURRENT);
			list.setMode(ListModeEnum.SNAPSHOT_LIST);

			// add discharge summary document type
			ListResource.Entry dischargeSummaryEntry = new ListResource.Entry();
			//use list item flag to specify a category and the item to specify an instance (e.g. DocumentReference)
			CodeableConceptDt dischargeSummaryCode = new CodeableConceptDt("urn:oid:2.16.840.1.113883.6.1", "18842-5");
			//dischargeSummaryCode
			dischargeSummaryCode.setText("Discharge Summary");
			dischargeSummaryEntry.setFlag(dischargeSummaryCode);
			Basic basicItem1 = new Basic();
			basicItem1.setId(new IdDt("item1"));
			basicItem1.setCode(dischargeSummaryCode);
			
			ResourceReferenceDt itemReference1  = new ResourceReferenceDt("#item1");
			dischargeSummaryEntry.setItem(itemReference1);
			list.addEntry(dischargeSummaryEntry);
			// add a summary note document type
			ListResource.Entry summaryNoteEntry= new ListResource.Entry();
			// "34133-9" LOINC term is currently used as the Clinical Document
			// code for both the Care Record Summary (CRS) and Continuity of Care Document (CCD).
			CodeableConceptDt summaryNoteCode = new CodeableConceptDt("urn:oid:2.16.840.1.113883.6.1", "34133-9");
			summaryNoteCode.setText("Summarization of Episode Note");
			summaryNoteEntry.setFlag(summaryNoteCode);
			summaryNoteEntry.setDeleted(false);		
			Basic basicItem2 = new Basic();
			basicItem2.setId("item2");
			basicItem2.setCode(summaryNoteCode);			
			ResourceReferenceDt itemReference2  = new ResourceReferenceDt("#item2");
			itemReference2.setDisplay("referenced document type or instance");
			summaryNoteEntry.setItem(itemReference2);
			list.addEntry(summaryNoteEntry);
			
			// substance abuse category may reference a single category code or several diagnosis codes
			ListResource.Entry substanceAbuseRelatedEntry = new ListResource.Entry();
			CodeableConceptDt substanceAbuseRelatedCode = new CodeableConceptDt("urn:oid:2.16.840.1.113883.5.25", "ETH");
			substanceAbuseRelatedCode.setText("Substance Abuse Related Data");
			substanceAbuseRelatedEntry.setFlag(substanceAbuseRelatedCode);
			Basic basicItem3 = new Basic();
			basicItem3.setId("item3");
			basicItem3.setCode(substanceAbuseRelatedCode);
			
			ResourceReferenceDt itemReference3  = new ResourceReferenceDt("#item3");
			substanceAbuseRelatedEntry.setItem(itemReference3);
			list.addEntry(substanceAbuseRelatedEntry);
			// add list to contract
			contract.getTerm().get(0).getSubject().setReference("#" + includedDataListId);
			contract.getContained().getContainedResources().add(list);
			//add items as Basic resources
			contract.getContained().getContainedResources().add(basicItem1);
			contract.getContained().getContainedResources().add(basicItem2);
			contract.getContained().getContainedResources().add(basicItem3);
			// Use the narrative generator
			// @TODO: add generator Thymeleaf templates
			// ctxt.setNarrativeGenerator(new  DefaultThymeleafNarrativeGenerator());
			// Create XML and JSON files including generated narrative XHTML
			String xmlEncodedGranularConsent = ctxt.newXmlParser().setPrettyPrint(true)
					.encodeResourceToString(contract);
			FileUtils.writeStringToFile(new File(testResourcesPath+"/XML/"+currentTest+".xml"), xmlEncodedGranularConsent);
			String jsonEncodedGranularConsent = ctxt.newJsonParser().setPrettyPrint(true)
					.encodeResourceToString(contract);
			FileUtils.writeStringToFile(new File(testResourcesPath+"/JSON/"+currentTest+".json"), jsonEncodedGranularConsent);

			// Create FHIR client
			 IGenericClient client =  ctxt.newRestfulGenericClient(serverBaseUrl);
			 client.registerInterceptor(loggingInterceptor);
			//  Invoke service
			MethodOutcome outcome =  client.create().resource(contract).execute();
		} catch (Exception e) {
			fail( e.getMessage());
		}
	}

	/**
	 * createBasicTestConsent()
	 * 
	 * @return Contract containing a basic consent
	 */
	private Contract createBasicTestConsent()
	{
		return createBasicTestConsent(null);
	}
	
	/**
	 * createBasicTestConsent(UUID identifier)
	 * 
	 * @return Contract containing a basic consent
	 */
	private Contract createBasicTestConsent(UUID identifier)
	{
		Contract contract = new Contract();
		contract.getId().setValueAsString((identifier != null) ? identifier.toString()
				: null);
		DateTime testDocId = DateTime.now(DateTimeZone.UTC);
		contract.getIdentifier().setSystem(uriPrefix + iExHubDomainOid)
				.setValue("2.25." + Long.toString(testDocId.getMillis()));
		contract.getType().setValueAsEnum(ContractTypeCodesEnum.DISCLOSURE);
		contract.getActionReason().add(new CodeableConceptDt("http://hl7.org/fhir/contractsubtypecodes", "TREAT"));
		DateTimeDt issuedDateTime = new DateTimeDt();
		issuedDateTime.setValue(Calendar.getInstance().getTime());
		contract.setIssued(issuedDateTime);
		// specify the covered entity authorized to disclose
		// add source resource and authority reference
		contract.getContained().getContainedResources().add(sourceOrganizationResource);
		contract.addAuthority().setReference("#" + sourceOrganizationId);
		//This is required if the organization was not already added as a "contained" resource reference by the Patient
		//contract.getContained().getContainedResources().add(sourceOrganizationResource);
		// specify the provider who authored the data
		contract.addActor().getEntity().setReference("#" + sourcePractitionerId);
		contract.getContained().getContainedResources().add(sourcePractitionerResource);
		// specify the patient identified in the consent
		// add local reference to patient
		ResourceReferenceDt patientReference = new ResourceReferenceDt("#" + patientId);
		contract.getSubject().add(patientReference);
		contract.getSignerFirstRep().setType(new CodingDt("http://hl7.org/fhir/contractsignertypecodes","1.2.840.10065.1.12.1.7"));
		contract.getSignerFirstRep().setSignature(testPatientResource.getNameFirstRep().getNameAsSingleString());
		contract.getSignerFirstRep().setParty(patientReference);
		//add test patient as a contained resource rather than external reference
		contract.getContained().getContainedResources().add(testPatientResource);
		// set terms of consent and intended recipient(s)
		PeriodDt applicablePeriod = new PeriodDt();
		applicablePeriod.setEnd(new DateTimeDt("2016-10-10"));
		applicablePeriod.setStart(new DateTimeDt("2015-10-10"));
		contract.getTermFirstRep().setApplies(applicablePeriod);
		// list all recipients
		contract.getTermFirstRep().addActor().getEntity().setReference("#" + recipientPractitionerId);
		contract.getContained().getContainedResources().add(recipientPractitionerResource);
		contract.getTermFirstRep().setText("description of the consent terms");
		return contract;
	}
}
