
/*******************************************************************************
* Copyright (c) 2015, 2016 Substance Abuse and Mental Health Services Administration (SAMHSA), 
* Department of Veterans Affairs
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
*     Ioana Singureanu, Eversolve, LLC - September 2016 
*     
*******************************************************************************
*/
package org.iexhub.services;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.dstu3.exceptions.FHIRException;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Device;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Procedure;
import org.hl7.fhir.dstu3.model.Procedure.ProcedureFocalDeviceComponent;
import org.hl7.fhir.dstu3.model.Procedure.ProcedureStatus;
import org.hl7.fhir.instance.model.api.IIdType;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.params.HttpMethodParams;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;

/**
 * VitalSignsMonitoringProcedureStu3Test Test for STU3 Procedure resource
 * 
 * 
 * @author Ioana Singureanu
 *
 */
public class VitalSignsMonitoringProcedureStu3Test {

	private static String testResourcesPath = "src/test/resources/";
	private static int fhirClientSocketTimeout = 50000;
	private static String uriPrefix = "urn:oid:";
	private static String iExHubDomainOid = "2.16.840.1.113883.3.72.5.9.1";
	private static String patientId = "patientId";
	private static String deviceUdiCarrier = "=/A9999XYZ100T0944=,000025=A99971312345600=>014032=}013032&,1000000000000XYZ123";
	private static String serverURL1 = "http://fhirtest.uhn.ca/baseDstu3";
	private static String serverURL2 = "http://wildfhir.aegis.net/fhir1-6-0";
	private static IIdType procedureCreatedId ;
	private static IIdType deviceCreatedId ;
	private static String currentTest = "MonitoringProcedure";
	
	private static Procedure vitalSignsMonitoringProcedure ;
	
	


