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

import jersey.repackaged.com.google.common.base.Equivalence;
import jersey.repackaged.com.google.common.base.Predicate;
import jersey.repackaged.com.google.common.collect.Collections2;
import jersey.repackaged.com.google.common.collect.MapDifference;
import jersey.repackaged.com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.log4j.Logger;
import org.iexhub.connectors.XdsB;
import org.iexhub.connectors.XdsBRepositoryManager;
import org.iexhub.exceptions.*;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.*;
import org.iexhub.services.client.DocumentRepository_ServiceStub.DocumentResponse_type0;
import org.iexhub.services.client.DocumentRepository_ServiceStub.RetrieveDocumentSetResponse;
import org.iexhub.services.dto.ClinicalDocumentRequest;
import org.iexhub.services.dto.ClinicalDocumentResponse;
import org.iexhub.services.dto.DocumentsResponseDto;
import org.iexhub.services.dto.PatientDocument;
import org.openhealthtools.mdht.mdmi.*;
import org.openhealthtools.mdht.mdmi.model.MdmiBusinessElementReference;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.activation.DataHandler;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.*;

import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;

//import org.openhealthtools.mdht.uml.cda.util.CDAUtil.ValidationHandler;

/**
 * @author A. Sute
 *
 */
@Deprecated
@Path("/GetPatientData")
public class GetPatientDataService
{
    /** Logger */
    public static Logger log = Logger.getLogger(GetPatientDataService.class);

//	public static class Handler implements ValidationHandler
//	{
//		boolean isValid = true;
//
//		public boolean isValid() {
//			return isValid;
//		}
//
//		public void setValid( boolean isValid ) {
//			this.isValid = isValid;
//		}
//
//		@Override
//		public void handleError( Diagnostic arg0 ) {
//			isValid = false;
//		}
//
//		@Override
//		public void handleInfo( Diagnostic arg0 ) {
//			// Currently ignoring all informational diagnostics
//		}
//
//		@Override
//		public void handleWarning( Diagnostic arg0 ) {
//			// Currently ignoring all warning diagnostics
//		}
//	}

	private static Properties props = null;
	private static XdsB xdsB = null;
	private static boolean testMode = false;
	private static String propertiesFile = "/temp/IExHub.properties";
	private static String xdsBRepositoryUniqueId = "1.3.6.1.4.1.21367.13.40.216";
	private static String cdaToJsonTransformXslt = null;
	private static String iExHubDomainOid = "1.3.6.1.4.1.21367.13.50.300132";
	private static String iExHubAssigningAuthority = "ISO";
	private static String testJSONDocumentPathname= null;

