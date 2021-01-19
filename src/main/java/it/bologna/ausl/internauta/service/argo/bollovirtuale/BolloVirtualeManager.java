/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.argo.bollovirtuale;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Top
 */
public class BolloVirtualeManager {

    public static String queryGetDatiBolliVirtuali() {
        String query = "--- procton\n"
                + "SELECT bv.tipo_oggetto as tipo_oggetto_bollo,\n"
                + "pd.codice_registro as codice_registro_doc,\n"
                + "--bv.guid_oggetto_origine as guid_oggetto_origine_doc,\n"
                + "bv.numero_righe as no_righe_bollo,\n"
                + "bv.numero_facciate as no_facciate_bollo,\n"
                + "bv.numero_bolli as no_bolli_altri_importi,\n"
                + "bv.importi as importo_bolli_altri_importi,\n"
                + "pd.protocollo as numero_doc,\n"
                + "pd.anno_protocollo as anno_numero_doc,\n"
                + "pd.data_protocollo as data_numero_doc,\n"
                + "pd.oggetto as oggetto_doc,\n"
                + "COALESCE (pu.descrizione, ub.descrizione) as redattore_doc\n"
                + "FROM bds_tools.bolli_virtuali_documenti bv\n"
                + "JOIN procton.documenti pd ON bv.guid_oggetto_origine = pd.guid_documento\n"
                + "LEFT JOIN procton.attori pa ON pd.id_documento = pa.id_documento AND id_task = 'I2ZsZ+9S7tNClAhkd>Rq'\n"
                + "LEFT JOIN procton.utenti pu ON pa.id_utente = pu.id_utente\n"
                + "LEFT JOIN bds_tools.attori a ON a.id_oggetto = pd.guid_documento AND id_step = 'REDAZIONE'\n"
                + "LEFT JOIN procton.utenti ub ON ub.id_utente = a.id_utente\n"
                + "WHERE pd.protocollo IS NOT null\n"
                + "and pd.data_protocollo >= :from\n"
                + "and pd.data_protocollo < :to\n"
                + "UNION\n"
                + "--- dete\n"
                + "SELECT bv.tipo_oggetto as tipo_oggetto_bollo,\n"
                + "dd.codice_registro as codice_registro_doc,\n"
                + "--bv.guid_oggetto_origine as guid_oggetto_origine_doc,\n"
                + "bv.numero_righe as no_righe_bollo,\n"
                + "bv.numero_facciate as no_facciate_bollo,\n"
                + "bv.numero_bolli as no_bolli_altri_importi,\n"
                + "bv.importi as importo_bolli_altri_importi,\n"
                + "dd.numero as numero_doc,\n"
                + "dd.anno as anno_numero_doc,\n"
                + "dd.data_adozione as data_numero_doc,\n"
                + "dd.oggetto as oggetto_doc,\n"
                + "pu.descrizione as redattore_doc\n"
                + "FROM bds_tools.bolli_virtuali_documenti bv\n"
                + "JOIN dete.determine dd ON bv.guid_oggetto_origine = dd.guid_determina\n"
                + "LEFT JOIN dete.attori_determine da ON dd.id_determina = da.id_determina AND id_tasks = 'wl[2n7=K,//C5\\nEEf)T'\n"
                + "LEFT JOIN procton.utenti pu ON da.id_utente = pu.id_utente\n"
                + "WHERE dd.numero IS NOT null\n"
                + "and dd.data_adozione >= :from\n"
                + "and dd.data_adozione < :to\n"
                + "UNION\n"
                + "--- deli\n"
                + "SELECT bv.tipo_oggetto as tipo_oggetto_bollo,\n"
                + "dd.codice_registro as codice_registro_doc,\n"
                + "--bv.guid_oggetto_origine as guid_oggetto_origine_doc,\n"
                + "bv.numero_righe as no_righe_bollo,\n"
                + "bv.numero_facciate as l,\n"
                + "bv.numero_bolli as no_bolli_altri_importi,\n"
                + "bv.importi as importo_bolli_altri_importi,\n"
                + "dd.numero_delibera as numero_doc,\n"
                + "dd.anno as anno_numero_doc,\n"
                + "dd.data_adozione as data_numero_doc,\n"
                + "dd.oggetto as oggetto_doc,\n"
                + "pu.descrizione as redattore_doc\n"
                + "FROM bds_tools.bolli_virtuali_documenti bv\n"
                + "JOIN deli.delibere dd ON bv.guid_oggetto_origine = dd.guid_delibera\n"
                + "LEFT JOIN deli.attori_delibere da ON dd.id_delibera = da.id_delibera AND id_tasks = 'nn`B7KIYZ-ict?6k+D(k'\n"
                + "LEFT JOIN procton.utenti pu ON da.id_utente = pu.id_utente\n"
                + "WHERE dd.numero_delibera IS NOT null\n"
                + "and dd.data_adozione >= :from\n"
                + "and dd.data_adozione < :to";
        return query;
    }

    public static Map<String, String> mapQueryGetDatiBolliVirtuali() {
        Map<String, String> mappings = new HashMap<>();
        mappings.put("tipo_oggetto_bollo", "tipoOggettoBollo");
        mappings.put("codice_registro_doc", "codiceRegistroDoc");
//        mappings.put("guid_oggetto_origine_doc", "guidOggettoOrigineDoc");
        mappings.put("no_righe_bollo", "noRigheBollo");
        mappings.put("no_facciate_bollo", "noFacciateBollo");
        mappings.put("no_bolli_altri_importi", "noBolliAltriImporti");
        mappings.put("importo_bolli_altri_importi", "importoBolliAltriImporti");
        mappings.put("numero_doc", "numeroDoc");
        mappings.put("anno_numero_doc", "annoNumeroDoc");
        mappings.put("data_numero_doc", "dataNumeroDoc");
        mappings.put("oggetto_doc", "oggettoDoc");
        mappings.put("redattore_doc", "redattoreDoc");
        return mappings;
    }
}
