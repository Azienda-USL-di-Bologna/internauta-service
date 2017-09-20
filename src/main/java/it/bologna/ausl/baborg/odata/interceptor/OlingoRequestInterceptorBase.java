package it.bologna.ausl.baborg.odata.interceptor;

import com.querydsl.core.types.Predicate;
//import it.nextsw.entities.Utente;
import it.bologna.ausl.baborg.odata.contex.CustomOdataJpaContextBase;
import it.nextsw.olingo.interceptor.OlingoInterceptorOperation;
import it.nextsw.olingo.interceptor.OlingoRequestInterceptorImpl;
import it.nextsw.olingo.interceptor.bean.OlingoQueryObject;
import it.nextsw.olingo.interceptor.exception.OlingoRequestRollbackException;
import org.apache.log4j.Logger;
import org.apache.olingo.odata2.jpa.processor.api.ODataJPAContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;


/**
 * Personalizzazione del generico handler secondo standard di progetto
 * Estendere questo handler per implementare la sicurezza sulle chiamate alla servlet di olingo
 * <p>
 * Created by f.longhitano on 30/06/2017.
 */
@Component
public abstract class OlingoRequestInterceptorBase extends OlingoRequestInterceptorImpl {

    private static Logger logger = Logger.getLogger(OlingoRequestInterceptorBase.class);


//    protected static String JPQL_ENTITY_NAME_REGEX = "(?:^|[^'])(%s)\\.\\w+[^']";
//    protected Pattern JPQL_ENTITY_NAME_PATTERN;
//    protected static Pattern JPQL_AND_PATTER=Pattern.compile("(?!\\B'[^']*)(&&)(?![^']*'\\B)");
//    protected static Pattern JPQL_OR_PATTER=Pattern.compile("(?!\\B'[^']*)(\\|\\|)(?![^']*'\\B)");

//    @PostConstruct
//    public void init() {
//        this.compilePatterns();
//    }
//
//    protected void compilePatterns() {
//        String entityPattern = String.format(JPQL_ENTITY_NAME_REGEX, getReferenceEntity().getSimpleName().toLowerCase());
//        JPQL_ENTITY_NAME_PATTERN = Pattern.compile(entityPattern);
//    }


    @Override
    public final void onUpdateEntityQueryEdit(OlingoQueryObject olingoQueryObject) {
        Predicate predicate = this.onQueryInterceptor(olingoQueryObject);
        olingoQueryObject.setCustomWhere(preticateToString(olingoQueryObject, predicate));
    }


    @Override
    public final Object onUpdateEntityPostProcess(Object object, ODataJPAContext oDataJPAContext) throws OlingoRequestRollbackException {
        return this.onChangeInterceptor(OlingoInterceptorOperation.UPDATE, object, oDataJPAContext.getEntityManager(), getAdditionalDataFromContext(oDataJPAContext));
    }

    @Override
    public final void onQueryEntityQueryEdit(OlingoQueryObject olingoQueryObject) {
        Predicate predicate = this.onQueryInterceptor(olingoQueryObject);
        olingoQueryObject.setCustomWhere(preticateToString(olingoQueryObject, predicate));
    }


    protected String preticateToString(OlingoQueryObject olingoQueryObject, Predicate predicate) {

        if (predicate != null) {
            String predicateString= OlingoRequestInterceptorBase.DSLQueryStringToJPQL(predicate.toString(),olingoQueryObject.getOlingoEntityName().toLowerCase(),olingoQueryObject.getOlingoEntityAlias());
            return predicateString;
            //cambia nome entità
//            String predicateString = change(JPQL_ENTITY_NAME_PATTERN, predicate.toString(), olingoQueryObject.getOlingoEntityAlias());
//            //and
//            predicateString=change(JPQL_AND_PATTER, predicateString, "and");
//            //or
//            predicateString=change(JPQL_OR_PATTER, predicateString, "or");

//            String predicateString = predicate.toString()
//                    .replaceFirst(getReferenceEntity().getSimpleName().toLowerCase() + ".", olingoQueryObject.getOlingoEntityAlias() + ".")
//                    .replaceAll(" " + getReferenceEntity().getSimpleName().toLowerCase() + ".", " " + olingoQueryObject.getOlingoEntityAlias() + ".")
//                    .replaceAll("&&", "and")
//                    .replaceAll("\\|\\|", "or");
        }
        return null;
    }

