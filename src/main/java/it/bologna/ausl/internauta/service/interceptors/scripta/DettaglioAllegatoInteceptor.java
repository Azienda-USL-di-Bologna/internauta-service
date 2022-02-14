///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package it.bologna.ausl.internauta.service.interceptors.scripta;
//
//import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
//import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
//import it.bologna.ausl.internauta.service.repositories.scripta.AllegatoRepository;
//import it.bologna.ausl.internauta.service.utils.InternautaConstants;
//import it.bologna.ausl.minio.manager.MinIOWrapper;
//import it.bologna.ausl.model.entities.scripta.DettaglioAllegato;
//import it.nextsw.common.annotations.NextSdrInterceptor;
//import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
//import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
//import java.util.HashSet;
//import java.util.Map;
//import java.util.Set;
//import javax.servlet.http.HttpServletRequest;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
///**
// *
// * @author Mdor
// */
//@Component
//@NextSdrInterceptor(name = "dettaglioallegato-interceptor")
//public class DettaglioAllegatoInteceptor extends InternautaBaseInterceptor {
//    private static final Logger LOGGER = LoggerFactory.getLogger(DettaglioAllegatoInteceptor.class);
//
//    @Autowired
//    ReporitoryConnectionManager aziendeConnectionManager;
//
//    @Autowired
//    AllegatoRepository allegatoRepository;    
//    
//    
//    @Override
//    public Class getTargetEntityClass() {
//        return DettaglioAllegato.class;
//    }
//    
//    @Override
//    public void beforeDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
//        DettaglioAllegato dettaglioAllegato = (DettaglioAllegato) entity;
//        //TODO: in caso di fallimento quando si hanno piu dettagli allegati se uno fallisce bisogna fare l'undelete degli altri
//        MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
//        try {
//            Set<String> data = (Set) super.httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.DettagliAllegatiDaEliminare);
//            if (data == null){
//                data = new HashSet();
//                super.httpSessionData.putData(InternautaConstants.HttpSessionData.Keys.DettagliAllegatiDaEliminare, data);
//            }
//            data.add(dettaglioAllegato.getIdRepository());
//            
//            
//        } catch (Exception ex) {
//            LOGGER.error("errore nell'eliminazione del file su minIO",ex);
//            throw new AbortSaveInterceptorException("errore nell'eliminazione del file su minIO",ex);
//        }
//    }
//    
//}
