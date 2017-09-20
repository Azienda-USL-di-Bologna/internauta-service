package it.bologna.ausl.baborg.messagedelivery.impl;


import it.bologna.ausl.baborg.messagedelivery.MessageDeliveryServiceType;
import it.bologna.ausl.baborg.messagedelivery.NextMail;
import it.bologna.ausl.baborg.messagedelivery.NextMessage;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;


import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * Il servizio per l'invio delle mail
 * permette di inviare oggetti che dichiarano l'interfaccia {@link NextMail}
 * deve essere esteso per indicare il {@link JavaMailSender} da utilizzare
 * <p>
 * Created by f.longhitano on 21/08/2017.
 */

public abstract class NextMailServiceBase extends NextMessageDeliveryServiceBase {

    private static final Logger logger = Logger.getLogger(NextMailServiceBase.class);


    /**
     * @return l'oggetto {@link JavaMailSender} che si occuperà di inviare materialmente l'email, note: l'oggetto viene dichiarato sull'xml
     */
    public abstract JavaMailSender getMailSender();

    @Override
    public MessageDeliveryServiceType getMessageDeliveryServiceType(){
        return MessageDeliveryServiceType.EMAIL;
    }

    /**
     * Metodo che permette di inviare mail in asincrono su altro thread
     *
     * @param nextMessage l'oggetto con interfaccia {@link NextMail} da inviare
     */
    @Override
    public void sendMessage(NextMessage nextMessage) {
        try {

            mailThreadPool.submit(new MailSenderServiceRunnable(nextMessage));

        } catch (Exception e) {
            logger.error("error during send mail ", e);
        }
    }

    /**
     * Metodo che permette di inviare mail in modo sincrono
     *
     * @param nextMessage l'oggetto con interfaccia {@link NextMail} da inviare
     * @return se l'email è stata inviata o no
     */
    @Override
    public boolean sendMessageSync(NextMessage nextMessage) {
        try {
            return processMessage(nextMessage);

        } catch (Exception e) {
            logger.error("error during send mail ", e);
            return false;
        }
    }

    protected void fillMessage(NextMail nextMail, MimeMessage message) throws MessagingException {
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(nextMail.getFrom(getMessageDeliveryServiceType()));
        helper.setTo(nextMail.getTo(getMessageDeliveryServiceType()).toArray(new String[0]));
        if (CollectionUtils.isNotEmpty(nextMail.getCc()))
            helper.setCc(nextMail.getCc().toArray(new String[0]));
        if (CollectionUtils.isNotEmpty(nextMail.getBcc()))
            helper.setBcc(nextMail.getCc().toArray(new String[0]));
        helper.setSubject(nextMail.getSubject());
        helper.setText(nextMail.getText(getMessageDeliveryServiceType()), nextMail.isHtml());

        //priorità
        if (nextMail.getPriority() != null){
            //se la priorità è fuori scala mando eccezione
            if(nextMail.getPriority()<NextMail.PRIORITY_HIGHEST || nextMail.getPriority()>NextMail.PRIORITY_LOWEST)
                throw new IllegalStateException("Priority "+nextMail.getPriority()+" is out of range");

            helper.setPriority(nextMail.getPriority());

        }

        //allegati
        if (CollectionUtils.isNotEmpty(nextMail.getAttachments()))
            nextMail.getAttachments().stream().forEach(attachment -> {
                try {
                    byte[] value = IOUtils.toByteArray(attachment.getAttachmentInputStream());
                    helper.addAttachment(attachment.getAttachmentName(), new ByteArrayResource(value));
                } catch (Exception e) {
                    logger.error("Error during add attachment", e);
                }
            });

        //conferma ricezione
        if(StringUtils.isNotBlank(nextMail.getConfirmReadingDeliveryAddress()))
            message.setHeader("Disposition-Notification-To","<"+nextMail.getConfirmReadingDeliveryAddress()+">");
    }

    /**
     *
     * @param nextMessage
     * @return
     * @throws MessagingException
     */
    @Override
    protected boolean processMessage(NextMessage nextMessage) throws MessagingException {
        NextMail nextMail=null;
        if(NextMail.class.isAssignableFrom(nextMessage.getClass()))
            nextMail= (NextMail)nextMessage;
        else
            throw new ClassCastException("NextMessage MUST implements NextMail interface for sending throw mail");
        MimeMessage message = getMailSender().createMimeMessage();
        fillMessage(nextMail, message);
        preSendMail(nextMail);
        boolean sended = false;
        try {
            getMailSender().send(message);
            sended = true;
        } catch (MailException e) {
            logger.error("Error during sending email",e);
        }
        postSendMail(nextMail, sended);

        return sended;
    }




}
