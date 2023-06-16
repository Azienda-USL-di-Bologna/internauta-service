package it.bologna.ausl.internauta.service.controllers.logs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.blackbox.utils.UtilityFunctions;
import it.bologna.ausl.internauta.service.krint.KrintLogDescription;
import it.bologna.ausl.internauta.service.repositories.logs.KrintRepository;
import it.bologna.ausl.internauta.model.bds.types.PermessoEntitaStoredProcedure;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author gusgus
 */
@RestController
@RequestMapping(value = "${logs.mapping.url.root}")
public class LogsCustomController {
    
    @Autowired
    private KrintRepository krintRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @RequestMapping(value = "get_logs", method = RequestMethod.GET)
    public ResponseEntity<List<KrintLogDescription>> getLogs(
            HttpServletRequest request,
            @RequestParam(name = "codiciOperazioni", required = false) List<String> codiciOperazioni,
            @RequestParam(name = "idOggetto", required = false) String idOggetto,
            @RequestParam(name = "tipoOggetto", required = false) String tipoOggetto,
            @RequestParam(name = "idUtente", required = false) Integer idUtente,
            @RequestParam(name = "idOggettoContenitore", required = false) String idOggettoContenitore,
            @RequestParam(name = "tipoOggettoContenitore", required = false) String tipoOggettoContenitore,
            @RequestParam(name = "dataDa", required = false) LocalDate dataDa,
            @RequestParam(name = "dataA", required = false) LocalDate dataA
    ) throws JsonProcessingException {
        // Mi aggiusto i parametri per la chiamata alla stored procedure.
        String dataDaStringa = null;
        String dataAStringa = null;
        String codiciOperazioniStringa = null;
        if (dataDa != null) dataDaStringa = UtilityFunctions.getLocalDateString(dataDa);
        if (dataA != null) dataAStringa = UtilityFunctions.getLocalDateString(dataA);
        if (codiciOperazioniStringa != null) codiciOperazioniStringa = UtilityFunctions.getArrayString(objectMapper, codiciOperazioni);
         
        // Effettuo la chiamata. Poi la parso e la ritorno.
        String res = krintRepository.getLogs(codiciOperazioniStringa, idOggetto, tipoOggetto, idUtente, idOggettoContenitore, tipoOggettoContenitore, dataDaStringa, dataAStringa);
        List<KrintLogDescription> logs = null;
        if (res != null) logs = objectMapper.readValue(res, new TypeReference<List<KrintLogDescription>>() {});
        return new ResponseEntity(logs, HttpStatus.OK);
    }
}
