package it.bologna.ausl.model.entities.lotti.projections;

import it.bologna.ausl.model.entities.lotti.GruppoLotto;
import it.bologna.ausl.model.entities.lotti.projections.generated.GruppoLottoWithComponentiList;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author QB
 */
@Component
public class LottiProjectionUtils {
    
    @Autowired
    protected ProjectionFactory factory;

    public List<GruppoLottoWithComponentiList> filterGruppoLottoPerTipo(List<GruppoLotto> gruppo, String tipo) {
        List<GruppoLottoWithComponentiList> res = new ArrayList<GruppoLottoWithComponentiList>();
        if (gruppo != null) {
            List<GruppoLotto> gruppiList = gruppo.stream().filter(g -> g.getTipo().toString().equals(tipo)).collect(Collectors.toList());
            if (gruppiList != null && !gruppiList.isEmpty()) {
                res = gruppiList.stream().map(g -> {
                    return factory.createProjection(GruppoLottoWithComponentiList.class, g);
                }).collect(Collectors.toList());
            }
        }
        return res;
    }
}
