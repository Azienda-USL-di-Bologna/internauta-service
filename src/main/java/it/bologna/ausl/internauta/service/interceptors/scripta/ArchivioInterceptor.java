package it.bologna.ausl.internauta.service.interceptors.scripta;

import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.MassimarioRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MasterjobsJobsQueuer;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.utils.AccodatoreVeloce;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.Massimario;
import it.bologna.ausl.model.entities.scripta.QMassimario;
import it.bologna.ausl.model.entities.scripta.QTitolo;
import it.bologna.ausl.model.entities.scripta.Titolo;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.controller.BeforeUpdateEntityApplier;
import it.nextsw.common.controller.exceptions.BeforeUpdateEntityApplierException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import java.util.Arrays;
import java.util.HashSet;
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
@NextSdrInterceptor(name = "archivio-interceptor")
public class ArchivioInterceptor extends InternautaBaseInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchivioInterceptor.class);

    @Autowired
    private MassimarioRepository massimarioRepository;

    @Autowired
    private ArchivioRepository archivioRepository;
    
    @Autowired
    private MasterjobsJobsQueuer masterjobsJobsQueuer;
    
    @Autowired
    private MasterjobsObjectsFactory masterjobsObjectsFactory;

    @Override
    public Class getTargetEntityClass() {
        return Archivio.class;
    }

    @Override
    public Object afterCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        Archivio archivio = (Archivio) entity;
        Integer idArchivioRadice = archivio.getId();
        //caso in cui sono un figlio di un archivio
        if (archivio.getIdArchivioRadice() != null) {
            idArchivioRadice = archivio.getIdArchivioRadice().getId();
        }
        Applicazione applicazione = cachedEntities.getApplicazione("scripta");
        AccodatoreVeloce accodatoreVeloce = new AccodatoreVeloce(masterjobsJobsQueuer, masterjobsObjectsFactory);
        try {
            accodatoreVeloce.accodaCalcolaPermessiArchivio(idArchivioRadice, idArchivioRadice.toString(), "scripta_archivio", applicazione);
            accodatoreVeloce.accodaCalcolaPersoneVedentiDaArchiviRadice(new HashSet(Arrays.asList(idArchivioRadice)), idArchivioRadice.toString(), "scripta_archivio", applicazione);
        } catch (MasterjobsWorkerException ex) {
            throw new AbortSaveInterceptorException(ex);
        }
        
        return super.afterCreateEntityInterceptor(entity, additionalData, request, mainEntity   , projectionClass);
    }

    @Override
    public Object beforeUpdateEntityInterceptor(Object entity, BeforeUpdateEntityApplier beforeUpdateEntityApplier, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {

        Archivio archivio = (Archivio) entity;
        Archivio archivioOld;
        try {
            archivioOld = super.getBeforeUpdateEntity(beforeUpdateEntityApplier, Archivio.class);
        } catch (BeforeUpdateEntityApplierException ex) {
            throw new AbortSaveInterceptorException("errore nell'ottenimento di beforeUpdateEntity di Archivio", ex);
        }
        
        if (archivio.getIdTitolo() == null) {
            archivio.setAnniTenuta(null);
            archivio.setIdMassimario(null);
        } else if (!archivioOld.getIdTitolo().getId().equals(archivio.getIdTitolo().getId()) && archivio.getIdMassimario() != null) {
            // Devo verificare che il nuovo titolo sia associato al massimario
            QMassimario qMassimario = QMassimario.massimario;
            Optional<Massimario> findOne = massimarioRepository.findOne(
                    qMassimario.titoli.contains(archivio.getIdTitolo())
                            .and(qMassimario.id.eq(archivio.getIdMassimario().getId())));
            if (!findOne.isPresent()) {
                archivio.setAnniTenuta(null);
                archivio.setIdMassimario(null);
            }
        }
        
//        if (archivioOld.getIdTitolo() != null && (Integer.compare(archivioOld.getIdTitolo().getId(), archivio.getIdTitolo().getId()) != 0)) {
//            if (archivioOld.getIdMassimario() != null && (Integer.compare(archivioOld.getIdMassimario().getId(), archivio.getIdMassimario().getId()) != 0)) {
//                List<Titolo> titoliMassimario = massimarioRepository.getById(archivioOld.getIdMassimario().getId()).getTitoli();
//                if (!titoliMassimario.contains(archivio.getIdTitolo())) {
//                    archivio.setAnniTenuta(null);
//                    archivio.setIdMassimario(null);
//                }
//            }
//        }
        
        List<InternautaConstants.AdditionalData.OperationsRequested> operationsRequested = InternautaConstants.AdditionalData.getOperationRequested(InternautaConstants.AdditionalData.Keys.OperationRequested, additionalData);
        if (operationsRequested != null && !operationsRequested.isEmpty()) {
            for (InternautaConstants.AdditionalData.OperationsRequested operationRequested : operationsRequested) {
                switch (operationRequested) {
                    case CloseOrReopenArchive:
                        try {
                            if (!archivio.getStato().equals(Archivio.StatoArchivio.APERTO)) {
                                archivioRepository.eliminaBozzeDaArchivioRadice(Archivio.StatoArchivio.BOZZA.toString(), archivioOld.getId());
                            }
                            archivioRepository.chiudiRiapriArchivioRadice(archivio.getStato().toString(), archivioOld.getId());
                        } catch (Exception ex) {
                            throw new AbortSaveInterceptorException("errore nel cambio di stato dell'archivio", ex);
                        }
                        break;
                }
            }
        }
        return super.beforeUpdateEntityInterceptor(archivio, beforeUpdateEntityApplier, additionalData, request, mainEntity, projectionClass);
    }
}
