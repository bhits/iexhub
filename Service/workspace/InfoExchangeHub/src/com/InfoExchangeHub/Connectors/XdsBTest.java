/**
 * 
 */
package com.InfoExchangeHub.Connectors;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;

import org.junit.Test;
import org.productivity.java.syslog4j.Syslog;
import org.productivity.java.syslog4j.SyslogConfigIF;
import org.productivity.java.syslog4j.impl.net.tcp.ssl.SSLTCPNetSyslogConfig;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.InfoExchangeHub.Exceptions.UnexpectedServerException;
import com.InfoExchangeHub.Services.Client.DocumentRegistry_ServiceStub.AdhocQueryResponse;
import com.InfoExchangeHub.Services.Client.DocumentRegistry_ServiceStub.ExternalIdentifierType;
import com.InfoExchangeHub.Services.Client.DocumentRegistry_ServiceStub.ExtrinsicObjectType;
import com.InfoExchangeHub.Services.Client.DocumentRegistry_ServiceStub.IdentifiableType;
import com.InfoExchangeHub.Services.Client.DocumentRegistry_ServiceStub.RegistryObjectListType;
import com.InfoExchangeHub.Services.Client.DocumentRepository_ServiceStub.DocumentResponse_type0;
import com.InfoExchangeHub.Services.Client.DocumentRepository_ServiceStub.RetrieveDocumentSetResponse;

/**
 * @author A. Sute
 *
 */
public class XdsBTest
{
    /** Logger */
    public static final Logger log = Logger.getLogger(XdsBTest.class);

	//	private static final String xdsBRegistryTLSEndpointURI = "https://ihexds.nist.gov:12091/tf6/services/xdsregistryb";
	private static final String xdsBRegistryTLSEndpointURI = "https://188.165.194.55:10011";
	private static final String xdsBRepositoryTLSEndpointURI = "https://ihexds.nist.gov:12091/tf6/services/xdsrepositoryb";

    private static final String KeyStoreFile = "c:/temp/1264.jks";
	private static final String KeyStorePwd = "IEXhub";
	private static final String CipherSuites = "TLS_RSA_WITH_AES_128_CBC_SHA";
	private static final String HttpsProtocols = "TLSv1";

	private static final String nistRepositoryId = "1.19.6.24.109.42.1.5";
	private static XdsB xdsB = null;
	private static final SOAPFactory soapFactory = OMAbstractFactory.getSOAP12Factory();

	private static String PropertiesFile = "/temp/IExHub.properties";

	private static boolean DebugSSL = false;

