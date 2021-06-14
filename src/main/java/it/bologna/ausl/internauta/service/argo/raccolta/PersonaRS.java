package it.bologna.ausl.internauta.service.argo.raccolta;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;
import it.bologna.ausl.model.entities.rubrica.Email;
import it.bologna.ausl.model.entities.rubrica.Indirizzo;
import it.bologna.ausl.model.entities.rubrica.Telefono;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Next
 */
public class PersonaRS {

    public enum Tipologia {
        FISICA("FISICA"),
        GIURIDICA("GIURIDICA");

        private final String val;

        private Tipologia(String value) {
            this.val = value;
        }

        private String getValue() {
            return val;
        }
    }

    private String descrizione;
    private String nome;
    private String cognome;
    private String codice_fiscale;
    private String p_iva;
    private String email;
    private Tipologia tipologia;
    private boolean salvaContatto;
    private String ragione_sociale;
    private String cap;
    private String comune;
    private String via;
    private String telefono;
    private String civico;
    private String provincia;
    private String nazione;

    public PersonaRS() {
    }

    public PersonaRS(String descrizione, String nome, String cognome,
            String codice_fiscale, String p_iva, String email,
            Tipologia tipologia, boolean salvaContatto, String ragione_sociale,
            String cap, String comune, String via, String civico,
            String telefono, String provincia, String nazione) {
        this.descrizione = descrizione;
        this.nome = nome;
        this.cognome = cognome;
        this.codice_fiscale = codice_fiscale;
        this.p_iva = p_iva;
        this.email = email;
        this.tipologia = tipologia;
        this.salvaContatto = salvaContatto;
        this.ragione_sociale = ragione_sociale;
        this.cap = cap;
        this.comune = comune;
        this.via = via;
        this.civico = civico;
        this.telefono = telefono;
        this.provincia = provincia;
        this.nazione = nazione;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public String getCodice_fiscale() {
        return codice_fiscale;
    }

    public void setCodice_fiscale(String codice_fiscale) {
        this.codice_fiscale = codice_fiscale;
    }

    public String getP_iva() {
        return p_iva;
    }

    public void setP_iva(String p_iva) {
        this.p_iva = p_iva;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Tipologia getTipologia() {
        return tipologia;
    }

    public void setTipologia(Tipologia tipologia) {
        this.tipologia = tipologia;
    }

    public boolean isSalvaContatto() {
        return salvaContatto;
    }

    public void setSalvaContatto(boolean salvaContatto) {
        this.salvaContatto = salvaContatto;
    }

    public String getRagione_sociale() {
        return ragione_sociale;
    }

    public void setRagione_sociale(String ragione_sociale) {
        this.ragione_sociale = ragione_sociale;
    }

    public String getCap() {
        return cap;
    }

    public void setCap(String cap) {
        this.cap = cap;
    }

    public String getComune() {
        return comune;
    }

    public void setComune(String comune) {
        this.comune = comune;
    }

    public String getVia() {
        return via;
    }

    public void setVia(String via) {
        this.via = via;
    }

    public String getCivico() {
        return civico;
    }

    public void setCivico(String civico) {
        this.civico = civico;
    }

    public boolean isValid() {
        boolean res = true;
        if (tipologia == null) {
            res = false;
        }
        return res;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getProvincia() {
        return provincia;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }

    public String getNazione() {
        return nazione;
    }

    public void setNazione(String nazione) {
        this.nazione = nazione;
    }

    public static List<PersonaRS> parse(ObjectMapper objectMapper, String src) throws IOException {
        return objectMapper.readValue(src, new TypeReference<List<PersonaRS>>() {
        });
    }

    public static String dumpToString(ObjectMapper objectMapper, PersonaRS p) throws JsonProcessingException {
        return objectMapper.writeValueAsString(p);
    }

    public static Contatto toContatto(Integer idAzienda, PersonaRS p, Persona persona, Utente utente, Boolean isProtocontatto) {

        Contatto contatto = new Contatto();
        contatto.setCategoria(Contatto.CategoriaContatto.ESTERNO);

        contatto.setIdUtenteCreazione(utente);
        contatto.setIdPersona(persona);
        contatto.setProvenienza("baborg");
        contatto.setModificabile(true);
        contatto.setDaVerificare(isProtocontatto ? true : false);
        contatto.setEliminato(false);
        contatto.setProtocontatto(isProtocontatto);
        contatto.setIdAziende(new Integer[]{idAzienda});
        contatto.setDescrizione(p.getDescrizione());
        if (p.getP_iva() == null) {
            contatto.setTipo(Contatto.TipoContatto.PERSONA_FISICA);
        } else {
            contatto.setPartitaIva(p.getP_iva());
            contatto.setRagioneSociale(p.getRagione_sociale());
            contatto.setTipo(Contatto.TipoContatto.AZIENDA);
        }

        contatto.setRiservato(false);
        contatto.setNome(p.getNome());
        contatto.setCognome(p.getCognome());
        contatto.setCodiceFiscale(p.getCodice_fiscale());
        contatto.setRagioneSociale(p.getRagione_sociale());

        // mail
        if (p.getEmail() != null && !p.getEmail().isEmpty()) {
            DettaglioContatto dettaglioContatto = new DettaglioContatto();
            dettaglioContatto.setDescrizione(p.getEmail());
            dettaglioContatto.setEliminato(false);
            dettaglioContatto.setPrincipale(false);
            dettaglioContatto.setTipo(DettaglioContatto.TipoDettaglio.EMAIL);
            dettaglioContatto.setIdContatto(contatto);

            List<Email> emailList1 = new ArrayList<>();
            Email email1 = new Email();
            email1.setEmail(p.getEmail());
            email1.setIdDettaglioContatto(dettaglioContatto);
            email1.setIdContatto(contatto);
            email1.setPec(false);
            email1.setPrincipale(false);
            email1.setProvenienza("InternautaBridge");
            emailList1.add(email1);
            contatto.setEmailList(emailList1);
        }

        // indirizzi
        if (p.getVia() != null) {
            DettaglioContatto dettaglioIndirizzo = new DettaglioContatto();
            String descrizione = null;
            String piva = p.getVia() != null ? p.getVia() : "";
            String civico = p.getCivico() != null ? p.getCivico() : "";
            String cap = p.getCap() != null ? p.getCap() : "";
            String comune = p.getComune() != null ? p.getComune() : "";
            if (p.getProvincia() != null && p.getNazione() != null) {
                descrizione = String.format("%s %s, %s %s (%s) %s", piva, civico, cap, comune, p.getProvincia(), p.getNazione());
            } else if (p.getProvincia() == null && p.getNazione() != null) {
                descrizione = String.format("%s %s, %s %s %s", piva, civico, cap, comune, p.getNazione());
            } else {
                descrizione = String.format("%s %s, %s %s", piva, civico, cap, comune);
            }

            dettaglioIndirizzo.setDescrizione(descrizione);
            dettaglioIndirizzo.setEliminato(false);
            dettaglioIndirizzo.setPrincipale(false);
            dettaglioIndirizzo.setTipo(DettaglioContatto.TipoDettaglio.INDIRIZZO_FISICO);
            dettaglioIndirizzo.setIdContatto(contatto);

            Indirizzo indirizzo = new Indirizzo();
            indirizzo.setCap(p.getCap());
            indirizzo.setCivico(p.getCivico());
            indirizzo.setComune(p.getComune());
            indirizzo.setIdDettaglioContatto(dettaglioIndirizzo);
            indirizzo.setIdContatto(contatto);
            indirizzo.setNazione(p.getNazione());
            indirizzo.setPrincipale(false);
            indirizzo.setProvenienza("InternautaBridge");
            indirizzo.setProvincia(p.getProvincia());
            indirizzo.setVia(p.getVia());

            List<Indirizzo> indirizziList = new ArrayList<>();
            indirizziList.add(indirizzo);
            contatto.setIndirizziList(indirizziList);
        }

        // telefono
        if (p.getTelefono() != null && p.getTelefono().isEmpty()) {
            DettaglioContatto dettagliotelefono = new DettaglioContatto();
            dettagliotelefono.setDescrizione(p.getTelefono());
            dettagliotelefono.setEliminato(false);
            dettagliotelefono.setPrincipale(false);
            dettagliotelefono.setTipo(DettaglioContatto.TipoDettaglio.TELEFONO);
            dettagliotelefono.setIdContatto(contatto);

            Telefono telefono = new Telefono();
            telefono.setFax(false);
            telefono.setIdDettaglioContatto(dettagliotelefono);
            telefono.setNumero(p.getTelefono());
            telefono.setPrincipale(false);
            telefono.setProvenienza("InternautaBridge");
            telefono.setIdContatto(contatto);

            List<Telefono> telefoniList = new ArrayList<>();
            telefoniList.add(telefono);
            contatto.setTelefonoList(telefoniList);
        }
        return contatto;
    }

    public static String dumpContatto(ObjectMapper objectMapper, Contatto c) throws JsonProcessingException {
        return objectMapper.writeValueAsString(c);
    }
}
