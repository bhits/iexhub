package com.InfoExchangeHub.Connectors;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Properties;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.productivity.java.syslog4j.Syslog;
import org.productivity.java.syslog4j.SyslogConfigIF;
import org.productivity.java.syslog4j.impl.net.tcp.ssl.SSLTCPNetSyslogConfig;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.InfoExchangeHub.Exceptions.DocumentTypeUnsupportedException;
import com.InfoExchangeHub.Exceptions.UnexpectedServerException;

import XdsBDocumentRepository.src.com.InfoExchangeHub.Services.Client.DocumentRepository_ServiceStub;
import XdsBDocumentRepository.src.ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;
import XdsBDocumentRepository.src.oasis.names.tc.ebxml_regrep.xsd.lcm._3.SubmitObjectsRequest;
import XdsBDocumentRepository.src.oasis.names.tc.ebxml_regrep.xsd.rim._3.AssociationType1;
import XdsBDocumentRepository.src.oasis.names.tc.ebxml_regrep.xsd.rim._3.ClassificationType;
import XdsBDocumentRepository.src.oasis.names.tc.ebxml_regrep.xsd.rim._3.ExternalIdentifierType;
import XdsBDocumentRepository.src.oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import XdsBDocumentRepository.src.oasis.names.tc.ebxml_regrep.xsd.rim._3.InternationalStringType;
import XdsBDocumentRepository.src.oasis.names.tc.ebxml_regrep.xsd.rim._3.ObjectFactory;
import XdsBDocumentRepository.src.oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectListType;
import XdsBDocumentRepository.src.oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import XdsBDocumentRepository.src.oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;
import XdsBDocumentRepository.src.oasis.names.tc.ebxml_regrep.xsd.rim._3.ValueListType;
import XdsBDocumentRepository.src.oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType;
import XdsBDocumentRepository.src.oasis.names.tc.ebxml_regrep.xsd.rim._3.LocalizedStringType;

/**
 * @author A. Sute
 *
 */

public class XdsBRepositoryManager
{
	/** Logger */
    public static final Logger log = Logger.getLogger(XdsBRepositoryManager.class);

	private static final String propertiesFile = "/temp/IExHub.properties";
	private static boolean testMode = false;

    private static String keyStoreFile = "c:/temp/1264.jks";
	private static String keyStorePwd = "IEXhub";
	private static String cipherSuites = "TLS_RSA_WITH_AES_128_CBC_SHA";
	private static String httpsProtocols = "TLSv1";

	private static String iti41AuditMsgTemplate = null;

	private static SyslogConfigIF sysLogConfig = null;

	private static final SOAPFactory soapFactory = OMAbstractFactory.getSOAP12Factory();
	private static final ObjectFactory objectFactory = new ObjectFactory();

	private static DocumentRepository_ServiceStub repositoryStub = null;

	private static boolean debugSSL = false;

	private static String repositoryEndpointURI = null;
	private static String iExHubDomainOid = "1.3.6.1.4.1.21367.13.60.232";

	private static String documentAuthorClassificationScheme = "urn:uuid:93606bcf-9494-43ec-9b4e-a7748d1a838d";
	private static String documentClassCodesClassificationScheme = "urn:uuid:41a5887f-8865-4c09-adf7-e362475b143a";
	private static String documentConfidentialityCodesClassificationScheme = "urn:uuid:f4f85eac-e6cb-4883-b524-f2705394840f";
	private static String documentContentTypeClassificationScheme = "urn:uuid:f0306f51-975f-434e-a61c-c59651d33983";
	private static String documentFormatCodesClassificationScheme = "urn:uuid:a09d5840-386c-46f2-b5ad-9c3699a4309d";
	private static String documentFormatCodesNodeRepresentation = "urn:ihe:iti:xds-sd:text:2008";
	private static String documentFormatCodesCodingScheme = "1.3.6.1.4.1.19376.1.2.3";
	private static String documentFormatCodesName = "Health encounter site";
	private static String documentHealthcareFacilityTypeCodesClassificationScheme = "urn:uuid:f33fb8ac-18af-42cc-ae0e-ed0b0bdb91e1";
	private static String documentHealthcareFacilityTypeCodesNodeRepresentation = "unobtainable";
	private static String documentPracticeSettingCodesClassificationScheme = "urn:uuid:cccf5598-8b07-4b77-a05e-ae952c785ead";
	private static String documentPracticeSettingCodesNodeRepresentation = "unobtainable";
	private static String documentPracticeSettingCodesCodingScheme = "Connect-a-thon practiceSettingCodes";
	private static String documentHealthcareFacilityTypeCodesCodingScheme = "Connect-a-thon healthcareFacilityTypeCodes";
	private static String documentHealthcareFacilityTypeCodesName = "unobtainable";
	private static String documentPracticeSettingCodesDisplayName = "unobtainable";

