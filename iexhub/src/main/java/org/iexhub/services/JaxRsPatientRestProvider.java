package org.iexhub.services;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import org.apache.log4j.Logger;
import org.iexhub.connectors.PDQQueryManager;
import org.iexhub.connectors.PIXManager;
import org.iexhub.exceptions.PatientIdParamMissingException;
import org.iexhub.exceptions.UnexpectedServerException;

import PDQSupplier.org.hl7.v3.AD;
import PDQSupplier.org.hl7.v3.COCTMT150003UV03Organization;
import PDQSupplier.org.hl7.v3.ON;
import PDQSupplier.org.hl7.v3.ObjectFactory;
import PDQSupplier.org.hl7.v3.PN;
import PDQSupplier.org.hl7.v3.PRPAIN201306UV02;
import PDQSupplier.org.hl7.v3.PRPAIN201306UV02MFMIMT700711UV01Subject1;
import PDQSupplier.org.hl7.v3.PRPAMT201310UV02Person;
import PDQSupplier.org.hl7.v3.ST;
import PIXManager.org.hl7.v3.MCCIIN000002UV01;
import ca.uhn.fhir.jaxrs.server.AbstractJaxRsResourceProvider;
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.AddressDt;
import ca.uhn.fhir.model.dstu2.composite.HumanNameDt;
import ca.uhn.fhir.model.dstu2.resource.Condition;
import ca.uhn.fhir.model.dstu2.resource.Parameters;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.StringDt;
import ca.uhn.fhir.rest.annotation.ConditionalUrlParam;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.AddProfileTagEnum;
import ca.uhn.fhir.rest.server.BundleInclusionRule;
import ca.uhn.fhir.rest.server.Constants;
import ca.uhn.fhir.rest.server.ETagSupportEnum;
import ca.uhn.fhir.rest.server.FifoMemoryPagingProvider;
import ca.uhn.fhir.rest.server.IPagingProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;

/**
 * @author 
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
	
	/**
	 * The HAPI paging provider for this server
	 */
	public static final IPagingProvider PAGE_PROVIDER;
	
	static final String PATH = "/Patient";
	private static final ConcurrentHashMap<String, List<Patient>> patients = new ConcurrentHashMap<String, List<Patient>>();

	static {
		PAGE_PROVIDER = new FifoMemoryPagingProvider(10);
	}

