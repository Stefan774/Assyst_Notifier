package ru.mvideo.assystnotifier;

import java.util.ArrayList;

public class IncidentList {
    private ArrayList<Incident> incidents;

    public ArrayList<Incident> getIncidents() {
        return incidents;
    }

    public IncidentList() {

        incidents = new ArrayList<Incident>();
    }

    public void add(Incident inc) {
        incidents.add(inc);
    }

    public void add(char eventType, int refNo, int incSerious, String desc) {
        incidents.add(new Incident(eventType, refNo, incSerious, desc));
    }

    @Override
    public String toString() {
        String result = "";
        for (Incident inc : incidents) {
            result += inc.getEventType() + " | " + inc.getRefNo() + " | " + inc.getIncSerious() + " | " + inc.getDesc() + "\n";
        }
        return result;
    }

    public int count() {
        return incidents.size();
    }

    public int count(int incSerious) {
        int count = 0;
        for (Incident inc : incidents) {
            if (inc.getIncSerious() == incSerious) count++;
        }
        return count;
    }
}