	/**
	 * Create monitoring Procedure, include a "contained" Device based on
	 * information read at the point of care.
	 */
	@Test
	public void testCreateMonitoringProcedureInProgress() {
		Logger logger = LoggerFactory.getLogger(VitalSignsMonitoringProcedureStu3Test.class);
		LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
		// by default only summary info is logged
		loggingInterceptor.setLogRequestSummary(true);
		loggingInterceptor.setLogRequestBody(true);
		loggingInterceptor.setLogger(logger);

		vitalSignsMonitoringProcedure = new Procedure();
		//vitalSignsMonitoringProcedure.setId("VitalSignsMonitoringProcedure1");
		Patient referencedPatient = createContainedPatientReference(patientId);
		Coding pulseOximetryProcedureCoding = new Coding();
		pulseOximetryProcedureCoding.setSystem("http://snomed.info/sct")
				.setCode("252465000")
				.setDisplay("Pulse oximetry");
		vitalSignsMonitoringProcedure.getCode()
		.addCoding(pulseOximetryProcedureCoding)
		.setText("Pulse Oximetry Procedure");
		Period start  = new Period();
		start.setStart(Calendar.getInstance().getTime());
		vitalSignsMonitoringProcedure.setPerformed(start);
		// Procedure status set to "in progress"
		vitalSignsMonitoringProcedure.setStatus(ProcedureStatus.INPROGRESS);
		// Add patient reference - if the id is null, it will create it as "contained"
		vitalSignsMonitoringProcedure.getSubject().setResource(referencedPatient);
		// Add monitoring device reference as a focal device - if the id is null, it will create it as "contained"
		Device referencedDevice = createContainedDeviceUdiReference("monitor1", deviceUdiCarrier);
		ProcedureFocalDeviceComponent focalDevice = new ProcedureFocalDeviceComponent();
		focalDevice.getManipulated().setResource(referencedDevice);;
		vitalSignsMonitoringProcedure.addFocalDevice(focalDevice);
		//Add Practitioner - operator reference
		Practitioner operator = createContainedPractitionerReference("operatorToken");
		vitalSignsMonitoringProcedure.addPerformer().getActor().setResource(operator);
		//Add a reason - text
		vitalSignsMonitoringProcedure.getReasonCodeFirstRep().setText("Point-of-care monitoring");
		//
		FhirContext ctx = FhirContext.forDstu3();
		ctx.getRestfulClientFactory().setSocketTimeout(fhirClientSocketTimeout);
		System.out
				.println(ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(vitalSignsMonitoringProcedure));

		// OAuth2 Token from authorization server
		// String token = "3w03fj.r3r3t";
		// BearerTokenAuthInterceptor authInterceptor = new
		// BearerTokenAuthInterceptor(token);

		// Create a client and post the transaction to the server
		// http://fhirtest.uhn.ca/baseDstu3
		IGenericClient client = ctx.newRestfulGenericClient(serverURL1);
		logger.debug("Server URL:"+serverURL2);
		// Required only for logging
		client.registerInterceptor(loggingInterceptor);
		// OAuth2 token
		// client.registerInterceptor(authInterceptor);		
		String xmlProcedure = ctx.newXmlParser().setPrettyPrint(true)
				.encodeResourceToString(vitalSignsMonitoringProcedure);
		File procedureFile = new File(testResourcesPath + "/XML/temp/" + currentTest + ".procedure.xml");
		try {
			FileUtils.writeStringToFile(procedureFile, xmlProcedure);
		} catch (IOException e) {
			fail("Write procedure resource to XML:" + e.getMessage());
		}
		String deviceResourceAsString= ctx.newXmlParser().setPrettyPrint(true)
				.encodeResourceToString(referencedDevice);
		File deviceFile = new File(testResourcesPath + "/XML/temp/" + currentTest + ".device.xml");
		try {
			FileUtils.writeStringToFile(deviceFile, deviceResourceAsString);
		} catch (IOException e) {
			fail("Write device resource to XML:" + e.getMessage());
		}
		MethodOutcome result;
		result = client.create().resource(referencedDevice).execute();
		deviceCreatedId = result.getId();
		System.out.println("Device id:"+deviceCreatedId);
		referencedDevice.setId(deviceCreatedId);
		deviceResourceAsString = ctx.newXmlParser().setPrettyPrint(true)
				.encodeResourceToString(referencedDevice );
		deviceFile = new File(testResourcesPath + "/XML/temp/" + currentTest + ".device.from.server.xml");
		try {
			FileUtils.writeStringToFile(deviceFile, deviceResourceAsString);
		} catch (IOException e) {
			fail("Write persisted device resource to XML:" + e.getMessage());
		}
		deviceResourceAsString = ctx.newJsonParser().setPrettyPrint(true)
				.encodeResourceToString(referencedDevice );
		deviceFile = new File(testResourcesPath + "/JSON/temp/" + currentTest + ".device.from.server.json");
		try {
			FileUtils.writeStringToFile(deviceFile, deviceResourceAsString);
		} catch (IOException e) {
			fail("Write persisted device resource to Json:" + e.getMessage());
		}
		// create procedure
		result = client.create().resource(vitalSignsMonitoringProcedure).execute();
		procedureCreatedId= result.getId();
		logger.debug("Procedure id from server:"+procedureCreatedId);
		vitalSignsMonitoringProcedure.setId(procedureCreatedId);
		xmlProcedure = ctx.newXmlParser().setPrettyPrint(true)
				.encodeResourceToString(vitalSignsMonitoringProcedure);
		procedureFile = new File(testResourcesPath + "/XML/temp/" + currentTest + ".procedure.from.server.xml");
		try {
			FileUtils.writeStringToFile(procedureFile, xmlProcedure);
		} catch (IOException e) {
			fail("Write persisted procedure resource to XML:" + e.getMessage());
		}
		xmlProcedure = ctx.newJsonParser().setPrettyPrint(true)
				.encodeResourceToString(vitalSignsMonitoringProcedure);
		procedureFile = new File(testResourcesPath + "/JSON/temp/" + currentTest + ".procedure.from.server.json");
		try {
			FileUtils.writeStringToFile(procedureFile, xmlProcedure);
		} catch (IOException e) {
			fail("Write persisted procedure resource to json:" + e.getMessage());
		}		
		
		//complete the procedure		
		vitalSignsMonitoringProcedure.setStatus(ProcedureStatus.COMPLETED);
		try {
			vitalSignsMonitoringProcedure.getPerformedPeriod().setEnd(Calendar.getInstance().getTime());
		} catch (FHIRException e) {
			e.printStackTrace();
			fail("Failed to set end date:" + e.getMessage());
		}
		System.out.println("For update:"+"\n"+ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(vitalSignsMonitoringProcedure));
		
		// update procedure
				result = client.update().resource(vitalSignsMonitoringProcedure).execute();
				//get the latest - after update
				Procedure procFromServer =  client.read().resource(Procedure.class).withId(procedureCreatedId.getIdPart()).execute();
		System.out.println("For update:"+"\n"+ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(vitalSignsMonitoringProcedure));
				
		xmlProcedure = ctx.newXmlParser().setPrettyPrint(true)
				.encodeResourceToString(procFromServer);
		procedureFile = new File(testResourcesPath + "/XML/temp/" + currentTest + ".procedure.updated.read.from.server.xml");
		try {
			FileUtils.writeStringToFile(procedureFile, xmlProcedure);
		} catch (IOException e) {
			fail("Write persisted procedure resource to XML:" + e.getMessage());
		}
		xmlProcedure = ctx.newJsonParser().setPrettyPrint(true)
				.encodeResourceToString(vitalSignsMonitoringProcedure);
		procedureFile = new File(testResourcesPath + "/JSON/temp/" + currentTest + ".procedure.updated.read.from.server.json");
		try {
			FileUtils.writeStringToFile(procedureFile, xmlProcedure);
		} catch (IOException e) {
			fail("Write persisted procedure resource to json:" + e.getMessage());
		}
		

		
	}
	
	
	
	


