package it.bologna.ausl.internauta.service.controllers.scripta;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.service.krint.KrintScriptaService;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteStrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioDetailRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioDocRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioRepository;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.ArchivioDetail;
import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.AttoreArchivio;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.Massimario;
import it.bologna.ausl.model.entities.scripta.PermessoArchivio;
import it.bologna.ausl.model.entities.scripta.QArchivioDoc;
import it.bologna.ausl.model.entities.scripta.QAttoreArchivio;
import it.bologna.ausl.model.entities.scripta.QDoc;
import it.bologna.ausl.model.entities.scripta.Titolo;
import it.nextsw.common.utils.EntityReflectionUtils;
import it.nextsw.common.utils.exceptions.EntityReflectionException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Classe utile alla copia di un Doc
 *
 * @author gusgus
 */
@Component
public class ScriptaCopyUtils {

    private static enum MotivazioneEsclusione {
        ARCHIVI_CHIUSI, ARCHIVI_ITER, SENZA_PERMESSO
    }

    private static final Logger log = LoggerFactory.getLogger(ScriptaCopyUtils.class);

    @Autowired
    private ScriptaArchiviUtils scriptaArchiviUtils;

    @Autowired
    private ArchivioDocRepository archivioDocRepository;

    @Autowired
    private UtenteStrutturaRepository utenteStrutturaRepository;

    @Autowired
    private ArchivioDetailRepository archivioDetailRepository;

    @Autowired
    private ArchivioRepository archivioRepository;

    @Autowired
    private StrutturaRepository strutturaRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private KrintScriptaService krintScriptaService;

    public Archivio copiaArchivio(Archivio archDaCopiare, Archivio archivioDestinazione, Utente utente, EntityManager em, boolean copiaTuttiGliAttori) throws JsonProcessingException, EntityReflectionException {
        return copiaArchivio(archDaCopiare, archivioDestinazione, utente, em, Boolean.TRUE, Boolean.FALSE, copiaTuttiGliAttori);
    }

//    public Archivio copiaArchivio(Archivio archDaCopiare, Archivio archivioDestinazione, Utente utente, EntityManager em, Boolean numera, Boolean rinomina, boolean copiaTuttiGliAttori) throws JsonProcessingException, EntityReflectionException {
//        return copiaArchivio(archDaCopiare, archivioDestinazione, utente, em, numera, rinomina, copiaTuttiGliAttori);
//    }

