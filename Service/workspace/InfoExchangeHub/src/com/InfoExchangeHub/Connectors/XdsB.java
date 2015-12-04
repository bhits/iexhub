package com.InfoExchangeHub.Connectors;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.log4j.Logger;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyEngine;
import org.apache.rampart.RampartMessageData;
import org.apache.rampart.policy.model.CryptoConfig;
import org.apache.rampart.policy.model.RampartConfig;
import org.apache.rampart.policy.model.SSLConfig;

import com.InfoExchangeHub.Exceptions.UnexpectedServerException;
import com.InfoExchangeHub.Services.Client.DocumentRegistry_ServiceStub;
import com.InfoExchangeHub.Services.Client.DocumentRegistry_ServiceStub.AdhocQueryRequest;
import com.InfoExchangeHub.Services.Client.DocumentRegistry_ServiceStub.AdhocQueryResponse;
import com.InfoExchangeHub.Services.Client.DocumentRegistry_ServiceStub.AdhocQueryType;
import com.InfoExchangeHub.Services.Client.DocumentRegistry_ServiceStub.LongName;
import com.InfoExchangeHub.Services.Client.DocumentRegistry_ServiceStub.ResponseOptionType;
import com.InfoExchangeHub.Services.Client.DocumentRegistry_ServiceStub.ReturnType_type0;
import com.InfoExchangeHub.Services.Client.DocumentRegistry_ServiceStub.SlotType1;
import com.InfoExchangeHub.Services.Client.DocumentRegistry_ServiceStub.ValueListType;
import com.InfoExchangeHub.Services.Client.DocumentRegistry_ServiceStub.ValueListTypeSequence;
import com.InfoExchangeHub.Services.Client.DocumentRepository_ServiceStub;
import com.InfoExchangeHub.Services.Client.DocumentRepository_ServiceStub.DocumentRequest_type0;
import com.InfoExchangeHub.Services.Client.DocumentRepository_ServiceStub.RetrieveDocumentSetRequest;
import com.InfoExchangeHub.Services.Client.DocumentRepository_ServiceStub.RetrieveDocumentSetRequestType;
import com.InfoExchangeHub.Services.Client.DocumentRepository_ServiceStub.RetrieveDocumentSetResponse;

/**
 * @author A. Sute
 *
 */
public class XdsB
{
    private static final String KeyStoreFile = "c:/temp/1264.jks";
	private static final String KeyStorePwd = "IEXhub";
	private static final String CipherSuites = "TLS_RSA_WITH_AES_128_CBC_SHA";
	private static final String HttpsProtocols = "TLSv1";

	/** Logger */
    public static Logger log = Logger.getLogger(XdsB.class);

	private static String PropertiesFile = "/temp/IExHub.properties";

    private static DocumentRegistry_ServiceStub registryStub = null;
	private static DocumentRepository_ServiceStub repositoryStub = null;

	private static boolean DebugSSL = false;

	public static void setRegistryEndpointURI(String registryEndpointURI)
	{
		if (registryStub != null)
		{
			registryStub._getServiceClient().getOptions().setTo(new org.apache.axis2.addressing.EndpointReference(registryEndpointURI));
		}
	}
	
	public static void setRepositoryEndpointURI(String repositoryEndpointURI)
	{
		if (repositoryStub != null)
		{
			repositoryStub._getServiceClient().getOptions().setTo(new org.apache.axis2.addressing.EndpointReference(repositoryEndpointURI));
		}
	}

	public XdsB(String registryEndpointURI,
			String repositoryEndpointURI) throws AxisFault, Exception
	{
		this(registryEndpointURI,
				repositoryEndpointURI,
				false);
	}
	
