package it.bologna.ausl.baborg.messagedelivery;

import java.util.List;

/**
 * L'interfaccia che devono implementare gli oggetti che vogliono essere spediti via mail
 * Created by f.longhitano on 21/08/2017.
 */
public interface NextMail extends NextMessage {

    public static final int PRIORITY_HIGHEST=1;
    public static final int PRIORITY_HIGH=2;
    public static final int PRIORITY_NORMAL=3;
    public static final int PRIORITY_LOW=4;
    public static final int PRIORITY_LOWEST=5;


    /**
     *
     * @return il subject della mail
     */
    public String getSubject();
    /**
     *
     * @return una lista di stringhe contenete gli indirizzi da mettere in cc
     */
    public List<String> getCc();

    /**
     *
     * @return una lista di stringhe contenete gli indirizzi da mettere in bcc
     */
    public List<String> getBcc();


    /**
     *
     * @return true se il testo deve essere interpretato come html, false se come txt
     */
    public boolean isHtml();

    /**
     *
     * @return un intero da 1 a 5 per indicare la priorità della mail, vedi costanti su {@link NextMail}
     */
    public Integer getPriority();

    /**
     *
     * @return la lista degli Attachment che devono implementare l'interfaccia {@link NextMailAttachment}
     */
    public List<NextMailAttachment> getAttachments();


    /**
     *
     * @return ritorna l'indirizzo in cui inviare la conferma di lettura della mail
     */
    public String getConfirmReadingDeliveryAddress();



}
