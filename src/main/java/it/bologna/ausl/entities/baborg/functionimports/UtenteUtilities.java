package it.bologna.ausl.entities.baborg.functionimports;


import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import it.bologna.ausl.entities.baborg.QUtenteStruttura;
import it.bologna.ausl.entities.baborg.Utente;
import it.nextsw.olingo.edmextension.annotation.EdmFunctionImportClass;
import org.apache.log4j.Logger;
import org.apache.olingo.odata2.api.annotation.edm.EdmFacets;
import org.apache.olingo.odata2.api.annotation.edm.EdmFunctionImport;
import org.apache.olingo.odata2.api.annotation.edm.EdmFunctionImportParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;

@EdmFunctionImportClass
@Component
public class UtenteUtilities {

    private static final Logger logger = Logger.getLogger(UtenteUtilities.class);

    @PersistenceContext
    private EntityManager em;

//    @Autowired
//    UtenteRepository utenteRepository;

    @EdmFunctionImport(
            name = "GetUtentiByAzienda",
            entitySet = "Utentes",
            returnType = @EdmFunctionImport.ReturnType(type = EdmFunctionImport.ReturnType.Type.ENTITY, isCollection = true),
            httpMethod = EdmFunctionImport.HttpMethod.GET
    )
    public List<Utente> getUtentiByAzienda(
            @EdmFunctionImportParameter(name = "idAzienda",  facets = @EdmFacets(nullable = false))
            final Integer idAzienda
    ){
        logger.info("sono in getUtentiByAzienda, idAzienda: " + idAzienda);
        JPQLQuery query=new JPAQuery(em);
        query.select(QUtenteStruttura.utenteStruttura.idUtente).from(QUtenteStruttura.utenteStruttura).where(QUtenteStruttura.utenteStruttura.idStruttura.idAzienda.id.eq(idAzienda));
        List<Utente> utenti=query.fetch();

//        List<Utente> utenti=em.createQuery("select u from Utente as u where u.id<12700").getResultList();
//        List<Utente> utenti=em.createQuery("select us.idUtente from UtenteStruttura as us where us.idStruttura.id="+idAzienda).getResultList();
        return utenti;
    }
}


