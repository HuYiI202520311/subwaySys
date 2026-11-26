package subway;

/**
 * 相邻两站之间的一条有向边，携带线路归属与区间里程。
 * 同一对站点之间可能有多条线路的边（如洪山广场—中南路同时有2号线和4号线），各自独立建边。
 */
public class Edge {

    private final Station from;
    private final Station to;
    private final String lineName;
    private final double distanceKm;

    public Edge(Station from, Station to, String lineName, double distanceKm) {
        this.from = from;
        this.to = to;
        this.lineName = lineName;
        this.distanceKm = distanceKm;
    }

    public Station getFrom() {
        return from;
    }

    public Station getTo() {
        return to;
    }

    public String getLineName() {
        return lineName;
    }

    public double getDistanceKm() {
        return distanceKm;
    }
}
