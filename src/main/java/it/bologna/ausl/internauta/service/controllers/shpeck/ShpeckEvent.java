package it.bologna.ausl.internauta.service.controllers.shpeck;

/**
 *
 * @author gdm
 */
public class ShpeckEvent {
    public enum Phase {
        BEFORE_INSERT, AFTER_INSERT, BEFORE_UPDATE, AFTER_UPDATE, BEFORE_DELETE, AFTER_DELETE
    }
    public enum Operation {
        SEND_CUSTOM_DELETE_INTIMUS_COMMAND
    }
    
    private Phase phase;
    private Operation operation;
    private Object data;

    public ShpeckEvent() {
    }

    public ShpeckEvent(Phase phase, Operation operation, Object data) {
        this.phase = phase;
        this.operation = operation;
        this.data = data;
    }

    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return String.format("phase: %s operation: %s", phase.toString(), operation.toString());
    }
}
