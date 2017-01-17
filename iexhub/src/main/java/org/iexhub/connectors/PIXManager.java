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

import PIXManager.org.hl7.v3.*;
import PIXManager.org.iexhub.services.client.PIXManager_ServiceStub;
import ca.uhn.fhir.model.dstu2.composite.*;
import ca.uhn.fhir.model.dstu2.resource.Organization;
import ca.uhn.fhir.model.dstu2.resource.Organization.Contact;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.dstu2.valueset.IdentifierUseEnum;
import ca.uhn.fhir.model.primitive.StringDt;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.iexhub.config.IExHubConfig;
import org.iexhub.exceptions.FamilyNameParamMissingException;
import org.iexhub.exceptions.PatientIdParamMissingException;
import org.iexhub.exceptions.UnexpectedServerException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.productivity.java.syslog4j.Syslog;
import org.productivity.java.syslog4j.impl.net.tcp.ssl.SSLTCPNetSyslogConfig;

import javax.xml.bind.JAXBElement;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;


/**
 * PIX Add Manager
 * @author A. Sute
 *
 */

public class PIXManager
{
    private static String keyStoreFile = IExHubConfig.getConfigLocationPath("1264.jks");
	private static String keyStorePwd = "IEXhub";
	private static String cipherSuites = "TLS_RSA_WITH_AES_128_CBC_SHA";
	private static String httpsProtocols = "TLSv1";
	private static boolean debugSsl = false;
	
	private static boolean logPixRequestMessages = false;
	private static boolean logPixResponseMessages = false;
	private static String logOutputPath = "/java/iexhub/logs";
	private static boolean logSyslogAuditMsgsLocally = false;

	private static String iti44AuditMsgTemplate = null;
	private static String iti45AuditMsgTemplate = null;
	private static SSLTCPNetSyslogConfig sysLogConfig = null;

    /** Logger */
    public static Logger log = Logger.getLogger(PIXManager.class);

	private static String endpointUri = null;
    private static String receiverApplicationName = "2.16.840.1.113883.3.72.6.5.100.556";
    private static String receiverApplicationRepresentedOrganization = "2.16.840.1.113883.3.72.6.1";
	private static String providerOrganizationName = "HIE Portal";
	private static String providerOrganizationContactTelecom = "555-555-5555";
	private static String providerOrganizationOid = "1.2.840.114350.1.13.99998.8734";
	private static String queryIdOid = "1.2.840.114350.1.13.28.1.18.5.999";
	private static String dataSourceOid = "2.16.840.1.113883.3.72.5.9.3";
	private static String iExHubDomainOid = "2.16.840.1.113883.3.72.5.9.1";
	private static String iExHubAssigningAuthority = "ISO";
	private static String iExHubSenderDeviceId = "1.3.6.1.4.1.21367.13.10.215";
	private static String patientIdAssigningAuthority = "1.3.6.1.4.1.21367.13.20.200";
	private static String uriPrefix = "urn:oid:";

	private static PIXManager_ServiceStub pixManagerStub = null;
	private static final ObjectFactory objectFactory = new ObjectFactory();

	public PIXManager(String endpointURI) throws AxisFault, Exception
	{
		this(endpointURI,
				false);
	}
	
