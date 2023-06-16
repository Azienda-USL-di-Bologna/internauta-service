package it.bologna.ausl.internauta.service.rubrica.utils.similarity;

import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.blackbox.utils.BlackBoxConstants;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.model.bds.types.PermessoStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Top
 */
public class SqlSimilarityResults {

    public static enum ContactListInclude {
        RISERVATI,
        PUBBLICI,
        ALL
    }

    private List<SqlSimilarityResult> emailList;
    // private List<SqlSimilarityResult> indirizzo;
    private List<SqlSimilarityResult> codiceFiscale;
    private List<SqlSimilarityResult> cognomeAndNome;
    private List<SqlSimilarityResult> partitaIva;
    private List<SqlSimilarityResult> ragioneSociale;

    public SqlSimilarityResults() {
    }

    public List<SqlSimilarityResult> getEmailList() {
        return emailList;
    }

    public void setEmailList(List<SqlSimilarityResult> emailList) {
        this.emailList = emailList;
    }

    public List<SqlSimilarityResult> getRagioneSociale() {
        return ragioneSociale;
    }

    public void setRagioneSociale(List<SqlSimilarityResult> ragioneSociale) {
        this.ragioneSociale = ragioneSociale;
    }

//    public List<SqlSimilarityResult> getIndirizzo() {
//        return indirizzo;
//    }
//
//    public void setIndirizzo(List<SqlSimilarityResult> indirizzo) {
//        this.indirizzo = indirizzo;
//    }
    public List<SqlSimilarityResult> getCodiceFiscale() {
        return codiceFiscale;
    }

    public void setCodiceFiscale(List<SqlSimilarityResult> codiceFiscale) {
        this.codiceFiscale = codiceFiscale;
    }

    public List<SqlSimilarityResult> getCognomeAndNome() {
        return cognomeAndNome;
    }

    public void setCognomeAndNome(List<SqlSimilarityResult> cognomeAndNome) {
        this.cognomeAndNome = cognomeAndNome;
    }

    public List<SqlSimilarityResult> getPartitaIva() {
        return partitaIva;
    }

    public void setPartitaIva(List<SqlSimilarityResult> partitaIva) {
        this.partitaIva = partitaIva;
    }

    public void filterByPermission(Persona persona, PermissionManager permissionManager) throws BlackBoxPermissionException {

        List<Object> contatti = new ArrayList<Object>(getContatti(SqlSimilarityResults.ContactListInclude.RISERVATI));

        Map<Integer, PermessoStoredProcedure> mappa = null;
        try {
            Map<String, Map<Integer, PermessoStoredProcedure>> mapOfPermissionsOfSubjectAdvanced = permissionManager.getMapOfPermissionsOfSubjectAdvanced(persona, contatti,
                    Arrays.asList("ACCESSO"), InternautaConstants.Permessi.Ambiti.RUBRICA.toString(),
                    InternautaConstants.Permessi.Tipi.CONTATTO.toString(), false,
                    LocalDate.now(), null, BlackBoxConstants.Direzione.PRESENTE);
            if (mapOfPermissionsOfSubjectAdvanced != null) {
                mappa = mapOfPermissionsOfSubjectAdvanced.get("ACCESSO");
            }
        } catch (Exception ex) {
            throw new BlackBoxPermissionException(ex);
        }
        if (emailList != null) {
            List<SqlSimilarityResult> res = new ArrayList();
            for (SqlSimilarityResult sqlSimilarityResult : emailList) {
                if (!sqlSimilarityResult.getContact().isRiservato() || (sqlSimilarityResult.getContact().isRiservato() && sqlSimilarityResult.getContact().getId_persona_creazione().equals(persona.getId())) || (mappa != null && mappa.containsKey(sqlSimilarityResult.getContact().getId()))) {
                    res.add(sqlSimilarityResult);
                }
            }
            emailList = res;
        }
        if (codiceFiscale != null) {
            List<SqlSimilarityResult> res = new ArrayList();
            for (SqlSimilarityResult sqlSimilarityResult : codiceFiscale) {
                if (!sqlSimilarityResult.getContact().isRiservato() || (sqlSimilarityResult.getContact().isRiservato() && sqlSimilarityResult.getContact().getId_persona_creazione().equals(persona.getId())) || (mappa != null && mappa.containsKey(sqlSimilarityResult.getContact().getId()))) {
                    res.add(sqlSimilarityResult);
                }
            }
            codiceFiscale = res;
        }
        if (cognomeAndNome != null) {
            List<SqlSimilarityResult> res = new ArrayList();
            for (SqlSimilarityResult sqlSimilarityResult : cognomeAndNome) {
                if (!sqlSimilarityResult.getContact().isRiservato() || (sqlSimilarityResult.getContact().isRiservato() && sqlSimilarityResult.getContact().getId_persona_creazione().equals(persona.getId())) || (mappa != null && mappa.containsKey(sqlSimilarityResult.getContact().getId()))) {
                    res.add(sqlSimilarityResult);
                }
            }
            cognomeAndNome = res;
        }
        if (partitaIva != null) {
            List<SqlSimilarityResult> res = new ArrayList();
            for (SqlSimilarityResult sqlSimilarityResult : partitaIva) {
                if (!sqlSimilarityResult.getContact().isRiservato() || (sqlSimilarityResult.getContact().isRiservato() && sqlSimilarityResult.getContact().getId_persona_creazione().equals(persona.getId())) || (mappa != null && mappa.containsKey(sqlSimilarityResult.getContact().getId()))) {
                    res.add(sqlSimilarityResult);
                }
            }
            partitaIva = res;
        }
        if (ragioneSociale != null) {
            List<SqlSimilarityResult> res = new ArrayList();
            for (SqlSimilarityResult sqlSimilarityResult : ragioneSociale) {
                if (!sqlSimilarityResult.getContact().isRiservato() || (sqlSimilarityResult.getContact().isRiservato() && sqlSimilarityResult.getContact().getId_persona_creazione().equals(persona.getId())) || (mappa != null && mappa.containsKey(sqlSimilarityResult.getContact().getId()))) {
                    res.add(sqlSimilarityResult);
                }
            }
            ragioneSociale = res;
        }

    }

