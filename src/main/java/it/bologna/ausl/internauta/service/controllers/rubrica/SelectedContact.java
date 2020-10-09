package it.bologna.ausl.internauta.service.controllers.rubrica;

import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;

/**
 *
 * @author gusgus
 */
class SelectedContact {

    private Contatto contact;
    private DettaglioContatto address;
    private Boolean estemporaneo;
    private Boolean addToRubrica;
    private SelectedContactStatus status;

    public SelectedContact() {
    }

    public Contatto getContact() {
        return contact;
    }

    public void setContact(Contatto contact) {
        this.contact = contact;
    }

    public DettaglioContatto getAddress() {
        return address;
    }

    public void setAddress(DettaglioContatto address) {
        this.address = address;
    }

    public Boolean getEstemporaneo() {
        return estemporaneo;
    }

    public void setEstemporaneo(Boolean estemporaneo) {
        this.estemporaneo = estemporaneo;
    }

    public Boolean getAddToRubrica() {
        return addToRubrica;
    }

    public void setAddToRubrica(Boolean addToRubrica) {
        this.addToRubrica = addToRubrica;
    }

    public SelectedContactStatus getStatus() {
        return status;
    }

    public void setStatus(SelectedContactStatus status) {
        this.status = status;
    }

}

enum SelectedContactStatus {
    INITIAL,
    INSERTED,
    UPDATED
}
