//package it.bologna.ausl.model.entities.rubrica.projections;
//
//import it.bologna.ausl.model.entities.rubrica.GruppiContatti;
//import it.bologna.ausl.model.entities.rubrica.projections.generated.DettaglioContattoWithIdContatto;
//import it.bologna.ausl.model.entities.rubrica.projections.generated.GruppiContattiWithIdDettaglioContatto;
//import java.util.List;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.data.rest.core.config.Projection;
//
///**
// *
// * @author gusgus
// */
//@Projection(name = "CustomGruppiContattiWithIdDettaglioContatto", types = GruppiContatti.class)
//public interface CustomGruppiContattiWithIdDettaglioContatto extends GruppiContattiWithIdDettaglioContatto {
//    
//    @Value("#{@projectionBeans.getDettaglioContattoWithIdContatto(target)}")
//    @Override
//    public DettaglioContattoWithIdContatto getIdDettaglioContatto();
//}
