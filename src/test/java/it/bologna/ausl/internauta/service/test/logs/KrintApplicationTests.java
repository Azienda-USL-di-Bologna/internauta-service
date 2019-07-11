
package it.bologna.ausl.internauta.service.test.logs;

import it.bologna.ausl.internauta.service.repositories.logs.OperazioneVersionataKrinRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.logs.OperazioneVersionataKrint;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 *
 * @author guido
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class KrintApplicationTests {
    
    @Autowired
    public CachedEntities cachedEntities;
    
    @Autowired
    public OperazioneVersionataKrinRepository operazioneVersionataKrinRepository;
    
    @Test
    public void testOperazioni() {
        
            
        
            OperazioneKrint operazioneKrint = cachedEntities.getOperazioneKrint(OperazioneKrint.CodiceOperazione.PEC_MESSAGE_PROTOCOLLAZIONE);
            OperazioneVersionataKrint operazioneVersionataKrint = 
                            operazioneVersionataKrinRepository.findFirstByIdOperazioneOrderByVersioneDesc(operazioneKrint).orElse(null);
            
            Assertions.assertThat(operazioneVersionataKrint.getVersione()).isEqualTo(1);
            
    }
    
}