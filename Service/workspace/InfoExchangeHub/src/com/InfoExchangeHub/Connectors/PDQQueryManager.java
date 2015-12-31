package com.InfoExchangeHub.Connectors;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.util.Properties;
import java.util.UUID;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import PDQSupplier.src.com.InfoExchangeHub.Services.Client.PDQSupplier_ServiceStub;
import PDQSupplier.src.org.hl7.v3.*;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.productivity.java.syslog4j.Syslog;
import org.productivity.java.syslog4j.SyslogConfigIF;
import org.productivity.java.syslog4j.impl.net.tcp.ssl.SSLTCPNetSyslogConfig;

import com.InfoExchangeHub.Exceptions.UnexpectedServerException;

/**
 * @author A. Sute
 *
 */

public class PDQQueryManager
{
    private static String keyStoreFile = "c:/temp/1264.jks";
	private static String keyStorePwd = "IEXhub";
	private static String cipherSuites = "TLS_RSA_WITH_AES_128_CBC_SHA";
	private static String httpsProtocols = "TLSv1";
	private static boolean debugSSL = false;

	private static String iti47AuditMsgTemplate = null;
	private static SyslogConfigIF sysLogConfig = null;
	private static final String propertiesFile = "/temp/IExHub.properties";

	/** Logger */
    public static Logger log = Logger.getLogger(PDQQueryManager.class);

    private final static ObjectFactory factory = new ObjectFactory();

    private static String receiverApplicationName = "2.16.840.1.113883.3.72.6.5.100.556";
	private static String receiverTelecomValue = "http://servicelocation/PDQuery";
	private static String queryIdOID = "1.2.840.114350.1.13.28.1.18.5.999";
	private static String otherIDsScopingOrganizationOID = "2.16.840.1.113883.3.72.5.9.1";
	private static String receiverApplicationRepresentedOrganization = "2.16.840.1.113883.3.72.6.1";
	private static String iExHubDomainOID = "2.16.840.1.113883.3.72.5.9.1";

	private static PDQSupplier_ServiceStub pdqSupplierStub = null;
	private static String endpointURI = null;
	
