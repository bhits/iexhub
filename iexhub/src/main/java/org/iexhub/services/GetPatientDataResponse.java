package org.iexhub.services;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author A. Sute
 *
 */

@XmlRootElement
public class GetPatientDataResponse
{
	public ArrayList<Object> getDocuments() {
		return documents;
	}
	public void setDocuments(ArrayList<Object> documents) {
		this.documents = documents;
	}
	public ArrayList<String> getErrorMsgs() {
		return errorMsgs;
	}
	public void setErrorMsgs(ArrayList<String> errorMsgs) {
		this.errorMsgs = errorMsgs;
	}

    private ArrayList<Object> documents = new ArrayList<Object>();
    private ArrayList<String> errorMsgs = new ArrayList<String>();
}
