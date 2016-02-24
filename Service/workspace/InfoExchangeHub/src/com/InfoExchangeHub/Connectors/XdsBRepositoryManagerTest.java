/**
 * 
 */
package com.InfoExchangeHub.Connectors;

import static org.junit.Assert.*;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;

import org.junit.Test;
import org.apache.log4j.Logger;

/**
 * @author A. Sute
 *
 */
public class XdsBRepositoryManagerTest
{
    /** Logger */
    public static final Logger log = Logger.getLogger(XdsBRepositoryManagerTest.class);

	private static final String xdsBRegistryEndpointURI = "http://ihexds.nist.gov:80/tf6/services/xdsregistryb";
	private static final String xdsBRepositoryEndpointURI = "http://ihexds.nist.gov:80/tf6/services/xdsrepositoryb";
//	private static final String xdsBRegistryTLSEndpointURI = "	https://nist1:9085/tf6/services/xdsregistryb";
//	private static final String xdsBRepositoryTLSEndpointURI = "https://nist1:9085/tf6/services/xdsrepositoryb";
//	private static final String xdsBRegistryTLSEndpointURI = "https://philips50:8443/philips/services/xdsregistry";
//	private static final String xdsBRepositoryTLSEndpointURI = "https://philips50:8443/philips/services/xdsrepository";
//	private static final String xdsBRegistryTLSEndpointURI = "https://nist1:9085/tf6/services/xdsregistryb";
//	private static final String xdsBRepositoryTLSEndpointURI = "https://nist1:9085/tf6/services/xdsrepositoryb";
//	private static final String xdsBRegistryTLSEndpointURI = "https://cerner14:9070/ihe/services/xdsregistryb";
//	private static final String xdsBRepositoryTLSEndpointURI = "https://tiani---cisco72:8443/XDS3/rep";
//	private static final String xdsBRegistryTLSEndpointURI = "https://ith-icoserve12:1243/Registry/services/RegistryService";
//	private static final String xdsBRegistryTLSEndpointURI = "https://merge11:443/iti18";
//	private static final String xdsBRepositoryTLSEndpointURI = "https://10.242.43.13:5000/repository";
	private static final String xdsBRegistryTLSEndpointURI = "https://demohie.thinkengage.com/XDS3/reg";
	private static final String xdsBRepositoryTLSEndpointURI = "https://demohie.thinkengage.com/SpiritProxy/repository";

	private static XdsBRepositoryManager XdsBRepository = null;
	/**
	 * Test method for {@link com.InfoExchangeHub.Connectors.XdsBRepositoryManager#ProvideAndRegisterDocumentSet(java.lang.String, java.util.List)}.
	 */
	@Test
	public void testProvideAndRegisterDocumentSet()
	{
		String cdaFilename = "c:/temp/b2 Adam Everyman ToC_IHERED-2332.xml";
		try
		{
			log.info("Repository ProvideAndRegisterDocumentSet (ITI-41) unit test started...");
			XdsBRepository = new XdsBRepositoryManager(xdsBRegistryEndpointURI,
					xdsBRepositoryEndpointURI);
			
			// Read contents of file into string...
			byte[] fileContents = readAllBytes(get(cdaFilename));
			
			XdsBRepository.provideAndRegisterDocumentSet(fileContents,
					"text/xml");
			
			log.info("XDS.b document set query response in log message immediately following...");
//			OMElement requestElement = documentSetResponse.getOMElement(RetrieveDocumentSetResponse.MY_QNAME,
//					soapFactory);
//			log.info(requestElement);

			log.info("Repository ProvideAndRegisterDocumentSet (ITI-41) unit test ending.");
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}
	
	/**
	 * Test method for {@link com.InfoExchangeHub.Connectors.XdsBRepositoryManager#ProvideAndRegisterDocumentSet(java.lang.String, java.util.List)}.
	 */
	@Test
	public void testTLSProvideAndRegisterDocumentSet()
	{
//		String cdaFilename = "c:/temp/b2 Adam Everyman ToC_IHERED-2298.xml";
//		String cdaFilename = "c:/temp/b2 Adam Everyman ToC_IHEGREEN-2376.xml";
//		String cdaFilename = "c:/temp/b2 Adam Everyman ToC_IHEBLUE-1019.xml";
		String cdaFilename = "c:/temp/Sally_Share_b1_Ambulatory_v3.xml";
		try
		{
			log.info("Repository TLS ProvideAndRegisterDocumentSet (ITI-41) unit test started...");
			XdsBRepository = new XdsBRepositoryManager(xdsBRegistryTLSEndpointURI,
					xdsBRepositoryTLSEndpointURI,
					true);
			
			// Read contents of file into string...
			byte[] fileContents = readAllBytes(get(cdaFilename));
			
			XdsBRepository.provideAndRegisterDocumentSet(fileContents,
					"text/xml");
			
			log.info("XDS.b document set query response in log message immediately following...");
//			OMElement requestElement = documentSetResponse.getOMElement(RetrieveDocumentSetResponse.MY_QNAME,
//					soapFactory);
//			log.info(requestElement);

			log.info("Repository TLS ProvideAndRegisterDocumentSet (ITI-41) unit test ending.");
		}
		catch (Exception e)
		{
			fail("Error - " + e.getMessage());
		}
	}
}