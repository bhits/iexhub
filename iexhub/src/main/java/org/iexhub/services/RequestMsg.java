/*******************************************************************************
 * Copyright (c) 2016 Substance Abuse and Mental Health Services Administration (SAMHSA)
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
 *     Eversolve, LLC - initial IExHub implementation
 *******************************************************************************/
package org.iexhub.services;

import java.util.Date;

/**
 * @author A. Sute
 *
 */
@Deprecated
public class RequestMsg
{
	public String getEnterpriseMasterRecordNumber() {
		return EnterpriseMasterRecordNumber;
	}
	public void setEnterpriseMasterRecordNumber(String enterpriseMasterRecordNumber) {
		EnterpriseMasterRecordNumber = enterpriseMasterRecordNumber;
	}
	public String getLastName() {
		return LastName;
	}
	public void setLastName(String lastName) {
		LastName = lastName;
	}
	public String getFirstName() {
		return FirstName;
	}
	public void setFirstName(String firstName) {
		FirstName = firstName;
	}
	public String getMiddleName() {
		return MiddleName;
	}
	public void setMiddleName(String middleName) {
		MiddleName = middleName;
	}
	public Date getDateOfBirth() {
		return DateOfBirth;
	}
	public void setDateOfBirth(Date dateOfBirth) {
		DateOfBirth = dateOfBirth;
	}
	public String getPatientGender() {
		return PatientGender;
	}
	public void setPatientGender(String patientGender) {
		PatientGender = patientGender;
	}
	public String getMotherMaidenName() {
		return MotherMaidenName;
	}
	public void setMotherMaidenName(String motherMaidenName) {
		MotherMaidenName = motherMaidenName;
	}
	public Date getStartDate() {
		return StartDate;
	}
	public void setStartDate(Date startDate) {
		StartDate = startDate;
	}
	public Date getEndDate() {
		return EndDate;
	}
	public void setEndDate(Date endDate) {
		EndDate = endDate;
	}
	
	private String EnterpriseMasterRecordNumber = null;
	private String LastName = null;
	private String FirstName = null;
	private String MiddleName = null;
	private Date DateOfBirth = null;
	private String PatientGender = null;
	private String MotherMaidenName = null;
	private Date StartDate = null;
	private Date EndDate = null;
}
