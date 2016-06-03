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
/**
 * 
 */
package org.iexhub.connectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.HashMap;
import java.util.Properties;

import javax.activation.DataHandler;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.log4j.Logger;
import org.iexhub.exceptions.UnexpectedServerException;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.AdhocQueryResponse;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.ExternalIdentifierType;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.ExtrinsicObjectType;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.IdentifiableType;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.RegistryObjectListType;
import org.iexhub.services.client.DocumentRepository_ServiceStub.DocumentResponse_type0;
import org.iexhub.services.client.DocumentRepository_ServiceStub.RetrieveDocumentSetResponse;
import org.junit.Before;
import org.junit.Test;

/**
 * @author A. Sute
 *
 */
public class XdsBTest
{
    /** Logger */
    public static final Logger log = Logger.getLogger(XdsBTest.class);

	//	private static final String xdsBRegistryTLSEndpointURI = "https://ihexds.nist.gov:12091/tf6/services/xdsregistryb";
//	private static final String xdsBRegistryTLSEndpointURI = "https://philips50:8443/philips/services/xdsregistry";
//	private static final String xdsBRepositoryTLSEndpointURI = "https://philips50:8443/philips/services/xdsrepository";
//	private static final String xdsBRegistryTLSEndpointURI = "https://cerner14:9070/ihe/services/xdsregistryb";
//    private static final String xdsBRegistryTLSEndpointURI = "https://ith-icoserve12:1243/Registry/services/RegistryService";
//	private static final String xdsBRepositoryTLSEndpointURI = "https://tiani---cisco72:8443/XDS3/rep";
//	private static final String xdsBRegistryTLSEndpointURI = "https://merge11:443/iti18";
//	private static final String xdsBRepositoryTLSEndpointURI = "https://10.242.43.13:5000/repository";
	private static final String xdsBRegistryTLSEndpointURI = "https://demohie.thinkengage.com/XDS3/reg";
	private static final String xdsBRepositoryTLSEndpointURI = "https://demohie.thinkengage.com/SpiritProxy/repository";

//    private static final String xdsBRepositoryUniqueId = "1.3.6.1.4.1.21367.13.40.226";		// Philips XDS.b repository ID
//    private static final String xdsBRepositoryUniqueId = "1.3.6.1.4.1.21367.13.40.228";			// NIST RED repository ID
	private static  String xdsBRepositoryUniqueId = "1.3.6.1.4.1.21367.13.40.216";
//    private static final String xdsBRepositoryUniqueId = "1.3.6.1.4.1.21367.13.40.214";
//	private static final String xdsBRepositoryUniqueId = "1.3.6.1.4.1.21367.13.40.210";    // OpenHIE
	private static XdsB xdsB = null;
	private static final SOAPFactory soapFactory = OMAbstractFactory.getSOAP12Factory();

	private static String propertiesFile = "/temp/IExHub.properties";
	private static String registryEndpointURI = "http://ihexds.nist.gov:80/tf6/services/xdsregistryb";
	private static String repositoryEndpointURI = "http://ihexds.nist.gov:80/tf6/services/xdsrepositoryb";
	private static String patientIdentifier = "'086666c2fd154f7^^^&1.3.6.1.4.1.21367.2005.13.20.3000&ISO'";

