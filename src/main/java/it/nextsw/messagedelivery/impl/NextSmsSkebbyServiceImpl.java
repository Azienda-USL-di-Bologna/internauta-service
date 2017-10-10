package it.nextsw.messagedelivery.impl;

import it.nextsw.messagedelivery.bean.SmsRelayConfiguration;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;


@Service
public class NextSmsSkebbyServiceImpl extends NextSmsSkebbyServiceBase {


    @Override
    public SmsRelayConfiguration getSmsRelayConfiguration() {
        SmsRelayConfiguration smsRelayConfiguration=new SmsRelayConfiguration();
        smsRelayConfiguration.setGateway("http://gateway.skebby.it/api/send/smseasy/advanced/http.php");
        smsRelayConfiguration.setUsername("nextsw");
        smsRelayConfiguration.setPassword("Next2014SMS");
        smsRelayConfiguration.setHttpMethod(HttpMethod.POST);
        return smsRelayConfiguration;
    }
}
