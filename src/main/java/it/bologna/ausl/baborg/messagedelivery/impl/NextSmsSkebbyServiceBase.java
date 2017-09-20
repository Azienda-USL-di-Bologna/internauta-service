package it.bologna.ausl.baborg.messagedelivery.impl;


import it.bologna.ausl.baborg.messagedelivery.MessageDeliveryServiceType;
import it.bologna.ausl.baborg.messagedelivery.NextMail;
import it.bologna.ausl.baborg.messagedelivery.NextMessage;
import it.bologna.ausl.baborg.messagedelivery.bean.SmsRelayConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParamBean;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.springframework.mail.javamail.JavaMailSender;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Il servizio per l'invio degli sms col servizio skebby
 * permette di inviare oggetti che dichiarano l'interfaccia {@link NextMessage}
 * deve essere esteso per indicare il {@link SmsRelayConfiguration} da utilizzare
 * <p>
 * Created by f.longhitano on 22/08/2017.
 */

public abstract class NextSmsSkebbyServiceBase extends NextMessageDeliveryServiceBase {

    private static final Logger logger = Logger.getLogger(NextSmsSkebbyServiceBase.class);


    /**
     * @return l'oggetto {@link SmsRelayConfiguration} che contiene le informazioni di configurazione del webserver sms
     */
    public abstract SmsRelayConfiguration getSmsRelayConfiguration();


    @Override
    public MessageDeliveryServiceType getMessageDeliveryServiceType(){
        return MessageDeliveryServiceType.SMS;
    }

    /**
     * Metodo che permette di inviare sms in asincrono su altro thread
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
     * Metodo che permette di inviare sms in modo sincrono
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

    protected SkebbyPostObject createMessage(NextMessage nextMessage) throws MessagingException {
        SkebbyPostObject skebbyPostObject=new SkebbyPostObject(getSmsRelayConfiguration());
        switch (getSmsRelayConfiguration().getSmsMethod()){
            case SmsRelayConfiguration.SMS_METHOD_CLASSIC:
                //from todo rimuovere il + ed eventuali zeri davanti al numero
                if(StringUtils.isNotBlank(nextMessage.getFrom(getMessageDeliveryServiceType()))){
                    if(StringUtils.isNumeric(nextMessage.getFrom(getMessageDeliveryServiceType())))
                        skebbyPostObject.setSender_number(nextMessage.getFrom(getMessageDeliveryServiceType()));
                    else
                        skebbyPostObject.setSender_string(nextMessage.getFrom(getMessageDeliveryServiceType()));
                }
            case SmsRelayConfiguration.SMS_METHOD_BASIC:
                //riempio il to
                String[] recipients=new String[nextMessage.getTo(getMessageDeliveryServiceType()).size()];
                for(int i=0; i<recipients.length;i++){
                    recipients[i]=nextMessage.getTo(getMessageDeliveryServiceType()).get(i);
                    //recipients[i][0]=nextMessage.getTo().get(i);
                }
                skebbyPostObject.setRecipients(recipients);
                //testo
                if(nextMessage.getText(getMessageDeliveryServiceType()).length()>getSmsRelayConfiguration().getMaxTextCharacter())
                    throw new MessagingException("il testo del messaggio è troppo lungo "+nextMessage.getText(getMessageDeliveryServiceType()).length()+", limite "+getSmsRelayConfiguration().getMaxTextCharacter());
                skebbyPostObject.setText(nextMessage.getText(getMessageDeliveryServiceType()));
                break;
            default:
                throw new IllegalStateException("Sms send method "+getSmsRelayConfiguration().getSmsMethod()+" non supportato");
        }
        return skebbyPostObject;
    }

    /**
     *
     * @param nextMessage
     * @return
     * @throws MessagingException
     */
    @Override
    protected boolean processMessage(NextMessage nextMessage) throws MessagingException {

        SkebbyPostObject skebbyPostObject=createMessage(nextMessage);
        preSendMail(nextMessage);
        boolean sended = false;
        try {
            String response=skebbyGatewaySendSMS(skebbyPostObject);
            logger.info("skebby repsonse: "+response);
            if(StringUtils.isNotBlank(response) && StringUtils.contains(response,"success"))
                sended = true;
        } catch (Exception e) {
            logger.error("Error during sending sms",e);
        }
        postSendMail(nextMessage, sended);

        return sended;
    }

    /**
     * Il metodo per l'invio dell'sms al gateway skebby, copiato e adattato dalla documentazione del loro sito
     * @param skebbyPostObject
     * @return
     * @throws IOException
     */
    protected String skebbyGatewaySendSMS(SkebbyPostObject skebbyPostObject) throws IOException{

        if (!getSmsRelayConfiguration().getCharset().equals("UTF-8") && !getSmsRelayConfiguration().getCharset().equals("ISO-8859-1")) {

            throw new IllegalArgumentException("Charset not supported.");
        }

        String endpoint = getSmsRelayConfiguration().getGateway();
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 10*1000);
        DefaultHttpClient httpclient = new DefaultHttpClient(params);
        HttpProtocolParamBean paramsBean = new HttpProtocolParamBean(params);
        paramsBean.setVersion(HttpVersion.HTTP_1_1);
        paramsBean.setContentCharset(getSmsRelayConfiguration().getCharset());
        paramsBean.setHttpElementCharset(getSmsRelayConfiguration().getCharset());

        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair("method", skebbyPostObject.getMethod()));
        formparams.add(new BasicNameValuePair("username", skebbyPostObject.getUsername()));
        formparams.add(new BasicNameValuePair("password", skebbyPostObject.getPassword()));
        if(StringUtils.isNotBlank(skebbyPostObject.getSender_number()))
            formparams.add(new BasicNameValuePair("sender_number", skebbyPostObject.getSender_number()));
        if(StringUtils.isNotBlank(skebbyPostObject.getSender_string()))
            formparams.add(new BasicNameValuePair("sender_string", skebbyPostObject.getSender_string()));

        for (String recipient : skebbyPostObject.getRecipients()) {
            formparams.add(new BasicNameValuePair("recipients[]", recipient));
        }
        formparams.add(new BasicNameValuePair("text", skebbyPostObject.getText()));
        //formparams.add(new BasicNameValuePair("charset", charset));


        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, getSmsRelayConfiguration().getCharset());
        HttpPost post = new HttpPost(endpoint);
        post.setEntity(entity);

        HttpResponse response = httpclient.execute(post);
        HttpEntity resultEntity = response.getEntity();
        if(null != resultEntity){
            return EntityUtils.toString(resultEntity);
        }
        return null;
    }








    /**
     * Classe di utilità per organizzare i dati da inviare a skebby
     */
    protected class SkebbyPostObject {

        private String username;
        private String password;
        private String method;
        private String[] recipients;
        private String text;

        //only send_sms_classic
        private String sender_number;
        private String sender_string;

        public SkebbyPostObject() {
        }
        public SkebbyPostObject(SmsRelayConfiguration smsRelayConfiguration) {
            this.username=smsRelayConfiguration.getUsername();
            this.password=smsRelayConfiguration.getPassword();
            this.method=smsRelayConfiguration.getSmsMethod();
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String[] getRecipients() {
            return recipients;
        }

        public void setRecipients(String[] recipients) {
            this.recipients = recipients;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getSender_number() {
            return sender_number;
        }

        public void setSender_number(String sender_number) {
            this.sender_number = sender_number;
        }

        public String getSender_string() {
            return sender_string;
        }

        public void setSender_string(String sender_string) {
            this.sender_string = sender_string;
        }
    }



}
