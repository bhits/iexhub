package org.iexhub.services.dto;

/**
 * Created by tomson.ngassa on 7/11/2016.
 */
public class PatientDocument {
    private String name;
    private String document;

    public PatientDocument() {}

    public PatientDocument(String name, String document) {
        this.name = name;
        this.document = document;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }
}
