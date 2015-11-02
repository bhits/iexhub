package com.InfoExchangeHub.Services;

import java.util.ArrayList;

public class GetPatientDataResponse
{
	public ArrayList<String> getDocuments() {
		return documents;
	}
	public void setDocuments(ArrayList<String> documents) {
		this.documents = documents;
	}
	public ArrayList<String> getErrorMsgs() {
		return errorMsgs;
	}
	public void setErrorMsgs(ArrayList<String> errorMsgs) {
		this.errorMsgs = errorMsgs;
	}

    private ArrayList<String> documents = new ArrayList<String>();
    private ArrayList<String> errorMsgs = new ArrayList<String>();
}