	private static String extrinsicObjectExternalIdentifierPatientIdIdentificationScheme = "urn:uuid:58a6f841-87b3-4a3e-92fd-a8ffeff98427";
	private static String extrinsicObjectExternalIdentifierPatientIdName = "XDSDocumentEntry.patientId";
	private static String extrinsicObjectExternalIdentifierUniqueIdIdentificationScheme = "urn:uuid:2e82c1f6-a085-4c72-9da3-8640a32e42ab";
	private static String extrinsicObjectExternalIdentifierUniqueIdName = "XDSDocumentEntry.uniqueId";

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

	
	public static void setRepositoryEndpointURI(String repositoryEndpointURI)
	{
		if (repositoryStub != null)
		{
			repositoryStub._getServiceClient().getOptions().setTo(new org.apache.axis2.addressing.EndpointReference(repositoryEndpointURI));
			XdsBRepositoryManager.repositoryEndpointURI = repositoryEndpointURI;
		}
	}

	public XdsBRepositoryManager(String registryEndpointURI,
			String repositoryEndpointURI) throws AxisFault, Exception
	{
		this(registryEndpointURI,
				repositoryEndpointURI,
				false);
	}
	
	public XdsBRepositoryManager(String registryEndpointURI,
			String repositoryEndpointURI,
			boolean enableTLS) throws AxisFault, Exception
	{
		Properties props = new Properties();
		try
		{
			props.load(new FileInputStream(propertiesFile));
			
			XdsBRepositoryManager.debugSSL = (props.getProperty("DebugSSL") == null) ? XdsBRepositoryManager.debugSSL
					: Boolean.parseBoolean(props.getProperty("DebugSSL"));
			XdsBRepositoryManager.testMode = (props.getProperty("TestMode") == null) ? XdsBRepositoryManager.testMode
					: Boolean.parseBoolean(props.getProperty("TestMode"));
			XdsBRepositoryManager.iExHubDomainOid = (props.getProperty("IExHubDomainOID") == null) ? XdsBRepositoryManager.iExHubDomainOid
					: props.getProperty("IExHubDomainOID");
			XdsBRepositoryManager.keyStoreFile = (props.getProperty("XdsBKeyStoreFile") == null) ? XdsBRepositoryManager.keyStoreFile
					: props.getProperty("XdsBKeyStoreFile");
			XdsBRepositoryManager.keyStorePwd = (props.getProperty("XdsBKeyStorePwd") == null) ? XdsBRepositoryManager.keyStorePwd
					: props.getProperty("XdsBKeyStorePwd");
			XdsBRepositoryManager.cipherSuites = (props.getProperty("XdsBCipherSuites") == null) ? XdsBRepositoryManager.cipherSuites
					: props.getProperty("XdsBCipherSuites");
			XdsBRepositoryManager.httpsProtocols = (props.getProperty("XdsBHttpsProtocols") == null) ? XdsBRepositoryManager.httpsProtocols
					: props.getProperty("XdsBHttpsProtocols");

			XdsBRepositoryManager.documentAuthorClassificationScheme = (props.getProperty("XdsBDocumentAuthorClassificationScheme") == null) ? XdsBRepositoryManager.documentAuthorClassificationScheme
					: props.getProperty("XdsBDocumentAuthorClassificationScheme");
			XdsBRepositoryManager.documentClassCodesClassificationScheme = (props.getProperty("XdsBDocumentClassCodesClassificationScheme") == null) ? XdsBRepositoryManager.documentClassCodesClassificationScheme
					: props.getProperty("XdsBDocumentClassCodesClassificationScheme");
			XdsBRepositoryManager.documentConfidentialityCodesClassificationScheme = (props.getProperty("XdsBDocumentConfidentialityCodesClassificationScheme") == null) ? XdsBRepositoryManager.documentConfidentialityCodesClassificationScheme
					: props.getProperty("XdsBDocumentConfidentialityCodesClassificationScheme");
			XdsBRepositoryManager.documentContentTypeClassificationScheme = (props.getProperty("XdsBDocumentContentTypeClassificationScheme") == null) ? XdsBRepositoryManager.documentContentTypeClassificationScheme
					: props.getProperty("XdsBDocumentContentTypeClassificationScheme");
			XdsBRepositoryManager.documentFormatCodesClassificationScheme = (props.getProperty("XdsBDocumentFormatCodesClassificationScheme") == null) ? XdsBRepositoryManager.documentFormatCodesClassificationScheme
					: props.getProperty("XdsBDocumentFormatCodesClassificationScheme");
			XdsBRepositoryManager.documentFormatCodesNodeRepresentation = (props.getProperty("XdsBDocumentFormatCodesNodeRepresentation") == null) ? XdsBRepositoryManager.documentFormatCodesNodeRepresentation
					: props.getProperty("XdsBDocumentFormatCodesNodeRepresentation");
			XdsBRepositoryManager.documentFormatCodesCodingScheme = (props.getProperty("XdsBDocumentFormatCodesCodingScheme") == null) ? XdsBRepositoryManager.documentFormatCodesCodingScheme
					: props.getProperty("XdsBDocumentFormatCodesCodingScheme");
			XdsBRepositoryManager.documentFormatCodesName = (props.getProperty("XdsBDocumentFormatCodesName") == null) ? XdsBRepositoryManager.documentFormatCodesName
					: props.getProperty("XdsBDocumentFormatCodesName");
			XdsBRepositoryManager.documentHealthcareFacilityTypeCodesClassificationScheme = (props.getProperty("XdsBDocumentHealthcareFacilityTypeCodesClassificationScheme") == null) ? XdsBRepositoryManager.documentHealthcareFacilityTypeCodesClassificationScheme
					: props.getProperty("XdsBDocumentHealthcareFacilityTypeCodesClassificationScheme");
			XdsBRepositoryManager.documentHealthcareFacilityTypeCodesNodeRepresentation = (props.getProperty("XdsBDocumentHealthcareFacilityTypeCodesNodeRepresentation") == null) ? XdsBRepositoryManager.documentHealthcareFacilityTypeCodesNodeRepresentation
					: props.getProperty("XdsBDocumentHealthcareFacilityTypeCodesNodeRepresentation");
			XdsBRepositoryManager.documentPracticeSettingCodesClassificationScheme = (props.getProperty("XdsBDocumentPracticeSettingCodesClassificationScheme") == null) ? XdsBRepositoryManager.documentPracticeSettingCodesClassificationScheme
					: props.getProperty("XdsBDocumentPracticeSettingCodesClassificationScheme");
			XdsBRepositoryManager.documentPracticeSettingCodesNodeRepresentation = (props.getProperty("XdsBDocumentPracticeSettingCodesNodeRepresentation") == null) ? XdsBRepositoryManager.documentPracticeSettingCodesNodeRepresentation
					: props.getProperty("XdsBDocumentPracticeSettingCodesNodeRepresentation");
			XdsBRepositoryManager.documentPracticeSettingCodesCodingScheme = (props.getProperty("XdsBDocumentPracticeSettingCodesCodingScheme") == null) ? XdsBRepositoryManager.documentPracticeSettingCodesCodingScheme
					: props.getProperty("XdsBDocumentPracticeSettingCodesCodingScheme");
			XdsBRepositoryManager.documentHealthcareFacilityTypeCodesCodingScheme = (props.getProperty("XdsBDocumentHealthcareFacilityTypeCodesCodingScheme") == null) ? XdsBRepositoryManager.documentHealthcareFacilityTypeCodesCodingScheme
					: props.getProperty("XdsBDocumentHealthcareFacilityTypeCodesCodingScheme");
			XdsBRepositoryManager.documentHealthcareFacilityTypeCodesName = (props.getProperty("XdsBDocumentHealthcareFacilityTypeCodesName") == null) ? XdsBRepositoryManager.documentHealthcareFacilityTypeCodesName
					: props.getProperty("XdsBDocumentHealthcareFacilityTypeCodesName");
			XdsBRepositoryManager.documentPracticeSettingCodesDisplayName = (props.getProperty("XdsBDocumentPracticeSettingCodesDisplayName") == null) ? XdsBRepositoryManager.documentPracticeSettingCodesDisplayName
					: props.getProperty("XdsBDocumentPracticeSettingCodesDisplayName");

			XdsBRepositoryManager.extrinsicObjectExternalIdentifierPatientIdIdentificationScheme = (props.getProperty("XdsBExtrinsicObjectExternalIdentifierPatientIdIdentificationScheme") == null) ? XdsBRepositoryManager.extrinsicObjectExternalIdentifierPatientIdIdentificationScheme
					: props.getProperty("XdsBExtrinsicObjectExternalIdentifierPatientIdIdentificationScheme");
			XdsBRepositoryManager.extrinsicObjectExternalIdentifierPatientIdName = (props.getProperty("XdsBExtrinsicObjectExternalIdentifierPatientIdName") == null) ? XdsBRepositoryManager.extrinsicObjectExternalIdentifierPatientIdName
					: props.getProperty("XdsBExtrinsicObjectExternalIdentifierPatientIdName");
			XdsBRepositoryManager.extrinsicObjectExternalIdentifierUniqueIdIdentificationScheme = (props.getProperty("XdsBExtrinsicObjectExternalIdentifierUniqueIdIdentificationScheme") == null) ? XdsBRepositoryManager.extrinsicObjectExternalIdentifierUniqueIdIdentificationScheme
					: props.getProperty("XdsBExtrinsicObjectExternalIdentifierUniqueIdIdentificationScheme");
			XdsBRepositoryManager.extrinsicObjectExternalIdentifierUniqueIdName = (props.getProperty("XdsBExtrinsicObjectExternalIdentifierUniqueIdName") == null) ? XdsBRepositoryManager.extrinsicObjectExternalIdentifierUniqueIdName
					: props.getProperty("XdsBExtrinsicObjectExternalIdentifierUniqueIdName");

			XdsBRepositoryManager.registryPackageAuthorClassificationScheme = (props.getProperty("XdsBRegistryPackageAuthorClassificationScheme") == null) ? XdsBRepositoryManager.registryPackageAuthorClassificationScheme
					: props.getProperty("XdsBRegistryPackageAuthorClassificationScheme");
			XdsBRepositoryManager.registryPackageContentTypeCodesClassificationScheme = (props.getProperty("XdsBRegistryPackageContentTypeCodesClassificationScheme") == null) ? XdsBRepositoryManager.registryPackageContentTypeCodesClassificationScheme
					: props.getProperty("XdsBRegistryPackageContentTypeCodesClassificationScheme");
			XdsBRepositoryManager.registryPackageSubmissionSetUniqueIdIdentificationScheme = (props.getProperty("XdsBRegistryPackageSubmissionSetUniqueIdIdentificationScheme") == null) ? XdsBRepositoryManager.registryPackageSubmissionSetUniqueIdIdentificationScheme
					: props.getProperty("XdsBRegistryPackageSubmissionSetUniqueIdIdentificationScheme");
			XdsBRepositoryManager.registryPackageSubmissionSetSourceIdIdentificationScheme = (props.getProperty("XdsBRegistryPackageSubmissionSetSourceIdIdentificationScheme") == null) ? XdsBRepositoryManager.registryPackageSubmissionSetSourceIdIdentificationScheme
					: props.getProperty("XdsBRegistryPackageSubmissionSetSourceIdIdentificationScheme");
			XdsBRepositoryManager.registryPackageSubmissionSetPatientIdIdentificationScheme = (props.getProperty("XdsBRegistryPackageSubmissionSetPatientIdIdentificationScheme") == null) ? XdsBRepositoryManager.registryPackageSubmissionSetPatientIdIdentificationScheme
					: props.getProperty("XdsBRegistryPackageSubmissionSetPatientIdIdentificationScheme");
			XdsBRepositoryManager.registryObjectListSubmissionSetClassificationNode = (props.getProperty("XdsBRegistryObjectListSubmissionSetClassificationNode") == null) ? XdsBRepositoryManager.registryObjectListSubmissionSetClassificationNode
					: props.getProperty("XdsBRegistryObjectListSubmissionSetClassificationNode");

			XdsBRepositoryManager.externalIdentifierSubmissionSetUniqueIdName = (props.getProperty("XdsBExternalIdentifierSubmissionSetUniqueIdName") == null) ? XdsBRepositoryManager.externalIdentifierSubmissionSetUniqueIdName
					: props.getProperty("XdsBExternalIdentifierSubmissionSetUniqueIdName");
			XdsBRepositoryManager.externalIdentifierSubmissionSetSourceIdName = (props.getProperty("XdsBExternalIdentifierSubmissionSetSourceIdName") == null) ? XdsBRepositoryManager.externalIdentifierSubmissionSetSourceIdName
					: props.getProperty("XdsBExternalIdentifierSubmissionSetSourceIdName");
			XdsBRepositoryManager.externalIdentifierSubmissionSetPatientIdName = (props.getProperty("XdsBExternalIdentifierSubmissionSetPatientIdName") == null) ? XdsBRepositoryManager.externalIdentifierSubmissionSetPatientIdName
					: props.getProperty("XdsBExternalIdentifierSubmissionSetPatientIdName");

			XdsBRepositoryManager.submissionSetOid = (props.getProperty("XdsBSubmissionSetOid") == null) ? XdsBRepositoryManager.submissionSetOid
					: props.getProperty("XdsBSubmissionSetOid");

			// If endpoint URI's are null, then set to the values in the properties file...
			if (registryEndpointURI == null)
			{
				registryEndpointURI = props.getProperty("XdsBRegistryEndpointURI");
			}
			
			if (repositoryEndpointURI == null)
			{
				repositoryEndpointURI = props.getProperty("XdsBRepositoryEndpointURI");
			}
			
			XdsBRepositoryManager.repositoryEndpointURI = repositoryEndpointURI;

			// If Syslog server host is specified, then configure...
			String syslogServerHost = props.getProperty("SyslogServerHost");
			int syslogServerPort = (props.getProperty("SyslogServerPort") != null) ? Integer.parseInt(props.getProperty("SyslogServerPort"))
					: -1;
			if ((syslogServerHost != null) &&
				(syslogServerPort > -1))
			{
				iti41AuditMsgTemplate = props.getProperty("Iti41AuditMsgTemplate");
				if (iti41AuditMsgTemplate == null)
				{
					log.error("ITI-41 audit message template not specified in properties file, "
							+ propertiesFile);
					throw new UnexpectedServerException("ITI-41 audit message template not specified in properties file, "
							+ propertiesFile);
				}

				// TCP over SSL (secure) syslog
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

				sysLogConfig = new SSLTCPNetSyslogConfig();
				sysLogConfig.setHost(syslogServerHost);
				sysLogConfig.setPort(syslogServerPort);
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

	private void logIti41AuditMsg(String submissionSetId,
			String patientId) throws IOException
	{
		if (sysLogConfig == null)
		{
			return;
		}

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
				XdsBRepositoryManager.repositoryEndpointURI);
		
		logMsg = logMsg.replace("$DestinationUserId$",
				"IExHub");
		
		logMsg = logMsg.replace("$PatientId$",
				patientId);

		logMsg = logMsg.replace("$SubmissionSetId$",
				submissionSetId);

		// Log the syslog message
		Syslog.getInstance("sslTcp").info(logMsg);
	}

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
			XPath xPath = XPathFactory.newInstance().newXPath();
			NodeList nodes = (NodeList)xPath.evaluate("/ClinicalDocument/effectiveTime",
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
			xPath = XPathFactory.newInstance().newXPath();
			nodes = (NodeList)xPath.evaluate("/ClinicalDocument/languageCode",
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
			xPath = XPathFactory.newInstance().newXPath();
			nodes = (NodeList)xPath.evaluate("/ClinicalDocument/documentationOf/serviceEvent/effectiveTime/low",
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
			xPath = XPathFactory.newInstance().newXPath();
			nodes = (NodeList)xPath.evaluate("/ClinicalDocument/documentationOf/serviceEvent/effectiveTime/high",
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
			xPath = XPathFactory.newInstance().newXPath();
			nodes = (NodeList)xPath.evaluate("/ClinicalDocument/recordTarget/patientRole/id",
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
			xPath = XPathFactory.newInstance().newXPath();
			nodes = (NodeList)xPath.evaluate("/ClinicalDocument/recordTarget/patientRole/patient/name/family",
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
			    
				xPath = XPathFactory.newInstance().newXPath();
				nodes = (NodeList)xPath.evaluate("/ClinicalDocument/recordTarget/patientRole/patient/name/given",
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
				
				xPath = XPathFactory.newInstance().newXPath();
				nodes = (NodeList)xPath.evaluate("/ClinicalDocument/recordTarget/patientRole/patient/name/prefix",
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

				xPath = XPathFactory.newInstance().newXPath();
				nodes = (NodeList)xPath.evaluate("/ClinicalDocument/recordTarget/patientRole/patient/name/suffix",
				        doc.getDocumentElement(),
				        XPathConstants.NODESET);
				if (nodes.getLength() > 0)
				{
					name.append(((Element)nodes.item(0)).getTextContent());
				}

				valueList.getValue().add(name.toString());

				xPath = XPathFactory.newInstance().newXPath();
				nodes = (NodeList)xPath.evaluate("/ClinicalDocument/recordTarget/patientRole/patient/birthTime",
				        doc.getDocumentElement(),
				        XPathConstants.NODESET);
				if (nodes.getLength() > 0)
				{
				    valueList.getValue().add("PID-7|"
				    		+ ((Element)nodes.item(0)).getAttribute("value"));
				}

				xPath = XPathFactory.newInstance().newXPath();
				nodes = (NodeList)xPath.evaluate("/ClinicalDocument/recordTarget/patientRole/patient/administrativeGenderCode",
				        doc.getDocumentElement(),
				        XPathConstants.NODESET);
				if (nodes.getLength() > 0)
				{
				    valueList.getValue().add("PID-8|"
				    		+ ((Element)nodes.item(0)).getAttribute("code"));
				}

				xPath = XPathFactory.newInstance().newXPath();
				nodes = (NodeList)xPath.evaluate("/ClinicalDocument/recordTarget/patientRole/addr",
				        doc.getDocumentElement(),
				        XPathConstants.NODESET);
				if (nodes.getLength() > 0)
				{
					StringBuilder address = new StringBuilder();
					address.append("PID-11|");
					
					xPath = XPathFactory.newInstance().newXPath();
					nodes = (NodeList)xPath.evaluate("/ClinicalDocument/recordTarget/patientRole/addr/streetAddressLine",
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
					
					xPath = XPathFactory.newInstance().newXPath();
					nodes = (NodeList)xPath.evaluate("/ClinicalDocument/recordTarget/patientRole/addr/city",
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

					xPath = XPathFactory.newInstance().newXPath();
					nodes = (NodeList)xPath.evaluate("/ClinicalDocument/recordTarget/patientRole/addr/state",
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

					xPath = XPathFactory.newInstance().newXPath();
					nodes = (NodeList)xPath.evaluate("/ClinicalDocument/recordTarget/patientRole/addr/postalCode",
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

					xPath = XPathFactory.newInstance().newXPath();
					nodes = (NodeList)xPath.evaluate("/ClinicalDocument/recordTarget/patientRole/addr/country",
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
			
			// Create rim:Name if necessary...
//			xPath = XPathFactory.newInstance().newXPath();
//			nodes = (NodeList)xPath.evaluate("/ClinicalDocument/title",
//			        doc.getDocumentElement(),
//			        XPathConstants.NODESET);
//			if (nodes.getLength() > 0)
//			{
//				InternationalStringType docName = new InternationalStringType();
//				LocalizedStringType localizedName = new LocalizedStringType();
//				localizedName.setValue(((Element)nodes.item(0)).getNodeValue());
//				docName.getLocalizedString().add(localizedName);
//				extrinsicObject.setName(docName);
//			}

			// Create rim:Description if necessary...
//			xPath = XPathFactory.newInstance().newXPath();
//			nodes = (NodeList)xPath.evaluate("/ClinicalDocument/comments",
//			        doc.getDocumentElement(),
//			        XPathConstants.NODESET);
//			if (nodes.getLength() > 0)
//			{
//				InternationalStringType docComments = new InternationalStringType();
//				LocalizedStringType localizedComments = new LocalizedStringType();
//				localizedComments.setValue(((Element)nodes.item(0)).getTextContent());
//				docComments.getLocalizedString().add(localizedComments);
//				extrinsicObject.setDescription(docComments);
//			}

			// Create classifications - start with document author(s)...
			ArrayList<ClassificationType> documentAuthorClassifications = new ArrayList<ClassificationType>();
			xPath = XPathFactory.newInstance().newXPath();
			nodes = (NodeList)xPath.evaluate("/ClinicalDocument/author",
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
					xPath = XPathFactory.newInstance().newXPath();
					NodeList subNodes = (NodeList)xPath.evaluate("assignedAuthor/assignedPerson",
							nodes.item(i).getChildNodes(),
							XPathConstants.NODESET);
					if (subNodes.getLength() > 0)
					{
						xPath = XPathFactory.newInstance().newXPath();
						NodeList assignedPersonSubNodes = (NodeList)xPath.evaluate("name/prefix",
								subNodes.item(0).getChildNodes(),
								XPathConstants.NODESET);
						if (assignedPersonSubNodes.getLength() > 0)
						{
							authorName.append(((Element)assignedPersonSubNodes.item(0)).getTextContent()
									+ " ");
						}

						xPath = XPathFactory.newInstance().newXPath();
						assignedPersonSubNodes = (NodeList)xPath.evaluate("name/given",
								subNodes.item(0).getChildNodes(),
						        XPathConstants.NODESET);
						if (assignedPersonSubNodes.getLength() > 0)
						{
							authorName.append(((Element)assignedPersonSubNodes.item(0)).getTextContent()
									+ " ");
						}
	
						xPath = XPathFactory.newInstance().newXPath();
						assignedPersonSubNodes = (NodeList)xPath.evaluate("name/family",
								subNodes.item(0).getChildNodes(),
						        XPathConstants.NODESET);
						if (assignedPersonSubNodes.getLength() > 0)
						{
							authorName.append(((Element)assignedPersonSubNodes.item(0)).getTextContent());
						}
	
						xPath = XPathFactory.newInstance().newXPath();
						assignedPersonSubNodes = (NodeList)xPath.evaluate("name/suffix",
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
						subNodes = (NodeList)xPath.evaluate("assignedAuthor/representedOrganization/name",
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
			xPath = XPathFactory.newInstance().newXPath();
			nodes = (NodeList)xPath.evaluate("/ClinicalDocument/code",
			        doc.getDocumentElement(),
			        XPathConstants.NODESET);
			if (nodes.getLength() > 0)
			{
				classification.setNodeRepresentation("*");
				
				slot = new SlotType1();
				slot.setName("codingScheme");
				
				if (((Element)nodes.item(0)).getAttribute("codeSystem") != null)
				{
					valueList = new ValueListType();
				    valueList.getValue().add(/*((Element)nodes.item(0)).getAttribute("codeSystem")*/ "1.3.6.1.4.1.21367.100.1");
					slot.setValueList(valueList);
					classification.getSlot().add(slot);
				}
				
				if (((Element)nodes.item(0)).getAttribute("displayName") != null)
				{
					InternationalStringType text = new InternationalStringType();
					LocalizedStringType localizedText = new LocalizedStringType();
					localizedText.setValue(((Element)nodes.item(0)).getAttribute("codeSystem"));
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
			xPath = XPathFactory.newInstance().newXPath();
			nodes = (NodeList)xPath.evaluate("/ClinicalDocument/confidentialityCode",
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
			xPath = XPathFactory.newInstance().newXPath();
			nodes = (NodeList)xPath.evaluate("/ClinicalDocument/code",
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
			xPath = XPathFactory.newInstance().newXPath();
			nodes = (NodeList)xPath.evaluate("/ClinicalDocument/code",
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
			xPath = XPathFactory.newInstance().newXPath();
			nodes = (NodeList)xPath.evaluate("/ClinicalDocument/recordTarget/patientRole/id",
			        doc.getDocumentElement(),
			        XPathConstants.NODESET);
			if (nodes.getLength() > 0)
			{
//				for (int i = 0; i < nodes.getLength(); ++i)
//				{
//					ExternalIdentifierType externalIdentifierPatientId = new ExternalIdentifierType();
//					externalIdentifierPatientId.setId(UUID.randomUUID().toString());
//					externalIdentifierPatientId.setRegistryObject(documentId);
//					externalIdentifierPatientId.setIdentificationScheme(ExtrinsicObjectExternalIdentifierPatientIdIdentificationScheme);
//					externalIdentifierPatientId.setValue(((Element)nodes.item(i)).getAttribute("extension")
//							+ "^^^&amp;"
//							+ ((Element)nodes.item(i)).getAttribute("root")
//							+ "&amp;ISO");
//					text = new InternationalStringType();
//					localizedText = new LocalizedStringType();
//					localizedText.setValue(ExtrinsicObjectExternalIdentifierPatientIdName);
//					text.getLocalizedString().add(localizedText);
//					externalIdentifierPatientId.setName(text);
//					extrinsicObject.getExternalIdentifier().add(externalIdentifierPatientId);
//				}
				
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
			xPath = XPathFactory.newInstance().newXPath();
			nodes = (NodeList)xPath.evaluate("/ClinicalDocument/id",
			        doc.getDocumentElement(),
			        XPathConstants.NODESET);
			if (nodes.getLength() > 0)
			{
//				for (int i = 0; i < nodes.getLength(); ++i)
//				{
//					ExternalIdentifierType externalIdentifierPatientId = new ExternalIdentifierType();
//					externalIdentifierPatientId.setId(UUID.randomUUID().toString());
//					externalIdentifierPatientId.setRegistryObject(documentId);
//					externalIdentifierPatientId.setIdentificationScheme(ExtrinsicObjectExternalIdentifierUniqueIdIdentificationScheme);
//					externalIdentifierPatientId.setValue(((Element)nodes.item(i)).getAttribute("root"));
//					text = new InternationalStringType();
//					localizedText = new LocalizedStringType();
//					localizedText.setValue(ExtrinsicObjectExternalIdentifierUniqueIdName);
//					text.getLocalizedString().add(localizedText);
//					externalIdentifierPatientId.setName(text);
//					extrinsicObject.getExternalIdentifier().add(externalIdentifierPatientId);
//				}
				
				ExternalIdentifierType externalIdentifierPatientId = new ExternalIdentifierType();
				externalIdentifierPatientId.setId(UUID.randomUUID().toString());
				externalIdentifierPatientId.setRegistryObject(documentId);
				externalIdentifierPatientId.setIdentificationScheme(extrinsicObjectExternalIdentifierUniqueIdIdentificationScheme);
				
				if (testMode)
				{
					DateTime testDocId = DateTime.now(DateTimeZone.UTC);
					externalIdentifierPatientId.setValue(((Element)nodes.item(0)).getAttribute("root")
							+ "."
							+ testDocId.getMillis());
				}
				else
				{
					externalIdentifierPatientId.setValue(((Element)nodes.item(0)).getAttribute("root"));
				}
				
				text = new InternationalStringType();
				localizedText = new LocalizedStringType();
				localizedText.setValue(extrinsicObjectExternalIdentifierUniqueIdName);
				text.getLocalizedString().add(localizedText);
				externalIdentifierPatientId.setName(text);
				extrinsicObject.getExternalIdentifier().add(externalIdentifierPatientId);
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
			xPath = XPathFactory.newInstance().newXPath();
			nodes = (NodeList)xPath.evaluate("/ClinicalDocument/code",
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
			submissionSetAssociation.setId(/*UUID.randomUUID().toString()*/ "as01");
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
			XdsBDocumentRepository.src.ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType.Document documentForMessage = new XdsBDocumentRepository.src.ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType.Document();
			documentForMessage.setValue(cdaDocument);
			documentForMessage.setId(documentId);
			documentSetRequest.getDocument().add(documentForMessage);
			
			logIti41AuditMsg(submissionSetId,
					patientId);

			return repositoryStub.documentRepository_ProvideAndRegisterDocumentSetB(documentSetRequest);
		}
		catch (Exception e)
		{
			throw e;
		}
	}
}