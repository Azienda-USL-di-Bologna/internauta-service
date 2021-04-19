/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.interceptors.scripta;

import edu.emory.mathcs.backport.java.util.Arrays;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.repositories.scripta.MezzoRepository;
import it.bologna.ausl.internauta.service.repositories.shpeck.MessageRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.Mezzo;
import it.bologna.ausl.model.entities.scripta.QMezzo;
import it.bologna.ausl.model.entities.scripta.Related;
import it.bologna.ausl.model.entities.scripta.Spedizione;
import it.bologna.ausl.model.entities.scripta.Spedizione.IndirizzoSpedizione;
import it.bologna.ausl.model.entities.shpeck.Address;
import it.bologna.ausl.model.entities.shpeck.Message;
import it.bologna.ausl.model.entities.shpeck.MessageAddress;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 *
 * @author utente
 */
@Component
@NextSdrInterceptor(name = "allegato-interceptor")
public class DocInterceptor extends InternautaBaseInterceptor {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MezzoRepository mezzoRepository;

    @Override
    public Object beforeCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        Doc doc = (Doc) entity;
        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        if (additionalData != null && StringUtils.hasText(additionalData.get(InternautaConstants.AdditionalData.Keys.idPec.toString()))) {
            Integer idPec = Integer.parseInt(additionalData.get(InternautaConstants.AdditionalData.Keys.idPec.toString()));
            Optional<Message> messageOp = this.messageRepository.findById(idPec);
            if (messageOp.isPresent()) {
                Message message = messageOp.get();
                doc.setOggetto(message.getSubject());
                Address addressMittente = message.getMessageAddressList().stream().filter(
                        messageAddress -> messageAddress.getAddressRole() == MessageAddress.AddressRoleType.FROM).findFirst().get().getIdAddress();
                Related mittenteDoc = new Related();
                mittenteDoc.setDataInserimento(ZonedDateTime.now());
                if (StringUtils.hasText(addressMittente.getOriginalAddress())) {
                    mittenteDoc.setDescrizione(addressMittente.getOriginalAddress());
                } else {
                    mittenteDoc.setDescrizione(addressMittente.getMailAddress());
                }
                mittenteDoc.setIdPersonaInserente(authenticatedSessionData.getPerson());
                mittenteDoc.setOrigine(Related.OrigineRelated.ESTERNO);
                mittenteDoc.setTipo(Related.TipoRelated.MITTENTE);
                mittenteDoc.setIdDoc(doc);
                Spedizione spedizione = new Spedizione();
                spedizione.setData(message.getReceiveTime().atZone(ZoneId.systemDefault()));
                spedizione.setIdMessage(message);
                Mezzo mezzo = mezzoRepository.findOne(QMezzo.mezzo.codice.eq(Mezzo.CodiciMezzo.MAIL.toString())).get();
                spedizione.setIdMezzo(mezzo);
                spedizione.setIdRelated(mittenteDoc);
                IndirizzoSpedizione indirizzoSpedizione = new IndirizzoSpedizione();
                indirizzoSpedizione.setCompleto(addressMittente.getMailAddress());
                spedizione.setIndirizzo(indirizzoSpedizione);
                mittenteDoc.setSpedizioneList(Arrays.asList(new Spedizione[]{spedizione}));
                List<Related> relatedList = new ArrayList();
                relatedList.add(mittenteDoc);
                doc.setRelated(relatedList);
            }
        }

        doc.setDataCreazione(ZonedDateTime.now());

        Azienda azienda = authenticatedSessionData.getUser().getIdAzienda();
        doc.setIdAzienda(azienda);
        return doc;
    }

    @Override
    public Class getTargetEntityClass() {
        return Doc.class;
    }

}
