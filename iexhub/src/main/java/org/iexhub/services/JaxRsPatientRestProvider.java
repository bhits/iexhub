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
package org.iexhub.services;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBElement;
import org.apache.log4j.Logger;
import org.iexhub.connectors.PDQQueryManager;
import org.iexhub.connectors.PIXManager;
import org.iexhub.exceptions.FamilyNameParamMissingException;
import org.iexhub.exceptions.PatientIdParamMissingException;
import org.iexhub.exceptions.UnexpectedServerException;

import PDQSupplier.org.hl7.v3.AD;
import PDQSupplier.org.hl7.v3.COCTMT150003UV03ContactParty;
import PDQSupplier.org.hl7.v3.COCTMT150003UV03Organization;
import PDQSupplier.org.hl7.v3.ON;
import PDQSupplier.org.hl7.v3.PN;
import PDQSupplier.org.hl7.v3.PRPAIN201306UV02;
import PDQSupplier.org.hl7.v3.PRPAIN201306UV02MFMIMT700711UV01Subject1;
import PDQSupplier.org.hl7.v3.PRPAMT201310UV02OtherIDs;
import PDQSupplier.org.hl7.v3.PRPAMT201310UV02Person;
import PDQSupplier.org.hl7.v3.TEL;
import PIXManager.org.hl7.v3.MCCIIN000002UV01;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jaxrs.server.AbstractJaxRsResourceProvider;
import ca.uhn.fhir.model.dstu2.composite.AddressDt;
import ca.uhn.fhir.model.dstu2.composite.HumanNameDt;
import ca.uhn.fhir.model.dstu2.resource.Organization;
import ca.uhn.fhir.model.dstu2.resource.Organization.Contact;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.ConditionalUrlParam;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.AddProfileTagEnum;
import ca.uhn.fhir.rest.server.BundleInclusionRule;
import ca.uhn.fhir.rest.server.Constants;
import ca.uhn.fhir.rest.server.ETagSupportEnum;
import ca.uhn.fhir.rest.server.FifoMemoryPagingProvider;
import ca.uhn.fhir.rest.server.IPagingProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.IValidatorModule;
import ca.uhn.fhir.validation.SchemaBaseValidator;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;
import ca.uhn.fhir.validation.schematron.SchematronBaseValidator;

/**
 * FHIR Patient Implementation supports: find, search, and create.
 * @author A. Sute
 */
@Local
@Path(JaxRsPatientRestProvider.PATH)
@Stateless
@Produces({ MediaType.APPLICATION_JSON, Constants.CT_FHIR_JSON, Constants.CT_FHIR_XML })
public class JaxRsPatientRestProvider extends AbstractJaxRsResourceProvider<Patient>
{
    /** Logger */
    public static Logger log = Logger.getLogger(JaxRsPatientRestProvider.class);

	private static PIXManager pixManager = null;
	private static PDQQueryManager pdqQueryManager = null;

	private static Properties props = null;
	private static boolean testMode = false;
	private static String propertiesFile = "/temp/IExHub.properties";
	private static String cdaToJsonTransformXslt = null;
	private static String iExHubDomainOid = "2.16.840.1.113883.3.72.5.9.1";
	private static String iExHubAssigningAuthority = "ISO";
	private static String pdqManagerEndpointUri = null;
	private static String pixManagerEndpointUri = null;
	private static FhirContext fhirCtxt = null;
	private static FhirValidator fhirValidator = null;
	
	/**
	 * The HAPI paging provider for this server
	 */
	public static final IPagingProvider PAGE_PROVIDER;
	static
	{
		PAGE_PROVIDER = JaxRsPatientPageProvider.PAGE_PROVIDER;
	}
	
	static final String PATH = "/Patient";

	public JaxRsPatientRestProvider()
	{
		super(JaxRsPatientRestProvider.class);
		
//		fhirCtxt = new FhirContext();
//		fhirValidator = fhirCtxt.newValidator();
//		IValidatorModule module1 = new SchemaBaseValidator(fhirCtxt);
//		fhirValidator.registerValidatorModule(module1);
//		IValidatorModule module2 = new SchematronBaseValidator(fhirCtxt);
//		fhirValidator.registerValidatorModule(module2);
	}