	/**
	 * Create a simple patient reference containing only the patient id read
	 * from the patient wrist band
	 * 
	 * @param barCodePatientId
	 * @return Patient resource
	 */
	private Patient createContainedPatientReference(String barCodePatientId) {
		Patient testPatientResource = new Patient();
		// if the id is set, the parent resource will reference it
		//testPatientResource.setId(barCodePatientId);
		// testPatientResource.addName().addFamily("Patient Family
		// Name").addGiven("Patient Given Name");
		// set SSN value using coding system 2.16.840.1.113883.4.1
		// testPatientResource.addIdentifier().setSystem(uriPrefix +
		// "2.16.840.1.113883.4.1").setValue("123-45-6789");
		// set local patient id
		testPatientResource.addIdentifier().setSystem(uriPrefix + iExHubDomainOid).setValue(barCodePatientId);

		return testPatientResource;
	}

	/**
	 * Invokes "parseUDI" Api
	 */
	@Test
	public void testParseUDI() {
		String accessGudidUrl = "https://accessgudid.nlm.nih.gov/api/v1/parse_udi.xml";
		HttpClient accessGudidClient = new HttpClient();
		System.out.println("Server URL:"+accessGudidUrl);
		GetMethod method = new GetMethod("http://www.apache.org/");
		method.setPath(accessGudidUrl);
		method.getParams().setParameter("udi", deviceUdiCarrier);
		
	}
	/**
	 * Create a Device resource based on scanned UDI carrier string
	 * http://www.fda.gov/downloads/MedicalDevices/DeviceRegulationandGuidance/UniqueDeviceIdentification/GlobalUDIDatabaseGUDID/UCM396595.doc
	 * @param id
	 * @param uidCarrierString
	 * @return Device
	 */
	private Device createContainedDeviceUdiReference(String id, String uidCarrierString) {
		Device testDevice = new Device();
		//testDevice.setId(id);
		Identifier udiCarrierIdentifier = new Identifier();
		udiCarrierIdentifier.setSystem("http://hl7.org/fhir/NamingSystem/fda-udi").setValue(uidCarrierString);
		testDevice.setUdiCarrier(udiCarrierIdentifier).getType().getCodingFirstRep().setDisplay("UDI")
				.setSystem("http://hl7.org/fhir/identifier-type").setCode("UDI");

		return testDevice;
	}
	
	private Practitioner createContainedPractitionerReference(String identifier)
	{
		Practitioner testPractitioner = new Practitioner();
		testPractitioner.addIdentifier().setSystem(uriPrefix+ iExHubDomainOid).setValue(identifier);
		return testPractitioner;
	}

}
