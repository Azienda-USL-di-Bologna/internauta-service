package it.bologna.ausl.internauta.service.controllers.baborg;

import com.mongodb.MongoException;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.baborg.utils.BaborgUtils;
import it.bologna.ausl.internauta.service.configuration.utils.MongoConnectionManager;
import it.bologna.ausl.internauta.service.exceptions.http.Http400ResponseException;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.ImportazioniOrganigrammaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.model.entities.baborg.ImportazioniOrganigramma;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.mongowrapper.MongoWrapper;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import okhttp3.Response;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value = "${baborg.mapping.url.root}")
public class BaborgCustomController {

    private static final Logger log = LoggerFactory.getLogger(RestController.class);

    @Autowired
    MongoConnectionManager mongoConnectionManager;

    @Autowired
    StrutturaRepository strutturaRepository;

    @Autowired
    AziendaRepository aziendaRepository;

    @Autowired
    BaborgUtils baborgUtils;

    @Autowired
    UserInfoService infoService;

    @Autowired
    AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;

    @Autowired
    ImportazioniOrganigrammaRepository importazioniOrganigrammaRepository;

    @RequestMapping(value = "struttureAntenate/{idStruttura}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> struttureAntenate(
            @PathVariable(required = true) Integer idStruttura) {

        String struttureAntenate = strutturaRepository.getStruttureAntenate(idStruttura);
        System.out.println("struttureAntenate: " + struttureAntenate);

        // trasformiamo la stringa restituita in un array
        String[] struttureAntenateArray = struttureAntenate.split(",");

        List<Integer> struttureAntenateList = Arrays.stream(struttureAntenateArray)
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        // invertiamo l'ordine degli elementi nell'array in modo che
        // l'id della struttura radice sia in prima posizione, quello della struttura passata in ultima
        Collections.reverse(struttureAntenateList);

        return new ResponseEntity(struttureAntenateList, HttpStatus.OK);
    }

    @RequestMapping(value = "insertImpOrgRowAndCsvUpload", method = RequestMethod.POST)
    public ResponseEntity<String> insertImpOrgRowAndCsvUpload(
            HttpServletRequest request,
            @RequestParam("codiceAzienda") String codiceAzienda,
            @RequestParam("idAzienda") String idAzienda,
            @RequestParam("tipo") String tipo,
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileName") String fileName) throws BlackBoxPermissionException, IOException, Http400ResponseException, Exception, Exception {

        // Carico utente e persona
        ImportazioniOrganigramma newRowInserted = null;
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Persona person = null;
        String completeDataFile = null;

        if (authenticatedUserProperties.getRealPerson() != null) {
            person = authenticatedUserProperties.getRealPerson();
        } else {
            person = authenticatedUserProperties.getPerson();
        }

        Utente user = null;
        if (authenticatedUserProperties.getRealUser() != null) {
            user = authenticatedUserProperties.getRealUser();
        } else {
            user = authenticatedUserProperties.getUser();
        }

        ImportazioniOrganigramma res = null;

        // Se sono CA e i parametri passati sono corretti inizio l'importazione
        if (infoService.isCA(user) && !file.isEmpty() && idAzienda != null && tipo != null) {
            ImportazioniOrganigramma newRowImportazione = null;
            newRowImportazione = baborgUtils.insertNewRowImportazioneOrganigrama(user.getId(), idAzienda, tipo, codiceAzienda, fileName, person, newRowImportazione);
            if (newRowImportazione != null) {
                res = baborgUtils.manageUploadFile(user.getId(), file, idAzienda, tipo, codiceAzienda, fileName, person, newRowImportazione);
            } else {
                throw new Http400ResponseException("1", "Non è stato posibile far partire l'importazione");
            }
        } else {
            throw new Http400ResponseException("2", "I dati passati per l'importazione sono assenti o non corretti");
        }

        return new ResponseEntity(res, HttpStatus.OK);
    }

    @RequestMapping(value = "downloadFileFromUUIDAndidAzienda", method = RequestMethod.GET)
    public void downloadFileFromUUIDAndidAzienda(
            @RequestParam(required = true) String uuid,
            @RequestParam(required = true) Integer idAzienda,
            HttpServletResponse response,
            HttpServletRequest request) throws FileNotFoundException {

        MongoWrapper mongoWrapper = mongoConnectionManager.getConnection(idAzienda);
        InputStream is = null;
        String fileName = String.format("%s_%d_%s.csv", uuid, idAzienda, UUID.randomUUID().toString());
        File csvFile = new File(System.getProperty("java.io.tmpdir"), fileName);
        //System.out.println(emlFile.getAbsolutePath());
        try {
            try {
                is = mongoWrapper.get(uuid);
                if (is == null) {
                    throw new MongoException("File non trovato!!");
                }
            } catch (Exception e) {
                throw new MongoException("qualcosa è andato storto in downloadFileFromUUIDAndidAzienda");
            }
            StreamUtils.copy(is, response.getOutputStream());
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(BaborgUtils.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            IOUtils.closeQuietly(is);
        }
        
    }

}
