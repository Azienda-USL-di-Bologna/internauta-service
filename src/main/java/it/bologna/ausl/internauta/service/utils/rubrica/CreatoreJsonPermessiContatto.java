/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils.rubrica;

import it.nextsw.common.utils.EntityReflectionUtils;
import java.lang.reflect.InvocationTargetException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import javax.persistence.Table;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Salo
 */
public class CreatoreJsonPermessiContatto {

    public static JSONObject generaJSONObjectPerAggiuntaPermessiSuOggettoContatto(Object soggetto, Object oggetto) throws Throwable {
        JSONObject jsonObjectPermessoDaTornare = new JSONObject();
        putJsonPermessiEntitaInJSONObject(jsonObjectPermessoDaTornare, soggetto, oggetto);
        putEmptyJsonArrayPermessiAggiuntiInJSONObject(jsonObjectPermessoDaTornare);
        putJsonAmbitiInteressatiInJSONObject(jsonObjectPermessoDaTornare);
        putJsonTipiInteressatiInJSONObject(jsonObjectPermessoDaTornare);
        return jsonObjectPermessoDaTornare;
    }

    private static JSONObject putJsonPermessiEntitaInJSONObject(JSONObject jsonObjectPermessoDaTornare, Object soggetto, Object oggetto) throws Throwable {
        JSONObject soggettoPermesso = getJsonEntitaPermessoByObject(soggetto);
        JSONObject oggettoPermesso = getJsonEntitaPermessoByObject(oggetto);
        jsonObjectPermessoDaTornare.put("permessiEntita", getJsonPermessiEntita(soggettoPermesso, oggettoPermesso));
        return jsonObjectPermessoDaTornare;
    }

    private static JSONObject putJsonAmbitiInteressatiInJSONObject(JSONObject jsonObjectPermessoDaTornare) throws Throwable {
        JSONArray ambitiInteressati = new JSONArray();
        ambitiInteressati.put("RUBRICA");
        jsonObjectPermessoDaTornare.put("ambitiInteressati", ambitiInteressati);
        return jsonObjectPermessoDaTornare;
    }

    private static JSONObject putJsonTipiInteressatiInJSONObject(JSONObject jsonObjectPermessoDaTornare) throws Throwable {
        JSONArray tipiInteressati = new JSONArray();
        tipiInteressati.put("CONTATTO");
        jsonObjectPermessoDaTornare.put("tipiInteressati", tipiInteressati);
        return jsonObjectPermessoDaTornare;
    }

    private static JSONObject putEmptyJsonArrayPermessiAggiuntiInJSONObject(JSONObject jsonObjectPermessoDaTornare) throws Throwable {
        jsonObjectPermessoDaTornare.put("permessiAggiunti", new JSONArray());
        return jsonObjectPermessoDaTornare;
    }

    private static JSONObject getJsonEntitaPermessoByObject(Object object) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Table[] tableAnnotations = object.getClass().getAnnotationsByType(javax.persistence.Table.class);
        Table table = tableAnnotations[0];
        JSONObject obj = new JSONObject();
        obj.put("schema", table.schema());
        obj.put("table", table.name());
        Integer id = (Integer) EntityReflectionUtils.getPrimaryKeyGetMethod(object).invoke(object);
        obj.put("id_provenienza", id);
        return obj;
    }

    private static JSONArray getJSONArrayPermessi() {
        JSONObject obj = new JSONObject();
        obj.put("predicato", "ACCESSO");
        obj.put("propaga_soggetto", false);
        obj.put("propaga_oggetto", false);
        obj.put("origine_permesso", "rubrica");
        //obj.put("id_permesso_bloccato", "");
        obj.put("virtuale", false);
        obj.put("attivo_dal", java.time.LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_DATE_TIME));
        //obj.put("attivo_al", "");

        JSONArray categorieJSONArray = new JSONArray();
        categorieJSONArray.put(obj);
        return categorieJSONArray;
    }

    private static JSONArray getJSONArrayCategorie() {
        JSONObject obj = new JSONObject();
        obj.put("ambito", "RUBRICA");
        obj.put("tipo", "CONTATTO");
        obj.put("permessi", getJSONArrayPermessi());

        JSONArray categorieJSONArray = new JSONArray();
        categorieJSONArray.put(obj);
        return categorieJSONArray;
    }

    private static JSONArray getJsonPermessiEntita(JSONObject soggetto, JSONObject oggetto) {
        JSONArray permessiEntitaJSONArray = new JSONArray();
        JSONObject obj = new JSONObject();
        obj.put("soggetto", soggetto);
        obj.put("oggetto", oggetto);
        obj.put("categorie", getJSONArrayCategorie());
        permessiEntitaJSONArray.put(obj);
        return permessiEntitaJSONArray;
    }
}
