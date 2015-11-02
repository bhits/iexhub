/**
 * 
 */
package com.InfoExchangeHub.Connectors;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.activation.DataHandler;

import org.junit.Test;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.log4j.Logger;

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
    public static Logger log = Logger.getLogger(XdsBTest.class);

	private static String XdsBRegistryEndpointURI = "http://ihexds.nist.gov:80/tf6/services/xdsregistryb";
	private static String XdsBRepositoryEndpointURI = "http://ihexds.nist.gov:80/tf6/services/xdsrepositoryb";
	private static String NistRepositoryId = "1.19.6.24.109.42.1.5";
	private static XdsB xdsB = null;
	private static SOAPFactory soapFactory = OMAbstractFactory.getSOAP12Factory();


	private List<String> QueryRegistry(String enterpriseMRN,
			String startDate,
			String endDate)
	{
		List<String> documentIds = null;

		try
		{
			AdhocQueryResponse registryResponse = xdsB.RegistryStoredQuery(enterpriseMRN,
					(startDate != null) ? DateFormat.getDateInstance().format(startDate) : null,
					(endDate != null) ? DateFormat.getDateInstance().format(endDate) : null);
			
			log.info("XDS.b registry stored query response in log message immediately following...");
			OMElement requestElement = registryResponse.getOMElement(AdhocQueryResponse.MY_QNAME,
					soapFactory);
			log.info(requestElement);
			
			// Try to retrieve document ID's...
			RegistryObjectListType registryObjectList = registryResponse.getRegistryObjectList();
			IdentifiableType[] documentObjects = registryObjectList.getIdentifiable();
			if ((documentObjects != null) &&
				(documentObjects.length > 0))
			{
				documentIds = new ArrayList<String>();
				for (IdentifiableType identifiable : documentObjects)
				{
					if (identifiable.getClass().equals(ExtrinsicObjectType.class))
					{
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
							documentIds.add(uniqueId);
							log.info("Document ID added: "
									+ uniqueId);
						}
					}
					else
					{
						documentIds.add(identifiable.getId().getPath());
						log.info("Document ID added: "
								+ identifiable.getId().getPath());
					}
				}
			}
		}
		catch (Exception e)
		{
			log.error("Error - " + e.getMessage());
			fail("Error - " + e.getMessage());
		}
		
		return documentIds;
	}
	
	/**
	 * Test method for {@link com.InfoExchangeHub.Connectors.XdsB#RegistryStoredQuery(java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testRegistryStoredQuery()
	{
		try
		{
			log.info("Registry stored query (ITI-18) unit test started...");
			xdsB = new XdsB(XdsBRegistryEndpointURI,
					XdsBRepositoryEndpointURI);
			
			String enterpriseMRN = "'086666c2fd154f7^^^&1.3.6.1.4.1.21367.2005.13.20.3000&ISO'";
			String startDate = null;
			String endDate = null;

			assertFalse("Error - no documents found",
					QueryRegistry(enterpriseMRN, startDate, endDate).isEmpty());
			log.info("Registry stored query (ITI-18) unit test ending.");
		}
		catch (Exception e)
		{
			log.error("Error - " + e.getMessage());
			fail("Error - " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link com.InfoExchangeHub.Connectors.XdsB#RetrieveDocumentSet(java.lang.String, java.util.List)}.
	 */
	@Test
	public void testRetrieveDocumentSet()
	{
		try
		{
			log.info("Repository document set retrieval query (ITI-43) unit test started...");
			xdsB = new XdsB(XdsBRegistryEndpointURI,
					XdsBRepositoryEndpointURI);
			
			String enterpriseMRN = "'086666c2fd154f7^^^&1.3.6.1.4.1.21367.2005.13.20.3000&ISO'";
			String startDate = null;
			String endDate = null;

			List<String> documentIds = QueryRegistry(enterpriseMRN, startDate, endDate);
			assertFalse("Error - no documents found",
					documentIds.isEmpty());
			
			RetrieveDocumentSetResponse documentSetResponse = xdsB.RetrieveDocumentSet(NistRepositoryId,
					documentIds);
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
			fail("Error - " + e.getMessage());
		}
	}
}