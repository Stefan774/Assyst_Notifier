package ru.mvideo.assystnotifier;

public class Incident {
    private char eventType;
    private int refNo;
    private int incSerious;
    private String desc;
    public static final int VIP = 1;
    public static final int HIGH = 2;
    public static final int NORMAL = 3;
    public static final int LOW = 4;

    public char getEventType() {
        return eventType;
    }

    public int getIncSerious() {
        return incSerious;
    }

    public int getRefNo() {
        return refNo;
    }

    public boolean equals(Incident inc) {
        return (inc.getRefNo() == this.refNo) && (inc.eventType == this.eventType);
    }

    public String getDesc() {
        return desc;
    }

    public Incident() {
        this.eventType = 'i';
        this.refNo = 0;
        this.incSerious = 3;
        this.desc = "";
    }

    public Incident(char eventType, int refNo, int incSerious, String desc) {
        this.eventType = eventType;
        this.refNo = refNo;
        this.incSerious = incSerious;
        this.desc = desc;
    }
}
