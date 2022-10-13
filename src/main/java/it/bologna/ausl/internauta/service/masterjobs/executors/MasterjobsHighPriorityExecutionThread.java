package it.bologna.ausl.internauta.service.masterjobs.executors;

import it.bologna.ausl.internauta.service.masterjobs.exceptions.MasterjobsExecutionThreadsException;
import it.bologna.ausl.internauta.service.masterjobs.exceptions.MasterjobsInterruptException;
import it.bologna.ausl.internauta.service.masterjobs.exceptions.MasterjobsReadQueueTimeout;
import it.bologna.ausl.model.entities.masterjobs.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author gdm
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MasterjobsHighPriorityExecutionThread extends MasterjobsExecutionThread {
    private static final Logger log = LoggerFactory.getLogger(MasterjobsHighPriorityExecutionThread.class);

    @Override
    protected String getName() {
        return "HighPriorityExecutor";
    }
    
    @Override
    public void runExecutor() throws MasterjobsInterruptException {
        Set.SetPriority priority = Set.SetPriority.HIGH;
        while (true) {
            try {
                self.manageQueue(priority);
                switch (priority) {
                    case HIGHEST:
                        priority = Set.SetPriority.HIGH;
                        break;
                    case NORMAL:
                    case HIGH:
                        priority = Set.SetPriority.HIGHEST;
                }
//                self.manageQueue(priority);
                //self.manageNormalQueue();
                Thread.sleep(super.sleepMillis);
            } catch (MasterjobsInterruptException ex) {
                throw ex;
            } catch (MasterjobsReadQueueTimeout ex) {
                String queue = ex.getQueue();
                if (queue.equals(inQueueHigh)) {
                    priority = Set.SetPriority.HIGHEST;
                } else if (queue.equals(inQueueHighest)) {
                    priority = Set.SetPriority.NORMAL;
                } else { // normal
                    priority = Set.SetPriority.HIGH;
                }
            } catch (Exception ex) {
                log.error("execution error, moving next...", ex);
            }
        }
        
    }

    
}
