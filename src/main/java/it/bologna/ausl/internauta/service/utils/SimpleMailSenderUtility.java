/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.mail.smtp.SMTPTransport;
import it.bologna.ausl.internauta.service.controllers.tools.ToolsCustomController;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Salo (Estratto parte di codice da ToolsCustomController)
 */
@Component
public class SimpleMailSenderUtility {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToolsCustomController.class);

    @Value("${customer.support.email}")
    private String emailCustomerSupport;
    @Value("${customer.support.name}")
    private String nameCustomerSupport;
    //per parametri pubblici?
    @Autowired
    private AziendaRepository aziendaRepository;
    @Autowired
    private CachedEntities cachedEntities;
    @Autowired
    private ObjectMapper objectMapper;

    public Boolean sendMail(
            Integer idAzienda, String fromName, String Subject, List<String> To, String body,
            List<String> cc, List<String> bcc, MultipartFile[] attachments, List<String> replyTo) throws IOException {

        Azienda azienda = cachedEntities.getAzienda(idAzienda);
        AziendaParametriJson aziendaParametri = AziendaParametriJson.parse(objectMapper, azienda.getParametri());
        AziendaParametriJson.MailParams mailParams = aziendaParametri.getMailParams();

        if (mailParams != null) {
            String smtpServer = mailParams.getMailServerSmtpUrl();
            Integer port = mailParams.getMailServerSmtpPort();

            String username = null;
            String password = null;

            Properties prop = System.getProperties();
            prop.put("mail.smtp.host", smtpServer);                                 //optional, defined in SMTPTransport
            prop.put("mail.smtp.timeout", 3000);
            prop.put("mail.smtp.connectiontimeout", 3000);
            if (StringUtils.isEmpty(username) && StringUtils.isEmpty(password)) {
                prop.put("mail.smtp.auth", "false");
            } else {
                prop.put("mail.smtp.auth", "true");
            }

            if (port != null && port != -1) {
                prop.put("mail.smtp.port", port.toString());                        // default port 25
            } else {
                prop.put("mail.smtp.port", "25");
            }

            Session session = Session.getInstance(prop, null);
            Message msg = new MimeMessage(session);

            try {
                String mailFrom = mailParams.getMailFrom();
                // from
                msg.setFrom(new InternetAddress(mailFrom, fromName));

                if (replyTo != null) {
                    String addressesReplyTo = "";
                    for (String toElement : replyTo) {
                        addressesReplyTo += toElement + ",";
                    }
                    addressesReplyTo = addressesReplyTo.substring(0, addressesReplyTo.length() - 1);
                    msg.setReplyTo(InternetAddress.parse(addressesReplyTo, false));
                }

                // inserisco lista TO
                if (To != null && !To.isEmpty()) {
                    String addressesTo = "";
                    for (String toElement : To) {
                        addressesTo += toElement + ",";
                    }
                    addressesTo = addressesTo.substring(0, addressesTo.length() - 1);
                    msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(addressesTo, false));
                }

                //inserico lista CC e BCC
                if (cc != null && !cc.isEmpty()) {
                    String addressesCC = "";
                    for (String ccElement : cc) {
                        addressesCC += ccElement + ",";
                    }
                    if (bcc != null && !bcc.isEmpty()) {
                        for (String bccElement : bcc) {
                            addressesCC += bccElement + ",";
                        }
                    }
                    addressesCC = addressesCC.substring(0, addressesCC.length() - 1);
                    msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(addressesCC, false));
                }

                //            if (bcc!=null && !bcc.isEmpty()){
                //                for (String bccElement : bcc) {
                //                    msg.setRecipients(Message.RecipientType.CC,
                //                        InternetAddress.parse(bccElement, false));
                //                    }
                //            }
                // subject
                //            LocalDateTime today = LocalDateTime.now();
                //            ZoneId id = ZoneId.of("Europe/Rome");
                //            ZonedDateTime zonedDateTime = ZonedDateTime.of(today, id);      //That's how you add timezone to date
                //            String formattedDateTime = DateTimeFormatter
                //                            .ofPattern("dd/MM/yyyy - HH:mm")
                //                            .format(zonedDateTime);             //  esempio 11/03/2020 - 10.44
                msg.setSubject(Subject);
                // content
                if (attachments != null && attachments.length > 0) {
                    Multipart multipart = new MimeMultipart();

                    // Body
                    MimeBodyPart messageBodyPart = new MimeBodyPart();
                    messageBodyPart.setText(body);
                    multipart.addBodyPart(messageBodyPart);

                    // Allegati
                    MimeBodyPart attachmentPart;
                    for (MultipartFile attachment : attachments) {
                        attachmentPart = new MimeBodyPart();
                        byte[] fileBytes = attachment.getBytes();
                        String attachmentName = attachment.getOriginalFilename();
                        ByteArrayDataSource source
                                = new ByteArrayDataSource(fileBytes, attachment.getContentType());
                        attachmentPart.setDataHandler(new DataHandler(source));
                        attachmentPart.setFileName(attachmentName);
                        multipart.addBodyPart(attachmentPart);
                    }
                    msg.setContent(multipart);
                } else {
                    msg.setText(body);
                }

                // msg.setContent(body, "text/html; charset=utf-8");
                msg.setSentDate(new Date());
                // Get SMTPTransport
                SMTPTransport t = (SMTPTransport) session.getTransport("smtp");
                // connect
                t.connect(smtpServer, username, password);
                // send
                t.sendMessage(msg, msg.getAllRecipients());
                LOGGER.info("Invio mail");
                System.out.println("Response: " + t.getLastServerResponse());
                t.close();
            } catch (MessagingException e) {
                e.printStackTrace();
                LOGGER.info("Invio mail fallito", e);
                return false;
            }
            return true;

        }
        return false;
    }

}
