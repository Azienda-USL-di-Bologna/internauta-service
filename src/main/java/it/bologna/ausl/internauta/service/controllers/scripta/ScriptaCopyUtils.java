
package it.bologna.ausl.internauta.service.controllers.scripta;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteStrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioDetailRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioDocRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioRepository;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.Utente;
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
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Classe utile alla copia di un Doc
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

    public Archivio copiaArchivio(Archivio archDaCopiare, Archivio archivioDestinazione, Utente utente, EntityManager em) throws JsonProcessingException, EntityReflectionException{
        return copiaArchivio(archDaCopiare, archivioDestinazione, utente, em, Boolean.TRUE, Boolean.FALSE);
    }
    
    public Archivio copiaArchivio(Archivio archDaCopiare, Archivio archivioDestinazione, Utente utente, EntityManager em, Boolean numera, Boolean rinomina) throws JsonProcessingException, EntityReflectionException{
        return copiaArchivio(archDaCopiare, archivioDestinazione, utente, em, numera, rinomina, null);
    }
    
    public Archivio copiaArchivio(Archivio archDaCopiare, Archivio archivioDestinazione, Utente utente, EntityManager em, Boolean numera, Boolean rinomina, Integer anno) throws JsonProcessingException, EntityReflectionException{
        String numerazioneGerarchicaDaEreditare;
        Archivio idArchivioRadiceDaEreditare;
        Titolo idTitoloDaEreditare;
        Massimario idMassimarioDaEreditare;
        Integer livelloDaEreditare;
       if (archivioDestinazione == null){
            numerazioneGerarchicaDaEreditare = "/" + ZonedDateTime.now().getYear();
            idArchivioRadiceDaEreditare = archDaCopiare;
            idTitoloDaEreditare = archDaCopiare.getIdTitolo();
            idMassimarioDaEreditare = archDaCopiare.getIdMassimario();
            livelloDaEreditare = 1;
        }else{
            numerazioneGerarchicaDaEreditare = archivioDestinazione.getNumerazioneGerarchica();
            idArchivioRadiceDaEreditare = archivioDestinazione.getIdArchivioRadice();
            idTitoloDaEreditare = archivioDestinazione.getIdTitolo();
            idMassimarioDaEreditare = archivioDestinazione.getIdMassimario();
            livelloDaEreditare = archivioDestinazione.getLivello()+1;
        }
        
        Archivio newArchivio = (Archivio) objectMapper.readValue(objectMapper.writeValueAsString(archDaCopiare), EntityReflectionUtils.getEntityFromProxyObject(archDaCopiare));
        
        newArchivio.setId(null);
        if(numera){
            newArchivio.setNumero(0);
            newArchivio.setNumerazioneGerarchica(numerazioneGerarchicaDaEreditare.replace("/", "-x/"));
            newArchivio.setStato(Archivio.StatoArchivio.BOZZA);
        }else {
            newArchivio.setNumerazioneGerarchica(numerazioneGerarchicaDaEreditare.replace("/", "-" + (newArchivio.getNumero().toString().equals("0") ? "x" : newArchivio.getNumero().toString()) + "/"));
        }
        if (rinomina)
            newArchivio.setOggetto(archDaCopiare.getOggetto() + " - copia");
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
        
        
        if (archivioDestinazione == null){
            detail.setDataCreazionePadre(null);
            newArchivio.setIdArchivioRadice(newArchivio);
        }
        detail.setLivello(livelloDaEreditare);

        setNewAttoriArchivio(newArchivio, newArchivio.getIdArchivioCopiato(), utente, em);
//        
//        List<AttoreArchivio> attoriList = new ArrayList<AttoreArchivio>();
//        for (AttoreArchivio attore: archDaCopiare.getAttoriList()){
//            AttoreArchivio newAttore = new AttoreArchivio(newArchivio, attore.getIdPersona(), attore.getIdStruttura(), attore.getRuolo());
//            em.persist(newAttore);
//            em.refresh(newAttore);
//            attoriList.add(newAttore);
//        }
//        newArchivio.setAttoriList(attoriList);
        
        
        if(numera){
            detail.setStato(Archivio.StatoArchivio.BOZZA);
            archivioRepository.numeraArchivio(newArchivio.getId());
        }
        return newArchivio;
    }
    
    public void setNewAttoriArchivio(Archivio arch, Archivio archDes, EntityManager em){
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(em);
        for (AttoreArchivio attore: arch.getIdArchivioRadice().getAttoriList()){
            if(attore.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.CREATORE)){
                Persona p = attore.getIdPersona();
                Struttura s = attore.getIdStruttura();
                jPAQueryFactory
                            .delete(QAttoreArchivio.attoreArchivio)
                            .where(QAttoreArchivio.attoreArchivio.idArchivio.id.eq(arch.getId()))
                            .execute();
                setNewAttoriArchivio(arch, archDes, p, s, em);
            }
        }
    }
    
    public void setNewAttoriArchivio(Archivio arch, Archivio archDes, Utente utenteCreatore, EntityManager em){
        Integer idStruttura = utenteStrutturaRepository.getIdStrutturaAfferenzaDirettaAttivaByIdUtente(utenteCreatore.getId());
        if (idStruttura == null) {
            idStruttura = utenteStrutturaRepository.getIdStrutturaAfferenzaUnificataAttivaByIdUtente(utenteCreatore.getId());
        }
        setNewAttoriArchivio(arch, archDes, utenteCreatore.getIdPersona(), strutturaRepository.getById(idStruttura), em);
    }
    
    public void setNewAttoriArchivio(Archivio arch, Archivio archDes, Persona personaCreatore, Struttura strutturaUtenteCreatore, EntityManager em){
        List<AttoreArchivio> attoriList = new ArrayList<AttoreArchivio>();
        //creazione e salvataggio dell'attore creatore
        AttoreArchivio newAttoreCreatore = new AttoreArchivio(arch, personaCreatore, strutturaUtenteCreatore, AttoreArchivio.RuoloAttoreArchivio.CREATORE);
        em.persist(newAttoreCreatore);
        em.refresh(newAttoreCreatore);
        attoriList.add(newAttoreCreatore);
        //creazione e salvataggio dell'attore responsabile
        AttoreArchivio newAttoreResponsabile = new AttoreArchivio(arch, personaCreatore, strutturaUtenteCreatore, AttoreArchivio.RuoloAttoreArchivio.RESPONSABILE);
        em.persist(newAttoreResponsabile);
        em.refresh(newAttoreResponsabile);
        attoriList.add(newAttoreResponsabile);
        for (AttoreArchivio attore: archDes.getIdArchivioRadice().getAttoriList()){
            if (attore.getIdPersona().getId().equals(personaCreatore.getId())){ 
                if (attore.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.VICARIO) ||
                        attore.getRuolo().equals(AttoreArchivio.RuoloAttoreArchivio.RESPONSABILE_PROPOSTO)) {
                    AttoreArchivio newAttore = new AttoreArchivio(arch, attore.getIdPersona(), attore.getIdStruttura(), attore.getRuolo());
                    em.persist(newAttore);
                    em.refresh(newAttore);
                    attoriList.add(newAttore);
                }
            }
        }
        arch.setAttoriList(attoriList);
    }
    
    public void copiaArchivioDoc(Archivio archDaCopiare, Archivio archivioDestinazione, Utente utente, EntityManager em){
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
        
        for(Doc idDoc: idDocsDaSpostareCheNonCiSonoGia){
            ArchivioDoc newArchivioDoc = new ArchivioDoc(archivioDestinazione, idDoc, utente.getIdPersona());
            em.persist(newArchivioDoc);
        }
    }
    
    public Archivio copiaArchivioConDoc(Archivio archDaCopiare, Archivio archivioDestinazione, Utente utente, EntityManager em, Boolean numera, Boolean contenuto) throws JsonProcessingException, EntityReflectionException{
        return copiaArchivioConDoc(archDaCopiare, archivioDestinazione, utente, em, numera, false, contenuto);
    }
    
    public Archivio copiaArchivioConDoc(Archivio archDaCopiare, Archivio archivioDestinazione, Utente utente, EntityManager em, Boolean numera, Boolean rinomina, Boolean contenuto) throws JsonProcessingException, EntityReflectionException{
        Archivio savedArchivio = copiaArchivio(archDaCopiare, archivioDestinazione, utente, em, numera, rinomina);
        if(contenuto){
            copiaArchivioDoc(archDaCopiare, savedArchivio, utente, em);
        }
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
}
