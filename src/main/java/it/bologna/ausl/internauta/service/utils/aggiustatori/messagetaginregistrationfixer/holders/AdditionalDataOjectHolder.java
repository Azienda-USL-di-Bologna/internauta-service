/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils.aggiustatori.messagetaginregistrationfixer.holders;

import org.json.JSONObject;

/**
 *
 * @author Salo
 */
public class AdditionalDataOjectHolder {

    private JSONObject additionalDataObject;

    public AdditionalDataOjectHolder(JSONObject additionalDataObject) {
        this.additionalDataObject = additionalDataObject;
    }

    private static enum AdditionalDataKey {

        ID_UTENTE {
            public String toString() {
                return "idUtente";
            }
        },
        ID_AZIENDA {
            public String toString() {
                return "idAzienda";
            }
        },
        ID_DOCUMENTO {
            public String toString() {
                return "idDocumento";
            }
        },
    }

    public JSONObject getAziendaObject() {
        return (JSONObject) additionalDataObject.get(AdditionalDataKey.ID_AZIENDA.toString());
    }

    public JSONObject getDocumentoObject() {
        return (JSONObject) additionalDataObject.get(AdditionalDataKey.ID_DOCUMENTO.toString());
    }

    public JSONObject getUtenteObject() {
        return (JSONObject) additionalDataObject.get(AdditionalDataKey.ID_UTENTE.toString());
    }
}
