/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.utils.redmine.managers;

import com.taskadapter.redmineapi.RedmineException;
import com.taskadapter.redmineapi.bean.Project;
import com.taskadapter.redmineapi.bean.Tracker;
import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;
import it.bologna.ausl.internauta.service.utils.redmine.factories.RedmineManagerFactory;
import java.util.Collection;
import org.json.JSONObject;

/**
 *
 * @author Salo
 */
public class RedmineTrackerManager extends RedmineAbstractManager {

    public RedmineTrackerManager(ParametersManagerInterface pm) {
        super(pm);
    }

    private Project getBabelProject() throws RedmineException {
        RedmineProjectManager redmineProjectManager = (RedmineProjectManager) RedmineManagerFactory.getRedmineProjectManager(parametersManager);
        return redmineProjectManager.getBabelProject();
    }

    public Collection<Tracker> getTrackers() throws RedmineException {
        Collection<Tracker> trackers = getBabelProject().getTrackers();
        return trackers;
    }

    public Integer getTrackeByName(String name) throws Exception {
        try {
            Collection<Tracker> trackers = getTrackers();
            for (Tracker tracker : trackers) {
                if (tracker.getName().equals(name)) {
                    return tracker.getId();
                }
            }
            return null;
        } catch (Exception ex) {
            throw new Exception("Errore durante la get dei del Tracker by name: " + ex.getMessage(), ex);
        }

    }

//    public void getTrackers() throws RedmineCallerException {
//        RedmineRequestManager trackersRequestManager = RedMineRequestManagerFactory.getGetTrackersRequestManager(parametersManager);
//        trackersRequestManager.prepareRequest();
//        RedMineCaller redMineCaller = RedMineCallerFactory.getRedMineCaller(parametersManager);
//        JSONObject res = redMineCaller.doCall(trackersRequestManager);
//        System.out.println("Trackers\n" + res.toString(4));
//    }
}
