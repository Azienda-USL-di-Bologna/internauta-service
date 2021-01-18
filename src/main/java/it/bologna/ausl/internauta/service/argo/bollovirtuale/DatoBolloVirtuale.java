/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.argo.bollovirtuale;

import java.time.LocalDateTime;
import java.util.Date;

/**
 *
 * @author Top
 */
public class DatoBolloVirtuale {

    private String tipoOggettoBollo;
    private String codiceRegistroDoc;
//    private String guidOggettoOrigineDoc;
    private String numeroDoc;
    private Integer annoNumeroDoc;
    private Date dataNumeroDoc;
    private String oggettoDoc;
    private String redattoreDoc;
    private Integer noFacciateBollo;
    private Integer noRigheBollo;
    private Integer noBolliAltriImporti;
    private Integer importoBolliAltriImporti;

//    bv.tipo_oggetto as tipoOggettoBollo,
//    pd.codice_registro as codiceRegistroDoc,
//    bv.guid_oggetto_origine as guidOggettoOrigineDoc,
//    bv.numero_righe as noRigheBollo,
//    bv.numero_facciate as noFaccialeBollo,
//    bv.numero_bolli as noBolliAltriImporti,
//    bv.importi as importoBolliAltriImporti,
//    pd.protocollo as numeroDoc,
//    pd.anno_protocollo as annoNumeroDoc,
//    pd.data_protocollo as dataNumeroDoc,
//    pd.oggetto as oggettoDoc,
//    COALESCE (pu.descrizione, ub.descrizione) as redattoreDoc   
    public String getTipoOggettoBollo() {
        return tipoOggettoBollo;
    }

    public void setTipoOggettoBollo(String tipoOggettoBollo) {
        this.tipoOggettoBollo = tipoOggettoBollo;
    }

    public String getCodiceRegistroDoc() {
        return codiceRegistroDoc;
    }

    public void setCodiceRegistroDoc(String codiceRegistroDoc) {
        this.codiceRegistroDoc = codiceRegistroDoc;
    }

//    public String getGuidOggettoOrigineDoc() {
//        return guidOggettoOrigineDoc;
//    }
//
//    public void setGuidOggettoOrigineDoc(String guidOggettoOrigineDoc) {
//        this.guidOggettoOrigineDoc = guidOggettoOrigineDoc;
//    }
    public String getNumeroDoc() {
        return numeroDoc;
    }

    public void setNumeroDoc(String numeroDoc) {
        this.numeroDoc = numeroDoc;
    }

    public Integer getAnnoNumeroDoc() {
        return annoNumeroDoc;
    }

    public void setAnnoNumeroDoc(Integer annoNumeroDoc) {
        this.annoNumeroDoc = annoNumeroDoc;
    }

    public Date getDataNumeroDoc() {
        return dataNumeroDoc;
    }

    public void setDataNumeroDoc(Date dataNumeroDoc) {
        this.dataNumeroDoc = dataNumeroDoc;
    }

    public String getOggettoDoc() {
        return oggettoDoc;
    }

    public void setOggettoDoc(String oggettoDoc) {
        this.oggettoDoc = oggettoDoc;
    }

    public String getRedattoreDoc() {
        return redattoreDoc;
    }

    public void setRedattoreDoc(String redattoreDoc) {
        this.redattoreDoc = redattoreDoc;
    }

    public Integer getNoFacciateBollo() {
        return noFacciateBollo;
    }

    public void setNoFacciateBollo(Integer noFacciateBollo) {
        this.noFacciateBollo = noFacciateBollo;
    }

    public Integer getNoRigheBollo() {
        return noRigheBollo;
    }

    public void setNoRigheBollo(Integer noRigheBollo) {
        this.noRigheBollo = noRigheBollo;
    }

    public Integer getNoBolliAltriImporti() {
        return noBolliAltriImporti;
    }

    public void setNoBolliAltriImporti(Integer noBolliAltriImporti) {
        this.noBolliAltriImporti = noBolliAltriImporti;
    }

    public Integer getImportoBolliAltriImporti() {
        return importoBolliAltriImporti;
    }

    public void setImportoBolliAltriImporti(Integer importoBolliAltriImporti) {
        this.importoBolliAltriImporti = importoBolliAltriImporti;
    }
}