    public Archivio copiaArchivio(Archivio archDaCopiare, Archivio archivioDestinazione, Utente utente, EntityManager em, Boolean numera, Boolean rinomina, boolean copiaTuttiGliAttori) throws JsonProcessingException, EntityReflectionException {
        String numerazioneGerarchicaDaEreditare;
        Archivio idArchivioRadiceDaEreditare;
        Titolo idTitoloDaEreditare;
        Massimario idMassimarioDaEreditare;
        Integer livelloDaEreditare;
        if (archivioDestinazione == null) {
            numerazioneGerarchicaDaEreditare = "/" + ZonedDateTime.now().getYear();
            idArchivioRadiceDaEreditare = archDaCopiare;
            idTitoloDaEreditare = archDaCopiare.getIdTitolo();
            idMassimarioDaEreditare = archDaCopiare.getIdMassimario();
            livelloDaEreditare = 1;
        } else {
            numerazioneGerarchicaDaEreditare = archivioDestinazione.getNumerazioneGerarchica();
            idArchivioRadiceDaEreditare = archivioDestinazione.getIdArchivioRadice();
            idTitoloDaEreditare = archivioDestinazione.getIdTitolo();
            idMassimarioDaEreditare = archivioDestinazione.getIdMassimario();
            livelloDaEreditare = archivioDestinazione.getLivello() + 1;
        }

        Archivio newArchivio = (Archivio) objectMapper.readValue(objectMapper.writeValueAsString(archDaCopiare), EntityReflectionUtils.getEntityFromProxyObject(archDaCopiare));

        newArchivio.setId(null);
        if (numera) {
            newArchivio.setNumero(0);
            newArchivio.setNumerazioneGerarchica(numerazioneGerarchicaDaEreditare.replace("/", "-x/"));
            newArchivio.setStato(Archivio.StatoArchivio.BOZZA);
        } else {
            newArchivio.setNumerazioneGerarchica(numerazioneGerarchicaDaEreditare.replace("/", "-" + (newArchivio.getNumero().toString().equals("0") ? "x" : newArchivio.getNumero().toString()) + "/"));
        }
        if (rinomina) {
            newArchivio.setOggetto(archDaCopiare.getOggetto() + " - copia");
        }
        newArchivio.setIdArchivioPadre(archivioDestinazione);
        newArchivio.setIdArchivioRadice(idArchivioRadiceDaEreditare);
        newArchivio.setIdTitolo(idTitoloDaEreditare);
        newArchivio.setIdMassimario(idMassimarioDaEreditare);
        newArchivio.setDataCreazione(ZonedDateTime.now());
        newArchivio.setDataInserimentoRiga(ZonedDateTime.now());
        newArchivio.setVersion(ZonedDateTime.now());
        newArchivio.setIdArchivioCopiato(archDaCopiare);
        newArchivio.setLivello(livelloDaEreditare);
        newArchivio.setNumeroSottoarchivi(0);
        newArchivio.setIdArchivioArgo(null);
        newArchivio.setIdArchivioImportato(null);
        em.persist(newArchivio);
        em.flush();
        em.refresh(newArchivio);

        //numero il nuovo archivio
        ArchivioDetail detail = archivioDetailRepository.getById(newArchivio.getId());
        detail.setIdPersonaResponsabile(utente.getIdPersona());
        detail.setIdPersonaCreazione(utente.getIdPersona());
        Integer idStruttura = utenteStrutturaRepository.getIdStrutturaAfferenzaDirettaAttivaByIdUtente(utente.getId());
        if (idStruttura == null) {
            idStruttura = utenteStrutturaRepository.getIdStrutturaAfferenzaUnificataAttivaByIdUtente(utente.getId());
        }
        detail.setIdStruttura(strutturaRepository.getById(idStruttura));

        if (archivioDestinazione == null) {
            detail.setDataCreazionePadre(null);
            newArchivio.setIdArchivioRadice(newArchivio);
        }
        detail.setLivello(livelloDaEreditare);

        setNewAttoriArchivio(archDaCopiare,newArchivio , utente, em, copiaTuttiGliAttori);
//        
//        List<AttoreArchivio> attoriList = new ArrayList<AttoreArchivio>();
//        for (AttoreArchivio attore: archDaCopiare.getAttoriList()){
//            AttoreArchivio newAttore = new AttoreArchivio(newArchivio, attore.getIdPersona(), attore.getIdStruttura(), attore.getRuolo());
//            em.persist(newAttore);
//            em.refresh(newAttore);
//            attoriList.add(newAttore);
//        }
//        newArchivio.setAttoriList(attoriList);

        if (numera) {
            detail.setStato(Archivio.StatoArchivio.BOZZA);
            archivioRepository.numeraArchivio(newArchivio.getId());
        }
        return newArchivio;
    }

    public Archivio duplicaArchivio(Archivio archivio, Utente utente, Boolean duplicaDocumenti, Boolean duplicaArchiviFigli, Boolean iHaveToKrint, Boolean meccanismoDiFineAnno) throws JsonProcessingException, EntityReflectionException {
        boolean haFigli = false;
        //controllo se l'archivio da copiare ha figli
        if (archivio.getArchiviFigliList().size() > 0) {
            haFigli = true;
        }
        //if utente null allora prendo da archivio / se utente è disattivo 
        //queto if è per i meccanismi di fine anno dove bisogna copiare tutto del fascicolo
        //qui va sistemato in caso di responsabile/vicario non piu attivo
        if (utente == null && meccanismoDiFineAnno) {
            List<AttoreArchivio> responsabili = archivio.getAttoriList().stream().filter(
                    p -> {
                        return p.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.RESPONSABILE);
                    }).collect(Collectors.toList());
            if (responsabili != null && !responsabili.isEmpty()) {
                List<Utente> utenti = responsabili.get(0).getIdPersona().getUtenteList().stream().filter(
                        u -> {
                            return u.getAttivo() && u.getIdAzienda().getId().equals(archivio.getIdAzienda().getId());
                        }).collect(Collectors.toList());
                if (utenti != null && !utenti.isEmpty()) {
                    utente = utenti.get(0);
                }
            }
        }
        // TODOMido: inserire gli attori archivi
        log.info(String.format("Inizio a duplicare il fascicolo con id %s", archivio.getId()));
        Archivio savedArchivio = copiaArchivioConDoc(archivio, 
                archivio.getIdArchivioPadre(), 
                utente, 
                entityManager, 
                Boolean.TRUE, 
                !meccanismoDiFineAnno, 
                duplicaDocumenti,
                meccanismoDiFineAnno);
        if (meccanismoDiFineAnno) {
            //copiaAttoriArchivio(archivio,savedArchivio,entityManager);
            archivioRepository.copiaPermessiArchivi(archivio.getId(), savedArchivio.getId());
        }
        
