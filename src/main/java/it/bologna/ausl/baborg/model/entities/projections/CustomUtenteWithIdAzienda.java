package it.bologna.ausl.baborg.model.entities.projections;
        
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdAziendaAndPecUtenteListAndUtenteStrutturaList;
import java.util.Set;
import org.springframework.data.rest.core.config.Projection;

@Projection(name = "CustomUtenteWithIdAzienda", types = it.bologna.ausl.model.entities.baborg.Utente.class)
public interface CustomUtenteWithIdAzienda extends UtenteWithIdAziendaAndPecUtenteListAndUtenteStrutturaList{
    
    @Override
//    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptor(target, 'getIdAzienda', target.getIdAzienda().getClass())}")
    public Object getIdAzienda();
    
    @Override
//    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorSet(target, 'getUtenteStrutturaSet')}")
    public Set getUtenteStrutturaList();

    @Override
//    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorSet(target, 'getPecUtenteSet')}")
    public Set getPecUtenteList();
}