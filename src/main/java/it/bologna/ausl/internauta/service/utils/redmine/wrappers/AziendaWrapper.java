/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils.redmine.wrappers;

/**
 *
 * @author Salo
 */
public class AziendaWrapper {

    final static String AUSL_BOLOGNA = "AUSL Bologna";
    final static String AOSP_BOLOGNA = "AOSP BO (S.Orsola)";
    final static String AUSL_FERRARA = "AUSL Ferrara";
    final static String AOSP_FERRARA = "AOSP Ferrara";
    final static String AUSL_IMOLA = "AUSL Imola";
    final static String AUSL_PARMA = "ASUL Parma";
    final static String AOSP_PARMA = "AOSP Parma";
    final static String IOR = "IOR (Rizzoli)";
    final static String TUTTE = "TUTTE";

    public static String convertAziendaToAvailableValues(String internautaAziendaDescription) {
        String aziendaToReturn = "";
        switch (internautaAziendaDescription) {
            case "Azienda USL Parma":
                aziendaToReturn = AUSL_PARMA.toString();
                break;
            case "Azienda USL Bologna":
                aziendaToReturn = AUSL_BOLOGNA.toString();
                break;
            case "Azienda USL Imola":
                aziendaToReturn = AUSL_IMOLA.toString();
                break;
            case "Azienda USL Ferrara":
                aziendaToReturn = AUSL_FERRARA.toString();
                break;
            case "Azienda Ospedaliera Parma":
                aziendaToReturn = AOSP_PARMA.toString();
                break;
            case "Azienda Ospedaliera Bologna":
                aziendaToReturn = AOSP_BOLOGNA.toString();
                break;
            case "Azienda Ospedaliera Ferrara":
                aziendaToReturn = AOSP_FERRARA.toString();
                break;
            case "Istituto Ortopedico Rizzoli":
                aziendaToReturn = IOR.toString();
                break;
            default:
                aziendaToReturn = TUTTE;
                break;

        }
        return aziendaToReturn;
    }

}
