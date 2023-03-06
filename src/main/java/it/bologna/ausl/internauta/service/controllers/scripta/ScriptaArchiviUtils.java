
package it.bologna.ausl.internauta.service.controllers.scripta;

import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.exceptions.BadParamsException;
import it.bologna.ausl.internauta.service.exceptions.http.Http403ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.Http404ResponseException;
import it.bologna.ausl.internauta.service.exceptions.http.Http500ResponseException;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioDiInteresseRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioDocRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.DocRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.PermessoArchivioRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageDocRepository;
import it.bologna.ausl.internauta.service.shpeck.utils.ShpeckUtils;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.service.utils.ScriptaUtils;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDetailInterface;
import it.bologna.ausl.model.entities.scripta.MessageDoc;
import it.bologna.ausl.model.entities.scripta.PermessoArchivio;
import it.bologna.ausl.model.entities.scripta.QPermessoArchivio;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.MessageInterface;
import it.bologna.ausl.model.entities.shpeck.data.AdditionalDataArchiviation;
import it.bologna.ausl.model.entities.shpeck.data.AdditionalDataTagComponent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
public class ScriptaArchiviUtils {
    
    private final List<MinIOWrapperFileInfo> savedFilesOnRepository = new ArrayList();
    
    @Autowired
    private PermessoArchivioRepository permessoArchivioRepository;
    
    @Autowired
    private CachedEntities cachedEntities;
    
    @Autowired
    private ParametriAziendeReader parametriAziende;

    @Autowired
    private ArchivioDocRepository archivioDocRepository;

    @Autowired
    private MessageDocRepository messageDocRepository;

    @Autowired
    private DocRepository docRepository;

    @Autowired
    private ArchivioDiInteresseRepository archivioDiInteresseRepository;

    @Autowired
    private ScriptaUtils scriptaUtils;

    @Autowired
    private ReporitoryConnectionManager aziendeConnectionManager;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private ShpeckUtils shpeckUtils;
    
    /**
     * Restituisce true se la persona ha ALMENO il permesso sull'archivio
     */
    public boolean personHasAtLeastThisPermissionOnTheArchive(Integer idPersona, Integer idArchivio, PermessoArchivio.DecimalePredicato permesso) {
        QPermessoArchivio permessoArchivio = QPermessoArchivio.permessoArchivio;
        BooleanExpression filterUserhasPermission = permessoArchivio.idArchivioDetail.id.eq(idArchivio).and(
                permessoArchivio.idPersona.id.eq(idPersona).and(
                permessoArchivio.bit.goe(permesso.getValue()))
        );
        Optional<PermessoArchivio> findOne = permessoArchivioRepository.findOne(filterUserhasPermission);
        return findOne.isPresent();
    }
    
    /**
     * Torna il nome dell'archivio così composto: [numerazione gerarchica] oggetto dell'archivio radice.In caso di azienda con facicoliParlanti il nome sarà solo: [numerazione gerarchica]
     * @param archivio
     * @return 
     */
    public String getNomeCompletoArchivioPerVisualizzazioneDiSicurezzaClassica(Archivio archivio) {
        return "[" + archivio.getNumerazioneGerarchica() + "] " + getOggettoArchivioPerVisualizzazioneDiSicurezzaClassica(archivio);
    }
    
