package it.bologna.ausl.internauta.service.interceptors;

import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.internauta.service.krint.KrintError;
import it.bologna.ausl.internauta.service.repositories.diagnostica.ReportRepository;
import it.bologna.ausl.internauta.service.repositories.logs.KrintRepository;
import it.bologna.ausl.internauta.service.utils.HttpSessionData;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.service.utils.MemoryAnalizerService;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.model.entities.diagnostica.Report;
import it.bologna.ausl.model.entities.logs.Krint;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
    private final HttpSessionData httpSessionData;
    private final KrintRepository krintRepository;
    private final ReportRepository reportRepository;
    private final ReporitoryConnectionManager reporitoryConnectionManager;

    public RequestInterceptor(HttpSessionData httpSessionData, KrintRepository krintRepository, ReportRepository reportRepository, MemoryAnalizerService memoryAnalizerService, ReporitoryConnectionManager reporitoryConnectionManager) {
        this.memoryAnalizerService = memoryAnalizerService;
        this.httpSessionData = httpSessionData;
        this.krintRepository = krintRepository;
        this.reportRepository = reportRepository;
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

            List<Krint> krintList = (List<Krint>) httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.KRINT_ROWS);

            if (krintList != null && krintList.size() > 0) {
                try {
                    krintList.forEach(k -> krintRepository.save(k));
                } catch (Exception e) {
                    Report report = new Report();
                    report.setTipologia("KRINT_ERROR");
                    Map<String, String> additionalData = new HashMap();
                    additionalData.put("message", "KRINT ERROR: errore nel salvataggio di una riga di krint");
                    additionalData.put("errorMessage", e.getMessage());
                    //String mapAsString = additionalData.keySet().stream().map(key -> "\"" + key + "\":\"" + additionalData.get(key) + "\"").collect(Collectors.joining(", ", "{", "}"));
                    report.setAdditionalData(additionalData);
                    reportRepository.save(report);
                    LOGGER.error("KRINT ERROR: errore nel salvataggio di una riga di krint", e);
                }
            }

            List<KrintError> krintErrorList = (List<KrintError>) httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.KRINT_ERRORS);

            if (krintErrorList != null && krintErrorList.size() > 0) {
                try {
                    krintErrorList.forEach(k -> {
                        String s = String.format("KRINT ERROR: idUtente: %s, idRealUser: %s, idOggetto: %s, functionName: %s, codiceOperazione: %s",
                                    k.getIdUtente() == null ? "null" : k.getIdUtente().toString(),
                                    k.getIdRealUser() == null ? "null" : k.getIdRealUser().toString(),
                                    k.getIdOggetto() == null ? "null" : k.getIdOggetto().toString(),
                                    k.getFunctionName() == null ? "null" : k.getFunctionName(),
                                    k.getCodiceOperazione() == null ? "null" : k.getCodiceOperazione().toString());
                        LOGGER.error(s);
                        Report report = new Report();
                        report.setTipologia("KRINT_ERROR_LOG");
                        Map<String, String> additionalData = new HashMap();
                        additionalData.put("message", "KRINT ERROR LOG: scrittura del log del krint error");
                        additionalData.put("errorMessage", s);
                        //String mapAsString = additionalData.keySet().stream().map(key -> "\"" + key + "\":\"" + additionalData.get(key) + "\"").collect(Collectors.joining(", ", "{", "}"));
                        report.setAdditionalData(additionalData);
                        reportRepository.save(report);
                    });
                } catch (Exception e) {
                    Report report = new Report();
                    report.setTipologia("KRINT_ERROR_LOG_ERROR");
                    Map<String, String> additionalData = new HashMap();
                    additionalData.put("message", "KRINT ERROR: errore nella scrittura del log del krint error");
                    additionalData.put("errorMessage", e.getMessage());
                    //String mapAsString = additionalData.keySet().stream().map(key -> "\"" + key + "\":\"" + additionalData.get(key) + "\"").collect(Collectors.joining(", ", "{", "}"));
                    report.setAdditionalData(additionalData);
                    reportRepository.save(report);
                    LOGGER.error("KRINT ERROR: errore nella scrittura del log del krint error");
                }
            }

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