    /**
     * Converte un predicato DSL nella forma JPQL. In particolare:
     * - sostituisce && e || con 'and' e 'or' (omettendo eventuali && o || indicati tra apici)
     * - sostituisce le entità  indicate nel parametro 'entityName' con la stringa indicata nel parametro 'jpqlEntityName'
     * (anche in questo caso omettendo quelle tra apici)
     * @param str stringa corrispondente alla query DSL
     * @param entityName nome dell'entità  interrogata
     * @param jpqlEntityName nuovo nome da assegnare all'entitÃ 
     * @return la query nel formato JPQL
     */
    public static String DSLQueryStringToJPQL(String str, String entityName, String jpqlEntityName){
        String andReplacement = generateReplacementGuid(str);
        String orReplacement = generateReplacementGuid(str);
        String entityReplacement = generateReplacementGuid(str);

        // Salvo tutte le posizioni in cui nella stringa compare un'apice (escluse quelle in cui prima dell'apice
        // c'Ã¨ il carattere '\', perchÃ¨ vuol dire che si tratta di un escape)
        ArrayList<Integer> indexes = new ArrayList<>();
        int actualPosition = 0;
        while (str.indexOf("'", actualPosition) != -1){
            int foundPosition = str.indexOf("'", actualPosition);
            if (str.charAt(foundPosition - 1) != '\\'){
                indexes.add(foundPosition);
            }
            actualPosition = foundPosition + 1;
        }

        // Giro tutti i pezzi di stringa tra apici sostituendo dove opportuno gli &&, gli || e l'entityName
        // con i rimpiazzamenti generati sopra; nella lista ci saranno alternate le posizioni degli apici
        // di apertura (in posizioni della lista dispari) e gli apici di chiusura (in posizioni pari)
        int i = 1;
        int prevPosition = 0;
        String resultString = "";
        for (Integer position : indexes) {
            if (i % 2 == 0){
                // Se l'apice che sto considerando Ã¨ in una posizione pari della lista, vuol dire che Ã¨ un'apice di chiusura.
                // Posso quindi recuperare tutto il testo tra apici (utilizzando la posizione precedente) ed effettuare le sostituzioni
                String stringInApexes = str.substring(prevPosition, position);
                stringInApexes = stringInApexes.replace("&&", andReplacement).replace("||", orReplacement).replace(entityName, entityReplacement);
                resultString += stringInApexes;
            } else {
                resultString += str.substring(prevPosition,position);

            }
            prevPosition = position;
            i++;
        }

        resultString += str.substring(prevPosition, str.length());
        resultString = resultString.replace("&&", "and");
        resultString = resultString.replace("||", "or");
        resultString = resultString.replace(entityName, jpqlEntityName);
        resultString = resultString.replace(andReplacement, "&&");
        resultString = resultString.replace(orReplacement, "||");
        resultString = resultString.replace(entityReplacement, entityName);

        return resultString;
    }

    /**
     * Genera un GUID per la sostituzione all'interno della stringa: ossia genera un UUID che non è presente
     * all'interno della stringa passata
     * @param str
     * @return il UUID generato
     */
    public static String generateReplacementGuid(String str){
        String replacementGuid = UUID.randomUUID().toString();
        while (str.contains(replacementGuid)){
            replacementGuid = UUID.randomUUID().toString();
        }
        return replacementGuid;
    }

//    public String change(Pattern p, String src, String replacement) {
//        //Pattern p = Pattern.compile(pattern);
//        Matcher m = p.matcher(src);
//        StringBuffer sb = new StringBuffer();
//        int last = 0;
//        while (m.find()) {
//            sb.append(src.substring(last, m.start(1)));
//            sb.append(replacement);
//            last = m.end(1);
//
//        }
//        sb.append(src.substring(last));
//        return sb.toString();
//
//    }


