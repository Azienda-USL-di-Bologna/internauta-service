package it.bologna.ausl.internauta.service.controllers.shpeck;

import it.bologna.ausl.eml.handler.EmlHandlerResult;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.mail.Address;

/**
 *
 * @author gusgus
 * 
 * 
 * MI SA CHE STA CLASSE NON SERVA A UN CAZZO
 */
public class MessageExtractedData {
    private List<String> from;
    private List<String> to;
    private List<String> cc;
//    private List<String> replyTo;
    private Boolean isPec;
    private String oggetto;
    private String body;
//    private Date sendDate;
//    private Date receiveDate;

    public List<String> getFrom() {
        return from;
    }

    public void setFrom(List<String> from) {
        this.from = from;
    }

    public List<String> getTo() {
        return to;
    }

    public void setTo(List<String> to) {
        this.to = to;
    }

    public List<String> getCc() {
        return cc;
    }

    public void setCc(List<String> cc) {
        this.cc = cc;
    }

//    public List<String> getReplyTo() {
//        return replyTo;
//    }
//
//    public void setReplyTo(List<String> replyTo) {
//        this.replyTo = replyTo;
//    }

    public Boolean getIsPec() {
        return isPec;
    }

    public void setIsPec(Boolean isPec) {
        this.isPec = isPec;
    }

    public String getOggetto() {
        return oggetto;
    }

    public void setOggetto(String oggetto) {
        this.oggetto = oggetto;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
    
    public MessageExtractedData buildMessageExtractedDataByMailMessage(EmlHandlerResult m) {
        Address[] from, to, cc, reply_to;
        
        List<String> list = new ArrayList();
        
        list.add(m.getFrom());
        this.setFrom(list);
        // String[] to1 = m.getTo();
        list = new ArrayList();
        for (String a : m.getTo()) 
            list.add(a.toString());
        this.setTo(list);
        
//        list = new ArrayList();
//        cc = m.getCc();
//        if(cc != null){
//            for (Address a : cc)
//                list.add(a.toString());
//            this.setCc(list);
//        }
        
//        list = new ArrayList();
//        for (Address a : m.getReply_to()) 
//            list.add(a.toString());
//        this.setReplyTo(list);
        
//        this.setIsPec(m.getIsPec());
        this.setOggetto(m.getSubject());
//        this.setIsPec(m.getReceive_date());
//        this.setIsPec(m.getSend_date());

        this.body = m.getHtmlText();
        return this;
    }
}

//protected Address[] from, to, cc, reply_to;
//protected MimeMessage original;
//protected Boolean ispec = false;
//protected String subject, string_headers, id, raw_message, message = null;
//protected HashMap<String, String> headers;
//protected Date send_date, receive_date; 