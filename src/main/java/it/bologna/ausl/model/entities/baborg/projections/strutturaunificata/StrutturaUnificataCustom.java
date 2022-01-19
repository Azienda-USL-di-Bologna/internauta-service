package it.bologna.ausl.model.entities.baborg.projections.strutturaunificata;

import it.bologna.ausl.model.entities.baborg.StrutturaUnificata;
import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaUnificataWithPlainFields;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;


@Projection(name = "StrutturaUnificataCustom", types = StrutturaUnificata.class)
public interface StrutturaUnificataCustom extends StrutturaUnificataWithPlainFields {
        
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptor(target, 'getIdStrutturaSorgente', 'StrutturaWithAttributiStrutturaAndIdAzienda')}")
    public Object getIdStrutturaSorgente();
        
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptor(target, 'getIdStrutturaDestinazione', 'StrutturaWithAttributiStrutturaAndIdAzienda')}")
    public Object getIdStrutturaDestinazione();
}