        log.info(String.format("finito di duplicare %s con i suoi documenti", archivio.getId()));
        if (haFigli) {
            log.info(String.format("procedo a duplicare i figli e nipoti di %s", archivio.getId()));
            for (Archivio archFiglio : archivio.getArchiviFigliList()) {
                if (!archFiglio.getStato().equals(Archivio.StatoArchivio.BOZZA)) {
                    log.info(String.format("inzio a duplicare %s, figlio di %s, con i suoi documenti", archFiglio.getId(), archivio.getId()));
                    Archivio savedFiglioArchivio = copiaArchivioConDoc(archFiglio, savedArchivio, utente, entityManager, Boolean.TRUE, duplicaDocumenti, meccanismoDiFineAnno);
                    if (meccanismoDiFineAnno) {
                        //copiaAttoriArchivio(archFiglio,savedFiglioArchivio,entityManager);
                        archivioRepository.copiaPermessiArchivi(archFiglio.getId(), savedFiglioArchivio.getId());
                    }

                    log.info(String.format("finito di duplicare %s, figlio di %s, con i suoi documenti", archFiglio.getId(), archivio.getId()));
                    if (iHaveToKrint) {
                        krintScriptaService.writeArchivioUpdate(savedFiglioArchivio, archFiglio, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_CREATION_DA_DUPLICA);
                    }
                    for (Archivio archNipote : archFiglio.getArchiviFigliList()) {
                        if (!archNipote.getStato().equals(Archivio.StatoArchivio.BOZZA)) {
                            log.info(String.format("inzio a duplicare %s, nipote di %s, con i suoi documenti", archNipote.getId(), archivio.getId()));
                            Archivio savedInsArchivio = copiaArchivioConDoc(archNipote, savedFiglioArchivio, utente, entityManager, Boolean.TRUE, duplicaDocumenti, meccanismoDiFineAnno);
                            if (meccanismoDiFineAnno) {
                                //copiaAttoriArchivio(archNipote,savedInsArchivio, entityManager);
                                archivioRepository.copiaPermessiArchivi(archNipote.getId(), savedInsArchivio.getId());
                            }
                            log.info(String.format("finito di duplicare %s, nipote di %s, con i suoi documenti", archNipote.getId(), archivio.getId()));
                            if (iHaveToKrint) {
                                krintScriptaService.writeArchivioUpdate(savedInsArchivio, archNipote, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_CREATION_DA_DUPLICA);
                            }
                        }
                    }
                }
            }
            log.info(String.format("finito le duplicare i figli e nipoti di %s", archivio.getId()));
        }
        archivioRepository.calcolaPermessiEsplicitiGerarchia(savedArchivio.getId());
        entityManager.flush();
        entityManager.refresh(savedArchivio);

