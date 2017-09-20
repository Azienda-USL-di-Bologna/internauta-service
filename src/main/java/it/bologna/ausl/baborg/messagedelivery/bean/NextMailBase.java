package it.bologna.ausl.baborg.messagedelivery.bean;

import it.bologna.ausl.baborg.messagedelivery.MessageDeliveryServiceType;
import it.bologna.ausl.baborg.messagedelivery.NextMail;
import it.bologna.ausl.baborg.messagedelivery.NextMailAttachment;

import java.util.LinkedList;
import java.util.List;

public class NextMailBase implements NextMail {

    private String from;

    /*

     */
    private List<String> to;
    private List<String> cc;
    private List<String> bcc;
    private String subject;
    private String text;
    private boolean html;

    private String confirmReadingDeliveryAddress;

    private List<NextMailAttachment> attachments;

    private Integer priority;

    public NextMailBase() {
        to=new LinkedList<>();
        cc=new LinkedList<>();
        bcc=new LinkedList<>();
        attachments=new LinkedList<>();
    }

    @Override
    public String getFrom(MessageDeliveryServiceType messageDeliveryServiceType) {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    @Override
    public List<String> getTo(MessageDeliveryServiceType messageDeliveryServiceType) {
        return to;
    }

    public void setTo(List<String> to) {
        this.to = to;
    }

    @Override
    public List<String> getCc() {
        return cc;
    }

    public void setCc(List<String> cc) {
        this.cc = cc;
    }

    @Override
    public List<String> getBcc() {
        return bcc;
    }

    public void setBcc(List<String> bcc) {
        this.bcc = bcc;
    }

    @Override
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    @Override
    public String getText(MessageDeliveryServiceType messageDeliveryServiceType) {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public boolean isHtml() {
        return html;
    }

    public void setHtml(boolean html) {
        this.html = html;
    }

    @Override
    public Integer getPriority() {
        return priority;
    }

    @Override
    public List<NextMailAttachment> getAttachments() {
        return attachments;
    }


    public void setAttachments(List<NextMailAttachment> attachments) {
        this.attachments = attachments;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    @Override
    public String getConfirmReadingDeliveryAddress() {
        return confirmReadingDeliveryAddress;
    }

    public void setConfirmReadingDeliveryAddress(String confirmReadingDeliveryAddress) {
        this.confirmReadingDeliveryAddress = confirmReadingDeliveryAddress;
    }
}