	@Before
	public void loadProperties(){
		Properties props = new Properties();
		try
		{
			props.load(new FileInputStream(propertiesFile));
			registryEndpointURI = props.getProperty("XdsBRegistryEndpointURI");
			repositoryEndpointURI = props.getProperty("XdsBRepositoryEndpointURI");
			patientIdentifier = props.getProperty("PatientIdToRetrieve");
			xdsBRepositoryUniqueId = props.getProperty("XdsBRepositoryUniqueId");
			Boolean.parseBoolean(props.getProperty("DebugSSL"));
			props.getProperty("SyslogServerHost");
			Integer.parseInt(props.getProperty("SyslogServerPort"));
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
	 * Query Registry
	 * @param patientIdentifier
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	private HashMap<String, String> queryRegistry(String patientIdentifier,
			String startDate,
			String endDate)
	{
		HashMap<String, String> documents = null;

		try
		{
			AdhocQueryResponse registryResponse = xdsB.registryStoredQuery(patientIdentifier,
					(startDate != null) ? DateFormat.getDateInstance().format(startDate) : null,
					(endDate != null) ? DateFormat.getDateInstance().format(endDate) : null);
			
			log.info("XDS.b registry stored query response in log message immediately following...");
			OMElement requestElement = registryResponse.getOMElement(AdhocQueryResponse.MY_QNAME,
					soapFactory);
			log.info(requestElement);
			
			// Try to retrieve document ID's and their homeCommunityId if present...
			RegistryObjectListType registryObjectList = registryResponse.getRegistryObjectList();
			IdentifiableType[] documentObjects = registryObjectList.getIdentifiable();
			if ((documentObjects != null) &&
				(documentObjects.length > 0))
			{
				documents = new HashMap<String, String>();
				for (IdentifiableType identifiable : documentObjects)
				{
					if (identifiable.getClass().equals(ExtrinsicObjectType.class))
					{
						// Determine if the "home" attribute (homeCommunityId in XCA parlance) is present...
						String home = ((((ExtrinsicObjectType)identifiable).getHome() != null) && (((ExtrinsicObjectType)identifiable).getHome().toString() != null) && (((ExtrinsicObjectType)identifiable).getHome().toString().length() > 0)) ? ((ExtrinsicObjectType)identifiable).getHome().toString()
								: null;
						
						ExternalIdentifierType[] externalIdentifiers = ((ExtrinsicObjectType)identifiable).getExternalIdentifier();
						
						// Find the ExternalIdentifier that has the "XDSDocumentEntry.uniqueId" value...
						String uniqueId = null;
						for (ExternalIdentifierType externalIdentifier : externalIdentifiers)
						{
							String val = externalIdentifier.getName().getInternationalStringTypeSequence()[0].getLocalizedString().getValue().getFreeFormText();
							if ((val != null) &&
								val.equalsIgnoreCase("XDSDocumentEntry.uniqueId") )
							{
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
			}
		}
		catch (Exception e)
		{
			log.error("Error - " + e.getMessage());
			fail("Error - " + e.getMessage());
		}
		
		return documents;
	}
	
	/**
	 * Test method for {@link org.iexhub.connectors.XdsB#registryStoredQuery(java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testRegistryStoredQuery()
	{
		Properties props = new Properties();

		try
		{
			log.info("Registry stored query (ITI-18) unit test started...");
			xdsB = new XdsB(registryEndpointURI,
					repositoryEndpointURI);
			
			String patientIdentifier = "'086666c2fd154f7^^^&1.3.6.1.4.1.21367.2005.13.20.3000&ISO'";
			String startDate = null;
			String endDate = null;

			assertFalse("Error - no documents found",
					queryRegistry(patientIdentifier, startDate, endDate).isEmpty());
			
			log.info("Registry stored query (ITI-18) unit test ending.");
		}
		catch (Exception e)
		{
			log.error("Error - " + e.getMessage());
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link org.iexhub.connectors.XdsB#registryStoredQuery(java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testTLSRegistryStoredQuery()
	{
		try
		{
			log.info("Registry TLS stored query (ITI-18) unit test started...");
			xdsB = new XdsB(xdsBRegistryTLSEndpointURI,
					null,
					true);
			
//			String patientIdentifier = "IHEBLUE-2332^^^&1.3.6.1.4.1.21367.13.20.3000&ISO^PI";
//			String patientIdentifier = "IHEBLUE-1019^^^&1.3.6.1.4.1.21367.13.20.3000&ISO";
//			String patientIdentifier = "IHEGREEN-2376^^^&1.3.6.1.4.1.21367.13.20.2000&ISO";
			//String patientIdentifier = "940140b7-7c8b-4491-90f4-4819ded969bf^^^&1.3.6.1.4.1.21367.13.20.3000&ISO";
			String startDate = null;
			String endDate = null;

			HashMap<String, String> documents = queryRegistry(patientIdentifier, startDate, endDate);

			assertFalse("Error - no documents found",
					documents == null);
			assertFalse("Error - no documents found",
					documents.isEmpty());
			log.info("Registry TLS stored query (ITI-18) unit test ending.");
		}
		catch (Exception e)
		{
			log.error("Error - " + e.getMessage());
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link org.iexhub.connectors.XdsB#registryStoredQuery(java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testXCARegistryStoredQuery()
	{
		try
		{
			log.info("XCA registry stored query (ITI-18/ITI-38) unit test started...");
			xdsB = new XdsB("http://ihexds.nist.gov:12090/tf6/services/xcaregistry",
					"http://ihexds.nist.gov:12090/tf6/services/xcarepository");
			
			String patientIdentifier = "'f10f8d972aba4fd^^^&1.3.6.1.4.1.21367.2005.13.20.3000&ISO'"; 
			//"'086666c2fd154f7^^^&1.3.6.1.4.1.21367.2005.13.20.3000&ISO'";
			String startDate = null;
			String endDate = null;

			assertFalse("Error - no documents found",
					queryRegistry(patientIdentifier, startDate, endDate).isEmpty());
			log.info("XCA registry stored query unit test ending.");
		}
		catch (Exception e)
		{
			log.error("Error - " + e.getMessage());
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link org.iexhub.connectors.XdsB#registryStoredQuery(java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testXCARepositoryStoredQuery()
	{
		try
		{
			log.info("XCA repository stored query unit test started...");
			xdsB = new XdsB("http://10.242.52.11:9080/tf6/services/xcaregistry",
					"http://10.242.52.11:9080/tf6/services/xcarepository");
			
			String patientIdentifier = "'P20160124152404.2^^^&1.3.6.1.4.1.21367.2005.13.20.1000&ISO'"; 
			//"'086666c2fd154f7^^^&1.3.6.1.4.1.21367.2005.13.20.3000&ISO'";
			String startDate = null;
			String endDate = null;

			HashMap<String, String> documents = queryRegistry(patientIdentifier, startDate, endDate);
			assertFalse("Error - no documents found",
					documents.isEmpty());
			
			RetrieveDocumentSetResponse documentSetResponse = xdsB.retrieveDocumentSet(xdsBRepositoryUniqueId,
					documents,
					patientIdentifier);
			log.info("XDS.b document set query response in log message immediately following...");
			OMElement requestElement = documentSetResponse.getOMElement(RetrieveDocumentSetResponse.MY_QNAME,
					soapFactory);
			log.info(requestElement);

			DocumentResponse_type0[] docResponseArray = documentSetResponse.getRetrieveDocumentSetResponse().getRetrieveDocumentSetResponseTypeSequence_type0().getDocumentResponse();
			assertFalse("Error - no documents returned",
					docResponseArray.length == 0);
			if (docResponseArray != null)
			{
				try
				{
					for (DocumentResponse_type0 document : docResponseArray)
					{
						String mimeType = docResponseArray[0].getMimeType().getLongName();
						if (mimeType.compareToIgnoreCase("text/xml") == 0)
						{
							String filename = /*"test/"*/ "c:/temp/Output/" + document.getDocumentUniqueId().getLongName() + ".xml";
							DataHandler dh = document.getDocument();
							File file = new File(filename);
							FileOutputStream fileOutStream = new FileOutputStream(file);
							dh.writeTo(fileOutStream);
							fileOutStream.close();
							
							log.info("Document written to "
									+ filename);
						}
					}
				}
				catch (Exception e)
				{
					throw e;
				}
			}
			
			log.info("XCA repository stored query unit test ending.");
		}
		catch (Exception e)
		{
			log.error("Error - " + e.getMessage());
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link org.iexhub.connectors.XdsB#retrieveDocumentSet(java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testRetrieveDocumentSet()
	{
		try
		{
			log.info("Repository document set retrieval query (ITI-43) unit test started...");
			xdsB = new XdsB(registryEndpointURI,
					repositoryEndpointURI);
			
			String startDate = null;
			String endDate = null;

			HashMap<String, String> documents = queryRegistry(patientIdentifier, startDate, endDate);
			assertFalse("Error - no documents found",
					documents.isEmpty());
			
			RetrieveDocumentSetResponse documentSetResponse = xdsB.retrieveDocumentSet(xdsBRepositoryUniqueId,
					documents,
					patientIdentifier);
			log.info("XDS.b document set query response in log message immediately following...");
			OMElement requestElement = documentSetResponse.getOMElement(RetrieveDocumentSetResponse.MY_QNAME,
					soapFactory);
			log.info(requestElement);

			DocumentResponse_type0[] docResponseArray = documentSetResponse.getRetrieveDocumentSetResponse().getRetrieveDocumentSetResponseTypeSequence_type0().getDocumentResponse();
			assertFalse("Error - no documents returned",
					docResponseArray.length == 0);
			if (docResponseArray != null)
			{
				try
				{
					for (DocumentResponse_type0 document : docResponseArray)
					{
						String mimeType = docResponseArray[0].getMimeType().getLongName();
						if (mimeType.compareToIgnoreCase("text/xml") == 0)
						{
							String filename = "/temp/Output/" + document.getDocumentUniqueId().getLongName() + ".xml";
							DataHandler dh = document.getDocument();
							File file = new File(filename);
							FileOutputStream fileOutStream = new FileOutputStream(file);
							dh.writeTo(fileOutStream);
							fileOutStream.close();
							
							log.info("Document written to "
									+ filename);
						}
					}
				}
				catch (Exception e)
				{
					throw e;
				}
			}
			
			log.info("Repository document set retrieval query (ITI-43) unit test ending.");
		}
		catch (Exception e)
		{
			log.error("Error-" + e.getMessage());
			fail("Error - " + e.getMessage());
		}
	}
	
	/**
	 * Test method for {@link org.iexhub.connectors.XdsB#retrieveDocumentSet(java.lang.String, java.lang.String, boolean)}.
	 */
	@Test
	public void testTLSRetrieveDocumentSet()
	{
		try
		{
			log.info("Repository TLS document set retrieval query (ITI-43) unit test started...");
			xdsB = new XdsB(xdsBRegistryTLSEndpointURI,
					xdsBRepositoryTLSEndpointURI,
					true);
			
//			String patientIdentifier = "IHERED-2332^^^&1.3.6.1.4.1.21367.13.20.1000&ISO^PI";
//			String patientIdentifier = "IHEBLUE-1019^^^&1.3.6.1.4.1.21367.13.20.3000&ISO";
//			String patientIdentifier = "IHEGREEN-2376^^^&1.3.6.1.4.1.21367.13.20.2000&ISO";
			String patientIdentifier = "940140b7-7c8b-4491-90f4-4819ded969bf^^^&1.3.6.1.4.1.21367.13.20.3000&ISO";
			String startDate = null;
			String endDate = null;

			HashMap<String, String> documents = queryRegistry(patientIdentifier, startDate, endDate);
			assertFalse("Error - no documents found",
					documents == null);
			assertFalse("Error - no documents found",
					documents.isEmpty());
			
			RetrieveDocumentSetResponse documentSetResponse = xdsB.retrieveDocumentSet(xdsBRepositoryUniqueId,
					documents,
					patientIdentifier);
			log.info("XDS.b document set query response in log message immediately following...");
			OMElement requestElement = documentSetResponse.getOMElement(RetrieveDocumentSetResponse.MY_QNAME,
					soapFactory);
			if (requestElement != null)
			{
				log.info(requestElement);
			}

			DocumentResponse_type0[] docResponseArray = documentSetResponse.getRetrieveDocumentSetResponse().getRetrieveDocumentSetResponseTypeSequence_type0().getDocumentResponse();
			assertFalse("Error - no documents found",
					documents == null);
			assertFalse("Error - no documents returned",
					docResponseArray.length == 0);
			if (docResponseArray != null)
			{
				try
				{
					for (DocumentResponse_type0 document : docResponseArray)
					{
						String mimeType = docResponseArray[0].getMimeType().getLongName();
						if (mimeType.compareToIgnoreCase("text/xml") == 0)
						{
							String filename = /*"test/"*/ "c:/temp/Output/" + document.getDocumentUniqueId().getLongName() + ".xml";
							DataHandler dh = document.getDocument();
							File file = new File(filename);
							FileOutputStream fileOutStream = new FileOutputStream(file);
							dh.writeTo(fileOutStream);
							fileOutStream.close();
							
							log.info("Document written to "
									+ filename);
						}
					}
				}
				catch (Exception e)
				{
					throw e;
				}
			}
			
			log.info("Repository TLS document set retrieval query (ITI-43) unit test ending.");
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}
}