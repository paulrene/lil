package no.leinstrandil.incident;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;

public class IncidentHub {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(IncidentHub.class);
    private static List<IncidentListener> listenerList = new ArrayList<>();

    private IncidentHub() {
    }

    public static void addIncidentListener(IncidentListener listener) {
        listenerList.add(listener);
    }

    public static void report(Incident incident) {
        for (IncidentListener listener : listenerList) {
            try {
                listener.incidentOccured(incident);
            } catch (Throwable t) {
                log.warn("Exception occured during invocation of IncidentListener", t);
            }
        }
    }

}
