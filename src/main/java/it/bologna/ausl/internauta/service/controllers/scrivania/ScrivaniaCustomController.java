package it.bologna.ausl.internauta.service.controllers.scrivania;

import it.bologna.ausl.internauta.service.exceptions.ControllerHandledExceptions;
import it.bologna.ausl.internauta.service.exceptions.Http400ResponseException;
import it.bologna.ausl.internauta.service.exceptions.Http403ResponseException;
import it.bologna.ausl.internauta.service.exceptions.Http404ResponseException;
import it.bologna.ausl.internauta.service.exceptions.Http500ResponseException;
import it.bologna.ausl.internauta.service.exceptions.HttpInternautaResponseException;
import it.bologna.ausl.internauta.service.scrivania.anteprima.BabelDownloader;
import it.bologna.ausl.internauta.service.scrivania.anteprima.BabelDownloaderResponseBody;
import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author gdm
 */
@RestController
@RequestMapping(value = "${scrivania.mapping.url.root}")
public class ScrivaniaCustomController implements ControllerHandledExceptions {
    
    @Autowired
    private BabelDownloader babelDownloader;
    
    @RequestMapping(value = {"getAnteprima"}, method = RequestMethod.GET)
    public void attivita(
        @RequestParam(required = true) String guid,
        @RequestParam(required = true) String tipologia,
        @RequestParam(required = true) Integer idAzienda,
        @RequestParam(required = true) String idApplicazione,
        @RequestParam(required = true) String fileName,
        HttpServletRequest request,
        HttpServletResponse response) throws HttpInternautaResponseException, IOException {

        BabelDownloaderResponseBody downloadUrlRsponseBody = babelDownloader.getDownloadUrl(babelDownloader.createRquestBody(guid, tipologia), idAzienda, idApplicazione);
        switch (downloadUrlRsponseBody.getStatus()) {
            case OK:
                if (!StringUtils.hasText(downloadUrlRsponseBody.getUrl())) {
                    throw new Http500ResponseException("8", downloadUrlRsponseBody.getMessage());
                }
                try(Response downloadStream = babelDownloader.getDownloadStream(downloadUrlRsponseBody.getUrl())) {
                    try(OutputStream out = response.getOutputStream()) {
                        response.setHeader(guid, guid);
                        response.setHeader("Content-Type", "application/pdf");
                        response.setHeader("X-Frame-Options", "sameorigin");
                        response.setHeader("Content-Disposition", ";filename=" + fileName + ".pdf");
                        IOUtils.copy(downloadStream.body().byteStream(), out, 4096);
                    }
                }
                break;

            case BAD_REQUEST:
                throw new Http400ResponseException("1", downloadUrlRsponseBody.getMessage());
            case FORBIDDEN:
                throw new Http403ResponseException("2", downloadUrlRsponseBody.getMessage());
            case USER_NOT_FOUND:
                throw new Http404ResponseException("3", downloadUrlRsponseBody.getMessage());
            case FILE_NOT_FOUND:
                throw new Http404ResponseException("4", downloadUrlRsponseBody.getMessage());
            case GENERAL_ERROR:
                throw new Http500ResponseException("5", downloadUrlRsponseBody.getMessage());
            default:
                throw new Http500ResponseException("6", downloadUrlRsponseBody.getMessage());
        }
    }
    
    
}
