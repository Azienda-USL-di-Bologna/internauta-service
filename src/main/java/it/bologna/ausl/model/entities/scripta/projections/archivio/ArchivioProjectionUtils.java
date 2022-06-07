package it.bologna.ausl.model.entities.scripta.projections.archivio;

import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.PermessoArchivioRepository;
import it.bologna.ausl.internauta.utils.bds.types.CategoriaPermessiStoredProcedure;
import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.internauta.utils.bds.types.PermessoStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.permessi.Entita;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.ArchivioDetail;
import it.bologna.ausl.model.entities.scripta.PermessoArchivio;
import it.bologna.ausl.model.entities.scripta.QPermessoArchivio;
import it.bologna.ausl.model.entities.scripta.views.ArchivioDetailView;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Matteo
 */
@Component
public class ArchivioProjectionUtils {

    @Autowired
    private PermessoArchivioRepository permessoArchivioRepository;

    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;
    
    @Autowired
    private PermissionManager permissionManager;
    
    @Autowired
    private StrutturaRepository strutturaRepository;
    
    @Autowired
    private PersonaRepository personaRepository;
     
    @Autowired
    private UtenteRepository utenteRepository;
    
    @Autowired
    private AziendaRepository aziendaRepository;

    public Boolean getIsArchivioNero(ArchivioDetail archivio) throws BlackBoxPermissionException {
        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Persona persona = authenticatedSessionData.getPerson();
        BooleanExpression filter = QPermessoArchivio.permessoArchivio.idArchivioDetail.id.eq(archivio.getId())
                .and(QPermessoArchivio.permessoArchivio.idPersona.id.eq(persona.getId()))
                .and(QPermessoArchivio.permessoArchivio.idAzienda.id.eq((archivio.getIdAzienda().getId())))
                .and(QPermessoArchivio.permessoArchivio.dataCreazione.eq(archivio.getDataCreazione()));
        PermessoArchivio pA = new PermessoArchivio();
        Iterable<PermessoArchivio> permessiArchivi = permessoArchivioRepository.findAll(filter);
        return !(permessiArchivi.iterator().hasNext());
    }

    public Boolean getIsArchivioNeroView(ArchivioDetailView archivio) throws BlackBoxPermissionException {
        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Persona persona = authenticatedSessionData.getPerson();
        BooleanExpression filter = QPermessoArchivio.permessoArchivio.idArchivioDetail.id.eq(archivio.getId())
                .and(QPermessoArchivio.permessoArchivio.idPersona.id.eq(persona.getId()))
                .and(QPermessoArchivio.permessoArchivio.idAzienda.id.eq((archivio.getIdAzienda().getId())))
                .and(QPermessoArchivio.permessoArchivio.dataCreazione.eq(archivio.getDataCreazione()));
        PermessoArchivio pA = new PermessoArchivio();
        Iterable<PermessoArchivio> permessiArchivi = permessoArchivioRepository.findAll(filter);
        return !(permessiArchivi.iterator().hasNext());
    }

    public Boolean getIsArchivioNero(Archivio archivio) throws BlackBoxPermissionException {
        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Persona persona = authenticatedSessionData.getPerson();
        BooleanExpression filter = QPermessoArchivio.permessoArchivio.idArchivioDetail.id.eq(archivio.getId())
                .and(QPermessoArchivio.permessoArchivio.idPersona.id.eq(persona.getId()))
                .and(QPermessoArchivio.permessoArchivio.idAzienda.id.eq((archivio.getIdAzienda().getId())))
                .and(QPermessoArchivio.permessoArchivio.dataCreazione.eq(archivio.getDataCreazione()));
        PermessoArchivio pA = new PermessoArchivio();
        Iterable<PermessoArchivio> permessiArchivi = permessoArchivioRepository.findAll(filter);
        return !(permessiArchivi.iterator().hasNext());
    }
    