	@GET
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	public Response getPatientData(@Context HttpHeaders headers)
	{
		log.info("Entered getPatientData service");
		
		boolean tls = false;
		if (props == null)
		{
			try
			{
				props = new Properties();
				props.load(new FileInputStream(propertiesFile));
				tls = (props.getProperty("XdsBRegistryEndpointURI") == null) ? false
						: ((props.getProperty("XdsBRegistryEndpointURI").toLowerCase().contains("https") ? true
								: false));
				GetPatientDataService.testMode = (props.getProperty("TestMode") == null) ? GetPatientDataService.testMode
						: Boolean.parseBoolean(props.getProperty("TestMode"));
				GetPatientDataService.testJSONDocumentPathname = (props.getProperty("TestJSONDocumentPathname") == null) ? GetPatientDataService.testJSONDocumentPathname
						: props.getProperty("TestJSONDocumentPathname");
				GetPatientDataService.cdaToJsonTransformXslt = (props.getProperty("CDAToJSONTransformXSLT") == null) ? GetPatientDataService.cdaToJsonTransformXslt
						: props.getProperty("CDAToJSONTransformXSLT");
				GetPatientDataService.iExHubDomainOid = (props.getProperty("IExHubDomainOID") == null) ? GetPatientDataService.iExHubDomainOid
						: props.getProperty("IExHubDomainOID");
				GetPatientDataService.iExHubAssigningAuthority = (props.getProperty("IExHubAssigningAuthority") == null) ? GetPatientDataService.iExHubAssigningAuthority
						: props.getProperty("IExHubAssigningAuthority");
				GetPatientDataService.xdsBRepositoryUniqueId = (props.getProperty("XdsBRepositoryUniqueId") == null) ? GetPatientDataService.xdsBRepositoryUniqueId
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
		
		String retVal = "";
		GetPatientDataResponse patientDataResponse = new GetPatientDataResponse();
		StringBuilder jsonOutput = new StringBuilder();

		if (!testMode)
		{
			try
			{
				if (xdsB == null)
				{
					log.info("Instantiating XdsB connector...");
					xdsB = new XdsB(null,
							null,
							tls);
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
				MultivaluedMap<String, String> headerParams = headers.getRequestHeaders();
				String ssoAuth = headerParams.getFirst("ssoauth");
				log.info("HTTP headers successfully retrieved");
				
				// Extract patient ID, query start date, and query end date.  Expected format from the client is
				//   "PatientId={0}&LastName={1}&FirstName={2}&MiddleName={3}&DateOfBirth={4}&PatientGender={5}&MotherMaidenName={6}&AddressStreet={7}&AddressCity={8}&AddressState={9}&AddressPostalCode={10}&OtherIDsScopingOrganization={11}&StartDate={12}&EndDate={13}"
				String[] splitPatientId = ssoAuth.split("&LastName=");
				String patientId = (splitPatientId[0].split("=").length == 2) ? splitPatientId[0].split("=")[1] : null;
				
				String[] parts = splitPatientId[1].split("&");
				String lastName = (parts[0].length() > 0) ? parts[0] : null;
				String firstName = (parts[1].split("=").length == 2) ? parts[1].split("=")[1] : null;
				String middleName = (parts[2].split("=").length == 2) ? parts[2].split("=")[1] : null;
				String dateOfBirth = (parts[3].split("=").length == 2) ? parts[3].split("=")[1] : null;
				String gender = (parts[4].split("=").length == 2) ? parts[4].split("=")[1] : null;
				String motherMaidenName = (parts[5].split("=").length == 2) ? parts[5].split("=")[1] : null;
				String addressStreet = (parts[6].split("=").length == 2) ? parts[6].split("=")[1] : null;
				String addressCity = (parts[7].split("=").length == 2) ? parts[7].split("=")[1] : null;
				String addressState = (parts[8].split("=").length == 2) ? parts[8].split("=")[1] : null;
				String addressPostalCode = (parts[9].split("=").length == 2) ? parts[9].split("=")[1] : null;
				String otherIDsScopingOrganization = (parts[10].split("=").length == 2) ? parts[10].split("=")[1] : null;
				String startDate = (parts[11].split("=").length == 2) ? parts[11].split("=")[1] : null;
				String endDate = (parts[12].split("=").length == 2) ? parts[12].split("=")[1] : null;

				log.info("HTTP headers successfully parsed, now calling XdsB registry...");
								
				// Determine if a complete patient ID (including OID and ISO specification) was provided.  If not, then append IExHubDomainOid
				//   and IExAssigningAuthority...
				if (!patientId.contains("^^^&"))
				{
					patientId = "'" + patientId + "^^^&" + GetPatientDataService.iExHubDomainOid + "&" + GetPatientDataService.iExHubAssigningAuthority + "'";
				}
				
//				if (!patientId.startsWith("'"))
//				{
//					patientId = "'" + patientId;
//				}
//				
//				if (!patientId.endsWith("'"))
//				{
//					patientId += "'";
//				}
				
				AdhocQueryResponse registryResponse = xdsB.registryStoredQuery(patientId,
						(startDate != null) ? DateFormat.getDateInstance().format(startDate) : null,
						(endDate != null) ? DateFormat.getDateInstance().format(endDate) : null);
				
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
						patientDataResponse.getErrorMsgs().add(errorText.toString());
						
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
							patientId);
					log.info("XdsB repository connector retrieval succeeded");

					// Invoke appropriate map(s) to process documents in documentSetResponse...
					if (documentSetResponse.getRetrieveDocumentSetResponse().getRetrieveDocumentSetResponseTypeSequence_type0() != null)
					{
						DocumentResponse_type0[] docResponseArray = documentSetResponse.getRetrieveDocumentSetResponse().getRetrieveDocumentSetResponseTypeSequence_type0().getDocumentResponse();
						if (docResponseArray != null)
						{
							jsonOutput.append("{\"Documents\":[");
							boolean first = true;
							try
							{
								for (DocumentResponse_type0 document : docResponseArray)
								{
									if (!first)
									{
										jsonOutput.append(",");
									}
									first = false;
									
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

										//set namespace to xpath
										xPath.setNamespaceContext(new NamespaceContext() {
											private final String uri = "urn:hl7-org:v3";
											private final String prefix = "hl7";
											@Override
											public String getNamespaceURI(String prefix) {
												return this.prefix.equals(prefix) ? uri : null;
											}
											@Override
											public String getPrefix(String namespaceURI) {
												return this.uri.equals(namespaceURI) ? this.prefix : null;
											}
											@Override
											public Iterator getPrefixes(String namespaceURI) {
												return null;
											}
										});

										NodeList nodes = (NodeList)xPath.evaluate("/hl7:ClinicalDocument/hl7:templateId",
												doc.getDocumentElement(),
												XPathConstants.NODESET);

/*										NodeList nodes = (NodeList)xPath.evaluate("/ClinicalDocument/templateId",
										        doc.getDocumentElement(),
										        XPathConstants.NODESET);*/

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
											    	
													// CCDA 1.1 validation check...
//													Handler handler = new Handler();
//													handler.setValid(true);
		//											ClinicalDocument clinicalDocument = CDAUtil.load(new FileInputStream(filename),
		//													handler);
		//											ByteArrayOutputStream output = new ByteArrayOutputStream();
		//											CDAUtil.save(clinicalDocument,
		//													output);
		
//													if (handler.isValid())
//													{
	//													log.info("Invoking map, document ID="
	//															+ document.getDocumentUniqueId().getLongName());
														
	//													String mapOutput = invokeMap(filename);
	
	//													log.info("Map invocation successful, document ID="
	//															+ document.getDocumentUniqueId().getLongName());
	
														// Persist transformed CCDA to filesystem for auditing...
	//													Files.write(Paths.get("test/" + document.getDocumentUniqueId().getLongName() + "_TransformedToPatientPortalXML.xml"),
	//															mapOutput.getBytes());
	
	//													log.info("Persisted transformed CCDA document to filesystem, filename="
	//															+ "test/"
	//															+ document.getDocumentUniqueId().getLongName()
	//															+ "_TransformedToPatientPortalXML.xml");
	
														log.info("Invoking XSL transform, document ID="
																+ document.getDocumentUniqueId().getLongName());
	
												        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
												        factory.setNamespaceAware(true);
												        DocumentBuilder builder = factory.newDocumentBuilder();
												        Document mappedDoc = builder.parse(new File(/*"test/" + document.getDocumentUniqueId().getLongName() + "_TransformedToPatientPortalXML.xml"*/ filename));
												        DOMSource source = new DOMSource(mappedDoc);
												 
												        TransformerFactory transformerFactory = TransformerFactory.newInstance();
												        
												        Transformer transformer = transformerFactory.newTransformer(new StreamSource(GetPatientDataService.cdaToJsonTransformXslt));
														String jsonFilename = "test/" + document.getDocumentUniqueId().getLongName() + ".json";
														File jsonFile = new File(jsonFilename);
														FileOutputStream jsonFileOutStream = new FileOutputStream(jsonFile);
												        StreamResult result = new StreamResult(jsonFileOutStream);
												        transformer.transform(source,
												        		result);
														jsonFileOutStream.close();
	
														log.info("Successfully transformed CCDA to JSON, filename="
																+ jsonFilename);
														
	//										            patientDataResponse.getDocuments().add(new String(readAllBytes(get(jsonFilename))));
														jsonOutput.append(new String(readAllBytes(get(jsonFilename))));
														
												    	templateFound = true;
//													}
//													else
//													{
//														patientDataResponse.getErrorMsgs().add("Document retrieved is not a valid C-CDA 1.1 document - document ID="
//																+ document.getDocumentUniqueId().getLongName());									
//													}
											    }
											}
										}
										
										if (!templateFound)
									    {
									    	// Document doesn't match the template ID - add to error list...
									    	patientDataResponse.getErrorMsgs().add("Document retrieved doesn't match required template ID - document ID="
									    			+ document.getDocumentUniqueId().getLongName());
										}
									}
									else
									{
										patientDataResponse.getErrorMsgs().add("Document retrieved is not XML - document ID="
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

					if (jsonOutput.length() > 0)
					{
						jsonOutput.append("]}");
					}
				}
			}
			catch (Exception e)
			{
				log.error("Error encountered, "
						+ e.getMessage());
				throw new UnexpectedServerException("Error - " + e.getMessage());
			}
		}
		else
		{
			// Return canned document for sprint #16 demo.  Sprint #17 will return JSON created by MDMI map (code below outside of this block).
			try
			{
				retVal = FileUtils.readFileToString(new File(GetPatientDataService.testJSONDocumentPathname));
				return Response.status(Response.Status.OK).entity(retVal).type(MediaType.APPLICATION_JSON).build();
			}
			catch (Exception e)
			{
				throw new UnexpectedServerException("Error - " + e.getMessage());
			}
		}
		
//		return Response.status(Response.Status.OK).entity(patientDataResponse).type(MediaType.APPLICATION_JSON).build();
		return Response.status(Response.Status.OK).entity(jsonOutput.toString()).type(MediaType.APPLICATION_JSON).build();
	}


	@GET
	@Path("/ccd")
	@Produces({MediaType.APPLICATION_JSON})
	public Response getCCD(@Context HttpHeaders headers)
	{
		log.info("Entered getPatientData service");

		boolean tls = false;
		if (props == null)
		{
			try
			{
				props = new Properties();
				props.load(new FileInputStream(propertiesFile));
				tls = (props.getProperty("XdsBRegistryEndpointURI") == null) ? false
						: ((props.getProperty("XdsBRegistryEndpointURI").toLowerCase().contains("https") ? true
						: false));
				GetPatientDataService.testMode = (props.getProperty("TestMode") == null) ? GetPatientDataService.testMode
						: Boolean.parseBoolean(props.getProperty("TestMode"));
				GetPatientDataService.testJSONDocumentPathname = (props.getProperty("TestJSONDocumentPathname") == null) ? GetPatientDataService.testJSONDocumentPathname
						: props.getProperty("TestJSONDocumentPathname");
				GetPatientDataService.cdaToJsonTransformXslt = (props.getProperty("CDAToJSONTransformXSLT") == null) ? GetPatientDataService.cdaToJsonTransformXslt
						: props.getProperty("CDAToJSONTransformXSLT");
				GetPatientDataService.iExHubDomainOid = (props.getProperty("IExHubDomainOID") == null) ? GetPatientDataService.iExHubDomainOid
						: props.getProperty("IExHubDomainOID");
				GetPatientDataService.iExHubAssigningAuthority = (props.getProperty("IExHubAssigningAuthority") == null) ? GetPatientDataService.iExHubAssigningAuthority
						: props.getProperty("IExHubAssigningAuthority");
				GetPatientDataService.xdsBRepositoryUniqueId = (props.getProperty("XdsBRepositoryUniqueId") == null) ? GetPatientDataService.xdsBRepositoryUniqueId
						: props.getProperty("XdsBRepositoryUniqueId");
			}
			catch (IOException e)
			{
				log.error("Error encountered loading properties file, "+ propertiesFile + ", " + e.getMessage());
				throw new UnexpectedServerException("Error encountered loading properties file, " + propertiesFile + ", " + e.getMessage());
			}
		}

		String retVal = "";
		GetPatientDataResponse patientDataResponse = new GetPatientDataResponse();
		DocumentsResponseDto documentsResponseDto = new DocumentsResponseDto();
		ArrayList<PatientDocument> patientDocuments = new ArrayList<PatientDocument>();

		if (!testMode)
		{
			try
			{
				if (xdsB == null)
				{
					log.info("Instantiating XdsB connector...");
					xdsB = new XdsB(null,null, tls);
					log.info("XdsB connector successfully started");
				}
			}
			catch (Exception e)
			{
				log.error("Error encountered instantiating XdsB connector, " + e.getMessage());
				throw new UnexpectedServerException("Error - " + e.getMessage());
			}

			try
			{
				MultivaluedMap<String, String> headerParams = headers.getRequestHeaders();
				String ssoAuth = headerParams.getFirst("ssoauth");

				log.info("HTTP headers successfully retrieved");
				// Extract patient ID, query start date, and query end date.  Expected format from the client is
				//   "PatientId={0}&LastName={1}&FirstName={2}&MiddleName={3}&DateOfBirth={4}&PatientGender={5}&MotherMaidenName={6}&AddressStreet={7}&AddressCity={8}&AddressState={9}&AddressPostalCode={10}&OtherIDsScopingOrganization={11}&StartDate={12}&EndDate={13}"
				String[] splitPatientId = ssoAuth.split("&LastName=");
				String patientId = (splitPatientId[0].split("=").length == 2) ? splitPatientId[0].split("=")[1] : null;

				String[] parts = splitPatientId[1].split("&");
				String lastName = (parts[0].length() > 0) ? parts[0] : null;
				String firstName = (parts[1].split("=").length == 2) ? parts[1].split("=")[1] : null;
				String middleName = (parts[2].split("=").length == 2) ? parts[2].split("=")[1] : null;
				String dateOfBirth = (parts[3].split("=").length == 2) ? parts[3].split("=")[1] : null;
				String gender = (parts[4].split("=").length == 2) ? parts[4].split("=")[1] : null;
				String motherMaidenName = (parts[5].split("=").length == 2) ? parts[5].split("=")[1] : null;
				String addressStreet = (parts[6].split("=").length == 2) ? parts[6].split("=")[1] : null;
				String addressCity = (parts[7].split("=").length == 2) ? parts[7].split("=")[1] : null;
				String addressState = (parts[8].split("=").length == 2) ? parts[8].split("=")[1] : null;
				String addressPostalCode = (parts[9].split("=").length == 2) ? parts[9].split("=")[1] : null;
				String otherIDsScopingOrganization = (parts[10].split("=").length == 2) ? parts[10].split("=")[1] : null;
				String startDate = (parts[11].split("=").length == 2) ? parts[11].split("=")[1] : null;
				String endDate = (parts[12].split("=").length == 2) ? parts[12].split("=")[1] : null;

				log.info("HTTP headers successfully parsed, now calling XdsB registry...");

				// Determine if a complete patient ID (including OID and ISO specification) was provided.  If not, then append IExHubDomainOid
				//   and IExAssigningAuthority...
				if (!patientId.contains("^^^&"))
				{
					patientId = "'" + patientId + "^^^&" + GetPatientDataService.iExHubDomainOid + "&" + GetPatientDataService.iExHubAssigningAuthority + "'";
				}

				AdhocQueryResponse registryResponse = xdsB.registryStoredQuery(patientId,
						(startDate != null) ? DateFormat.getDateInstance().format(startDate) : null,
						(endDate != null) ? DateFormat.getDateInstance().format(endDate) : null);

				log.info("Call to XdsB registry successful");

				// Determine if registry server returned any errors...
				if ((registryResponse.getRegistryErrorList() != null) && (registryResponse.getRegistryErrorList().getRegistryError().length > 0)) {

					for (RegistryError_type0 error : registryResponse.getRegistryErrorList().getRegistryError()) {
						StringBuilder errorText = new StringBuilder();
						if (error.getErrorCode() != null) {
							errorText.append("Error code=" + error.getErrorCode() + "\n");
						}
						if (error.getCodeContext() != null) {
							errorText.append("Error code context=" + error.getCodeContext() + "\n");
						}

						// Error code location (i.e., stack trace) only to be logged to IExHub error file
						patientDataResponse.getErrorMsgs().add(errorText.toString());

						if (error.getLocation() != null) {
							errorText.append("Error location=" + error.getLocation());
						}
						log.error(errorText.toString());
					}
				}

				// Try to retrieve documents...
				RegistryObjectListType registryObjectList = registryResponse.getRegistryObjectList();
				IdentifiableType[] documentObjects = registryObjectList.getIdentifiable();
				if ((documentObjects != null) && (documentObjects.length > 0)) {
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
								if ((val != null) && (val.compareToIgnoreCase("XDSDocumentEntry.uniqueId") == 0)) {
									log.info("Located XDSDocumentEntry.uniqueId ExternalIdentifier, uniqueId="
											+ uniqueId);
									uniqueId = externalIdentifier.getValue().getLongName();
									break;
								}
							}

							if (uniqueId != null) {
								documents.put(uniqueId, home);
								log.info("Document ID added: " + uniqueId + ", homeCommunityId: " + home);
							}
						}
						else
						{
							String home = ((identifiable.getHome() != null) && (identifiable.getHome().getPath().length() > 0)) ? identifiable.getHome().getPath() : null;
							documents.put(identifiable.getId().getPath(), home);
							log.info("Document ID added: " + identifiable.getId().getPath() + ", homeCommunityId: " + home);
						}
					}

					log.info("Invoking XdsB repository connector retrieval...");
					RetrieveDocumentSetResponse documentSetResponse = xdsB.retrieveDocumentSet(xdsBRepositoryUniqueId, documents, patientId);
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
										DataHandler dh = document.getDocument();
										String documentStr = dh.getContent().toString();
										String documentName = document.getDocumentUniqueId().getLongName();
										PatientDocument patientDocument = new PatientDocument(documentName,documentStr);
										patientDocuments.add(patientDocument);
									}
									else
									{
										patientDataResponse.getErrorMsgs().add("Document retrieved is not XML - document ID=" + document.getDocumentUniqueId().getLongName());
									}
								}
							}
							catch (Exception e)
							{
								log.error("Error encountered, "+ e.getMessage());
								throw e;
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
		}
		else
		{
			// Return canned document for sprint #16 demo.  Sprint #17 will return JSON created by MDMI map (code below outside of this block).
			try
			{
				retVal = FileUtils.readFileToString(new File(GetPatientDataService.testJSONDocumentPathname));
				return Response.status(Response.Status.OK).entity(retVal).type(MediaType.APPLICATION_JSON).build();
			}
			catch (Exception e)
			{
				throw new UnexpectedServerException("Error - " + e.getMessage());
			}
		}
		documentsResponseDto.setDocuments(patientDocuments);

		return Response.status(Response.Status.OK).entity(documentsResponseDto).type(MediaType.APPLICATION_JSON).build();
	}