	private void loadProperties()
	{
		try
		{
			props = new Properties();
			props.load(new FileInputStream(propertiesFile));
			JaxRsPatientRestProvider.testMode = (props.getProperty("TestMode") == null) ? JaxRsPatientRestProvider.testMode
					: Boolean.parseBoolean(props.getProperty("TestMode"));
			JaxRsPatientRestProvider.cdaToJsonTransformXslt = (props.getProperty("CDAToJSONTransformXSLT") == null) ? JaxRsPatientRestProvider.cdaToJsonTransformXslt
					: props.getProperty("CDAToJSONTransformXSLT");
			JaxRsPatientRestProvider.iExHubDomainOid = (props.getProperty("IExHubDomainOID") == null) ? JaxRsPatientRestProvider.iExHubDomainOid
					: props.getProperty("IExHubDomainOID");
			JaxRsPatientRestProvider.iExHubAssigningAuthority = (props.getProperty("IExHubAssigningAuthority") == null) ? JaxRsPatientRestProvider.iExHubAssigningAuthority
					: props.getProperty("IExHubAssigningAuthority");
			JaxRsPatientRestProvider.pixManagerEndpointUri = (props.getProperty("PIXManagerEndpointURI") == null) ? JaxRsPatientRestProvider.pixManagerEndpointUri
					: props.getProperty("PIXManagerEndpointURI");
			JaxRsPatientRestProvider.pdqManagerEndpointUri = (props.getProperty("PDQManagerEndpointURI") == null) ? JaxRsPatientRestProvider.pdqManagerEndpointUri
					: props.getProperty("PDQManagerEndpointURI");
		}
		catch (IOException e)
		{
			log.error("Error encountered loading properties file, "
					+ propertiesFile
					+ ", "
					+ e.getMessage());
			throw new UnexpectedServerException("Error encountered loading properties file, "
					+ propertiesFile
					+ ", "
					+ e.getMessage());
		}
	}
	
	/**
	 * Patient create
	 * @param patient
	 * @param theConditional
	 * @return
	 * @throws Exception
	 */
	@Create
	public MethodOutcome create(@ResourceParam final Patient patient, @ConditionalUrlParam String theConditional) throws Exception
	{
		log.info("Entered FHIR Patient create service");

		// Validate FHIR resource...
//		ValidationResult validationResult = fhirValidator.validateWithResult(patient);
//		if (!validationResult.isSuccessful())
//		{
//			StringBuilder errorMsgs = new StringBuilder();
//			for (SingleValidationMessage next : validationResult.getMessages())
//			{
//				errorMsgs.append(next.getLocationString()
//						+ " "
//						+ next.getMessage());
//			}
//			
//			throw new UnprocessableEntityException(errorMsgs.toString(),
//					validationResult.toOperationOutcome()); 
//		}
		
		if (props == null)
		{
			loadProperties();
		}

		MethodOutcome result = new MethodOutcome().setCreated(false);
		result.setResource(patient);
		
		try
		{
			if (pixManager == null)
			{
				log.info("Instantiating PIXManager connector...");
				pixManager = new PIXManager(null,
						false);
				log.info("PIXManager connector successfully started");
			}
		}
		catch (Exception e)
		{
			log.error("Error encountered instantiating PIXManager connector, "
					+ e.getMessage());
			throw new UnexpectedServerException("Error - " + e.getMessage());
		}

		try
		{
			// ITI-44-Source-Feed message
			MCCIIN000002UV01 pixRegistrationResponse = pixManager.registerPatient(patient);
			
			if ((pixRegistrationResponse.getAcknowledgement().get(0).getTypeCode().getCode().compareToIgnoreCase("CA") == 0) ||
				(pixRegistrationResponse.getAcknowledgement().get(0).getTypeCode().getCode().compareToIgnoreCase("AA") == 0))
			{
				result = new MethodOutcome().setCreated(true);
			}
		}
		catch (Exception e)
		{
			log.error("Error encountered, "
					+ e.getMessage());
			throw new UnexpectedServerException("Error - " + e.getMessage());
		}

		log.info("Exiting FHIR Patient create service");
		return result;
	}

//	@Delete
//	public MethodOutcome delete(@IdParam final IdDt theId) throws Exception {
//		final Patient deletedPatient = find(theId);
//		patients.remove(deletedPatient.getId().getIdPart());
//		final MethodOutcome result = new MethodOutcome().setCreated(true);
//		result.setResource(deletedPatient);
//		return result;
//	}

