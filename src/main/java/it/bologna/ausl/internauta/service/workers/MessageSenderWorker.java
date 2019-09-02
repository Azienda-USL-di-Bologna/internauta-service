package it.bologna.ausl.internauta.service.workers;

import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class MessageSenderWorker implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(MessageSenderWorker.class);

    /*
    {
	"dest": [{
                    "id_persona": row["id_persona"],
                    "id_aziende": [],
                    "apps": ["scrivania"]
		}
	],
	"command": {
            "params": {
                "id_attivita": row["id"],
                "operation": TD["event"]
            },
            "command": "RefreshAttivita"
	}
    }
    */
    
    public MessageSenderWorker() {
    }

    @Override
    public void run() {
        log.info(" in run di " + getClass().getSimpleName());
        
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ex) {
            java.util.logging.Logger.getLogger(MessageSenderWorker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
