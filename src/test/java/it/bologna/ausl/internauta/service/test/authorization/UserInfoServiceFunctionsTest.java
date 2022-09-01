/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.test.authorization;

import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.repositories.baborg.PersonaRepository;
import it.bologna.ausl.model.entities.baborg.Persona;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 *
 * @author Salo
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class UserInfoServiceFunctionsTest {

    @Autowired
    UserInfoService userInfoService;

    @Autowired
    PersonaRepository personaRepository;

    int idPersona = 225089; // Vincenzo Gasbarro (Resp & Segr)

    private int getCollectionSize(List lista) {
        int size = 0;
        if (lista != null) {
            size = lista.size();
        }
        return size;
    }

    private Persona getStaticPersonaForTest() {
        Persona p = null;
        try {
            p = personaRepository.findById(idPersona).get();
        } catch (Throwable t) {
            System.out.println("PERSONA NON TROVATA");
        }
        return p;
    }

    //@Test
    public void verificaSeCaricaResponsabilita() throws BlackBoxPermissionException {
        Persona persona = getStaticPersonaForTest();
        List< Integer> idStruttureDiResponsabilitaOfPersona
                = userInfoService.getIdStruttureDiResponsabilitaOfPersona(persona);
        System.out.println("idStruttureDiResponsabilitaOfPersona SIZE "
                + getCollectionSize(idStruttureDiResponsabilitaOfPersona));
    }

    //@Test
    public void verificaSeCaricaPersonaDiStruttureConResponsabilita() throws BlackBoxPermissionException {
        Persona persona = getStaticPersonaForTest();
        List<Persona> personeDiStruttureDiCuiPersonaIsResponsabile
                = userInfoService.getPersoneDiStruttureDiCuiPersonaIsResponsabile(persona);
        System.out.println("Responsabilita => Persone trovate "
                + getCollectionSize(personeDiStruttureDiCuiPersonaIsResponsabile));
    }

    //@Test
    public void verificaSeCaricaPersonaDiStruttureDiCuiSegretario() throws BlackBoxPermissionException {
        Persona persona = getStaticPersonaForTest();
        List<Persona> personeDiStruttureDiCuiPersonaIsResponsabile
                = userInfoService.getPersoneDiStruttureDiCuiPersonaIsSegretario(persona);
        System.out.println("Segretario => Persone trovate "
                + getCollectionSize(personeDiStruttureDiCuiPersonaIsResponsabile));
    }

    //@Test
    public void verificaSeCaricaPersonaDiStruttureDiCuiResponsabileOrSegretario() throws BlackBoxPermissionException {
        Persona persona = getStaticPersonaForTest();
        List<Persona> personeDiStruttureDiCuiPersonaIsResponsabile
                = userInfoService.getPersoneDiStruttureDiCuiPersonaIsResponsabileOrSegretario(persona);
        System.out.println("Responsabile Or Segretario => Persone trovate "
                + getCollectionSize(personeDiStruttureDiCuiPersonaIsResponsabile));
    }

    @Test
    public void xxxTestaTutto() throws Throwable {
        idPersona = 225089; // Vincenzo Gasbarro (Resp & Segr)
        verificaSeCaricaResponsabilita();
        verificaSeCaricaPersonaDiStruttureConResponsabilita();
        verificaSeCaricaPersonaDiStruttureDiCuiSegretario();
        verificaSeCaricaPersonaDiStruttureDiCuiResponsabileOrSegretario();

        idPersona = 276624;     // utente spento
        verificaSeCaricaResponsabilita();
        verificaSeCaricaPersonaDiStruttureConResponsabilita();
        verificaSeCaricaPersonaDiStruttureDiCuiSegretario();
        verificaSeCaricaPersonaDiStruttureDiCuiResponsabileOrSegretario();

//        idPersona = 0;  // INESISTENTE
//        verificaSeCaricaResponsabilita();
//        verificaSeCaricaPersonaDiStruttureConResponsabilita();
//        verificaSeCaricaPersonaDiStruttureDiCuiSegretario();
//        verificaSeCaricaPersonaDiStruttureDiCuiResponsabileOrSegretario();
    }

}