	/**
	 * Parses the patient address from V3 Patient
	 * @param patientPerson
	 * @return patient address as FHIR Address datatype
	 */
	private AddressDt populateFhirAddress(PRPAMT201310UV02Person patientPerson)
	{
		AddressDt fhirAddr = new AddressDt();

		for (AD addr : patientPerson.getAddr())
		{
			for (Serializable nameComponent : addr.getContent())
			{
				JAXBElement<?> testNameComponent = (JAXBElement<?>) nameComponent;
				if (testNameComponent.getValue().getClass() == PDQSupplier.org.hl7.v3.AdxpCity.class)
				{
					fhirAddr.setCity(((PDQSupplier.org.hl7.v3.AdxpCity)testNameComponent.getValue()).getContent().get(0).toString());
				}
				else
				if (testNameComponent.getValue().getClass() == PDQSupplier.org.hl7.v3.AdxpState.class)
				{
					fhirAddr.setState(((PDQSupplier.org.hl7.v3.AdxpState)testNameComponent.getValue()).getContent().get(0).toString());
				}
				else
				if (testNameComponent.getValue().getClass() == PDQSupplier.org.hl7.v3.AdxpPostalCode.class)
				{
					fhirAddr.setPostalCode(((PDQSupplier.org.hl7.v3.AdxpPostalCode)testNameComponent.getValue()).getContent().get(0).toString());
				}
				else
				if (testNameComponent.getValue().getClass() == PDQSupplier.org.hl7.v3.AdxpCountry.class)
				{
					fhirAddr.setCountry(((PDQSupplier.org.hl7.v3.AdxpCountry)testNameComponent.getValue()).getContent().get(0).toString());
				}
				else
				if (testNameComponent.getValue().getClass() == PDQSupplier.org.hl7.v3.AdxpStreetAddressLine.class)
				{
					fhirAddr.addLine(((PDQSupplier.org.hl7.v3.AdxpStreetAddressLine)testNameComponent.getValue()).getContent().get(0).toString());
				}
			}
		}
		
		return fhirAddr;
	}

	/**
	 * Parses the contact address from V3 Contact
	 * @param contact
	 * @return FHIR Address datatype
	 */
	private AddressDt populateFhirAddress(COCTMT150003UV03ContactParty contact)
	{
		AddressDt fhirAddr = new AddressDt();

		for (AD addr : contact.getAddr())
		{
			for (Serializable nameComponent : addr.getContent())
			{
				JAXBElement<?> testNameComponent = (JAXBElement<?>) nameComponent;
				if (testNameComponent.getValue().getClass() == PDQSupplier.org.hl7.v3.AdxpCity.class)
				{
					fhirAddr.setCity(((PDQSupplier.org.hl7.v3.AdxpCity)testNameComponent.getValue()).getContent().get(0).toString());
				}
				else
				if (testNameComponent.getValue().getClass() == PDQSupplier.org.hl7.v3.AdxpState.class)
				{
					fhirAddr.setState(((PDQSupplier.org.hl7.v3.AdxpState)testNameComponent.getValue()).getContent().get(0).toString());
				}
				else
				if (testNameComponent.getValue().getClass() == PDQSupplier.org.hl7.v3.AdxpPostalCode.class)
				{
					fhirAddr.setPostalCode(((PDQSupplier.org.hl7.v3.AdxpPostalCode)testNameComponent.getValue()).getContent().get(0).toString());
				}
				else
				if (testNameComponent.getValue().getClass() == PDQSupplier.org.hl7.v3.AdxpCountry.class)
				{
					fhirAddr.setCountry(((PDQSupplier.org.hl7.v3.AdxpCountry)testNameComponent.getValue()).getContent().get(0).toString());
				}
				else
				if (testNameComponent.getValue().getClass() == PDQSupplier.org.hl7.v3.AdxpStreetAddressLine.class)
				{
					fhirAddr.addLine(((PDQSupplier.org.hl7.v3.AdxpStreetAddressLine)testNameComponent.getValue()).getContent().get(0).toString());
				}
			}
		}
		
		return fhirAddr;
	}

