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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.iexhub.config.IExHubConfig;
import org.iexhub.connectors.XdsB;
import org.iexhub.connectors.XdsBRepositoryManager;
import org.iexhub.exceptions.ContractIdParamMissingException;
import org.iexhub.exceptions.DocumentRegistryErrorException;
import org.iexhub.exceptions.InvalidDocumentTypeException;
import org.iexhub.exceptions.MultipleResourcesFoundException;
import org.iexhub.exceptions.UnexpectedServerException;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.AdhocQueryResponse;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.ClassificationType;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.ExternalIdentifierType;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.ExtrinsicObjectType;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.IdentifiableType;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.RegistryError_type0;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.RegistryObjectListType;
import org.iexhub.services.client.DocumentRepository_ServiceStub;
import org.iexhub.services.client.DocumentRepository_ServiceStub.DocumentResponse_type0;
import org.iexhub.services.client.DocumentRepository_ServiceStub.RetrieveDocumentSetResponse;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jaxrs.server.AbstractJaxRsResourceProvider;
import ca.uhn.fhir.model.dstu2.composite.IdentifierDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
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

/**
 * FHIR Contract Implementation supports: create, update, search, find.
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
	private static XdsB xdsB = null;

	private static boolean testMode = false;
	private static String cdaToJsonTransformXslt = null;
	private static String iExHubDomainOid = "2.16.840.1.113883.3.72.5.9.1";
	private static String xdsBRepositoryUniqueId = "1.3.6.1.4.1.21367.13.40.216";
	private static String iExHubAssigningAuthority = "ISO";
	private static String xdsBRegistryEndpointURI = null;
	private static String xdsBRepositoryEndpointURI = null;
	private static FhirContext fhirCtxt = new FhirContext();
	private static String privacyConsentClassificationType = "uuid:f0306f51-975f-434e-a61c-c59651d33983";
	private final String testOutputPath;

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
		loadProperties();
		this.testOutputPath = IExHubConfig.getProperty("TestOutputPath");
		assert StringUtils.isNotBlank(this.testOutputPath) : "'TestOutputPath' property must be configured";
	}

	private void loadProperties()
	{
		JaxRsContractRestProvider.testMode = IExHubConfig.getProperty("TestMode", JaxRsContractRestProvider.testMode);
		JaxRsContractRestProvider.cdaToJsonTransformXslt = IExHubConfig.getProperty("CDAToJSONTransformXSLT", JaxRsContractRestProvider.cdaToJsonTransformXslt);
		JaxRsContractRestProvider.iExHubDomainOid = IExHubConfig.getProperty("IExHubDomainOID", JaxRsContractRestProvider.iExHubDomainOid);
		JaxRsContractRestProvider.iExHubAssigningAuthority = IExHubConfig.getProperty("IExHubAssigningAuthority", JaxRsContractRestProvider.iExHubAssigningAuthority);
		JaxRsContractRestProvider.xdsBRepositoryEndpointURI = IExHubConfig.getProperty("XdsBRepositoryEndpointURI", JaxRsContractRestProvider.xdsBRepositoryEndpointURI);
		JaxRsContractRestProvider.xdsBRepositoryUniqueId = IExHubConfig.getProperty("XdsBRepositoryUniqueId", JaxRsContractRestProvider.xdsBRepositoryUniqueId);
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

		MethodOutcome result = new MethodOutcome().setCreated(false);
	
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
				result.setCreated(true);
				result.setId(contract.getId());
				result.setResource(contract);
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
	 * Contract search
	 * @param identifier
	 * @return
	 * @throws Exception
	 */
	@Search
	public List<Contract> search(@RequiredParam(name = Patient.SP_IDENTIFIER) final IdentifierDt identifier) throws Exception
	{
		log.info("Entered FHIR Contract search service");

		List<Contract> result = null;
		
		try
		{
			if (xdsB == null)
			{
				log.info("Instantiating XdsB connector...");
				xdsB = new XdsB(null,
						null,
						false);
				log.info("XdsB connector successfully started");
			}
		}
		catch (Exception e)
		{
			log.error("Error encountered instantiating XdsB connector, "
					+ e.getMessage());
			throw new UnexpectedServerException("Error - " + e.getMessage());
		}

		try
		{
			// Determine if a complete patient ID (including OID and ISO specification) was provided.  If not, then append IExHubDomainOid and IExAssigningAuthority...
			String referencedId = identifier.getValue();
			if (!referencedId.contains("^^^&"))
			{
				referencedId = "'"
						+ referencedId
						+ "^^^&"
						+ identifier.getSystem()
						+ "&"
						+ iExHubAssigningAuthority
						+ "'";
			}
			
			AdhocQueryResponse registryResponse = xdsB.registryStoredQuery(referencedId,
					null,
					null
					/*"'57016-8'"*/);
			
			log.info("Call to XdsB registry successful");

			// Determine if registry server returned any errors...
			if ((registryResponse.getRegistryErrorList() != null) &&
				(registryResponse.getRegistryErrorList().getRegistryError().length > 0))
			{
				for (RegistryError_type0 error : registryResponse.getRegistryErrorList().getRegistryError())
				{
					StringBuilder errorText = new StringBuilder();
					if (error.getErrorCode() != null)
					{
						errorText.append("Error code=" + error.getErrorCode() + "\n");
					}
					if (error.getCodeContext() != null)
					{
						errorText.append("Error code context=" + error.getCodeContext() + "\n");
					}
					

					if (error.getLocation() != null)
					{
						errorText.append("Error location=" + error.getLocation());
					}
					
					log.error(errorText.toString());
					throw new DocumentRegistryErrorException(errorText.toString());
				}
			}

			// Try to retrieve documents...
			boolean documentTypeFound = false;
			RegistryObjectListType registryObjectList = registryResponse.getRegistryObjectList();
			IdentifiableType[] documentObjects = registryObjectList.getIdentifiable();
			if ((documentObjects != null) &&
				(documentObjects.length > 0))
			{
				log.info("Documents found in the registry, retrieving them from the repository...");
				
				HashMap<String, String> documentUniqueIds = new HashMap<String, String>();
				HashMap<String, String> documentUuids = new HashMap<String, String>();
				for (IdentifiableType identifiable : documentObjects)
				{
					if (identifiable.getClass().equals(ExtrinsicObjectType.class))
					{
						// Determine if this is the privacy consent document type...
						ClassificationType[] classifications = ((ExtrinsicObjectType)identifiable).getClassification();
						for (ClassificationType classification : classifications)
						{
							if (classification.getClassificationScheme().getReferenceURI().getPath().compareToIgnoreCase(privacyConsentClassificationType) == 0)
							{
								documentTypeFound = true;
								break;
							}
						}
						
						if (documentTypeFound)
						{
							// Determine if the "home" attribute (homeCommunityId in XCA parlance) is present...
							String home = ((((ExtrinsicObjectType)identifiable).getHome() != null) && (((ExtrinsicObjectType)identifiable).getHome().getPath().length() > 0)) ? ((ExtrinsicObjectType)identifiable).getHome().getPath()
									: null;
	
							ExternalIdentifierType[] externalIdentifiers = ((ExtrinsicObjectType)identifiable).getExternalIdentifier();
							
							// Find the ExternalIdentifier that has the "XDSDocumentEntry.uniqueId" value...
							String uniqueId = null;
							for (ExternalIdentifierType externalIdentifier : externalIdentifiers)
							{
								String val = externalIdentifier.getName().getInternationalStringTypeSequence()[0].getLocalizedString().getValue().getFreeFormText();
								if ((val != null) &&
									(val.compareToIgnoreCase("XDSDocumentEntry.uniqueId") == 0))
								{
									log.info("Located XDSDocumentEntry.uniqueId ExternalIdentifier, uniqueId="
											+ uniqueId);
									uniqueId = externalIdentifier.getValue().getLongName();
									break;
								}
							}
							
							if (uniqueId != null)
							{
								documentUniqueIds.put(uniqueId,
										home);
								documentUuids.put(uniqueId,
										((ExtrinsicObjectType)identifiable).getId().getPath().replace("uuid:", ""));
								log.info("Document ID added: "
										+ uniqueId
										+ ", homeCommunityId: "
										+ home
										+", UUID: "
										+ ((ExtrinsicObjectType)identifiable).getId().getPath().replace("uuid:", ""));
							}
						}
						else
						{
							String home = ((identifiable.getHome() != null) && (identifiable.getHome().getPath().length() > 0)) ? identifiable.getHome().getPath()
									: null;
							documentUniqueIds.put(identifiable.getId().getPath(),
									home);
							log.info("Document ID added: "
									+ identifiable.getId().getPath()
									+ ", homeCommunityId: "
									+ home);
						}
					}
				}
				
				if (documentTypeFound)
				{
					log.info("Invoking XdsB repository connector retrieval...");
					RetrieveDocumentSetResponse documentSetResponse = xdsB.retrieveDocumentSet(xdsBRepositoryUniqueId,
							documentUniqueIds,
							referencedId);
					log.info("XdsB repository connector retrieval succeeded");
	
					if ((documentSetResponse.getRetrieveDocumentSetResponse().getRegistryResponse().getRegistryErrorList() != null) &&
						(documentSetResponse.getRetrieveDocumentSetResponse().getRegistryResponse().getRegistryErrorList().getRegistryError().length > 0))
					{
						for (DocumentRepository_ServiceStub.RegistryError_type0 error : documentSetResponse.getRetrieveDocumentSetResponse().getRegistryResponse().getRegistryErrorList().getRegistryError())
						{ 
							StringBuilder errorText = new StringBuilder();
							if (error.getErrorCode() != null)
							{
								errorText.append("Error code=" + error.getErrorCode() + "\n");
							}
							if (error.getCodeContext() != null)
							{
								errorText.append("Error code context=" + error.getCodeContext() + "\n");
							}
							

							if (error.getLocation() != null)
							{
								errorText.append("Error location=" + error.getLocation());
							}
							
							log.error(errorText.toString());
						}
					}
					else
					{
						result = new ArrayList<Contract>();
						
						if (documentSetResponse.getRetrieveDocumentSetResponse().getRetrieveDocumentSetResponseTypeSequence_type0() != null)
						{
							DocumentResponse_type0[] docResponseArray = documentSetResponse.getRetrieveDocumentSetResponse().getRetrieveDocumentSetResponseTypeSequence_type0().getDocumentResponse();
							if (docResponseArray != null)
							{
								try
								{
									for (DocumentResponse_type0 document : docResponseArray)
									{
										log.info("Processing document ID="
												+ document.getDocumentUniqueId().getLongName());
										
										String mimeType = document.getMimeType().getLongName();
										if (mimeType.compareToIgnoreCase("text/xml") == 0)
										{
											final String filename = this.testOutputPath + "/" + document.getDocumentUniqueId().getLongName() + ".xml";
											log.info("Persisting document to filesystem, filename="
													+ filename);
											DataHandler dh = document.getDocument();
											File file = new File(filename);
											FileOutputStream fileOutStream = new FileOutputStream(file);
											dh.writeTo(fileOutStream);
											fileOutStream.close();

											IParser xmlParser = fhirCtxt.newXmlParser();
											Contract contract = (Contract)xmlParser.parseResource(new InputStreamReader(new FileInputStream(filename)));
											contract.setId(documentUuids.get(document.getDocumentUniqueId().getLongName()));
											result.add(contract);
										}
										else
										{
											log.info("Document retrieved is not XML - document ID="
													+ document.getDocumentUniqueId().getLongName());
										}
									}
								}
								catch (Exception e)
								{
									log.error("Error encountered, "
											+ e.getMessage());
									throw e;
								}
							}
						}
					}
				}
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

		try
		{
			if (xdsB == null)
			{
				log.info("Instantiating XdsB connector...");
				xdsB = new XdsB(null,
						null,
						false);
				log.info("XdsB connector successfully started");
			}
		}
		catch (Exception e)
		{
			log.error("Error encountered instantiating XdsB connector, "
					+ e.getMessage());
			throw new UnexpectedServerException("Error - " + e.getMessage());
		}

		Contract result = null;
		HashMap<String, String> documentUniqueIds = new HashMap<String, String>();
		HashMap<String, String> documentUuids = new HashMap<String, String>();
		documentUniqueIds.put(id.getIdPart(),
				null);

		log.info("Invoking XdsB repository connector retrieval...");
		RetrieveDocumentSetResponse documentSetResponse = xdsB.retrieveDocumentSet(xdsBRepositoryUniqueId,
				documentUniqueIds,
				null);
		log.info("XdsB repository connector retrieval succeeded");

		if ((documentSetResponse.getRetrieveDocumentSetResponse().getRegistryResponse().getRegistryErrorList() != null) &&
			(documentSetResponse.getRetrieveDocumentSetResponse().getRegistryResponse().getRegistryErrorList().getRegistryError().length > 0))
		{
			for (DocumentRepository_ServiceStub.RegistryError_type0 error : documentSetResponse.getRetrieveDocumentSetResponse().getRegistryResponse().getRegistryErrorList().getRegistryError())
			{ 
				StringBuilder errorText = new StringBuilder();
				if (error.getErrorCode() != null)
				{
					errorText.append("Error code=" + error.getErrorCode() + "\n");
				}
				if (error.getCodeContext() != null)
				{
					errorText.append("Error code context=" + error.getCodeContext() + "\n");
				}
				
				if (error.getLocation() != null)
				{
					errorText.append("Error location=" + error.getLocation());
				}
				
				log.error(errorText.toString());
			}
		}
		else
		{
			result = new Contract();
			
			if (documentSetResponse.getRetrieveDocumentSetResponse().getRetrieveDocumentSetResponseTypeSequence_type0() != null)
			{
				DocumentResponse_type0[] docResponseArray = documentSetResponse.getRetrieveDocumentSetResponse().getRetrieveDocumentSetResponseTypeSequence_type0().getDocumentResponse();
				if (docResponseArray != null)
				{
					try
					{
						if (docResponseArray.length > 1)
						{
							log.error(String.format("Multiple contract resources found when only one expected during find operation, resource count=%d",
									docResponseArray.length));
							throw new MultipleResourcesFoundException(String.format("Multiple contract resources found when only one expected during find operation, resource count=%d",
									docResponseArray.length));
						}
						
						DocumentResponse_type0 document = docResponseArray[0];
						log.info("Processing document ID="
								+ document.getDocumentUniqueId().getLongName());
						
						String mimeType = document.getMimeType().getLongName();
						if (mimeType.compareToIgnoreCase("text/xml") == 0)
						{
							final String filename = this.testOutputPath + "/" + document.getDocumentUniqueId().getLongName() + ".xml";
							log.info("Persisting document to filesystem, filename="
									+ filename);
							DataHandler dh = document.getDocument();
							File file = new File(filename);
							FileOutputStream fileOutStream = new FileOutputStream(file);
							dh.writeTo(fileOutStream);
							fileOutStream.close();

							IParser xmlParser = fhirCtxt.newXmlParser();
							result = (Contract)xmlParser.parseResource(new InputStreamReader(new FileInputStream(filename)));
							
							ResourceReferenceDt consentSubjectRef = result.getSubject().get(0);
							IBaseResource referencedSubject = consentSubjectRef.getResource();
							String referencedId = referencedSubject.getIdElement().getIdPart();

							// Need to query the registry to get the document UUID (XDSDocumentEntry. entryUUID).  This value is required for document replacement.
							AdhocQueryResponse registryResponse = xdsB.registryStoredQuery("'"
										+ referencedId
										+ "^^^&"
										+ iExHubDomainOid
										+ "&"
										+ iExHubAssigningAuthority
										+ "'",
									null,
									null,
									null,
									"'" + id.getIdPart() + "'");
							
							log.info("Call to XdsB registry successful");

							// Determine if registry server returned any errors...
							if ((registryResponse.getRegistryErrorList() != null) &&
								(registryResponse.getRegistryErrorList().getRegistryError().length > 0))
							{
								for (RegistryError_type0 error : registryResponse.getRegistryErrorList().getRegistryError())
								{
									StringBuilder errorText = new StringBuilder();
									if (error.getErrorCode() != null)
									{
										errorText.append("Error code=" + error.getErrorCode() + "\n");
									}
									if (error.getCodeContext() != null)
									{
										errorText.append("Error code context=" + error.getCodeContext() + "\n");
									}
									
									if (error.getLocation() != null)
									{
										errorText.append("Error location=" + error.getLocation());
									}
									
									log.error(errorText.toString());
									throw new DocumentRegistryErrorException(errorText.toString());
								}
							}
							
							// One document should be present...
							boolean documentTypeFound = false;
							RegistryObjectListType registryObjectList = registryResponse.getRegistryObjectList();
							IdentifiableType[] documentObjects = registryObjectList.getIdentifiable();
							if ((documentObjects != null) &&
								(documentObjects.length == 1))
							{
								log.info("Document found in the registry, retrieving XDSDocumentEntry. entryUUID...");
								
								if (documentObjects[0].getClass().equals(ExtrinsicObjectType.class))
								{
									// Determine if this is the privacy consent document type...
									ClassificationType[] classifications = ((ExtrinsicObjectType)documentObjects[0]).getClassification();
									for (ClassificationType classification : classifications)
									{
										if (classification.getClassificationScheme().getReferenceURI().getPath().compareToIgnoreCase(privacyConsentClassificationType) == 0)
										{
											documentTypeFound = true;
											break;
										}
									}
									
									if (documentTypeFound)
									{
										// Set FHIR resource ID to document UUID...
										result.setId(((ExtrinsicObjectType)documentObjects[0]).getId().getPath().replace("uuid:", ""));
										log.info("Found document UUID: "
												+ ((ExtrinsicObjectType)documentObjects[0]).getId().getPath().replace("uuid:", ""));
									}
									else
									{
										log.error(String.format("Requested document is of invalid type, expected classification type %s",
												privacyConsentClassificationType));
										throw new InvalidDocumentTypeException(String.format("Requested document is of invalid type, expected classification type %s",
												privacyConsentClassificationType));
									}
								}
							}
						}
						else
						{
							log.info("Document retrieved is not XML - document ID="
									+ document.getDocumentUniqueId().getLongName());
						}
					}
					catch (Exception e)
					{
						log.error("Error encountered, "
								+ e.getMessage());
						throw e;
					}
				}
			}
		}
			
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
	
	@Update
	public MethodOutcome update(@ResourceParam final Contract contract, @ConditionalUrlParam String theConditional) throws Exception
	{
		log.info("Entered FHIR Contract update service");

		MethodOutcome result = new MethodOutcome().setCreated(false);
		
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
					"text/xml",
					true);
			
			if ((response.getRegistryErrorList() == null) || (response.getRegistryErrorList().getRegistryError().isEmpty()))
			{
				result.setCreated(true);
				result.setId(contract.getId());
				result.setResource(contract);
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
}
