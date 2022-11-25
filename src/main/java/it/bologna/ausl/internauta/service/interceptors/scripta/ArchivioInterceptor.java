package it.bologna.ausl.internauta.service.interceptors.scripta;

import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioRepository;
import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsQueuingException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MasterjobsJobsQueuer;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolopermessiarchivio.CalcoloPermessiArchivioJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.calcolopermessiarchivio.CalcoloPermessiArchivioJobWorkerData;
import it.bologna.ausl.internauta.service.repositories.scripta.MassimarioRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.masterjobs.Set;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.Titolo;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.controller.BeforeUpdateEntityApplier;
import it.nextsw.common.controller.exceptions.BeforeUpdateEntityApplierException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import java.util.List;
import java.util.Map;
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
    private MasterjobsJobsQueuer mjQueuer;

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
        //archivioRepository.calcolaPermessiEspliciti(idArchivio);
        Applicazione applicazione = cachedEntities.getApplicazione("scripta");
        CalcoloPermessiArchivioJobWorker worker = masterjobsObjectsFactory.getJobWorker(
                CalcoloPermessiArchivioJobWorker.class,
                new CalcoloPermessiArchivioJobWorkerData(idArchivioRadice),
                false
        );
        try {
            mjQueuer.queue(worker, idArchivioRadice.toString(), "scripta_archivio", applicazione.getId(), true, Set.SetPriority.HIGHEST);
        } catch (MasterjobsQueuingException ex) {
            String er = "Errore nella creazione del job CalcoloPermessiArchivio";
            LOGGER.error(er);
            throw new AbortSaveInterceptorException(er, ex);
        }
        return super.afterCreateEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass);
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

        List<InternautaConstants.AdditionalData.OperationsRequested> operationsRequested = InternautaConstants.AdditionalData.getOperationRequested(InternautaConstants.AdditionalData.Keys.OperationRequested, additionalData);
        if (operationsRequested != null && !operationsRequested.isEmpty()) {
            for (InternautaConstants.AdditionalData.OperationsRequested operationRequested : operationsRequested) {
                switch (operationRequested) {
                    case CloseArchivio:
                        try {
                        boolean status = archivioRepository.chiudiRiapriArchivio(archivioOld.getId(), archivioOld.getLivello(), archivio.getStato().toString());
                        if (!status) {
                            throw new Exception();
                        }
                    } catch (Exception e) {
                        LOGGER.error("Errore nel cambio di stato dell'archivio", e);
                    }
                }
            }
        }

//        if (archivioOld.getIdTitolo() != null && archivioOld.getIdTitolo() != archivio.getIdTitolo()) {
//            if (archivioOld.getIdMassimario() != null && archivioOld.getIdMassimario() != archivio.getIdMassimario()) {
//                List<Titolo> titoliMassimario = massimarioRepository.getById(archivioOld.getIdMassimario().getId()).getTitoli();
//                if (!titoliMassimario.contains(archivio.getIdTitolo())) {
//                    archivio.setAnniTenuta(null);
//                    archivio.setIdMassimario(null);
//                }
//            }
//        }
        return super.beforeUpdateEntityInterceptor(archivio, beforeUpdateEntityApplier, additionalData, request, mainEntity, projectionClass);
    }
}