	/**
	 * Parses the V3 Patient structure returned by PDQV Query
	 * @param queryResponse Query response message containing matching patient records
	 * @return ArrayList<Patient> containing FHIR Patient object 
	 * @throws ParseException
	 */
	private ArrayList<Patient> populatePatientObject(PRPAIN201306UV02 queryResponse) throws ParseException
	{
		ArrayList<Patient> result = new ArrayList<Patient>();
		
		/////////////////////////////////////////////////////////////////////////////////////
		// Create PatientPerson...
//		ObjectFactory objectFactory = new PDQSupplier.org.hl7.v3.ObjectFactory();
//		PRPAMT201310UV02Person patientPersonTest = new PRPAMT201310UV02Person();
//		
//		PDQSupplier.org.hl7.v3.EnFamily enFamily = new PDQSupplier.org.hl7.v3.EnFamily();
//		enFamily.getContent().add("Alpha");
//		PDQSupplier.org.hl7.v3.EnGiven enGiven = new PDQSupplier.org.hl7.v3.EnGiven();
//		enGiven.getContent().add("Alan");
//		
//		PN patientName = new PN();
//		patientName.getContent().add(objectFactory.createENFamily(enFamily));
//		
//		if (enGiven != null)
//		{
//			patientName.getContent().add(objectFactory.createENGiven(enGiven));
//		}
//		
//		patientPersonTest.getName().add(patientName);
//		for (PN name : patientPersonTest.getName())
//		{
//			String family = null;
//			String given = null;
//
//			for (Serializable nameComponent : name.getContent())
//			{
//				JAXBElement<?> testNameComponent = (JAXBElement<?>) nameComponent;
//				if (testNameComponent.getValue().getClass() == PDQSupplier.org.hl7.v3.EnFamily.class)
//				{
//					family = new String();
//					ST test = (ST) ((EnFamily)testNameComponent.getValue()).getContent().get(0);
//					family = family.concat(test.toString());
//				}
//				else
//				if (testNameComponent.getValue().getClass() == PDQSupplier.org.hl7.v3.EnGiven.class)
//				{
//					int test = 0;
//				}
//			}
//		}

		// Iterate through each Subject and extract demographic info
		for (PRPAIN201306UV02MFMIMT700711UV01Subject1 subject : queryResponse.getControlActProcess().getSubject())
		{
			Patient patient = new Patient();
			
			if ((subject.getRegistrationEvent() != null) &&
				(subject.getRegistrationEvent().getSubject1() != null) &&
				(subject.getRegistrationEvent().getSubject1().getPatient() != null) &&
				(subject.getRegistrationEvent().getSubject1().getPatient().getPatientPerson() != null))
			{
				// FHIR resource ID is in format "<root OID>^<extension>"
				patient.setId(new StringBuilder()
						.append(subject.getRegistrationEvent().getSubject1().getPatient().getId().get(0).getRoot())
						.append("^")
						.append(subject.getRegistrationEvent().getSubject1().getPatient().getId().get(0).getExtension()).toString());
				
				PRPAMT201310UV02Person patientPerson = subject.getRegistrationEvent().getSubject1().getPatient().getPatientPerson().getValue(); 

				// Extract other identifiers...
				for (PRPAMT201310UV02OtherIDs otherId : patientPerson.getAsOtherIDs())
				{
					patient.addIdentifier().setSystem(otherId.getId().get(0).getRoot()).setValue(otherId.getId().get(0).getExtension());
				}
				
				// Extract name if present...
				if (patientPerson.getName() != null)
				{
					for (PN name : patientPerson.getName())
					{
						HumanNameDt fhirName = new HumanNameDt();

						for (Serializable nameComponent : name.getContent())
						{
							JAXBElement<?> testNameComponent = (JAXBElement<?>) nameComponent;
							if (testNameComponent.getValue().getClass() == PDQSupplier.org.hl7.v3.EnFamily.class)
							{
								fhirName.addFamily(((PDQSupplier.org.hl7.v3.EnFamily)testNameComponent.getValue()).getContent().get(0).toString());
							}
							else
							if (testNameComponent.getValue().getClass() == PDQSupplier.org.hl7.v3.EnGiven.class)
							{
								fhirName.addGiven(((PDQSupplier.org.hl7.v3.EnGiven)testNameComponent.getValue()).getContent().get(0).toString());
							}
						}
						
						patient.addName(fhirName);
					}
				}
				
				// Extract gender if present...
				if (patientPerson.getAdministrativeGenderCode() != null)
				{
					patient.setGender((patientPerson.getAdministrativeGenderCode().getCode().compareToIgnoreCase("M") == 0) ? AdministrativeGenderEnum.MALE
							: (patientPerson.getAdministrativeGenderCode().getCode().compareToIgnoreCase("F") == 0) ? AdministrativeGenderEnum.FEMALE
									: AdministrativeGenderEnum.UNKNOWN);
				}
				
				// Extract telecom if present...
				if (patientPerson.getTelecom() != null)
				{
					for (TEL telecom : patientPerson.getTelecom())
					{
						patient.addTelecom().setValue(telecom.getValue());
					}
				}
				
				// Extract birth date if present...
				if (patientPerson.getBirthTime() != null)
				{
					Calendar birthDateCalendar = Calendar.getInstance();
					SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
					birthDateCalendar.setTime(formatter.parse(patientPerson.getBirthTime().getValue()));
					DateDt birthDate = new DateDt();
					birthDate.setValueAsString(new StringBuilder()
							.append(birthDateCalendar.get(Calendar.YEAR))
							.append("-")
							.append(String.format("%02d", (birthDateCalendar.get(Calendar.MONTH) + 1)))
							.append("-")
							.append(String.format("%02d", birthDateCalendar.get(Calendar.DAY_OF_MONTH))).toString());
					patient.setBirthDate(birthDate);
				}
				
				// Extract address if present...
				if (patientPerson.getAddr() != null)
				{
					patient.addAddress(populateFhirAddress(patientPerson));
				}
			}
			
			if ((subject.getRegistrationEvent() != null) &&
				(subject.getRegistrationEvent().getSubject1() != null) &&
				(subject.getRegistrationEvent().getSubject1().getPatient() != null) &&
				(subject.getRegistrationEvent().getSubject1().getPatient().getProviderOrganization() != null))
			{
				COCTMT150003UV03Organization provider = subject.getRegistrationEvent().getSubject1().getPatient().getProviderOrganization().getValue(); 
				
				// Create Organization FHIR resource and set its ID...
				Organization organization = new Organization();
				organization.getId().setValue(provider.getId().get(0).getRoot());

				// Extract name if present...
				if (provider.getName() != null)
				{
					for (ON name : provider.getName())
					{
						for (Serializable nameComponent : name.getContent())
						{
							JAXBElement<?> testNameComponent = (JAXBElement<?>) nameComponent;
							if (testNameComponent.getValue().getClass() == String.class)
							{
								organization.setName(testNameComponent.getValue().toString());
								break;
							}
						}
					}
				}
					
				if (provider.getContactParty() != null)
				{
					for (COCTMT150003UV03ContactParty contact : provider.getContactParty())
					{
						Contact organizationContact = new Contact();
						
						if (contact.getTelecom() != null)
						{
							for (TEL telecom : contact.getTelecom())
							{
								organizationContact.addTelecom().setValue(telecom.getValue());
							}
						}
						
						if (contact.getAddr() != null)
						{
							//  While the PDQ ITI-47 response can contain multiple providerOrganization contact addresses, FHIR only allows one contact address to be specified...
							organizationContact.setAddress(populateFhirAddress(contact));
						}
						
						organization.getContact().add(organizationContact);
					}
				}

				patient.addCareProvider().setReference("#"
						+ organization.getId().getValue());
				patient.getContained().getContainedResources().add(organization);
			}
			
			result.add(patient);
		}
		
		return result;
	}
	
