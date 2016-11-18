package org.iexhub.services.dto;

public class ClinicalDocumentResponse {
    private boolean published;

    public ClinicalDocumentResponse() {
    }

    public ClinicalDocumentResponse(boolean published) {
        this.published = published;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }
}
