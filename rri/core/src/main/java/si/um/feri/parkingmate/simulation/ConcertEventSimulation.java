// ConcertEventSimulation.java
package si.um.feri.parkingmate.simulation;

public class ConcertEventSimulation {

    public enum EventPhase {
        PRE_EVENT,      // 3 sata pre koncerta
        EVENT_START,    // početak koncerta
        EVENT_PEAK,     // vrhunac koncerta
        EVENT_END,      // kraj koncerta
        POST_EVENT      // 2 sata nakon koncerta
    }

    private float eventTime = 0f; // u minutima (0-24h)
    private boolean isEventActive = false;
    private float eventDuration = 4 * 60f; // 4 sata trajanje koncerta
    private float eventStartTime = 18 * 60f; // 18:00 početak

    public ConcertEventSimulation() {
        eventTime = eventStartTime - (3 * 60f); // počinjemo 3 sata pre koncerta
    }

    public EventPhase getCurrentPhase() {
        if (!isEventActive) return EventPhase.PRE_EVENT;

        float elapsed = eventTime - eventStartTime;

        if (elapsed < 0) return EventPhase.PRE_EVENT;
        else if (elapsed < 60) return EventPhase.EVENT_START; // prvi sat
        else if (elapsed < 120) return EventPhase.EVENT_PEAK; // drugi sat (vrhunac)
        else if (elapsed < eventDuration) return EventPhase.EVENT_END; // završetak
        else return EventPhase.POST_EVENT;
    }

    public void update(float delta, float timeMultiplier) {
        eventTime += (delta * 30f * timeMultiplier); // 30x realnog vremena

        // Ako smo u vremenskom periodu koncerta
        if (eventTime >= (eventStartTime - (3 * 60f)) &&
            eventTime <= (eventStartTime + eventDuration + (2 * 60f))) {
            isEventActive = true;
        } else {
            isEventActive = false;
        }
    }

    public void reset() {
        eventTime = eventStartTime - (3 * 60f);
        isEventActive = false;
    }

    public float getEventTime() {
        return eventTime;
    }

    public boolean isEventActive() {
        return isEventActive;
    }

    public float getEventOccupancyMultiplier() {
        EventPhase phase = getCurrentPhase();
        switch (phase) {
            case PRE_EVENT:
                return 1.5f; // polako se povećava
            case EVENT_START:
                return 2.0f; // brži dolazak
            case EVENT_PEAK:
                return 3.0f; // maksimalna gužva
            case EVENT_END:
                return 2.5f; // polako se smanjuje
            case POST_EVENT:
                return 1.2f; // još uvijek gužva
            default:
                return 1.0f;
        }
    }

    public String formatEventTime() {
        int hours = ((int) eventTime / 60) % 24;
        int minutes = (int) eventTime % 60;

        EventPhase phase = getCurrentPhase();
        String phaseText = "";
        switch (phase) {
            case PRE_EVENT: phaseText = "Pre koncerta"; break;
            case EVENT_START: phaseText = "Početak"; break;
            case EVENT_PEAK: phaseText = "Vrhunac!"; break;
            case EVENT_END: phaseText = "Završetak"; break;
            case POST_EVENT: phaseText = "Nakon koncerta"; break;
        }

        return String.format("🎵 %02d:%02d - %s", hours, minutes, phaseText);
    }

    public float getEventStartTime() {
        return eventStartTime;
    }
}
