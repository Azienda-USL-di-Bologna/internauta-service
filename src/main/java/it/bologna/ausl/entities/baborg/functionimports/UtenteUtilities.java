package it.bologna.ausl.entities.baborg.functionimports;

import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;

import it.bologna.ausl.entities.baborg.QUtenteStruttura;
import it.bologna.ausl.entities.baborg.Utente;
import it.nextsw.olingo.edmextension.EdmFunctionImportClassBase;
import it.nextsw.olingo.edmextension.annotation.EdmFunctionImportClass;
import org.apache.log4j.Logger;
import org.apache.olingo.odata2.api.annotation.edm.EdmFacets;
import org.apache.olingo.odata2.api.annotation.edm.EdmFunctionImport;
import org.apache.olingo.odata2.api.annotation.edm.EdmFunctionImportParameter;
import org.apache.olingo.odata2.jpa.processor.core.access.data.JPAQueryInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

@EdmFunctionImportClass
@Component
public class UtenteUtilities extends EdmFunctionImportClassBase{

    private static final Logger logger = Logger.getLogger(UtenteUtilities.class);

    @PersistenceContext
    private EntityManager em;

//    @Autowired
//    UtenteRepository utenteRepository;

//    @EdmFunctionImport(
//            name = "GetUtentiByAzienda",
//            entitySet = "Utentes",
            returnType = @EdmFunctionImport.ReturnType(type = EdmFunctionImport.ReturnType.Type.ENTITY, formatResult = EdmFunctionImport.FormatResult.PAGINATED_COLLECTION, EdmEntityTypeName = "Utente"),
//            httpMethod = EdmFunctionImport.HttpMethod.GET
//    )
    public JPAQueryInfo getUtentiByAzienda(
//            @EdmFunctionImportParameter(name = "idAzienda",  facets = @EdmFacets(nullable = false))
//            final Integer idAzienda
//    ){
//        logger.info("sono in getUtentiByAzienda, idAzienda: " + idAzienda);
        JPAQuery queryDSL=new JPAQuery(em);
        queryDSL.select(QUtenteStruttura.utenteStruttura.idUtente).from(QUtenteStruttura.utenteStruttura).where(QUtenteStruttura.utenteStruttura.idStruttura.idAzienda.id.eq(idAzienda));
//        Query query=queryDSL.createQuery();
//        Query countQuery =queryDSL.clone(em).select(QUtenteStruttura.utenteStruttura.idUtente.count()).createQuery();
////        List<Utente> utenti=em.createQuery("select u from Utente as u where u.id<12700").getResultList();
        return createQueryInfo(queryDSL,QUtenteStruttura.utenteStruttura.idUtente.count(),em);
    }
    }

}

// vecchia versione
//import com.querydsl.core.SimpleQuery;
//import com.querydsl.jpa.JPQLQuery;
//import com.querydsl.jpa.impl.JPAQuery;
//import it.bologna.ausl.entities.baborg.QUtenteStruttura;
//import it.bologna.ausl.entities.baborg.Utente;
//import it.nextsw.olingo.edmextension.annotation.EdmFunctionImportClass;
//import org.apache.log4j.Logger;
//import org.apache.olingo.odata2.api.annotation.edm.EdmFacets;
//import org.apache.olingo.odata2.api.annotation.edm.EdmFunctionImport;
//import org.apache.olingo.odata2.api.annotation.edm.EdmFunctionImportParameter;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import javax.persistence.EntityManager;
//import javax.persistence.PersistenceContext;
//import java.util.ArrayList;
//import java.util.List;
//import org.apache.olingo.odata2.api.uri.info.GetEntitySetUriInfo;
//import org.apache.olingo.odata2.core.uri.UriInfoImpl;
//import org.hibernate.property.access.spi.GetterFieldImpl;
//import org.springframework.context.ApplicationContext;
//import static org.springframework.security.core.context.SecurityContextHolder.getContext;
//
//@EdmFunctionImportClass
//@Component
//public class UtenteUtilities {
//
//    private static final Logger logger = Logger.getLogger(UtenteUtilities.class);
//
//    @PersistenceContext
//    private EntityManager em;
//
////    @Autowired
////    UtenteRepository utenteRepository;
//
//    @EdmFunctionImport(
//            name = "GetUtentiByAzienda",
//            entitySet = "Utentes",
//            returnType = @EdmFunctionImport.ReturnType(type = EdmFunctionImport.ReturnType.Type.ENTITY, isCollection = true),
//            httpMethod = EdmFunctionImport.HttpMethod.GET
//    )
//    public List<Utente> getUtentiByAzienda(
//            @EdmFunctionImportParameter(name = "idAzienda",  facets = @EdmFacets(nullable = false))
//            final Integer idAzienda,
//            
//            @EdmFunctionImportParameter(name = "skip", facets = @EdmFacets(nullable = true))
//            Integer skip,
//            @EdmFunctionImportParameter(name = "top", facets = @EdmFacets(nullable = true))
//            Integer top
//    ){
//        
//        
//        logger.info("sono in getUtentiByAzienda, idAzienda: " + idAzienda);
//        JPQLQuery query=new JPAQuery(em);
//        query.select(QUtenteStruttura.utenteStruttura.idUtente).from(QUtenteStruttura.utenteStruttura).where(QUtenteStruttura.utenteStruttura.idStruttura.idAzienda.id.eq(idAzienda));
//        if (skip != null && top != null)
//            query = (JPQLQuery) query.offset(skip).limit(top);
//        
//        List<Utente> utenti=query.fetch();
//
////        List<Utente> utenti=em.createQuery("select u from Utente as u where u.id<12700").getResultList();
////        List<Utente> utenti=em.createQuery("select us.idUtente from UtenteStruttura as us where us.idStruttura.id="+idAzienda).getResultList();
//        return utenti;
//    }
//}
//
//
