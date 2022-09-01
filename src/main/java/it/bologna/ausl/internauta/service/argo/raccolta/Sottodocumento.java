package it.bologna.ausl.internauta.service.argo.raccolta;

/**
 *
 * @author Matteo Next
 */
public class Sottodocumento {

    private String nome;

    private String nomeOriginale;

    private String mimeTypeOriginale;

    private String guidSottodocumento;

    private String idGddoc;

    private String uuidMongo;

    private String estensione;

    public String getEstensione() {
        return estensione;
    }

    public void setEstensione(String estensione) {
        this.estensione = estensione;
    }

    public String getGuidSottodocumento() {
        return guidSottodocumento;
    }

    public void setIdSottodocumento(String guidSottodocumento) {
        this.guidSottodocumento = guidSottodocumento;
    }

    public String getIdGddoc() {
        return idGddoc;
    }

    public void setIdGddoc(String idGddoc) {
        this.idGddoc = idGddoc;
    }

    public String getUuidMongo() {
        return uuidMongo;
    }

    public void setUuidMongo(String uuidMongo) {
        this.uuidMongo = uuidMongo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getNomeOriginale() {
        return nomeOriginale;
    }

    public void setNomeOriginale(String nomeOriginale) {
        this.nomeOriginale = nomeOriginale;
    }

    public String getMimeTypeOriginale() {
        return mimeTypeOriginale;
    }

    public void setMimeTypeOriginale(String mymeTypeOriginale) {
        this.mimeTypeOriginale = mymeTypeOriginale;
    }
}