//	static {
//		patients.put(String.valueOf(counter), createPatient("Van Houte"));
//		patients.put(String.valueOf(counter), createPatient("Agnew"));
//		for (int i = 0; i < 20; i++) {
//			patients.put(String.valueOf(counter), createPatient("Random Patient " + counter));
//		}
//	}

	public JaxRsPatientRestProvider() {
		super(JaxRsPatientRestProvider.class);
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
	
	@Create
	public MethodOutcome create(@ResourceParam final Patient patient, @ConditionalUrlParam String theConditional) throws Exception
	{
		log.info("Entered FHIR Patient create service");
		
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
			MCCIIN000002UV01 pixRegistrationResponse = pixManager.registerPatient((patient.getName() != null) ? patient.getName().get(0).getGivenAsSingleString()
							: null,
					(patient.getName() != null) ? patient.getName().get(0).getFamilyAsSingleString()
							: null,
					null,
					(patient.getBirthDate() != null) ? patient.getBirthDateElement().getValueAsString()
							: null,
					(patient.getGender() == null) ? ""
							: (patient.getGender().compareToIgnoreCase(AdministrativeGenderEnum.MALE.getCode()) == 0) ? "M"
									: ((patient.getGender().compareToIgnoreCase(AdministrativeGenderEnum.FEMALE.getCode()) == 0) ? "F"
											: ((patient.getGender().compareToIgnoreCase(AdministrativeGenderEnum.OTHER.getCode()) == 0) ? "UN"
													: "")),
					(patient.getIdentifier() != null) ? patient.getIdentifier().get(0).getValue()
							: null);
			
			if ((pixRegistrationResponse.getAcceptAckCode().getCode() == "CA") ||
				(pixRegistrationResponse.getAcceptAckCode().getCode() == "AA"))
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

	@Delete
	public MethodOutcome delete(@IdParam final IdDt theId) throws Exception {
		final Patient deletedPatient = find(theId);
		patients.remove(deletedPatient.getId().getIdPart());
		final MethodOutcome result = new MethodOutcome().setCreated(true);
		result.setResource(deletedPatient);
		return result;
	}

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
					ST test = (ST) ((PDQSupplier.org.hl7.v3.AdxpCity)testNameComponent.getValue()).getContent().get(0);
					fhirAddr.setCity(test.toString());
				}
				else
				if (testNameComponent.getValue().getClass() == PDQSupplier.org.hl7.v3.AdxpState.class)
				{
					ST test = (ST) ((PDQSupplier.org.hl7.v3.AdxpState)testNameComponent.getValue()).getContent().get(0);
					fhirAddr.setState(test.toString());
				}
				else
				if (testNameComponent.getValue().getClass() == PDQSupplier.org.hl7.v3.AdxpPostalCode.class)
				{
					ST test = (ST) ((PDQSupplier.org.hl7.v3.AdxpPostalCode)testNameComponent.getValue()).getContent().get(0);
					fhirAddr.setPostalCode(test.toString());
				}
				else
				if (testNameComponent.getValue().getClass() == PDQSupplier.org.hl7.v3.AdxpCountry.class)
				{
					ST test = (ST) ((PDQSupplier.org.hl7.v3.AdxpCountry)testNameComponent.getValue()).getContent().get(0);
					fhirAddr.setCountry(test.toString());
				}
				else
				if (testNameComponent.getValue().getClass() == PDQSupplier.org.hl7.v3.AdxpStreetAddressLine.class)
				{
					ST test = (ST) ((PDQSupplier.org.hl7.v3.AdxpStreetAddressLine)testNameComponent.getValue()).getContent().get(0);
					fhirAddr.addLine(test.toString());
				}
			}
		}
		
		return fhirAddr;
	}
	
	private Patient populatePatientObject(PRPAIN201306UV02 queryResponse)
	{
		Patient retVal = new Patient();
		boolean nameFound = false;
		boolean addressFound = false;
		boolean providerFound = false;
		boolean genderFound = false;
		boolean birthDateFound = false;
		
		/////////////////////////////////////////////////////////////////////////////////////
		// Create PatientPerson...
		ObjectFactory objectFactory = new PDQSupplier.org.hl7.v3.ObjectFactory();
		PRPAMT201310UV02Person patientPersonTest = new PRPAMT201310UV02Person();
		
		PDQSupplier.org.hl7.v3.EnFamily enFamily = new PDQSupplier.org.hl7.v3.EnFamily();
		enFamily.getContent().add("Alpha");
		PDQSupplier.org.hl7.v3.EnGiven enGiven = new PDQSupplier.org.hl7.v3.EnGiven();
		enGiven.getContent().add("Alan");
		
		PN patientName = new PN();
		patientName.getContent().add(objectFactory.createENFamily(enFamily));
		
		if (enGiven != null)
		{
			patientName.getContent().add(objectFactory.createENGiven(enGiven));
		}
		
		patientPersonTest.getName().add(patientName);
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
			if ((subject.getRegistrationEvent() != null) &&
				(subject.getRegistrationEvent().getSubject1() != null) &&
				(subject.getRegistrationEvent().getSubject1().getPatient() != null) &&
				(subject.getRegistrationEvent().getSubject1().getPatient().getPatientPerson() != null))
			{
				PRPAMT201310UV02Person patientPerson = subject.getRegistrationEvent().getSubject1().getPatient().getPatientPerson().getValue(); 

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
								ST test = (ST) ((PDQSupplier.org.hl7.v3.EnFamily)testNameComponent.getValue()).getContent().get(0);
								fhirName.addFamily(test.toString());
							}
							else
							if (testNameComponent.getValue().getClass() == PDQSupplier.org.hl7.v3.EnGiven.class)
							{
								ST test = (ST) ((PDQSupplier.org.hl7.v3.EnGiven)testNameComponent.getValue()).getContent().get(0);
								fhirName.addGiven(test.toString());
							}
							
							nameFound = true;
						}
						
						retVal.addName(fhirName);
					}
				}
				
				// Extract gender if present...
				if (patientPerson.getAdministrativeGenderCode() != null)
				{
					retVal.setGender((patientPerson.getAdministrativeGenderCode().getCode().compareToIgnoreCase("M") == 0) ? AdministrativeGenderEnum.MALE
							: (patientPerson.getAdministrativeGenderCode().getCode().compareToIgnoreCase("F") == 0) ? AdministrativeGenderEnum.FEMALE
									: AdministrativeGenderEnum.UNKNOWN);
					genderFound = true;
				}
				
				// Extract birth date if present...
				if (patientPerson.getBirthTime() != null)
				{
					DateDt birthDate = new DateDt();
					birthDate.setValueAsString(patientPerson.getBirthTime().getValue());
					retVal.setBirthDate(birthDate);
					birthDateFound = true;
				}
				
				// Extract address if present...
				if (patientPerson.getAddr() != null)
				{
					retVal.addAddress(populateFhirAddress(patientPerson));
				}
			}
			
			if ((subject.getRegistrationEvent() != null) &&
				(subject.getRegistrationEvent().getSubject1() != null) &&
				(subject.getRegistrationEvent().getSubject1().getPatient() != null) &&
				(subject.getRegistrationEvent().getSubject1().getPatient().getProviderOrganization() != null))
			{
				COCTMT150003UV03Organization provider = subject.getRegistrationEvent().getSubject1().getPatient().getProviderOrganization().getValue(); 
				String providerName = null;
				String providerTelecom = null;
				
				// Create Organization FHIR resource...
//				Organization organization = new Organization();
//				organization.setId(arg0);
//				organization.setIdentifier(theValue)

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
								providerName = testNameComponent.getValue().toString();
								break;
							}
						}
					}
				}
					
