package it.bologna.ausl.internauta.service.controllers.rubrica;

import it.bologna.ausl.model.entities.rubrica.Contatto;

/**
 *
 * @author gusgus
 */
class SelectedContact {
    private Contatto contact;
    private Object address; // Class: Email, Indirizzo, Telefono
    private String status; // TODO fare Enum
    private String contactInde;

    public Contatto getContact() {
        return contact;
    }

    public void setContact(Contatto contact) {
        this.contact = contact;
    }

    public Object getAddress() {
        return address;
    }

    public void setAddress(Object address) {
        this.address = address;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getContactInde() {
        return contactInde;
    }

    public void setContactInde(String contactInde) {
        this.contactInde = contactInde;
    }
    
    
}
