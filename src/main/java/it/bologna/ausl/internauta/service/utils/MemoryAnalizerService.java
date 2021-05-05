package it.bologna.ausl.internauta.service.utils;

import it.bologna.ausl.internauta.service.interceptors.RequestInterceptor;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 *
 * @author gdm
 */
@Service
public class MemoryAnalizerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryAnalizerService.class);
    private static final Logger ANALYTIC_LOGGER = LoggerFactory.getLogger("analytics");
    private static final String ANALYTIC_LOG_PREFIX = "shpeck-message-analytics";

    @Value("${analytics.memory-debbuger.shpeck.log-on-message-number:100}")
    private Integer logOnMessageNumber;

    @Value("${analytics.memory-debbuger.shpeck.log-on-message-total-bytes-size:214748364800}")
    private Integer logOnMessageSize;
    @Autowired
    private HttpSessionData httpSessionData;

    private final AtomicInteger openedMessageNumberCounter = new AtomicInteger();
    private final AtomicInteger openedMessageSizeCounter = new AtomicInteger();

    public static enum MessageMemoryInfoMapKeys {
        COUNTER, SIZE
    }

    public int handleIncrementMessage(int size) {
        try {
            Integer openedMessageNumber = this.openedMessageNumberCounter.incrementAndGet();
            Integer openedMessageSize = this.openedMessageSizeCounter.accumulateAndGet(size, (x, y) -> (x + y));
            httpSessionData.putData(InternautaConstants.HttpSessionData.Keys.MEMORY_DEBUGGER_MESSAGE_SIZE, size);
            if (openedMessageNumber >= logOnMessageNumber || openedMessageSize >= logOnMessageSize) {
                this.writeAnalyticsLog();
            }
            return openedMessageSize;
        } catch (Throwable t) {
            LOGGER.error("errore nel handleIncrementMessage del memory analizer", t);
            return -1;
        }
    }

    public synchronized void writeAnalyticsLog() {
        Integer openedMessageNumber = this.getMessageNumberCounter();
        Integer openedMessageSize = this.getMessageSizeCounter();
        if (openedMessageNumber >= logOnMessageNumber || openedMessageSize >= logOnMessageSize) {
            ANALYTIC_LOGGER.info(String.format("%s opened message: %d", ANALYTIC_LOG_PREFIX, openedMessageNumber));
            ANALYTIC_LOGGER.info(String.format("%s opened message size: %d bytes - ~%f MB", ANALYTIC_LOG_PREFIX, openedMessageSize, (openedMessageSize / 1000000f)));
        }
    }

    public Integer getMessageNumberCounter() {
        return this.openedMessageNumberCounter.get();
    }

    public Integer getMessageSizeCounter() {
        return this.openedMessageSizeCounter.get();
    }

    public void handleDecrementMessage() {
        try {
            Object messageSize = httpSessionData.getData(InternautaConstants.HttpSessionData.Keys.MEMORY_DEBUGGER_MESSAGE_SIZE);
            boolean messageSizeWritten = messageSize != null;
            if (messageSizeWritten) {
                this.openedMessageNumberCounter.decrementAndGet();
                Integer res = this.openedMessageSizeCounter.accumulateAndGet((Integer) messageSize, (x, y) -> (x - y));
            }
        } catch (Throwable t) {
            LOGGER.error("errore nel handleDecrementMessage memory analizer", t);
        }
    }
}
