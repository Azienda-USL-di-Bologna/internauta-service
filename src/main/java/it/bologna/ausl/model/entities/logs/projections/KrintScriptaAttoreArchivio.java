package it.bologna.ausl.model.entities.logs.projections;

import it.bologna.ausl.model.entities.scripta.AttoreArchivio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "KrintScriptaAttoreArchivio", types = AttoreArchivio.class)
public interface KrintScriptaAttoreArchivio{
    
    Integer getId();
    
//    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptor(target, 'getIdPersona', 'KrintBaborgPersona')}")
//    KrintBaborgPersona getIdPersona();
    
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptor(target, 'getIdStruttura', 'KrintBaborgStruttura')}")
    KrintBaborgStruttura getIdStruttura();

    @Value("#{target.getRuolo().name()}") 
    String getRuolo();
}
