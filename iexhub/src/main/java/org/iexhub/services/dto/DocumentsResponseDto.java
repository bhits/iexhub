package org.iexhub.services.dto;


import java.util.ArrayList;

public class DocumentsResponseDto {

    private ArrayList<PatientDocument> documents;

    public ArrayList<PatientDocument> getDocuments() {
        return this.documents;
    }

    public void setDocuments(ArrayList<PatientDocument> documents) {
        this.documents = documents;
    }
}