	/**
	 * Patient find
	 * @param id
	 * @return matching patient
	 * @throws Exception
	 */
	@Read
	public Patient find(@IdParam final IdDt id) throws Exception
	{
		if ((id == null) ||
			(id.isEmpty()))
		{
			throw new PatientIdParamMissingException("Patient ID parameter missing");
		}

		log.info(String.format("Entered FHIR Patient find (by ID) service, ID=%s",
				id.getValueAsString()));

		if (props == null)
		{
			if (props == null)
			{
				loadProperties();
			}
		}

		Patient result = null;
		
		if ((pdqManagerEndpointUri != null) &&
			(pdqManagerEndpointUri.length() > 0))
		{
			try
			{
				if (pdqQueryManager == null)
				{
					log.info("Instantiating PDQQueryManager connector...");
					pdqQueryManager = new PDQQueryManager(null,
							false);
					log.info("PDQQueryManager connector successfully started");
				}
			}
			catch (Exception e)
			{
				log.error("Error encountered instantiating PDQQueryManager connector, "
						+ e.getMessage());
				throw new UnexpectedServerException("Error - " + e.getMessage());
			}

			try
			{
				// ITI-47-Consumer-Query-Patient-PatientId message
				PRPAIN201306UV02 pdqQueryResponse = pdqQueryManager.queryPatientDemographics(null,
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						(id.getIdPart().contains("^")) ? id.getIdPart().split("\\^")[0]
								: id.getIdPart(),
						(id.getIdPart().contains("^")) ? id.getIdPart().split("\\^")[1]
								: null,
						null,
						null);
				
				// Determine if demographics data is present.
				if ((pdqQueryResponse != null) &&
					(pdqQueryResponse.getAcknowledgement() != null) &&
					(pdqQueryResponse.getAcknowledgement().get(0).getTypeCode().getCode().equalsIgnoreCase("AA")) &&
					(pdqQueryResponse.getControlActProcess() != null) &&
					(pdqQueryResponse.getControlActProcess().getSubject() != null) &&
					(!pdqQueryResponse.getControlActProcess().getSubject().isEmpty()))
				{
					result = populatePatientObject(pdqQueryResponse).get(0);
				}
				else
				{
					throw new ResourceNotFoundException(id);
				}
			}
			catch (Exception e)
			{
				log.error("Error encountered, "
						+ e.getMessage());
				throw e;
			}
		}
			
		log.info("Exiting FHIR Patient find (by ID) service");
		return result;
	}

//	@Read(version = true)
//	public Patient findHistory(@IdParam final IdDt theId) {
//		if (patients.containsKey(theId.getIdPart())) {
//			final List<Patient> list = patients.get(theId.getIdPart());
//			for (final Patient patient : list) {
//				if (patient.getId().getVersionIdPartAsLong().equals(theId.getVersionIdPartAsLong())) {
//					return patient;
//				}
//			}
//		}
//		throw new ResourceNotFoundException(theId);
//	}
//
//	@Operation(name = "firstVersion", idempotent = true, returnParameters = { @OperationParam(name = "return", type = StringDt.class) })
//	public Parameters firstVersion(@IdParam final IdDt theId, @OperationParam(name = "dummy") StringDt dummyInput) throws Exception {
//		Parameters parameters = new Parameters();
//		Patient patient = find(new IdDt(theId.getResourceType(), theId.getIdPart(), "0"));
//		parameters.addParameter().setName("return").setResource(patient).setValue(new StringDt((1) + "" + "inputVariable [ " + dummyInput.getValue() + "]"));
//		return parameters;
//	}

