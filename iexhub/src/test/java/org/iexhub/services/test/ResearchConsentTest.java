package org.iexhub.services.test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.*;
import ca.uhn.fhir.model.dstu2.resource.*;
import ca.uhn.fhir.model.dstu2.resource.Organization.Contact;
import ca.uhn.fhir.model.dstu2.valueset.*;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.IdDt;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.*;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ResearchConsentTest {
	// FHIR resource identifiers for contained objects
	private static String consentId = "consentId";
	private static String patientId = "patientId";
	private static String sourceOrganizationId = "researchOrgOID";
	private static String recipientResearcherId = "recipientResearcherId";
	private static String testResourcesPath = "src/test/resources/";
	private static FhirContext ctxt = new FhirContext();

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
		Contact organizationContactResource = new Contact();
		Practitioner recipientResearcherPractitionerResource = new Practitioner();
		// Consent as contract
		Contract contract = new Contract();
		// create the testPatient resource to be embedded into a contract
		NarrativeDt patientNarrative= new NarrativeDt ();
		patientNarrative.setDiv("Sample patient demographics pertinent to consent");
		testPatientResource.setText(patientNarrative);
		testPatientResource.setId(new IdDt(patientId));
		testPatientResource.addName().addFamily("Patient Family Name").addGiven("Patient Given Name");
		// set SSN value using coding system 2.16.840.1.113883.4.1
		testPatientResource.addIdentifier().setSystem(uriPrefix + "2.16.840.1.113883.4.1").setValue("123-45-6789");
		// set local patient id
		testPatientResource.addIdentifier().setSystem(uriPrefix + iExHubDomainOid).setValue(patientId);
		testPatientResource.setGender(AdministrativeGenderEnum.FEMALE);
		testPatientResource.setBirthDate(new DateDt("1966-10-22"));
		testPatientResource.addAddress().addLine("Patient Address Line").setCity("City").setState("NY")
				.setPostalCode("12345").setType(AddressTypeEnum.POSTAL);
		testPatientResource.addTelecom().setUse(ContactPointUseEnum.HOME).setSystem(ContactPointSystemEnum.PHONE)
				.setValue("555-1212");
		testPatientResource.addTelecom().setUse(ContactPointUseEnum.HOME).setSystem(ContactPointSystemEnum.EMAIL)
				.setValue("patient@home.org");
		// Provider organization...
		// set id to be used to reference the providerOrganization as an inline
		// resource
		sourceOrganizationResource.setId(new IdDt(sourceOrganizationId));
		sourceOrganizationResource.addIdentifier().setSystem(uriPrefix + "2.16.840.1.113883.4.6").setValue("NPI of source organization");
		sourceOrganizationResource.setName("Research Organization Name");
		sourceOrganizationResource.addAddress().addLine("1 Research Drive").setCity("Research City").setState("MA")
				.setPostalCode("01221");
		// contact
		organizationContactResource.addTelecom().setUse(ContactPointUseEnum.WORK)
				.setSystem(ContactPointSystemEnum.PHONE).setValue("408-555-1212");
		organizationContactResource.addTelecom().setUse(ContactPointUseEnum.WORK)
				.setSystem(ContactPointSystemEnum.EMAIL).setValue("contact@sourceorgnization.org");
		HumanNameDt contactName = new HumanNameDt();
		contactName.addFamily().setValue("Researcher Family Name");
		contactName.addGiven().setValue("Researcher Given Name");
		organizationContactResource.setName(contactName);
		List<Contact> contacts = new ArrayList<Contact>();
		contacts.add(organizationContactResource);
		sourceOrganizationResource.setContact(contacts);
		// set reference using "#" prefix
		testPatientResource.addCareProvider().setReference("#" + sourceOrganizationId);
		// recipient practitioner
		recipientResearcherPractitionerResource.setId(new IdDt(recipientResearcherId));
		recipientResearcherPractitionerResource.addIdentifier().setSystem(uriPrefix + "2.16.840.1.113883.4.6").setValue("NPI of recipient provider");
		recipientResearcherPractitionerResource.getName().addFamily("Recipient Practitioner Last Name")
				.addGiven("Recipient Practitioner Given Name").addSuffix("MD").addPrefix("Ms.");
		recipientResearcherPractitionerResource.addAddress().addLine("Recipient Practitioner Address Line").setCity("City")
				.setState("NY").setPostalCode("98765");
		recipientResearcherPractitionerResource.addTelecom().setUse(ContactPointUseEnum.WORK)
				.setSystem(ContactPointSystemEnum.PHONE).setValue("212-000-1212");
		recipientResearcherPractitionerResource.addTelecom().setUse(ContactPointUseEnum.WORK)
				.setSystem(ContactPointSystemEnum.EMAIL).setValue("recipient@destination.org");
		// populate contract
		// set the id as a concatenated "OID.consentId"
		contract.setId(new IdDt(iExHubDomainOid + "." + consentId));
		contract.getIdentifier().setSystem(uriPrefix + iExHubDomainOid)
				.setValue("consent GUID");
		contract.getType().setValueAsEnum(ContractTypeCodesEnum.DISCLOSURE);
		//research
		CodeableConceptDt reasearchPurposeCode = new CodeableConceptDt("http://hl7.org/fhir/contractsubtypecodes", "HRESCH");
		reasearchPurposeCode.setText("Consent for research"); //$NON-NLS-1$
		contract.getActionReason().add(reasearchPurposeCode);
		DateTimeDt issuedDateTime = new DateTimeDt();
		issuedDateTime.setValue(Calendar.getInstance().getTime());
		contract.setIssued(issuedDateTime);
		// specify the covered entity authorized to disclose
		// add source resource and authority reference
		contract.getContained().getContainedResources().add(sourceOrganizationResource);
		contract.addAuthority().setReference("#" + sourceOrganizationId);
		// specify the patient identified in the consent
		// add local reference to patient
		ResourceReferenceDt patientReference = new ResourceReferenceDt("#" + patientId);
		patientReference.setDisplay("Research Participant");
		contract.getSubject().add(patientReference);
		contract.getSignerFirstRep()
				.setType(new CodingDt("http://hl7.org/fhir/contractsignertypecodes", "1.2.840.10065.1.12.1.7"));
		contract.getSignerFirstRep().setSignature(testPatientResource.getNameFirstRep().getNameAsSingleString());
		contract.getSignerFirstRep().setParty(patientReference);
		// add test patient as a contained resource rather than externalreference
		contract.getContained().getContainedResources().add(testPatientResource);
		// set terms of consent and intended recipient(s)
		PeriodDt applicablePeriod = new PeriodDt();
		applicablePeriod.setEnd(new DateTimeDt("2016-05-10"));
		applicablePeriod.setStart(new DateTimeDt("2016-05-10"));
		contract.getTermFirstRep().setApplies(applicablePeriod);
		// list all recipients
		contract.getTermFirstRep().addActor().getEntity().setReference("#" + recipientResearcherId);
		contract.getContained().getContainedResources().add(recipientResearcherPractitionerResource);
		contract.getTermFirstRep().setText("description of the consent terms");
		// add granular preferences
		String includedDataListId = "includedListOfDataTypes";
		ListResource list = new ListResource();
		list.setId(new IdDt(includedDataListId));
		list.setTitle("List of included data types");
		// specifies how the list items are to be used
		CodeableConceptDt includeCodeValue = new CodeableConceptDt("http://hl7.org/fhir/v3/SetOperator", "I");
		includeCodeValue.setText("Include");
		list.setCode(includeCodeValue);

		list.setStatus(ListStatusEnum.CURRENT);
		list.setMode(ListModeEnum.SNAPSHOT_LIST);

		// add discharge summary document type
		ListResource.Entry researchStudyEntry = new ListResource.Entry();
		// use list item flag to specify a category and the item to specify an
		// instance (e.g. DocumentReference)
		CodeableConceptDt researchStudy = new CodeableConceptDt("urn:oid:2.16.840.1.113883.6.1", "LOINC_TBD");
		researchStudy.setText("Human Nature Research Study");
		Basic basicItem1 = new Basic();
		basicItem1.setId(new IdDt("item1"));
		basicItem1.setCode(researchStudy);
		basicItem1.addIdentifier(new IdentifierDt("local system id","id for Human Nature study"));

		ResourceReferenceDt itemReference1  = new ResourceReferenceDt("#item1");
		researchStudyEntry.setItem(itemReference1);
		list.addEntry(researchStudyEntry);
		// add list to contract
		contract.getTerm().get(0).getSubject().setReference("#" + includedDataListId);
		contract.getContained().getContainedResources().add(list);
		// add items as Basic resources
		contract.getContained().getContainedResources().add(basicItem1);
		// Use the narrative generator
		// @TODO: add generator Thymeleaf templates
		// ctxt.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
		// Create XML and JSON files including generated narrative XHTML
		String xmlEncodedGranularConsent = ctxt.newXmlParser().setPrettyPrint(true).encodeResourceToString(contract);
		try {
			FileUtils.writeStringToFile(new File(testResourcesPath + "/XML/temp/" + currentTest + ".xml"),
					xmlEncodedGranularConsent);
		} catch (IOException e) {

			fail("Write resource to XML:" + e.getMessage());
		}
		String jsonEncodedGranularConsent = ctxt.newJsonParser().setPrettyPrint(true).encodeResourceToString(contract);
		try {
			FileUtils.writeStringToFile(new File(testResourcesPath + "/JSON/temp/" + currentTest + ".json"),
					jsonEncodedGranularConsent);
		} catch (IOException e) {
			fail("Write resource to JSON:" + e.getMessage());
		}
		Contract consentFromFile = null;
		String readContractString = "";
		try {
			readContractString = FileUtils
					.readFileToString(new File(testResourcesPath + "/XML/temp/" + currentTest + ".xml"), "UTF-8");
		} catch (IOException e) {
			fail("Reading resource from file error:" + e.getMessage());
		}
		consentFromFile = (Contract) ctxt.newXmlParser().parseResource(readContractString);
		if (!consentFromFile.getActionReason().isEmpty()) {
			// read purpose of use
			Iterator<CodeableConceptDt> purposeOfUseIterator = consentFromFile.getActionReason().iterator();
			while (purposeOfUseIterator.hasNext()) {
				CodeableConceptDt cd = purposeOfUseIterator.next();
				assertTrue(cd.getCodingFirstRep().getCode().equalsIgnoreCase("HRESCH"));
			}
		}
		if (!consentFromFile.getSubject().isEmpty()) {
			ResourceReferenceDt consentSubjectRef = consentFromFile.getSubject().get(0);
			IBaseResource referencedSubject = consentSubjectRef.getResource();
			String referencedId = referencedSubject.getIdElement().getIdPart();
			assertTrue(referencedId.equalsIgnoreCase(patientId));
			ListIterator<IResource> containedResources = consentFromFile.getContained().getContainedResources()
					.listIterator();
			Patient subjectPatientResource = null;
			while (containedResources.hasNext()) {
				IResource cr = (IResource) containedResources.next();
				if (cr.getIdElement().getIdPart().equalsIgnoreCase(referencedId)) {
					subjectPatientResource = (Patient) cr;
					HumanNameDt n = subjectPatientResource.getName().get(0);
					assertTrue(n.getFamily().get(0).getValue().equalsIgnoreCase("Patient Family Name"));
					break;
				}
			}
			//subjectPatientResource.getContained().getContainedResources().add(sourceOrganizationResource);
			//ResourceReferenceDt sourceIdRef = new ResourceReferenceDt();
			//sourceIdRef.setReference("#"+sourceOrganizationId);
			//subjectPatientResource.getCareProvider().add(sourceIdRef);

			String xmlEncodedPatient = ctxt.newXmlParser().setPrettyPrint(true).encodeResourceToString(subjectPatientResource);
			try {
				FileUtils.writeStringToFile(new File(testResourcesPath + "/XML/" + "PatientSubject.xml"),
						xmlEncodedPatient);
			} catch (IOException e) {

				fail("Write Patient resource to XML:" + e.getMessage());
			}
		}

	}

}