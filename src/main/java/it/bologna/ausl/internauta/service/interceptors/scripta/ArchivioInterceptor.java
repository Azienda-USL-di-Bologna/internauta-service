package it.bologna.ausl.internauta.service.interceptors.scripta;

import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.service.masterjobs.exceptions.MasterjobsQueuingException;
import it.bologna.ausl.internauta.service.masterjobs.workers.MasterjobsQueuer;
import it.bologna.ausl.internauta.service.masterjobs.workers.calcolopermessiarchivio.CalcoloPermessiArchivioWorker;
import it.bologna.ausl.internauta.service.masterjobs.workers.calcolopermessiarchivio.CalcoloPermessiArchivioWorkerData;
import it.bologna.ausl.internauta.service.repositories.scripta.MassimarioRepository;
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
    private MasterjobsQueuer mjQueuer;

    @Autowired
    private MasterjobsObjectsFactory masterjobsObjectsFactory;
    
    @Override
    public Class getTargetEntityClass() {
        return Archivio.class;
    }

    @Override
    public Object afterCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        Archivio archivio = (Archivio) entity;    
        Integer idArchivio = archivio.getId();
        //caso in cui sono un figlio di un archivio
        if (archivio.getIdArchivioRadice() != null){
            idArchivio=archivio.getIdArchivioRadice().getId();
        }
        //archivioRepository.calcolaPermessiEspliciti(idArchivio);
        Applicazione applicazione = cachedEntities.getApplicazione("scripta");
        CalcoloPermessiArchivioWorker worker = masterjobsObjectsFactory.getWorker(CalcoloPermessiArchivioWorker.class, new CalcoloPermessiArchivioWorkerData(idArchivio), false);
        try {
            mjQueuer.queue(worker,idArchivio.toString(), "scripta_archivio", applicazione.getId(), true, Set.SetPriority.HIGHEST);
        } catch (MasterjobsQueuingException ex) {
            return super.afterCreateEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass);
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
        if (archivioOld.getIdTitolo()!=null && archivioOld.getIdTitolo()!=archivio.getIdTitolo()){
            if (archivioOld.getIdMassimario() != null && archivioOld.getIdMassimario() != archivio.getIdMassimario()) {
                List<Titolo> titoliMassimario = massimarioRepository.getById(archivioOld.getIdMassimario().getId()).getTitoli();
                if (!titoliMassimario.contains(archivio.getIdTitolo())){
                    archivio.setAnniTenuta(null);
                    archivio.setIdMassimario(null);
                }
            }
        }
        
        return super.beforeUpdateEntityInterceptor(archivio, beforeUpdateEntityApplier, additionalData, request, mainEntity, projectionClass);
    }

   
    
}
