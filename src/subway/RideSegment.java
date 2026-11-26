package subway;

/**
 * 一段连续乘车区间（功能5）：同一线路上不发生换乘的连续路段。
 * stationCount 为该段乘车的站数（不含上车站，含下车站）。
 */
public class RideSegment {

    private final String lineName;
    private final String fromStation;
    private final String toStation;
    private final int stationCount;
    private final double distanceKm;

    public RideSegment(
            String lineName,
            String fromStation,
            String toStation,
            int stationCount,
            double distanceKm) {
        this.lineName = lineName;
        this.fromStation = fromStation;
        this.toStation = toStation;
        this.stationCount = stationCount;
        this.distanceKm = distanceKm;
    }

    public String getLineName() {
        return lineName;
    }

    public String getFromStation() {
        return fromStation;
    }

    public String getToStation() {
        return toStation;
    }

    public int getStationCount() {
        return stationCount;
    }

    public double getDistanceKm() {
        return distanceKm;
    }
}
