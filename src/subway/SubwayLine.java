package subway;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 一条地铁线路：有序站点列表 + 相邻区间距离。
 * segmentDistances 的第 i 项表示 stationNames 中第 i 站与第 i+1 站之间的距离（公里）。
 * 约定数据文件中同一条线路的区间记录连续且按车站顺序排列。
 */
public class SubwayLine {

    private final String name;
    private final List<String> stationNames = new ArrayList<>();
    private final List<Double> segmentDistances = new ArrayList<>();

    public SubwayLine(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<String> getStationNames() {
        return Collections.unmodifiableList(stationNames);
    }

    public List<Double> getSegmentDistances() {
        return Collections.unmodifiableList(segmentDistances);
    }

    public int getStationCount() {
        return stationNames.size();
    }

    void appendStation(String stationName) {
        stationNames.add(stationName);
    }

    void appendSegment(double distanceKm) {
        segmentDistances.add(distanceKm);
    }

    /** 返回车站在本线路中的序号，不在本线路上时返回 -1 */
    public int indexOfStation(String stationName) {
        return stationNames.indexOf(stationName);
    }

    @Override
    public String toString() {
        return name;
    }
}
