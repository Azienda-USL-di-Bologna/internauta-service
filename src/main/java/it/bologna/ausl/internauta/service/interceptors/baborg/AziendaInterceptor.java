package it.bologna.ausl.internauta.service.interceptors.baborg;

import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.QAzienda;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.NextSdrEmptyControllerInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import org.eclipse.persistence.internal.jpa.EntityManagerImpl;
import static org.hibernate.jpa.AvailableSettings.PERSISTENCE_UNIT_NAME;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
@NextSdrInterceptor(name = "azienda-interceptorTest")
public class AziendaInterceptor extends NextSdrEmptyControllerInterceptor {

    @Autowired
    AziendaRepository aziendaRepository;

    @PersistenceContext
    EntityManager em;

    @Override
    public Class getTargetEntityClass() {
        return Azienda.class;
    }

//    @Override
//    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request) {
//        return QAzienda.azienda.id.eq(2).and(initialPredicate);
//    }
    @Override
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) {
        if (entity != null) {
            Azienda azienda = (Azienda) entity;
//            if (azienda.getId() != 2) {
//                System.out.println("222222222222222222");
//                return null;
//            }
//            else {
//                System.out.println("hahahahahahayh");
//                return azienda;
//            }
//            azienda.setAoo(UUID.randomUUID().toString().substring(0, 15));
//            EntityManager createEntityManager = em.getEntityManagerFactory().createEntityManager();
//            createEntityManager.merge(azienda);
//            aziendaRepository.save(azienda);

            return azienda;
        } else {
            return entity;
        }
    }

    @Override
    public Object beforeCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws AbortSaveInterceptorException {
        Azienda a = (Azienda) entity;
        return a;
    }

    @Override
    public Object beforeUpdateEntityInterceptor(Object entity, Object beforeUpdateEntity, Map<String, String> additionalData, HttpServletRequest request) throws AbortSaveInterceptorException {
        Azienda a = (Azienda) entity;
        Azienda aOld = (Azienda) beforeUpdateEntity;

        Optional<Azienda> findOne = aziendaRepository.findOne(QAzienda.azienda.aoo.eq("messamo"));
        if (findOne.isPresent()) {
            System.out.println("messamo trovata: " + findOne.get());
        }

        Azienda one = aziendaRepository.getOne(17);
        if (one != null) {
            System.out.println("aoo rp della 17: " + one.getAoo());
        }

        Azienda find = em.find(Azienda.class, 17);
        if (find != null) {
            System.out.println("aoo em della 17: " + find.getAoo());
        }

        System.out.println("aoo em della 17old: " + aOld.getAoo());
        return a;
    }

    @Override
    public void beforeDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
        Azienda a = (Azienda) entity;
        throw new SkipDeleteInterceptorException("non la voglio cancellare l'aziedna con id: " + a.getId());
    }
}
