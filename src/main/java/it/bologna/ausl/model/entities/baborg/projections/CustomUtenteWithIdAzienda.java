package it.bologna.ausl.model.entities.baborg.projections;
        
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdAziendaAndPecUtenteListAndUtenteStrutturaList;
import java.util.List;

import org.springframework.data.rest.core.config.Projection;


@Projection(name = "CustomUtenteWithIdAzienda", types = Utente.class)
public interface CustomUtenteWithIdAzienda extends UtenteWithIdAziendaAndPecUtenteListAndUtenteStrutturaList {
    
    @Override
//    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptor(target, 'getIdAzienda', target.getIdAzienda().getClass())}")
    public Object getIdAzienda();
    
    @Override
//    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorList(target, 'getUtenteStrutturaList')}")
    public List getUtenteStrutturaList();

    @Override
//    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorList(target, 'getPecUtenteList')}")
    public List getPecUtenteList();
}