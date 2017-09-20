package it.bologna.ausl.baborg.messagedelivery;

import java.util.List;

/**
 * L'interfaccia che devono implementare gli oggetti che vogliono essere spediti via sms
 * Created by f.longhitano on 22/08/2017.
 */
public interface NextMessage {


    /**
     * @param messageDeliveryServiceType il tipo di servizio per il quale si sta richiedendo l'informazione
     * @return il from della mail, ATTENZIONE questo valore potrebbe essere sovrascritto dal mail service
     */
    public String getFrom(MessageDeliveryServiceType messageDeliveryServiceType);

    /**
     * @param messageDeliveryServiceType il tipo di servizio per il quale si sta richiedendo l'informazione
     * @return una lista di stringhe contenete gli indirizzi dei destinatari
     */
    public List<String> getTo(MessageDeliveryServiceType messageDeliveryServiceType);


    /**
     *
     * @param messageDeliveryServiceType il tipo di servizio per il quale si sta richiedendo l'informazione
     * @return il testo della mail
     */
    public String getText(MessageDeliveryServiceType messageDeliveryServiceType);




}
