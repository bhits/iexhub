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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.iexhub.connectors.XdsB;
import org.iexhub.connectors.XdsBRepositoryManager;
import org.iexhub.exceptions.ContractIdParamMissingException;
import org.iexhub.exceptions.UnexpectedServerException;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.AdhocQueryResponse;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.ExternalIdentifierType;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.ExtrinsicObjectType;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.IdentifiableType;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.RegistryError_type0;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.RegistryObjectListType;
import org.iexhub.services.client.DocumentRepository_ServiceStub.DocumentResponse_type0;
import org.iexhub.services.client.DocumentRepository_ServiceStub.RetrieveDocumentSetResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jaxrs.server.AbstractJaxRsResourceProvider;
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.IdentifierDt;
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
	private static XdsB xdsB = null;

	private static Properties props = null;
	private static boolean testMode = false;
	private static String propertiesFile = "/temp/IExHub.properties";
	private static String cdaToJsonTransformXslt = null;
	private static String iExHubDomainOid = "2.16.840.1.113883.3.72.5.9.1";
	private static String xdsBRepositoryUniqueId = "1.3.6.1.4.1.21367.13.40.216";
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
			JaxRsContractRestProvider.xdsBRepositoryUniqueId = (props.getProperty("XdsBRepositoryUniqueId") == null) ? JaxRsContractRestProvider.xdsBRepositoryUniqueId
					: props.getProperty("XdsBRepositoryUniqueId");
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
	 * Contract search
	 * @param identifier
	 * @return
	 * @throws Exception
	 */
	@Search
	public List<Contract> search(@RequiredParam(name = Patient.SP_IDENTIFIER) final IdentifierDt identifier) throws Exception
	{
		log.info("Entered FHIR Contract search service");
		
		if (props == null)
		{
			loadProperties();
		}

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
			// Determine if a complete patient ID (including OID and ISO specification) was provided.  If not, then append IExHubDomainOid
			//   and IExAssigningAuthority...
			String referencedId = identifier.getValue();
//			ResourceReferenceDt consentSubjectRef = contract.getSubject().get(0);
//			IBaseResource referencedSubject = consentSubjectRef.getResource();
//			String referencedId = referencedSubject.getIdElement().getIdPart();
//			Patient patient = (getContainedResource(Patient.class, contract.getContained().getContainedResources(), referencedId) == null) ? null
//					: (Patient)getContainedResource(Patient.class, contract.getContained().getContainedResources(), referencedId);
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
					null);
			
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
					
					// Error code location (i.e., stack trace) only to be logged to IExHub error file