	private HashMap<String, String> queryRegistry(String enterpriseMRN,
			String startDate,
			String endDate)
	{
		HashMap<String, String> documents = null;

		try
		{
			AdhocQueryResponse registryResponse = xdsB.registryStoredQuery(enterpriseMRN,
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
								(val.compareToIgnoreCase("XDSDocumentEntry.uniqueId") == 0))
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
	 * Test method for {@link com.InfoExchangeHub.Connectors.XdsB#registryStoredQuery(java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testRegistryStoredQuery()
	{
		Properties props = new Properties();
		String registryEndpointURI = null;
		String repositoryEndpointURI = null;
		String syslogServerHost = null;
		int syslogServerPort = -1;
		try
		{
			props.load(new FileInputStream(PropertiesFile));
			DebugSSL = Boolean.parseBoolean(props.getProperty("DebugSSL"));
			registryEndpointURI = props.getProperty("XdsBRegistryEndpointURI");
			repositoryEndpointURI = props.getProperty("XdsBRepositoryEndpointURI");
			syslogServerHost = props.getProperty("SyslogServerHost");
			syslogServerPort = Integer.parseInt(props.getProperty("SyslogServerPort"));
		}
		catch (IOException e)
		{
			log.error("Error encountered loading properties file, "
					+ PropertiesFile
					+ ", "
					+ e.getMessage());
			throw new UnexpectedServerException("Error encountered loading properties file, "
					+ PropertiesFile
					+ ", "
					+ e.getMessage());
		}

		try
		{
			log.info("Registry stored query (ITI-18) unit test started...");
			xdsB = new XdsB(registryEndpointURI,
					repositoryEndpointURI);
			
			String enterpriseMRN = "'086666c2fd154f7^^^&1.3.6.1.4.1.21367.2005.13.20.3000&ISO'";
			String startDate = null;
			String endDate = null;

			assertFalse("Error - no documents found",
					queryRegistry(enterpriseMRN, startDate, endDate).isEmpty());
			
			log.info("Registry stored query (ITI-18) unit test ending.");
		}
		catch (Exception e)
		{
			log.error("Error - " + e.getMessage());
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link com.InfoExchangeHub.Connectors.XdsB#registryStoredQuery(java.lang.String, java.lang.String, java.lang.String)}.
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
			
			String enterpriseMRN = "'086666c2fd154f7^^^&1.3.6.1.4.1.21367.2005.13.20.3000&ISO'";
			String startDate = null;
			String endDate = null;

			assertFalse("Error - no documents found",
					queryRegistry(enterpriseMRN, startDate, endDate).isEmpty());
			log.info("Registry TLS stored query (ITI-18) unit test ending.");
		}
		catch (Exception e)
		{
			log.error("Error - " + e.getMessage());
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link com.InfoExchangeHub.Connectors.XdsB#registryStoredQuery(java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testXCARegistryStoredQuery()
	{
		try
		{
			log.info("XCA registry stored query (ITI-18/ITI-38) unit test started...");
			xdsB = new XdsB("http://ihexds.nist.gov:12090/tf6/services/xcaregistry",
					"http://ihexds.nist.gov:12090/tf6/services/xcarepository");
			
			String enterpriseMRN = "'f10f8d972aba4fd^^^&1.3.6.1.4.1.21367.2005.13.20.3000&ISO'"; 
			//"'086666c2fd154f7^^^&1.3.6.1.4.1.21367.2005.13.20.3000&ISO'";
			String startDate = null;
			String endDate = null;

			assertFalse("Error - no documents found",
					queryRegistry(enterpriseMRN, startDate, endDate).isEmpty());
			log.info("XCA registry stored query unit test ending.");
		}
		catch (Exception e)
		{
			log.error("Error - " + e.getMessage());
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link com.InfoExchangeHub.Connectors.XdsB#registryStoredQuery(java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testXCARepositoryStoredQuery()
	{
		try
		{
			log.info("XCA repository stored query unit test started...");
			xdsB = new XdsB("http://ihexds.nist.gov:12090/tf6/services/xcaregistry",
					"http://ihexds.nist.gov:12090/tf6/services/xcarepository");
			
			String enterpriseMRN = "'f10f8d972aba4fd^^^&1.3.6.1.4.1.21367.2005.13.20.3000&ISO'"; 
			//"'086666c2fd154f7^^^&1.3.6.1.4.1.21367.2005.13.20.3000&ISO'";
			String startDate = null;
			String endDate = null;

			HashMap<String, String> documents = queryRegistry(enterpriseMRN, startDate, endDate);
			assertFalse("Error - no documents found",
					documents.isEmpty());
			
			RetrieveDocumentSetResponse documentSetResponse = xdsB.retrieveDocumentSet(nistRepositoryId,
					documents);
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
	 * Test method for {@link com.InfoExchangeHub.Connectors.XdsB#retrieveDocumentSet(java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testRetrieveDocumentSet()
	{
		Properties props = new Properties();
		String registryEndpointURI = null;
		String repositoryEndpointURI = null;
		try
		{
			props.load(new FileInputStream(PropertiesFile));
			registryEndpointURI = props.getProperty("XdsBRegistryEndpointURI");
			repositoryEndpointURI = props.getProperty("XdsBRepositoryEndpointURI");
		}
		catch (IOException e)
		{
			log.error("Error encountered loading properties file, "
					+ PropertiesFile
					+ ", "
					+ e.getMessage());
			throw new UnexpectedServerException("Error encountered loading properties file, "
					+ PropertiesFile
					+ ", "
					+ e.getMessage());
		}

		try
		{
			log.info("Repository document set retrieval query (ITI-43) unit test started...");
			xdsB = new XdsB(registryEndpointURI,
					repositoryEndpointURI);
			
			String enterpriseMRN = "'086666c2fd154f7^^^&1.3.6.1.4.1.21367.2005.13.20.3000&ISO'";
			String startDate = null;
			String endDate = null;

			HashMap<String, String> documents = queryRegistry(enterpriseMRN, startDate, endDate);
			assertFalse("Error - no documents found",
					documents.isEmpty());
			
			RetrieveDocumentSetResponse documentSetResponse = xdsB.retrieveDocumentSet(nistRepositoryId,
					documents);
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
			
			log.info("Repository document set retrieval query (ITI-43) unit test ending.");
		}
		catch (Exception e)
		{
			log.error("Error-" + e.getMessage());
			fail("Error - " + e.getMessage());
		}
	}
	
	/**
	 * Test method for {@link com.InfoExchangeHub.Connectors.XdsB#retrieveDocumentSet(java.lang.String, java.lang.String, boolean)}.
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
			
			String enterpriseMRN = "'086666c2fd154f7^^^&1.3.6.1.4.1.21367.2005.13.20.3000&ISO'";
			String startDate = null;
			String endDate = null;

			HashMap<String, String> documents = queryRegistry(enterpriseMRN, startDate, endDate);
			assertFalse("Error - no documents found",
					documents.isEmpty());
			
			RetrieveDocumentSetResponse documentSetResponse = xdsB.retrieveDocumentSet(nistRepositoryId,
					documents);
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
			
			log.info("Repository TLS document set retrieval query (ITI-43) unit test ending.");
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}
}