package it.bologna.ausl.internauta.service.argo.raccolta;

/**
 *
 * @author Next
 */
public class SottoDocumentoGdDoc {

    private String id;
    private String guid;
    private String idGdDoc;
    private String nome;
    private String uuidMongoPdf;
    private String uuidMongoFirmato;
    private String uuidMongoOriginale;
    private Long dimensioneOriginale;
    private Long dimensionePdf;
    private Long dimensioneFirmato;
    private Integer convertibilePdf;
    private Integer principale;
    private String mimetypeFileOriginale;
    private String tipo;
    private String codice;

    public SottoDocumentoGdDoc() {
    }

    public SottoDocumentoGdDoc(String id, String guid, String idGdDoc, String nome, String uuidMongoPdf, String uuidMongoFirmato, String uuidMongoOriginale, Long dimensioneOriginale, Long dimensionePdf, Long dimensioneFirmato, Integer convertibilePdf, Integer principale, String mimetypeFileOriginale, String tipo, String codice) {
        this.id = id;
        this.guid = guid;
        this.idGdDoc = idGdDoc;
        this.nome = nome;
        this.uuidMongoPdf = uuidMongoPdf;
        this.uuidMongoFirmato = uuidMongoFirmato;
        this.uuidMongoOriginale = uuidMongoOriginale;
        this.dimensioneOriginale = dimensioneOriginale;
        this.dimensionePdf = dimensionePdf;
        this.dimensioneFirmato = dimensioneFirmato;
        this.convertibilePdf = convertibilePdf;
        this.principale = principale;
        this.mimetypeFileOriginale = mimetypeFileOriginale;
        this.tipo = tipo;
        this.codice = codice;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getIdGdDoc() {
        return idGdDoc;
    }

    public void setIdGdDoc(String idGdDoc) {
        this.idGdDoc = idGdDoc;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getUuidMongoPdf() {
        return uuidMongoPdf;
    }

    public void setUuidMongoPdf(String uuidMongoPdf) {
        this.uuidMongoPdf = uuidMongoPdf;
    }

    public String getUuidMongoFirmato() {
        return uuidMongoFirmato;
    }

    public void setUuidMongoFirmato(String uuidMongoFirmato) {
        this.uuidMongoFirmato = uuidMongoFirmato;
    }

    public String getUuidMongoOriginale() {
        return uuidMongoOriginale;
    }

    public void setUuidMongoOriginale(String uuidMongoOriginale) {
        this.uuidMongoOriginale = uuidMongoOriginale;
    }

    public Long getDimensioneOriginale() {
        return dimensioneOriginale;
    }

    public void setDimensioneOriginale(Long dimensioneOriginale) {
        this.dimensioneOriginale = dimensioneOriginale;
    }

    public Long getDimensionePdf() {
        return dimensionePdf;
    }

    public void setDimensionePdf(Long dimensionePdf) {
        this.dimensionePdf = dimensionePdf;
    }

    public Long getDimensioneFirmato() {
        return dimensioneFirmato;
    }

    public void setDimensioneFirmato(Long dimensioneFirmato) {
        this.dimensioneFirmato = dimensioneFirmato;
    }

    public Integer getConvertibilePdf() {
        return convertibilePdf;
    }

    public void setConvertibilePdf(Integer convertibilePdf) {
        this.convertibilePdf = convertibilePdf;
    }

    public Integer getPrincipale() {
        return principale;
    }

    public void setPrincipale(Integer principale) {
        this.principale = principale;
    }

    public String getMimetypeFileOriginale() {
        return mimetypeFileOriginale;
    }

    public void setMimetypeFileOriginale(String mimetypeFileOriginale) {
        this.mimetypeFileOriginale = mimetypeFileOriginale;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getCodice() {
        return codice;
    }

    public void setCodice(String codice) {
        this.codice = codice;
    }

}
