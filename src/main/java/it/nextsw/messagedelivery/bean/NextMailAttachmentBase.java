package it.nextsw.messagedelivery.bean;

import it.nextsw.messagedelivery.NextMailAttachment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class NextMailAttachmentBase implements NextMailAttachment {

    private String name;
    private String contentType;
    private InputStream inputStream;


    public NextMailAttachmentBase() {
    }

    public NextMailAttachmentBase(String name, String contentType, InputStream inputStream) {
        this.name = name;
        this.contentType = contentType;
        this.inputStream = inputStream;
    }

    @Override
    public InputStream getAttachmentInputStream() throws IOException {
        return inputStream;
    }

    public OutputStream getOutputStream() throws IOException {
        return null;
    }

    @Override
    public String getAttachmentContentType() {
        return contentType;
    }

    @Override
    public String getAttachmentName() {
        return name;
    }


    public void setAttachmentName(String name) {
        this.name = name;
    }

    public void setAttachmentContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setAttachmentInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }
}
