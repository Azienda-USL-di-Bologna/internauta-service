package it.bologna.ausl.internauta.service.controllers.shpeck;

import it.bologna.ausl.eml.handler.EmlHandler;
import it.bologna.ausl.eml.handler.EmlHandlerException;
import it.bologna.ausl.internauta.service.shpeck.utils.ShpeckCacheableFunctions;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 *
 * @author gusgus
 */
@RestController
@RequestMapping(value = "${shpeck.mapping.url.root}")
public class ShpeckCustomController {

    private static final Logger LOG = LoggerFactory.getLogger(ShpeckCustomController.class);
    
    /**
     * 
     * @param idMessage
     * @return
     * @throws EmlHandlerException 
     */
    @RequestMapping(value = "extractMessageData/{idMessage}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> extractMessageData(
            @PathVariable(required = true) Integer idMessage
        ) throws EmlHandlerException {
        // TODO: Gestire idMessage.
        LOG.info("extractMessageData", idMessage);
        return new ResponseEntity(ShpeckCacheableFunctions.getInfoEml(idMessage), HttpStatus.OK);
    }
    
    @RequestMapping(value = "getEmlAttachment/{idMessage}/{idAllegato}", method = RequestMethod.GET)
    public void getEmlAttachment(
            @PathVariable(required = true) Integer idMessage,
            @PathVariable(required = true) Integer idAllegato,
            HttpServletResponse response
        ) throws EmlHandlerException, FileNotFoundException, MalformedURLException, IOException, MessagingException {
        // TODO: Gestire idMessage e idAllegato.
        InputStream attachment = EmlHandler.getAttachment("C:\\Users\\Public\\prova8.eml", idAllegato);
        IOUtils.copy(attachment, response.getOutputStream());
        response.flushBuffer();
    }
    
    @RequestMapping(value = "get_all_eml_attachment/{idMessage}", method = RequestMethod.GET,  produces = "application/zip")
    public void getAllEmlAttachment(
            @PathVariable(required = true) Integer idMessage,
            HttpServletResponse response
        ) throws EmlHandlerException, FileNotFoundException, MalformedURLException, IOException, MessagingException {
        // TODO: Gestire idMessage
        response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=allegati.zip");
        List<Pair> attachments = EmlHandler.getAttachments("C:\\Users\\Public\\prova8.eml");
        ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(response.getOutputStream()));
        Integer i;
        for(Pair p : attachments) {
            i = 0;
            Boolean in_error = true;
            while(in_error) {
                try {
                    String s = "";
                    if (i > 0) {
                        s = "_" + Integer.toString(i); 
                    }
                    zos.putNextEntry(new ZipEntry((String) p.getLeft() + s));
                    in_error = false;
                } catch(ZipException ex) {
                    i++;
                }
            }
            IOUtils.copy((InputStream) p.getRight(), zos);
        }
        
        zos.close();
        response.flushBuffer();
    }
    
    
//    @RequestMapping(value = "extractMessageData", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseEntity<String> extractMessageData(@RequestBody final Message message) throws EmlHandlerException {
//        return new ResponseEntity(ShpeckCacheableFunctions.getInfoEml(message), HttpStatus.OK);
//    }
    
    
//        Path filePath = Paths.get("C:\\Users\\Public\\autocertificazione _1__19.pdf").normalize();
//        Resource resource = new UrlResource(filePath.toUri());
//        return ResponseEntity.ok()
//                .contentType(MediaType.parseMediaType("application/pdf"))
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + "autocertificazione _1__19.pdf" + "\"")
//                .body(resource);
    
    
        //response.addHeader(HttpHeaders.CONTENT_TYPE, );
        // per farlo scarica
        // response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=allegati.zip");
}