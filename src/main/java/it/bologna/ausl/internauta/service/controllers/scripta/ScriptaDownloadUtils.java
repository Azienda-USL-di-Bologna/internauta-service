//package it.bologna.ausl.internauta.service.controllers.scripta;
//
//import com.querydsl.jpa.impl.JPAQueryFactory;
//import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
//import it.bologna.ausl.internauta.service.exceptions.http.Http404ResponseException;
//import it.bologna.ausl.minio.manager.MinIOWrapper;
//import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
//import it.bologna.ausl.model.entities.scripta.Allegato;
//import it.bologna.ausl.model.entities.scripta.QAllegato;
//import java.io.InputStream;
//import java.lang.reflect.InvocationTargetException;
//import java.util.List;
//import java.util.Optional;
//import javax.persistence.EntityManager;
//import javax.persistence.LockModeType;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.springframework.util.StreamUtils;
//
///**
// *
// * @author gusgus
// */
//@Component
//public class ScriptaDownloadUtils {
//    
//    @Autowired
//    private EntityManager entityManager;
//    
//    @Autowired
//    private ReporitoryConnectionManager aziendeConnectionManager;
//    
//    /**
//     * 
//     * @param idRepository
//     * @throws MinIOWrapperException 
//     */
//    public InputStream downloadAttachment(
//            Allegato allegato,  
//            Allegato.DettagliAllegato.TipoDettaglioAllegato tipoDettaglioAllegato
//    ) throws MinIOWrapperException {
////        MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
////        InputStream file = minIOWrapper.getByFileId(idRepository);
////        if (file == null) {
////            if (allegato.getIdAllegatoPadre() != null) {
////                Allegato allegatoRadice = getAllegatoRadice(allegato);
////            }
////        }
//    }
//    
//    
//    
//    /**
//     * Dato un allegato viene convertito il dettaglio orginale, nel caso non sia convertibile
//     * viene creato il segnaposto.
//     */
//    public void convertiOriginaleInPdf(Allegato allegato) 
//            throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
//        Allegato.DettaglioAllegato dettaglioAllegatoOriginale = allegato.getDettagli().getDettaglioAllegato(Allegato.DettagliAllegato.TipoDettaglioAllegato.ORIGINALE);
//        
//    }
//    
//    /**
//     * Dato il contenitore devo estrarre il contenuto e poi andare a salvare/aggiornare questo contenuto 
//     */
//    private updateTemporaryFileInfo(Allegato contenitore) {
//        
//    }
//    
//    
//    private InputStream convertToPdf(Allegato allegato, InputStream fileToConvert, String idRepositoryScadutoPdf) throws MinIOWrapperException {
//        // Eseguo la select for update
//        Optional<Allegato> res = null;
//        QAllegato qAllegato = QAllegato.allegato;
//        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
//
//        Allegato allegatoForUpdate = queryFactory.query().setLockMode(LockModeType.PESSIMISTIC_WRITE)
//            .select(qAllegato)
//            .from(qAllegato)
//            .where(qAllegato.id.eq(allegato.getId()))
//            .fetchOne();
//        
//        // Ora che ho l'allegato, controllo se qualcun'altro ha appena salavato il convertito
//        // Controllo quindi se ora il convertito c'è e se ha l'idRepository diverso da quello scaduto passato come parametro
//        Allegato.DettagliAllegato dettagliForUpdate = allegatoForUpdate.getDettagli();
//        Allegato.DettaglioAllegato convertitoForUpdate = dettagliForUpdate.getConvertito();
//        
//        if (convertitoForUpdate != null && !convertitoForUpdate.getIdRepository().equals(idRepositoryScadutoPdf)) {
//            MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
//            return minIOWrapper.getByFileId(convertitoForUpdate.getIdRepository());
//        }
//        
//        // Il convertito nuovo non c'è già. sta a me crearo e aggiornare i dati dell'allegato
//        
//    }
//    
//    /**
//     * Metodo chiamato se si voleva un convertito che però non c'è (magari è scaduto)
//     */
//    public InputStream downloadOriginaleAndConvertToPdf(Allegato allegato, String idRepositoryScadutoPdf) throws MinIOWrapperException, Http404ResponseException {
//        InputStream originale = downloadAttachment(allegato, Allegato.DettagliAllegato.TipoDettaglioAllegato.ORIGINALE); // Eventualmente ricreando il temp se lo era e nel frattempo è scaduto
//        if (originale == null) {
//            throw new Http404ResponseException("5", "Manca l'originale dell'allegato richiesto");
//        } else {
//            return convertToPdf(allegato, originale, idRepositoryScadutoPdf);
//        }
//    }
//    
//    private Allegato getAllegatoRadice(Allegato allegato) {
//        Allegato idAllegatoPadre = allegato.getIdAllegatoPadre();
//        if (idAllegatoPadre != null) {
//            return getAllegatoRadice(idAllegatoPadre);
//        } else {
//            return allegato;
//        }
//    }
//}
