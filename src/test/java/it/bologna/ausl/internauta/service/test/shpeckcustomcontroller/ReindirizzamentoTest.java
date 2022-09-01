/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.test.shpeckcustomcontroller;

import it.bologna.ausl.internauta.service.controllers.shpeck.ShpeckCustomController;
import javax.servlet.http.HttpServletRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import static org.springframework.test.web.servlet.MockMvcExtensionsKt.request;

/**
 *
 * @author Salo
 */
//@RunWith(SpringRunner.class)
//@SpringBootTest
public class ReindirizzamentoTest {

    @Autowired
    ShpeckCustomController controller;

//    @Test
//    public void testaReindirizzamento() {
//
//        try {
//            controller.readdressMessage(189612, 1502, null);
//        } catch (Throwable t) {
//            System.out.println(t.getMessage());
//            t.printStackTrace();
//        }
//    }
}