    public String getElencoCodiciAziendeAttualiPersona(Persona persona) {
        String codiciAziende = "";
        List<Utente> utenteList = persona.getUtenteList();
        if (utenteList != null) {
            for (Utente utente : utenteList) {
                utente = utenteRepository.findById(utente.getId()).get();
                if (utente.getAttivo()) {
                    Azienda azienda = aziendaRepository.findById(utente.getIdAzienda().getId()).get();
                    codiciAziende = codiciAziende + (codiciAziende.length() == 0 ? "" : ", ") + azienda.getNome();
                }
            }
        }
        return codiciAziende;
    }
    
    public List<PermessoEntitaStoredProcedure> getPermessi(Archivio archivio) throws BlackBoxPermissionException {
        List<String> predicati = new ArrayList<>();
        predicati.add("VISUALIZZA");
        predicati.add("MODIFICA");
        predicati.add("ELIMINA");
        predicati.add("BLOCCO");
        List<String> ambiti = new ArrayList<>();
        ambiti.add("SCRIPTA");
        List<String> tipi = new ArrayList<>();
        tipi.add("ARCHIVIO");
        List<PermessoEntitaStoredProcedure> subjectsWithPermissionsOnObject = new ArrayList<>();
        subjectsWithPermissionsOnObject = permissionManager.getSubjectsWithPermissionsOnObject(archivio, predicati, ambiti, tipi, Boolean.FALSE,Boolean.TRUE);
        if (subjectsWithPermissionsOnObject != null) {
            for (PermessoEntitaStoredProcedure permessoEntitaStoredProcedure : subjectsWithPermissionsOnObject) {
                if (permessoEntitaStoredProcedure.getSoggetto().getTable().equals(Entita.TabelleTipiEntita.strutture.toString())) {
                    Struttura strutturaSoggetto = strutturaRepository.findById(permessoEntitaStoredProcedure.getSoggetto().getIdProvenienza()).get();
                    permessoEntitaStoredProcedure.getSoggetto().setDescrizione(strutturaSoggetto.getNome()
                            + " [ " + strutturaSoggetto.getIdAzienda().getNome() + (strutturaSoggetto.getCodice() != null ? " - " + strutturaSoggetto.getCodice() : "") + " ]");
                    permessoEntitaStoredProcedure.getSoggetto().setAdditionalData(
                            strutturaRepository.getCountUtentiStruttura(permessoEntitaStoredProcedure.getSoggetto().getIdProvenienza())
                    );
                } else if (permessoEntitaStoredProcedure.getSoggetto().getTable().equals(Entita.TabelleTipiEntita.persone.toString())) {
                    Persona personaSoggetto = personaRepository.findById(permessoEntitaStoredProcedure.getSoggetto().getIdProvenienza()).get();
                    permessoEntitaStoredProcedure.getSoggetto().setDescrizione(personaSoggetto.getDescrizione());
                }

                for (CategoriaPermessiStoredProcedure categoriaPermessiStoredProcedure : permessoEntitaStoredProcedure.getCategorie()) {
                    for (PermessoStoredProcedure permessoStoredProcedure : categoriaPermessiStoredProcedure.getPermessi()) {
                        if (permessoStoredProcedure.getEntitaVeicolante() != null && permessoStoredProcedure.getEntitaVeicolante().getIdProvenienza() != null) {
                            Struttura strutturaVeicolante = strutturaRepository.findById(
                                    permessoStoredProcedure.getEntitaVeicolante().getIdProvenienza()).get();
                            permessoStoredProcedure.getEntitaVeicolante().setDescrizione(strutturaVeicolante.getNome()
                                    + " [ " + strutturaVeicolante.getIdAzienda().getNome() + (strutturaVeicolante.getCodice() != null ? " - " + strutturaVeicolante.getCodice() : "") + " ]");
                        }
                    }
                }
            }
        }
        return subjectsWithPermissionsOnObject;
    }
}