	@POST
	@Path("/publish")
	@Produces({MediaType.APPLICATION_JSON})
	public Response publishDocument(ClinicalDocumentRequest clinicalDocumentRequest)
	{
		Properties props = new Properties();

		try
		{
			byte[] document = clinicalDocumentRequest.getDocument();
			props.load(new FileInputStream(propertiesFile));
			String xdsBRegistryEndpointURI = props.getProperty("XdsBRegistryEndpointURI");
			String xdsBRepositoryEndpointURI = props.getProperty("XdsBRepositoryEndpointURI");
			XdsBRepositoryManager XdsBRepository = new XdsBRepositoryManager(xdsBRegistryEndpointURI, xdsBRepositoryEndpointURI);
			XdsBRepository.provideAndRegisterDocumentSet(document ,"text/xml" );
		}
		catch (Exception e)
		{
			String msg = "Error encounter while publishing document to HIE: " + e.getMessage();
			log.error(msg);
			System.out.println(msg);
			throw new UnexpectedServerException(msg);
		}

		return Response.status(Response.Status.OK).entity(new ClinicalDocumentResponse(true)).type(MediaType.APPLICATION_JSON).build();
	}

	private String invokeMap(String sourceFilename)
	{
		String trgMap = "test/PatientPortalMap.xmi";
		String trgMdl = "PatientPortal.CCD";
		String trgMsg = "test/PP_minimal.xml";
		String srcMap = null;
		String srcMdl = null;
		String cnvElm = "";
		File sourceDataSetFile = null;
		
		File rootDir = new File(System.getProperties().getProperty("user.dir"));

		// initialize the runtime, using the current folder as the root folder
		Mdmi.INSTANCE.initialize(rootDir);
		Mdmi.INSTANCE.start();

		String retVal = null;
		try
		{
			srcMap = "test/CCDA.9.1.xmi";
			srcMdl = "CCDMessageGroup.CCD";

			// 1. check to make sure the maps and messages exist
			File f = Mdmi.INSTANCE.fileFromRelPath(srcMap);
			if (!f.exists() || !f.isFile())
			{
				throw new SourceMapMissingException("Error - source map file '" + srcMap + "' does not exist");
			}
			else
			{
				f = Mdmi.INSTANCE.fileFromRelPath(trgMap);
				if (!f.exists() || !f.isFile())
				{
					throw new TargetMapMissingException("Error - target map file '" + trgMap + "' does not exist");
				}
				else
				{
					sourceDataSetFile = Mdmi.INSTANCE.fileFromRelPath(sourceFilename);
					if (!sourceDataSetFile.exists() || !sourceDataSetFile.isFile())
					{
						// The source message may be a URI - verify if that's the case...
						sourceDataSetFile = new File(sourceFilename);
					}
					
					if (!sourceDataSetFile.exists() || !sourceDataSetFile.isFile())
					{
						throw new SourceMsgMissingException("Error - source message file '" + sourceFilename + "' does not exist");
					}
					else
					{
						f = Mdmi.INSTANCE.fileFromRelPath(trgMsg);
						if (!f.exists() || !f.isFile())
						{
							throw new TargetMapMissingException("Error - target message file '" + trgMsg + "' does not exist");
						}
						else
						{
							// 2. make sure the qualified message names are spelled right
							String[] a = srcMdl.split("\\.");
							if (a == null || a.length != 2)
							{
								throw new InvalidSourceModelException("Error - invalid source model '" + srcMdl + "', must be formatted as MapName.MessageName");
							}
							else
							{
								String srcMapName = a[0];
								String srcMsgMdl = a[1];
					
								a = trgMdl.split("\\.");
								if (a == null || a.length != 2)
								{
									throw new InvalidTargetModelException("Error - invalid target model '" + trgMdl + "', must be formatted as MapName.MessageName");
								}
								else
								{
									try
									{
										String trgMapName = a[0];
										String trgMsgMdl = a[1];
							
										// 3. parse the elements array
										final ArrayList<String> elements = new ArrayList<String>();
										String[] ss = cnvElm.split(";");
										for (String s : ss) {
											if (s != null && s.trim().length() > 0) {
												elements.add(s);
											}
										}
							
										// 4. load the maps into the runtime.
										Mdmi.INSTANCE.getConfig().putMapInfo(new MdmiConfig.MapInfo(srcMapName, srcMap));
										Mdmi.INSTANCE.getConfig().putMapInfo(new MdmiConfig.MapInfo(trgMapName, trgMap));
										Mdmi.INSTANCE.getResolver().resolveConfig(Mdmi.INSTANCE.getConfig());
							
										// 5. Construct the parameters to the call based on the values passed in
										MdmiModelRef sMod = new MdmiModelRef(srcMapName, srcMsgMdl);
										MdmiMessage sMsg = new MdmiMessage(sourceDataSetFile);
										MdmiModelRef tMod = new MdmiModelRef(trgMapName, trgMsgMdl);
										MdmiMessage tMsg = new MdmiMessage(Mdmi.INSTANCE.fileFromRelPath(trgMsg));
							
										Map<String, MdmiBusinessElementReference> left = sMod.getModel().getBusinessElementHashMap();
							
										Map<String, MdmiBusinessElementReference> right = tMod.getModel().getBusinessElementHashMap();
							
										Equivalence<MdmiBusinessElementReference> valueEquivalence = new Equivalence<MdmiBusinessElementReference>() {
											@Override
											protected boolean doEquivalent(MdmiBusinessElementReference a, MdmiBusinessElementReference b) {
												return a.getUniqueIdentifier().equals(b.getUniqueIdentifier());
											}
							
											@Override
											protected int doHash(MdmiBusinessElementReference t) {
												return t.getUniqueIdentifier().hashCode();
											}
										};
							
										MapDifference<String, MdmiBusinessElementReference> differences = Maps.difference(
											left, right, valueEquivalence);
							
										Predicate<MdmiBusinessElementReference> predicate = new Predicate<MdmiBusinessElementReference>() {
											@Override
											public boolean apply(MdmiBusinessElementReference input) {
							
												if (!elements.isEmpty()) {
													for (String element : elements) {
														if (input.getName().equalsIgnoreCase(element)) {
															return true;
														}
							
													}
													return false;
												}
												return true;
							
											}
										};
										;
							
										ArrayList<MdmiBusinessElementReference> bers = new ArrayList<MdmiBusinessElementReference>();
										bers.addAll(Collections2.filter(differences.entriesInCommon().values(), predicate));
							
										MdmiTransferInfo ti = new MdmiTransferInfo(sMod, sMsg, tMod, tMsg, bers);
										ti.useDictionary = true;
							
										// 6. call the runtime
										Mdmi.INSTANCE.executeTransfer(ti);
							
										// 7. set the return value
										retVal = tMsg.getDataAsString();
									}
									catch (Exception e)
									{
										throw new MessageTransformException(e.getMessage());
									}
								}
							}
						}
					}
				}
			}
		}
		catch (InvalidSourceModelException |
			   InvalidTargetModelException |
			   MessageTransformException |
			   SourceMapMissingException |
			   SourceMsgMissingException |
			   TargetMapMissingException
			   ex)
		{
			throw ex;
		}
		catch (Exception ex)
		{
			throw ex;
		}
		finally
		{
			Mdmi.INSTANCE.stop();			
		}
		
		return retVal;
	}
}