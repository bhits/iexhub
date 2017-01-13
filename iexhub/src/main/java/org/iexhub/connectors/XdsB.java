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
package org.iexhub.connectors;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.iexhub.exceptions.UnexpectedServerException;
import org.iexhub.services.client.DocumentRegistry_ServiceStub;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.AdhocQueryRequest;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.AdhocQueryResponse;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.AdhocQueryType;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.LongName;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.ResponseOptionType;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.ReturnType_type0;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.SlotType1;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.ValueListType;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.ValueListTypeSequence;
import org.iexhub.services.client.DocumentRepository_ServiceStub;
import org.iexhub.services.client.DocumentRepository_ServiceStub.DocumentRequest_type0;
import org.iexhub.services.client.DocumentRepository_ServiceStub.RetrieveDocumentSetRequest;
import org.iexhub.services.client.DocumentRepository_ServiceStub.RetrieveDocumentSetRequestType;
import org.iexhub.services.client.DocumentRepository_ServiceStub.RetrieveDocumentSetResponse;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.productivity.java.syslog4j.Syslog;
import org.productivity.java.syslog4j.impl.net.tcp.ssl.SSLTCPNetSyslogConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;


/**
 * XDS.b   Registry
 * ITI-18 Ad-hoc Query
 * ITI-43 Retrieve Document
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

	private static boolean debugSsl = false;

	private static String registryEndpointUri = null;
	private static String repositoryEndpointUri = null;
	private static boolean testMode = false;

	private static SSLTCPNetSyslogConfig sysLogConfig = null;

	/**
	 * @param registryEndpointURI
	 */
	public static void setRegistryEndpointURI(String registryEndpointURI)
	{
		if (registryStub != null)
		{
			registryStub._getServiceClient().getOptions().setTo(new org.apache.axis2.addressing.EndpointReference(registryEndpointURI));
			XdsB.registryEndpointUri = registryEndpointURI;
		}
	}
	
	/**
	 * @param repositoryEndpointURI
	 */
	public static void setRepositoryEndpointURI(String repositoryEndpointURI)
	{
		if (repositoryStub != null)
		{
			repositoryStub._getServiceClient().getOptions().setTo(new org.apache.axis2.addressing.EndpointReference(repositoryEndpointURI));
			XdsB.repositoryEndpointUri = repositoryEndpointURI;
		}
	}

	/**
	 * @param registryEndpointURI
	 * @param repositoryEndpointURI
	 * @throws AxisFault
	 * @throws Exception
	 */
	public XdsB(String registryEndpointURI,
			String repositoryEndpointURI) throws AxisFault, Exception
	{
		this(registryEndpointURI,
				repositoryEndpointURI,
				false);
	}
	
	/**
	 * @param registryEndpointURI
	 * @param repositoryEndpointURI
	 * @param enableTLS
	 * @throws AxisFault
	 * @throws Exception
	 */
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
			XdsB.debugSsl = (props.getProperty("DebugSSL") == null) ? XdsB.debugSsl
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

			XdsB.registryEndpointUri = registryEndpointURI;
			XdsB.repositoryEndpointUri = repositoryEndpointURI;

			// If Syslog server host is specified, then configure...
			iti18AuditMsgTemplate = props.getProperty("Iti18AuditMsgTemplate");
			iti43AuditMsgTemplate = props.getProperty("Iti43AuditMsgTemplate");
			String syslogServerHost = props.getProperty("SyslogServerHost");
			int syslogServerPort = (props.getProperty("SyslogServerPort") != null) ? Integer.parseInt(props.getProperty("SyslogServerPort"))
					: -1;
			if ((syslogServerHost != null) &&
				(syslogServerHost.length() > 0) &&
				(syslogServerPort > -1))
			{
				if ((iti18AuditMsgTemplate == null) ||
					(iti43AuditMsgTemplate == null))
				{
					log.error("ITI-18 audit message template or ITI-43 audit message template not specified in properties file, "
							+ propertiesFile);
					throw new UnexpectedServerException("ITI-18 audit message template or ITI-43 audit message template not specified in properties file, "
							+ propertiesFile);
				}

				System.setProperty("https.cipherSuites",
						cipherSuites);
				System.setProperty("https.protocols",
						httpsProtocols);
				
				if (debugSsl)
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
					
					if (debugSsl)
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
					
					if (debugSsl)
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
	 * @param queryText
	 * @param patientId
	 * @throws IOException
	 */
	private void logIti18AuditMsg(String queryText,
			String patientId) throws IOException
	{
		String logMsg = FileUtils.readFileToString(new File(iti18AuditMsgTemplate));
		
		// Substitutions...
		if ((patientId != null) &&
			(patientId.length() > 0))
		{
			patientId = patientId.replace("'",
					"");
			patientId = patientId.replace("&",
					"&amp;");
		}
		else
		{
			patientId = new String("");
		}
		
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
				XdsB.registryEndpointUri);
		
		logMsg = logMsg.replace("$DestinationUserId$",
				"IExHub");
		
		// Query text must be Base64 encoded...
		logMsg = logMsg.replace("$RegistryQueryMtom$",
				Base64.getEncoder().encodeToString(queryText.getBytes()));
		
		logMsg = logMsg.replace("$PatientId$",
				patientId);

		if (logSyslogAuditMsgsLocally)
		{
			log.info(logMsg);
		}

		if ((sysLogConfig == null) ||
            (iti18AuditMsgTemplate == null))
		{
			return;
		}

		// Log the syslog message and close connection
		Syslog.getInstance("sslTcp").info(logMsg);
		Syslog.getInstance("sslTcp").flush();
	}

	/**
	 * @param documentId
	 * @param repositoryUniqueId
	 * @param homeCommunityId
	 * @param patientId
	 * @throws IOException
	 */
	private void logIti43AuditMsg(String documentId,
			String repositoryUniqueId,
			String homeCommunityId,
			String patientId) throws IOException
	{
		String logMsg = FileUtils.readFileToString(new File(iti43AuditMsgTemplate));
		
		// Substitutions...
		if ((patientId != null) &&
			(patientId.length() > 0))
		{
			patientId = patientId.replace("'",
					"");
			patientId = patientId.replace("&",
					"&amp;");
		}
		else
		{
			patientId = new String("");
		}
		
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
				XdsB.registryEndpointUri);
		
		logMsg = logMsg.replace("$DestinationUserId$",
				"IExHub");
		
		// Repository ID text must be Base64 encoded...
		logMsg = logMsg.replace("$RepositoryIdMtom$",
				Base64.getEncoder().encodeToString(repositoryUniqueId.getBytes()));
		
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

		if ((sysLogConfig == null) ||
            (iti43AuditMsgTemplate == null))
		{
			return;
		}

		// Log the syslog message and close connection
		Syslog.getInstance("sslTcp").info(logMsg);
		Syslog.getInstance("sslTcp").flush();
	}

	/**
	 * @param patientId
	 * @param queryStartDate
	 * @param queryEndDate
	 * @return
	 * @throws Exception
	 */
	public AdhocQueryResponse registryStoredQuery(String patientId,
			String queryStartDate,
			String queryEndDate) throws Exception
	{
		return registryStoredQuery(patientId,
				queryStartDate,
				queryEndDate,
				null,
				null);
	}

	/**
	 * @param patientId
	 * @param queryStartDate
	 * @param queryEndDate
	 * @return
	 * @throws Exception
	 */
	public AdhocQueryResponse registryStoredQuery(String patientId,
			String queryStartDate,
			String queryEndDate,
			String typeCode) throws Exception
	{
		return registryStoredQuery(patientId,
				queryStartDate,
				queryEndDate,
				typeCode,
				null);
	}
	
	/**
	 * @param patientId
	 * @param queryStartDate
	 * @param queryEndDate
	 * @return
	 * @throws Exception
	 */
	public AdhocQueryResponse registryStoredQuery(String patientId,
			String queryStartDate,
			String queryEndDate,
			String typeCode,
			String documentUniqueId) throws Exception
	{
		AdhocQueryRequest request = new AdhocQueryRequest();
		AdhocQueryType adhocQuery = new AdhocQueryType();
		SlotType1 slot = null;
		LongName name = null;
		ValueListType valueList = null;
		ValueListTypeSequence[] valueListSequenceArray = null;
		ValueListTypeSequence valueListSequence = null;
		LongName valueName = null;
		
		if ((patientId != null) &&
			(patientId.length() > 0))
		{
			slot = new SlotType1();
			name = new LongName();
			name.setLongName("$XDSDocumentEntryPatientId");
			slot.setName(name);
			valueList = new ValueListType();
			valueListSequenceArray = new ValueListTypeSequence[1];
			valueListSequence = new ValueListTypeSequence(); 
			valueName = new LongName();
			valueName.setLongName(patientId);
			valueListSequence.setValue(valueName);
			valueListSequenceArray[0] = valueListSequence;
			valueList.setValueListTypeSequence(valueListSequenceArray);
			slot.setValueList(valueList);
			adhocQuery.addSlot(slot);
		}
		
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
		
		if (typeCode != null)
		{
			slot = new SlotType1();
			name = new LongName();
			name.setLongName("$XDSDocumentEntryTypeCode");
			slot.setName(name);
			valueList = new ValueListType();
			valueListSequenceArray = new ValueListTypeSequence[1];
			valueListSequence = new ValueListTypeSequence(); 
			valueName = new LongName();
			valueName.setLongName(typeCode);
			valueListSequence.setValue(valueName);
			valueListSequenceArray[0] = valueListSequence;
			valueList.setValueListTypeSequence(valueListSequenceArray);
			slot.setValueList(valueList);
			adhocQuery.addSlot(slot);			
		}

		if (documentUniqueId != null)
		{
			slot = new SlotType1();
			name = new LongName();
			name.setLongName("$XDSDocumentEntryUniqueId");
			slot.setName(name);
			valueList = new ValueListType();
			valueListSequenceArray = new ValueListTypeSequence[1];
			valueListSequence = new ValueListTypeSequence(); 
			valueName = new LongName();
			valueName.setLongName(documentUniqueId);
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
					patientId);

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
	
	/**
	 * @param repositoryUniqueIdVal
	 * @param documents
	 * @param patientId
	 * @return
	 * @throws Exception
	 */
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
				org.iexhub.services.client.DocumentRepository_ServiceStub.LongName homeCommunityId = null;
				DocumentRequest_type0 documentRequest = new DocumentRequest_type0();
				org.iexhub.services.client.DocumentRepository_ServiceStub.LongName repositoryUniqueId = new org.iexhub.services.client.DocumentRepository_ServiceStub.LongName();
				repositoryUniqueId.setLongName(repositoryUniqueIdVal);
				documentRequest.setRepositoryUniqueId(repositoryUniqueId);
				
				if (documents.get(documentId) != null)
				{
					homeCommunityId = new org.iexhub.services.client.DocumentRepository_ServiceStub.LongName();
					homeCommunityId.setLongName(documents.get(documentId).toString());
					documentRequest.setHomeCommunityId(homeCommunityId);
				}
				
				org.iexhub.services.client.DocumentRepository_ServiceStub.LongName documentUniqueId = new org.iexhub.services.client.DocumentRepository_ServiceStub.LongName();
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