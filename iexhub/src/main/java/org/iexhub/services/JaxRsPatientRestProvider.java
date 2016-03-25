package org.iexhub.services;

import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.IOException;
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

import org.apache.log4j.Logger;
import org.iexhub.connectors.PIXManager;
import org.iexhub.exceptions.UnexpectedServerException;

import PIXManager.org.hl7.v3.MCCIIN000002UV01;
import ca.uhn.fhir.jaxrs.server.AbstractJaxRsResourceProvider;
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.resource.Condition;
import ca.uhn.fhir.model.dstu2.resource.Parameters;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.StringDt;
import ca.uhn.fhir.rest.annotation.ConditionalUrlParam;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
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

	private static Properties props = null;
	private static boolean testMode = false;
	private static String propertiesFile = "/temp/IExHub.properties";
	private static String repositoryUniqueId = "1.19.6.24.109.42.1.5";
	private static String cdaToJsonTransformXslt = null;
	private static String iExHubDomainOid = "2.16.840.1.113883.3.72.5.9.1";
	private static String iExHubAssigningAuthority = "ISO";
	
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

	@Create
	public MethodOutcome create(@ResourceParam final Patient patient, @ConditionalUrlParam String theConditional) throws Exception
	{
		log.info("Entered FHIR create service");
		
		if (props == null)
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

		// Extract patient name and make ITI-44 transaction...
		String retVal = "";
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
			// ITI-44-Source-Feed message, performing FHIR gender translation...
			try
			{					
				MCCIIN000002UV01 pixRegistrationResponse = pixManager.registerPatient((patient.getName() != null) ? patient.getName().get(0).getGivenAsSingleString()
								: null,
						(patient.getName() != null) ? patient.getName().get(0).getFamilyAsSingleString()
								: null,
						null,
						(patient.getBirthDate() != null) ? patient.getBirthDateElement().getValueAsString()
								: null,
						(patient.getGender().compareToIgnoreCase(AdministrativeGenderEnum.MALE.getCode()) == 0) ? "M"
								: ((patient.getGender().compareToIgnoreCase(AdministrativeGenderEnum.FEMALE.getCode()) == 0) ? "F"
										: "UN"),
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
				fail("Error - " + e.getMessage());
			}
		}
		catch (Exception e)
		{
			log.error("Error encountered, "
					+ e.getMessage());
			throw new UnexpectedServerException("Error - " + e.getMessage());
		}

		return result;
	}

	@Delete
	public MethodOutcome delete(@IdParam final IdDt theId) {
		final Patient deletedPatient = find(theId);
		patients.remove(deletedPatient.getId().getIdPart());
		final MethodOutcome result = new MethodOutcome().setCreated(true);
		result.setResource(deletedPatient);
		return result;
	}

	@Read
	public Patient find(@IdParam final IdDt theId) {
		if (patients.containsKey(theId.getIdPart())) {
			return getLast(patients.get(theId.getIdPart()));
		} else {
			throw new ResourceNotFoundException(theId);
		}
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
	public Parameters firstVersion(@IdParam final IdDt theId, @OperationParam(name = "dummy") StringDt dummyInput) {
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
	public List<Patient> search(@RequiredParam(name = Patient.SP_NAME) final StringParam name) {
		final List<Patient> result = new LinkedList<Patient>();
		for (final List<Patient> patientIterator : patients.values()) {
			Patient single = null;
			for (Patient patient : patientIterator) {
				if (name == null || patient.getNameFirstRep().getFamilyFirstRep().getValueNotNull().equals(name.getValueNotNull())) {
					single = patient;
				}
			}
			if (single != null) {
				result.add(single);
			}
		}
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
