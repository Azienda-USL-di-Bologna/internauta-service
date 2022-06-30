
package it.bologna.ausl.internauta.service.krint;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.model.entities.logs.Krint;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.logs.projections.KrintRubricaContatto;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Service;

/**
 *
 * @author gusgus
 */
@Service
public class KrintScriptaService {
    
    @Autowired
    ProjectionFactory factory;
   
    @Autowired
    ObjectMapper objectMapper;
    
    @Autowired
    KrintService krintService;
    
    /**
     */
//    public void writeAttoreArchivioCreation(Contatto contatto, OperazioneKrint.CodiceOperazione codiceOperazione) {
//        try {
//            // Informazioni oggetto
//            KrintRubricaContatto krintRubricaContatto = factory.createProjection(KrintRubricaContatto.class, contatto);
//            String jsonKrintContatto = objectMapper.writeValueAsString(krintRubricaContatto);
//            
//            krintService.writeKrintRow(
//                contatto.getId().toString(),
//                Krint.TipoOggettoKrint.RUBRICA_CONTATTO,
//                contatto.getDescrizione(),
//                jsonKrintContatto,
//                null,
//                null,
//                null,
//                null,
//                codiceOperazione);
//        } catch (Exception ex) {
//            Integer idOggetto = null;
//            try {
//                ex.printStackTrace();
//                idOggetto = contatto.getId();
//            } catch (Exception exa) {}
//            krintService.writeKrintError(idOggetto, "writeContactCreation", codiceOperazione);
//        }
//    }
}
