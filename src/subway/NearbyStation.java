package subway;

/**
 * 功能2的结果记录：沿某条线路距离给定站点小于 n 公里的车站。
 * 同一车站若可从不同线路到达，会产生多条记录（线路归属不同）。
 */
public class NearbyStation {

    private final String stationName;
    private final String lineName;
    private final double distanceKm;

    public NearbyStation(String stationName, String lineName, double distanceKm) {
        this.stationName = stationName;
        this.lineName = lineName;
        this.distanceKm = distanceKm;
    }

    public String getStationName() {
        return stationName;
    }

    public String getLineName() {
        return lineName;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    @Override
    public String toString() {
        return String.format("<%s, %s, %.3fkm>", stationName, lineName, distanceKm);
    }
}
