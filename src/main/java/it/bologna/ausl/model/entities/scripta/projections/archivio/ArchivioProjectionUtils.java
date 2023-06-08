package it.bologna.ausl.model.entities.scripta.projections.archivio;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.PermessoArchivioRepository;
import it.bologna.ausl.internauta.model.bds.types.CategoriaPermessiStoredProcedure;
import it.bologna.ausl.internauta.model.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.internauta.model.bds.types.PermessoStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.permessi.Entita;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.ArchivioDetail;
import it.bologna.ausl.model.entities.scripta.PermessoArchivio;
import it.bologna.ausl.model.entities.scripta.QArchivio;
import it.bologna.ausl.model.entities.scripta.QPermessoArchivio;
import it.bologna.ausl.model.entities.scripta.projections.generated.PermessoArchivioWithPlainFields;
import it.bologna.ausl.model.entities.scripta.views.ArchivioDetailView;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
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
    private ProjectionFactory projectionFactory;

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
    
    @PersistenceContext
    private EntityManager entityManager;

    public Boolean getIsArchivioNero(ArchivioDetail archivio) throws BlackBoxPermissionException {
        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Persona persona = authenticatedSessionData.getPerson();
        return checkIfPermessoArchivioExists(persona.getId(), archivio.getId(), archivio.getIdAzienda().getId(), archivio.getDataCreazione());
//        BooleanExpression filter = QPermessoArchivio.permessoArchivio.idArchivioDetail.id.eq(archivio.getId())
//                .and(QPermessoArchivio.permessoArchivio.idPersona.id.eq(persona.getId()))
//                .and(QPermessoArchivio.permessoArchivio.idAzienda.id.eq((archivio.getIdAzienda().getId())))
//                .and(QPermessoArchivio.permessoArchivio.dataCreazione.eq(archivio.getDataCreazione()));
//        PermessoArchivio pA = new PermessoArchivio();
//        Iterable<PermessoArchivio> permessiArchivi = permessoArchivioRepository.findAll(filter);
//        return !(permessiArchivi.iterator().hasNext());
    }

    public Boolean getIsArchivioNeroView(ArchivioDetailView archivio) throws BlackBoxPermissionException {
        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Persona persona = authenticatedSessionData.getPerson();
        return checkIfPermessoArchivioExists(persona.getId(), archivio.getId(), archivio.getIdAzienda().getId(), archivio.getDataCreazione());
//        BooleanExpression filter = QPermessoArchivio.permessoArchivio.idArchivioDetail.id.eq(archivio.getId())
//                .and(QPermessoArchivio.permessoArchivio.idPersona.id.eq(persona.getId()))
//                .and(QPermessoArchivio.permessoArchivio.idAzienda.id.eq((archivio.getIdAzienda().getId())))
//                .and(QPermessoArchivio.permessoArchivio.dataCreazione.eq(archivio.getDataCreazione()));
//        PermessoArchivio pA = new PermessoArchivio();
//        Iterable<PermessoArchivio> permessiArchivi = permessoArchivioRepository.findAll(filter);
//        return !(permessiArchivi.iterator().hasNext());
    }
    
    public Boolean getIsArchivioNero(Archivio archivio) throws BlackBoxPermissionException {
        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Persona persona = authenticatedSessionData.getPerson();
        return checkIfPermessoArchivioExists(persona.getId(), archivio.getId(), archivio.getIdAzienda().getId(), archivio.getDataCreazione());
    }
    
    private Boolean checkIfPermessoArchivioExists(Integer idPersona, Integer idArchivio, Integer idAzienda, ZonedDateTime dataCreazione) {
        QPermessoArchivio qPermessoArchivio = QPermessoArchivio.permessoArchivio;
//        QArchivio qArchivio = QArchivio.archivio;
        
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
//        Integer fetchFirst = jPAQueryFactory
//                .selectOne()
//                .from(qArchivio)
//                .join(qPermessoArchivio).on(
//                        qArchivio.id.eq(qPermessoArchivio.idArchivioDetail.id)
//                                .and(qArchivio.idAzienda.id.eq(qPermessoArchivio.idAzienda.id))
//                                .and(qArchivio.dataCreazione.eq(qPermessoArchivio.dataCreazione))
//                )
//                .where(qPermessoArchivio.idPersona.id.eq(idPersona)
//                    .and(qArchivio.id.eq(idArchivio)))
//                .fetchFirst();
        Integer fetchFirst = jPAQueryFactory
                .selectOne()
                .from(qPermessoArchivio)
                .where(
                    qPermessoArchivio.idPersona.id.eq(idPersona)
                    .and(qPermessoArchivio.idArchivioDetail.id.eq(idArchivio))
                    .and(qPermessoArchivio.idAzienda.id.eq(idAzienda))
                    .and(qPermessoArchivio.dataCreazione.eq(dataCreazione))
                )
                .fetchFirst();
        return fetchFirst == null;
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
        predicati.add("PASSAGGIO"); // 1
        predicati.add("VISUALIZZA"); // 2
        predicati.add("MODIFICA"); // 4
        predicati.add("ELIMINA"); // 8
        predicati.add("VICARIO"); // 16
        predicati.add("REPONSABILE_PROPOSTO"); // 32
        predicati.add("RESPONSABILE"); // 64
        predicati.add("NON_PROPAGATO"); // E' un permesso che blocca il permesso in id_permesso_bloccato
        predicati.add("BLOCCO"); // E' un permesso negativo. Se ad esempio un utente riceve un permesso da struttura ma non si vuole che veda l'archivio allora basta dargli questo permesso
        List<String> ambiti = new ArrayList<>();
        ambiti.add("SCRIPTA");
        List<String> tipi = new ArrayList<>();
        tipi.add("ARCHIVIO");
        List<PermessoEntitaStoredProcedure> subjectsWithPermissionsOnObject = new ArrayList<>();
        subjectsWithPermissionsOnObject = permissionManager.getSubjectsWithPermissionsOnObject(archivio, predicati, ambiti, tipi, Boolean.FALSE, Boolean.TRUE);
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
                            Optional<Struttura> strutturaVeicolanteOptional
                                    = strutturaRepository.findById(
                                            permessoStoredProcedure.getEntitaVeicolante().getIdProvenienza());
                            if (strutturaVeicolanteOptional != null && strutturaVeicolanteOptional.isPresent()) {
                                Struttura strutturaVeicolante = strutturaVeicolanteOptional.get();
                                permessoStoredProcedure.getEntitaVeicolante().setDescrizione(strutturaVeicolante.getNome()
                                        + " [ " + strutturaVeicolante.getIdAzienda().getNome() + (strutturaVeicolante.getCodice() != null ? " - " + strutturaVeicolante.getCodice() : "") + " ]");
                            }

                        }
                    }
                }
            }
        }
        return subjectsWithPermissionsOnObject;
    }
    
    /**
     * Dato un archivio torno la lista dei PermessoArchivio che ci sono sull'arhciviodetail corrispondente
     * @param archivio
     * @return
     */
    public List<PermessoArchivioWithPlainFields> getPermessiEspliciti(Archivio archivio) {
        BooleanExpression filter = QPermessoArchivio.permessoArchivio.idArchivioDetail.id.eq(archivio.getId())
                .and(QPermessoArchivio.permessoArchivio.idAzienda.id.eq((archivio.getIdAzienda().getId())))
                .and(QPermessoArchivio.permessoArchivio.dataCreazione.eq(archivio.getDataCreazione()));
        Iterable<PermessoArchivio> permessiArchivio = permessoArchivioRepository.findAll(filter);
        List<PermessoArchivio> permessiArchiviList = new ArrayList<>();
        permessiArchivio.forEach(permessiArchiviList::add);
        List<PermessoArchivioWithPlainFields> res = null;
        if (!permessiArchiviList.isEmpty()) {
            res = permessiArchiviList.stream().map(pa -> {
                return projectionFactory.createProjection(PermessoArchivioWithPlainFields.class, pa);                
            }).collect(Collectors.toList());
        }
        return res;
    }
}
