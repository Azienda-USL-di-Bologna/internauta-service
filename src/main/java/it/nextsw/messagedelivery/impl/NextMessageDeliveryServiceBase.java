package it.nextsw.messagedelivery.impl;



import it.nextsw.messagedelivery.NextMessageDeliveryService;
import it.nextsw.messagedelivery.NextMessage;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;


import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.mail.MessagingException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * classe che fornisce l'impalcatura di base per i servizi che vogliono implementare {@link NextMessageDeliveryService}
 * Fornisce servizi per l'invio Asincrono su thread separato
 * Created by f.longhitano on 21/08/2017.
 */

public abstract class NextMessageDeliveryServiceBase implements NextMessageDeliveryService {

    private static final Logger logger = Logger.getLogger(NextMessageDeliveryServiceBase.class);
    public static final String DEFAULT_THREAD_POOL_SHUTDOWN_ERROR_MESSAGE = "Il thread pool non si è fermato entro il timeout";


    /**
     * Le dimensioni del threadpool per l'invio asincrono delle mail, DEFAULT 1
     */
    @Value("${email.thread.pool.size: 1}")
    protected Integer threadPoolSize;




    protected ExecutorService mailThreadPool;

    @PostConstruct
    public void init() {
        //costruisce il threadpool email
        mailThreadPool = Executors.newFixedThreadPool(getThreadPoolSize(),
                new BasicThreadFactory.Builder().namingPattern("message-delivery-thread-pool-" + getClass().getSimpleName()).build());

    }
    @PreDestroy
    protected void destroy() {
        //distrugge il threadpool email
        mailThreadPool.shutdownNow();
        try {
            mailThreadPool.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error(DEFAULT_THREAD_POOL_SHUTDOWN_ERROR_MESSAGE, e);
        }
    }

    /**
     * Metodo chiamato prima di inviare la mail, fare override se necessario
     *
     * @param nextMessage
     */
    @Override
    public void preSendMail(NextMessage nextMessage) {
        logger.debug("preSendMail " + nextMessage);
    }

    /**
     * Metodo chiamato dopo aver inviato la mail, fare override se necessario
     *
     * @param nextMessage
     * @param sended   se l'email è stata inviata o no
     */
    @Override
    public void postSendMail(NextMessage nextMessage, boolean sended) {
        logger.debug("postSendMail " + nextMessage + ", sended: " + sended);
    }

    @Override
    public int getThreadPoolSize(){
        return threadPoolSize;
    }



    /**
     * Il metodo che si occupa materialmente di inviare il messaggio
     *
     * @param nextMessage
     * @return se l'email è stata inviata o no
     *
     * @throws MessagingException
     */
    protected abstract boolean processMessage(NextMessage nextMessage) throws MessagingException;


    protected class MailSenderServiceRunnable implements Runnable {
        private NextMessage nextMessage;

        public MailSenderServiceRunnable(NextMessage nextMessage) {
            this.nextMessage = nextMessage;
        }

        @Override
        public void run() {
            try {
                if (nextMessage != null) {
//                    logger.info("Mando messaggio email " + mimeMessage);
//                    logger.info("host email " + mailSender.getHost());
//                    logger.info("host getPort " + mailSender.getPort());
//                    logger.info("host getProtocol " + mailSender.getProtocol());
//                    logger.info("host getJavaMailProperties " + mailSender.getJavaMailProperties().toString());
//
//                    logger.info("user getUsername " + mailSender.getUsername());
//                    logger.info("host getPassword " + mailSender.getPassword());

                    processMessage(nextMessage);

                    logger.info("message sent" + nextMessage);

                }

            } catch (Exception e) {
                logger.error("Error sending message", e);
            }
        }
    }
}
