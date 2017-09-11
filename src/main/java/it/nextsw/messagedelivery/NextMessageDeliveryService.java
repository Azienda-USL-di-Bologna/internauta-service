package it.nextsw.messagedelivery;


/**
 * Interfaccia che deve essere implementata dai servizi che vogliono inviare messaggi
 * l'interfaccia {@link NextMessage} è sufficiente solo per l'invio di sms, per l'invio di email implementare l'interfaccia {@link NextMail}
 *
 */
public interface NextMessageDeliveryService {


    /**
     *
     * @return il numero di thread nel poll per questa configurazione, più è grande il thread più richieste possono partire contemporaneamente
     */
    public int getThreadPoolSize();
    /**
     *
     * @return il tipo di servizio fornito
     */
    public MessageDeliveryServiceType getMessageDeliveryServiceType();

    /**
     * Metodo che permette di inviare mail in modo sincrono
     *
     * @param nextMessage l'oggetto con interfaccia {@link NextMessage} da inviare
     * @return se l'email è stata inviata o no
     */
    public void sendMessage(NextMessage nextMessage);

    /**
     * Metodo che permette di inviare mail in asincrono su altro thread
     *
     * @param nextMessage l'oggetto con interfaccia {@link NextMail} da inviare
     */
    public boolean sendMessageSync(NextMessage nextMessage);

    /**
     * Metodo chiamato prima di inviare la mail
     *
     * @param nextMessage
     */
    public void preSendMail(NextMessage nextMessage);


    /**
     * Metodo chiamato dopo aver inviato la mail
     *
     * @param nextMessage
     * @param sended   se l'email è stata inviata o no
     */
    public void postSendMail(NextMessage nextMessage, boolean sended);
}