	public XdsB(String registryEndpointURI,
			String repositoryEndpointURI,
			boolean enableTLS) throws AxisFault, Exception
	{
		Properties props = new Properties();
		try
		{
			props.load(new FileInputStream(PropertiesFile));
			DebugSSL = Boolean.parseBoolean(props.getProperty("DebugSSL"));
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
			if (registryEndpointURI != null)
			{
				// Instantiate DocumentRegistry client stub and enable WS-Addressing...
				registryStub = new DocumentRegistry_ServiceStub(registryEndpointURI);
				registryStub._getServiceClient().engageModule("addressing");

				if (enableTLS)
				{
//					registryStub._getServiceClient().getOptions().setProperty(RampartMessageData.KEY_RAMPART_POLICY,
//				            loadPolicy("policy.xml"));

//					registryStub._getServiceClient().getOptions().setProperty(org.apache.axis2.transport.http.HTTPConstants.HTTP_PROTOCOL_VERSION,
//							org.apache.axis2.transport.http.HTTPConstants.HEADER_PROTOCOL_10);
					
//					System.setProperty("javax.net.ssl.trustStore",
//							"/temp/1264.jks");
//					System.setProperty("javax.net.ssl.trustStorePassword",
//							"IEXhub");
//					registryStub._getServiceClient().engageModule("rampart");
					
//					Policy policy = loadPolicy("c:/temp/policy.xml");
					
//					RampartConfig rampartConfig = new RampartConfig();
//					rampartConfig.setUser("client");
//					rampartConfig.setPwCbClass("com.InfoExchangeHub.Connectors.PWCBHandler");
//
//					CryptoConfig sigCrypto = new CryptoConfig();
//					sigCrypto.setProvider("org.apache.ws.security.components.crypto.Merlin");
//
//					Properties props = new Properties();
//					props.setProperty("org.apache.ws.security.crypto.merlin.keystore.type", "JKS");
//					props.setProperty("org.apache.ws.security.crypto.merlin.file",
//							"c:/temp/1264.jks");
//					props.setProperty("org.apache.ws.security.crypto.merlin.keystore.password",
//							"IEXhub");
//
//					sigCrypto.setProp(props);
//					rampartConfig.setSigCryptoConfig(sigCrypto);
//
////					Policy policy = new Policy();
//					policy.addAssertion(rampartConfig);

//					System.setProperty("javax.net.ssl.keyStore",
//							KeyStoreFile);
//					System.setProperty("javax.net.ssl.keyStorePassword",
//							KeyStorePwd);
//					registryStub._getServiceClient().getOptions().setProperty(RampartMessageData.KEY_RAMPART_POLICY,
//							loadPolicy("c:/temp/policy.xml"));
//					registryStub._getServiceClient().getAxisService().getPolicySubject().attachPolicy(loadPolicy("c:/temp/policy.xml"));

					System.setProperty("javax.net.ssl.keyStore",
							(props.getProperty("KeyStoreFile") == null) ? KeyStoreFile
									: props.getProperty("KeyStoreFile"));
					System.setProperty("javax.net.ssl.keyStorePassword",
							(props.getProperty("KeyStorePwd") == null) ? KeyStorePwd
									: props.getProperty("KeyStorePwd"));
					System.setProperty("javax.net.ssl.trustStore",
							(props.getProperty("KeyStoreFile") == null) ? KeyStoreFile
									: props.getProperty("KeyStoreFile"));
					System.setProperty("javax.net.ssl.trustStorePassword",
							(props.getProperty("KeyStorePwd") == null) ? KeyStorePwd
									: props.getProperty("KeyStorePwd"));
					System.setProperty("https.cipherSuites",
							(props.getProperty("CipherSuites") == null) ? CipherSuites
									: props.getProperty("CipherSuites"));
					System.setProperty("https.protocols",
							(props.getProperty("HttpsProtocols") == null) ? HttpsProtocols
									: props.getProperty("HttpsProtocols"));
					
					if (DebugSSL)
					{
						System.setProperty("javax.net.debug",
								"ssl");
					}
					
					registryStub._getServiceClient().engageModule("rampart");
				}
			}

			if (repositoryEndpointURI != null)
			{
				// Instantiate DocumentRepository client stub and enable WS-Addressing and MTOM...
				repositoryStub = new DocumentRepository_ServiceStub(repositoryEndpointURI);
				repositoryStub._getServiceClient().engageModule("addressing");
				repositoryStub._getServiceClient().getOptions().setProperty(Constants.Configuration.ENABLE_MTOM, Constants.VALUE_TRUE);
				
				if (enableTLS)
				{
					System.setProperty("javax.net.ssl.keyStore",
							(props.getProperty("KeyStoreFile") == null) ? KeyStoreFile
									: props.getProperty("KeyStoreFile"));
					System.setProperty("javax.net.ssl.keyStorePassword",
							(props.getProperty("KeyStorePwd") == null) ? KeyStorePwd
									: props.getProperty("KeyStorePwd"));
					System.setProperty("javax.net.ssl.trustStore",
							(props.getProperty("KeyStoreFile") == null) ? KeyStoreFile
									: props.getProperty("KeyStoreFile"));
					System.setProperty("javax.net.ssl.trustStorePassword",
							(props.getProperty("KeyStorePwd") == null) ? KeyStorePwd
									: props.getProperty("KeyStorePwd"));
					System.setProperty("https.cipherSuites",
							(props.getProperty("CipherSuites") == null) ? CipherSuites
									: props.getProperty("CipherSuites"));
					System.setProperty("https.protocols",
							(props.getProperty("HttpsProtocols") == null) ? HttpsProtocols
									: props.getProperty("HttpsProtocols"));
					
					if (DebugSSL)
					{
						System.setProperty("javax.net.debug",
								"ssl");
					}
					
					repositoryStub._getServiceClient().engageModule("rampart");
				}
			}
			
			log.info("XdsB connector successfully initialized, registryEndpointURI="
					+ registryEndpointURI
					+ ", repositoryEndpointURI="
					+ repositoryEndpointURI);
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	 /**
     * Load policy file from classpath.
	 * @throws FileNotFoundException 
     */
//    private static org.apache.neethi.Policy loadPolicy(String xmlPath)
//    		throws XMLStreamException,
//    		FileNotFoundException
//    {
//    	StAXOMBuilder builder = new StAXOMBuilder(xmlPath);
//        Policy policy = PolicyEngine.getPolicy(builder.getDocumentElement());
//
//        RampartConfig rc = new RampartConfig();
//        rc.setUser("client");
//        rc.setPwCbClass(PWCBHandler.class.getName());
//
//        CryptoConfig encryptionCryptoConfig = new CryptoConfig();
//        encryptionCryptoConfig.setProvider("org.apache.ws.security.components.crypto.Merlin");
////        encryptionCryptoConfig.setCryptoKey("org.apache.ws.security.crypto.merlin.file");
//
//        Properties cryptoProperties = new Properties();
//        cryptoProperties.put("org.apache.ws.security.crypto.merlin.keystore.type",
//        		"JKS");
//        cryptoProperties.put("org.apache.ws.security.crypto.merlin.file",
//        		KeyStoreFile);
//        cryptoProperties.put("org.apache.ws.security.crypto.merlin.keystore.password",
//        		KeyStorePwd);
//        encryptionCryptoConfig.setProp(cryptoProperties);
//        rc.setEncrCryptoConfig(encryptionCryptoConfig);
//
//        Properties sslProperties = new Properties();
//        sslProperties.put("javax.net.ssl.trustStore",
//				KeyStoreFile);
//        sslProperties.put("javax.net.ssl.trustStorePassword",
//        		KeyStorePwd);
//        SSLConfig sslConfig = new SSLConfig();
//        sslConfig.setProp(sslProperties);
//        rc.setSSLConfig(sslConfig);
//
//        policy.addAssertion(rc);
//        
//        return policy;
//    }

	public AdhocQueryResponse registryStoredQuery(String patientID,
			String queryStartDate,
			String queryEndDate) throws Exception
	{
		AdhocQueryRequest request = new AdhocQueryRequest();
		AdhocQueryType adhocQuery = new AdhocQueryType();
		
//		patientID = "'086666c2fd154f7^^^&1.3.6.1.4.1.21367.2005.13.20.3000&ISO'";
		
		SlotType1 slot = new SlotType1();
		LongName name = new LongName();
		name.setLongName("$XDSDocumentEntryPatientId");
		slot.setName(name);
		ValueListType valueList = new ValueListType();
		ValueListTypeSequence[] valueListSequenceArray = new ValueListTypeSequence[1];
		ValueListTypeSequence valueListSequence = new ValueListTypeSequence(); 
		LongName valueName = new LongName();
		valueName.setLongName(patientID);
		valueListSequence.setValue(valueName);
		valueListSequenceArray[0] = valueListSequence;
		valueList.setValueListTypeSequence(valueListSequenceArray);
		slot.setValueList(valueList);
		adhocQuery.addSlot(slot);
		
		slot = new SlotType1();
		name = new LongName();
		name.setLongName("$XDSDocumentEntryStatus");
		slot.setName(name);
		valueList = new ValueListType();
		valueListSequenceArray = new ValueListTypeSequence[1];
		valueListSequence = new ValueListTypeSequence();
		valueName = new LongName();
		valueName.setLongName("('urn:oasis:names:tc:ebxml-regrep:StatusType:Approved')");
		valueListSequence.setValue(valueName);
		valueListSequenceArray[0] = valueListSequence;
		valueList.setValueListTypeSequence(valueListSequenceArray);
		slot.setValueList(valueList);
		adhocQuery.addSlot(slot);

		if (queryStartDate != null)
		{
			slot = new SlotType1();
			name = new LongName();
			name.setLongName("$XDSDocumentEntryCreationTimeFrom");
			slot.setName(name);
			valueList = new ValueListType();
			valueListSequenceArray = new ValueListTypeSequence[1];
			valueListSequence = new ValueListTypeSequence(); 
			valueName = new LongName();
			valueName.setLongName(queryStartDate);
			valueListSequence.setValue(valueName);
			valueListSequenceArray[0] = valueListSequence;
			valueList.setValueListTypeSequence(valueListSequenceArray);
			slot.setValueList(valueList);
			adhocQuery.addSlot(slot);
		}

		if (queryEndDate != null)
		{
			slot = new SlotType1();
			name = new LongName();
			name.setLongName("$XDSDocumentEntryCreationTimeTo");
			slot.setName(name);
			valueList = new ValueListType();
			valueListSequenceArray = new ValueListTypeSequence[1];
			valueListSequence = new ValueListTypeSequence(); 
			valueName = new LongName();
			valueName.setLongName(queryEndDate);
			valueListSequence.setValue(valueName);
			valueListSequenceArray[0] = valueListSequence;
			valueList.setValueListTypeSequence(valueListSequenceArray);
			slot.setValueList(valueList);
			adhocQuery.addSlot(slot);
		}
		
		try
		{
			org.apache.axis2.databinding.types.URI id = new org.apache.axis2.databinding.types.URI();
			id.setPath("urn:uuid:14d4debf-8f97-4251-9a74-a90016b0af0d");
			adhocQuery.setId(id);
			request.setAdhocQuery(adhocQuery);
			
			ResponseOptionType responseOption = new ResponseOptionType();
			responseOption.setReturnComposedObjects(true);
			responseOption.setReturnType(ReturnType_type0.LeafClass);
			request.setResponseOption(responseOption);
	
			return registryStub.documentRegistry_RegistryStoredQuery(request);
		}
		catch (Exception e)
		{
			throw e;
		}
	}
	
	public RetrieveDocumentSetResponse retrieveDocumentSet(String repositoryUniqueIdVal,
			HashMap<String, String> documents) throws Exception
	{
		try
		{
			RetrieveDocumentSetRequestType documentSetRequestType = new RetrieveDocumentSetRequestType();
			RetrieveDocumentSetRequest documentSetRequest = new RetrieveDocumentSetRequest();
			
			for (String documentId : documents.keySet())
			{
				DocumentRequest_type0 documentRequest = new DocumentRequest_type0();
				com.InfoExchangeHub.Services.Client.DocumentRepository_ServiceStub.LongName repositoryUniqueId = new com.InfoExchangeHub.Services.Client.DocumentRepository_ServiceStub.LongName();
				repositoryUniqueId.setLongName(repositoryUniqueIdVal);
				documentRequest.setRepositoryUniqueId(repositoryUniqueId);
				
				if (documents.get(documentId) != null)
				{
					com.InfoExchangeHub.Services.Client.DocumentRepository_ServiceStub.LongName homeCommunityId = new com.InfoExchangeHub.Services.Client.DocumentRepository_ServiceStub.LongName();
					homeCommunityId.setLongName(documents.get(documentId).toString());
					documentRequest.setHomeCommunityId(homeCommunityId);
				}
				
				com.InfoExchangeHub.Services.Client.DocumentRepository_ServiceStub.LongName documentUniqueId = new com.InfoExchangeHub.Services.Client.DocumentRepository_ServiceStub.LongName();
				documentUniqueId.setLongName(documentId);
				documentRequest.setDocumentUniqueId(documentUniqueId);
				documentSetRequestType.addDocumentRequest(documentRequest);
			}
			
			documentSetRequest.setRetrieveDocumentSetRequest(documentSetRequestType);
			
			return repositoryStub.documentRepository_RetrieveDocumentSet(documentSetRequest);
		}
		catch (Exception e)
		{
			throw e;
		}
	}
}