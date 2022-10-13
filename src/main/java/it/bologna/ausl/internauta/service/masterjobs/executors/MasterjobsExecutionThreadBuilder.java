package it.bologna.ausl.internauta.service.masterjobs.executors;

/**
 *
 * @author gdm
 */
public interface MasterjobsExecutionThreadBuilder {
    public MasterjobsExecutionThread self(MasterjobsExecutionThread self);
    public MasterjobsExecutionThread activeThreadsSetName(String activeThreadsSetName);
    public MasterjobsExecutionThread inQueueNormal(String inQueueNormal);
    public MasterjobsExecutionThread inQueueHigh(String inQueueNormal);
    public MasterjobsExecutionThread inQueueHighest(String inQueueNormal);
    public MasterjobsExecutionThread workQueue(String workQueue);
    public MasterjobsExecutionThread errorQueue(String errorQueue);
    public MasterjobsExecutionThread waitQueue(String waitQueue);
    public MasterjobsExecutionThread outQueue(String outQueue);
    public MasterjobsExecutionThread sleepMillis(int sleepMillis);
    public MasterjobsExecutionThread queueReadTimeoutMillis(int queueReadTimeoutMillis);
}
