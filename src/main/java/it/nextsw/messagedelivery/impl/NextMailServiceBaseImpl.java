package it.nextsw.messagedelivery.impl;


import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Created by f.longhitano on 21/08/2017.
 */

@Service
public class NextMailServiceBaseImpl extends NextMailServiceBase {

    private static final Logger logger = Logger.getLogger(NextMailServiceBaseImpl.class);

    @Autowired
    private JavaMailSender mailSender;


    @Override
    public JavaMailSender getMailSender() {
        return mailSender;
    }
}
