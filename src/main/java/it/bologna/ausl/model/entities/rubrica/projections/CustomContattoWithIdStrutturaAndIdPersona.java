package it.bologna.ausl.model.entities.rubrica.projections;

import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithIdAzienda;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.projections.generated.ContattoWithIdPersonaAndIdPersonaCreazioneAndIdStruttura;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "CustomContattoWithIdStrutturaAndIdPersona", types = Contatto.class)
public interface CustomContattoWithIdStrutturaAndIdPersona extends ContattoWithIdPersonaAndIdPersonaCreazioneAndIdStruttura {

    @Override
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptor(target, 'getIdStruttura', 'StrutturaWithIdAzienda')}")
    public StrutturaWithIdAzienda getIdStruttura();
}