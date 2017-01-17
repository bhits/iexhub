package org.iexhub.services.dto;


import java.util.List;

public class DocumentsResponseDto {

    private List<PatientDocument> documents;

    public List<PatientDocument> getDocuments() {
        return this.documents;
    }

    public void setDocuments(List<PatientDocument> documents) {
        this.documents = documents;
    }
}
