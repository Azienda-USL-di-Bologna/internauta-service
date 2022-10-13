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
public class MasterjobsHighestPriorityExecutionThread extends MasterjobsExecutionThread {
    private static final Logger log = LoggerFactory.getLogger(MasterjobsHighestPriorityExecutionThread.class);
    
    @Override
    protected String getName() {
        return "HighestPriorityExecutor";
    }
    
    @Override
    public void runExecutor() throws MasterjobsInterruptException {
        Set.SetPriority priority = Set.SetPriority.HIGHEST;
        while (true) {
            try {
                self.manageQueue(priority);
                priority = Set.SetPriority.HIGHEST;
                Thread.sleep(super.sleepMillis);
            } catch (MasterjobsInterruptException ex) {
                throw ex;
            } catch (MasterjobsReadQueueTimeout ex) {
                String queue = ex.getQueue();
                if (queue.equals(inQueueHighest)) {
                    priority = Set.SetPriority.HIGH;
                } else if (queue.equals(inQueueHigh)) {
                    priority = Set.SetPriority.NORMAL;
                } else { // normal
                    priority = Set.SetPriority.HIGHEST;
                }
            } catch (Exception ex) {
                log.error("execution error, moving next...", ex);
            }
        }
    }
    
}