//				if (provider.getContactParty() != null)
//				{
//					for (COCTMT150003UV03ContactParty contact : provider.getContactParty())
//					{
//						if (contact.getAddr() != null)
//						{
//							retVal.addAddress(populateFhirAddress(patientPerson));
//						}
//					}
//				}

					retVal.addCareProvider().setDisplay(providerName);
			
			}
		}
		return retVal;
	}
	
	@Read
	public Patient find(@IdParam final IdDt id) throws Exception
	{
		log.info("Entered FHIR Patient find (by ID) service");
		
		if (id == null)
		{
			throw new PatientIdParamMissingException("Patient ID parameter missing");
		}
		
		if (props == null)
		{
			if (props == null)
			{
				loadProperties();
			}
		}

		Patient result = null;
		
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
				// TESTING ONLY ///////////////////////////////////////////////////////////////
				populatePatientObject(null);
				///////////////////////////////////////////////////////////////////////////////
				
				
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
						id.getIdPart().toString(),
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
					result = populatePatientObject(pdqQueryResponse);
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

	@Read(version = true)
	public Patient findHistory(@IdParam final IdDt theId) {
		if (patients.containsKey(theId.getIdPart())) {
			final List<Patient> list = patients.get(theId.getIdPart());
			for (final Patient patient : list) {
				if (patient.getId().getVersionIdPartAsLong().equals(theId.getVersionIdPartAsLong())) {
					return patient;
				}
			}
		}
		throw new ResourceNotFoundException(theId);
	}

	@Operation(name = "firstVersion", idempotent = true, returnParameters = { @OperationParam(name = "return", type = StringDt.class) })
	public Parameters firstVersion(@IdParam final IdDt theId, @OperationParam(name = "dummy") StringDt dummyInput) throws Exception {
		Parameters parameters = new Parameters();
		Patient patient = find(new IdDt(theId.getResourceType(), theId.getIdPart(), "0"));
		parameters.addParameter().setName("return").setResource(patient).setValue(new StringDt((1) + "" + "inputVariable [ " + dummyInput.getValue() + "]"));
		return parameters;
	}

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

	@GET
	@Path("/{id}/$firstVersion")
	public Response operationFirstVersionUsingGet(@PathParam("id") String id) throws IOException {
		return customOperation(null, RequestTypeEnum.GET, id, "$firstVersion", RestOperationTypeEnum.EXTENDED_OPERATION_INSTANCE);
	}

	@POST
	@Path("/{id}/$firstVersion")
	public Response operationFirstVersionUsingGet(@PathParam("id") String id, final String resource) throws Exception {
		return customOperation(resource, RequestTypeEnum.POST, id, "$firstVersion", RestOperationTypeEnum.EXTENDED_OPERATION_INSTANCE);
	}

	@Search
	public List<Patient> search(@RequiredParam(name = Patient.SP_FAMILY) final StringParam familyName,
			@RequiredParam(name = Patient.SP_GIVEN) final StringParam givenName,
			@OptionalParam(name = Patient.SP_GENDER) final StringParam gender,
			@OptionalParam(name = Patient.SP_BIRTHDATE) final StringParam birthDate,
			@OptionalParam(name = Patient.SP_ADDRESS) final StringParam addressLine,
			@OptionalParam(name = Patient.SP_ADDRESS_CITY) final StringParam addressCity,
			@OptionalParam(name = Patient.SP_ADDRESS_STATE) final StringParam addressState,
			@OptionalParam(name = Patient.SP_ADDRESS_POSTALCODE) final StringParam addressPostalCode)
	{
		// TBD
		
		final List<Patient> result = new LinkedList<Patient>();
		return result;
	}

	@Search(compartmentName = "Condition")
	public List<IResource> searchCompartment(@IdParam IdDt thePatientId) {
		List<IResource> retVal = new ArrayList<IResource>();
		Condition condition = new Condition();
		condition.setId(new IdDt("665577"));
		retVal.add(condition);
		return retVal;
	}

	@Update
	public MethodOutcome update(@IdParam final IdDt theId, @ResourceParam final Patient patient) {
		final String idPart = theId.getIdPart();
		if (patients.containsKey(idPart)) {
			final List<Patient> patientList = patients.get(idPart);
			final Patient lastPatient = getLast(patientList);
			patient.setId(createId(theId.getIdPartAsLong(), lastPatient.getId().getVersionIdPartAsLong() + 1));
			patientList.add(patient);
			final MethodOutcome result = new MethodOutcome().setCreated(false);
			result.setResource(patient);
			result.setId(patient.getId());
			return result;
		} else {
			throw new ResourceNotFoundException(theId);
		}
	}

	private static IdDt createId(final Long id, final Long theVersionId) {
		return new IdDt("Patient", "" + id, "" + theVersionId);
	}

	private static List<Patient> createPatient(final Patient patient) {
		patient.setId(createId(1L, 1L));
		final LinkedList<Patient> list = new LinkedList<Patient>();
		list.add(patient);
		return list;
	}

	private static List<Patient> createPatient(final String name) {
		final Patient patient = new Patient();
		patient.getNameFirstRep().addFamily(name);
		return createPatient(patient);
	}

}
