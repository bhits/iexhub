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
 * FHIR Consent Resource Test
 * 
 */
package org.iexhub.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.PreferReturnEnum;
import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.Basic;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.iexhub.exceptions.UnexpectedServerException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConsentDstu3Test {
	private static String testResourcesPath = "C:/intellij-workspaces/c2s-ws/iexhub-fork/iexhub/src/test/resources"; //"src/test/resources/"
	private static String propertiesFile = "c:/temp/IExHub.properties"; //testResourcesPath+"/properties/IExHub.properties";
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
	private static Organization.OrganizationContactComponent organizationContactResource = new Organization.OrganizationContactComponent();
	private static Practitioner sourcePractitionerResource = new Practitioner();
	private static Practitioner recipientPractitionerResource = new Practitioner();
	// FHIR resource identifiers for inline/embedded objects
	private static String consentId = "consentId";
	private static String defaultPatientId = "32eef402464f475";
	private static String sourceOrganizationId = "sourceOrgOID";
	private static String sourcePractitionerId = "sourcePractitionerNPI";
	private static String recipientPractitionerId = "recipientPractitionerNPI";
	//FHIr context singleton
	private static FhirContext ctxt = FhirContext.forDstu3();
	static {
		try {
			properties.load(new FileInputStream(propertiesFile));
			ConsentDstu3Test.iExHubDomainOid = (properties.getProperty("IExHubDomainOID") == null)
					? ConsentDstu3Test.iExHubDomainOid : properties.getProperty("IExHubDomainOID");
			ConsentDstu3Test.iExHubAssigningAuthority = (properties.getProperty("IExHubAssigningAuthority") == null)
					? ConsentDstu3Test.iExHubAssigningAuthority
					: properties.getProperty("IExHubAssigningAuthority");
			ConsentDstu3Test.fhirClientSocketTimeout = (properties
					.getProperty("FHIRClientSocketTimeoutInMs") == null) ? ConsentDstu3Test.fhirClientSocketTimeout
							: Integer.parseInt(properties.getProperty("FHIRClientSocketTimeoutInMs"));
			ConsentDstu3Test.iExHubDomainOid = (properties.getProperty("IExHubDomainOID") == null) ? ConsentDstu3Test.iExHubDomainOid
					: properties.getProperty("IExHubDomainOID");
		} catch (IOException e) {
			throw new UnexpectedServerException(
					"Error encountered loading properties file, " + propertiesFile + ", " + e.getMessage());
		}

		ctxt.getRestfulClientFactory().setSocketTimeout(fhirClientSocketTimeout);

		// create the testPatient resource to be embedded into a contract
		testPatientResource.setId(new IdType(defaultPatientId));
		testPatientResource.addName().setFamily("Patient Family Name").addGiven("Patient Given Name");
		// set SSN value using coding system 2.16.840.1.113883.4.1
//		testPatientResource.addIdentifier().setSystem(uriPrefix + "2.16.840.1.113883.4.1").setValue("123-45-6789");
//		testPatientResource.addIdentifier().setSystem(uriPrefix + "1.3.6.1.4.1.21367.2005.13.20.1000").setValue(patientId);
		// set local patient id
		testPatientResource.addIdentifier().setSystem(uriPrefix + iExHubDomainOid).setValue(defaultPatientId);
		testPatientResource.setGender(Enumerations.AdministrativeGender.FEMALE);
		testPatientResource.setBirthDate(new DateType("1966-10-22").toCalendar().getTime());
		testPatientResource.addAddress().addLine("Patient Address Line").setCity("City").setState("NY")
				.setPostalCode("12345").setType(Address.AddressType.POSTAL);
		testPatientResource.addTelecom().setUse(ContactPoint.ContactPointUse.HOME)
		.setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("555-1212");
		testPatientResource.addTelecom().setUse(ContactPoint.ContactPointUse.HOME)
		.setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue("patient@home.org");
		// Provider organization...
		// set id to be used to reference the providerOrganization as an inline
		// resource
		sourceOrganizationResource.setId(new IdType(sourceOrganizationId));
		sourceOrganizationResource.addIdentifier().setSystem(uriPrefix + "2.16.840.1.113883.4.6").setValue("NPI of source organization");
		sourceOrganizationResource.setName("Provider Organization Name");
		sourceOrganizationResource.addAddress().addLine("1 Main Street").setCity("Cupertino").setState("CA")
				.setPostalCode("95014");
		// contact
		organizationContactResource.addTelecom().setUse(ContactPoint.ContactPointUse.WORK)
		.setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("408-555-1212");
		organizationContactResource.addTelecom().setUse(ContactPoint.ContactPointUse.WORK)
		.setSystem(ContactPoint.ContactPointSystem.EMAIL)
		.setValue("contact@sourceorgnization.org");
		HumanName contactName = new HumanName();
		contactName.setFamily("Contact Family Name");
		contactName.addGiven("Contact Given Name");
		organizationContactResource.setName(contactName);
		List<Organization.OrganizationContactComponent> contacts = new ArrayList<Organization.OrganizationContactComponent>();
		contacts.add(organizationContactResource);
		sourceOrganizationResource.setContact(contacts);

		// authoring practitioner
		sourcePractitionerResource.setId(new IdType(sourcePractitionerId));
		sourcePractitionerResource.addIdentifier().setSystem(uriPrefix + "2.16.840.1.113883.4.6").setValue("NPI");
		sourcePractitionerResource.addName(new HumanName().setFamily("Source Practitioner Last Name")
				.addGiven("Source Practitioner Given Name").addSuffix("MD"));
		sourcePractitionerResource.addAddress().addLine("Source Practitioner Address Line").setCity("City")
				.setState("NY").setPostalCode("98765");
		sourcePractitionerResource.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("212-555-1212");
		sourcePractitionerResource.addTelecom().setUse(ContactPoint.ContactPointUse.WORK)
		.setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue("contact@sourceorgnization.org");
		// recipient practitioner
		recipientPractitionerResource.setId(new IdType(recipientPractitionerId));
		recipientPractitionerResource.addIdentifier().setSystem(uriPrefix + "2.16.840.1.113883.4.6").setValue("NPI");
		recipientPractitionerResource.addName(new HumanName().setFamily("Recipient Practitioner Last Name")
				.addGiven("Recipient Practitioner Given Name").addSuffix("MD").addPrefix("Ms."));
		recipientPractitionerResource.addAddress().addLine("Recipient Practitioner Address Line").setCity("City")
				.setState("NY").setPostalCode("98765");
		recipientPractitionerResource.addTelecom().setUse(ContactPoint.ContactPointUse.WORK)
		.setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("212-000-1212");
		recipientPractitionerResource.addTelecom().setUse(ContactPoint.ContactPointUse.WORK)
		.setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue("recipient@destination.org");
	}

	/**
	 * Test method for general Consent resource workflow
	 */
	@Test
	public void testConsentWorkflow()
	{
		// Assumes that user ID is known (i.e., patient ID feed has been provided by NIST for use with their test server at
		//   http://ihexds.nist.gov:12090/xdstools/pidallocate).  Use the assigning authority "1.3.6.1.4.1.21367.2005.13.20.1000&ISO"
		//   shown on the page (typically the first button).
		//
		// Specify that patient ID in the "defaultPatientId" static variable above prior to running this test.
		String currentTest = "ConsentWorkflow";
		Logger logger = LoggerFactory.getLogger(ConsentDstu3Test.class);
		LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
		loggingInterceptor.setLogRequestSummary(true);
		loggingInterceptor.setLogRequestBody(true);
		loggingInterceptor.setLogger(logger);

		// create FHIR client
		IGenericClient client = ctxt.newRestfulGenericClient(serverBaseUrl /*"http://fhirtest.uhn.ca/baseDstu2"*/);
		client.registerInterceptor(loggingInterceptor);

		// Create a Consent for the user...
		MethodOutcome createMethodOutcome = null;
		try
		{
			Consent consent = createBasicTestConsent();

			String xmlEncodedGranularConsent = ctxt.newXmlParser().setPrettyPrint(true)
					.encodeResourceToString(consent);
			FileUtils.writeStringToFile(new File(testResourcesPath+"/XML/"+currentTest+".xml"), xmlEncodedGranularConsent);
			String jsonEncodedGranularConsent = ctxt.newJsonParser().setPrettyPrint(true)
					.encodeResourceToString(consent);
			FileUtils.writeStringToFile(new File(testResourcesPath+"/JSON/"+currentTest+".json"), jsonEncodedGranularConsent);
			
			//  invoke Consent service
			createMethodOutcome = client.create().resource(consent).prefer(PreferReturnEnum.REPRESENTATION).execute();
		}
		catch (Exception e)
		{
			fail( e.getMessage());
		}

		// Now search for the contract to ensure it was stored...
		Bundle response = null;
		List<Consent> retrievedConsent = null;
		try
		{
			Identifier searchParam = new Identifier();
			searchParam.setSystem(iExHubDomainOid).setValue(iExHubDomainOid);
			response = client
					.search()
					.forResource(Consent.class)
					.where(Patient.IDENTIFIER.exactly().identifier(searchParam.getId()))
					.returnBundle(Bundle.class).execute();
			//TODO: How to set retrievedConsent ???

			assertTrue("Error - unexpected return value for testSearchConsent",
					((response != null) && (retrievedConsent.size() == 1)));
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}

		// Now update the consent.  Note that the document entry UUID (i.e., Contract resource ID) should not be modified by the client as this is required by the
		//   XDS.b document repository to establish the association between the old and new consent.  However, a new document unique ID (i.e., Contract resource
		//   identifier) must be stored in the retrieved Contract because the NIST test server (and likely other implementations) requires that the replacement
		//   document have a different unique ID.  This is shown below...
		MethodOutcome updateMethodOutcome = null;
		try
		{
			// Change document unique ID.  For this example, a timestamp is used to generate one portion of the identifier value.  The document repository will set the status of the
			//   old document being replaced to "Deprecated".
			retrievedConsent.get(0).getIdentifier().setSystem(uriPrefix + iExHubDomainOid).setValue("2.25." + Long.toString(DateTime.now(DateTimeZone.UTC).getMillis()));

            // TODO - This is where you would make changes to the consent...
			
			updateMethodOutcome = client.update().resource(retrievedConsent.get(0)).prefer(PreferReturnEnum.REPRESENTATION).execute();
			assertTrue("Update failed",
					updateMethodOutcome.getCreated());
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}

		// Alternative way of looking for a Consent resource - FHIR Find using the document unique ID which is the Consent resource identifier...
		try
		{
			Consent findVal = client.read(Consent.class,
					((Consent)updateMethodOutcome.getResource()).getIdentifier().getValue());
			
			assertTrue("Error - unexpected return value for testFindConsent",
					findVal != null);
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}
	
	/**
	 * Test method for
	 * {@link org.iexhub.services.JaxRsConsentRestProvider\#find(@IdParam final
	 * IdDt id)}.
	 */
	@Test
	public void testFindConsent() {

		try {
			Logger logger = LoggerFactory.getLogger(ConsentDstu3Test.class);
			LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
			loggingInterceptor.setLogRequestSummary(true);
			loggingInterceptor.setLogRequestBody(true);
			loggingInterceptor.setLogger(logger);

			IGenericClient client = ctxt.newRestfulGenericClient(serverBaseUrl);
			client.registerInterceptor(loggingInterceptor); // Required only for
															// logging
			Consent retVal = client.read(Consent.class,
					/*iExHubDomainOid + "." + consentId*/ /*"2.25.1469220780502"*/ "2.25.1471531116858");
			
			assertTrue("Error - unexpected return value for testFindConsent", retVal != null);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Test method for
	 * {@link org.iexhub.services.JaxRsConsentRestProvider#search\(@IdParam
	 * final IdDt id)}.
	 */
	@Test
	public void testSearchConsent() {

		try {
			Logger logger = LoggerFactory.getLogger(ConsentDstu3Test.class);
			LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
			loggingInterceptor.setLogRequestSummary(true);
			loggingInterceptor.setLogRequestBody(true);
			loggingInterceptor.setLogger(logger);

			IGenericClient client = ctxt.newRestfulGenericClient(serverBaseUrl);
			client.registerInterceptor(loggingInterceptor);

			Identifier searchParam = new Identifier();
			searchParam.setSystem(iExHubDomainOid).setValue(defaultPatientId);
			Bundle response = client
					.search()
					.forResource(Consent.class)
					.where(Patient.IDENTIFIER.exactly().identifier(searchParam.getId()))
					.returnBundle(Bundle.class).execute();

			assertTrue("Error - unexpected return value for testSearchConsent", response != null);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Test method for Basic Consent Content
	 * {@link org.iexhub.services.JaxRsConsentRestProvider\#create(Patient patient, String theConditional)}
	 * 
	 * @author Ioana Singureanu
	 */
	@Test
	public void testCreateBasicConsent() {

		// Create a Privacy Consent as a Consent to be submitted as document
		// using ITI-41
		String currentTest = "BasicConsent";
		try {
			Logger logger = LoggerFactory.getLogger(ConsentDstu3Test.class);
			LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
			loggingInterceptor.setLogRequestSummary(true);
			loggingInterceptor.setLogRequestBody(true);
			loggingInterceptor.setLogger(logger);

			Consent consent = createBasicTestConsent();

			// Use the narrative generator
			// @TODO: add generator Thymeleaf templates
			// ctxt.setNarrativeGenerator(new  DefaultThymeleafNarrativeGenerator());

			String xmlEncodedGranularConsent = ctxt.newXmlParser().setPrettyPrint(true)
					.encodeResourceToString(consent);
			FileUtils.writeStringToFile(new File(testResourcesPath+"/XML/"+currentTest+".xml"), xmlEncodedGranularConsent);
			String jsonEncodedGranularConsent = ctxt.newJsonParser().setPrettyPrint(true)
					.encodeResourceToString(consent);
			FileUtils.writeStringToFile(new File(testResourcesPath+"/JSON/"+currentTest+".json"), jsonEncodedGranularConsent);

			// create FHIR client
			IGenericClient client = ctxt.newRestfulGenericClient(serverBaseUrl);
			client.registerInterceptor(loggingInterceptor);
			 
			//  invoke Consent service
			client.create().resource(consent).execute();
		} catch (Exception e) {
			fail( e.getMessage());
		}
	}

	/**
	 * Test method for Basic Consent Content update
	 * {@link org.iexhub.services.JaxRsConsentRestProvider\#create(Patient patient, String theConditional)}
	 * 
	 * @author A. Sute
	 */
	@Test
	public void testUpdateBasicConsent() {

		// Create a Privacy Consent as a Consent to be submitted as document
		// using ITI-41
		String currentTest = "BasicConsentUpdate";
		try {
			Logger logger = LoggerFactory.getLogger(ConsentDstu3Test.class);
			LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
			loggingInterceptor.setLogRequestSummary(true);
			loggingInterceptor.setLogRequestBody(true);
			loggingInterceptor.setLogger(logger);

			Consent consent = createBasicTestConsent(UUID.fromString("819efe60-d1bb-47b7-b5d6-ab5fa073eef0"));

			// Use the narrative generator
			// @TODO: add generator Thymeleaf templates
			// ctxt.setNarrativeGenerator(new  DefaultThymeleafNarrativeGenerator());

			String xmlEncodedGranularConsent = ctxt.newXmlParser().setPrettyPrint(true)
					.encodeResourceToString(consent);
			FileUtils.writeStringToFile(new File(testResourcesPath+"/XML/"+currentTest+".xml"), xmlEncodedGranularConsent);
			String jsonEncodedGranularConsent = ctxt.newJsonParser().setPrettyPrint(true)
					.encodeResourceToString(consent);
			FileUtils.writeStringToFile(new File(testResourcesPath+"/JSON/"+currentTest+".json"), jsonEncodedGranularConsent);

			// create FHIR client
			IGenericClient client = ctxt.newRestfulGenericClient(serverBaseUrl);
			client.registerInterceptor(loggingInterceptor);
			
			//  invoke Consent service
			client.update().resource(consent).execute();
		} catch (Exception e) {
			fail( e.getMessage());
		}
	}

	/**
	 * Test method for Basic Granular Content
	 * {@link org.iexhub.services.JaxRsConsentRestProvider\#create(Patient patient, String theConditional)}
	 * 
	 * @author Ioana Singureanu
	 */
	@Test
	public void testCreateGranularConsent() {

		String currentTest = "GranularConsent";
		// Create a Privacy Consent as a Consent to be submitted as document
		// using ITI-41
		try {
			Logger logger = LoggerFactory.getLogger(ConsentDstu3Test.class);
			LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
			loggingInterceptor.setLogRequestSummary(true);
			loggingInterceptor.setLogRequestBody(true);
			loggingInterceptor.setLogger(logger);

			Consent consent = createBasicTestConsent();
			// add granular preferences
			String includedDataListId = "includedListOfDataTypes";
			ListResource list = new ListResource();
			list.setId(new IdType(includedDataListId));
			list.setTitle("List of included data types");

			//specifies how the list items are to be used
			CodeableConcept includeCodeValue = new CodeableConcept();
			includeCodeValue.addCoding(new Coding()
					.setSystem("http://hl7.org/fhir/v3/SetOperator")
					.setCode("I")
					.setDisplay("Include"));
			list.setCode(includeCodeValue);
			list.setStatus(ListResource.ListStatus.CURRENT);
			list.setMode(ListResource.ListMode.SNAPSHOT);

			// add discharge summary document type
			ListResource.ListEntryComponent dischargeSummaryEntry = new ListResource.ListEntryComponent();
			//use list item flag to specify a category and the item to specify an instance (e.g. DocumentReference)
			CodeableConcept dischargeSummaryCode = new CodeableConcept();
			dischargeSummaryCode.addCoding(new Coding()
					.setSystem("urn:oid:2.16.840.1.113883.6.1")
					.setCode("18842-5")
					.setDisplay("Discharge Summary"));
					dischargeSummaryEntry.setFlag(dischargeSummaryCode);

			Basic basicItem1 = new Basic();
			basicItem1.setId(new IdType("item1"));
			basicItem1.setCode(dischargeSummaryCode);
			
			Reference itemReference1  = new Reference("#item1");
			dischargeSummaryEntry.setItem(itemReference1);
			list.addEntry(dischargeSummaryEntry);
			// add a summary note document type
			ListResource.ListEntryComponent summaryNoteEntry= new ListResource.ListEntryComponent();
			// "34133-9" LOINC term is currently used as the Clinical Document
			// code for both the Care Record Summary (CRS) and Continuity of Care Document (CCD).
			CodeableConcept summaryNoteCode = new CodeableConcept();
			summaryNoteCode.addCoding(new Coding()
					.setSystem("urn:oid:2.16.840.1.113883.6.1")
					.setCode("34133-9")
					.setDisplay("Summarization of Episode Note"));
			summaryNoteEntry.setFlag(summaryNoteCode);
			summaryNoteEntry.setDeleted(false);

			Basic basicItem2 = new Basic();
			basicItem2.setId("item2");
			basicItem2.setCode(summaryNoteCode);			
			Reference itemReference2  = new Reference("#item2");
			itemReference2.setDisplay("referenced document type or instance");
			summaryNoteEntry.setItem(itemReference2);
			list.addEntry(summaryNoteEntry);
			
			// substance abuse category may reference a single category code or several diagnosis codes
			ListResource.ListEntryComponent substanceAbuseRelatedEntry = new ListResource.ListEntryComponent();
			CodeableConcept substanceAbuseRelatedCode = new CodeableConcept();
			summaryNoteCode.addCoding(new Coding()
					.setSystem("urn:oid:2.16.840.1.113883.5.25")
					.setCode("ETH")
					.setDisplay("Substance Abuse Related Data"));
			substanceAbuseRelatedEntry.setFlag(substanceAbuseRelatedCode);

			Basic basicItem3 = new Basic();
			basicItem3.setId("item3");
			basicItem3.setCode(substanceAbuseRelatedCode);
			
			Reference itemReference3  = new Reference("#item3");
			substanceAbuseRelatedEntry.setItem(itemReference3);
			list.addEntry(substanceAbuseRelatedEntry);

			// add list to consent
			consent.getRecipientFirstRep().setReference("#" + includedDataListId);
			consent.getContained().add(list);
			//add items as Basic resources
			consent.getContained().add(basicItem1);
			consent.getContained().add(basicItem2);
			consent.getContained().add(basicItem3);
			// Use the narrative generator
			// @TODO: add generator Thymeleaf templates
			// ctxt.setNarrativeGenerator(new  DefaultThymeleafNarrativeGenerator());
			// Create XML and JSON files including generated narrative XHTML
			String xmlEncodedGranularConsent = ctxt.newXmlParser().setPrettyPrint(true)
					.encodeResourceToString(consent);
			FileUtils.writeStringToFile(new File(testResourcesPath+"/XML/"+currentTest+".xml"), xmlEncodedGranularConsent);
			String jsonEncodedGranularConsent = ctxt.newJsonParser().setPrettyPrint(true)
					.encodeResourceToString(consent);
			FileUtils.writeStringToFile(new File(testResourcesPath+"/JSON/"+currentTest+".json"), jsonEncodedGranularConsent);

			// Create FHIR client
			 IGenericClient client =  ctxt.newRestfulGenericClient(serverBaseUrl);
			 client.registerInterceptor(loggingInterceptor);
			//  Invoke service
			MethodOutcome outcome =  client.create().resource(consent).execute();
		} catch (Exception e) {
			fail( e.getMessage());
		}
	}

	/**
	 * createBasicTestConsent()
	 * 
	 * @return Consent containing a basic consent
	 */
	private Consent createBasicTestConsent()
	{
		return createBasicTestConsent(null);
	}

	/**
	 * createBasicTestConsent(UUID identifier)
	 * 
	 * @return Consent containing a basic consent
	 */
	private Consent createBasicTestConsent(UUID identifier)
	{
		Consent consent = new Consent();
		consent.setId((identifier != null) ? identifier.toString()
				: null);
		DateTime testDocId = DateTime.now(DateTimeZone.UTC);
		consent.getIdentifier().setSystem(uriPrefix + iExHubDomainOid)
				.setValue("2.25." + Long.toString(testDocId.getMillis()));
		consent.getCategoryFirstRep().addCoding().setSystem("urn:oid:2.16.840.1.113883.5.4").setCode("IDSCL");
		consent.addPurpose(new Coding()
				.setSystem("http://hl7.org/fhir/contractsubtypecodes")
				.setCode("TREAT")
				.setDisplay("Consent for TREAT"));
		consent.setDateTime(new Date());

		// specify the covered entity authorized to disclose
		// add source resource and authority reference
		consent.getContained().add(sourceOrganizationResource);
		consent.addRecipient(new Reference().setReference("#" + sourceOrganizationId));
		//This is required if the organization was not already added as a "contained" resource reference by the Patient
		//consent.getContained().getContainedResources().add(sourceOrganizationResource);
		// specify the provider who authored the data
		consent.addRecipient(new Reference().setReference("#" + sourcePractitionerId));
		consent.getContained().add(sourcePractitionerResource);
		// specify the patient identified in the consent
		/*
		consent.getSignerFirstRep().setType(new CodingDt("http://hl7.org/fhir/contractsignertypecodes","1.2.840.10065.1.12.1.7"));
		consent.getSignerFirstRep().setParty(patientReference);
		*/
		Reference consentSignature = new Reference();
		consentSignature.setDisplay(testPatientResource.getNameFirstRep().getNameAsSingleString());
		consentSignature.setReference("#" + testPatientResource.getId());
		consent.getConsentor().add(consentSignature);
		//add test patient as a contained resource rather than external reference
		consent.getContained().add(testPatientResource);
		// set terms of consent and intended recipient(s)
		consent.getPeriod().setStart(new DateType("2015-10-10").toCalendar().getTime());
		consent.getPeriod().setEnd(new DateType("2016-10-10").toCalendar().getTime());

		// list all recipients
		consent.getRecipient().add(new Reference().setReference("#" + recipientPractitionerId));
		consent.getContained().add(recipientPractitionerResource);
		consent.getConsentorFirstRep().setDisplay("description of the consent terms");

		return consent;
	}
}
