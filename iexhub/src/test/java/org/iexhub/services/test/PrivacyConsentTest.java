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
package org.iexhub.services.test;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.primitive.IdDt;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.Basic;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Consent;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class PrivacyConsentTest {
	// FHIR resource identifiers for contained objects
	private static String consentId = "consentId";
	private static String patientId = "patientId";
	private static String sourceOrganizationId = "sourceOrgOID";
	private static String sourcePractitionerId = "sourcePractitionerNPI";
	private static String recipientPractitionerId = "recipientPractitionerNPI";
	private static String testResourcesPath = "src/test/resources/";
	private static FhirContext ctxt = FhirContext.forDstu3();
	private static Consent consent;

	private static String uriPrefix = "urn:oid:";
	private static String iExHubDomainOid = "2.16.840.1.113883.3.72.5.9.1";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		// Consent as contract
		consent = new Consent();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws IOException {
		String currentTest = "PrivacyConsentTest";
		// FHIR objects used to create a Consent
		Patient testPatientResource = new Patient();
		Organization sourceOrganizationResource = new Organization();
		Organization.OrganizationContactComponent organizationContactComponent = new Organization.OrganizationContactComponent();
		Practitioner sourcePractitionerResource = new Practitioner();
		Practitioner recipientPractitionerResource = new Practitioner();

		// create the testPatient resource to be embedded into a contract
		testPatientResource.setId(new IdType(patientId));
		testPatientResource.addName().setFamily("Patient Family Name").addGiven("Patient Given Name");
		// set SSN value using coding system 2.16.840.1.113883.4.1
		testPatientResource.addIdentifier().setSystem(uriPrefix + "2.16.840.1.113883.4.1").setValue("123-45-6789");
		// set local patient id
		testPatientResource.addIdentifier().setSystem(uriPrefix + iExHubDomainOid).setValue(patientId);
		testPatientResource.setGender(Enumerations.AdministrativeGender.FEMALE);
		testPatientResource.setBirthDate(new DateType("1966-10-22").toCalendar().getTime());
		testPatientResource.addAddress().addLine("Patient Address Line").setCity("City").setState("NY")
				.setPostalCode("12345").setType(Address.AddressType.POSTAL);
		testPatientResource.addTelecom().setUse(ContactPoint.ContactPointUse.HOME).setSystem(ContactPoint.ContactPointSystem.PHONE)
				.setValue("555-1212");
		testPatientResource.addTelecom().setUse(ContactPoint.ContactPointUse.HOME).setSystem(ContactPoint.ContactPointSystem.EMAIL)
				.setValue("patient@home.org");
		// Provider organization...
		// set id to be used to reference the providerOrganization as an inline resource
		sourceOrganizationResource.setId(new IdType(sourceOrganizationId));
		// system 2.16.840.1.113883.4.6
		sourceOrganizationResource.addIdentifier().setSystem(uriPrefix + "2.16.840.1.113883.4.6")
				.setValue("NPI of source organization");
		sourceOrganizationResource.setName("Source Organization Name");
		sourceOrganizationResource.addAddress().addLine("1 Source Drive").setCity("Source City").setState("NY")
				.setPostalCode("01221");
		// contact
		organizationContactComponent.addTelecom().setUse(ContactPoint.ContactPointUse.WORK)
				.setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("408-555-1212");
		organizationContactComponent.addTelecom().setUse(ContactPoint.ContactPointUse.WORK)
				.setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue("contact@sourceorgnization.org");
		HumanName contactName = new HumanName();
		contactName.setFamily("Contact Family Name");
		contactName.addGiven("Contact Given Name");
		organizationContactComponent.setName(contactName);

		List<Organization.OrganizationContactComponent> contacts = new ArrayList<Organization.OrganizationContactComponent>();
		contacts.add(organizationContactComponent);
		sourceOrganizationResource.setContact(contacts);
		// set reference using "#" prefix
		//testPatientResource.addCareProvider().setReference("#" + sourceOrganizationId);

		// authoring practitioner
		sourcePractitionerResource.setId(new IdType(sourcePractitionerId));
		sourcePractitionerResource.addIdentifier().setSystem(uriPrefix + "2.16.840.1.113883.4.6")
				.setValue("NPI of source provider");
		sourcePractitionerResource.addName(new HumanName().setFamily("Source Practitioner Last Name")
				.addGiven("Source Practitioner Given Name").addSuffix("MD"));
		sourcePractitionerResource.addAddress().addLine("Source Practitioner Address Line").setCity("City")
				.setState("NY").setPostalCode("98765");
		sourcePractitionerResource.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("212-555-1212");
		sourcePractitionerResource.addTelecom().setUse(ContactPoint.ContactPointUse.WORK).setSystem(ContactPoint.ContactPointSystem.EMAIL)
				.setValue("contact@sourceorgnization.org");
		// recipient practitioner
		recipientPractitionerResource.setId(new IdType(recipientPractitionerId));
		recipientPractitionerResource.addIdentifier().setSystem(uriPrefix + "2.16.840.1.113883.4.6")
				.setValue("NPI of recipient provider");
		recipientPractitionerResource.addName(new HumanName().setFamily("Recipient Practitioner Last Name")
				.addGiven("Recipient Practitioner Given Name").addSuffix("MD").addPrefix("Ms."));
		recipientPractitionerResource.addAddress().addLine("Recipient Practitioner Address Line").setCity("City")
				.setState("NY").setPostalCode("98765");
		recipientPractitionerResource.addTelecom().setUse(ContactPoint.ContactPointUse.WORK)
				.setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("212-000-1212");
		recipientPractitionerResource.addTelecom().setUse(ContactPoint.ContactPointUse.WORK)
				.setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue("recipient@destination.org");
		// populate contract
		// set the id as a concatenated "OID.consentId"
		consent.setId(new IdType(iExHubDomainOid + "." + consentId));
		consent.getIdentifier().setSystem(uriPrefix + iExHubDomainOid).setValue("consent GUID");
		consent.getCategoryFirstRep().addCoding().setSystem("http://hl7.org/fhir/contractsubtypecodes").setCode("TREAT");
		//TODO: Where to set ContractTypeCodesEnum.DISCLOSURE?
		consent.setDateTime(new Date());

		// specify the covered entity authorized to disclose
		// add source resource and authority reference
		consent.getContained().add(sourceOrganizationResource);
		consent.addRecipient(new Reference().setReference("#" + sourceOrganizationId));
		consent.getContained().add(sourcePractitionerResource);
		// specify the patient identified in the consent
		// add local reference to patient
		consent.getPatient().setReference("#" + patientId);
		consent.getContained().add(testPatientResource);

		// Consent signature details
		Reference consentSignature = new Reference();
		consentSignature.setDisplay(testPatientResource.getNameFirstRep().getNameAsSingleString());
		consentSignature.setReference("#" + testPatientResource.getId());
		/*
		consent.getSignerFirstRep()
				.setType(new CodingDt("http://hl7.org/fhir/contractsignertypecodes", "1.2.840.10065.1.12.1.7"));

		consent.getSignerFirstRep().setParty(patientReference);
		*/
		consent.getConsentor().add(consentSignature);

		// set terms of consent and intended recipient(s)
		consent.getPeriod().setStart(new DateType("2015-10-10").toCalendar().getTime());
		consent.getPeriod().setEnd(new DateType("2016-05-10").toCalendar().getTime());

		// list all recipients
		consent.getRecipient().add(new Reference().setReference("#" + recipientPractitionerId));
		consent.getContained().add(recipientPractitionerResource);
		consent.getConsentorFirstRep().setDisplay("description of the consent terms");

		// add granular preferences
		String includedDataListId = "includedListOfDataTypes";
		ListResource list = new ListResource();
		list.setId(new IdType(includedDataListId));
		list.setTitle("List of included data types");

		// specifies how the list items are to be used
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
		// use list item flag to specify a category and the item to specify an instance (e.g. DocumentReference)
		CodeableConcept dischargeSummaryCode = new CodeableConcept();
		dischargeSummaryCode.addCoding(new Coding()
				.setSystem("urn:oid:2.16.840.1.113883.5.25")
				.setCode("SDV")
				.setDisplay("Sexual and domestic violence related"));

		Basic basicItem1 = new Basic();
		basicItem1.setId(new IdDt("item1"));
		basicItem1.setCode(dischargeSummaryCode);

		Reference itemReference1 = new Reference("#item1");
		dischargeSummaryEntry.setItem(itemReference1);
		list.addEntry(dischargeSummaryEntry);

		// add a summary note document type
		ListResource.ListEntryComponent summaryNoteEntry = new ListResource.ListEntryComponent();
		/* "34133-9" LOINC term is currently used as the Clinical Document code for both
		   the Care Record Summary (CRS) and Continuity of Care Document (CCD). */
		CodeableConcept summaryNoteCode = new CodeableConcept();
		summaryNoteCode.addCoding(new Coding()
				.setSystem("urn:oid:2.16.840.1.113883.5.25")
				.setCode("PSY")
				.setDisplay("Psychiatry Related Data"));
		summaryNoteEntry.setFlag(summaryNoteCode);
		summaryNoteEntry.setDeleted(false);

		Basic basicItem2 = new Basic();
		basicItem2.setId("item2");
		basicItem2.setCode(summaryNoteCode);
		Reference itemReference2 = new Reference("#item2");
		itemReference2.setDisplay("referenced document type or instance");
		summaryNoteEntry.setItem(itemReference2);
		list.addEntry(summaryNoteEntry);

		// substance abuse category may reference a single category code or several diagnosis codes
		ListResource.ListEntryComponent substanceAbuseRelatedEntry = new ListResource.ListEntryComponent();
		CodeableConcept substanceAbuseRelatedCode = new CodeableConcept();
		substanceAbuseRelatedCode.addCoding(new Coding()
				.setSystem("urn:oid:2.16.840.1.113883.5.25")
				.setCode("ETH")
				.setDisplay("Substance Abuse Related Data"));

		Basic basicItem3 = new Basic();
		basicItem3.setId("item3");
		basicItem3.setCode(substanceAbuseRelatedCode);

		Reference itemReference3 = new Reference("#item3");
		substanceAbuseRelatedEntry.setItem(itemReference3);
		list.addEntry(substanceAbuseRelatedEntry);

		// add list to contract
		consent.getRecipientFirstRep().setReference("#" + includedDataListId);
		consent.getContained().add(list);
		// add items as Basic resources
		consent.getContained().add(basicItem1);
		consent.getContained().add(basicItem2);
		consent.getContained().add(basicItem3);

		// Use the narrative generator
		// @TODO: add generator Thymeleaf templates
		// Create XML and JSON files including generated narrative XHTML
		String xmlEncodedGranularConsent = ctxt.newXmlParser().setPrettyPrint(true)
				.encodeResourceToString(consent);
		File xmlEncodedGranularConsentFile = new File(testResourcesPath + "/XML/temp/" + currentTest + ".xml");
		try {
			FileUtils.writeStringToFile(xmlEncodedGranularConsentFile, xmlEncodedGranularConsent);
		} catch (IOException e) {
			fail("Write Consent resource to XML:" + e.getMessage());
		}

		//Commenting out adding XACML to consent object

		// write Consent + XACML
		xmlEncodedGranularConsent = ctxt.newXmlParser().setPrettyPrint(true).encodeResourceToString(consent);
		try {
			FileUtils.writeStringToFile(xmlEncodedGranularConsentFile, xmlEncodedGranularConsent);
		} catch (IOException e) {
			fail("Write resource to XML + XACML:" + e.getMessage());
		}

		String jsonEncodedGranularConsent = ctxt.newJsonParser().setPrettyPrint(true)
				.encodeResourceToString(consent);

		try {
			FileUtils.writeStringToFile(new File(testResourcesPath + "/JSON/temp/" + currentTest + ".json"),
					jsonEncodedGranularConsent);
		} catch (IOException e) {
			fail("Write resource to JSON:" + e.getMessage());
		}
		Consent consentFromFile = null;
		String readContractString = "";
		try {
			readContractString = FileUtils
					.readFileToString(new File(testResourcesPath + "/XML/temp/" + currentTest + ".xml"), "UTF-8");
		} catch (IOException e) {
			fail("Reading resource from file error:" + e.getMessage());
		}
		consentFromFile = (Consent) ctxt.newXmlParser().parseResource(readContractString);
		if (consentFromFile.getPurpose().size() != 0) {
			// read purpose of use
			Iterator<Coding> purposeOfUseIterator = consentFromFile.getPurpose().iterator();
			while (purposeOfUseIterator.hasNext()) {
				Coding cd = purposeOfUseIterator.next();
				assertTrue(cd.getCode().equalsIgnoreCase("TREAT"));
			}
		}
		if (consentFromFile.getConsentor().size() != 0) {
			Reference consentSubjectRef = consentFromFile.getConsentor().get(0);
			IBaseResource referencedSubject = consentSubjectRef.getResource();
			String referencedId = referencedSubject.getIdElement().getIdPart();
			assertTrue(referencedId.equalsIgnoreCase(patientId));
			ListIterator<Resource> containedResources = consentFromFile.getContained()
					.listIterator();
			Patient subjectPatientResource = null;
			while (containedResources.hasNext()) {
				Resource cr = containedResources.next();
				if (cr.getIdElement().getIdPart().equalsIgnoreCase(referencedId)) {
					subjectPatientResource = (Patient) cr;
					HumanName n = subjectPatientResource.getName().get(0);
					assertTrue(n.getFamily().equalsIgnoreCase("Patient Family Name"));
					break;
				}
			}

		}

	}

}