	public PDQQueryManager(String endpointURI) throws AxisFault, Exception
	{
		Properties props = new Properties();
		try
		{
			props.load(new FileInputStream(propertiesFile));
			
			PDQQueryManager.keyStoreFile = (props.getProperty("PDQKeyStoreFile") == null) ? PDQQueryManager.keyStoreFile
					: props.getProperty("PDQKeyStoreFile");
			PDQQueryManager.keyStorePwd = (props.getProperty("PDQKeyStorePwd") == null) ? PDQQueryManager.keyStorePwd
					: props.getProperty("PIXKeyStorePwd");
			PDQQueryManager.cipherSuites = (props.getProperty("PIXCipherSuites") == null) ? PDQQueryManager.cipherSuites
					: props.getProperty("PIXCipherSuites");
			PDQQueryManager.httpsProtocols = (props.getProperty("PIXHttpsProtocols") == null) ? PDQQueryManager.httpsProtocols
					: props.getProperty("PIXHttpsProtocols");
			PDQQueryManager.debugSSL = (props.getProperty("DebugSSL") == null) ? PDQQueryManager.debugSSL
					: Boolean.parseBoolean(props.getProperty("DebugSSL"));
			PDQQueryManager.receiverApplicationName = (props.getProperty("PDQReceiverApplicationName") == null) ? PDQQueryManager.receiverApplicationName
					: props.getProperty("PDQReceiverApplicationName");
			PDQQueryManager.receiverTelecomValue = (props.getProperty("PDQReceiverTelecomValue") == null) ? PDQQueryManager.receiverTelecomValue
					: props.getProperty("PDQReceiverTelecomValue");
			PDQQueryManager.queryIdOID = (props.getProperty("PDQQueryIdOID") == null) ? PDQQueryManager.queryIdOID
					: props.getProperty("PDQQueryIdOID");
			PDQQueryManager.otherIDsScopingOrganizationOID = (props.getProperty("PDQOtherIDsScopingOrganizationOID") == null) ? PDQQueryManager.otherIDsScopingOrganizationOID
					: props.getProperty("PDQOtherIDsScopingOrganizationOID");
			PDQQueryManager.receiverApplicationRepresentedOrganization = (props.getProperty("PDQReceiverApplicationRepresentedOrganization") == null) ? PDQQueryManager.receiverApplicationRepresentedOrganization
					: props.getProperty("PDQReceiverApplicationRepresentedOrganization");
			PDQQueryManager.iExHubDomainOID = (props.getProperty("IExHubDomainOID") == null) ? PDQQueryManager.iExHubDomainOID
					: props.getProperty("IExHubDomainOID");

			// If endpoint URI's are null, then set to the values in the properties file...
			if (endpointURI == null)
			{
				endpointURI = props.getProperty("PIXManagerEndpointURI");
			}
			
			PDQQueryManager.endpointURI = endpointURI;

			// If Syslog server host is specified, then configure...
			String syslogServerHost = props.getProperty("SyslogServerHost");
			int syslogServerPort = (props.getProperty("SyslogServerPort") != null) ? Integer.parseInt(props.getProperty("SyslogServerPort"))
					: -1;
			if ((syslogServerHost != null) &&
				(syslogServerPort > -1))
			{
				iti47AuditMsgTemplate = props.getProperty("Iti47AuditMsgTemplate");
				if (iti47AuditMsgTemplate == null)
				{
					log.error("ITI-47 audit message templates not specified in properties file, "
							+ propertiesFile);
					throw new UnexpectedServerException("ITI-47 audit message templates not specified in properties file, "
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

			// Instantiate PDQSupplier client stub and enable WS-Addressing...
			pdqSupplierStub = new PDQSupplier_ServiceStub(endpointURI);
			pdqSupplierStub._getServiceClient().engageModule("addressing");
			
			log.info("PDQQueryManager connector successfully initialized, endpointURI="
					+ endpointURI);
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	private void logIti47AuditMsg(String queryText,
			String patientId) throws IOException
	{
		String logMsg = FileUtils.readFileToString(new File(iti47AuditMsgTemplate));
		
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
				PDQQueryManager.endpointURI );
		
		logMsg = logMsg.replace("$DestinationUserId$",
				"IExHub");
		
		logMsg = logMsg.replace("$PatientIdMtom$",
				Base64.encodeBase64String(patientId.getBytes()));

		logMsg = logMsg.replace("$QueryByParameterMtom$",
				Base64.encodeBase64String(queryText.getBytes()));

		logMsg = logMsg.replace("$PatientId$",
				patientId);
		
		// Log the syslog message
		Syslog.getInstance("sslTcp").info(logMsg);
	}

	public PRPAIN201306UV02 queryPatientDemographics(String givenName,
			String familyName,
			String middleName,
			String dateOfBirth,
			String gender,
			String motherMaidenName,
			String addressStreet,
			String addressCity,
			String addressState,
			String addressPostalCode,
			String patientId,
			String patientIdDomain,
			String otherIDsScopingOrganization) throws IOException
	{
		return queryPatientDemographics(givenName,
			familyName,
			middleName,
			dateOfBirth,
			gender,
			motherMaidenName,
			addressStreet,
			addressCity,
			addressState,
			addressPostalCode,
			patientId,
			patientIdDomain,
			otherIDsScopingOrganization,
			-1);
	}

	public void queryCancel(PRPAIN201306UV02 queryResult) throws RemoteException
	{
		QUQIIN000003UV01Type qUQIIN000003UV01 = new QUQIIN000003UV01Type();

		// ITS version...
		qUQIIN000003UV01.setITSVersion("XML_1.0");
		
		// ID...
		II messageId = new II();
		messageId.setRoot(UUID.randomUUID().toString());
		qUQIIN000003UV01.setId(messageId);
		
		// Creation time...
		setCreationTime(qUQIIN000003UV01);
		
		// Interaction ID...
		II interactionId = new II();
		interactionId.setRoot("2.16.840.1.113883.1.6");
		interactionId.setExtension("QUQI_IN000003UV01");
		qUQIIN000003UV01.setInteractionId(interactionId);
		
		// Processing code...
		CS processingCode = new CS();
		processingCode.setCode("T");
		qUQIIN000003UV01.setProcessingCode(processingCode);
		
		// Processing mode code...
		CS processingModeCode = new CS();
		processingModeCode.setCode("T");
		qUQIIN000003UV01.setProcessingModeCode(processingModeCode);
		
		// Accept ack code...
		CS acceptAckCode = new CS();
		acceptAckCode.setCode("AL");
		qUQIIN000003UV01.setAcceptAckCode(acceptAckCode);
		
		// Set receiver...
		setReceiverQueryContinuation(qUQIIN000003UV01);
		
		// Set sender...
		setSenderQueryContinuation(qUQIIN000003UV01);
		
		MCCIMT000300UV01Acknowledgement ack = new MCCIMT000300UV01Acknowledgement();
		CS ackValue = new CS();
		ackValue.setCode("AA");
		ack.setTypeCode(ackValue);
		MCCIMT000300UV01TargetMessage ackTargetMsg = new MCCIMT000300UV01TargetMessage();
		II ackTargetMsgId = new II();
		ackTargetMsgId.setRoot(queryResult.getId().getRoot());
		ackTargetMsg.setId(ackTargetMsgId);
		ack.setTargetMessage(ackTargetMsg);
		qUQIIN000003UV01.getAcknowledgement().add(ack);

		QUQIMT000001UV01ControlActProcess controlAct = new QUQIMT000001UV01ControlActProcess();
		CD controlActProcessCode = new CD();
		controlActProcessCode.setCode("PRPA_TE000003UV01");
		controlActProcessCode.setCodeSystem("2.16.840.1.113883.1.6");
		controlAct.setCode(controlActProcessCode);
		controlAct.setClassCode(ActClassControlAct.CACT);
		controlAct.setMoodCode(XActMoodIntentEvent.EVN);
		
		// Create queryContinuation section...
		QUQIMT000001UV01QueryContinuation queryContinuation = new QUQIMT000001UV01QueryContinuation();
		II queryId = new II();
		queryId.setRoot(queryIdOID);
		queryId.setExtension("NIST_CONTINUATION");
		queryContinuation.setQueryId(queryId);
		
		INT resultSetSizeINT = new INT();
		resultSetSizeINT.setValue(BigInteger.valueOf(0));
		queryContinuation.setContinuationQuantity(resultSetSizeINT);
		
		CS statusCode = new CS();
		statusCode.setCode("aborted");
		queryContinuation.setStatusCode(statusCode);

		controlAct.setQueryContinuation(queryContinuation);

		qUQIIN000003UV01.setControlActProcess(controlAct);

		pdqSupplierStub.pDQSupplier_QUQI_IN000003UV01_Cancel(qUQIIN000003UV01);
	}
	
	public PRPAIN201306UV02 queryContinue(PRPAIN201306UV02 queryResult) throws RemoteException
	{
		return queryContinue(queryResult,
				-1);
	}

	public PRPAIN201306UV02 queryContinue(PRPAIN201306UV02 queryResult,
			int resultSetSize) throws RemoteException
	{
		QUQIIN000003UV01Type qUQIIN000003UV01 = new QUQIIN000003UV01Type();

		// ITS version...
		qUQIIN000003UV01.setITSVersion("XML_1.0");
		
		// ID...
		II messageId = new II();
		messageId.setRoot(UUID.randomUUID().toString());
		qUQIIN000003UV01.setId(messageId);
		
		// Creation time...
		setCreationTime(qUQIIN000003UV01);
		
		// Interaction ID...
		II interactionId = new II();
		interactionId.setRoot("2.16.840.1.113883.1.6");
		interactionId.setExtension("QUQI_IN000003UV01");
		qUQIIN000003UV01.setInteractionId(interactionId);
		
		// Processing code...
		CS processingCode = new CS();
		processingCode.setCode("T");
		qUQIIN000003UV01.setProcessingCode(processingCode);
		
		// Processing mode code...
		CS processingModeCode = new CS();
		processingModeCode.setCode("T");
		qUQIIN000003UV01.setProcessingModeCode(processingModeCode);
		
		// Accept ack code...
		CS acceptAckCode = new CS();
		acceptAckCode.setCode("AL");
		qUQIIN000003UV01.setAcceptAckCode(acceptAckCode);
		
		// Set receiver...
		setReceiverQueryContinuation(qUQIIN000003UV01);
		
		// Set sender...
		setSenderQueryContinuation(qUQIIN000003UV01);
		
		MCCIMT000300UV01Acknowledgement ack = new MCCIMT000300UV01Acknowledgement();
		CS ackValue = new CS();
		ackValue.setCode("AA");
		ack.setTypeCode(ackValue);
		MCCIMT000300UV01TargetMessage ackTargetMsg = new MCCIMT000300UV01TargetMessage();
		II ackTargetMsgId = new II();
		ackTargetMsgId.setRoot(queryResult.getId().getRoot());
		ackTargetMsg.setId(ackTargetMsgId);
		ack.setTargetMessage(ackTargetMsg);
		qUQIIN000003UV01.getAcknowledgement().add(ack);

		QUQIMT000001UV01ControlActProcess controlAct = new QUQIMT000001UV01ControlActProcess();
		CD controlActProcessCode = new CD();
		controlActProcessCode.setCode("PRPA_TE000003UV01");
		controlActProcessCode.setCodeSystem("2.16.840.1.113883.1.6");
		controlAct.setCode(controlActProcessCode);
		controlAct.setClassCode(ActClassControlAct.CACT);
		controlAct.setMoodCode(XActMoodIntentEvent.EVN);
		
		// Create queryContinuation section...
		QUQIMT000001UV01QueryContinuation queryContinuation = new QUQIMT000001UV01QueryContinuation();
		II queryId = new II();
		queryId.setRoot(queryIdOID);
		queryId.setExtension("NIST_CONTINUATION");
		queryContinuation.setQueryId(queryId);
		
		if (resultSetSize > 0)
		{
			INT resultSetSizeINT = new INT();
			resultSetSizeINT.setValue(BigInteger.valueOf(resultSetSize));
			queryContinuation.setContinuationQuantity(resultSetSizeINT);
		}
		
		CS statusCode = new CS();
		statusCode.setCode("waitContinuedQueryResponse");
		queryContinuation.setStatusCode(statusCode);

		controlAct.setQueryContinuation(queryContinuation);

		qUQIIN000003UV01.setControlActProcess(controlAct);
		return pdqSupplierStub.pDQSupplier_QUQI_IN000003UV01_Continue(qUQIIN000003UV01);
	}
	
	public PRPAIN201306UV02 queryPatientDemographics(String givenName,
			String familyName,
			String middleName,
			String dateOfBirth,
			String gender,
			String motherMaidenName,
			String addressStreet,
			String addressCity,
			String addressState,
			String addressPostalCode,
			String patientId,
			String patientIdDomain,
			String otherIDsScopingOrganization,
			int resultSetSize) throws IOException
	{
//		if ((familyName == null) ||
//			(familyName.length() == 0))
//		{
//			throw new FamilyNameParamMissingException("FamilyName parameter is required");
//		}
//		
//		if ((gender == null) ||
//			(gender.length() == 0))
//		{
//			throw new GenderParamMissingException("Gender parameter is required");			
//		}
		
		PRPAIN201305UV02 pRPA_IN201305UV02 = new PRPAIN201305UV02(); 
		
		// ITS version...
		pRPA_IN201305UV02.setITSVersion("XML_1.0");
		
		// ID...
		II messageId = new II();
		messageId.setRoot(UUID.randomUUID().toString());
		pRPA_IN201305UV02.setId(messageId);
		
		// Creation time...
		setCreationTime(pRPA_IN201305UV02);
		
		// Interaction ID...
		II interactionId = new II();
		interactionId.setRoot("2.16.840.1.113883.1.6");
		interactionId.setExtension("PRPA_IN201305UV02");
		pRPA_IN201305UV02.setInteractionId(interactionId);
		
		// Processing code...
		CS processingCode = new CS();
		processingCode.setCode("T");
		pRPA_IN201305UV02.setProcessingCode(processingCode);
		
		// Processing mode code...
		CS processingModeCode = new CS();
		processingModeCode.setCode("T");
		pRPA_IN201305UV02.setProcessingModeCode(processingModeCode);
		
		// Accept ack code...
		CS acceptAckCode = new CS();
		acceptAckCode.setCode("AL");
		pRPA_IN201305UV02.setAcceptAckCode(acceptAckCode);
		
		// Set receiver...
		setReceiver(pRPA_IN201305UV02);
		
		// Set sender...
		setSender(pRPA_IN201305UV02);
		
		// Generate GUID portion of patient ID...
		String guid = UUID.randomUUID().toString();
		
		// Create QueryByParameter...
		PRPAMT201306UV02QueryByParameter queryByParam = new PRPAMT201306UV02QueryByParameter();
		II queryId = new II();
		queryId.setRoot(queryIdOID);
		queryId.setExtension((resultSetSize > 0) ? "NIST_CONTINUATION"
				: guid);
		queryByParam.setQueryId(queryId);
		CS responseModalityCode = new CS();
		responseModalityCode.setCode("R");
		queryByParam.setResponseModalityCode(responseModalityCode);
		CS responsePriorityCode = new CS();
		responsePriorityCode.setCode("I");
		queryByParam.setResponsePriorityCode(responsePriorityCode);
		CS statusCode = new CS();
		statusCode.setCode("new");
		queryByParam.setStatusCode(statusCode);
		
		// Create ParameterList...
		PRPAMT201306UV02ParameterList paramList = new PRPAMT201306UV02ParameterList();
		
		// Create LivingSubjectId if present...
		if ((patientId != null) &&
			(patientId.length() > 0))
		{
			PRPAMT201306UV02LivingSubjectId livingSubjectId = new PRPAMT201306UV02LivingSubjectId();
			paramList.getLivingSubjectId().add(setLivingSubjectId(livingSubjectId,
					patientId,
					patientIdDomain));
		}
		
		// Create LivingSubjectAdministrativeGender if present...
		if ((gender != null) &&
			(gender.length() > 0))
		{
			PRPAMT201306UV02LivingSubjectAdministrativeGender livingSubjectAdministrativeGender = new PRPAMT201306UV02LivingSubjectAdministrativeGender();
			paramList.getLivingSubjectAdministrativeGender().add(setLivingSubjectAdministrativeGender(livingSubjectAdministrativeGender,
					gender));
		}

		// Create LivingSubjectBirthTime if present...
		if ((dateOfBirth != null) &&
			(dateOfBirth.length() > 0))
		{
			PRPAMT201306UV02LivingSubjectBirthTime livingSubjectBirthTime = new PRPAMT201306UV02LivingSubjectBirthTime();
			paramList.getLivingSubjectBirthTime().add(setLivingSubjectBirthTime(livingSubjectBirthTime,
					dateOfBirth));
		}

		// Create LivingSubjectName...
		if ((familyName != null) &&
			(familyName.length() > 0))
		{
			PRPAMT201306UV02LivingSubjectName livingSubjectName = new PRPAMT201306UV02LivingSubjectName();
			paramList.getLivingSubjectName().add(setLivingSubjectName(livingSubjectName,
					familyName,
					givenName,
					middleName));
		}
		
		// Create patientAddress if present...
		if (((addressStreet != null) && (addressStreet.length() > 0)) ||
			((addressCity != null) && (addressCity.length() > 0)) ||
			((addressState != null) && (addressState.length() > 0)) ||
			((addressPostalCode != null) && (addressPostalCode.length() > 0)))
		{
			PRPAMT201306UV02PatientAddress patientAddress = new PRPAMT201306UV02PatientAddress(); 
			paramList.getPatientAddress().add(setPatientAddress(patientAddress,
					addressStreet,
					addressCity,
					addressState,
					addressPostalCode));
		}
		
		if ((otherIDsScopingOrganization != null) &&
			(otherIDsScopingOrganization.length() > 0))
//		    ((otherIDsScopingOrganizationOID != null) &&
//			 (otherIDsScopingOrganizationOID.length() > 0)))
		{
			// Populate otherIDsScopingOrganization...
			PRPAMT201306UV02OtherIDsScopingOrganization otherIDsScopingOrg = new PRPAMT201306UV02OtherIDsScopingOrganization();
			paramList.getOtherIDsScopingOrganization().add(setOtherIDsScopingOrganization(otherIDsScopingOrg));
		}

		if (resultSetSize > 0)
		{
			INT resultSetSizeINT = new INT();
			resultSetSizeINT.setValue(BigInteger.valueOf(resultSetSize));
			queryByParam.setInitialQuantity(resultSetSizeINT);
		}
		
		queryByParam.setParameterList(paramList);
		
		PRPAIN201305UV02QUQIMT021001UV01ControlActProcess controlAct = new PRPAIN201305UV02QUQIMT021001UV01ControlActProcess();
		CD controlActProcessCode = new CD();
		controlActProcessCode.setCode("PRPA_TE201305UV02");
		controlActProcessCode.setCodeSystem("2.16.840.1.113883.1.6");
		controlAct.setCode(controlActProcessCode);
		controlAct.setClassCode(ActClassControlAct.CACT);
		controlAct.setMoodCode(XActMoodIntentEvent.EVN);
		controlAct.setQueryByParameter(factory.createPRPAIN201305UV02QUQIMT021001UV01ControlActProcessQuerybyParameter(queryByParam));
		
		pRPA_IN201305UV02.setControlActProcess(controlAct);
		
		OMElement requestElement = pdqSupplierStub.toOM(pRPA_IN201305UV02, pdqSupplierStub.optimizeContent(
                new javax.xml.namespace.QName("urn:hl7-org:v3",
                    "PRPA_IN201305UV02")),
                new javax.xml.namespace.QName("urn:hl7-org:v3",
    				"PRPA_IN201305UV02"));
		String queryText = requestElement.toString();

		logIti47AuditMsg(queryText,
				patientId + "^^^&" + patientIdDomain + "&" + "ISO");

		return pdqSupplierStub.pDQSupplier_PRPA_IN201305UV02(pRPA_IN201305UV02);
	}
	
	private void setCreationTime(QUQIIN000003UV01Type qUQIIN000003UV01)
	{
		DateTime dt = new DateTime(DateTimeZone.UTC);
		TS creationTime = new TS();
		StringBuilder timeBuilder = new StringBuilder();
		timeBuilder.append(dt.getYear());
		timeBuilder.append((dt.getMonthOfYear() < 10) ? ("0" + dt.getMonthOfYear())
				: dt.getMonthOfYear());
		timeBuilder.append((dt.getDayOfMonth() < 10) ? ("0" + dt.getDayOfMonth())
				: dt.getDayOfMonth());
		timeBuilder.append((dt.getHourOfDay() < 10) ? ("0" + dt.getHourOfDay())
				: dt.getHourOfDay());
		timeBuilder.append((dt.getMinuteOfHour() < 10) ? ("0" + dt.getMinuteOfHour())
				: dt.getMinuteOfHour());
		timeBuilder.append((dt.getSecondOfMinute() < 10) ? ("0" + dt.getSecondOfMinute())
				: dt.getSecondOfMinute());
		creationTime.setValue(timeBuilder.toString());
		qUQIIN000003UV01.setCreationTime(creationTime);				
	}
	
	private void setCreationTime(PRPAIN201305UV02 pRPA_IN201305UV02)
	{
		DateTime dt = new DateTime(DateTimeZone.UTC);
		TS creationTime = new TS();
		StringBuilder timeBuilder = new StringBuilder();
		timeBuilder.append(dt.getYear());
		timeBuilder.append((dt.getMonthOfYear() < 10) ? ("0" + dt.getMonthOfYear())
				: dt.getMonthOfYear());
		timeBuilder.append((dt.getDayOfMonth() < 10) ? ("0" + dt.getDayOfMonth())
				: dt.getDayOfMonth());
		timeBuilder.append((dt.getHourOfDay() < 10) ? ("0" + dt.getHourOfDay())
				: dt.getHourOfDay());
		timeBuilder.append((dt.getMinuteOfHour() < 10) ? ("0" + dt.getMinuteOfHour())
				: dt.getMinuteOfHour());
		timeBuilder.append((dt.getSecondOfMinute() < 10) ? ("0" + dt.getSecondOfMinute())
				: dt.getSecondOfMinute());
		creationTime.setValue(timeBuilder.toString());
		pRPA_IN201305UV02.setCreationTime(creationTime);		
	}

	private void setReceiverQueryContinuation(QUQIIN000003UV01Type qUQIIN000003UV01)
	{
		MCCIMT000300UV01Receiver receiver = new MCCIMT000300UV01Receiver();
		receiver.setTypeCode(CommunicationFunctionType.RCV);
		MCCIMT000300UV01Device receiverDevice = new MCCIMT000300UV01Device();
		receiverDevice.setClassCode(EntityClassDevice.DEV);
		receiverDevice.setDeterminerCode("INSTANCE");
		II receiverDeviceId = new II();
		receiverDeviceId.setRoot(receiverApplicationName);
		receiverDevice.getId().add(receiverDeviceId);
		TEL receiverTelecom = new TEL();
		receiverTelecom.setValue(receiverTelecomValue);
		receiverDevice.getTelecom().add(receiverTelecom);
		receiver.setDevice(receiverDevice);
		qUQIIN000003UV01.getReceiver().add(receiver);
	}

	private void setReceiver(PRPAIN201305UV02 pRPA_IN201305UV02)
	{
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
		representedOrganizationId.setRoot(receiverApplicationRepresentedOrganization);
		representedOrganization.getId().add(representedOrganizationId);
		asAgent.setRepresentedOrganization(factory.createMCCIMT000100UV01AgentRepresentedOrganization(representedOrganization));
		receiverDevice.setAsAgent(factory.createMCCIMT000100UV01DeviceAsAgent(asAgent));
		receiver.setDevice(receiverDevice);
		pRPA_IN201305UV02.getReceiver().add(receiver);
	}

	private void setSenderQueryContinuation(QUQIIN000003UV01Type qUQIIN000003UV01)
	{
		MCCIMT000300UV01Sender sender = new MCCIMT000300UV01Sender();
		sender.setTypeCode(CommunicationFunctionType.SND);
		MCCIMT000300UV01Device senderDevice = new MCCIMT000300UV01Device();
		senderDevice.setClassCode(EntityClassDevice.DEV);
		senderDevice.setDeterminerCode("INSTANCE");
		II senderDeviceId = new II();
		senderDeviceId.setRoot(iExHubDomainOID);
		senderDevice.getId().add(senderDeviceId);
		sender.setDevice(senderDevice);
		qUQIIN000003UV01.setSender(sender);		
	}

	private void setSender(PRPAIN201305UV02 pRPA_IN201305UV02)
	{
		MCCIMT000100UV01Sender sender = new MCCIMT000100UV01Sender();
		sender.setTypeCode(CommunicationFunctionType.SND);
		MCCIMT000100UV01Device senderDevice = new MCCIMT000100UV01Device();
		senderDevice.setClassCode(EntityClassDevice.DEV);
		senderDevice.setDeterminerCode("INSTANCE");
		II senderDeviceId = new II();
		senderDeviceId.setRoot(iExHubDomainOID);
		senderDevice.getId().add(senderDeviceId);
		MCCIMT000100UV01Agent senderAsAgent = new MCCIMT000100UV01Agent();
		senderAsAgent.getClassCode().add("AGNT");
		MCCIMT000100UV01Organization senderRepresentedOrganization = new MCCIMT000100UV01Organization();
		senderRepresentedOrganization.setDeterminerCode("INSTANCE");
		senderRepresentedOrganization.setClassCode("ORG");
		II senderRepresentedOrganizationId = new II();
		senderRepresentedOrganizationId.setRoot(iExHubDomainOID);
		senderRepresentedOrganization.getId().add(senderRepresentedOrganizationId);
		senderAsAgent.setRepresentedOrganization(factory.createMCCIMT000100UV01AgentRepresentedOrganization(senderRepresentedOrganization));
		senderDevice.setAsAgent(factory.createMCCIMT000100UV01DeviceAsAgent(senderAsAgent));
		sender.setDevice(senderDevice);
		pRPA_IN201305UV02.setSender(sender);
	}
	
	private PRPAMT201306UV02LivingSubjectName setLivingSubjectName(PRPAMT201306UV02LivingSubjectName livingSubjectName,
			String familyName,
			String givenName,
			String middleName)
	{
		EN enFamily = null;
		EnFamily enFamily2 = new EnFamily();
		if ((familyName != null) &&
			(familyName.length() > 0))
		{
			enFamily2.getContent().add(familyName);
			enFamily = new EN();
			enFamily.getContent().add(factory.createENFamily(enFamily2));
		}
		
		livingSubjectName.getValue().add(enFamily);

		EN enGiven = null;
		EnGiven enGiven2 = new EnGiven();
		if ((givenName != null) &&
			(givenName.length() > 0))
		{
			enGiven = new EN();
			
			if ((middleName != null) &&
				(middleName.length() > 0))
			{
				enGiven2.getContent().add(givenName + " " + middleName);
			}
			else
			{
				enGiven2.getContent().add(givenName);
			}
			
			enGiven = new EN();
			enGiven.getContent().add(factory.createENGiven(enGiven2));
			livingSubjectName.getValue().add(enGiven);
		}
				
		ST livingSubjectNameSemanticsText = new ST();
		livingSubjectNameSemanticsText.getContent().add("LivingSubject.name");
		livingSubjectName.setSemanticsText(livingSubjectNameSemanticsText);
		return livingSubjectName;
	}
	
	private PRPAMT201306UV02LivingSubjectAdministrativeGender setLivingSubjectAdministrativeGender(PRPAMT201306UV02LivingSubjectAdministrativeGender adminGender,
			String gender)
	{
		CE genderValue = new CE();
		genderValue.setCode(((gender.compareToIgnoreCase("f") == 0) || (gender.compareToIgnoreCase("female") == 0)) ? "F"
				: (((gender.compareToIgnoreCase("m") == 0) || (gender.compareToIgnoreCase("male") == 0)) ? "M"
						: ""));
		adminGender.getValue().add(genderValue);
		ST genderSemanticsText = new ST();
		genderSemanticsText.getContent().add("LivingSubject.administrativeGender");
		adminGender.setSemanticsText(genderSemanticsText);
		return adminGender;
	}
	
	private PRPAMT201306UV02OtherIDsScopingOrganization setOtherIDsScopingOrganization(PRPAMT201306UV02OtherIDsScopingOrganization otherIDsScopingOrganization)
	{
		II scopingOrganizationId = new II();
		scopingOrganizationId.setRoot(otherIDsScopingOrganizationOID);
		otherIDsScopingOrganization.getValue().add(scopingOrganizationId);
		ST otherIDsScopingOrganizationSemanticsText = new ST();
		otherIDsScopingOrganizationSemanticsText.getContent().add("OtherIDs.scopingOrganization.id");
		otherIDsScopingOrganization.setSemanticsText(otherIDsScopingOrganizationSemanticsText);
		return otherIDsScopingOrganization; 
	}
	
	private PRPAMT201306UV02PatientAddress setPatientAddress(PRPAMT201306UV02PatientAddress patientAddress,
			String addressStreet,
			String addressCity,
			String addressState,
			String addressPostalCode)
	{
		AD address = new AD();
		if ((addressStreet != null) &&
			(addressStreet.length() > 0))
		{
			AdxpStreetAddressLine streetAddressLine = new AdxpStreetAddressLine();
			streetAddressLine.getContent().add(addressStreet);
			address.getContent().add(factory.createADStreetAddressLine(streetAddressLine));
		}
		
		if ((addressCity != null) &&
			(addressCity.length() > 0))
		{
			AdxpCity cityName = new AdxpCity();
			cityName.getContent().add(addressCity);
			address.getContent().add(factory.createADCity(cityName));
		}

		if ((addressState != null) &&
			(addressState.length() > 0))
		{
			AdxpState stateName = new AdxpState();
			stateName.getContent().add(addressState);
			address.getContent().add(factory.createADState(stateName));
		}

		if ((addressPostalCode != null) &&
			(addressPostalCode.length() > 0))
		{
			AdxpPostalCode postalCodeName = new AdxpPostalCode();
			postalCodeName.getContent().add(addressPostalCode);
			address.getContent().add(factory.createADPostalCode(postalCodeName));
		}

		patientAddress.getValue().add(address);

		ST patientAddressSemanticsText = new ST();
		patientAddressSemanticsText.getContent().add("Patient.addr");
		patientAddress.setSemanticsText(patientAddressSemanticsText);

		return patientAddress;
	}
	
	private PRPAMT201306UV02LivingSubjectBirthTime setLivingSubjectBirthTime(PRPAMT201306UV02LivingSubjectBirthTime livingSubjectBirthTime,
			String dateOfBirth)
	{
		DateTimeFormatter formatter = DateTimeFormat.forPattern("MM/dd/yyyy");
		DateTime dt = formatter.parseDateTime(dateOfBirth);
		StringBuilder timeBuilder = new StringBuilder();
		timeBuilder.append(dt.getYear());
		timeBuilder.append((dt.getMonthOfYear() < 10) ? ("0" + dt.getMonthOfYear())
				: dt.getMonthOfYear());
		timeBuilder.append((dt.getDayOfMonth() < 10) ? ("0" + dt.getDayOfMonth())
				: dt.getDayOfMonth());
		
		IVLTS date = new IVLTS();
		date.setValue(timeBuilder.toString());
		livingSubjectBirthTime.getValue().add(date);
		
		ST birthTimeSemanticsText = new ST();
		birthTimeSemanticsText.getContent().add("LivingSubject.administrativeGender");
		livingSubjectBirthTime.setSemanticsText(birthTimeSemanticsText);
		
		return livingSubjectBirthTime;
	}
	
	private PRPAMT201306UV02LivingSubjectId setLivingSubjectId(PRPAMT201306UV02LivingSubjectId livingSubjectId,
			String patientId,
			String patientIdDomain)
	{
		II livingSubjectIdII = new II();
		livingSubjectIdII.setRoot(patientIdDomain);
		livingSubjectIdII.setExtension(patientId);
		livingSubjectId.getValue().add(livingSubjectIdII);
		ST livingSubjectIdSemanticsText = new ST();
		livingSubjectIdSemanticsText.getContent().add("LivingSubject.id");
		livingSubjectId.setSemanticsText(livingSubjectIdSemanticsText);
		return livingSubjectId;
	}
}
