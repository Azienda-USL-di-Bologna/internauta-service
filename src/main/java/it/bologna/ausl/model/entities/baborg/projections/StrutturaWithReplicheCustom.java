package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithIdAzienda;
import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithAttributiStrutturaAndIdStrutturaReplicataAndStruttureReplicheList;
import it.bologna.ausl.model.entities.baborg.projections.generated.AttributiStrutturaWithIdTipologiaStruttura;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

@Projection(name = "StrutturaWithReplicheCustom", types = Struttura.class)
public interface StrutturaWithReplicheCustom extends StrutturaWithAttributiStrutturaAndIdStrutturaReplicataAndStruttureReplicheList {
    
    @Override
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptor(target, 'getIdStrutturaReplicata', 'StrutturaWithIdAzienda')}")
    public StrutturaWithIdAzienda getIdStrutturaReplicata();
    
    @Override
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getStruttureReplicheList', 'StrutturaWithIdAzienda')}")
    public List<StrutturaWithIdAzienda> getStruttureReplicheList();
    
    @Value("#{@projectionBeans.getFusioni(target)}")
    public Object getFusioni();
    
    @Override
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptor(target, 'getAttributiStruttura', 'AttributiStrutturaWithIdTipologiaStruttura')}")
    public AttributiStrutturaWithIdTipologiaStruttura getAttributiStruttura();
}