	public PIXManager(String endpointURI,
			boolean enableTLS) throws AxisFault, Exception
	{
		PIXManager.logSyslogAuditMsgsLocally = IExHubConfig.getProperty("LogSyslogAuditMsgsLocally", PIXManager.logSyslogAuditMsgsLocally);
		PIXManager.iExHubSenderDeviceId = IExHubConfig.getProperty("IExHubSenderDeviceID", PIXManager.iExHubSenderDeviceId);
		PIXManager.patientIdAssigningAuthority = IExHubConfig.getProperty("PatientIDAssigningAuthority", PIXManager.patientIdAssigningAuthority);
		PIXManager.logOutputPath = IExHubConfig.getProperty("LogOutputPath", PIXManager.logOutputPath);
		PIXManager.logPixRequestMessages = IExHubConfig.getProperty("LogPIXRequestMessages", PIXManager.logPixRequestMessages);
		PIXManager.logPixResponseMessages = IExHubConfig.getProperty("LogPIXResponseMessages", PIXManager.logPixResponseMessages);
		PIXManager.keyStoreFile = IExHubConfig.getProperty("PIXKeyStoreFile", PIXManager.keyStoreFile);
		PIXManager.keyStorePwd = IExHubConfig.getProperty("PIXKeyStorePwd", PIXManager.keyStorePwd);
		PIXManager.cipherSuites = IExHubConfig.getProperty("PIXCipherSuites", PIXManager.cipherSuites);
		PIXManager.httpsProtocols = IExHubConfig.getProperty("PIXHttpsProtocols", PIXManager.httpsProtocols);
		PIXManager.debugSsl = IExHubConfig.getProperty("DebugSSL", PIXManager.debugSsl);
		PIXManager.receiverApplicationName = IExHubConfig.getProperty("PIXReceiverApplicationName", PIXManager.receiverApplicationName);
		PIXManager.receiverApplicationRepresentedOrganization = IExHubConfig.getProperty("PIXReceiverApplicationRepresentedOrganization", PIXManager.receiverApplicationRepresentedOrganization);
		PIXManager.providerOrganizationName = IExHubConfig.getProperty("PIXProviderOrganizationName", PIXManager.providerOrganizationName);
		PIXManager.providerOrganizationContactTelecom = IExHubConfig.getProperty("PIXProviderOrganizationContactTelecom", PIXManager.providerOrganizationContactTelecom);
		PIXManager.providerOrganizationOid = IExHubConfig.getProperty("PIXProviderOrganizationOID", PIXManager.providerOrganizationOid);
		PIXManager.queryIdOid = IExHubConfig.getProperty("PIXQueryIdOID", PIXManager.queryIdOid);
		PIXManager.dataSourceOid = IExHubConfig.getProperty("PIXDataSourceOID", PIXManager.dataSourceOid);
		PIXManager.iExHubDomainOid = IExHubConfig.getProperty("IExHubDomainOID", PIXManager.iExHubDomainOid);
		PIXManager.iExHubAssigningAuthority = IExHubConfig.getProperty("IExHubAssigningAuthority", PIXManager.iExHubAssigningAuthority);

		// If endpoint URI's are null, then set to the values in the properties file...
		if (endpointURI == null)
		{
			endpointURI = IExHubConfig.getProperty("PIXManagerEndpointURI");
		}

		PIXManager.endpointUri = endpointURI;

		// If Syslog server host is specified, then configure...
		iti44AuditMsgTemplate = IExHubConfig.getProperty("Iti44AuditMsgTemplate");
		iti45AuditMsgTemplate = IExHubConfig.getProperty("Iti45AuditMsgTemplate");
		String syslogServerHost = IExHubConfig.getProperty("SyslogServerHost");
		int syslogServerPort = IExHubConfig.getProperty("SyslogServerPort", -1);
		if ((syslogServerHost != null) &&
			(syslogServerHost.length() > 0) &&
			(syslogServerPort > -1))
		{
			if ((iti44AuditMsgTemplate == null) ||
				(iti45AuditMsgTemplate == null))
			{
				log.error("ITI-44 and/or ITI-45 audit message templates not specified in properties file, "
						+ IExHubConfig.CONFIG_FILE);
				throw new UnexpectedServerException("ITI-44 and/or ITI-45 audit message templates not specified in properties file, "
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
			// Instantiate PIXManager client stub and enable WS-Addressing...
			pixManagerStub = new PIXManager_ServiceStub(endpointURI);
			pixManagerStub._getServiceClient().engageModule("addressing");

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
				
				pixManagerStub._getServiceClient().engageModule("rampart");
			}

			log.info("PIXManager connector successfully initialized, endpointURI="
					+ endpointURI);
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	/**
	 * @param patientId
	 * @throws IOException
	 */
	private void logIti44AuditMsg(String patientId) throws IOException
	{
		String logMsg = FileUtils.readFileToString(new File(iti44AuditMsgTemplate));
		
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
				PIXManager.endpointUri);
		
		logMsg = logMsg.replace("$DestinationUserId$",
				"IExHub");
		
		logMsg = logMsg.replace("$PatientIdMtom$",
				Base64.getEncoder().encodeToString(patientId.getBytes()));
		
		logMsg = logMsg.replace("$PatientId$",
				patientId);

		if (logSyslogAuditMsgsLocally)
		{
			log.info(logMsg);
		}

		if ((sysLogConfig == null) ||
            (iti44AuditMsgTemplate == null))
		{
			return;
		}

		// Log the syslog message and close connection
		Syslog.getInstance("sslTcp").info(logMsg);
		Syslog.getInstance("sslTcp").flush();
	}

	/**
	 * @param queryText
	 * @param patientId
	 * @throws IOException
	 */
	private void logIti45AuditMsg(String queryText,
			String patientId) throws IOException
	{
		String logMsg = FileUtils.readFileToString(new File(iti45AuditMsgTemplate));
		
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
				PIXManager.endpointUri);
		
		logMsg = logMsg.replace("$DestinationUserId$",
				"IExHub");
		
		// Query text must be Base64 encoded...
		logMsg = logMsg.replace("$PixQueryMtom$",
				Base64.getEncoder().encodeToString(queryText.getBytes()));
		
		logMsg = logMsg.replace("$PatientIdMtom$",
				Base64.getEncoder().encodeToString(patientId.getBytes()));
		
		logMsg = logMsg.replace("$PatientId$",
				patientId);

		if (logSyslogAuditMsgsLocally)
		{
			log.info(logMsg);
		}

		if ((sysLogConfig == null) ||
            (iti45AuditMsgTemplate == null))
		{
			return;
		}

		// Log the syslog message and close connection
		Syslog.getInstance("sslTcp").info(logMsg);
		Syslog.getInstance("sslTcp").flush();
	}

	/**
	 * @param patientId
	 * @param domainOID
	 * @param populateDataSource
	 * @return
	 * @throws IOException
	 */
	public PRPAIN201310UV02 patientRegistryGetIdentifiers(String patientId,
			String domainOID,
			boolean populateDataSource) throws IOException
	{
		if ((patientId == null) ||
			(patientId.length() == 0))
		{
			throw new PatientIdParamMissingException("PatientId parameter is required");
		}
		
		PRPAIN201309UV02 pRPA_IN201309UV02 = new PRPAIN201309UV02();
		
		// ITS version...
		pRPA_IN201309UV02.setITSVersion("XML_1.0");
		
		// ID...
		II messageId = new II();
		messageId.setRoot(iExHubDomainOid);
		messageId.setExtension(UUID.randomUUID().toString());
		pRPA_IN201309UV02.setId(messageId);
		
		// Creation time...
		DateTime dt = new DateTime(DateTimeZone.UTC);
		TS creationTime = new TS();
		StringBuilder creationTimeBuilder = new StringBuilder();
		creationTimeBuilder.append(dt.getYear());
		creationTimeBuilder.append((dt.getMonthOfYear() < 10) ? ("0" + dt.getMonthOfYear())
				: dt.getMonthOfYear());
		creationTimeBuilder.append((dt.getDayOfMonth() < 10) ? ("0" + dt.getDayOfMonth())
				: dt.getDayOfMonth());
		creationTimeBuilder.append((dt.getHourOfDay() < 10) ? ("0" + dt.getHourOfDay())
				: dt.getHourOfDay());
		creationTimeBuilder.append((dt.getMinuteOfHour() < 10) ? ("0" + dt.getMinuteOfHour())
				: dt.getMinuteOfHour());
		creationTimeBuilder.append((dt.getSecondOfMinute() < 10) ? ("0" + dt.getSecondOfMinute())
				: dt.getSecondOfMinute());
		creationTime.setValue(creationTimeBuilder.toString());
		pRPA_IN201309UV02.setCreationTime(creationTime);
		
		// Interaction ID...
		II interactionId = new II();
		interactionId.setRoot("2.16.840.1.113883.1.6");
		interactionId.setExtension("PRPA_IN201309UV02");
		pRPA_IN201309UV02.setInteractionId(interactionId);
		
		// Processing code...
		CS processingCode = new CS();
		processingCode.setCode("P");
		pRPA_IN201309UV02.setProcessingCode(processingCode);
		
		// Processing mode code...
		CS processingModeCode = new CS();
		processingModeCode.setCode("T");
		pRPA_IN201309UV02.setProcessingModeCode(processingModeCode);
		
		// Accept ack code...
		CS acceptAckCode = new CS();
		acceptAckCode.setCode("AL");
		pRPA_IN201309UV02.setAcceptAckCode(acceptAckCode);

		// Create receiver...
		MCCIMT000100UV01Receiver receiver = new MCCIMT000100UV01Receiver();
		receiver.setTypeCode(CommunicationFunctionType.RCV);
		MCCIMT000100UV01Device receiverDevice = new MCCIMT000100UV01Device();
		receiverDevice.setClassCode(EntityClassDevice.DEV);
		receiverDevice.setDeterminerCode("INSTANCE");
		II receiverDeviceId = new II();
		receiverDeviceId.setRoot(receiverApplicationName);
		receiverDevice.getId().add(receiverDeviceId);
		receiver.setDevice(receiverDevice);
		pRPA_IN201309UV02.getReceiver().add(receiver);
		
		// Create sender...
		MCCIMT000100UV01Sender sender = new MCCIMT000100UV01Sender();
		sender.setTypeCode(CommunicationFunctionType.SND);
		MCCIMT000100UV01Device senderDevice = new MCCIMT000100UV01Device();
		senderDevice.setClassCode(EntityClassDevice.DEV);
		senderDevice.setDeterminerCode("INSTANCE");
		II senderDeviceId = new II();
		senderDeviceId.setRoot(PIXManager.iExHubSenderDeviceId);
		senderDevice.getId().add(senderDeviceId);
		sender.setDevice(senderDevice);
		pRPA_IN201309UV02.setSender(sender);

		// Create AuthorOrPerformer...
		QUQIMT021001UV01AuthorOrPerformer authorOrPerformer = new QUQIMT021001UV01AuthorOrPerformer();
		authorOrPerformer.setTypeCode(XParticipationAuthorPerformer.fromValue("AUT"));
		COCTMT090100UV01AssignedPerson assignedPerson = new COCTMT090100UV01AssignedPerson();
		assignedPerson.setClassCode("ASSIGNED");
		II assignedPersonId = new II();
		assignedPersonId.setRoot(PIXManager.iExHubDomainOid);
		assignedPersonId.setExtension("IExHub");
		assignedPerson.getId().add(assignedPersonId);
		authorOrPerformer.setAssignedPerson(objectFactory.createQUQIMT021001UV01AuthorOrPerformerAssignedPerson(assignedPerson));
		
		// Create QueryByParameter...
		PRPAMT201307UV02QueryByParameter queryByParam = new PRPAMT201307UV02QueryByParameter();
		II queryId = new II();
		queryId.setRoot(queryIdOid);
		queryId.setExtension(UUID.randomUUID().toString());
		queryByParam.setQueryId(queryId);
		CS responsePriorityCode = new CS();
		responsePriorityCode.setCode("I");
		queryByParam.setResponsePriorityCode(responsePriorityCode);
		CS statusCode = new CS();
		statusCode.setCode("new");
		queryByParam.setStatusCode(statusCode);
		
		// Create ParameterList...
		PRPAMT201307UV02ParameterList paramList = new PRPAMT201307UV02ParameterList();

		if (populateDataSource)
		{
			// Create DataSource...
			PRPAMT201307UV02DataSource dataSource = new PRPAMT201307UV02DataSource();
			II dataSourceId = new II();
			dataSourceId.setRoot(dataSourceOid);
			dataSource.getValue().add(dataSourceId);
			ST dataSourceSemanticsText = new ST();
			dataSourceSemanticsText.getContent().add("DataSource.id");
			dataSource.setSemanticsText(dataSourceSemanticsText);
			paramList.getDataSource().add(dataSource);
		}

		// Create PatientIdentifier...
		PRPAMT201307UV02PatientIdentifier patientIdentifier = new PRPAMT201307UV02PatientIdentifier();
		II patientIdentifierId = new II();
		patientIdentifierId.setRoot(domainOID);
		patientIdentifierId.setExtension(patientId);
		patientIdentifier.getValue().add(patientIdentifierId);
		ST patientIdentifierSemanticsText = new ST();
		patientIdentifierSemanticsText.getContent().add("Patient.Id");
		patientIdentifier.setSemanticsText(patientIdentifierSemanticsText);
		paramList.getPatientIdentifier().add(patientIdentifier);
		queryByParam.setParameterList(paramList);

		// Create ControlActProcess...
		PRPAIN201309UV02QUQIMT021001UV01ControlActProcess controlAct = new PRPAIN201309UV02QUQIMT021001UV01ControlActProcess();
		CD controlActProcessCode = new CD();
		controlActProcessCode.setCode("PRPA_TE201309UV02");
		controlActProcessCode.setCodeSystem("2.16.840.1.113883.1.6");
		controlAct.setCode(controlActProcessCode);
		controlAct.setClassCode(ActClassControlAct.CACT);
		controlAct.setMoodCode(XActMoodIntentEvent.EVN);
		controlAct.getAuthorOrPerformer().add(authorOrPerformer);
		controlAct.setQueryByParameter(objectFactory.createPRPAIN201309UV02QUQIMT021001UV01ControlActProcessQueryByParameter(queryByParam));

		pRPA_IN201309UV02.setControlActProcess(controlAct);
		
		OMElement requestElement = pixManagerStub.toOM(pRPA_IN201309UV02, pixManagerStub.optimizeContent(
                new javax.xml.namespace.QName("urn:hl7-org:v3",
                    "PRPA_IN201301UV02")),
                new javax.xml.namespace.QName("urn:hl7-org:v3",
    				"PRPA_IN201309UV02"));
		String queryText = requestElement.toString();
		
		UUID logMsgId = null;
		if (logPixRequestMessages)
		{
			logMsgId = UUID.randomUUID();
			Files.write(Paths.get(logOutputPath + logMsgId.toString() + "_PIXGetIdentifiersRequest.xml"),
					requestElement.toString().getBytes());
		}

		logIti45AuditMsg(queryText,
				patientId + "^^^&" + domainOID + "&ISO");

		PRPAIN201310UV02 response = pixManagerStub.pIXManager_PRPA_IN201309UV02(pRPA_IN201309UV02);
		if (logPixResponseMessages)
		{
			OMElement responseElement = pixManagerStub.toOM(response, pixManagerStub.optimizeContent(
	                new javax.xml.namespace.QName("urn:hl7-org:v3",
	                    "PRPA_IN201310UV02")),
	                new javax.xml.namespace.QName("urn:hl7-org:v3",
	    				"PRPA_IN201310UV02"));
			Files.write(Paths.get(logOutputPath + ((logMsgId == null) ? UUID.randomUUID().toString() : logMsgId.toString()) + "_PIXGetIdentifiersResponse.xml"),
					responseElement.toString().getBytes());			
		}
		
		return response;
	}
	
	/**
	 * @param fhirName
	 * @return
	 */
	private PN populatePersonName(HumanNameDt fhirName)
	{
		EnFamily enFamily = null;
		if ((fhirName.getFamilyAsSingleString() != null) &&
			(fhirName.getFamilyAsSingleString().length() > 0))
		{
			enFamily = new EnFamily();
			enFamily.getContent().add(fhirName.getFamilyAsSingleString());
		}
		
		EnGiven enGiven = null;
		if ((fhirName.getGivenAsSingleString() != null) &&
			(fhirName.getGivenAsSingleString().length() > 0))
		{
			enGiven = new EnGiven();
			enGiven.getContent().add(fhirName.getGivenAsSingleString());
		}
		
		PN personName = new PN();
		personName.getContent().add(objectFactory.createENFamily(enFamily));
		
		if (enGiven != null)
		{
			personName.getContent().add(objectFactory.createENGiven(enGiven));
		}
		
		return personName;
	}
	
	/**
	 * @param fhirAddress
	 * @return
	 */
	private AD populatePatientAddress(AddressDt fhirAddress)
	{
		AD address = new AD();
		if ((fhirAddress.getLine() != null) &&
			(!fhirAddress.getLine().isEmpty()))
		{
			for (StringDt addressLine : fhirAddress.getLine())
			{
				AdxpStreetAddressLine streetAddressLine = new AdxpStreetAddressLine();
				streetAddressLine.getContent().add(addressLine.getValueAsString());
				address.getContent().add(objectFactory.createADStreetAddressLine(streetAddressLine));
			}
		}
		
		if ((fhirAddress.getCity() != null) &&
			(fhirAddress.getCity().length() > 0))
		{
			AdxpCity cityName = new AdxpCity();
			cityName.getContent().add(fhirAddress.getCity());
			address.getContent().add(objectFactory.createADCity(cityName));
		}

		if ((fhirAddress.getState() != null) &&
			(fhirAddress.getState().length() > 0))
		{
			AdxpState stateName = new AdxpState();
			stateName.getContent().add(fhirAddress.getState());
			address.getContent().add(objectFactory.createADState(stateName));
		}

		if ((fhirAddress.getPostalCode() != null) &&
			(fhirAddress.getPostalCode().length() > 0))
		{
			AdxpPostalCode postalCodeName = new AdxpPostalCode();
			postalCodeName.getContent().add(fhirAddress.getPostalCode());
			address.getContent().add(objectFactory.createADPostalCode(postalCodeName));
		}

		return address;
	}

	/**
	 * @param fhirPatientResource
	 * @return
	 * @throws IOException
	 */
	public MCCIIN000002UV01 registerPatient(Patient fhirPatientResource) throws IOException
	{
		String dateOfBirth = (fhirPatientResource.getBirthDate() != null) ? fhirPatientResource.getBirthDateElement().getValueAsString()
				: null;
		String gender = (fhirPatientResource.getGender() == null) ? ""
				: (fhirPatientResource.getGender().compareToIgnoreCase(AdministrativeGenderEnum.MALE.getCode()) == 0) ? "M"
						: ((fhirPatientResource.getGender().compareToIgnoreCase(AdministrativeGenderEnum.FEMALE.getCode()) == 0) ? "F"
								: ((fhirPatientResource.getGender().compareToIgnoreCase(AdministrativeGenderEnum.OTHER.getCode()) == 0) ? "UN"
										: ""));
		
		if ((fhirPatientResource.getName().get(0).getFamilyAsSingleString() == null) ||
			(fhirPatientResource.getName().get(0).getFamilyAsSingleString().length() == 0))
		{
			throw new FamilyNameParamMissingException("FamilyName parameter is required");
		}
		
		PRPAIN201301UV02 pRPA_IN201301UV02 = new PRPAIN201301UV02();
		
		// ITS version...
		pRPA_IN201301UV02.setITSVersion("XML_1.0");
		
		// ID...
		II messageId = new II();
		messageId.setRoot(iExHubDomainOid);
		messageId.setExtension(UUID.randomUUID().toString());
		pRPA_IN201301UV02.setId(messageId);
		
		// Creation time...
		DateTime dt = new DateTime(DateTimeZone.UTC);
		TS creationTime = new TS();
		StringBuilder creationTimeBuilder = new StringBuilder();
		creationTimeBuilder.append(dt.getYear());
		creationTimeBuilder.append((dt.getMonthOfYear() < 10) ? ("0" + dt.getMonthOfYear())
				: dt.getMonthOfYear());
		creationTimeBuilder.append((dt.getDayOfMonth() < 10) ? ("0" + dt.getDayOfMonth())
				: dt.getDayOfMonth());
		creationTimeBuilder.append((dt.getHourOfDay() < 10) ? ("0" + dt.getHourOfDay())
				: dt.getHourOfDay());
		creationTimeBuilder.append((dt.getMinuteOfHour() < 10) ? ("0" + dt.getMinuteOfHour())
				: dt.getMinuteOfHour());
		creationTimeBuilder.append((dt.getSecondOfMinute() < 10) ? ("0" + dt.getSecondOfMinute())
				: dt.getSecondOfMinute());
		creationTime.setValue(creationTimeBuilder.toString());
		pRPA_IN201301UV02.setCreationTime(creationTime);
		
		// Interaction ID...
		II interactionId = new II();
		interactionId.setRoot("2.16.840.1.113883.1.6");
		interactionId.setExtension("PRPA_IN201301UV02");
		pRPA_IN201301UV02.setInteractionId(interactionId);
		
		// Processing code...
		CS processingCode = new CS();
		processingCode.setCode("P");
		pRPA_IN201301UV02.setProcessingCode(processingCode);
		
		// Processing mode code...
		CS processingModeCode = new CS();
		processingModeCode.setCode("T");
		pRPA_IN201301UV02.setProcessingModeCode(processingModeCode);
		
		// Accept ack code...
		CS acceptAckCode = new CS();
		acceptAckCode.setCode("AL");
		pRPA_IN201301UV02.setAcceptAckCode(acceptAckCode);
		
		// Create receiver...
		MCCIMT000100UV01Receiver receiver = new MCCIMT000100UV01Receiver();
		receiver.setTypeCode(CommunicationFunctionType.RCV);
		MCCIMT000100UV01Device receiverDevice = new MCCIMT000100UV01Device();
		receiverDevice.setClassCode(EntityClassDevice.DEV);
		receiverDevice.setDeterminerCode("INSTANCE");
		II receiverDeviceId = new II();
		receiverDeviceId.setRoot(receiverApplicationName);
		receiverDevice.getId().add(receiverDeviceId);
		MCCIMT000100UV01Agent asAgent = new MCCIMT000100UV01Agent();
		asAgent.getClassCode().add("AGNT");
		MCCIMT000100UV01Organization representedOrganization = new MCCIMT000100UV01Organization();
		representedOrganization.setDeterminerCode("INSTANCE");
		representedOrganization.setClassCode("ORG");
		II representedOrganizationId = new II();
		representedOrganizationId.setRoot(PIXManager.receiverApplicationRepresentedOrganization);
		representedOrganization.getId().add(representedOrganizationId);
		asAgent.setRepresentedOrganization(objectFactory.createMCCIMT000100UV01AgentRepresentedOrganization(representedOrganization));
		receiverDevice.setAsAgent(objectFactory.createMCCIMT000100UV01DeviceAsAgent(asAgent));
		receiver.setDevice(receiverDevice);
		pRPA_IN201301UV02.getReceiver().add(receiver);
		
		// Create sender...
		MCCIMT000100UV01Sender sender = new MCCIMT000100UV01Sender();
		sender.setTypeCode(CommunicationFunctionType.SND);
		MCCIMT000100UV01Device senderDevice = new MCCIMT000100UV01Device();
		senderDevice.setClassCode(EntityClassDevice.DEV);
		senderDevice.setDeterminerCode("INSTANCE");
		II senderDeviceId = new II();
		senderDeviceId.setRoot(PIXManager.iExHubSenderDeviceId);
		senderDevice.getId().add(senderDeviceId);
		MCCIMT000100UV01Agent senderAsAgent = new MCCIMT000100UV01Agent();
		senderAsAgent.getClassCode().add("AGNT");
		MCCIMT000100UV01Organization senderRepresentedOrganization = new MCCIMT000100UV01Organization();
		senderRepresentedOrganization.setDeterminerCode("INSTANCE");
		senderRepresentedOrganization.setClassCode("ORG");
		II senderRepresentedOrganizationId = new II();
		senderRepresentedOrganizationId.setRoot(PIXManager.iExHubDomainOid);
		senderRepresentedOrganization.getId().add(senderRepresentedOrganizationId);
		senderAsAgent.setRepresentedOrganization(objectFactory.createMCCIMT000100UV01AgentRepresentedOrganization(senderRepresentedOrganization));
		senderDevice.setAsAgent(objectFactory.createMCCIMT000100UV01DeviceAsAgent(senderAsAgent));
		sender.setDevice(senderDevice);
		pRPA_IN201301UV02.setSender(sender);
		
		PRPAIN201301UV02MFMIMT700701UV01Subject1 subject = new PRPAIN201301UV02MFMIMT700701UV01Subject1();
		
		// Create Registration Event...
		PRPAIN201301UV02MFMIMT700701UV01RegistrationEvent registrationEvent = new PRPAIN201301UV02MFMIMT700701UV01RegistrationEvent();
		registrationEvent.getClassCode().add("REG");
		registrationEvent.getMoodCode().add("EVN");
		registrationEvent.getNullFlavor().add("NA");
		CS statusCode = new CS();
		statusCode.setCode("active");
		registrationEvent.setStatusCode(statusCode);
		PRPAIN201301UV02MFMIMT700701UV01Subject2 subject1 = new PRPAIN201301UV02MFMIMT700701UV01Subject2();
		subject1.setTypeCode(ParticipationTargetSubject.SBJ);
		
		// Create Patient...
		PRPAMT201301UV02Patient patient = new PRPAMT201301UV02Patient();
		patient.getClassCode().add("PAT");
		CS patientStatusCode = new CS();
		patientStatusCode.setCode("active");
		patient.setStatusCode(patientStatusCode);
		
		// Create PatientPerson...
		PRPAMT201301UV02Person patientPerson = new PRPAMT201301UV02Person();
		
		// Other ID's specified...
		II constructedPatientId = null;
		if (fhirPatientResource.getIdentifier() != null)
		{
			for (IdentifierDt fhirId : fhirPatientResource.getIdentifier())
			{
				if ((fhirId.getUse() != null) &&
					(fhirId.getUse().equals(IdentifierUseEnum.OFFICIAL.getCode())))
				{
					// This is the official identifier
					constructedPatientId = new II();
					if ((fhirId.getSystem() == null) || (fhirId.getSystem().length() == 0))
					{
						throw new PatientIdParamMissingException("Patient ID system missing");
					}
					constructedPatientId.setRoot((fhirId.getSystem().toLowerCase().startsWith(uriPrefix)) ? fhirId.getSystem().replace(uriPrefix, "")
							: fhirId.getSystem());
					constructedPatientId.setExtension(fhirId.getValue());
					constructedPatientId.setAssigningAuthorityName(PIXManager.iExHubAssigningAuthority);
					patient.getId().add(constructedPatientId);
				}
				else
				{
					PRPAMT201301UV02OtherIDs asOtherId = new PRPAMT201301UV02OtherIDs();
					asOtherId.getClassCode().add("SD");
					II otherId = new II();
					otherId.setRoot(fhirId.getSystemElement().getValueAsString());
					otherId.setExtension(fhirId.getValue());
					asOtherId.getId().add(otherId);
	
					COCTMT150002UV01Organization scopingOrg = new COCTMT150002UV01Organization();
					scopingOrg.setClassCode("ORG");
					scopingOrg.setDeterminerCode("INSTANCE");
					II scopingOrgId = new II();
					scopingOrgId.setRoot(fhirId.getSystemElement().getValueAsString());
					scopingOrg.getId().add(scopingOrgId);
					asOtherId.setScopingOrganization(scopingOrg);
	
					patientPerson.getAsOtherIDs().add(asOtherId);
				}
			}
		}
		
		patientPerson.getName().add(populatePersonName(fhirPatientResource.getName().get(0)));
		
		if ((gender != null) &&
			(gender.length() > 0))
		{
			CE adminGenderCode = new CE();
			adminGenderCode.setCode(gender);
			adminGenderCode.setCodeSystem("2.16.840.1.113883.5.1");
			patientPerson.setAdministrativeGenderCode(adminGenderCode);
		}
		else
		{
			CE adminGenderCode = new CE();
			adminGenderCode.getNullFlavor().add("UNK");
			adminGenderCode.setCodeSystem("2.16.840.1.113883.5.1");
			patientPerson.setAdministrativeGenderCode(adminGenderCode);
		}

		patientPerson.getClassCode().add("PSN");
		patientPerson.setDeterminerCode("INSTANCE");
		
		if ((fhirPatientResource.getTelecom() != null) &&
			(!fhirPatientResource.getTelecom().isEmpty()))
		{
			for (ContactPointDt contactPoint : fhirPatientResource.getTelecom())
			{
				// Add if telecom value is present only
				if( contactPoint.getValue() != null
						&& !contactPoint.getValue().isEmpty()) {
					TEL contactPartyTelecom = new TEL();
					contactPartyTelecom.setValue(contactPoint.getValue());
					patientPerson.getTelecom().add(contactPartyTelecom);
				}
			}
		}
		
		if (dateOfBirth != null)
		{
			// Try several formats for date parsing...
			DateTimeFormatter formatter = null;
			DateTime birthDateTime = null;
			try
			{
				formatter = DateTimeFormat.forPattern("MM/dd/yyyy");
				birthDateTime = formatter.parseDateTime(dateOfBirth);
			}
			catch (Exception e)
			{
				try
				{
					formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
					birthDateTime = formatter.parseDateTime(dateOfBirth);
				}
				catch (Exception e2)
				{
					throw e2;
				}
			}

			StringBuilder birthDateBuilder = new StringBuilder();
			birthDateBuilder.append(birthDateTime.getYear());
			birthDateBuilder.append((birthDateTime.getMonthOfYear() < 10) ? ("0" + birthDateTime.getMonthOfYear())
					: birthDateTime.getMonthOfYear());
			birthDateBuilder.append((birthDateTime.getDayOfMonth() < 10) ? ("0" + birthDateTime.getDayOfMonth())
					: birthDateTime.getDayOfMonth());
			TS birthTime = new TS();
			birthTime.setValue(birthDateBuilder.toString());
			patientPerson.setBirthTime(birthTime);
		}
		
		JAXBElement<PRPAMT201301UV02Person> patientPersonElement = objectFactory.createPRPAMT201301UV02PatientPatientPerson(patientPerson);
		patient.setPatientPerson(patientPersonElement);
		
		// Create ProviderOrganization - set IExHub as provider if no careProvider specified in FHIR patient resource parameter...
		COCTMT150003UV03Organization providerOrganization = new COCTMT150003UV03Organization();
		providerOrganization.setDeterminerCode("INSTANCE");
		providerOrganization.setClassCode("ORG");
		
		if ((fhirPatientResource.getCareProvider() != null) &&
			(!fhirPatientResource.getCareProvider().isEmpty()))
		{
			for (ResourceReferenceDt resourceRef : fhirPatientResource.getCareProvider())
			{
				if (resourceRef.getResource().getClass() == Organization.class)
				{
					Organization careProvider = (Organization)resourceRef.getResource();
				
					// Provider ID
					II providerId = new II();
					providerId.setRoot((careProvider.getId().getValueAsString().startsWith("#")) ? careProvider.getId().getValueAsString().substring(1)
							: careProvider.getId().getValueAsString());
					providerOrganization.getId().add(providerId);
					
					// Provider name
					if ((careProvider.getName() != null) &&
						(careProvider.getName().length() > 0))
					{
						ON providerName = new ON();
						providerName.getContent().add(careProvider.getName());
						providerOrganization.getName().add(providerName);
					}
					
					// Create Contact Party if FHIR organization contacts are present...
					for (Contact fhirOrganizationContact : careProvider.getContact())
					{
						COCTMT150003UV03ContactParty contactParty = new COCTMT150003UV03ContactParty();
						contactParty.setClassCode(RoleClassContact.CON);
						
						// Contact telecom(s)
						if ((fhirOrganizationContact.getTelecom() != null) &&
							(!fhirOrganizationContact.getTelecom().isEmpty()))
						{
							for (ContactPointDt contactPoint : fhirOrganizationContact.getTelecom())
							{
								TEL contactPartyTelecom = new TEL();
								contactPartyTelecom.setValue(contactPoint.getValue());
								contactParty.getTelecom().add(contactPartyTelecom);
							}
						}
						
						// Contact name
						if ((fhirOrganizationContact.getName() != null) &&
						    (!fhirOrganizationContact.getName().isEmpty()))
						{
							COCTMT150003UV03Person contactPerson = new COCTMT150003UV03Person();
							contactPerson.getName().add(populatePersonName(fhirOrganizationContact.getName()));
							contactParty.setContactPerson(objectFactory.createCOCTMT150003UV03ContactPartyContactPerson(contactPerson));
						}
						
						// Contact address(es)
						if ((careProvider.getAddress() != null) &&
							(!careProvider.getAddress().isEmpty()))
						{
							for (AddressDt fhirAddr : careProvider.getAddress())
							{
								contactParty.getAddr().add(populatePatientAddress(fhirAddr));
							}
						}
						
						providerOrganization.getContactParty().add(contactParty);
					}
				}
			}
		}
		else
		{
			II providerId = new II();
			providerId.setRoot(providerOrganizationOid);
			providerOrganization.getId().add(providerId);
			ON providerName = new ON();
			providerName.getContent().add(providerOrganizationName);
			providerOrganization.getName().add(providerName);
			COCTMT150003UV03ContactParty contactParty = new COCTMT150003UV03ContactParty();
			contactParty.setClassCode(RoleClassContact.CON);
			TEL contactPartyTelecom = new TEL();
			contactPartyTelecom.setValue(providerOrganizationContactTelecom);
			contactParty.getTelecom().add(contactPartyTelecom);
			providerOrganization.getContactParty().add(contactParty);
		}
		
		patient.setProviderOrganization(providerOrganization);
		
		subject1.setPatient(patient);

		registrationEvent.setSubject1(subject1);
		
		// Create Custodian info...
		MFMIMT700701UV01Custodian custodian = new MFMIMT700701UV01Custodian();
		custodian.getTypeCode().add("CST");
		COCTMT090003UV01AssignedEntity assignedEntity = new COCTMT090003UV01AssignedEntity();
		assignedEntity.setClassCode("ASSIGNED");
		II assignedEntityId = new II();
		assignedEntityId.setRoot(iExHubDomainOid);
		assignedEntity.getId().add(assignedEntityId);
		COCTMT090003UV01Organization assignedOrganization = new COCTMT090003UV01Organization();
		assignedOrganization.setDeterminerCode("INSTANCE");
		assignedOrganization.setClassCode("ORG");
		EN organizationName = new EN();
		organizationName.getContent().add("IHE Portal");
		assignedOrganization.getName().add(organizationName);
		assignedEntity.setAssignedOrganization(objectFactory.createCOCTMT090003UV01AssignedEntityAssignedOrganization(assignedOrganization));
		custodian.setAssignedEntity(assignedEntity);
		registrationEvent.setCustodian(custodian);
		
		// Set Subject info...
		subject.getTypeCode().add("SUBJ");
		
		subject.setRegistrationEvent(registrationEvent);
		
		PRPAIN201301UV02MFMIMT700701UV01ControlActProcess controlAct = new PRPAIN201301UV02MFMIMT700701UV01ControlActProcess();
		CD controlActProcessCode = new CD();
		controlActProcessCode.setCode("PRPA_TE201301UV02");
		controlActProcessCode.setCodeSystem("2.16.840.1.113883.1.6");
		controlAct.setCode(controlActProcessCode);
		controlAct.setClassCode(ActClassControlAct.CACT);
		controlAct.setMoodCode(XActMoodIntentEvent.EVN);
		controlAct.getSubject().add(subject);
		
		pRPA_IN201301UV02.setControlActProcess(controlAct);

		logIti44AuditMsg(constructedPatientId.getExtension() + "^^^&" + constructedPatientId.getRoot() + "&" + PIXManager.iExHubAssigningAuthority);

		UUID logMsgId = null;
		if (logPixRequestMessages)
		{
			logMsgId = UUID.randomUUID();
			OMElement requestElement = pixManagerStub.toOM(pRPA_IN201301UV02, pixManagerStub.optimizeContent(
	                new javax.xml.namespace.QName("urn:hl7-org:v3",
	                    "PRPA_IN201301UV02")),
	                new javax.xml.namespace.QName("urn:hl7-org:v3",
	    				"PRPA_IN201301UV02"));
			Files.write(Paths.get(logOutputPath + logMsgId.toString() + "_PIXRegisterPatientRequest.xml"),
					requestElement.toString().getBytes());
		}

		MCCIIN000002UV01 response = pixManagerStub.pIXManager_PRPA_IN201301UV02(pRPA_IN201301UV02);
		if (logPixResponseMessages)
		{
			OMElement responseElement = pixManagerStub.toOM(response, pixManagerStub.optimizeContent(
	                new javax.xml.namespace.QName("urn:hl7-org:v3",
	                    "MCCI_IN000002UV01")),
	                new javax.xml.namespace.QName("urn:hl7-org:v3",
	    				"MCCI_IN000002UV01"));
			Files.write(Paths.get(logOutputPath + ((logMsgId == null) ? UUID.randomUUID().toString() : logMsgId.toString()) + "_PIXRegisterPatientResponse.xml"),
					responseElement.toString().getBytes());			
		}

		return response;
	}
	
	// Non-FHIR version of registerPatient()
	/**
	 * @param givenName
	 * @param familyName
	 * @param middleName
	 * @param dateOfBirth
	 * @param gender
	 * @param patientId
	 * @return
	 * @throws IOException
	 */
	public MCCIIN000002UV01 registerPatient(String givenName,
			String familyName,
			String middleName,
			String dateOfBirth,
			String gender,
			String patientId) throws IOException
	{
		if ((familyName == null) ||
			(familyName.length() == 0))
		{
			throw new FamilyNameParamMissingException("FamilyName parameter is required");
		}
		
		PRPAIN201301UV02 pRPA_IN201301UV02 = new PRPAIN201301UV02();
		
		// ITS version...
		pRPA_IN201301UV02.setITSVersion("XML_1.0");
		
		// ID...
		II messageId = new II();
		messageId.setRoot(iExHubDomainOid);
		messageId.setExtension(UUID.randomUUID().toString());
		pRPA_IN201301UV02.setId(messageId);
		
		// Creation time...
		DateTime dt = new DateTime(DateTimeZone.UTC);
		TS creationTime = new TS();
		StringBuilder creationTimeBuilder = new StringBuilder();
		creationTimeBuilder.append(dt.getYear());
		creationTimeBuilder.append((dt.getMonthOfYear() < 10) ? ("0" + dt.getMonthOfYear())
				: dt.getMonthOfYear());
		creationTimeBuilder.append((dt.getDayOfMonth() < 10) ? ("0" + dt.getDayOfMonth())
				: dt.getDayOfMonth());
		creationTimeBuilder.append((dt.getHourOfDay() < 10) ? ("0" + dt.getHourOfDay())
				: dt.getHourOfDay());
		creationTimeBuilder.append((dt.getMinuteOfHour() < 10) ? ("0" + dt.getMinuteOfHour())
				: dt.getMinuteOfHour());
		creationTimeBuilder.append((dt.getSecondOfMinute() < 10) ? ("0" + dt.getSecondOfMinute())
				: dt.getSecondOfMinute());
		creationTime.setValue(creationTimeBuilder.toString());
		pRPA_IN201301UV02.setCreationTime(creationTime);
		
		// Interaction ID...
		II interactionId = new II();
		interactionId.setRoot("2.16.840.1.113883.1.6");
		interactionId.setExtension("PRPA_IN201301UV02");
		pRPA_IN201301UV02.setInteractionId(interactionId);
		
		// Processing code...
		CS processingCode = new CS();
		processingCode.setCode("P");
		pRPA_IN201301UV02.setProcessingCode(processingCode);
		
		// Processing mode code...
		CS processingModeCode = new CS();
		processingModeCode.setCode("T");
		pRPA_IN201301UV02.setProcessingModeCode(processingModeCode);
		
		// Accept ack code...
		CS acceptAckCode = new CS();
		acceptAckCode.setCode("AL");
		pRPA_IN201301UV02.setAcceptAckCode(acceptAckCode);
		
		// Create receiver...
		MCCIMT000100UV01Receiver receiver = new MCCIMT000100UV01Receiver();
		receiver.setTypeCode(CommunicationFunctionType.RCV);
		MCCIMT000100UV01Device receiverDevice = new MCCIMT000100UV01Device();
		receiverDevice.setClassCode(EntityClassDevice.DEV);
		receiverDevice.setDeterminerCode("INSTANCE");
		II receiverDeviceId = new II();
		receiverDeviceId.setRoot(receiverApplicationName);
		receiverDevice.getId().add(receiverDeviceId);
		MCCIMT000100UV01Agent asAgent = new MCCIMT000100UV01Agent();
		asAgent.getClassCode().add("AGNT");
		MCCIMT000100UV01Organization representedOrganization = new MCCIMT000100UV01Organization();
		representedOrganization.setDeterminerCode("INSTANCE");
		representedOrganization.setClassCode("ORG");
		II representedOrganizationId = new II();
		representedOrganizationId.setRoot(PIXManager.receiverApplicationRepresentedOrganization);
		representedOrganization.getId().add(representedOrganizationId);
		asAgent.setRepresentedOrganization(objectFactory.createMCCIMT000100UV01AgentRepresentedOrganization(representedOrganization));
		receiverDevice.setAsAgent(objectFactory.createMCCIMT000100UV01DeviceAsAgent(asAgent));
		receiver.setDevice(receiverDevice);
		pRPA_IN201301UV02.getReceiver().add(receiver);
		
		// Create sender...
		MCCIMT000100UV01Sender sender = new MCCIMT000100UV01Sender();
		sender.setTypeCode(CommunicationFunctionType.SND);
		MCCIMT000100UV01Device senderDevice = new MCCIMT000100UV01Device();
		senderDevice.setClassCode(EntityClassDevice.DEV);
		senderDevice.setDeterminerCode("INSTANCE");
		II senderDeviceId = new II();
		senderDeviceId.setRoot(PIXManager.iExHubSenderDeviceId);
		senderDevice.getId().add(senderDeviceId);
		MCCIMT000100UV01Agent senderAsAgent = new MCCIMT000100UV01Agent();
		senderAsAgent.getClassCode().add("AGNT");
		MCCIMT000100UV01Organization senderRepresentedOrganization = new MCCIMT000100UV01Organization();
		senderRepresentedOrganization.setDeterminerCode("INSTANCE");
		senderRepresentedOrganization.setClassCode("ORG");
		II senderRepresentedOrganizationId = new II();
		senderRepresentedOrganizationId.setRoot(PIXManager.iExHubDomainOid);
		senderRepresentedOrganization.getId().add(senderRepresentedOrganizationId);
		senderAsAgent.setRepresentedOrganization(objectFactory.createMCCIMT000100UV01AgentRepresentedOrganization(senderRepresentedOrganization));
		senderDevice.setAsAgent(objectFactory.createMCCIMT000100UV01DeviceAsAgent(senderAsAgent));
		sender.setDevice(senderDevice);
		pRPA_IN201301UV02.setSender(sender);
		
		PRPAIN201301UV02MFMIMT700701UV01Subject1 subject = new PRPAIN201301UV02MFMIMT700701UV01Subject1();
		
		// Create Registration Event...
		PRPAIN201301UV02MFMIMT700701UV01RegistrationEvent registrationEvent = new PRPAIN201301UV02MFMIMT700701UV01RegistrationEvent();
		registrationEvent.getClassCode().add("REG");
		registrationEvent.getMoodCode().add("EVN");
		registrationEvent.getNullFlavor().add("NA");
		CS statusCode = new CS();
		statusCode.setCode("active");
		registrationEvent.setStatusCode(statusCode);
		PRPAIN201301UV02MFMIMT700701UV01Subject2 subject1 = new PRPAIN201301UV02MFMIMT700701UV01Subject2();
		subject1.setTypeCode(ParticipationTargetSubject.SBJ);
		
		// Create Patient...
		PRPAMT201301UV02Patient patient = new PRPAMT201301UV02Patient();
		patient.getClassCode().add("PAT");
		
		II constructedPatientId = new II();
		constructedPatientId.setRoot(PIXManager.patientIdAssigningAuthority);
		constructedPatientId.setExtension(UUID.randomUUID().toString());
		constructedPatientId.setAssigningAuthorityName(PIXManager.iExHubAssigningAuthority);
		patient.getId().add(constructedPatientId);
		
		CS patientStatusCode = new CS();
		patientStatusCode.setCode("active");
		patient.setStatusCode(patientStatusCode);
		
		// Create PatientPerson...
		PRPAMT201301UV02Person patientPerson = new PRPAMT201301UV02Person();
		
		EnFamily enFamily = null;
		if ((familyName != null) &&
			(familyName.length() > 0))
		{
			enFamily = new EnFamily();
			enFamily.getContent().add(familyName);
		}
		
		EnGiven enGiven = null;
		if ((givenName != null) &&
			(givenName.length() > 0))
		{
			enGiven = new EnGiven();
			
			if ((middleName != null) &&
				(middleName.length() > 0))
			{
				enGiven.getContent().add(givenName + " " + middleName);
			}
			else
			{
				enGiven.getContent().add(givenName);
			}
		}
		
		PN patientName = new PN();
		patientName.getContent().add(objectFactory.createENFamily(enFamily));
		
		if (enGiven != null)
		{
			patientName.getContent().add(objectFactory.createENGiven(enGiven));
		}
		
		patientPerson.getName().add(patientName);
		
		if ((gender != null) &&
			(gender.length() > 0))
		{
			CE adminGenderCode = new CE();
			adminGenderCode.setCode(gender);
			adminGenderCode.setCodeSystem("2.16.840.1.113883.5.1");
			patientPerson.setAdministrativeGenderCode(adminGenderCode);
		}
		else
		{
			CE adminGenderCode = new CE();
			adminGenderCode.getNullFlavor().add("UNK");
			adminGenderCode.setCodeSystem("2.16.840.1.113883.5.1");
			patientPerson.setAdministrativeGenderCode(adminGenderCode);
		}

		patientPerson.getClassCode().add("PSN");
		patientPerson.setDeterminerCode("INSTANCE");
		
		if (dateOfBirth != null)
		{
			// Try several formats for date parsing...
			DateTimeFormatter formatter = null;
			DateTime birthDateTime = null;
			try
			{
				formatter = DateTimeFormat.forPattern("MM/dd/yyyy");
				birthDateTime = formatter.parseDateTime(dateOfBirth);
			}
			catch (Exception e)
			{
				try
				{
					formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
					birthDateTime = formatter.parseDateTime(dateOfBirth);
				}
				catch (Exception e2)
				{
					throw e2;
				}
			}

			StringBuilder birthDateBuilder = new StringBuilder();
			birthDateBuilder.append(birthDateTime.getYear());
			birthDateBuilder.append((birthDateTime.getMonthOfYear() < 10) ? ("0" + birthDateTime.getMonthOfYear())
					: birthDateTime.getMonthOfYear());
			birthDateBuilder.append((birthDateTime.getDayOfMonth() < 10) ? ("0" + birthDateTime.getDayOfMonth())
					: birthDateTime.getDayOfMonth());
			TS birthTime = new TS();
			birthTime.setValue(birthDateBuilder.toString());
			patientPerson.setBirthTime(birthTime);
		}
		
		JAXBElement<PRPAMT201301UV02Person> patientPersonElement = objectFactory.createPRPAMT201301UV02PatientPatientPerson(patientPerson);
		patient.setPatientPerson(patientPersonElement);
		
		// Create ProviderOrganization...
		COCTMT150003UV03Organization providerOrganization = new COCTMT150003UV03Organization();
		providerOrganization.setDeterminerCode("INSTANCE");
		providerOrganization.setClassCode("ORG");
		II providerId = new II();
		providerId.setRoot(providerOrganizationOid);
		providerOrganization.getId().add(providerId);
		ON providerName = new ON();
		providerName.getContent().add(providerOrganizationName);
		providerOrganization.getName().add(providerName);
		COCTMT150003UV03ContactParty contactParty = new COCTMT150003UV03ContactParty();
		contactParty.setClassCode(RoleClassContact.CON);
		TEL contactPartyTelecom = new TEL();
		contactPartyTelecom.setValue(providerOrganizationContactTelecom);
		contactParty.getTelecom().add(contactPartyTelecom);
		providerOrganization.getContactParty().add(contactParty);
		
		patient.setProviderOrganization(providerOrganization);
		
		subject1.setPatient(patient);

		registrationEvent.setSubject1(subject1);
		
		// Create Custodian info...
		MFMIMT700701UV01Custodian custodian = new MFMIMT700701UV01Custodian();
		custodian.getTypeCode().add("CST");
		COCTMT090003UV01AssignedEntity assignedEntity = new COCTMT090003UV01AssignedEntity();
		assignedEntity.setClassCode("ASSIGNED");
		II assignedEntityId = new II();
		assignedEntityId.setRoot(iExHubDomainOid);
		assignedEntity.getId().add(assignedEntityId);
		COCTMT090003UV01Organization assignedOrganization = new COCTMT090003UV01Organization();
		assignedOrganization.setDeterminerCode("INSTANCE");
		assignedOrganization.setClassCode("ORG");
		EN organizationName = new EN();
		organizationName.getContent().add("IHE Portal");
		assignedOrganization.getName().add(organizationName);
		assignedEntity.setAssignedOrganization(objectFactory.createCOCTMT090003UV01AssignedEntityAssignedOrganization(assignedOrganization));
		custodian.setAssignedEntity(assignedEntity);
		registrationEvent.setCustodian(custodian);
		
		// Set Subject info...
		subject.getTypeCode().add("SUBJ");
		
		subject.setRegistrationEvent(registrationEvent);
		
		PRPAIN201301UV02MFMIMT700701UV01ControlActProcess controlAct = new PRPAIN201301UV02MFMIMT700701UV01ControlActProcess();
		CD controlActProcessCode = new CD();
		controlActProcessCode.setCode("PRPA_TE201301UV02");
		controlActProcessCode.setCodeSystem("2.16.840.1.113883.1.6");
		controlAct.setCode(controlActProcessCode);
		controlAct.setClassCode(ActClassControlAct.CACT);
		controlAct.setMoodCode(XActMoodIntentEvent.EVN);
		controlAct.getSubject().add(subject);
		
		pRPA_IN201301UV02.setControlActProcess(controlAct);

		logIti44AuditMsg(patientId + "^^^&" + PIXManager.iExHubDomainOid + "&" + PIXManager.iExHubAssigningAuthority);

		UUID logMsgId = null;
		if (logPixRequestMessages)
		{
			logMsgId = UUID.randomUUID();
			OMElement requestElement = pixManagerStub.toOM(pRPA_IN201301UV02, pixManagerStub.optimizeContent(
	                new javax.xml.namespace.QName("urn:hl7-org:v3",
	                    "PRPA_IN201301UV02")),
	                new javax.xml.namespace.QName("urn:hl7-org:v3",
	    				"PRPA_IN201301UV02"));
			Files.write(Paths.get(logOutputPath + logMsgId.toString() + "_PIXRegisterPatientRequest.xml"),
					requestElement.toString().getBytes());
		}

		MCCIIN000002UV01 response = pixManagerStub.pIXManager_PRPA_IN201301UV02(pRPA_IN201301UV02);
		if (logPixResponseMessages)
		{
			OMElement responseElement = pixManagerStub.toOM(response, pixManagerStub.optimizeContent(
	                new javax.xml.namespace.QName("urn:hl7-org:v3",
	                    "MCCI_IN000002UV01")),
	                new javax.xml.namespace.QName("urn:hl7-org:v3",
	    				"MCCI_IN000002UV01"));
			Files.write(Paths.get(logOutputPath + ((logMsgId == null) ? UUID.randomUUID().toString() : logMsgId.toString()) + "_PIXRegisterPatientResponse.xml"),
					responseElement.toString().getBytes());			
		}

		return response;
	}
}
