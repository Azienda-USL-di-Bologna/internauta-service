package it.nextsw.messagedelivery;

import java.io.IOException;
import java.io.InputStream;

/**
 * L'interfaccia che devono implementare gli oggetti che vogliono essere usati come Attachment mail
 *
 * Created by f.longhitano on 21/08/2017.
 */
public interface NextMailAttachment {

    /**
     *
     * @return l'input stream dell'allegato
     * @throws IOException
     */
    public InputStream getAttachmentInputStream() throws IOException;

    public String getAttachmentContentType();

    /**
     *
     * @return il nome dell'allegato
     */
    public String getAttachmentName();
}
