
package it.bologna.ausl.internauta.service.controllers.scripta;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioDetailRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioDocRepository;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioRepository;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.ArchivioDetail;
import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.AttoreArchivio;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.PermessoArchivio;
import it.bologna.ausl.model.entities.scripta.QArchivio;
import static it.bologna.ausl.model.entities.scripta.QArchivio.archivio;
import it.bologna.ausl.model.entities.scripta.QArchivioDoc;
import it.bologna.ausl.model.entities.scripta.QDoc;
import it.bologna.ausl.model.entities.scripta.Titolo;
import it.nextsw.common.utils.EntityReflectionUtils;
import it.nextsw.common.utils.exceptions.EntityReflectionException;
import static java.lang.Math.log;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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
    private ArchivioDetailRepository archivioDetailRepository;
    
    @Autowired
    private ArchivioRepository archivioRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    public Archivio copiaArchivio(Archivio archDaCopiare, Archivio archivioDestinazione, Persona utente, EntityManager em) throws JsonProcessingException, EntityReflectionException{
        return copiaArchivio(archDaCopiare, archivioDestinazione, utente, em, Boolean.TRUE);
    }
    
    public Archivio copiaArchivio(Archivio archDaCopiare, Archivio archivioDestinazione, Persona utente, EntityManager em, Boolean numera) throws JsonProcessingException, EntityReflectionException{
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(em); 
        String numerazioneGerarchicaDaEreditare;
        Archivio idArchivioRadiceDaEreditare;
        Titolo idTitoloDaEreditare;
        Integer livelloDaEreditare;
       if (archivioDestinazione == null){
            numerazioneGerarchicaDaEreditare = "/" + ZonedDateTime.now().getYear();
            idArchivioRadiceDaEreditare = archDaCopiare;
            idTitoloDaEreditare = archDaCopiare.getIdTitolo();
            livelloDaEreditare = 1;
        }else{
            numerazioneGerarchicaDaEreditare = archivioDestinazione.getNumerazioneGerarchica();
            idArchivioRadiceDaEreditare = archivioDestinazione.getIdArchivioRadice();
            idTitoloDaEreditare = archivioDestinazione.getIdTitolo();
            livelloDaEreditare = archivioDestinazione.getLivello()+1;
        }
        
        Archivio newArchivio = (Archivio) objectMapper.readValue(objectMapper.writeValueAsString(archDaCopiare), EntityReflectionUtils.getEntityFromProxyObject(archDaCopiare));
        newArchivio.setId(null);
        if(numera){
            newArchivio.setNumero(0);
            newArchivio.setNumerazioneGerarchica(numerazioneGerarchicaDaEreditare.replace("/", "-x/"));
            newArchivio.setStato(Archivio.StatoArchivio.BOZZA);
        }else {
            newArchivio.setNumerazioneGerarchica(numerazioneGerarchicaDaEreditare.replace("/", "-" + newArchivio.getNumero().toString() + "/"));
        }
        newArchivio.setIdArchivioPadre(archivioDestinazione);
        newArchivio.setIdArchivioRadice(idArchivioRadiceDaEreditare);
        newArchivio.setIdTitolo(idTitoloDaEreditare);
        newArchivio.setDataCreazione(ZonedDateTime.now());
        newArchivio.setDataInserimentoRiga(ZonedDateTime.now());
        newArchivio.setVersion(ZonedDateTime.now());
//        newArchivio.setAttoriList(archDaCopiare.getAttoriList());
        newArchivio.setLivello(livelloDaEreditare);
        em.persist(newArchivio);
        em.refresh(newArchivio);
        
        //numero il nuovo archivio
        ArchivioDetail detail = archivioDetailRepository.getById(newArchivio.getId());
        detail.setIdPersonaResponsabile(archDaCopiare.getIdArchivioDetail().getIdPersonaResponsabile());
        detail.setIdPersonaCreazione(utente);
        detail.setIdStruttura(archDaCopiare.getIdArchivioDetail().getIdStruttura());
        
        if (archivioDestinazione == null){
            detail.setDataCreazionePadre(null);
            newArchivio.setIdArchivioRadice(newArchivio);
        }
        detail.setLivello(livelloDaEreditare);

        List<AttoreArchivio> attoriList = new ArrayList<AttoreArchivio>();
        for (AttoreArchivio attore: archDaCopiare.getAttoriList()){
            AttoreArchivio newAttore = new AttoreArchivio(newArchivio, attore.getIdPersona(), attore.getIdStruttura(), attore.getRuolo());
            em.persist(newAttore);
            em.refresh(newAttore);
            attoriList.add(newAttore);
        }
        newArchivio.setAttoriList(attoriList);
        
        
        if(numera){
            detail.setStato(Archivio.StatoArchivio.BOZZA);
            archivioRepository.numeraArchivio(newArchivio.getId());
        }
        archivioRepository.calcolaPermessiEspliciti(newArchivio.getId());
        return newArchivio;
    }
    
    public void coiaArchivioDoc(Archivio archDaCopiare, Archivio archivioDestinazione, Persona utente, EntityManager em){
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
            ArchivioDoc newArchivioDoc = new ArchivioDoc(archivioDestinazione, idDoc, utente);
            em.persist(newArchivioDoc);
        }
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
