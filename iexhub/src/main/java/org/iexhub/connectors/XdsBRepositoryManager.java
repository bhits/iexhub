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

import XdsBDocumentRepository.ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;
import XdsBDocumentRepository.oasis.names.tc.ebxml_regrep.xsd.lcm._3.SubmitObjectsRequest;
import XdsBDocumentRepository.oasis.names.tc.ebxml_regrep.xsd.rim._3.AssociationType1;
import XdsBDocumentRepository.oasis.names.tc.ebxml_regrep.xsd.rim._3.ClassificationType;
import XdsBDocumentRepository.oasis.names.tc.ebxml_regrep.xsd.rim._3.ExternalIdentifierType;
import XdsBDocumentRepository.oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import XdsBDocumentRepository.oasis.names.tc.ebxml_regrep.xsd.rim._3.InternationalStringType;
import XdsBDocumentRepository.oasis.names.tc.ebxml_regrep.xsd.rim._3.LocalizedStringType;
import XdsBDocumentRepository.oasis.names.tc.ebxml_regrep.xsd.rim._3.ObjectFactory;
import XdsBDocumentRepository.oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectListType;
import XdsBDocumentRepository.oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import XdsBDocumentRepository.oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;
import XdsBDocumentRepository.oasis.names.tc.ebxml_regrep.xsd.rim._3.ValueListType;
import XdsBDocumentRepository.oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType;
import XdsBDocumentRepository.org.iexhub.services.client.DocumentRepository_ServiceStub;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.hl7.fhir.dstu3.model.Consent;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.iexhub.config.IExHubConfig;
import org.iexhub.exceptions.DocumentTypeUnsupportedException;
import org.iexhub.exceptions.UnexpectedServerException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.productivity.java.syslog4j.Syslog;
import org.productivity.java.syslog4j.impl.net.tcp.ssl.SSLTCPNetSyslogConfig;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;


/**
 * XdsBRepositoryManager Connector
 * ITI-41  Provide and Register Document
 * @author A. Sute
 *
 */

public class XdsBRepositoryManager
{
	/** Logger */
    public static final Logger log = Logger.getLogger(XdsBRepositoryManager.class);
	public static final int SYSLOG_SERVER_PORT_MIN = 0;
	public static final int SYSLOG_SERVER_PORT_MAX = 65535;

	private static boolean testMode = false;

	private static boolean logXdsBRequestMessages = false;
	private static String logOutputPath = "/java/iexhub/logs";
	private static boolean logSyslogAuditMsgsLocally = false;

    private static String keyStoreFile = IExHubConfig.getConfigLocationPath("1264.jks");
	private static String keyStorePwd = "IEXhub";
	private static String cipherSuites = "TLS_RSA_WITH_AES_128_CBC_SHA";
	private static String httpsProtocols = "TLSv1";

	private static String iti41AuditMsgTemplate = null;

	private static SSLTCPNetSyslogConfig sysLogConfig = null;

	private static final SOAPFactory soapFactory = OMAbstractFactory.getSOAP12Factory();
	private static final ObjectFactory objectFactory = new ObjectFactory();

	private static DocumentRepository_ServiceStub repositoryStub = null;

	private static boolean debugSsl = false;

	private static String repositoryEndpointUri = null;
	private static String iExHubDomainOid = "1.3.6.1.4.1.21367.13.60.232";

	private static String documentAuthorClassificationScheme = "urn:uuid:93606bcf-9494-43ec-9b4e-a7748d1a838d";

	private static String documentClassCodesClassificationScheme = "urn:uuid:41a5887f-8865-4c09-adf7-e362475b143a";
	private static String documentClassCodesNodeRepresentation = "*";
	private static String documentClassCodesNodeRepresentationContract = "*";
	private static String documentClassCodesCodingScheme = "1.3.6.1.4.1.21367.100.1";
	private static String documentClassCodesName = "2.16.840.1.113883.6.1";

	private static String documentConfidentialityCodesClassificationScheme = "urn:uuid:f4f85eac-e6cb-4883-b524-f2705394840f";
	private static String documentContentTypeClassificationScheme = "urn:uuid:f0306f51-975f-434e-a61c-c59651d33983";

	private static String documentFormatCodesClassificationScheme = "urn:uuid:a09d5840-386c-46f2-b5ad-9c3699a4309d";
	private static String documentFormatCodesNodeRepresentation = "urn:ihe:iti:xds-sd:text:2008";
	private static String documentFormatCodesCodingScheme = "1.3.6.1.4.1.19376.1.2.3";
	private static String documentFormatCodesName = "Health encounter site";

	private static String documentHealthcareFacilityTypeCodesClassificationScheme = "urn:uuid:f33fb8ac-18af-42cc-ae0e-ed0b0bdb91e1";
	private static String documentHealthcareFacilityTypeCodesNodeRepresentation = "unobtainable";
	private static String documentHealthcareFacilityTypeCodesCodingScheme = "Connect-a-thon healthcareFacilityTypeCodes";
	private static String documentHealthcareFacilityTypeCodesName = "unobtainable";
	private static String documentPracticeSettingCodesDisplayName = "unobtainable";

	private static String documentPracticeSettingCodesClassificationScheme = "urn:uuid:cccf5598-8b07-4b77-a05e-ae952c785ead";
	private static String documentPracticeSettingCodesNodeRepresentation = "unobtainable";
	private static String documentPracticeSettingCodesCodingScheme = "Connect-a-thon practiceSettingCodes";

	private static String extrinsicObjectExternalIdentifierPatientIdIdentificationScheme = "urn:uuid:58a6f841-87b3-4a3e-92fd-a8ffeff98427";
	private static String extrinsicObjectExternalIdentifierPatientIdName = "XDSDocumentEntry.patientId";
	private static String extrinsicObjectExternalIdentifierUniqueIdIdentificationScheme = "urn:uuid:2e82c1f6-a085-4c72-9da3-8640a32e42ab";
	private static String extrinsicObjectExternalIdentifierUniqueIdName = "XDSDocumentEntry.uniqueId";
	private static String extrinsicObjectExternalIdentifierEntryUuidIdentificationScheme = "urn:uuid:7edca82f-054d-47f2-a032-9b2a5b5186c1";
	private static String extrinsicObjectExternalIdentifierEntryUuidName = "XDSDocumentEntry.entryUuid";

	private static String submissionSetOid = "1.2.3.4.5";

	private static String registryPackageAuthorClassificationScheme = "urn:uuid:a7058bb9-b4e4-4307-ba5b-e3f0ab85e12d";
	private static String registryPackageContentTypeCodesClassificationScheme = "urn:uuid:aa543740-bdda-424e-8c96-df4873be8500";
	private static String registryPackageSubmissionSetUniqueIdIdentificationScheme = "urn:uuid:96fdda7c-d067-4183-912e-bf5ee74998a8";
	private static String registryPackageSubmissionSetSourceIdIdentificationScheme = "urn:uuid:554ac39e-e3fe-47fe-b233-965d2a147832";
	private static String registryPackageSubmissionSetPatientIdIdentificationScheme = "urn:uuid:6b5aea1a-874d-4603-a4bc-96a0a7b38446";
	private static String registryObjectListSubmissionSetClassificationNode = "urn:uuid:a54d6aa5-d40d-43f9-88c5-b4633d873bdd";

	private static String externalIdentifierSubmissionSetUniqueIdName = "XDSSubmissionSet.uniqueId";
	private static String externalIdentifierSubmissionSetSourceIdName = "XDSSubmissionSet.sourceId";
	private static String externalIdentifierSubmissionSetPatientIdName = "XDSSubmissionSet.patientId";

	public XdsBRepositoryManager() {
	}

	/**
	 * @param repositoryEndpointURI
	 */
	public static void setRepositoryEndpointURI(String repositoryEndpointURI)
	{
		if (repositoryStub != null)
		{
			repositoryStub._getServiceClient().getOptions().setTo(new org.apache.axis2.addressing.EndpointReference(repositoryEndpointURI));
			XdsBRepositoryManager.repositoryEndpointUri = repositoryEndpointURI;
		}
	}

