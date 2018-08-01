package it.bologna.ausl.baborg.model.entities.projections;
        
import it.bologna.ausl.baborg.model.entities.Azienda;
import it.bologna.ausl.baborg.model.entities.UtenteStruttura;
import it.bologna.ausl.baborg.model.entities.projections.generated.UtenteWithIdAzienda;
import it.bologna.ausl.baborg.model.entities.projections.generated.UtenteWithIdAziendaAndPecUtenteSetAndUtenteStrutturaSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;


@Projection(name = "CustomUtenteWithIdAzienda", types = it.bologna.ausl.baborg.model.entities.Utente.class)
public interface CustomUtenteWithIdAzienda extends UtenteWithIdAziendaAndPecUtenteSetAndUtenteStrutturaSet{
    
    @Override
//    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptor(target, 'getIdAzienda', target.getIdAzienda().getClass())}")
    public Object getIdAzienda();
    
    @Override
//    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorSet(target, 'getUtenteStrutturaSet')}")
    public Set getUtenteStrutturaSet();

    @Override
//    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorSet(target, 'getPecUtenteSet')}")
    public Set getPecUtenteSet();
}