//					patientDataResponse.getErrorMsgs().add(errorText.toString());
					
					if (error.getLocation() != null)
					{
						errorText.append("Error location=" + error.getLocation());
					}
					
					log.error(errorText.toString());
				}
			}

			// Try to retrieve documents...
			RegistryObjectListType registryObjectList = registryResponse.getRegistryObjectList();
			IdentifiableType[] documentObjects = registryObjectList.getIdentifiable();
			if ((documentObjects != null) &&
				(documentObjects.length > 0))
			{
				log.info("Documents found in the registry, retrieving them from the repository...");
				
				HashMap<String, String> documents = new HashMap<String, String>();
				for (IdentifiableType identifiable : documentObjects)
				{
					if (identifiable.getClass().equals(ExtrinsicObjectType.class))
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
							documents.put(uniqueId,
									home);
							log.info("Document ID added: "
									+ uniqueId
									+ ", homeCommunityId: "
									+ home);
						}
					}
					else
					{
						String home = ((identifiable.getHome() != null) && (identifiable.getHome().getPath().length() > 0)) ? identifiable.getHome().getPath()
								: null;
						documents.put(identifiable.getId().getPath(),
								home);
						log.info("Document ID added: "
								+ identifiable.getId().getPath()
								+ ", homeCommunityId: "
								+ home);
					}
				}
				
				log.info("Invoking XdsB repository connector retrieval...");
				RetrieveDocumentSetResponse documentSetResponse = xdsB.retrieveDocumentSet(xdsBRepositoryUniqueId,
						documents,
						referencedId);
				log.info("XdsB repository connector retrieval succeeded");

				// Invoke appropriate map(s) to process documents in documentSetResponse...
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
								
								String mimeType = docResponseArray[0].getMimeType().getLongName();
								if (mimeType.compareToIgnoreCase("text/xml") == 0)
								{
									String filename = "test/" + document.getDocumentUniqueId().getLongName() + ".xml";
									log.info("Persisting document to filesystem, filename="
											+ filename);
									DataHandler dh = document.getDocument();
									File file = new File(filename);
									FileOutputStream fileOutStream = new FileOutputStream(file);
									dh.writeTo(fileOutStream);
									fileOutStream.close();

									DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
									DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
									Document doc = dBuilder.parse(new FileInputStream(filename));
									XPath xPath = XPathFactory.newInstance().newXPath();
									NodeList nodes = (NodeList)xPath.evaluate("/ClinicalDocument/templateId",
									        doc.getDocumentElement(),
									        XPathConstants.NODESET);

									boolean templateFound = false;
									if (nodes.getLength() > 0)
									{
										log.info("Searching for /ClinicalDocument/templateId, document ID="
												+ document.getDocumentUniqueId().getLongName());
										
										for (int i = 0; i < nodes.getLength(); ++i)
										{
										    String val = ((Element)nodes.item(i)).getAttribute("root");
										    if ((val != null) &&
										    	(val.compareToIgnoreCase("2.16.840.1.113883.10.20.22.1.2") == 0))
										    {
										    	log.info("/ClinicalDocument/templateId node found, document ID="
										    			+ document.getDocumentUniqueId().getLongName());
										    	
												log.info("Invoking XSL transform, document ID="
														+ document.getDocumentUniqueId().getLongName());

										        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
										        factory.setNamespaceAware(true);
										        DocumentBuilder builder = factory.newDocumentBuilder();
										        Document mappedDoc = builder.parse(new File(/*"test/" + document.getDocumentUniqueId().getLongName() + "_TransformedToPatientPortalXML.xml"*/ filename));
										        DOMSource source = new DOMSource(mappedDoc);
										 
//										        TransformerFactory transformerFactory = TransformerFactory.newInstance();
//										        
//										        Transformer transformer = transformerFactory.newTransformer(new StreamSource(GetPatientDataService.cdaToJsonTransformXslt));
//												String jsonFilename = "test/" + document.getDocumentUniqueId().getLongName() + ".json";
//												File jsonFile = new File(jsonFilename);
//												FileOutputStream jsonFileOutStream = new FileOutputStream(jsonFile);
/*										        StreamResult result = new StreamResult(jsonFileOutStream);
										        transformer.transform(source,
										        		result);
												jsonFileOutStream.close();

												log.info("Successfully transformed CCDA to JSON, filename="
														+ jsonFilename);
												
//										            patientDataResponse.getDocuments().add(new String(readAllBytes(get(jsonFilename))));
												jsonOutput.append(new String(readAllBytes(get(jsonFilename))));
												
										    	templateFound = true; */
										    }
										}
									}
									
									if (!templateFound)
								    {
								    	// Document doesn't match the template ID - add to error list...
//								    	patientDataResponse.getErrorMsgs().add("Document retrieved doesn't match required template ID - document ID="
//								    			+ document.getDocumentUniqueId().getLongName());
									}
								}
								else
								{
//									patientDataResponse.getErrorMsgs().add("Document retrieved is not XML - document ID="
//											+ document.getDocumentUniqueId().getLongName());
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
		catch (Exception e)
		{
			
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
	
	@Update
	public MethodOutcome update(@ResourceParam final Contract contract, @ConditionalUrlParam String theConditional) throws Exception
	{
		log.info("Entered FHIR Contract update service");
		
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
					"text/xml",
					true);
			
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
	
	private Object getContainedResource(Class<?> resourceClass,
			List<IResource> containedResources,
			String idValue)
	{
		Object retVal = null;
		for (IResource resource : containedResources)
		{
			if ((resource.getClass().equals(resourceClass)) &&
				(resource.getIdElement().getIdPart().equalsIgnoreCase(idValue)))
			{
				retVal = resource;
				break;
			}
		}

		return retVal;
	}	
}
