package it.bologna.ausl.internauta.service.interceptors.scripta;

import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.krint.KrintScriptaService;
import it.bologna.ausl.internauta.service.krint.KrintUtils;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioRecenteRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioDiInteresseRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.DocRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.PermessoArchivioRepository;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.ArchivioRecente;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDetailInterface;
import it.bologna.ausl.model.entities.scripta.PermessoArchivio;
import it.bologna.ausl.model.entities.scripta.QPermessoArchivio;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.controller.BeforeUpdateEntityApplier;
import it.nextsw.common.controller.exceptions.BeforeUpdateEntityApplierException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
@NextSdrInterceptor(name = "archiviodoc-interceptor")
public class ArchivioDocInterceptor extends InternautaBaseInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchivioDocInterceptor.class);

    @Autowired
    DocRepository docRepository;

    @Autowired
    PermessoArchivioRepository permessoArchivioRepository;

    @Autowired
    ArchivioDiInteresseRepository archiviDiInteresseRepository;

    @Autowired
    ArchivioRecenteRepository archivioRecenteRepository;

    @Autowired
    private KrintUtils krintUtils;

    @Autowired
    private KrintScriptaService krintScriptaService;

    @Override
    public Class getTargetEntityClass() {
        return ArchivioDoc.class;
    }

    @Override
    public Object beforeUpdateEntityInterceptor(Object entity, BeforeUpdateEntityApplier beforeUpdateEntityApplier, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        Utente user = authenticatedSessionData.getUser();
        Persona persona = user.getIdPersona();

        ArchivioDoc archivioDoc = (ArchivioDoc) entity;
        Doc idDoc = archivioDoc.getIdDoc();
        ArchivioDoc archivioDocBeforeUpdate;

        try {
            archivioDocBeforeUpdate = super.getBeforeUpdateEntity(beforeUpdateEntityApplier, ArchivioDoc.class);
        } catch (BeforeUpdateEntityApplierException ex) {
            throw new AbortSaveInterceptorException("errore nell'ottenimento di beforeUpdateEntity", ex);
        }

        // Faccio dei controlli di sicurezza
        if (archivioDocBeforeUpdate.getDataEliminazione() == null && archivioDoc.getDataEliminazione() != null) {
            // Sto eliminando logicamente questa archiviazione
            // Ho il permesso di farlo? Devo avere almeno il permesso elimina
            QPermessoArchivio permessoArchivio = QPermessoArchivio.permessoArchivio;
            BooleanExpression filterUserhasPermission = permessoArchivio.idArchivioDetail.id.eq(archivioDoc.getIdArchivio().getId()).and(
                    permessoArchivio.idPersona.id.eq(persona.getId()).and(
                            permessoArchivio.bit.goe(PermessoArchivio.DecimalePredicato.ELIMINA.getValue()))
            );
            Optional<PermessoArchivio> findOne = permessoArchivioRepository.findOne(filterUserhasPermission);

            if (!findOne.isPresent()) {
                throw new AbortSaveInterceptorException("Persona senza permesso su Archivio. Permesso minimo richiesto per eliminare logicamente: ELIMINA");
            }

            // Altro controllo: se l'archiviazione che sto cancellando riguarda un PDD allora deve esserci almeno un'altra archiviazione non cancellata logicamente
            List<DocDetailInterface.TipologiaDoc> tipologiePDD = new ArrayList();
            tipologiePDD.add(DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA);
            tipologiePDD.add(DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA);
            tipologiePDD.add(DocDetailInterface.TipologiaDoc.DETERMINA);
            tipologiePDD.add(DocDetailInterface.TipologiaDoc.DELIBERA);

            if (tipologiePDD.contains(idDoc.getTipologia())) {
                List<ArchivioDoc> archiviDocList = idDoc.getArchiviDocList();
                boolean esisteAltraArchiviazioneNonEliminata = archiviDocList.stream().anyMatch(ad -> {
                    return !ad.getId().equals(archivioDoc.getId()) && ad.getDataEliminazione() == null;
                });
                if (!esisteAltraArchiviazioneNonEliminata) {
                    throw new AbortSaveInterceptorException("Il documento non ha altre fasciolazioni attive");
                }
            }
        } else if (archivioDocBeforeUpdate.getDataEliminazione() != null && archivioDoc.getDataEliminazione() == null) {
            // Sto ripristinando questa archiviazione
            // Ho il permesso di farlo? Devo avere almeno il permesso vicario
            QPermessoArchivio permessoArchivio = QPermessoArchivio.permessoArchivio;
            BooleanExpression filterUserhasPermission = permessoArchivio.idArchivioDetail.id.eq(archivioDoc.getIdArchivio().getId()).and(
                    permessoArchivio.idPersona.id.eq(persona.getId()).and(
                            permessoArchivio.bit.goe(PermessoArchivio.DecimalePredicato.VICARIO.getValue()))
            );
            Optional<PermessoArchivio> findOne = permessoArchivioRepository.findOne(filterUserhasPermission);

            if (!findOne.isPresent()) {
                throw new AbortSaveInterceptorException("Persona senza permesso su Archivio. Permesso minimo richiesto per ripristinare: VICARIO");
            }
        }

        return super.beforeUpdateEntityInterceptor(entity, beforeUpdateEntityApplier, additionalData, request, mainEntity, projectionClass); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object afterCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        Utente user = authenticatedSessionData.getUser();
        Persona persona = user.getIdPersona();

        ArchivioDoc archivioDoc = (ArchivioDoc) entity;
        archiviDiInteresseRepository.aggiungiArchivioRecente(archivioDoc.getIdArchivio().getIdArchivioRadice().getId(), persona.getId());
        /*
        ZonedDateTime data_recentezza = ZonedDateTime.now();
        Optional<ArchivioRecente> archivio = archiviRecentiRepository.getArchivioFromPersonaAndArchivio(archivioDoc.getIdArchivio().getIdArchivioRadice().getId(), persona.getId());
        boolean isPresent = archivio.isPresent();
        if (isPresent) {
            ArchivioRecente archivioUpdate = archivio.get();
            archivioUpdate.setDataRecentezza(data_recentezza);
            archiviRecentiRepository.save(archivioUpdate);
        } else {
            ArchivioRecente archivioUpdate = new ArchivioRecente();
            archivioUpdate.setIdArchivio(archivioDoc.getIdArchivio().getIdArchivioRadice());
            archivioUpdate.setIdPersona(persona);
            archivioUpdate.setDataRecentezza(data_recentezza);
            archiviRecentiRepository.save(archivioUpdate);
        }
         */
        if (krintUtils.doIHaveToKrint(request)) {
            krintScriptaService.writeArchivioDoc(archivioDoc, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_DOC_BY_ADI);
        }
        return super.afterCreateEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }
}