    @Override
    public Object onQueryEntityPostProcess(Object object, ODataJPAContext oDataJPAContext) {
        return object;
    }

    @Override
    public void onDeleteEntityQueryEdit(OlingoQueryObject olingoQueryObject) {
        Predicate predicate = this.onQueryInterceptor(olingoQueryObject);
        olingoQueryObject.setCustomWhere(preticateToString(olingoQueryObject, predicate));
    }


    @Override
    public final void onDeleteEntityPostProcess(Object object, ODataJPAContext oDataJPAContext) throws OlingoRequestRollbackException {
        onDeleteInterceptor(object, oDataJPAContext.getEntityManager(), getAdditionalDataFromContext(oDataJPAContext));
    }


    @Override
    public final Object onCreateEntityPostProcess(Object object, ODataJPAContext oDataJPAContext) throws OlingoRequestRollbackException {
        return this.onChangeInterceptor(OlingoInterceptorOperation.CREATE, object, oDataJPAContext.getEntityManager(), getAdditionalDataFromContext(oDataJPAContext));
    }

    protected Map getAdditionalDataFromContext(ODataJPAContext oDataJPAContext) {
        return oDataJPAContext.getClass().isAssignableFrom(CustomOdataJpaContextBase.class) ? ((CustomOdataJpaContextBase) oDataJPAContext).getContextAdditionalData() : null;
    }


    /**
     * Metodo richiamato ogni qual volta venga fatta una query sul tipo di entità dichiarato {@link #getReferenceEntity()}
     *
     * @param olingoQueryObject Contiene le informazioni sulla query richiesta
     * @return un predicato che andrà in and con le altre condizioni provenienti dall'url
     */
    public abstract Predicate onQueryInterceptor(final OlingoQueryObject olingoQueryObject);

    /**
     * Metodo richiamato ogni qual volta un'entità di tipo {@link #getReferenceEntity()} venga creata o updatata
     *
     * @param olingoInterceptorOperation il tipo di operazione: CREATE, UPDATE
     * @param object                  l'oggetto che si sta creando o updatando
     * @param entityManager           l'entity manager contenete la transazione su cui vengono eseguite le operazioni
     * @param contextAdditionalData   una mappa che contiene eventuali informazioni di sessione (da l'ingresso di OdataRequest all'uscita di OdataResponse), ha un reale utilizzo solo in caso di batch
     * @return l'oggetto eventualmente modificato
     * @throws OlingoRequestRollbackException lanciando questa eccezione il sistema fa rollback della transazione
     */
    public abstract Object onChangeInterceptor(OlingoInterceptorOperation olingoInterceptorOperation, Object object, EntityManager entityManager, Map<String, Object> contextAdditionalData) throws OlingoRequestRollbackException;

    /**
     * Metodo richiamato ogni qual volta un'entità di tipo {@link #getReferenceEntity()} venga cancellata
     *
     * @param object                l'entità che sta per essere cancellata
     * @param entityManager         l'entity manager contenete la transazione su cui vengono eseguite le operazioni
     * @param contextAdditionalData una mappa che contiene eventuali informazioni di sessione (da l'ingresso di OdataRequest all'uscita di OdataResponse), ha un reale utilizzo solo in caso di batch
     * @throws OlingoRequestRollbackException lanciando questa eccezione il sistema fa rollback della transazione
     */
    public abstract void onDeleteInterceptor(Object object, EntityManager entityManager, Map<String, Object> contextAdditionalData) throws OlingoRequestRollbackException;

    protected Authentication getAuthentication() {
        if (SecurityContextHolder.getContext().getAuthentication() != null)
            return SecurityContextHolder.getContext().getAuthentication();
        return null;
    }

//    protected Utente getUtente() {
//        if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() != null && Utente.class.isAssignableFrom(SecurityContextHolder.getContext().getAuthentication().getPrincipal().getClass()))
//            return (Utente) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//        return null;
//    }


}
