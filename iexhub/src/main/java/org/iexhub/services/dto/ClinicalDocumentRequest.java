package org.iexhub.services.dto;

/**
 * Created by tomson.ngassa on 7/20/2016.
 */
public class ClinicalDocumentRequest {
    private byte[] document;

    public ClinicalDocumentRequest() {
    }

    public ClinicalDocumentRequest(byte[] document) {
        this.document = document;
    }

    public byte[] getDocument() {
        return document;
    }

    public void setDocument(byte[] document) {
        this.document = document;
    }
}
