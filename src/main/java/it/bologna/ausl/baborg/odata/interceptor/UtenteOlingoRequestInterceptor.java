package it.bologna.ausl.baborg.odata.interceptor;

import com.querydsl.core.types.Predicate;
import it.bologna.ausl.entities.baborg.QUtente;
import it.bologna.ausl.entities.baborg.Utente;
import it.nextsw.olingo.interceptor.OlingoInterceptorOperation;
import it.nextsw.olingo.interceptor.bean.BinaryGrantExpansionValue;
import it.nextsw.olingo.interceptor.bean.OlingoQueryObject;
import it.nextsw.olingo.interceptor.exception.OlingoRequestRollbackException;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import org.springframework.stereotype.Component;

/**
 *
 * @author Utente
 */
@Component
public class UtenteOlingoRequestInterceptor extends OlingoRequestInterceptorBase{

    @Override
    public Predicate onQueryInterceptor(OlingoQueryObject olingoQueryObject) {
        //Predicate p = QUtente.utente.cognome.contains("'Olivieri'");
//        Predicate p = QUtente.utente.attivo.eq(Boolean.FALSE);
        //return p;
        return null;
    }

    @Override
    public Object onChangeInterceptor(OlingoInterceptorOperation olingoInterceptorOperation, Object object, EntityManager entityManager, Map<String, Object> contextAdditionalData) throws OlingoRequestRollbackException {
        return object;
    }

    @Override
    public void onDeleteInterceptor(Object object, EntityManager entityManager, Map<String, Object> contextAdditionalData) throws OlingoRequestRollbackException {
    }

    @Override
    public Class<?> getReferenceEntity() {
        return Utente.class;
    }

    @Override
    public void onGrantExpandsAuthorization(List<BinaryGrantExpansionValue> binaryGrantExpansionValues) {
        BinaryGrantExpansionValue binaryGrantExpansionValue = new BinaryGrantExpansionValue("idUtente", Boolean.TRUE);
    }
    
}