        archivioRepository.copiaPermessiArchivi(archivio.getId(), savedArchivio.getId());
        archivioRepository.calcolaPermessiEsplicitiGerarchia(savedArchivio.getId());
        if (iHaveToKrint) {
            krintScriptaService.writeArchivioUpdate(archivio, savedArchivio, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_DUPLICA);
            krintScriptaService.writeArchivioUpdate(savedArchivio, archivio, OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_CREATION_DA_DUPLICA);
        }
        return savedArchivio;
    }

    public void setNewAttoriArchivio(Archivio arch, Archivio archDes, EntityManager em, boolean copiaTuttiGliAttori) {
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(em);
        for (AttoreArchivio attore : arch.getIdArchivioRadice().getAttoriList()) {
            if (attore.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.CREATORE)) {
                Persona p = attore.getIdPersona();
                Struttura s = attore.getIdStruttura();
                jPAQueryFactory
                        .delete(QAttoreArchivio.attoreArchivio)
                        .where(QAttoreArchivio.attoreArchivio.idArchivio.id.eq(arch.getId()))
                        .execute();
                setNewAttoriArchivio(arch, archDes, p, s, em, copiaTuttiGliAttori);
            }
        }
    }

    public void setNewAttoriArchivio(Archivio archivioSorgente, Archivio archivioDestinazione, Utente utenteCreatore, EntityManager em, boolean copiaTuttiGliAttori) {
        Integer idStruttura = utenteStrutturaRepository.getIdStrutturaAfferenzaDirettaAttivaByIdUtente(utenteCreatore.getId());
        if (idStruttura == null) {
            idStruttura = utenteStrutturaRepository.getIdStrutturaAfferenzaUnificataAttivaByIdUtente(utenteCreatore.getId());
        }
        setNewAttoriArchivio(archivioSorgente, archivioDestinazione, utenteCreatore.getIdPersona(), strutturaRepository.getById(idStruttura), em, copiaTuttiGliAttori);
    }
    
    /**
     * 
     * @param archivioSorgente
     * @param archivioDestinazione
     * @param personaCreatore
     * @param strutturaUtenteCreatore
     * @param em
     * @param copiaTuttiGliAttori 
     */
    private void setNewAttoriArchivio(Archivio archivioSorgente, Archivio archivioDestinazione, Persona personaCreatore, Struttura strutturaUtenteCreatore, EntityManager em, boolean copiaTuttiGliAttori) {
        List<AttoreArchivio> attoriList = new ArrayList<AttoreArchivio>();
        //creazione e salvataggio dell'attore creatore
        if (copiaTuttiGliAttori){
            for (AttoreArchivio attoreArchivioDaCopiare : archivioSorgente.getAttoriList()) {
                attoriList.add(new AttoreArchivio(archivioDestinazione, attoreArchivioDaCopiare.getIdPersona(), attoreArchivioDaCopiare.getIdStruttura(), attoreArchivioDaCopiare.getRuolo()));
            }
        } else {
            AttoreArchivio newAttoreCreatore = new AttoreArchivio(archivioSorgente, personaCreatore, strutturaUtenteCreatore, AttoreArchivio.RuoloAttoreArchivio.CREATORE);
            em.persist(newAttoreCreatore);
            em.flush();
            em.refresh(newAttoreCreatore);
            attoriList.add(newAttoreCreatore);
            //creazione e salvataggio dell'attore responsabile
            AttoreArchivio newAttoreResponsabile = new AttoreArchivio(archivioSorgente, personaCreatore, strutturaUtenteCreatore, AttoreArchivio.RuoloAttoreArchivio.RESPONSABILE);
            em.persist(newAttoreResponsabile);
            em.flush();
            em.refresh(newAttoreResponsabile);
            attoriList.add(newAttoreResponsabile);
            for (AttoreArchivio attore : archivioDestinazione.getIdArchivioRadice().getAttoriList()) {
                if (attore.getIdPersona().getId().equals(personaCreatore.getId())) {
                    if (attore.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.VICARIO)
                            || attore.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.RESPONSABILE_PROPOSTO)) {
                        AttoreArchivio newAttore = new AttoreArchivio(archivioSorgente, attore.getIdPersona(), attore.getIdStruttura(), attore.getRuolo());
                        em.persist(newAttore);
                        em.flush();
                        em.refresh(newAttore);
                        attoriList.add(newAttore);
                    }
                }
            }
        }
        archivioDestinazione.setAttoriList(attoriList);
    }

    public void copiaArchivioDoc(Archivio archDaCopiare, Archivio archivioDestinazione, Utente utente, EntityManager em) {
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(em);

        List<Integer> idDocsDaSpostare = jPAQueryFactory
                .select(QArchivioDoc.archivioDoc.idDoc.id)
                .from(QArchivioDoc.archivioDoc)
                .where(QArchivioDoc.archivioDoc.idArchivio.eq(archDaCopiare))
                .fetch();
        List<Integer> idDocsDaSpostareCheCiSonoGia = jPAQueryFactory
                .select(QArchivioDoc.archivioDoc.idDoc.id)
                .from(QArchivioDoc.archivioDoc)
                .where(QArchivioDoc.archivioDoc.idArchivio.eq(archivioDestinazione))
                .fetch();
        List<Doc> idDocsDaSpostareCheNonCiSonoGia = jPAQueryFactory
                .select(QDoc.doc)
                .from(QDoc.doc)
                .where(QDoc.doc.id.in(idDocsDaSpostare).and(QDoc.doc.id.notIn(idDocsDaSpostareCheCiSonoGia)))
                .fetch();

        for (Doc idDoc : idDocsDaSpostareCheNonCiSonoGia) {
            ArchivioDoc newArchivioDoc = new ArchivioDoc(archivioDestinazione, idDoc, utente.getIdPersona());
            em.persist(newArchivioDoc);
        }
    }

    public Archivio copiaArchivioConDoc(Archivio archDaCopiare, Archivio archivioDestinazione, Utente utente, EntityManager em, Boolean numera, Boolean contenuto, boolean copiaTuttiGliAttori) throws JsonProcessingException, EntityReflectionException {
        return copiaArchivioConDoc(archDaCopiare, archivioDestinazione, utente, em, numera, false, contenuto, copiaTuttiGliAttori);
    }

    public Archivio copiaArchivioConDoc(Archivio archDaCopiare, Archivio archivioDestinazione, Utente utente, EntityManager em, Boolean numera, Boolean rinomina, Boolean contenuto, boolean copiaTuttiGliAttori) throws JsonProcessingException, EntityReflectionException {
        Archivio savedArchivio = copiaArchivio(archDaCopiare, archivioDestinazione, utente, em, numera, rinomina, copiaTuttiGliAttori);
        if (contenuto) {
            log.info(String.format("inzio a duplicare %s con i suoi documenti", archDaCopiare.getId()));
            copiaArchivioDoc(archDaCopiare, savedArchivio, utente, em);
        }
        em.flush();
        em.refresh(savedArchivio);
        return savedArchivio;
    }

    public Map<String, List<String>> copiaArchiviazioni(Doc docOrgine, Doc docDestinazione, Persona persona) {
        List<ArchivioDoc> archiviazioniOrigine = docOrgine.getArchiviDocList();
        Map<String, List<String>> infoArchiviNonCopiati = new HashMap();
        List<String> archiviChiusi = new ArrayList();
        List<String> archiviIter = new ArrayList();
        List<String> archiviSenzaPermesso = new ArrayList();
        for (ArchivioDoc archiviazioneOrgine : archiviazioniOrigine) {
            Archivio archivio = archiviazioneOrgine.getIdArchivio();
            // Se è eliminata faccio finta che non ci sia, non deve essere informato l'utente
            if (archiviazioneOrgine.getDataEliminazione() == null) {
                boolean personHasAtLeastThisPermissionOnTheArchive = scriptaArchiviUtils.personHasAtLeastThisPermissionOnTheArchive(persona.getId(), archivio.getId(), PermessoArchivio.DecimalePredicato.MODIFICA);
                if (personHasAtLeastThisPermissionOnTheArchive) {
                    if (archivio.getStato().equals(Archivio.StatoArchivio.APERTO)) {
                        if (archivio.getIdIter() == null) {
                            ArchivioDoc newArchivioDoc = new ArchivioDoc(archivio, docDestinazione, persona);
                            archivioDocRepository.save(newArchivioDoc);
                        } else {
                            archiviIter.add(scriptaArchiviUtils.getNomeCompletoArchivioPerVisualizzazioneDiSicurezzaClassica(archivio));
                        }
                    } else {
                        archiviChiusi.add(scriptaArchiviUtils.getNomeCompletoArchivioPerVisualizzazioneDiSicurezzaClassica(archivio));
                    }
                } else {
                    archiviSenzaPermesso.add(scriptaArchiviUtils.getNomeCompletoArchivioPerVisualizzazioneDiSicurezzaClassica(archivio));
                }
            }
        }
        infoArchiviNonCopiati.put(MotivazioneEsclusione.ARCHIVI_ITER.toString(), archiviIter);
        infoArchiviNonCopiati.put(MotivazioneEsclusione.ARCHIVI_CHIUSI.toString(), archiviChiusi);
        infoArchiviNonCopiati.put(MotivazioneEsclusione.SENZA_PERMESSO.toString(), archiviSenzaPermesso);
        return infoArchiviNonCopiati;
    }
    
    /**
     * Clono gli attori dell'archivio che sto duplicando e li metto a quello appena creato.
     * @param archivioDestinazione Il nuovo archivio che riceverà gli attori.
     * @param archivioSorgente Il vecchio archivio da cui clonerò gli attori.
     */
    private void copiaAttoriArchivio(Archivio archivioSorgente, Archivio archivioDestinazione, EntityManager entityManager){
        // preparo la lista che riemperò con tutti gli attori
        List<AttoreArchivio> attoriList = new ArrayList<AttoreArchivio>();

        for (AttoreArchivio attore: archivioSorgente.getAttoriList()){
            // creo un nuovo attore basndomi su quelli che sto ciclando
            AttoreArchivio newAttore = new AttoreArchivio(archivioDestinazione, attore.getIdPersona(), attore.getIdStruttura(), attore.getRuolo());
            
            entityManager.persist(newAttore);
            entityManager.flush();
            entityManager.refresh(newAttore);
            attoriList.add(newAttore);
        }
        // setto gli attori appena clonati al nuovo archivio
        archivioDestinazione.setAttoriList(attoriList);
    }
}