	@Override
	public AddProfileTagEnum getAddProfileTag() {
		return AddProfileTagEnum.NEVER;
	}

	@Override
	public BundleInclusionRule getBundleInclusionRule() {
		return BundleInclusionRule.BASED_ON_INCLUDES;
	}

	@Override
	public ETagSupportEnum getETagSupport() {
		return ETagSupportEnum.DISABLED;
	}

	/** THE DEFAULTS */

	@Override
	public List<IServerInterceptor> getInterceptors() {
		return Collections.emptyList();
	}

	private Patient getLast(final List<Patient> list) {
		return list.get(list.size() - 1);
	}

	@Override
	public IPagingProvider getPagingProvider() {
		return PAGE_PROVIDER;
	}

	@Override
	public Class<Patient> getResourceType() {
		return Patient.class;
	}

	@Override
	public boolean isDefaultPrettyPrint() {
		return true;
	}

	@Override
	public boolean isUseBrowserFriendlyContentTypes() {
		return true;
	}
	
//
//	@GET
//	@Path("/{id}/$firstVersion")
//	public Response operationFirstVersionUsingGet(@PathParam("id") String id) throws IOException {
//		return customOperation(null, RequestTypeEnum.GET, id, "$firstVersion", RestOperationTypeEnum.EXTENDED_OPERATION_INSTANCE);
//	}
//
//	@POST
//	@Path("/{id}/$firstVersion")
//	public Response operationFirstVersionUsingGet(@PathParam("id") String id, final String resource) throws Exception {
//		return customOperation(resource, RequestTypeEnum.POST, id, "$firstVersion", RestOperationTypeEnum.EXTENDED_OPERATION_INSTANCE);
//	}

