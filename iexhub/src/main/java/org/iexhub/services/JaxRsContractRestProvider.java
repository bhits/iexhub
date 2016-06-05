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
package org.iexhub.services;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.log4j.Logger;
import org.iexhub.connectors.XdsBRepositoryManager;
import org.iexhub.exceptions.ContractIdParamMissingException;
import org.iexhub.exceptions.UnexpectedServerException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jaxrs.server.AbstractJaxRsResourceProvider;
import ca.uhn.fhir.model.dstu2.resource.Contract;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.AddProfileTagEnum;
import ca.uhn.fhir.rest.server.BundleInclusionRule;
import ca.uhn.fhir.rest.server.Constants;
import ca.uhn.fhir.rest.server.ETagSupportEnum;
import ca.uhn.fhir.rest.server.IPagingProvider;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;
import org.apache.log4j.Logger;
import org.iexhub.connectors.XdsBRepositoryManager;
import org.iexhub.exceptions.ContractIdParamMissingException;
import org.iexhub.exceptions.UnexpectedServerException;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * FHIR Contract Implementation supports: create, update.
 * @author A. Sute
 */
@Local
@Path(JaxRsContractRestProvider.PATH)
@Stateless
@Produces({ MediaType.APPLICATION_JSON, Constants.CT_FHIR_JSON, Constants.CT_FHIR_XML })
public class JaxRsContractRestProvider extends AbstractJaxRsResourceProvider<Contract>
{
    /** Logger */
    public static Logger log = Logger.getLogger(JaxRsContractRestProvider.class);

	private static XdsBRepositoryManager xdsBRepositoryManager = null;

	private static Properties props = null;
	private static boolean testMode = false;
	private static String propertiesFile = "/temp/IExHub.properties";
	private static String cdaToJsonTransformXslt = null;
	private static String iExHubDomainOid = "2.16.840.1.113883.3.72.5.9.1";
	private static String iExHubAssigningAuthority = "ISO";
	private static String xdsBRegistryEndpointURI = null;
	private static String xdsBRepositoryEndpointURI = null;
	private static FhirContext fhirCtxt = new FhirContext();

	/**
	 * The HAPI paging provider for this server
	 */
	public static final IPagingProvider PAGE_PROVIDER;
	static
	{
		PAGE_PROVIDER = JaxRsContractPageProvider.PAGE_PROVIDER;
	}
	
	static final String PATH = "/Contract";

	public JaxRsContractRestProvider() {
		super(JaxRsContractRestProvider.class);
	}

	private void loadProperties()
	{
		try
		{
			props = new Properties();
			props.load(new FileInputStream(propertiesFile));
			JaxRsContractRestProvider.testMode = (props.getProperty("TestMode") == null) ? JaxRsContractRestProvider.testMode
					: Boolean.parseBoolean(props.getProperty("TestMode"));
			JaxRsContractRestProvider.cdaToJsonTransformXslt = (props.getProperty("CDAToJSONTransformXSLT") == null) ? JaxRsContractRestProvider.cdaToJsonTransformXslt
					: props.getProperty("CDAToJSONTransformXSLT");
			JaxRsContractRestProvider.iExHubDomainOid = (props.getProperty("IExHubDomainOID") == null) ? JaxRsContractRestProvider.iExHubDomainOid
					: props.getProperty("IExHubDomainOID");
			JaxRsContractRestProvider.iExHubAssigningAuthority = (props.getProperty("IExHubAssigningAuthority") == null) ? JaxRsContractRestProvider.iExHubAssigningAuthority
					: props.getProperty("IExHubAssigningAuthority");
			JaxRsContractRestProvider.xdsBRepositoryEndpointURI = (props.getProperty("XdsBRepositoryEndpointURI") == null) ? JaxRsContractRestProvider.xdsBRepositoryEndpointURI
					: props.getProperty("XdsBRepositoryEndpointURI");
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
	 * Contract create
	 * @param contract
	 * @param theConditional
	 * @return
	 * @throws Exception
	 */
	@Create
	public MethodOutcome create(@ResourceParam final Contract contract, @ConditionalUrlParam String theConditional) throws Exception
	{
		log.info("Entered FHIR Contract create service");
		
		if (props == null)
		{
			loadProperties();
		}

		MethodOutcome result = new MethodOutcome().setCreated(false);
		result.setResource(contract);
		
		try
		{
			if (xdsBRepositoryManager == null)
			{
				log.info("Instantiating XdsBRepositoryManager connector...");
				xdsBRepositoryManager = new XdsBRepositoryManager(null,
						null,
						false);
				log.info("XdsBRepositoryManager connector successfully started");
			}
		}
		catch (Exception e)
		{
			log.error("Error encountered instantiating XdsBRepositoryManager connector, "
					+ e.getMessage());
			throw new UnexpectedServerException("Error - " + e.getMessage());
		}

		try
		{
			// Serialize document to XML...
			IParser xmlParser = fhirCtxt.newXmlParser();
			xmlParser.setPrettyPrint(true);
			String xmlContent = xmlParser.encodeResourceToString(contract);
			
			// ITI-41 ProvideAndRegisterDocumentSet message...
			XdsBDocumentRepository.oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType response = xdsBRepositoryManager.provideAndRegisterDocumentSet(contract,
					xmlContent.getBytes(),
					"text/xml");
			
			if ((response.getRegistryErrorList() == null) || (response.getRegistryErrorList().getRegistryError().isEmpty()))
				
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

		log.info("Exiting FHIR Contract create service");
		return result;
	}
	
	/**
	 * Contract find
	 * @param id
	 * @return matching contract
	 * @throws Exception
	 */
	@Read
	public Contract find(@IdParam final IdDt id) throws Exception
	{
		if ((id == null) ||
			(id.isEmpty()))
		{
			throw new ContractIdParamMissingException("Contract ID parameter missing");
		}

		log.info(String.format("Entered FHIR Contract find (by ID) service, ID=%s",
				id.getValueAsString()));

		if (props == null)
		{
			if (props == null)
			{
				loadProperties();
			}
		}

		Contract result = null;

		// TBD...
		
		log.info("Exiting FHIR Contract find (by ID) service");
		return result;
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
	public Class<Contract> getResourceType() {
		return Contract.class;
	}

	@Override
	public boolean isDefaultPrettyPrint() {
		return true;
	}

	@Override
	public boolean isUseBrowserFriendlyContentTypes() {
		return true;
	}
	
//	@Update
//	public MethodOutcome update(@IdParam final IdDt theId, @ResourceParam final Patient patient)
//	{
//		final String idPart = theId.getIdPart();
//		if (patients.containsKey(idPart))
//		{
//			final List<Patient> patientList = patients.get(idPart);
//			final Patient lastPatient = getLast(patientList);
//			patient.setId(createId(theId.getIdPartAsLong(), lastPatient.getId().getVersionIdPartAsLong() + 1));
//			patientList.add(patient);
//			final MethodOutcome result = new MethodOutcome().setCreated(false);
//			result.setResource(patient);
//			result.setId(patient.getId());
//			return result;
//		}
//		else
//		{
//			throw new ResourceNotFoundException(theId);
//		}
//	}
}