    /**
     * Torna l'oggetto dell'archivio radice. Torna solo null se azienda con fascioli parlanti
     * @param archivio
     * @return 
     */
    public String getOggettoArchivioPerVisualizzazioneDiSicurezzaClassica(Archivio archivio) {
        List<ParametroAziende> fascicoliParlanti = cachedEntities.getParameters("fascicoliParlanti", archivio.getIdAzienda().getId());
        if (fascicoliParlanti != null && !fascicoliParlanti.isEmpty() && parametriAziende.getValue(fascicoliParlanti.get(0), Boolean.class)) {
            return "";
        } else {
            Archivio archivioRadice = null;
            if (archivio.getLivello().equals(1)) {
                archivioRadice = archivio;
            } else {
                archivioRadice = archivio.getIdArchivioRadice();
            }
            return archivioRadice.getOggetto();
        }
    }
    
    
    /**
     * Funzione che archvia il message dentro archivio.
     * @param message
     * @param archivio
     * @param persona
     * @param azienda
     * @param utente
     * @throws Http404ResponseException
     * @throws Http403ResponseException
     * @throws MasterjobsWorkerException
     * @throws BlackBoxPermissionException
     * @throws BadParamsException
     * @throws IOException
     * @throws MinIOWrapperException
     * @throws Http500ResponseException 
     */
    public Integer archiveMessage(Message message, Archivio archivio, Persona persona, Azienda azienda, Utente utente) 
            throws Http404ResponseException, Http403ResponseException, MasterjobsWorkerException, BlackBoxPermissionException, 
            BadParamsException, IOException, MinIOWrapperException, Http500ResponseException {
        // Controlli di sicurezza
        // 1
        // Se il messaggio non è archiviabile perché non ha ancora l'idRepository
        // Nel frontend l'icona è disattiva quindi qui non dovrei mai entrare.
        if (message.getUuidRepository() == null) {
            throw new Http404ResponseException("1", "Messaggio senza idRepository");
        }

        // 2
        // Controllo che l'utente abbia permessi nella casella pec del message
        Map<Integer, List<String>> permessiPec = userInfoService.getPermessiPec(persona);
        if (!permessiPec.isEmpty()) {
            List<Integer> pecList = new ArrayList();
            pecList.addAll(permessiPec.keySet());
            if (!pecList.contains(message.getIdPec().getId())) {
                throw new Http403ResponseException("2", "Utente senza permessi sulla casella pec");
            }
        } else {
            throw new Http403ResponseException("2", "Utente senza permessi sulla casella pec");
        }

        // 3
        // Controllo che l'utente abbia almeno permesso di modifica sull'archivio
        if (!personHasAtLeastThisPermissionOnTheArchive(persona.getId(), archivio.getId(), PermessoArchivio.DecimalePredicato.MODIFICA)) {
            throw new Http403ResponseException("3", "Utente senza permesso di modificare l'archivio");
        }

        /*
        Ora vedo se il doc già esiste ( lo becco dentro messages_docs. )
        Se non esiste allora dovrò crearlo e quindi dovrò creare i vari allegati.
         */
        Doc doc = null;
        List<MessageDoc> messageDocList = message.getMessageDocList().stream().filter(md -> md.getScope().equals(MessageDoc.ScopeMessageDoc.ARCHIVIAZIONE)).collect(Collectors.toList());
        for (MessageDoc md : messageDocList) {
            if (md.getIdDoc().getIdAzienda().getId().equals(azienda.getId())) {
                doc = md.getIdDoc();
                break;
            }
        }

        if (doc == null) {
            File downloadEml = shpeckUtils.downloadEml(ShpeckUtils.EmlSource.MESSAGE, message.getId());
            MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();

            doc = new Doc("Pec_" + message.getId().toString(), persona, archivio.getIdAzienda(), DocDetailInterface.TipologiaDoc.DOCUMENT_PEC.toString());
            doc = docRepository.save(doc);
            MessageDoc.TipoMessageDoc tipo = null;
            if (message.getInOut().equals(MessageInterface.InOut.IN)) {
                tipo = MessageDoc.TipoMessageDoc.IN;
            } else {
                tipo = MessageDoc.TipoMessageDoc.OUT;
            }
            MessageDoc md = new MessageDoc(message, doc, tipo, MessageDoc.ScopeMessageDoc.ARCHIVIAZIONE);
            messageDocRepository.save(md);

            try {
                scriptaUtils.creaAndAllegaAllegati(doc, new FileInputStream(downloadEml), "Pec_" + message.getId().toString(), true, true, message.getUuidRepository(), false, null); // downloadEml.getName()
            } catch (Exception e) {
                if (savedFilesOnRepository != null && !savedFilesOnRepository.isEmpty()) {
                    for (MinIOWrapperFileInfo minIOWrapperFileInfo : savedFilesOnRepository) {
                        minIOWrapper.removeByFileId(minIOWrapperFileInfo.getFileId(), false);
                    }
                }
                e.printStackTrace();
                throw new Http500ResponseException("4", "Qualcosa è andato storto nelle creazione degli allegati");
            }
        }

        // Ora che o il doc lo archivio
        ArchivioDoc archiviazione = new ArchivioDoc(archivio, doc, persona);
        archivioDocRepository.save(archiviazione);
        archivioDiInteresseRepository.aggiungiArchivioRecente(archivio.getIdArchivioRadice().getId(), persona.getId());

        // Ora aggiungo il tag di archiviazione sul message
        AdditionalDataTagComponent.idUtente utenteAdditionalData = new AdditionalDataTagComponent.idUtente(utente.getId(), persona.getDescrizione());
        AdditionalDataTagComponent.idAzienda aziendaAdditionalData = new AdditionalDataTagComponent.idAzienda(azienda.getId(), azienda.getNome(), azienda.getDescrizione());
        AdditionalDataTagComponent.idArchivio archivioAdditionalData = new AdditionalDataTagComponent.idArchivio(archivio.getId(), archivio.getOggetto(), archivio.getNumerazioneGerarchica());
        AdditionalDataArchiviation additionalDataArchiviation = new AdditionalDataArchiviation(utenteAdditionalData, aziendaAdditionalData, archivioAdditionalData, LocalDateTime.now());
        shpeckUtils.SetArchiviationTag(message.getIdPec(), message, additionalDataArchiviation, utente, true, true);

//        AccodatoreVeloce accodatoreVeloce = new AccodatoreVeloce(masterjobsJobsQueuer, masterjobsObjectsFactory);
//        accodatoreVeloce.accodaCalcolaPersoneVedentiDoc(doc.getId());
        return doc.getId();
    }
}
