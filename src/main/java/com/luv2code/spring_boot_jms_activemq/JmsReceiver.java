package com.luv2code.spring_boot_jms_activemq;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class JmsReceiver {

    @JmsListener(destination = "mailBox", containerFactory = "containerForJmsListener")
    public void receiveMessage(Email email) {
        System.out.println("Received<" + email + ">");
    }
}
