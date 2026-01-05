package si.um.feri.parkingmate.simulation;

public class SimulationTimeMapper {

    public static DayPhase getDayPhase(float simulationMinutes) {
        int hour = ((int) simulationMinutes / 60) % 24;

        if (hour >= 7 && hour < 10) {
            return DayPhase.MORNING;
        } else if (hour >= 10 && hour < 20) {
            return DayPhase.DAY;
        } else {
            return DayPhase.NIGHT;
        }
    }
}
