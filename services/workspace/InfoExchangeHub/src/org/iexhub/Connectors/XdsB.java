package com.InfoExchangeHub.Connectors;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.productivity.java.syslog4j.Syslog;
import org.productivity.java.syslog4j.impl.net.tcp.ssl.SSLTCPNetSyslogConfig;

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
	private static boolean logXdsBRequestMessages = false;
	private static String logOutputPath = "c:/temp/";
	private static boolean logSyslogAuditMsgsLocally = false;

	private static String keyStoreFile = "c:/temp/1264.jks";
	private static String keyStorePwd = "IEXhub";
	private static String cipherSuites = "TLS_RSA_WITH_AES_128_CBC_SHA";
	private static String httpsProtocols = "TLSv1";

	private static String iti18AuditMsgTemplate = null;
	private static String iti43AuditMsgTemplate = null;
	
	/** Logger */
    public static Logger log = Logger.getLogger(XdsB.class);

	private static final String propertiesFile = "/temp/IExHub.properties";

    private static DocumentRegistry_ServiceStub registryStub = null;
	private static DocumentRepository_ServiceStub repositoryStub = null;

	private static final SOAPFactory soapFactory = OMAbstractFactory.getSOAP12Factory();

	private static boolean debugSSL = false;

	private static String registryEndpointURI = null;
	private static String repositoryEndpointURI = null;
	private static boolean testMode = false;

	private static SSLTCPNetSyslogConfig sysLogConfig = null;

	public static void setRegistryEndpointURI(String registryEndpointURI)
	{
		if (registryStub != null)
		{
			registryStub._getServiceClient().getOptions().setTo(new org.apache.axis2.addressing.EndpointReference(registryEndpointURI));
			XdsB.registryEndpointURI = registryEndpointURI;
		}
	}
	
	public static void setRepositoryEndpointURI(String repositoryEndpointURI)
	{
		if (repositoryStub != null)
		{
			repositoryStub._getServiceClient().getOptions().setTo(new org.apache.axis2.addressing.EndpointReference(repositoryEndpointURI));
			XdsB.repositoryEndpointURI = repositoryEndpointURI;
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
			props.load(new FileInputStream(propertiesFile));
			
			XdsB.logSyslogAuditMsgsLocally = (props.getProperty("LogSyslogAuditMsgsLocally") == null) ? XdsB.logSyslogAuditMsgsLocally
					: Boolean.parseBoolean(props.getProperty("LogSyslogAuditMsgsLocally"));
			XdsB.logOutputPath = (props.getProperty("LogOutputPath") == null) ? XdsB.logOutputPath
					: props.getProperty("LogOutputPath");
			XdsB.logXdsBRequestMessages = (props.getProperty("LogXdsBRequestMessages") == null) ? XdsB.logXdsBRequestMessages
					: Boolean.parseBoolean(props.getProperty("LogXdsBRequestMessages"));
			XdsB.debugSSL = (props.getProperty("DebugSSL") == null) ? XdsB.debugSSL
					: Boolean.parseBoolean(props.getProperty("DebugSSL"));
			XdsB.testMode  = (props.getProperty("TestMode") == null) ? XdsB.testMode
					: Boolean.parseBoolean(props.getProperty("TestMode"));
			XdsB.keyStoreFile = (props.getProperty("XdsBKeyStoreFile") == null) ? XdsB.keyStoreFile
					: props.getProperty("XdsBKeyStoreFile");
			XdsB.keyStorePwd = (props.getProperty("XdsBKeyStorePwd") == null) ? XdsB.keyStorePwd
					: props.getProperty("XdsBKeyStorePwd");
			XdsB.cipherSuites = (props.getProperty("XdsBCipherSuites") == null) ? XdsB.cipherSuites
					: props.getProperty("XdsBCipherSuites");
			XdsB.httpsProtocols = (props.getProperty("XdsBHttpsProtocols") == null) ? XdsB.httpsProtocols
					: props.getProperty("XdsBHttpsProtocols");
			
			// If endpoint URI's are null, then set to the values in the properties file...
			if (registryEndpointURI == null)
			{
				registryEndpointURI = props.getProperty("XdsBRegistryEndpointURI");
			}
			
			if (repositoryEndpointURI == null)
			{
				repositoryEndpointURI = props.getProperty("XdsBRepositoryEndpointURI");
			}

			XdsB.registryEndpointURI = registryEndpointURI;
			XdsB.repositoryEndpointURI = repositoryEndpointURI;

			// If Syslog server host is specified, then configure...
			String syslogServerHost = props.getProperty("SyslogServerHost");
			int syslogServerPort = (props.getProperty("SyslogServerPort") != null) ? Integer.parseInt(props.getProperty("SyslogServerPort"))
					: -1;
			if ((syslogServerHost != null) &&
				(syslogServerPort > -1))
			{
				iti18AuditMsgTemplate = props.getProperty("Iti18AuditMsgTemplate");
				iti43AuditMsgTemplate = props.getProperty("Iti43AuditMsgTemplate");
				if ((iti18AuditMsgTemplate == null) ||
					(iti43AuditMsgTemplate == null))
				{
					log.error("ITI-18 audit message template or ITI-43 audit message template not specified in properties file, "
							+ propertiesFile);
					throw new UnexpectedServerException("ITI-18 audit message template or ITI-43 audit message template not specified in properties file, "
							+ propertiesFile);
				}

				// TCP over SSL (secure) syslog
//				System.setProperty("javax.net.ssl.keyStore",
//						keyStoreFile);
//				System.setProperty("javax.net.ssl.keyStorePassword",
//						keyStorePwd);
//				System.setProperty("javax.net.ssl.trustStore",
//						keyStoreFile);
//				System.setProperty("javax.net.ssl.trustStorePassword",
//						keyStorePwd);
				System.setProperty("https.cipherSuites",
						cipherSuites);
				System.setProperty("https.protocols",
						httpsProtocols);
				
				if (debugSSL)
				{
					System.setProperty("javax.net.debug",
							"ssl");
				}

				sysLogConfig = new SSLTCPNetSyslogConfig();
				sysLogConfig.setHost(syslogServerHost);
				sysLogConfig.setPort(syslogServerPort);
				sysLogConfig.setKeyStore(keyStoreFile);
				sysLogConfig.setKeyStorePassword(keyStorePwd);
				sysLogConfig.setTrustStore(keyStoreFile);
				sysLogConfig.setTrustStorePassword(keyStorePwd);
				sysLogConfig.setUseStructuredData(true);
				sysLogConfig.setMaxMessageLength(8192);
				Syslog.createInstance("sslTcp",
						sysLogConfig);
			}
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
							keyStoreFile);
					System.setProperty("javax.net.ssl.keyStorePassword",
							keyStorePwd);
					System.setProperty("javax.net.ssl.trustStore",
							keyStoreFile);
					System.setProperty("javax.net.ssl.trustStorePassword",
							keyStorePwd);
					System.setProperty("https.cipherSuites",
							cipherSuites);
					System.setProperty("https.protocols",
							httpsProtocols);
					
					if (debugSSL)
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
							keyStoreFile);
					System.setProperty("javax.net.ssl.keyStorePassword",
							keyStorePwd);
					System.setProperty("javax.net.ssl.trustStore",
							keyStoreFile);
					System.setProperty("javax.net.ssl.trustStorePassword",
							keyStorePwd);
					System.setProperty("https.cipherSuites",
							cipherSuites);
					System.setProperty("https.protocols",
							httpsProtocols);
					
					if (debugSSL)
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
	 * @throws IOException 
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

	private void logIti18AuditMsg(String queryText,
			String patientId) throws IOException
	{
		if ((sysLogConfig == null) ||
            (iti18AuditMsgTemplate == null))
		{
			return;
		}
		
		String logMsg = FileUtils.readFileToString(new File(iti18AuditMsgTemplate));
		
		// Substitutions...
		patientId = patientId.replace("'",
				"");
		patientId = patientId.replace("&",
				"&amp;");
		
		DateTime now = new DateTime(DateTimeZone.UTC);
		DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
		logMsg = logMsg.replace("$DateTime$",
				fmt.print(now));
		
		logMsg = logMsg.replace("$AltUserId$",
				"IExHub");
		
		logMsg = logMsg.replace("$IexhubIpAddress$",
				InetAddress.getLocalHost().getHostAddress());
		
		logMsg = logMsg.replace("$IexhubUserId$",
				"http://" + InetAddress.getLocalHost().getCanonicalHostName());
		
		logMsg = logMsg.replace("$DestinationIpAddress$",
				XdsB.registryEndpointURI);
		
		logMsg = logMsg.replace("$DestinationUserId$",
				"IExHub");
		
		// Query text must be Base64 encoded...
		logMsg = logMsg.replace("$RegistryQueryMtom$",
				Base64.encodeBase64String(queryText.getBytes()));
		
		logMsg = logMsg.replace("$PatientId$",
				patientId);

		if (logSyslogAuditMsgsLocally)
		{
			log.info(logMsg);
		}

		// Log the syslog message and close connection
		Syslog.getInstance("sslTcp").info(logMsg);
		Syslog.getInstance("sslTcp").flush();
	}

	private void logIti43AuditMsg(String documentId,
			String repositoryUniqueId,
			String homeCommunityId,
			String patientId) throws IOException
	{
		if ((sysLogConfig == null) ||
            (iti43AuditMsgTemplate == null))
		{
			return;
		}

		String logMsg = FileUtils.readFileToString(new File(iti43AuditMsgTemplate));
		
		// Substitutions...
		patientId = patientId.replace("'",
				"");
		patientId = patientId.replace("&",
				"&amp;");
		
		DateTime now = new DateTime(DateTimeZone.UTC);
		DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
		logMsg = logMsg.replace("$DateTime$",
				fmt.print(now));
		
		logMsg = logMsg.replace("$AltUserId$",
				"IExHub");
		
		logMsg = logMsg.replace("$IexhubIpAddress$",
				InetAddress.getLocalHost().getHostAddress());
		
		logMsg = logMsg.replace("$IexhubUserId$",
				"http://" + InetAddress.getLocalHost().getCanonicalHostName());
		
		logMsg = logMsg.replace("$DestinationIpAddress$",
				XdsB.registryEndpointURI);
		
		logMsg = logMsg.replace("$DestinationUserId$",
				"IExHub");
		
		// Repository ID text must be Base64 encoded...
		logMsg = logMsg.replace("$RepositoryIdMtom$",
				Base64.encodeBase64String(repositoryUniqueId.getBytes()));
		
		logMsg = logMsg.replace("$PatientId$",
				patientId);
		
		logMsg = logMsg.replace("$DocumentId$",
				documentId);
		
		if (homeCommunityId != null)
		{
			logMsg = logMsg.replace("$HomeCommunityId$",
					homeCommunityId);
		}
		else
		{
			logMsg = logMsg.replace("$HomeCommunityId$",
					"");
		}

		if (logSyslogAuditMsgsLocally)
		{
			log.info(logMsg);
		}

		// Log the syslog message and close connection
		Syslog.getInstance("sslTcp").info(logMsg);
		Syslog.getInstance("sslTcp").flush();
	}

	public AdhocQueryResponse registryStoredQuery(String patientID,
			String queryStartDate,
			String queryEndDate) throws Exception
	{
		AdhocQueryRequest request = new AdhocQueryRequest();
		AdhocQueryType adhocQuery = new AdhocQueryType();
		
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
	
			OMElement requestElement = request.getOMElement(AdhocQueryRequest.MY_QNAME,
					soapFactory);
			String queryText = requestElement.toString();
			logIti18AuditMsg(queryText,
					patientID);

			if (logXdsBRequestMessages)
			{
				Files.write(Paths.get(logOutputPath + UUID.randomUUID().toString() + "_RegistryStoredQueryRequest.xml"),
						requestElement.toString().getBytes());
			}

			return registryStub.documentRegistry_RegistryStoredQuery(request);
		}
		catch (Exception e)
		{
			throw e;
		}
	}
	
	public RetrieveDocumentSetResponse retrieveDocumentSet(String repositoryUniqueIdVal,
			HashMap<String, String> documents,
			String patientId) throws Exception
	{
		try
		{
			RetrieveDocumentSetRequestType documentSetRequestType = new RetrieveDocumentSetRequestType();
			RetrieveDocumentSetRequest documentSetRequest = new RetrieveDocumentSetRequest();
			
			for (String documentId : documents.keySet())
			{
				com.InfoExchangeHub.Services.Client.DocumentRepository_ServiceStub.LongName homeCommunityId = null;
				DocumentRequest_type0 documentRequest = new DocumentRequest_type0();
				com.InfoExchangeHub.Services.Client.DocumentRepository_ServiceStub.LongName repositoryUniqueId = new com.InfoExchangeHub.Services.Client.DocumentRepository_ServiceStub.LongName();
				repositoryUniqueId.setLongName(repositoryUniqueIdVal);
				documentRequest.setRepositoryUniqueId(repositoryUniqueId);
				
				if (documents.get(documentId) != null)
				{
					homeCommunityId = new com.InfoExchangeHub.Services.Client.DocumentRepository_ServiceStub.LongName();
					homeCommunityId.setLongName(documents.get(documentId).toString());
					documentRequest.setHomeCommunityId(homeCommunityId);
				}
				
				com.InfoExchangeHub.Services.Client.DocumentRepository_ServiceStub.LongName documentUniqueId = new com.InfoExchangeHub.Services.Client.DocumentRepository_ServiceStub.LongName();
				documentUniqueId.setLongName(documentId);
				documentRequest.setDocumentUniqueId(documentUniqueId);
				documentSetRequestType.addDocumentRequest(documentRequest);
				
				logIti43AuditMsg(documentId,
						repositoryUniqueId.getLongName(),
						(homeCommunityId == null)? null : homeCommunityId.getLongName(),
						patientId);
			}
			
			documentSetRequest.setRetrieveDocumentSetRequest(documentSetRequestType);

			if (logXdsBRequestMessages)
			{
				OMElement requestElement = documentSetRequest.getOMElement(RetrieveDocumentSetRequest.MY_QNAME,
						soapFactory);
				Files.write(Paths.get(logOutputPath + UUID.randomUUID().toString() + "_RetrieveDocumentSetRequest.xml"),
						requestElement.toString().getBytes());
			}

			return repositoryStub.documentRepository_RetrieveDocumentSet(documentSetRequest);
		}
		catch (Exception e)
		{
			throw e;
		}
	}
}