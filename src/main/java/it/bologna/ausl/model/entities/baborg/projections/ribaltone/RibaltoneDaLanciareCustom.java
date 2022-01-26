
package it.bologna.ausl.model.entities.baborg.projections.ribaltone;

import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdPersona;
import it.bologna.ausl.model.entities.ribaltoneutils.RibaltoneDaLanciare;
import it.bologna.ausl.model.entities.ribaltoneutils.projections.generated.RibaltoneDaLanciareWithIdUtente;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;


@Projection(name = "RibaltoneDaLanciareCustom", types = RibaltoneDaLanciare.class)
public interface RibaltoneDaLanciareCustom extends RibaltoneDaLanciareWithIdUtente {

    @Override
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptor(target, 'getIdUtente', 'UtenteWithIdPersona')}")
    public Object getIdUtente();
}
