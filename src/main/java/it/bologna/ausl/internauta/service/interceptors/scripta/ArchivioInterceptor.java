package it.bologna.ausl.internauta.service.interceptors.scripta;

import it.bologna.ausl.internauta.service.controllers.scripta.ScriptaArchiviUtils;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.krint.KrintScriptaService;
import it.bologna.ausl.internauta.service.krint.KrintUtils;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.MassimarioRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.Massimario;
import it.bologna.ausl.model.entities.scripta.QMassimario;
import it.nextsw.common.data.annotations.NextSdrInterceptor;
import it.nextsw.common.controller.BeforeUpdateEntityApplier;
import it.nextsw.common.controller.exceptions.BeforeUpdateEntityApplierException;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
    private KrintScriptaService krintScriptaService;

    @Autowired
    private ParametriAziendeReader parametriAziendeReader;

    @Autowired
    private KrintUtils krintUtils;
    
    @Autowired
    private ScriptaArchiviUtils scriptaArchiviUtils;

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

//      Applicazione applicazione = cachedEntities.getApplicazione("scripta");
//      AccodatoreVeloce accodatoreVeloce = new AccodatoreVeloce(masterjobsJobsQueuer, masterjobsObjectsFactory);
//      try {
//          accodatoreVeloce.accodaCalcolaPermessiArchivio(idArchivioRadice, idArchivioRadice.toString(), "scripta_archivio", applicazione);
//          accodatoreVeloce.accodaCalcolaPersoneVedentiDaArchiviRadice(new HashSet(Arrays.asList(idArchivioRadice)), idArchivioRadice.toString(), "scripta_archivio", applicazione);
//      } catch (MasterjobsWorkerException ex) {
//          throw new AbortSaveInterceptorException(ex);
//      }
        if (krintUtils.doIHaveToKrint(request)) {
            krintScriptaService.writeArchivioCreation(archivio, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_CREATION);
        }

        return super.afterCreateEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass);
    }

    @Override
    public Object beforeUpdateEntityInterceptor(Object entity, BeforeUpdateEntityApplier beforeUpdateEntityApplier, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        Utente user = authenticatedSessionData.getUser();
        Persona persona = user.getIdPersona();
        
        Archivio archivio = (Archivio) entity;
        Archivio archivioOld;
        List<Archivio> listArchivioOld = new ArrayList<>();
        try {
            archivioOld = new Archivio();
            beforeUpdateEntityApplier.beforeUpdateApply(oldEntity -> {
                Archivio archivioVecchio = (Archivio) oldEntity;
                // Load del massimario per il krint, altrimenti non abbiamo i dati nel nuovo oggetto
                if (archivioVecchio.getIdMassimario() != null) { 
                    archivioVecchio.getIdMassimario().getNome();
                    archivioVecchio.getIdMassimario().getDescrizioneTenuta();
                }  
                listArchivioOld.add(archivioVecchio);
                if (archivioVecchio.getIdTitolo() != null) {
                    archivioVecchio.getIdTitolo().getClassificazione();
                }
                //archivioOld.setIdTitolo(((Archivio) oldEntity).getIdTitolo());
                //((Archivio) oldEntity).getIdTitolo().getNome();
//                archivioOld.setIdMassimario(archivioVecchio.getIdMassimario());
            });
        } catch (BeforeUpdateEntityApplierException ex) {
            throw new AbortSaveInterceptorException("errore nell'ottenimento di beforeUpdateEntity di Archivio", ex);
        }

        archivioOld = listArchivioOld.get(0);

        if (archivio.getIdTitolo() == null) {
            archivio.setAnniTenuta(null);
            archivio.setIdMassimario(null);
        } else if (archivioOld.getIdTitolo() != null && !archivioOld.getIdTitolo().getId().equals(archivio.getIdTitolo().getId()) && archivio.getIdMassimario() != null) {
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
        List<Pair<OperazioneKrint.CodiceOperazione, Boolean>> codiciOperazioneWithOldArchivio = new ArrayList();
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
                        
                        /* 
                        se l'azienda ha impostata la chiusura definita del fascicolo, parametro "chiusuraArchivio" in configurazione.parametri_Aziende, allora numero tutti
                        i doc ti tipo document (cioè quelli con tipologia DOCUMENT_UTENTE e DOCUMENT_PEC)
                        */
                        List<ParametroAziende> parameters = parametriAziendeReader.getParameters(ParametriAziendeReader.ParametriAzienda.chiusuraArchivio, new Integer[]{archivio.getIdAzienda().getId()});
                        if (parameters != null && !parameters.isEmpty()) {
                            // se trovo il parametro, sono sicuro che sia una sola riga, dato che ho passato una sola azienda, per cui prendo il primo
                            if (parametriAziendeReader.getValue(parameters.get(0), Boolean.class) == true) {
                                /*
                                se il parametro è true, allora lancio la numerazione di tutti i document sull'archivio.
                                Questa numererà tutti i docs di tipo DOCUMENT_UTENTE e DOCUMENT_PEC di tuti gli archivi della gerarchia a partire dell'archivio radice
                                */
                                LOGGER.info(String.format("numerazione di tutti i document dell'archivio %s...", archivio.getId()));
                                archivioRepository.numeraTuttiDocumentsArchivioRadice(archivio.getId(), persona.getId());
                                LOGGER.info("numerazione completata", archivio.getId());
                            }
                        } else {
                            // se non trovo il parametri non do errore, ma lo considero come false
                            LOGGER.warn(String.format("manca il parametro %s per l'azienda %s, lo considero false", ParametriAziendeReader.ParametriAzienda.chiusuraArchivio, archivio.getIdAzienda().getId()));
                        }
                        codiciOperazioneWithOldArchivio.add(Pair.of(OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_STATO_CHIUSURA_UPDATE, false));
                    } catch (Exception ex) {
                        throw new AbortSaveInterceptorException("errore nel cambio di stato dell'archivio", ex);
                    }
                    break;
                }
            }
        }
        try {
            if (archivio.getOggetto() != null && archivioOld.getOggetto() != null && !(archivio.getOggetto().equals(archivioOld.getOggetto()))) {
                codiciOperazioneWithOldArchivio.add(Pair.of(OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_NOME_UPDATE, true));
            } 
            
            if (archivio.getIdTitolo() != null) {
                if (archivioOld.getIdTitolo() == null) {
                    codiciOperazioneWithOldArchivio.add(Pair.of(OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_CLASSIFICAZIONE_INSERT, false));
                } else if (!(archivio.getIdTitolo().getId().equals(archivioOld.getIdTitolo().getId()))) {
                    codiciOperazioneWithOldArchivio.add(Pair.of(OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_CLASSIFICAZIONE_UPDATE, true));
                }
            } 
            
            if (archivio.getIdMassimario() != null) {
                if (archivioOld.getIdMassimario() == null) {
                    codiciOperazioneWithOldArchivio.add(Pair.of(OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_CATEGORIA_DOCUMENTALE_INSERT, false));
                } else if (!(archivio.getIdMassimario().getId().equals(archivioOld.getIdMassimario().getId()))) {
                    codiciOperazioneWithOldArchivio.add(Pair.of(OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_CATEGORIA_DOCUMENTALE_UPDATE, true));
                }
            } 
            
            if (archivio.getAnniTenuta() != null) {
                if (archivioOld.getAnniTenuta() == null) {
                    codiciOperazioneWithOldArchivio.add(Pair.of(OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_CONSERVAZIONE_INSERT, false));
                } else if (!archivio.getAnniTenuta().toString().equals(archivioOld.getAnniTenuta().toString())) {
                    codiciOperazioneWithOldArchivio.add(Pair.of(OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_CONSERVAZIONE_UPDATE, true));
                }
            } 
     
            if (archivio.getTipo() != null && archivioOld.getTipo() != null && !(archivio.getTipo().toString().equals(archivioOld.getTipo().toString()))) {
                codiciOperazioneWithOldArchivio.add(Pair.of(OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_TIPO_UPDATE, true));
            }
            
            if (archivio.getRiservato() != null && archivioOld.getRiservato() != null && !(archivio.getRiservato().equals(archivioOld.getRiservato()))) {
                codiciOperazioneWithOldArchivio.add(Pair.of(OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_RISERVATO_UPDATE, false));
            }
            
            if (StringUtils.hasText(archivio.getNote())) {                
                if (!StringUtils.hasText(archivioOld.getNote())) {
                    codiciOperazioneWithOldArchivio.add(Pair.of(OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_NOTE_INSERT, false));
                } else if (!archivio.getNote().equals(archivioOld.getNote())) {
                    codiciOperazioneWithOldArchivio.add(Pair.of(OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_NOTE_UPDATE, true));
                }
            } else if (StringUtils.hasText(archivioOld.getNote())) {
                codiciOperazioneWithOldArchivio.add(Pair.of(OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_NOTE_DELETE, true));
            }

            if (krintUtils.doIHaveToKrint(request)) {
                for (Pair<OperazioneKrint.CodiceOperazione, Boolean> pairCodiceOpWithOld: codiciOperazioneWithOldArchivio) {
                    if (pairCodiceOpWithOld.getSecond() == true) {
                        krintScriptaService.writeArchivioUpdate(archivio, archivioOld, pairCodiceOpWithOld.getFirst());
                    } else {
                        krintScriptaService.writeArchivioUpdate(archivio, pairCodiceOpWithOld.getFirst());
                    }
                }
            }
        } catch (Exception ex) {
            throw new AbortSaveInterceptorException("errore nel krintaggio del cambio dell'archivio", ex);
        }

        return super.beforeUpdateEntityInterceptor(archivio, beforeUpdateEntityApplier, additionalData, request, mainEntity, projectionClass);
    }
    
    
    @Override
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        Archivio archivio = (Archivio) entity;
        
        if (mainEntity) {
            scriptaArchiviUtils.updateDataUltimoUtilizzoArchivio(archivio.getId());
        }
        
        return super.afterSelectQueryInterceptor(entity, additionalData, request, mainEntity, projectionClass);
    }
}
