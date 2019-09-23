package it.bologna.ausl.internauta.service.schedulers.workers.messagesender;

import it.bologna.ausl.model.entities.messaggero.AmministrazioneMessaggio;

/**
 *
 * @author gdm
 */
public class MessageThreadEvent {
    public enum InterceptorPhase {
        BEFORE_INSERT, AFTER_INSERT, BEFORE_UPDATE, AFTER_UPDATE, BEFORE_DELETE, AFTER_DELETE
    }
    
    private AmministrazioneMessaggio amministrazioneMessaggio;
    private InterceptorPhase interceptorPhase;

    public MessageThreadEvent(AmministrazioneMessaggio amministrazioneMessaggio, InterceptorPhase interceptorPhase) {
        this.amministrazioneMessaggio = amministrazioneMessaggio;
        this.interceptorPhase = interceptorPhase;
    }

    public AmministrazioneMessaggio getAmministrazioneMessaggio() {
        return amministrazioneMessaggio;
    }

    public void setAmministrazioneMessaggio(AmministrazioneMessaggio amministrazioneMessaggio) {
        this.amministrazioneMessaggio = amministrazioneMessaggio;
    }

    public InterceptorPhase getInterceptorPhase() {
        return interceptorPhase;
    }

    public void setInterceptorPhase(InterceptorPhase interceptorPhase) {
        this.interceptorPhase = interceptorPhase;
    }

    @Override
    public String toString() {
        return String.format("interceptor phase: %s AmministrazioneMessaggio: %s", interceptorPhase.toString(), amministrazioneMessaggio.toString());
    }
}
