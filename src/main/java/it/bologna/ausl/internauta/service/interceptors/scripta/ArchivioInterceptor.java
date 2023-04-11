package it.bologna.ausl.internauta.service.interceptors.scripta;

import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.krint.KrintScriptaService;
import it.bologna.ausl.internauta.service.krint.KrintUtils;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.MassimarioRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.Massimario;
import it.bologna.ausl.model.entities.scripta.QMassimario;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.controller.BeforeUpdateEntityApplier;
import it.nextsw.common.controller.exceptions.BeforeUpdateEntityApplierException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
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
    private KrintUtils krintUtils;

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
        try {
            boolean nomeIsChanged;
            if (archivio.getOggetto() != null && archivioOld.getOggetto() != null) {
                nomeIsChanged = !(archivio.getOggetto().equals(archivioOld.getOggetto()));
            } else {
                nomeIsChanged = false;
            }
            boolean classificazioneIsChanged;
            if (archivio.getIdTitolo() != null && archivioOld.getIdTitolo() != null) {
                classificazioneIsChanged = !(archivio.getIdTitolo().getId().equals(archivioOld.getIdTitolo().getId()));
            } else {
                classificazioneIsChanged = false;
            }
            boolean categoriaDocumentaleIsChanged;
            if (archivio.getIdMassimario() != null && archivioOld.getIdMassimario() != null) {
                categoriaDocumentaleIsChanged = !(archivio.getIdMassimario().getId().equals(archivioOld.getIdMassimario().getId()));
            } else {
                categoriaDocumentaleIsChanged = false;
            }
            boolean conservazioneIsChanged;
            if (archivio.getAnniTenuta() != null && archivioOld.getAnniTenuta() != null) {
                conservazioneIsChanged = !archivio.getAnniTenuta().toString().equals(archivioOld.getAnniTenuta().toString()) || 
                        !archivio.getIdMassimario().getDescrizioneTenuta().equals(archivioOld.getIdMassimario().getDescrizioneTenuta());
            } else {
                conservazioneIsChanged = false;
            }
            boolean tipoIsChanged;
            if (archivio.getTipo() != null && archivioOld.getTipo() != null) {
                tipoIsChanged = !(archivio.getTipo().toString().equals(archivioOld.getTipo().toString()));
            } else {
                tipoIsChanged = false;
            }
            boolean riservatoIsChanged;
            if (archivio.getRiservato() != null && archivioOld.getRiservato() != null) {
                riservatoIsChanged = !(archivio.getRiservato().equals(archivioOld.getRiservato()));
            } else {
                riservatoIsChanged = false;
            }
            boolean noteIsChanged;
            if (archivio.getNote() != null && archivioOld.getNote() != null) {
                noteIsChanged = !(archivio.getNote().equals(archivioOld.getNote()));
            } else {
                noteIsChanged = false;
            }

            if (krintUtils.doIHaveToKrint(request)) {
                if (nomeIsChanged) {
                    krintScriptaService.writeArchivioUpdate(archivio, archivioOld, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_NOME_UPDATE);
                }
                if (classificazioneIsChanged) {
                    krintScriptaService.writeArchivioUpdate(archivio, archivioOld, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_CLASSIFICAZIONE_UPDATE);
                }
                if (categoriaDocumentaleIsChanged) {
                    krintScriptaService.writeArchivioUpdate(archivio, archivioOld, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_CATEGORIA_DOCUMENTALE_UPDATE);
                }
                if (conservazioneIsChanged) {
                    krintScriptaService.writeArchivioUpdate(archivio, archivioOld, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_CONSERVAZIONE_UPDATE);
                }
                if (tipoIsChanged) {
                    krintScriptaService.writeArchivioUpdate(archivio, archivioOld, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_TIPO_UPDATE);
                }
                if (riservatoIsChanged) {
                    krintScriptaService.writeArchivioUpdate(archivio, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_RISERVATO_UPDATE);
                }
                if (noteIsChanged) {
                    krintScriptaService.writeArchivioUpdate(archivio, archivioOld, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_NOTE_UPDATE);
                }

            }
        } catch (Exception ex) {
            throw new AbortSaveInterceptorException("errore nel krintaggio del cambio dell'archivio", ex);
        }

        return super.beforeUpdateEntityInterceptor(archivio, beforeUpdateEntityApplier, additionalData, request, mainEntity, projectionClass);
    }
}