	/**
	 * @param registryEndpointURI
	 * @param repositoryEndpointURI
	 * @throws AxisFault
	 * @throws Exception
	 */
	public XdsBRepositoryManager(String registryEndpointURI,
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
	public XdsBRepositoryManager(String registryEndpointURI,
			String repositoryEndpointURI,
			boolean enableTLS) throws AxisFault, Exception
	{
		XdsBRepositoryManager.logSyslogAuditMsgsLocally = IExHubConfig.getProperty("LogSyslogAuditMsgsLocally", XdsBRepositoryManager.logSyslogAuditMsgsLocally);
		XdsBRepositoryManager.logOutputPath = IExHubConfig.getProperty("LogOutputPath", XdsBRepositoryManager.logOutputPath);
		XdsBRepositoryManager.logXdsBRequestMessages = IExHubConfig.getProperty("LogXdsBRequestMessages", XdsBRepositoryManager.logXdsBRequestMessages);
		XdsBRepositoryManager.debugSsl = IExHubConfig.getProperty("DebugSSL", XdsBRepositoryManager.debugSsl);
		XdsBRepositoryManager.testMode = IExHubConfig.getProperty("TestMode", XdsBRepositoryManager.testMode);
		XdsBRepositoryManager.iExHubDomainOid = IExHubConfig.getProperty("IExHubDomainOID", XdsBRepositoryManager.iExHubDomainOid);
		XdsBRepositoryManager.keyStoreFile = IExHubConfig.getProperty("XdsBKeyStoreFile", XdsBRepositoryManager.keyStoreFile);
		XdsBRepositoryManager.keyStorePwd = IExHubConfig.getProperty("XdsBKeyStorePwd", XdsBRepositoryManager.keyStorePwd);
		XdsBRepositoryManager.cipherSuites = IExHubConfig.getProperty("XdsBCipherSuites", XdsBRepositoryManager.cipherSuites);
		XdsBRepositoryManager.httpsProtocols = IExHubConfig.getProperty("XdsBHttpsProtocols", XdsBRepositoryManager.httpsProtocols);

		XdsBRepositoryManager.documentAuthorClassificationScheme = IExHubConfig.getProperty("XdsBDocumentAuthorClassificationScheme", XdsBRepositoryManager.documentAuthorClassificationScheme);

		// ClassCode
		XdsBRepositoryManager.documentClassCodesClassificationScheme = IExHubConfig.getProperty("XdsBDocumentClassCodesClassificationScheme", XdsBRepositoryManager.documentClassCodesClassificationScheme);
		XdsBRepositoryManager.documentClassCodesNodeRepresentation = IExHubConfig.getProperty("XdsBDocumentClassCodesNodeRepresentation", XdsBRepositoryManager.documentClassCodesNodeRepresentation);
		XdsBRepositoryManager.documentClassCodesNodeRepresentationContract = IExHubConfig.getProperty("XdsBDocumentClassCodesNodeRepresentationContract", XdsBRepositoryManager.documentClassCodesNodeRepresentationContract);
		XdsBRepositoryManager.documentClassCodesCodingScheme = IExHubConfig.getProperty("XdsBDocumentClassCodesCodingScheme", XdsBRepositoryManager.documentClassCodesCodingScheme);
		XdsBRepositoryManager.documentClassCodesName = IExHubConfig.getProperty("XdsBDocumentClassCodesName", XdsBRepositoryManager.documentClassCodesName);


		XdsBRepositoryManager.documentConfidentialityCodesClassificationScheme = IExHubConfig.getProperty("XdsBDocumentConfidentialityCodesClassificationScheme", XdsBRepositoryManager.documentConfidentialityCodesClassificationScheme);
		XdsBRepositoryManager.documentContentTypeClassificationScheme = IExHubConfig.getProperty("XdsBDocumentContentTypeClassificationScheme", XdsBRepositoryManager.documentContentTypeClassificationScheme);

		XdsBRepositoryManager.documentFormatCodesClassificationScheme = IExHubConfig.getProperty("XdsBDocumentFormatCodesClassificationScheme", XdsBRepositoryManager.documentFormatCodesClassificationScheme);
		XdsBRepositoryManager.documentFormatCodesNodeRepresentation = IExHubConfig.getProperty("XdsBDocumentFormatCodesNodeRepresentation", XdsBRepositoryManager.documentFormatCodesNodeRepresentation);
		XdsBRepositoryManager.documentFormatCodesCodingScheme = IExHubConfig.getProperty("XdsBDocumentFormatCodesCodingScheme", XdsBRepositoryManager.documentFormatCodesCodingScheme);
		XdsBRepositoryManager.documentFormatCodesName = IExHubConfig.getProperty("XdsBDocumentFormatCodesName", XdsBRepositoryManager.documentFormatCodesName);

		XdsBRepositoryManager.documentHealthcareFacilityTypeCodesClassificationScheme = IExHubConfig.getProperty("XdsBDocumentHealthcareFacilityTypeCodesClassificationScheme", XdsBRepositoryManager.documentHealthcareFacilityTypeCodesClassificationScheme);
		XdsBRepositoryManager.documentHealthcareFacilityTypeCodesNodeRepresentation = IExHubConfig.getProperty("XdsBDocumentHealthcareFacilityTypeCodesNodeRepresentation", XdsBRepositoryManager.documentHealthcareFacilityTypeCodesNodeRepresentation);
		XdsBRepositoryManager.documentHealthcareFacilityTypeCodesCodingScheme = IExHubConfig.getProperty("XdsBDocumentHealthcareFacilityTypeCodesCodingScheme", XdsBRepositoryManager.documentHealthcareFacilityTypeCodesCodingScheme);
		XdsBRepositoryManager.documentHealthcareFacilityTypeCodesName = IExHubConfig.getProperty("XdsBDocumentHealthcareFacilityTypeCodesName", XdsBRepositoryManager.documentHealthcareFacilityTypeCodesName);

		XdsBRepositoryManager.documentPracticeSettingCodesClassificationScheme = IExHubConfig.getProperty("XdsBDocumentPracticeSettingCodesClassificationScheme", XdsBRepositoryManager.documentPracticeSettingCodesClassificationScheme);
		XdsBRepositoryManager.documentPracticeSettingCodesNodeRepresentation = IExHubConfig.getProperty("XdsBDocumentPracticeSettingCodesNodeRepresentation", XdsBRepositoryManager.documentPracticeSettingCodesNodeRepresentation);
		XdsBRepositoryManager.documentPracticeSettingCodesCodingScheme = IExHubConfig.getProperty("XdsBDocumentPracticeSettingCodesCodingScheme", XdsBRepositoryManager.documentPracticeSettingCodesCodingScheme);
		XdsBRepositoryManager.documentHealthcareFacilityTypeCodesCodingScheme = IExHubConfig.getProperty("XdsBDocumentHealthcareFacilityTypeCodesCodingScheme", XdsBRepositoryManager.documentHealthcareFacilityTypeCodesCodingScheme);
		XdsBRepositoryManager.documentHealthcareFacilityTypeCodesName = IExHubConfig.getProperty("XdsBDocumentHealthcareFacilityTypeCodesName", XdsBRepositoryManager.documentHealthcareFacilityTypeCodesName);
		XdsBRepositoryManager.documentPracticeSettingCodesDisplayName = IExHubConfig.getProperty("XdsBDocumentPracticeSettingCodesDisplayName", XdsBRepositoryManager.documentPracticeSettingCodesDisplayName);

		XdsBRepositoryManager.extrinsicObjectExternalIdentifierPatientIdIdentificationScheme = IExHubConfig.getProperty("XdsBExtrinsicObjectExternalIdentifierPatientIdIdentificationScheme", XdsBRepositoryManager.extrinsicObjectExternalIdentifierPatientIdIdentificationScheme);
		XdsBRepositoryManager.extrinsicObjectExternalIdentifierPatientIdName = IExHubConfig.getProperty("XdsBExtrinsicObjectExternalIdentifierPatientIdName", XdsBRepositoryManager.extrinsicObjectExternalIdentifierPatientIdName);
		XdsBRepositoryManager.extrinsicObjectExternalIdentifierUniqueIdIdentificationScheme = IExHubConfig.getProperty("XdsBExtrinsicObjectExternalIdentifierUniqueIdIdentificationScheme", XdsBRepositoryManager.extrinsicObjectExternalIdentifierUniqueIdIdentificationScheme);
		XdsBRepositoryManager.extrinsicObjectExternalIdentifierUniqueIdName = IExHubConfig.getProperty("XdsBExtrinsicObjectExternalIdentifierUniqueIdName", XdsBRepositoryManager.extrinsicObjectExternalIdentifierUniqueIdName);
		XdsBRepositoryManager.extrinsicObjectExternalIdentifierEntryUuidIdentificationScheme = IExHubConfig.getProperty("XdsBExtrinsicObjectExternalIdentifierEntryUuidIdentificationScheme", XdsBRepositoryManager.extrinsicObjectExternalIdentifierEntryUuidIdentificationScheme);
		XdsBRepositoryManager.extrinsicObjectExternalIdentifierEntryUuidName = IExHubConfig.getProperty("XdsBExtrinsicObjectExternalIdentifierEntryUuidName", XdsBRepositoryManager.extrinsicObjectExternalIdentifierEntryUuidName);

		XdsBRepositoryManager.registryPackageAuthorClassificationScheme = IExHubConfig.getProperty("XdsBRegistryPackageAuthorClassificationScheme", XdsBRepositoryManager.registryPackageAuthorClassificationScheme);
		XdsBRepositoryManager.registryPackageContentTypeCodesClassificationScheme = IExHubConfig.getProperty("XdsBRegistryPackageContentTypeCodesClassificationScheme", XdsBRepositoryManager.registryPackageContentTypeCodesClassificationScheme);
		XdsBRepositoryManager.registryPackageSubmissionSetUniqueIdIdentificationScheme = IExHubConfig.getProperty("XdsBRegistryPackageSubmissionSetUniqueIdIdentificationScheme", XdsBRepositoryManager.registryPackageSubmissionSetUniqueIdIdentificationScheme);
		XdsBRepositoryManager.registryPackageSubmissionSetSourceIdIdentificationScheme = IExHubConfig.getProperty("XdsBRegistryPackageSubmissionSetSourceIdIdentificationScheme", XdsBRepositoryManager.registryPackageSubmissionSetSourceIdIdentificationScheme);
		XdsBRepositoryManager.registryPackageSubmissionSetPatientIdIdentificationScheme = IExHubConfig.getProperty("XdsBRegistryPackageSubmissionSetPatientIdIdentificationScheme", XdsBRepositoryManager.registryPackageSubmissionSetPatientIdIdentificationScheme);
		XdsBRepositoryManager.registryObjectListSubmissionSetClassificationNode = IExHubConfig.getProperty("XdsBRegistryObjectListSubmissionSetClassificationNode", XdsBRepositoryManager.registryObjectListSubmissionSetClassificationNode);

		XdsBRepositoryManager.externalIdentifierSubmissionSetUniqueIdName = IExHubConfig.getProperty("XdsBExternalIdentifierSubmissionSetUniqueIdName", XdsBRepositoryManager.externalIdentifierSubmissionSetUniqueIdName);
		XdsBRepositoryManager.externalIdentifierSubmissionSetSourceIdName = IExHubConfig.getProperty("XdsBExternalIdentifierSubmissionSetSourceIdName", XdsBRepositoryManager.externalIdentifierSubmissionSetSourceIdName);
		XdsBRepositoryManager.externalIdentifierSubmissionSetPatientIdName = IExHubConfig.getProperty("XdsBExternalIdentifierSubmissionSetPatientIdName", XdsBRepositoryManager.externalIdentifierSubmissionSetPatientIdName);

		XdsBRepositoryManager.submissionSetOid = IExHubConfig.getProperty("XdsBSubmissionSetOid", XdsBRepositoryManager.submissionSetOid);

		// If endpoint URI's are null, then set to the values in the properties file...
		if (registryEndpointURI == null)
		{
			registryEndpointURI = IExHubConfig.getProperty("XdsBRegistryEndpointURI");
		}

		if (repositoryEndpointURI == null)
		{
			repositoryEndpointURI = IExHubConfig.getProperty("XdsBRepositoryEndpointURI");
		}

		XdsBRepositoryManager.repositoryEndpointUri = repositoryEndpointURI;

		// If Syslog server host is specified, then configure...
		iti41AuditMsgTemplate = IExHubConfig.getProperty("Iti41AuditMsgTemplate");
		String syslogServerHost = IExHubConfig.getProperty("SyslogServerHost");
		int syslogServerPort = IExHubConfig.getProperty("SyslogServerPort", -1);
		if ((syslogServerHost != null) &&
			(syslogServerHost.length() > 0) &&
			(syslogServerPort > SYSLOG_SERVER_PORT_MIN && syslogServerPort <= SYSLOG_SERVER_PORT_MAX))
		{
			if (iti41AuditMsgTemplate == null)
			{
				log.error("ITI-41 audit message template not specified in properties file, "
						+ IExHubConfig.CONFIG_FILE);
				throw new UnexpectedServerException("ITI-41 audit message template not specified in properties file, "
						+ IExHubConfig.CONFIG_FILE);
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

		try
		{
			// Note that XDS.b registry access is not currently implemented in this class.  It is implemented in the XdsB class.
			if (repositoryEndpointURI != null)
			{
				// Instantiate DocumentRegistry client stub and enable WS-Addressing and MTOM...
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
	 * @param submissionSetId
	 * @param patientId
	 * @throws IOException
	 */
	private void logIti41AuditMsg(String submissionSetId,
			String patientId) throws IOException
	{		
		String logMsg = FileUtils.readFileToString(new File(iti41AuditMsgTemplate));
		
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
				XdsBRepositoryManager.repositoryEndpointUri);
		
		logMsg = logMsg.replace("$DestinationUserId$",
				"IExHub");
		
		logMsg = logMsg.replace("$PatientId$",
				patientId);

		logMsg = logMsg.replace("$SubmissionSetId$",
				submissionSetId);

		if (logSyslogAuditMsgsLocally)
		{
			log.info(logMsg);
		}

		if ((sysLogConfig == null) ||
            (iti41AuditMsgTemplate == null))
		{
			return;
		}

		// Log the syslog message and close connection
		Syslog.getInstance("sslTcp").info(logMsg);
		Syslog.getInstance("sslTcp").flush();
	}

	/**
	 * @param cdaDocument
	 * @param mimeType
	 * @return
	 * @throws Exception
	 */
	public RegistryResponseType provideAndRegisterDocumentSet(byte[] cdaDocument,
			String mimeType) throws Exception
	{
		// Support only for a single document per submission set...
		if (mimeType.compareToIgnoreCase("text/xml") != 0)
		{
			throw new DocumentTypeUnsupportedException("Only XML documents currently supported for ProvideAndRegisterDocumentSet");
		}
		
		try
		{
			ProvideAndRegisterDocumentSetRequestType documentSetRequest = new ProvideAndRegisterDocumentSetRequestType();
			
			// Create SubmitObjectsRequest...
			SubmitObjectsRequest submitObjectsRequest = new SubmitObjectsRequest();
			
			//  Create RegistryObjectList...
			RegistryObjectListType registryObjectList = new RegistryObjectListType();
			
			// Create ExtrinsicObject...
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			InputStream is = new ByteArrayInputStream(cdaDocument);
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			Document doc = dBuilder.parse(new InputSource(reader));

			String documentId = UUID.randomUUID().toString();
			ExtrinsicObjectType extrinsicObject = new ExtrinsicObjectType();
			extrinsicObject.setId(documentId);
			extrinsicObject.setMimeType(mimeType);
			extrinsicObject.setObjectType("urn:uuid:7edca82f-054d-47f2-a032-9b2a5b5186c1");

			// Create creationTime rim:Slot...
			ValueListType valueList = null;
			SlotType1 slot = new SlotType1();
			slot.setName("creationTime");
			XPath xPath = getCustomXPath();
			NodeList nodes = (NodeList)xPath.evaluate("/hl7:ClinicalDocument/hl7:effectiveTime",
			        doc.getDocumentElement(),
			        XPathConstants.NODESET);
			if (nodes.getLength() > 0)
			{
				valueList = new ValueListType();
			    valueList.getValue().add(((Element)nodes.item(0)).getAttribute("value"));
				slot.setValueList(valueList);
				extrinsicObject.getSlot().add(slot);
			}
			
			// Create languageCode rim:Slot...
			xPath = getCustomXPath();
			nodes = (NodeList)xPath.evaluate("/hl7:ClinicalDocument/hl7:languageCode",
			        doc.getDocumentElement(),
			        XPathConstants.NODESET);
			if (nodes.getLength() > 0)
			{
				slot = new SlotType1();
				slot.setName("languageCode");
				valueList = new ValueListType();
			    valueList.getValue().add(((Element)nodes.item(0)).getAttribute("code"));
				slot.setValueList(valueList);
				extrinsicObject.getSlot().add(slot);
			}
			
			// Create serviceStartTime rim:Slot...
			xPath = getCustomXPath();
			nodes = (NodeList)xPath.evaluate("/hl7:ClinicalDocument/hl7:documentationOf/hl7:serviceEvent/hl7:effectiveTime/hl7:low",
			        doc.getDocumentElement(),
			        XPathConstants.NODESET);
			if (nodes.getLength() > 0)
			{
				slot = new SlotType1();
				slot.setName("serviceStartTime");
				valueList = new ValueListType();
			    valueList.getValue().add(((Element)nodes.item(0)).getAttribute("value"));
				slot.setValueList(valueList);
				extrinsicObject.getSlot().add(slot);
			}

			// Create serviceStopTime rim:Slot...
			xPath = getCustomXPath();
			nodes = (NodeList)xPath.evaluate("/hl7:ClinicalDocument/hl7:documentationOf/hl7:serviceEvent/hl7:effectiveTime/high",
			        doc.getDocumentElement(),
			        XPathConstants.NODESET);
			if (nodes.getLength() > 0)
			{
				slot = new SlotType1();
				slot.setName("serviceStopTime");
				valueList = new ValueListType();
			    valueList.getValue().add(((Element)nodes.item(0)).getAttribute("value"));
				slot.setValueList(valueList);
				extrinsicObject.getSlot().add(slot);
			}

			// Create sourcePatientId rim:Slot...
			xPath = getCustomXPath();
			nodes = (NodeList)xPath.evaluate("/hl7:ClinicalDocument/hl7:recordTarget/hl7:patientRole/hl7:id",
			        doc.getDocumentElement(),
			        XPathConstants.NODESET);
			String patientId = null;
			if (nodes.getLength() > 0)
			{
				slot = new SlotType1();
				slot.setName("sourcePatientId");
				valueList = new ValueListType();
				patientId = ((Element)nodes.item(0)).getAttribute("extension")
			    		+ "^^^&"
			    		+ ((Element)nodes.item(0)).getAttribute("root")
			    		+ "&ISO";
			    valueList.getValue().add(patientId);
				slot.setValueList(valueList);
				extrinsicObject.getSlot().add(slot);
			}

			// Create sourcePatientInfo rim:Slot...
			xPath = getCustomXPath();
			nodes = (NodeList)xPath.evaluate("/hl7:ClinicalDocument/hl7:recordTarget/hl7:patientRole/hl7:patient/hl7:name/hl7:family",
			        doc.getDocumentElement(),
			        XPathConstants.NODESET);
			if (nodes.getLength() > 0)
			{
				slot = new SlotType1();
				slot.setName("sourcePatientInfo");
				valueList = new ValueListType();
			    valueList.getValue().add("PID-3|"
			    		+ patientId);

			    StringBuilder name = new StringBuilder();
			    name.append("PID-5|"
			    		+ ((Element)nodes.item(0)).getTextContent()
			    		+ "^");
			    
				xPath = getCustomXPath();
				nodes = (NodeList)xPath.evaluate("/hl7:ClinicalDocument/hl7:recordTarget/hl7:patientRole/hl7:patient/hl7:name/hl7:given",
				        doc.getDocumentElement(),
				        XPathConstants.NODESET);
				if (nodes.getLength() > 0)
				{
					name.append(((Element)nodes.item(0)).getTextContent()
							+ "^^");
				}
				else
				{
					name.append("^^");
				}
				
				xPath = getCustomXPath();
				nodes = (NodeList)xPath.evaluate("/hl7:ClinicalDocument/hl7:recordTarget/hl7:patientRole/hl7:patient/hl7:name/hl7:prefix",
				        doc.getDocumentElement(),
				        XPathConstants.NODESET);
				if (nodes.getLength() > 0)
				{
					name.append(((Element)nodes.item(0)).getTextContent()
							+ "^");
				}
				else
				{
					name.append("^");
				}

				xPath = getCustomXPath();
				nodes = (NodeList)xPath.evaluate("/hl7:ClinicalDocument/hl7:recordTarget/hl7:patientRole/hl7:patient/hl7:name/hl7:suffix",
				        doc.getDocumentElement(),
				        XPathConstants.NODESET);
				if (nodes.getLength() > 0)
				{
					name.append(((Element)nodes.item(0)).getTextContent());
				}

				valueList.getValue().add(name.toString());

				xPath = getCustomXPath();
				nodes = (NodeList)xPath.evaluate("/hl7:ClinicalDocument/hl7:recordTarget/hl7:patientRole/hl7:patient/hl7:birthTime",
				        doc.getDocumentElement(),
				        XPathConstants.NODESET);
				if (nodes.getLength() > 0)
				{
				    valueList.getValue().add("PID-7|"
				    		+ ((Element)nodes.item(0)).getAttribute("value"));
				}

				xPath = getCustomXPath();
				nodes = (NodeList)xPath.evaluate("/hl7:ClinicalDocument/hl7:recordTarget/hl7:patientRole/hl7:patient/hl7:administrativeGenderCode",
				        doc.getDocumentElement(),
				        XPathConstants.NODESET);
				if (nodes.getLength() > 0)
				{
				    valueList.getValue().add("PID-8|"
				    		+ ((Element)nodes.item(0)).getAttribute("code"));
				}

				xPath = getCustomXPath();
				nodes = (NodeList)xPath.evaluate("/hl7:ClinicalDocument/hl7:recordTarget/hl7:patientRole/hl7:addr",
				        doc.getDocumentElement(),
				        XPathConstants.NODESET);
				if (nodes.getLength() > 0)
				{
					StringBuilder address = new StringBuilder();
					address.append("PID-11|");
					
					xPath = getCustomXPath();
					nodes = (NodeList)xPath.evaluate("/hl7:ClinicalDocument/hl7:recordTarget/hl7:patientRole/hl7:addr/hl7:streetAddressLine",
					        doc.getDocumentElement(),
					        XPathConstants.NODESET);
					if (nodes.getLength() > 0)
					{
						address.append(((Element)nodes.item(0)).getTextContent()
								+ "^^");
					}
					else
					{
						address.append("^^");
					}
					
					xPath = getCustomXPath();
					nodes = (NodeList)xPath.evaluate("/hl7:ClinicalDocument/hl7:recordTarget/hl7:patientRole/hl7:addr/hl7:city",
					        doc.getDocumentElement(),
					        XPathConstants.NODESET);
					if (nodes.getLength() > 0)
					{
						address.append(((Element)nodes.item(0)).getTextContent()
								+ "^");
					}
					else
					{
						address.append("^");
					}

					xPath = getCustomXPath();
					nodes = (NodeList)xPath.evaluate("/hl7:ClinicalDocument/hl7:recordTarget/hl7:patientRole/hl7:addr/hl7:state",
					        doc.getDocumentElement(),
					        XPathConstants.NODESET);
					if (nodes.getLength() > 0)
					{
						address.append(((Element)nodes.item(0)).getTextContent()
								+ "^");
					}
					else
					{
						address.append("^");
					}

					xPath = getCustomXPath();
					nodes = (NodeList)xPath.evaluate("/hl7:ClinicalDocument/hl7:recordTarget/hl7:patientRole/hl7:addr/hl7:postalCode",
					        doc.getDocumentElement(),
					        XPathConstants.NODESET);
					if (nodes.getLength() > 0)
					{
						address.append(((Element)nodes.item(0)).getTextContent()
								+ "^");
					}
					else
					{
						address.append("^");
					}

					xPath = getCustomXPath();
					nodes = (NodeList)xPath.evaluate("/hl7:ClinicalDocument/hl7:recordTarget/hl7:patientRole/hl7:addr/hl7:country",
					        doc.getDocumentElement(),
					        XPathConstants.NODESET);
					if (nodes.getLength() > 0)
					{
						address.append(((Element)nodes.item(0)).getTextContent());
					}
					else
					{
						address.append("^");
					}

				    valueList.getValue().add(address.toString());
				}

				slot.setValueList(valueList);
				extrinsicObject.getSlot().add(slot);
			}

			// Create classifications - start with document author(s)...
			ArrayList<ClassificationType> documentAuthorClassifications = new ArrayList<ClassificationType>();
			xPath = getCustomXPath();
			nodes = (NodeList)xPath.evaluate("/hl7:ClinicalDocument/hl7:author",
			        doc.getDocumentElement(),
			        XPathConstants.NODESET);
			if (nodes.getLength() > 0)
			{
				for (int i = 0; i < nodes.getLength(); ++i)
				{
					ClassificationType documentAuthorClassification = new ClassificationType();
					documentAuthorClassification.setId(UUID.randomUUID().toString());
					documentAuthorClassification.setClassificationScheme(documentAuthorClassificationScheme);
					documentAuthorClassification.setClassifiedObject(documentId);
					documentAuthorClassification.setNodeRepresentation("");
					slot = new SlotType1();
					slot.setName("authorPerson");

					// authorPerson rim:Slot
					StringBuilder authorName = new StringBuilder();
					xPath = getCustomXPath();
					NodeList subNodes = (NodeList)xPath.evaluate("hl7:assignedAuthor/hl7:assignedPerson",
							nodes.item(i).getChildNodes(),
							XPathConstants.NODESET);
					if (subNodes.getLength() > 0)
					{
						xPath = getCustomXPath();
						NodeList assignedPersonSubNodes = (NodeList)xPath.evaluate("hl7:name/hl7:prefix",
								subNodes.item(0).getChildNodes(),
								XPathConstants.NODESET);
						if (assignedPersonSubNodes.getLength() > 0)
						{
							authorName.append(((Element)assignedPersonSubNodes.item(0)).getTextContent()
									+ " ");
						}

						xPath = getCustomXPath();
						assignedPersonSubNodes = (NodeList)xPath.evaluate("hl7:name/hl7:given",
								subNodes.item(0).getChildNodes(),
						        XPathConstants.NODESET);
						if (assignedPersonSubNodes.getLength() > 0)
						{
							authorName.append(((Element)assignedPersonSubNodes.item(0)).getTextContent()
									+ " ");
						}
	
						xPath = getCustomXPath();
						assignedPersonSubNodes = (NodeList)xPath.evaluate("hl7:name/hl7:family",
								subNodes.item(0).getChildNodes(),
						        XPathConstants.NODESET);
						if (assignedPersonSubNodes.getLength() > 0)
						{
							authorName.append(((Element)assignedPersonSubNodes.item(0)).getTextContent());
						}
	
						xPath = getCustomXPath();
						assignedPersonSubNodes = (NodeList)xPath.evaluate("hl7:name/hl7:suffix",
								subNodes.item(0).getChildNodes(),
						        XPathConstants.NODESET);
						if (assignedPersonSubNodes.getLength() > 0)
						{
							authorName.append(" "
									+ ((Element)assignedPersonSubNodes.item(0)).getTextContent());
						}
					}
					else
					{
						// If assignedAuthor is not present, then check for representedOrganization/name...
						subNodes = (NodeList)xPath.evaluate("hl7:assignedAuthor/hl7:representedOrganization/hl7:name",
								nodes.item(i).getChildNodes(),
								XPathConstants.NODESET);
						xPath = XPathFactory.newInstance().newXPath();
						if (subNodes.getLength() > 0)
						{
							authorName.append(((Element)subNodes.item(0)).getTextContent());
						}
					}
					
					valueList = new ValueListType();
				    valueList.getValue().add(authorName.toString());
					slot.setValueList(valueList);
					documentAuthorClassification.getSlot().add(slot);
					
					documentAuthorClassifications.add(documentAuthorClassification);
					extrinsicObject.getClassification().add(documentAuthorClassification);
				}
			}

			// ClassCodes classification...
			ClassificationType classification = new ClassificationType();
			classification.setId(UUID.randomUUID().toString());
			classification.setClassificationScheme(documentClassCodesClassificationScheme);
			classification.setClassifiedObject(documentId);
			xPath =  getCustomXPath();
			nodes = (NodeList)xPath.evaluate("/hl7:ClinicalDocument/hl7:code",
			        doc.getDocumentElement(),
			        XPathConstants.NODESET);
			if (nodes.getLength() > 0)
			{
				//  from properties file
				classification.setNodeRepresentation(documentClassCodesNodeRepresentation);
				
				slot = new SlotType1();
				slot.setName("codingScheme");
				
				if (((Element)nodes.item(0)).getAttribute("codeSystem") != null)
				{
					valueList = new ValueListType();
					// dynamically from the document or from properties file
				    valueList.getValue().add(documentClassCodesCodingScheme) ;
					slot.setValueList(valueList);
					classification.getSlot().add(slot);
				}
				
				if (((Element)nodes.item(0)).getAttribute("displayName") != null)
				{
					InternationalStringType text = new InternationalStringType();
					LocalizedStringType localizedText = new LocalizedStringType();
					// dynamically from the document or from properties file
					localizedText.setValue(documentClassCodesName);
					text.getLocalizedString().add(localizedText);
					classification.setName(text);
				}
				
				extrinsicObject.getClassification().add(classification);
			}

			// ConfidentialityCodes classification...
			classification = new ClassificationType();
			classification.setId(UUID.randomUUID().toString());
			classification.setClassificationScheme(documentConfidentialityCodesClassificationScheme);
			classification.setClassifiedObject(documentId);
			xPath = getCustomXPath();
			nodes = (NodeList)xPath.evaluate("/hl7:ClinicalDocument/hl7:confidentialityCode",
			        doc.getDocumentElement(),
			        XPathConstants.NODESET);
			if (nodes.getLength() > 0)
			{
				if (((Element)nodes.item(0)).getAttribute("code") != null)
				{
					classification.setNodeRepresentation(((Element)nodes.item(0)).getAttribute("code"));
				}
				
				slot = new SlotType1();
				slot.setName("codingScheme");

				if (((Element)nodes.item(0)).getAttribute("codeSystem") != null)
				{
					valueList = new ValueListType();
				    valueList.getValue().add(((Element)nodes.item(0)).getAttribute("codeSystem"));
					slot.setValueList(valueList);
					classification.getSlot().add(slot);
				}
				
				if (((Element)nodes.item(0)).getAttribute("displayName") != null)
				{
					InternationalStringType text = new InternationalStringType();
					LocalizedStringType localizedText = new LocalizedStringType();
					localizedText.setValue(((Element)nodes.item(0)).getAttribute("displayName"));
					text.getLocalizedString().add(localizedText);
					classification.setName(text);
				}
				
				extrinsicObject.getClassification().add(classification);
			}

			// FormatCodes classification...
			classification = new ClassificationType();
			classification.setId(UUID.randomUUID().toString());
			classification.setClassificationScheme(documentFormatCodesClassificationScheme);
			classification.setClassifiedObject(documentId);
			classification.setNodeRepresentation(documentFormatCodesNodeRepresentation);
			slot = new SlotType1();
			slot.setName("codingScheme");
			valueList = new ValueListType();
		    valueList.getValue().add(documentFormatCodesCodingScheme);
			slot.setValueList(valueList);
			classification.getSlot().add(slot);
			InternationalStringType text = new InternationalStringType();
			LocalizedStringType localizedText = new LocalizedStringType();
			localizedText.setValue(documentFormatCodesName);
			text.getLocalizedString().add(localizedText);
			classification.setName(text);
			extrinsicObject.getClassification().add(classification);

			// HealthcareFacilityTypeCodes classification...
			classification = new ClassificationType();
			classification.setId(UUID.randomUUID().toString());
			classification.setClassificationScheme(documentHealthcareFacilityTypeCodesClassificationScheme);
			classification.setClassifiedObject(documentId);
			classification.setNodeRepresentation(documentHealthcareFacilityTypeCodesNodeRepresentation);
			slot = new SlotType1();
			slot.setName("codingScheme");
			valueList = new ValueListType();
		    valueList.getValue().add(documentHealthcareFacilityTypeCodesCodingScheme);
			slot.setValueList(valueList);
			classification.getSlot().add(slot);
			text = new InternationalStringType();
			localizedText = new LocalizedStringType();
			localizedText.setValue(documentHealthcareFacilityTypeCodesName);
			text.getLocalizedString().add(localizedText);
			classification.setName(text);
			extrinsicObject.getClassification().add(classification);

			// PracticeSettingCodes classification...
			classification = new ClassificationType();
			classification.setId(UUID.randomUUID().toString());
			classification.setClassificationScheme(documentPracticeSettingCodesClassificationScheme);
			classification.setClassifiedObject(documentId);
			xPath = getCustomXPath();
			nodes = (NodeList)xPath.evaluate("/hl7:ClinicalDocument/hl7:code",
			        doc.getDocumentElement(),
			        XPathConstants.NODESET);
			if (nodes.getLength() > 0)
			{
				if (((Element)nodes.item(0)).getAttribute("code") != null)
				{
					classification.setNodeRepresentation(documentPracticeSettingCodesNodeRepresentation);
				}
				
				slot = new SlotType1();
				slot.setName("codingScheme");
				
				if (((Element)nodes.item(0)).getAttribute("codeSystem") != null)
				{
					valueList = new ValueListType();
				    valueList.getValue().add(documentPracticeSettingCodesCodingScheme);
					slot.setValueList(valueList);
					classification.getSlot().add(slot);
				}
				
				if (((Element)nodes.item(0)).getAttribute("displayName") != null)
				{
					text = new InternationalStringType();
					localizedText = new LocalizedStringType();
					localizedText.setValue(documentPracticeSettingCodesDisplayName);
					text.getLocalizedString().add(localizedText);
					classification.setName(text);
				}
				
				extrinsicObject.getClassification().add(classification);
			}

			// Type code classification...
			classification = new ClassificationType();
			classification.setId(UUID.randomUUID().toString());
			classification.setClassificationScheme(documentContentTypeClassificationScheme);
			classification.setClassifiedObject(documentId);
			xPath = getCustomXPath();
			nodes = (NodeList)xPath.evaluate("/hl7:ClinicalDocument/hl7:code",
			        doc.getDocumentElement(),
			        XPathConstants.NODESET);
			if (nodes.getLength() > 0)
			{
				if (((Element)nodes.item(0)).getAttribute("code") != null)
				{
					classification.setNodeRepresentation(((Element)nodes.item(0)).getAttribute("code"));
				}
				
				slot = new SlotType1();
				slot.setName("codingScheme");
				
				if (((Element)nodes.item(0)).getAttribute("codeSystem") != null)
				{
					valueList = new ValueListType();
				    valueList.getValue().add(((Element)nodes.item(0)).getAttribute("codeSystem"));
					slot.setValueList(valueList);
					classification.getSlot().add(slot);
				}
				
				if (((Element)nodes.item(0)).getAttribute("displayName") != null)
				{
					text = new InternationalStringType();
					localizedText = new LocalizedStringType();
					localizedText.setValue(((Element)nodes.item(0)).getAttribute("displayName"));
					text.getLocalizedString().add(localizedText);
					classification.setName(text);
				}
				
				extrinsicObject.getClassification().add(classification);
			}
			
			// Create rim:ExternalIdentifier(s) - first the XDSDocumentEntry.patientId value(s)...
			xPath =getCustomXPath();
			nodes = (NodeList)xPath.evaluate("/hl7:ClinicalDocument/hl7:recordTarget/hl7:patientRole/hl7:id",
			        doc.getDocumentElement(),
			        XPathConstants.NODESET);
			if (nodes.getLength() > 0)
			{
				ExternalIdentifierType externalIdentifierPatientId = new ExternalIdentifierType();
				externalIdentifierPatientId.setId(UUID.randomUUID().toString());
				externalIdentifierPatientId.setRegistryObject(documentId);
				externalIdentifierPatientId.setIdentificationScheme(extrinsicObjectExternalIdentifierPatientIdIdentificationScheme);
				externalIdentifierPatientId.setValue(((Element)nodes.item(0)).getAttribute("extension")
						+ "^^^&"
						+ ((Element)nodes.item(0)).getAttribute("root")
						+ "&ISO");
				text = new InternationalStringType();
				localizedText = new LocalizedStringType();
				localizedText.setValue(extrinsicObjectExternalIdentifierPatientIdName);
				text.getLocalizedString().add(localizedText);
				externalIdentifierPatientId.setName(text);
				extrinsicObject.getExternalIdentifier().add(externalIdentifierPatientId);
			}
			
			// Now the XDSDocumentEntry.uniqueId value(s)...
			xPath = getCustomXPath();
			nodes = (NodeList)xPath.evaluate("/hl7:ClinicalDocument/hl7:id",
			        doc.getDocumentElement(),
			        XPathConstants.NODESET);
			if (nodes.getLength() > 0)
			{
				ExternalIdentifierType externalIdentifierDocumentId = new ExternalIdentifierType();
				externalIdentifierDocumentId.setId(UUID.randomUUID().toString());
				externalIdentifierDocumentId.setRegistryObject(documentId);
				externalIdentifierDocumentId.setIdentificationScheme(extrinsicObjectExternalIdentifierUniqueIdIdentificationScheme);
				
				if (testMode)
				{
					DateTime testDocId = DateTime.now(DateTimeZone.UTC);
					externalIdentifierDocumentId.setValue(((Element)nodes.item(0)).getAttribute("root")
							+ "^"
							+ testDocId.getMillis());
				}
				else
				{
					externalIdentifierDocumentId.setValue(((Element)nodes.item(0)).getAttribute("root")
							+ "^"
							+ ((Element)nodes.item(0)).getAttribute("extension"));
				}
				
				text = new InternationalStringType();
				localizedText = new LocalizedStringType();
				localizedText.setValue(extrinsicObjectExternalIdentifierUniqueIdName);
				text.getLocalizedString().add(localizedText);
				externalIdentifierDocumentId.setName(text);
				extrinsicObject.getExternalIdentifier().add(externalIdentifierDocumentId);
			}
			
			registryObjectList.getIdentifiable().add(objectFactory.createExtrinsicObject(extrinsicObject));
			
			// Create rim:RegistryPackage...
			String submissionSetId = UUID.randomUUID().toString();
			RegistryPackageType registryPackage = new RegistryPackageType();
			registryPackage.setId(submissionSetId);

			// Create rim:RegistryPackage/submissionTime attribute...
			slot = new SlotType1();
			slot.setName("submissionTime");
			valueList = new ValueListType();
			DateTime now = new DateTime(DateTimeZone.UTC);
			StringBuilder timeBuilder = new StringBuilder();
			timeBuilder.append(now.getYear());
			timeBuilder.append((now.getMonthOfYear() < 10) ? ("0" + now.getMonthOfYear())
					: now.getMonthOfYear());
			timeBuilder.append((now.getDayOfMonth() < 10) ? ("0" + now.getDayOfMonth())
					: now.getDayOfMonth());
			timeBuilder.append((now.getHourOfDay() < 10) ? ("0" + now.getHourOfDay())
					: now.getHourOfDay());
			timeBuilder.append((now.getMinuteOfHour() < 10) ? ("0" + now.getMinuteOfHour())
					: now.getMinuteOfHour());

			valueList.getValue().add(timeBuilder.toString());
			slot.setValueList(valueList);
			registryPackage.getSlot().add(slot);
			
			// Recreate authorName classification(s) in rim:RegistryPackage...
			for (ClassificationType registryClassification : documentAuthorClassifications)
			{
				ClassificationType newClassification = new ClassificationType();
				newClassification.setId(UUID.randomUUID().toString());
				newClassification.setClassificationScheme(registryPackageAuthorClassificationScheme);
				newClassification.setClassifiedObject(submissionSetId);
				newClassification.setNodeRepresentation("");
				
				if (!registryClassification.getSlot().isEmpty())
				{
					slot = new SlotType1();				
					slot.setName(registryClassification.getSlot().get(0).getName());
					slot.setValueList(registryClassification.getSlot().get(0).getValueList());
					newClassification.getSlot().add(slot);
				}
				
				registryPackage.getClassification().add(newClassification);
			}
			
			// ContentTypeCodes classification...
			classification = new ClassificationType();
			classification.setId(UUID.randomUUID().toString());
			classification.setClassificationScheme(registryPackageContentTypeCodesClassificationScheme);
			classification.setClassifiedObject(submissionSetId);
			xPath =getCustomXPath();
			nodes = (NodeList)xPath.evaluate("/hl7:ClinicalDocument/hl7:code",
			        doc.getDocumentElement(),
			        XPathConstants.NODESET);
			if (nodes.getLength() > 0)
			{
				if (((Element)nodes.item(0)).getAttribute("code") != null)
				{
					classification.setNodeRepresentation(((Element)nodes.item(0)).getAttribute("code"));
				}
				
				slot = new SlotType1();
				slot.setName("codingScheme");
				
				if (((Element)nodes.item(0)).getAttribute("codeSystem") != null)
				{
					valueList = new ValueListType();
				    valueList.getValue().add(((Element)nodes.item(0)).getAttribute("codeSystem"));
					slot.setValueList(valueList);
					classification.getSlot().add(slot);
				}
					
				if (((Element)nodes.item(0)).getAttribute("displayName") != null)
				{
					text = new InternationalStringType();
					localizedText = new LocalizedStringType();
					localizedText.setValue(((Element)nodes.item(0)).getAttribute("displayName"));
					text.getLocalizedString().add(localizedText);
					classification.setName(text);
				}
					
				registryPackage.getClassification().add(classification);					
			}
			
			// ExternalIdentifiers - first XDSSubmissionSet.uniqueId...
			ExternalIdentifierType submissionSetUniqueId = new ExternalIdentifierType();
			submissionSetUniqueId.setId(UUID.randomUUID().toString());
			submissionSetUniqueId.setRegistryObject(submissionSetId);
			submissionSetUniqueId.setIdentificationScheme(registryPackageSubmissionSetUniqueIdIdentificationScheme);
			DateTime oidTimeValue = DateTime.now(DateTimeZone.UTC);
			submissionSetUniqueId.setValue(submissionSetOid
					+ "."
					+ oidTimeValue.getMillis());
			
			text = new InternationalStringType();
			localizedText = new LocalizedStringType();
			localizedText.setValue(externalIdentifierSubmissionSetUniqueIdName);
			text.getLocalizedString().add(localizedText);
			submissionSetUniqueId.setName(text);
			
			registryPackage.getExternalIdentifier().add(submissionSetUniqueId);
			
			// Now XDSSubmissionSet.sourceId...
			ExternalIdentifierType submissionSetSourceId = new ExternalIdentifierType();
			submissionSetSourceId.setId(UUID.randomUUID().toString());
			submissionSetSourceId.setRegistryObject(submissionSetId);
			submissionSetSourceId.setIdentificationScheme(registryPackageSubmissionSetSourceIdIdentificationScheme);
			submissionSetSourceId.setValue(iExHubDomainOid);
			
			text = new InternationalStringType();
			localizedText = new LocalizedStringType();
			localizedText.setValue(externalIdentifierSubmissionSetSourceIdName);
			text.getLocalizedString().add(localizedText);
			submissionSetSourceId.setName(text);

			registryPackage.getExternalIdentifier().add(submissionSetSourceId);

			// Now XDSSubmissionSet.patientId...
			ExternalIdentifierType submissionSetPatientId = new ExternalIdentifierType();
			submissionSetPatientId.setId(UUID.randomUUID().toString());
			submissionSetPatientId.setRegistryObject(submissionSetId);
			submissionSetPatientId.setIdentificationScheme(registryPackageSubmissionSetPatientIdIdentificationScheme);
			submissionSetPatientId.setValue(patientId);

			text = new InternationalStringType();
			localizedText = new LocalizedStringType();
			localizedText.setValue(externalIdentifierSubmissionSetPatientIdName);
			text.getLocalizedString().add(localizedText);
			submissionSetPatientId.setName(text);

			registryPackage.getExternalIdentifier().add(submissionSetPatientId);
			registryObjectList.getIdentifiable().add(objectFactory.createRegistryPackage(registryPackage));
			
			// Create SubmissionSet classification for RegistryObjectList...
			ClassificationType submissionSetClassification = new ClassificationType();
			submissionSetClassification.setId(UUID.randomUUID().toString());
			submissionSetClassification.setClassifiedObject(submissionSetId);
			submissionSetClassification.setClassificationNode(registryObjectListSubmissionSetClassificationNode);
			registryObjectList.getIdentifiable().add(objectFactory.createClassification(submissionSetClassification));
			
			// Create SubmissionSet Association for RegistryObjectList...
			AssociationType1 submissionSetAssociation = new AssociationType1();
			submissionSetAssociation.setId("as01");
			submissionSetAssociation.setAssociationType("urn:oasis:names:tc:ebxml-regrep:AssociationType:HasMember");
			submissionSetAssociation.setSourceObject(submissionSetId);
			submissionSetAssociation.setTargetObject(documentId);
			slot = new SlotType1();
			slot.setName("SubmissionSetStatus");
			valueList = new ValueListType();
		    valueList.getValue().add("Original");
			slot.setValueList(valueList);
			submissionSetAssociation.getSlot().add(slot);
			registryObjectList.getIdentifiable().add(objectFactory.createAssociation(submissionSetAssociation));
			
			submitObjectsRequest.setRegistryObjectList(registryObjectList);
			documentSetRequest.setSubmitObjectsRequest(submitObjectsRequest);
			
			// Add document to message...
			XdsBDocumentRepository.ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType.Document documentForMessage = new XdsBDocumentRepository.ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType.Document();
			documentForMessage.setValue(cdaDocument);
			documentForMessage.setId(documentId);
			documentSetRequest.getDocument().add(documentForMessage);
			
			logIti41AuditMsg(submissionSetId,
					patientId);

			if (logXdsBRequestMessages)
			{
				OMElement requestElement = repositoryStub.toOM(documentSetRequest, repositoryStub.optimizeContent(
		                new javax.xml.namespace.QName("urn:ihe:iti:xds-b:2007",
		                    "ProvideAndRegisterDocumentSetRequest")),
		                new javax.xml.namespace.QName("urn:ihe:iti:xds-b:2007",
		    				"ProvideAndRegisterDocumentSetRequest"));
				Files.write(Paths.get(logOutputPath + "/" + documentId + "_ProvideAndRegisterDocumentSetRequest.xml"),
						requestElement.toString().getBytes());
			}

			return repositoryStub.documentRepository_ProvideAndRegisterDocumentSetB(documentSetRequest);
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	/**
	 *
	 * @param consent
	 * @param xmlContent
	 * @param mimeType
	 * @return
	 * @throws Exception
	 */
	public RegistryResponseType provideAndRegisterDocumentSet(Consent consent,
			byte[] xmlContent,
			String mimeType)
			throws Exception
	{
		return provideAndRegisterDocumentSet(consent,
				xmlContent,
				mimeType,
				false);
	}

	/**
	 *
	 * @param consent
	 * @param xmlContent
	 * @param mimeType
	 * @param updateDocument
	 * @return
	 * @throws Exception
	 */
	public RegistryResponseType provideAndRegisterDocumentSet(Consent consent,
			byte[] xmlContent,
			String mimeType,
			boolean updateDocument)
			throws Exception
	{
		try
		{
			UUID previousDocumentUuid = (updateDocument) ? UUID.fromString(consent.getIdElement().getIdPart())
					: null;
			UUID newDocumentUuid = UUID.randomUUID();
			String documentIdToUse = consent.getIdentifier().getValue();

			ProvideAndRegisterDocumentSetRequestType documentSetRequest = new ProvideAndRegisterDocumentSetRequestType();

			// Create SubmitObjectsRequest...
			SubmitObjectsRequest submitObjectsRequest = new SubmitObjectsRequest();

			//  Create RegistryObjectList...
			RegistryObjectListType registryObjectList = new RegistryObjectListType();

			// Create ExtrinsicObject...
			ExtrinsicObjectType extrinsicObject = new ExtrinsicObjectType();
			extrinsicObject.setId("urn:uuid:" + newDocumentUuid.toString());
			extrinsicObject.setMimeType(mimeType);
			extrinsicObject.setObjectType("urn:uuid:7edca82f-054d-47f2-a032-9b2a5b5186c1");

			// Create creationTime rim:Slot...
			ValueListType valueList = null;
			SlotType1 slot = new SlotType1();
			slot.setName("creationTime");
			valueList = new ValueListType();
			Calendar dateVal = Calendar.getInstance();
			dateVal.setTime(consent.getDateTime());
		    valueList.getValue().add(new StringBuilder()
					.append(dateVal.get(Calendar.YEAR))
					.append(String.format("%02d", (dateVal.get(Calendar.MONTH) + 1)))
					.append(String.format("%02d", dateVal.get(Calendar.DAY_OF_MONTH)))
					.append(String.format("%02d", dateVal.get(Calendar.HOUR)))
					.append(String.format("%02d", dateVal.get(Calendar.MINUTE)))
					.append(String.format("%02d", dateVal.get(Calendar.SECOND))).toString());
			slot.setValueList(valueList);
			extrinsicObject.getSlot().add(slot);

			// Create languageCode rim:Slot...
			slot = new SlotType1();
			slot.setName("languageCode");
			valueList = new ValueListType();
		    valueList.getValue().add("en-US");
			slot.setValueList(valueList);
			extrinsicObject.getSlot().add(slot);

			// Create serviceStartTime rim:Slot...
			slot = new SlotType1();
			slot.setName("serviceStartTime");
			valueList = new ValueListType();
			dateVal.setTime(consent.getPeriod().getStart());
		    valueList.getValue().add(new StringBuilder()
					.append(dateVal.get(Calendar.YEAR))
					.append(String.format("%02d", (dateVal.get(Calendar.MONTH) + 1)))
					.append(String.format("%02d", dateVal.get(Calendar.DAY_OF_MONTH)))
					.append(String.format("%02d", dateVal.get(Calendar.HOUR)))
					.append(String.format("%02d", dateVal.get(Calendar.MINUTE)))
					.append(String.format("%02d", dateVal.get(Calendar.SECOND))).toString());
			slot.setValueList(valueList);
			extrinsicObject.getSlot().add(slot);

			// Create serviceStopTime rim:Slot...
			slot = new SlotType1();
			slot.setName("serviceStopTime");
			valueList = new ValueListType();
			dateVal.setTime(consent.getPeriod().getEnd());
		    valueList.getValue().add(new StringBuilder()
					.append(dateVal.get(Calendar.YEAR))
					.append(String.format("%02d", (dateVal.get(Calendar.MONTH) + 1)))
					.append(String.format("%02d", dateVal.get(Calendar.DAY_OF_MONTH)))
					.append(String.format("%02d", dateVal.get(Calendar.HOUR)))
					.append(String.format("%02d", dateVal.get(Calendar.MINUTE)))
					.append(String.format("%02d", dateVal.get(Calendar.SECOND))).toString());
			slot.setValueList(valueList);
			extrinsicObject.getSlot().add(slot);

			// Create sourcePatientId rim:Slot...
			Reference consentSubjectRef = consent.getConsentor().get(0);
			IBaseResource referencedSubject = consentSubjectRef.getResource();
			String referencedId = referencedSubject.getIdElement().getIdPart();
			Patient patient = (getContainedResource(Patient.class, consent.getContained(), referencedId) == null) ? null
					: (Patient)getContainedResource(Patient.class, consent.getContained(), referencedId);
			slot = new SlotType1();
			slot.setName("sourcePatientId");
			valueList = new ValueListType();
			String patientId = null;
			patientId = referencedId
				+ "^^^&"
	    		+ iExHubDomainOid
	    		+ "&ISO";
		    valueList.getValue().add(patientId);
			slot.setValueList(valueList);
			extrinsicObject.getSlot().add(slot);

			// Create sourcePatientInfo rim:Slot...
			if ((patient.getName() != null) && (!patient.getName().isEmpty()))
			{
				slot = new SlotType1();
				slot.setName("sourcePatientInfo");
				valueList = new ValueListType();
			    valueList.getValue().add("PID-3|"
			    		+ referencedId
						+ "^^^&"
			    		+ iExHubDomainOid
			    		+ "&ISO");

			    StringBuilder name = new StringBuilder();
			    name.append("PID-5|"
			    		+ ((patient.getName().get(0).getFamily() != null) ? patient.getName().get(0).getFamily()
			    				: "")
			    		+ "^");

				name.append(((patient.getName().get(0).getGivenAsSingleString() != null) ? patient.getName().get(0).getGivenAsSingleString()
						: "")
						+ "^^");

				name.append(((patient.getName().get(0).getPrefixAsSingleString() != null) ? patient.getName().get(0).getPrefixAsSingleString()
						: "")
						+ "^");

				name.append(((patient.getName().get(0).getSuffixAsSingleString() != null) ? patient.getName().get(0).getSuffixAsSingleString()
						: ""));

				valueList.getValue().add(name.toString());
			}

			// Birthdate
			dateVal.setTime(patient.getBirthDate());
		    valueList.getValue().add(new StringBuilder()
		    		.append("PID-7|")
					.append(dateVal.get(Calendar.YEAR))
					.append(String.format("%02d", (dateVal.get(Calendar.MONTH) + 1)))
					.append(String.format("%02d", dateVal.get(Calendar.DAY_OF_MONTH))).toString());

		    // Administrative gender code
		    valueList.getValue().add("PID-8|"
		    		+ ((patient.getGender().getDefinition().compareToIgnoreCase("female") == 0) || (patient.getGender().getDefinition().compareToIgnoreCase("f") == 0) ? "F"
		    				: (((patient.getGender().getDefinition().compareToIgnoreCase("male") == 0) || (patient.getGender().getDefinition().compareToIgnoreCase("m") == 0) ? "M"
		    						: "U"))));

		    // Address info
		    if ((patient.getAddress() != null) &&
		    	(!patient.getAddress().isEmpty()))
			{
				StringBuilder address = new StringBuilder();
				address.append("PID-11|");

				// Street address line
				StringBuilder addressLine = new StringBuilder();
				for (StringType lineItem : patient.getAddress().get(0).getLine())
				{
					addressLine.append(lineItem.getValue());
				}
				address.append(addressLine
						+ "^^");

				// City
				address.append(((patient.getAddress().get(0).getCity() != null) ? patient.getAddress().get(0).getCity()
						: "")
						+ "^");

				// State
				address.append(((patient.getAddress().get(0).getState() != null) ? patient.getAddress().get(0).getState()
						: "")
						+ "^");

				// Postal code
				address.append(((patient.getAddress().get(0).getPostalCode() != null) ? patient.getAddress().get(0).getPostalCode()
						: "")
						+ "^");

				// Country
				address.append(((patient.getAddress().get(0).getCountry() != null) ? patient.getAddress().get(0).getCountry()
						: ""));

			    valueList.getValue().add(address.toString());
			}

			slot.setValueList(valueList);
			extrinsicObject.getSlot().add(slot);

			// Create classifications - start with document author(s) represented in the Contract as the patient...
			ArrayList<ClassificationType> documentAuthorClassifications = new ArrayList<ClassificationType>();
			StringBuilder authorName = new StringBuilder();
			ClassificationType documentAuthorClassification = new ClassificationType();
			documentAuthorClassification.setId(UUID.randomUUID().toString());
			documentAuthorClassification.setClassificationScheme(documentAuthorClassificationScheme);
			documentAuthorClassification.setClassifiedObject("urn:uuid:" + newDocumentUuid.toString());
			documentAuthorClassification.setNodeRepresentation("");
			slot = new SlotType1();
			slot.setName("authorPerson");

			// authorPerson rim:Slot
			// Prefix
			authorName.append(((patient.getName().get(0).getPrefixAsSingleString() != null) ? (patient.getName().get(0).getPrefixAsSingleString() + " ")
					: ""));

			// Given name
			authorName.append(((patient.getName().get(0).getGivenAsSingleString() != null) ? (patient.getName().get(0).getGivenAsSingleString() + " ")
					: ""));

			// Family name
			authorName.append(((patient.getName().get(0).getFamily() != null) ? (patient.getName().get(0).getFamily())
					: ""));

			// Suffix
			authorName.append(((patient.getName().get(0).getSuffixAsSingleString() != null) ? (" " + patient.getName().get(0).getSuffixAsSingleString())
					: ""));

			valueList = new ValueListType();
		    valueList.getValue().add(authorName.toString());
			slot.setValueList(valueList);
			documentAuthorClassification.getSlot().add(slot);

			documentAuthorClassifications.add(documentAuthorClassification);
			extrinsicObject.getClassification().add(documentAuthorClassification);
			
			// ClassCodes classification...
			ClassificationType classification = new ClassificationType();
			classification.setId(UUID.randomUUID().toString());
			classification.setClassificationScheme(documentClassCodesClassificationScheme);
			classification.setClassifiedObject("urn:uuid:" + newDocumentUuid.toString());

			classification.setNodeRepresentation(documentClassCodesNodeRepresentationContract);
				
			slot = new SlotType1();
			slot.setName("codingScheme");

			// Code system
			valueList = new ValueListType();
		    valueList.getValue().add("1.3.6.1.4.1.21367.100.1");
			slot.setValueList(valueList);
			classification.getSlot().add(slot);

			// Display name
			InternationalStringType text = new InternationalStringType();
			LocalizedStringType localizedText = new LocalizedStringType();
			localizedText.setValue("Privacy Policy Acknowledgement Document");
			text.getLocalizedString().add(localizedText);
			classification.setName(text);
				
			extrinsicObject.getClassification().add(classification);

			// ConfidentialityCodes classification...
			classification = new ClassificationType();
			classification.setId(UUID.randomUUID().toString());
			classification.setClassificationScheme(documentConfidentialityCodesClassificationScheme);
			classification.setClassifiedObject("urn:uuid:" + newDocumentUuid.toString());
			// Code
			classification.setNodeRepresentation("N");
				
			slot = new SlotType1();
			slot.setName("codingScheme");

			// Code system
			valueList = new ValueListType();
		    valueList.getValue().add("2.16.840.1.113883.5.25");
			slot.setValueList(valueList);
			classification.getSlot().add(slot);

			// Display name
			text = new InternationalStringType();
			localizedText = new LocalizedStringType();
			localizedText.setValue("Confidentiality Code");
			text.getLocalizedString().add(localizedText);
			classification.setName(text);
				
			extrinsicObject.getClassification().add(classification);

			// FormatCodes classification...
			classification = new ClassificationType();
			classification.setId(UUID.randomUUID().toString());
			classification.setClassificationScheme(documentFormatCodesClassificationScheme);
			classification.setClassifiedObject("urn:uuid:" + newDocumentUuid.toString());
			classification.setNodeRepresentation(documentFormatCodesNodeRepresentation);
			slot = new SlotType1();
			slot.setName("codingScheme");
			valueList = new ValueListType();
		    valueList.getValue().add(documentFormatCodesCodingScheme);
			slot.setValueList(valueList);
			classification.getSlot().add(slot);
			text = new InternationalStringType();
			localizedText = new LocalizedStringType();
			localizedText.setValue(documentFormatCodesName);
			text.getLocalizedString().add(localizedText);
			classification.setName(text);
			extrinsicObject.getClassification().add(classification);

			// HealthcareFacilityTypeCodes classification...
			classification = new ClassificationType();
			classification.setId(UUID.randomUUID().toString());
			classification.setClassificationScheme(documentHealthcareFacilityTypeCodesClassificationScheme);
			classification.setClassifiedObject("urn:uuid:" + newDocumentUuid.toString());
			classification.setNodeRepresentation(documentHealthcareFacilityTypeCodesNodeRepresentation);
			slot = new SlotType1();
			slot.setName("codingScheme");
			valueList = new ValueListType();
		    valueList.getValue().add(documentHealthcareFacilityTypeCodesCodingScheme);
			slot.setValueList(valueList);
			classification.getSlot().add(slot);
			text = new InternationalStringType();
			localizedText = new LocalizedStringType();
			localizedText.setValue(documentHealthcareFacilityTypeCodesName);
			text.getLocalizedString().add(localizedText);
			classification.setName(text);
			extrinsicObject.getClassification().add(classification);

			// PracticeSettingCodes classification...
			classification = new ClassificationType();
			classification.setId(UUID.randomUUID().toString());
			classification.setClassificationScheme(documentPracticeSettingCodesClassificationScheme);
			classification.setClassifiedObject("urn:uuid:" + newDocumentUuid.toString());
			classification.setNodeRepresentation(documentPracticeSettingCodesNodeRepresentation);
				
			slot = new SlotType1();
			slot.setName("codingScheme");
				
			valueList = new ValueListType();
		    valueList.getValue().add(documentPracticeSettingCodesCodingScheme);
			slot.setValueList(valueList);
			classification.getSlot().add(slot);
				
			text = new InternationalStringType();
			localizedText = new LocalizedStringType();
			localizedText.setValue(documentPracticeSettingCodesDisplayName);
			text.getLocalizedString().add(localizedText);
			classification.setName(text);
				
			extrinsicObject.getClassification().add(classification);

			// Type code classification...
			classification = new ClassificationType();
			classification.setId(UUID.randomUUID().toString());
			classification.setClassificationScheme(documentContentTypeClassificationScheme);
			classification.setClassifiedObject("urn:uuid:" + newDocumentUuid.toString());
			
			// Code
			classification.setNodeRepresentation("57016-8");
				
			slot = new SlotType1();
			slot.setName("codingScheme");

			//  Code system
			valueList = new ValueListType();
		    valueList.getValue().add("2.16.840.1.113883.6.1");
			slot.setValueList(valueList);
			classification.getSlot().add(slot);

			// Display name
			text = new InternationalStringType();
			localizedText = new LocalizedStringType();
			localizedText.setValue("Privacy Policy Acknowledgement Document");
			text.getLocalizedString().add(localizedText);
			classification.setName(text);
				
			extrinsicObject.getClassification().add(classification);
			
			// Create rim:ExternalIdentifier(s) - first the XDSDocumentEntry.patientId value(s)...
			ExternalIdentifierType externalIdentifierPatientId = new ExternalIdentifierType();
			externalIdentifierPatientId.setId(UUID.randomUUID().toString());
			externalIdentifierPatientId.setRegistryObject("urn:uuid:" + newDocumentUuid.toString());
			externalIdentifierPatientId.setIdentificationScheme(extrinsicObjectExternalIdentifierPatientIdIdentificationScheme);
			externalIdentifierPatientId.setValue(referencedId
					+ "^^^&"
					+ iExHubDomainOid
					+ "&ISO");
			text = new InternationalStringType();
			localizedText = new LocalizedStringType();
			localizedText.setValue(extrinsicObjectExternalIdentifierPatientIdName);
			text.getLocalizedString().add(localizedText);
			externalIdentifierPatientId.setName(text);
			extrinsicObject.getExternalIdentifier().add(externalIdentifierPatientId);
			
			// Now the XDSDocumentEntry.uniqueId value(s)...
			ExternalIdentifierType externalIdentifierUniqueId = new ExternalIdentifierType();
			externalIdentifierUniqueId.setId(UUID.randomUUID().toString());
			externalIdentifierUniqueId.setRegistryObject("urn:uuid:" + newDocumentUuid.toString());
			externalIdentifierUniqueId.setIdentificationScheme(extrinsicObjectExternalIdentifierUniqueIdIdentificationScheme);

			//externalIdentifierUniqueId.setValue(documentIdToUse);
			externalIdentifierUniqueId.setValue(getOid());

			text = new InternationalStringType();
			localizedText = new LocalizedStringType();
			localizedText.setValue(extrinsicObjectExternalIdentifierUniqueIdName);
			text.getLocalizedString().add(localizedText);
			externalIdentifierUniqueId.setName(text);
			extrinsicObject.getExternalIdentifier().add(externalIdentifierUniqueId);
			
			registryObjectList.getIdentifiable().add(objectFactory.createExtrinsicObject(extrinsicObject));
			
			// Create rim:RegistryPackage...
			String submissionSetId = UUID.randomUUID().toString();
			RegistryPackageType registryPackage = new RegistryPackageType();
			registryPackage.setId(submissionSetId);

			// Create rim:RegistryPackage/submissionTime attribute...
			slot = new SlotType1();
			slot.setName("submissionTime");
			valueList = new ValueListType();
			DateTime now = new DateTime(DateTimeZone.UTC);
			StringBuilder timeBuilder = new StringBuilder();
			timeBuilder.append(now.getYear());
			timeBuilder.append((now.getMonthOfYear() < 10) ? ("0" + now.getMonthOfYear())
					: now.getMonthOfYear());
			timeBuilder.append((now.getDayOfMonth() < 10) ? ("0" + now.getDayOfMonth())
					: now.getDayOfMonth());
			timeBuilder.append((now.getHourOfDay() < 10) ? ("0" + now.getHourOfDay())
					: now.getHourOfDay());
			timeBuilder.append((now.getMinuteOfHour() < 10) ? ("0" + now.getMinuteOfHour())
					: now.getMinuteOfHour());

			valueList.getValue().add(timeBuilder.toString());
			slot.setValueList(valueList);
			registryPackage.getSlot().add(slot);
			
			// Recreate authorName classification(s) in rim:RegistryPackage...
			for (ClassificationType registryClassification : documentAuthorClassifications)
			{
				ClassificationType newClassification = new ClassificationType();
				newClassification.setId(UUID.randomUUID().toString());
				newClassification.setClassificationScheme(registryPackageAuthorClassificationScheme);
				newClassification.setClassifiedObject(submissionSetId);
				newClassification.setNodeRepresentation("");
				
				if (!registryClassification.getSlot().isEmpty())
				{
					slot = new SlotType1();				
					slot.setName(registryClassification.getSlot().get(0).getName());
					slot.setValueList(registryClassification.getSlot().get(0).getValueList());
					newClassification.getSlot().add(slot);
				}
				
				registryPackage.getClassification().add(newClassification);
			}
			
			// ContentTypeCodes classification...
			classification = new ClassificationType();
			classification.setId(UUID.randomUUID().toString());
			classification.setClassificationScheme(registryPackageContentTypeCodesClassificationScheme);
			classification.setClassifiedObject(submissionSetId);
			// Code
			classification.setNodeRepresentation("57016-8");
				
			slot = new SlotType1();
			slot.setName("codingScheme");

			// Code system
			valueList = new ValueListType();
		    valueList.getValue().add("2.16.840.1.113883.6.1");
			slot.setValueList(valueList);
			classification.getSlot().add(slot);

			// Display name
			text = new InternationalStringType();
			localizedText = new LocalizedStringType();
			localizedText.setValue("Privacy Policy Acknowledgement Document");
			text.getLocalizedString().add(localizedText);
			classification.setName(text);
					
			registryPackage.getClassification().add(classification);					

			// ExternalIdentifiers - first XDSSubmissionSet.uniqueId...
			ExternalIdentifierType submissionSetUniqueId = new ExternalIdentifierType();
			submissionSetUniqueId.setId(UUID.randomUUID().toString());
			submissionSetUniqueId.setRegistryObject(submissionSetId);
			submissionSetUniqueId.setIdentificationScheme(registryPackageSubmissionSetUniqueIdIdentificationScheme);
			DateTime oidTimeValue = DateTime.now(DateTimeZone.UTC);
			submissionSetUniqueId.setValue(submissionSetOid
					+ "."
					+ oidTimeValue.getMillis());
			
			text = new InternationalStringType();
			localizedText = new LocalizedStringType();
			localizedText.setValue(externalIdentifierSubmissionSetUniqueIdName);
			text.getLocalizedString().add(localizedText);
			submissionSetUniqueId.setName(text);
			
			registryPackage.getExternalIdentifier().add(submissionSetUniqueId);
			
			// Now XDSSubmissionSet.sourceId...
			ExternalIdentifierType submissionSetSourceId = new ExternalIdentifierType();
			submissionSetSourceId.setId(UUID.randomUUID().toString());
			submissionSetSourceId.setRegistryObject(submissionSetId);
			submissionSetSourceId.setIdentificationScheme(registryPackageSubmissionSetSourceIdIdentificationScheme);
			submissionSetSourceId.setValue(iExHubDomainOid);
			
			text = new InternationalStringType();
			localizedText = new LocalizedStringType();
			localizedText.setValue(externalIdentifierSubmissionSetSourceIdName);
			text.getLocalizedString().add(localizedText);
			submissionSetSourceId.setName(text);

			registryPackage.getExternalIdentifier().add(submissionSetSourceId);

			// Now XDSSubmissionSet.patientId...
			ExternalIdentifierType submissionSetPatientId = new ExternalIdentifierType();
			submissionSetPatientId.setId(UUID.randomUUID().toString());
			submissionSetPatientId.setRegistryObject(submissionSetId);
			submissionSetPatientId.setIdentificationScheme(registryPackageSubmissionSetPatientIdIdentificationScheme);
			submissionSetPatientId.setValue(patientId);

			text = new InternationalStringType();
			localizedText = new LocalizedStringType();
			localizedText.setValue(externalIdentifierSubmissionSetPatientIdName);
			text.getLocalizedString().add(localizedText);
			submissionSetPatientId.setName(text);

			registryPackage.getExternalIdentifier().add(submissionSetPatientId);
			registryObjectList.getIdentifiable().add(objectFactory.createRegistryPackage(registryPackage));
			
			// Create SubmissionSet classification for RegistryObjectList...
			ClassificationType submissionSetClassification = new ClassificationType();
			submissionSetClassification.setId(UUID.randomUUID().toString());
			submissionSetClassification.setClassifiedObject(submissionSetId);
			submissionSetClassification.setClassificationNode(registryObjectListSubmissionSetClassificationNode);
			registryObjectList.getIdentifiable().add(objectFactory.createClassification(submissionSetClassification));
			
			// Create SubmissionSet Association for RegistryObjectList...
			AssociationType1 submissionSetAssociation = new AssociationType1();
			submissionSetAssociation.setId("as01");
			submissionSetAssociation.setAssociationType("urn:oasis:names:tc:ebxml-regrep:AssociationType:HasMember");
			submissionSetAssociation.setSourceObject(submissionSetId);
			submissionSetAssociation.setTargetObject("urn:uuid:" + newDocumentUuid.toString());
			
			slot = new SlotType1();
			slot.setName("SubmissionSetStatus");
			valueList = new ValueListType();
		    valueList.getValue().add("Original");
			slot.setValueList(valueList);
			submissionSetAssociation.getSlot().add(slot);
			registryObjectList.getIdentifiable().add(objectFactory.createAssociation(submissionSetAssociation));
			
			// If updating a document, then we need to add another association which links the current document in the repository to this one...
			if (updateDocument)
			{
				AssociationType1 rplcAssociation = new AssociationType1();
				rplcAssociation.setId("Assoc1");
				rplcAssociation.setAssociationType("urn:ihe:iti:2007:AssociationType:RPLC");
				rplcAssociation.setTargetObject("urn:uuid:"
						+ previousDocumentUuid.toString());
				rplcAssociation.setSourceObject("urn:uuid:" + newDocumentUuid.toString());
				
				registryObjectList.getIdentifiable().add(objectFactory.createAssociation(rplcAssociation));
				
				// Replace old contract identifier with new one...
				consent.getIdElement().setValue(newDocumentUuid.toString());
			}
			else
			{
				consent.getIdElement().setValueAsString(newDocumentUuid.toString());
			}
			
			submitObjectsRequest.setRegistryObjectList(registryObjectList);
			documentSetRequest.setSubmitObjectsRequest(submitObjectsRequest);
			
			// Add document to message...
			XdsBDocumentRepository.ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType.Document documentForMessage = new XdsBDocumentRepository.ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType.Document();
			documentForMessage.setValue(xmlContent);
			documentForMessage.setId("urn:uuid:" + newDocumentUuid.toString());
			documentSetRequest.getDocument().add(documentForMessage);
			
			logIti41AuditMsg(submissionSetId,
					patientId);

			if (logXdsBRequestMessages)
			{
				OMElement requestElement = repositoryStub.toOM(documentSetRequest, repositoryStub.optimizeContent(
		                new javax.xml.namespace.QName("urn:ihe:iti:xds-b:2007",
		                    "ProvideAndRegisterDocumentSetRequest")),
		                new javax.xml.namespace.QName("urn:ihe:iti:xds-b:2007",
		    				"ProvideAndRegisterDocumentSetRequest"));
				Files.write(Paths.get(logOutputPath + "/" + newDocumentUuid.toString() + "_ProvideAndRegisterDocumentSetRequest.xml"),
						requestElement.toString().getBytes());
			}

			return repositoryStub.documentRepository_ProvideAndRegisterDocumentSetB(documentSetRequest);
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	private Object getContainedResource(Class<?> resourceClass,
			List<Resource> containedResources,
			String idValue)
	{
		Object retVal = null;
		for (Resource resource : containedResources)
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

	private XPath getCustomXPath(){
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
		return xPath;

	}

	private String getOid() {
		final UUID uuid = UUID.randomUUID();
		String id = String.valueOf(uuid);

		id = id.replace("-", ".");

		id = id.replace("a", "10");
		id = id.replace("b", "11");
		id = id.replace("c", "12");
		id = id.replace("d", "13");
		id = id.replace("e", "14");
		id = id.replace("f", "15");

		// Removes leading zeroes
		id = id.replaceFirst("^0+(?!$)", "");

		return id;
	}
}