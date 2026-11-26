package subway;

import java.util.ArrayList;
import java.util.List;

/**
 * 功能4的结果：一条最短乘车路径。
 * 除站点序列外还保留乘车边序列（含线路信息），供功能5分段展示和功能6/7计价使用。
 */
public class RouteResult {

    private final List<String> stationNames;
    private final List<Edge> edges;
    private final double totalDistanceKm;
    private final int transferCount;

    public RouteResult(
            List<String> stationNames,
            List<Edge> edges,
            double totalDistanceKm,
            int transferCount) {
        this.stationNames = List.copyOf(stationNames);
        this.edges = List.copyOf(edges);
        this.totalDistanceKm = totalDistanceKm;
        this.transferCount = transferCount;
    }

    public List<String> getStationNames() {
        return stationNames;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public int getTransferCount() {
        return transferCount;
    }

    public int getStationCount() {
        return stationNames.size();
    }

    /**
     * 功能5：把路径拆分为若干同线连续乘车段。
     * 相邻两条边线路名不同即发生一次换乘，开始新的一段。
     */
    public List<RideSegment> splitIntoSegments() {
        List<RideSegment> segments = new ArrayList<>();
        int start = 0;
        for (int i = 1; i <= edges.size(); i++) {
            boolean lineChanged = i == edges.size()
                    || !edges.get(i).getLineName().equals(edges.get(i - 1).getLineName());
            if (!lineChanged) {
                continue;
            }
            Edge firstEdge = edges.get(start);
            Edge lastEdge = edges.get(i - 1);
            double segmentDistance = 0.0;
            for (int j = start; j < i; j++) {
                segmentDistance += edges.get(j).getDistanceKm();
            }
            segments.add(new RideSegment(
                    firstEdge.getLineName(),
                    firstEdge.getFrom().getName(),
                    lastEdge.getTo().getName(),
                    i - start,
                    segmentDistance));
            start = i;
        }
        return segments;
    }

    /**
     * 功能5：以简洁形式把乘车方案打印至标准输出，
     * 每段只包含线路与起讫站，如“在B站换乘2号线，乘N站至C站”。
     */
    public void printItinerary() {
        List<RideSegment> segments = splitIntoSegments();
        for (int i = 0; i < segments.size(); i++) {
            RideSegment segment = segments.get(i);
            if (i == 0) {
                System.out.printf("从%s乘坐%s，乘%d站至%s%n",
                        segment.getFromStation(), segment.getLineName(),
                        segment.getStationCount(), segment.getToStation());
            } else {
                System.out.printf("在%s换乘%s，乘%d站至%s%n",
                        segment.getFromStation(), segment.getLineName(),
                        segment.getStationCount(), segment.getToStation());
            }
        }
        System.out.printf("全程共%d站，里程%.3f公里，换乘%d次%n",
                stationNames.size(), totalDistanceKm, transferCount);
    }
}
