package it.bologna.ausl.internauta.service.interceptors;

import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.utils.HttpSessionData;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.service.utils.MemoryAnalizerService;
import it.bologna.ausl.internauta.service.krint.KrintUtils;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
public class RequestInterceptor implements AsyncHandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestInterceptor.class);

    private final MemoryAnalizerService memoryAnalizerService;
    private final KrintUtils krintUtils;
    private final HttpSessionData httpSessionData;
//    private final KrintRepository krintRepository;
//    private final ReportRepository reportRepository;
    private final ReporitoryConnectionManager reporitoryConnectionManager;

    public RequestInterceptor(HttpSessionData httpSessionData, KrintUtils krintUtils, MemoryAnalizerService memoryAnalizerService, ReporitoryConnectionManager reporitoryConnectionManager) {
        this.memoryAnalizerService = memoryAnalizerService;
        this.krintUtils = krintUtils;
        this.httpSessionData = httpSessionData;
//        this.krintRepository = krintRepository;
//        this.reportRepository = reportRepository;
        this.reporitoryConnectionManager = reporitoryConnectionManager;
    }

    @Override
    public void afterCompletion(HttpServletRequest hsr, HttpServletResponse hsr1, Object o, Exception excptn) throws Exception {
        memoryAnalizerService.handleDecrementMessage();

        Integer messageNumber = memoryAnalizerService.getMessageNumberCounter();
        Integer messageSize = memoryAnalizerService.getMessageSizeCounter();
//        System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaaa1 " + messageNumber);
//        System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaaa2 " + messageSize);
//        System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaaa2 " + messageInfo.get(MessageMemoryInfoMapKeys.SIZE));

        // Se è arrivato una eccezione non ho fatto niente e quindi non voglio loggare niente
        if (excptn == null) {

            krintUtils.saveAllKrintsInSessionData();

        } else {
            LOGGER.error("Rilevata eccezione nel afterCompletion del RequestInterceptor");
            LOGGER.error(excptn.toString());
        }

        // TODO: capire se il reset map va qui o dentro l'if
        deleteMinIODettagliAllegatiScripta();

        httpSessionData.resetDataMap();

//        super.afterCompletion(hsr, hsr1, o, excptn);
    }

    private void deleteMinIODettagliAllegatiScripta() {
        try {
            Set<String> data = (Set) this.httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.DettagliAllegatiDaEliminare);
            if (data != null && !data.isEmpty()) {
                MinIOWrapper minIOWrapper = reporitoryConnectionManager.getMinIOWrapper();

                for (String da : data) {
                    minIOWrapper.deleteByFileId(da);
                }
            }
        } catch (Throwable ex) {
            LOGGER.error("errore durante l'eliminazione dell'allegato su minIO", ex);
        }
    }
}