    public List<Contatto> getContatti(ContactListInclude contactListInclude) {
        List<Contatto> res = new ArrayList();
        if (emailList != null) {
            for (SqlSimilarityResult sqlSimilarityResult : emailList) {
                if (contactListInclude == ContactListInclude.ALL
                        || (contactListInclude == ContactListInclude.RISERVATI && sqlSimilarityResult.getContact().isRiservato())
                        || (contactListInclude == ContactListInclude.PUBBLICI && !sqlSimilarityResult.getContact().isRiservato())) {
                    Contatto c = new Contatto(sqlSimilarityResult.getContact().getId());
                    res.add(c);
                }
            }
        }

        if (codiceFiscale != null) {
            for (SqlSimilarityResult sqlSimilarityResult : codiceFiscale) {
                if (contactListInclude == ContactListInclude.ALL
                        || (contactListInclude == ContactListInclude.RISERVATI && sqlSimilarityResult.getContact().isRiservato())
                        || (contactListInclude == ContactListInclude.PUBBLICI && !sqlSimilarityResult.getContact().isRiservato())) {
                    Contatto c = new Contatto(sqlSimilarityResult.getContact().getId());
                    res.add(c);
                }
            }
        }

        if (cognomeAndNome != null) {
            for (SqlSimilarityResult sqlSimilarityResult : cognomeAndNome) {
                if (contactListInclude == ContactListInclude.ALL
                        || (contactListInclude == ContactListInclude.RISERVATI && sqlSimilarityResult.getContact().isRiservato())
                        || (contactListInclude == ContactListInclude.PUBBLICI && !sqlSimilarityResult.getContact().isRiservato())) {
                    Contatto c = new Contatto(sqlSimilarityResult.getContact().getId());
                    res.add(c);
                }
            }
        }

        if (partitaIva != null) {
            for (SqlSimilarityResult sqlSimilarityResult : partitaIva) {
                if (contactListInclude == ContactListInclude.ALL
                        || (contactListInclude == ContactListInclude.RISERVATI && sqlSimilarityResult.getContact().isRiservato())
                        || (contactListInclude == ContactListInclude.PUBBLICI && !sqlSimilarityResult.getContact().isRiservato())) {
                    Contatto c = new Contatto(sqlSimilarityResult.getContact().getId());
                    res.add(c);
                }
            }
        }

        if (ragioneSociale != null) {
            for (SqlSimilarityResult sqlSimilarityResult : ragioneSociale) {
                if (contactListInclude == ContactListInclude.ALL
                        || (contactListInclude == ContactListInclude.RISERVATI && sqlSimilarityResult.getContact().isRiservato())
                        || (contactListInclude == ContactListInclude.PUBBLICI && !sqlSimilarityResult.getContact().isRiservato())) {
                    Contatto c = new Contatto(sqlSimilarityResult.getContact().getId());
                    res.add(c);
                }
            }
        }

        return res;
    }

    public Integer similaritiesNumber() {
        Integer res = 0;
        if (emailList != null) {
            res += emailList.size();
        }
        if (codiceFiscale != null) {
            res += codiceFiscale.size();
        }
        if (cognomeAndNome != null) {
            res += cognomeAndNome.size();
        }
        if (partitaIva != null) {
            res += partitaIva.size();
        }
        if (ragioneSociale != null) {
            res += ragioneSociale.size();
        }

        return res;
    }
    
    public void removeSimileById(Integer idSimileDaRimuovere) {
        if (emailList != null) {
            emailList.removeIf(e -> e.getIdContact().equals(idSimileDaRimuovere));
        }
        if (codiceFiscale != null) {
            codiceFiscale.removeIf(e -> e.getIdContact().equals(idSimileDaRimuovere));
        }
        if (cognomeAndNome != null) {
            cognomeAndNome.removeIf(e -> e.getIdContact().equals(idSimileDaRimuovere));
        }
        if (partitaIva != null) {
            partitaIva.removeIf(e -> e.getIdContact().equals(idSimileDaRimuovere));
        }
        if (ragioneSociale != null) {
            ragioneSociale.removeIf(e -> e.getIdContact().equals(idSimileDaRimuovere));
        }
    }
}
