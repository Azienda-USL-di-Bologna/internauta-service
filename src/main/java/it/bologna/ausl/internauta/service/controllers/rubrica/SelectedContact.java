package it.bologna.ausl.internauta.service.controllers.rubrica;

/**
 *
 * @author gusgus
 */
class SelectedContact {

    private Object contact;
    private Object address;
    private Boolean estemporaneo;
    private Boolean addToRubrica;
    private SelectedContactStatus status;

    public SelectedContact() {
    }

    public Object getContact() {
        return contact;
    }

    public void setContact(Object contact) {
        this.contact = contact;
    }

    public Object getAddress() {
        return address;
    }

    public void setAddress(Object address) {
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
