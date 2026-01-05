// SimulationTimeMapper.java (ažurirano)
package si.um.feri.parkingmate.simulation;

public class SimulationTimeMapper {

    public static DayPhase getDayPhase(float totalMinutes) {
        int hour = ((int) totalMinutes / 60) % 24;

        if (hour >= 7 && hour < 9) {
            return DayPhase.MORNING_RUSH;
        } else if (hour >= 9 && hour < 15) {
            return DayPhase.DAYTIME;
        } else if (hour >= 15 && hour < 18) {
            return DayPhase.AFTERNOON_RUSH;
        } else if (hour >= 18 && hour < 22) {
            return DayPhase.EVENING;
        } else {
            return DayPhase.NIGHT;
        }
    }

    public static float getOccupancyRateMultiplier(DayPhase phase) {
        switch (phase) {
            case MORNING_RUSH:
                return 1.8f;
            case AFTERNOON_RUSH:
                return 2.0f;
            case EVENING:
                return 0.8f;
            case NIGHT:
                return 0.5f;
            case DAYTIME:
            default:
                return 1.0f;
        }
    }

    public static float getArrivalMultiplier(DayPhase phase) {
        switch (phase) {
            case MORNING_RUSH:
                return 2.5f;
            case AFTERNOON_RUSH:
                return 2.0f;
            case EVENING:
                return 0.7f;
            case NIGHT:
                return 0.3f;
            case DAYTIME:
            default:
                return 1.0f;
        }
    }
}
