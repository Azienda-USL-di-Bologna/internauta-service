/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.controllers.tools;

import com.mongodb.util.JSON;
import it.bologna.ausl.internauta.service.configuration.utils.MongoConnectionManager;
import it.bologna.ausl.internauta.service.repositories.baborg.AziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PecAziendaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.PecRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.AddressRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageAddressRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageRepository;
import it.bologna.ausl.internauta.service.shpeck.utils.ShpeckUtils;
import static it.bologna.ausl.internauta.service.utils.InternautaConstants.AdditionalData.Keys.idAzienda;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.PecAzienda;
import it.bologna.ausl.model.entities.shpeck.Address;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.mongowrapper.MongoWrapper;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.client.HttpResponseException;
import org.graalvm.compiler.code.DataSection;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author mdonza
 */
@RestController
@RequestMapping(value = "${tools.mapping.url.root}")
public class MailController {

    @Autowired
    PecRepository pecRepository;

    @Autowired
    AddressRepository addressRepository;

    @Autowired
    MessageAddressRepository messageAddressRepository;

    @Autowired
    MessageRepository messageRepository;

    @Autowired
    AziendaRepository aziendaRepository;

    @Autowired
    PecAziendaRepository pecAziendaRepository;

    @Autowired
    ShpeckUtils shpeckUtils;

    @Autowired
    MongoConnectionManager mongoConnectionManager;

    @RequestMapping(value = "getPesoMail", method = RequestMethod.GET)
    public String getPesoMail(
            @RequestParam(required = true) String mail,
            @RequestParam(required = true) String codiceAzienda,
            HttpServletResponse response,
            HttpServletRequest request) throws HttpResponseException {

        //prima cosa mi becco id azienda da codice azienda
        Azienda aziendaTrovata = aziendaRepository.findByCodice(codiceAzienda);
        Address mailAddress = addressRepository.findByMailAddress(mail);
        if (aziendaTrovata == null || mailAddress == null) {
            throw new HttpResponseException(400, "azienda o mail errate o non trovate");
        }
        List<Integer> idPecsAzienda = pecAziendaRepository.getIdPecByIdAzienda(aziendaTrovata.getId());
//        {"Azienda": 
//            {"vfacchino@asul.pr.it":{
//                "mails":{
//                    ["eml1":{ 
//                        "data": data,
//                        "oggetto": oggetto,
//                        "size":size
//                        },
//                    "eml2":{ 
//                        "data": data,
//                        "oggetto": oggetto,
//                        "size":size
//                        }
//                    ]
//                },
//                {"totalesize":totalesize
//                
//                }
//            },...
//          "TotaleAzienda":sizetotaleAzienda
//        }

        JSONObject aziendaJson = new JSONObject();
        JSONObject jsonObjectCasella = new JSONObject();
        for (Integer idPecAzienda : idPecsAzienda) {
            Pec pecAzienda = pecRepository.findById(idPecAzienda).get();
            Address addressPecAzienda = addressRepository.findByMailAddress(pecAzienda.getIndirizzo());
            if (addressPecAzienda == null) {continue;}
            List<Integer> idMessagesFromXtoAziendaPecY = messageAddressRepository.getIdMessagesByAddressFromAndAddressTO(mailAddress.getId(), addressPecAzienda.getId());
            JSONObject jsonObjectDettagliCasella = new JSONObject();
            long sommaCasellaMails = 0;
            JSONArray jsonObjectMails = new JSONArray();
            for (Integer idMessageFromXtoAziendaPecY : idMessagesFromXtoAziendaPecY) {
                JSONObject jsonObjectMail = new JSONObject();

                Message message = messageRepository.findById(idMessageFromXtoAziendaPecY).get();
                String uuid = message.getUuidRepository();
                message.getPathRepository();
                String name = message.getName();
                Azienda idAziendaRepository = message.getIdAziendaRepository();
                MongoWrapper mongoWrapper = null;
                try {
                    if (idAziendaRepository == null) {
                        mongoWrapper = shpeckUtils.getMongoWrapperFromUuid(uuid);

                    } else {
                        mongoWrapper = mongoConnectionManager.getConnection(idAziendaRepository.getId());

                    }
                    if (mongoWrapper == null) {
                        System.out.println("messaggio non trovato");
                        continue;
                    }

                } catch (Exception ex) {
                    System.out.println("connessione rotta");
                    break;
                }
                Long sizeByUuid = mongoWrapper.getSizeByUuid(uuid);
                LocalDateTime receiveTime = message.getReceiveTime();
                JSONObject jsonObjectMailDettagli = new JSONObject();

                jsonObjectMailDettagli.put("data", receiveTime);
                jsonObjectMailDettagli.put("oggetto", message.getSubject());
                jsonObjectMailDettagli.put("size", sizeByUuid.toString());

                jsonObjectMail.put(message.getId().toString(), jsonObjectMailDettagli);
                jsonObjectMails.put(jsonObjectMail);
                sommaCasellaMails += sizeByUuid;
            }
            jsonObjectDettagliCasella.put("mails", jsonObjectMails);
            jsonObjectDettagliCasella.put("sommaCasellaMails", sommaCasellaMails);

            jsonObjectCasella.put(pecAzienda.getIndirizzo(), jsonObjectDettagliCasella);
        }
        aziendaJson.put("Azienda", jsonObjectCasella);
        return aziendaJson.toString();
    }
}