	@Search
	public List<Patient> search(@RequiredParam(name = Patient.SP_FAMILY) final StringParam familyName,
			@RequiredParam(name = Patient.SP_GIVEN) final StringParam givenName,
			@OptionalParam(name = Patient.SP_GENDER) final StringParam gender,
			@OptionalParam(name = Patient.SP_BIRTHDATE) final DateParam birthDate,
			@OptionalParam(name = Patient.SP_ADDRESS) final StringParam addressLine,
			@OptionalParam(name = Patient.SP_ADDRESS_CITY) final StringParam addressCity,
			@OptionalParam(name = Patient.SP_ADDRESS_STATE) final StringParam addressState,
			@OptionalParam(name = Patient.SP_ADDRESS_POSTALCODE) final StringParam addressPostalCode,
			@OptionalParam(name = Patient.SP_IDENTIFIER) final TokenParam patientId,
			@OptionalParam(name = Patient.SP_TELECOM) final TokenParam telecom) throws Exception
	{
		if ((familyName == null) ||
			(familyName.isEmpty()) ||
			(givenName == null) ||
			(givenName.isEmpty()))
		{
			throw new FamilyNameParamMissingException("Patient familyName and givenName missing");
		}

		log.info(String.format("Entered FHIR Patient search service, familyName=%s, givenName=%s, gender=%s, birthDate=%s, addressLine=%s, addressCity=%s, addressState=%s, addressPostalCode=%s",
					(familyName == null) ? "" : familyName,
					(givenName == null) ? "" : givenName,
					(gender == null) ? "" : gender,
					(birthDate == null) ? "" : birthDate,
					(addressLine == null) ? "" : addressLine,
					(addressCity == null) ? "" : addressCity,
					(addressState == null) ? "" : addressState,
					(addressPostalCode == null) ? "" : addressPostalCode));
		
		if (props == null)
		{
			if (props == null)
			{
				loadProperties();
			}
		}

		List<Patient> result = null;
		
		// First try PDQ ITI-47 retrieval if PDQ endpoint is specified...
		if ((pdqManagerEndpointUri != null) &&
			(pdqManagerEndpointUri.length() > 0))
		{
			try
			{
				if (pdqQueryManager == null)
				{
					log.info("Instantiating PDQQueryManager connector...");
					pdqQueryManager = new PDQQueryManager(null,
							false);
					log.info("PDQQueryManager connector successfully started");
				}
			}
			catch (Exception e)
			{
				log.error("Error encountered instantiating PDQQueryManager connector, "
						+ e.getMessage());
				throw new UnexpectedServerException("Error - " + e.getMessage());
			}

			try
			{
				// ITI-47-Consumer-Query-Patient-PatientId message
				Calendar birthDateCalendar = Calendar.getInstance();
				if (birthDate != null)
				{
					birthDateCalendar.setTime(birthDate.getValue());
				}
				
				PRPAIN201306UV02 pdqQueryResponse = pdqQueryManager.queryPatientDemographics(givenName.getValue(),
						familyName.getValue(),
						null,
						(birthDate == null) ? null
								: new StringBuilder()
									.append(String.format("%02d", (birthDateCalendar.get(Calendar.MONTH) + 1)))
									.append("/")
									.append(String.format("%02d", birthDateCalendar.get(Calendar.DAY_OF_MONTH)))
									.append("/")
									.append(birthDateCalendar.get(Calendar.YEAR)).toString(),
						(gender == null) ? null
								: gender.getValue(),
						null,
						(addressLine == null) ? null
								: addressLine.getValue(),
						(addressCity == null) ? null
								: addressCity.getValue(),
						(addressState == null) ? null
								: addressState.getValue(),
						(addressPostalCode == null) ? null
								: addressPostalCode.getValue(),
						null,//						patientId.getIdPart().toString(),
						null,
						null,
						(telecom == null) ? null
								: telecom.getValue());
				
				// Determine if demographics data is present.
				if ((pdqQueryResponse != null) &&
					(pdqQueryResponse.getAcknowledgement() != null) &&
					((pdqQueryResponse.getAcknowledgement().get(0).getTypeCode().getCode().equalsIgnoreCase("AA")) ||
					 (pdqQueryResponse.getAcknowledgement().get(0).getTypeCode().getCode().equalsIgnoreCase("CA"))) &&
					(pdqQueryResponse.getControlActProcess() != null) &&
					(pdqQueryResponse.getControlActProcess().getSubject() != null) &&
					(!pdqQueryResponse.getControlActProcess().getSubject().isEmpty()))
				{
					result = populatePatientObject(pdqQueryResponse);
				}
				else
				{
					throw new ResourceNotFoundException(String.format("Patient not found, familyName=%s, givenName=%s, gender=%s, birthDate=%s, addressLine=%s, addressCity=%s, addressState=%s, addressPostalCode=%s",
									(familyName == null) ? "" : familyName,
									(givenName == null) ? "" : givenName,
									(gender == null) ? "" : gender,
									(birthDate == null) ? "" : birthDate,
									(addressLine == null) ? "" : addressLine,
									(addressCity == null) ? "" : addressCity,
									(addressState == null) ? "" : addressState,
									(addressPostalCode == null) ? "" : addressPostalCode));
				}
			}
			catch (Exception e)
			{
				log.error("Error encountered, "
						+ e.getMessage());
				throw e;
			}
		}
			
		log.info("Exiting FHIR Patient search service");
		return result;
	}

//	@Search(compartmentName = "Condition")
//	public List<IResource> searchCompartment(@IdParam IdDt thePatientId) {
//		List<IResource> retVal = new ArrayList<IResource>();
//		Condition condition = new Condition();
//		condition.setId(new IdDt("665577"));
//		retVal.add(condition);
//		return retVal;
//	}
//
//	@Update
//	public MethodOutcome update(@IdParam final IdDt theId, @ResourceParam final Patient patient) {
//		final String idPart = theId.getIdPart();
//		if (patients.containsKey(idPart)) {
//			final List<Patient> patientList = patients.get(idPart);
//			final Patient lastPatient = getLast(patientList);
//			patient.setId(createId(theId.getIdPartAsLong(), lastPatient.getId().getVersionIdPartAsLong() + 1));
//			patientList.add(patient);
//			final MethodOutcome result = new MethodOutcome().setCreated(false);
//			result.setResource(patient);
//			result.setId(patient.getId());
//			return result;
//		} else {
//			throw new ResourceNotFoundException(theId);
//		}
//	}
//
//	private static IdDt createId(final Long id, final Long theVersionId) {
//		return new IdDt("Patient", "" + id, "" + theVersionId);
//	}
//
//	private static List<Patient> createPatient(final Patient patient) {
//		patient.setId(createId(1L, 1L));
//		final LinkedList<Patient> list = new LinkedList<Patient>();
//		list.add(patient);
//		return list;
//	}
//
//	private static List<Patient> createPatient(final String name) {
//		final Patient patient = new Patient();
//		patient.getNameFirstRep().addFamily(name);
//		return createPatient(patient);
//	}

}
