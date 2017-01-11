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
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ResearchConsentTest {
    private Logger log = Logger.getLogger(this.getClass());
    // FHIR resource identifiers for contained objects
    private static String consentId = "consentId";
    private static String patientId = "patientId";
    private static String sourceOrganizationId = "researchOrgOID";
    private static String recipientResearcherId = "recipientResearcherId";
    private static String testResourcesPath = "src/test/resources/";
    private static FhirContext ctxt = FhirContext.forDstu3();

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
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() {
        String currentTest = "ResearchConsentTest";
        // FHIR objects used to create a Consent
        Patient testPatientResource = new Patient();
        Organization sourceOrganizationResource = new Organization();
        Organization.OrganizationContactComponent organizationContact = new Organization.OrganizationContactComponent();
        Practitioner recipientResearcherPractitionerResource = new Practitioner();
        // Consent as contract
        Consent consent = new Consent();
        // create the testPatient resource to be embedded into a contract
        Narrative patientNarrative = new Narrative();
        patientNarrative.setDivAsString("Sample patient demographics pertinent to consent");
        testPatientResource.setText(patientNarrative);
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
        sourceOrganizationResource.addIdentifier().setSystem(uriPrefix + "2.16.840.1.113883.4.6").setValue("NPI of source organization");
        sourceOrganizationResource.setName("Research Organization Name");
        sourceOrganizationResource.addAddress().addLine("1 Research Drive").setCity("Research City").setState("MA")
                .setPostalCode("01221");

        // contact
        organizationContact.addTelecom().setUse(ContactPoint.ContactPointUse.WORK)
                .setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("408-555-1212");
        organizationContact.addTelecom().setUse(ContactPoint.ContactPointUse.WORK)
                .setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue("contact@sourceorgnization.org");
        HumanName contactName = new HumanName();
        contactName.setFamily("Researcher Family Name");
        contactName.addGiven("Researcher Given Name");
        organizationContact.setName(contactName);
        List<Organization.OrganizationContactComponent> contacts = new ArrayList<Organization.OrganizationContactComponent>();
        contacts.add(organizationContact);
        sourceOrganizationResource.setContact(contacts);

        // recipient practitioner
        recipientResearcherPractitionerResource.setId(new IdType(recipientResearcherId));
        recipientResearcherPractitionerResource.addIdentifier().setSystem(uriPrefix + "2.16.840.1.113883.4.6").setValue("NPI of recipient provider");
        recipientResearcherPractitionerResource.addName(new HumanName().setFamily("Recipient Practitioner Last Name").addGiven("Recipient Practitioner Given Name").addSuffix("MD").addPrefix("Ms."));
        recipientResearcherPractitionerResource.addAddress().addLine("Recipient Practitioner Address Line").setCity("City")
                .setState("NY").setPostalCode("98765");
        recipientResearcherPractitionerResource.addTelecom().setUse(ContactPoint.ContactPointUse.WORK)
                .setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("212-000-1212");
        recipientResearcherPractitionerResource.addTelecom().setUse(ContactPoint.ContactPointUse.WORK)
                .setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue("recipient@destination.org");
        // populate contract
        // set the id as a concatenated "OID.consentId"
        consent.setId(new IdType(iExHubDomainOid + "." + consentId));
        consent.getIdentifier().setSystem(uriPrefix + iExHubDomainOid)
                .setValue("consent GUID");
        consent.getCategoryFirstRep().addCoding().setSystem("urn:oid:2.16.840.1.113883.5.4").setCode("IDSCL");
        //TODO: Where to set ContractTypeCodesEnum.DISCLOSURE?
        //research
         consent.addPurpose(new Coding()
                .setSystem("http://hl7.org/fhir/contractsubtypecodes")
                .setCode("HRESCH")
                .setDisplay("Consent for research"));
        consent.setDateTime(new Date());

        // specify the covered entity authorized to disclose
        // add source resource and authority reference
        consent.getContained().add(sourceOrganizationResource);
        consent.addRecipient(new Reference().setReference("#" + sourceOrganizationId));

        // specify the patient identified in the consent
        // add local reference to patient
        consent.getPatient().setReference("#" + patientId).setDisplay("Research Participant");
        consent.getContained().add(testPatientResource);

        // Consent signature details
        Reference consentSignature = new Reference();
        consentSignature.setDisplay(testPatientResource.getNameFirstRep().getNameAsSingleString());
        consentSignature.setReference("#" + testPatientResource.getId());
          /*
		consent.getConsentorFirstRep().
				.setType(new Coding("http://hl7.org/fhir/contractsignertypecodes", "1.2.840.10065.1.12.1.7", ""));
		consent.getSignerFirstRep().setParty(patientReference);
		*/
        consent.getConsentor().add(consentSignature);

        consent.getPeriod().setStart(new DateType("2016-05-10").toCalendar().getTime());
        consent.getPeriod().setEnd(new DateType("2016-05-10").toCalendar().getTime());

        // list all recipients
        consent.getRecipient().add(new Reference().setReference("#" + recipientResearcherId));
        consent.getContained().add(recipientResearcherPractitionerResource);
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
        ListResource.ListEntryComponent researchStudyEntry = new ListResource.ListEntryComponent();
        // use list item flag to specify a category and the item to specify an instance (e.g. DocumentReference)
        CodeableConcept researchStudy = new CodeableConcept();
        researchStudy.addCoding(new Coding()
                .setSystem("urn:oid:2.16.840.1.113883.6.1")
                .setCode("LOINC_TBD")
                .setDisplay("Human Nature Research Study"));

        Basic basicItem1 = new Basic();
        basicItem1.setId(new IdType("item1"));
        basicItem1.setCode(researchStudy);
        basicItem1.addIdentifier(new Identifier().setSystem("local system id").setValue("id for Human Nature study"));

        Reference itemReference1 = new Reference("#item1");
        researchStudyEntry.setItem(itemReference1);
        list.addEntry(researchStudyEntry);

        // add list to contract
        consent.getRecipientFirstRep().setReference("#" + includedDataListId);
        consent.getContained().add(list);
        // add items as Basic resources
        consent.getContained().add(basicItem1);

        // Create XML and JSON files including generated narrative XHTML
        String xmlEncodedGranularConsent = ctxt.newXmlParser().setPrettyPrint(true).encodeResourceToString(consent);
        try {
            FileUtils.writeStringToFile(new File(testResourcesPath + "/XML/temp/" + currentTest + ".xml"),
                    xmlEncodedGranularConsent);
        } catch (IOException e) {

            fail("Write resource to XML:" + e.getMessage());
        }
        String jsonEncodedGranularConsent = ctxt.newJsonParser().setPrettyPrint(true).encodeResourceToString(consent);
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
                assertTrue(cd.getCode().equalsIgnoreCase("HRESCH"));
            }
        }
        if (consentFromFile.getConsentor().size() != 0) {
            Reference consentSubjectRef = consentFromFile.getConsentor().get(0);
            IBaseResource referencedSubject = consentSubjectRef.getResource();
            String referencedId = referencedSubject.getIdElement().getIdPart();
            assertTrue(referencedId.equalsIgnoreCase(patientId));
            ListIterator<Resource> containedResources = consentFromFile.getContained().listIterator();
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

            String xmlEncodedPatient = ctxt.newXmlParser().setPrettyPrint(true).encodeResourceToString(subjectPatientResource);
            try {
                FileUtils.writeStringToFile(new File(testResourcesPath + "/XML/temp/" + "PatientSubject.xml"),
                        xmlEncodedPatient);
            } catch (IOException e) {

                fail("Write Patient resource to XML:" + e.getMessage());
            }
        }

    }

}