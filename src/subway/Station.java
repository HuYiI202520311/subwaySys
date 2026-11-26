package subway;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 地铁车站。
 * 站名作为全网唯一标识：不同线路的同名车站视为同一个站点（即换乘节点）。
 */
public class Station {

    private final String name;

    /** 经过该站的所有线路名，保持数据文件中的出现顺序 */
    private final Set<String> lineNames = new LinkedHashSet<>();

    /** 以该站为起点的邻接边（每个双向区间在加载时生成两条方向相反的边） */
    private final List<Edge> outgoingEdges = new ArrayList<>();

    public Station(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Set<String> getLineNames() {
        return Collections.unmodifiableSet(lineNames);
    }

    public List<Edge> getOutgoingEdges() {
        return Collections.unmodifiableList(outgoingEdges);
    }

    void addLine(String lineName) {
        lineNames.add(lineName);
    }

    void addOutgoingEdge(Edge edge) {
        outgoingEdges.add(edge);
    }

    /** 至少两条线路经过的站点即为中转站 */
    public boolean isTransferStation() {
        return lineNames.size() >= 2;
    }

    @Override
    public String toString() {
        return name;
    }
}
