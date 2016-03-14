package org.iexhub.services;

import java.util.Date;

/**
 * @author A. Sute
 *
 */

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
