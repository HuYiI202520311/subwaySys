package subway;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * 地铁线网核心类：负责从数据文件加载线网、维护站点图，并提供各类查询功能。
 *
 * 图模型：车站为节点（不同线路的同名车站视为同一节点，即换乘站），
 * 相邻车站区间为带权无向边（实现上每条无向边拆成两条方向相反的有向边），
 * 边权为区间里程（公里），边上记录所属线路。
 */
public class SubwayNetwork {

    private static final double EPS = 1e-6;

    /** 功能3枚举全部路径的数量安全上限，防止大线网中简单路径组合爆炸耗尽内存 */
    public static final int MAX_PATH_COUNT = 20000;

    /** 功能3单条路径最多包含的站点数安全上限，超过则剪掉该分支（其余分支继续搜索） */
    public static final int MAX_PATH_LENGTH = 30;

    /** 功能3搜索访问节点总数的安全上限，防止极端线网下枚举耗时过长 */
    public static final int MAX_VISITED_NODES = 2000000;

    /** 车站表：站名 -> 车站对象，保持加载顺序 */
    private final Map<String, Station> stations = new LinkedHashMap<>();

    /** 线路表：线路名 -> 线路对象，保持加载顺序 */
    private final Map<String, SubwayLine> lines = new LinkedHashMap<>();

    /**
     * 从数据文件加载整个地铁网。
     * 文件格式：每行以 Tab 分隔，依次为 线路名、起点站、终点站、距离(km)；
     * 以 # 开头的行和空行视为注释跳过。单行数据非法只告警跳过，不影响整体加载。
     */
    public void loadFromFile(Path filePath) {
        List<String> rawLines;
        try {
            rawLines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        } catch (IOException exc) {
            throw new SubwayException("读取数据文件失败: " + filePath.toAbsolutePath(), exc);
        }

        stations.clear();
        lines.clear();

        int lineNo = 0;
        for (String rawLine : rawLines) {
            lineNo++;
            String text = rawLine.strip();
            // 兼容文件带 UTF-8 BOM 的情况
            if (lineNo == 1 && text.startsWith("\uFEFF")) {
                text = text.substring(1);
            }
            if (text.isEmpty() || text.startsWith("#")) {
                continue;
            }
            String[] columns = text.split("\t");
            if (columns.length < 4) {
                System.out.println("警告: 第" + lineNo + "行不足4列, 已跳过");
                continue;
            }
            try {
                double distanceKm = Double.parseDouble(columns[3].strip());
                if (distanceKm <= 0) {
                    System.out.println("警告: 第" + lineNo + "行距离非正数, 已跳过");
                    continue;
                }
                addSegment(columns[0].strip(), columns[1].strip(), columns[2].strip(), distanceKm);
            } catch (NumberFormatException exc) {
                System.out.println("警告: 第" + lineNo + "行距离不是数字, 已跳过");
            }
        }
    }

    /** 把一个区间同时登记到线路（有序站点表）和站点图（双向邻接边）中 */
    private void addSegment(String lineName, String fromName, String toName, double distanceKm) {
        SubwayLine line = lines.computeIfAbsent(lineName, SubwayLine::new);
        // 线路的第一个区间记录两站，之后每个区间只追加一个新站
        if (line.getStationCount() == 0) {
            line.appendStation(fromName);
        }
        line.appendStation(toName);
        line.appendSegment(distanceKm);

        Station from = stations.computeIfAbsent(fromName, Station::new);
        Station to = stations.computeIfAbsent(toName, Station::new);
        from.addLine(lineName);
        to.addLine(lineName);
        from.addOutgoingEdge(new Edge(from, to, lineName, distanceKm));
        to.addOutgoingEdge(new Edge(to, from, lineName, distanceKm));
    }

    public int getLineCount() {
        return lines.size();
    }

    public int getStationCount() {
        return stations.size();
    }

    public Collection<SubwayLine> getAllLines() {
        return Collections.unmodifiableCollection(lines.values());
    }

    public Collection<Station> getAllStations() {
        return Collections.unmodifiableCollection(stations.values());
    }

    /**
     * 按名称查找车站。
     * 为兼容口语化输入，精确匹配失败且以“站”结尾时去掉末字再试一次
     * （如“中南路站”->“中南路”）；“汉口火车站”这类站名本身含“站”，
     * 精确匹配已先命中，不受影响。找不到时抛出异常。
     */
    public Station getStation(String stationName) {
        if (stationName == null || stationName.strip().isEmpty()) {
            throw new SubwayException("站点名称不能为空");
        }
        String key = stationName.strip();
        Station station = stations.get(key);
        if (station == null && key.endsWith("站")) {
            station = stations.get(key.substring(0, key.length() - 1));
        }
        if (station == null) {
            throw new SubwayException("站点不存在: " + stationName);
        }
        return station;
    }

    /**
     * 功能1：识别所有中转站（至少两条线路经过的站点）。
     * 返回 站点名称 -> 经过的所有线路 的集合，如 <中南路, [2号线, 4号线]>。
     */
    public Map<String, Set<String>> findTransferStations() {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        for (Station station : stations.values()) {
            if (station.isTransferStation()) {
                result.put(station.getName(), station.getLineNames());
            }
        }
        return result;
    }

    /**
     * 功能2：输入某一站点，输出线路距离小于 maxDistanceKm 的所有站点集合，
     * 每条记录包含站点名称、所在线路、距离给定站点的距离。
     * 沿经过该站的每条线路向两个方向展开并累计区间里程，超出限额即终止该方向。
     */
    public List<NearbyStation> findNearbyStations(String stationName, double maxDistanceKm) {
        if (Double.isNaN(maxDistanceKm) || Double.isInfinite(maxDistanceKm) || maxDistanceKm <= 0) {
            throw new SubwayException("距离参数不合法: " + maxDistanceKm + ", 必须为正数");
        }
        Station center = getStation(stationName);
        List<NearbyStation> result = new ArrayList<>();
        for (String lineName : center.getLineNames()) {
            SubwayLine line = lines.get(lineName);
            int centerIndex = line.indexOfStation(center.getName());
            if (centerIndex < 0) {
                continue;
            }
            collectAlongLine(line, centerIndex, 1, maxDistanceKm, result);
            collectAlongLine(line, centerIndex, -1, maxDistanceKm, result);
        }
        return result;
    }

    /** 沿线路单方向累计里程，收集距离小于限额的站点；direction 取 1（向后）或 -1（向前） */
    private void collectAlongLine(
            SubwayLine line,
            int centerIndex,
            int direction,
            double maxDistanceKm,
            List<NearbyStation> result) {
        List<String> names = line.getStationNames();
        List<Double> distances = line.getSegmentDistances();
        double accumulated = 0.0;
        int index = centerIndex;
        while (true) {
            // 向前方向取 index-1 号区间，向后方向取 index 号区间
            int segmentIndex = direction > 0 ? index : index - 1;
            if (segmentIndex < 0 || segmentIndex >= distances.size()) {
                return;
            }
            accumulated += distances.get(segmentIndex);
            index += direction;
            // 区间里程均为正数，一旦累计距离达到限额，更远的站点必然也超限
            if (accumulated >= maxDistanceKm - EPS) {
                return;
            }
            result.add(new NearbyStation(names.get(index), line.getName(), accumulated));
        }
    }

    /**
     * 功能3：输入起点站和终点站，返回连接两站的所有路径集合，
     * 每条路径经过的站点不重复（即不包含环路）。
     * 深度优先搜索 + 访问标记回溯；对站点序列去重
     * （同一对站点间有多条线路时会走出完全相同的站点序列，只保留一条）。
     * 达到 MAX_PATH_COUNT 或 MAX_PATH_LENGTH 安全上限时停止枚举并置截断标记。
     */
    public PathSearchResult findAllPaths(String fromName, String toName) {
        Station from = getStation(fromName);
        Station to = getStation(toName);
        if (from == to) {
            throw new SubwayException("起点与终点不能相同: " + from.getName());
        }
        List<List<String>> paths = new ArrayList<>();
        Set<List<String>> deduplicated = new HashSet<>();
        Set<String> visited = new HashSet<>();
        PathSearchState state = new PathSearchState();
        searchAllPaths(from, to, visited, new ArrayList<>(), paths, deduplicated, state);
        return new PathSearchResult(paths, state.aborted || state.lengthCapped);
    }

    /** 功能3搜索过程的状态：截断标记与已访问节点计数 */
    private static final class PathSearchState {

        int visitedNodes;

        /** 有分支因达到路径长度上限被剪掉，更长的路径未纳入结果 */
        boolean lengthCapped;

        /** 达到路径数量或访问节点上限，搜索被整体中止 */
        boolean aborted;
    }

    /** 深度优先搜索递归体；state.aborted 用于提前终止所有层的递归 */
    private void searchAllPaths(
            Station current,
            Station target,
            Set<String> visited,
            List<String> path,
            List<List<String>> paths,
            Set<List<String>> deduplicated,
            PathSearchState state) {
        if (state.aborted) {
            return;
        }
        state.visitedNodes++;
        if (state.visitedNodes > MAX_VISITED_NODES) {
            state.aborted = true;
            return;
        }
        path.add(current.getName());
        visited.add(current.getName());

        if (current == target) {
            List<String> found = List.copyOf(path);
            if (deduplicated.add(found)) {
                paths.add(new ArrayList<>(found));
                if (paths.size() >= MAX_PATH_COUNT) {
                    state.aborted = true;
                }
            }
        } else if (path.size() >= MAX_PATH_LENGTH) {
            // 只剪掉当前分支，其余分支继续搜索
            state.lengthCapped = true;
        } else {
            for (Edge edge : current.getOutgoingEdges()) {
                if (!visited.contains(edge.getTo().getName())) {
                    searchAllPaths(edge.getTo(), target, visited, path, paths, deduplicated, state);
                    if (state.aborted) {
                        break;
                    }
                }
            }
        }

        path.remove(path.size() - 1);
        visited.remove(current.getName());
    }

    /**
     * 功能4：给定起点站和终点站，返回一条最短路径（总里程最小），
     * 结果为从起点到终点依次经过的所有站点及对应乘车区间。
     * Dijkstra 算法；里程相同时优先换乘次数少的路径，更符合实际出行习惯。
     * 站点不存在、起终点相同或两站不连通时抛出异常。
     */
    public RouteResult findShortestPath(String fromName, String toName) {
        Station from = getStation(fromName);
        Station to = getStation(toName);
        if (from == to) {
            throw new SubwayException("起点与终点不能相同: " + from.getName());
        }

        Map<String, Double> bestDistance = new HashMap<>();
        Map<String, Integer> bestTransfers = new HashMap<>();
        Map<String, Edge> previousEdge = new HashMap<>();
        Set<String> settled = new HashSet<>();

        // 优先队列允许同一站点重复入队，出队时依据 settled 集合跳过过期条目
        record Candidate(double distance, int transfers, Station station) {
        }
        PriorityQueue<Candidate> queue = new PriorityQueue<>(
                Comparator.comparingDouble(Candidate::distance)
                        .thenComparingInt(Candidate::transfers));

        bestDistance.put(from.getName(), 0.0);
        bestTransfers.put(from.getName(), 0);
        queue.add(new Candidate(0.0, 0, from));

        while (!queue.isEmpty()) {
            Candidate current = queue.poll();
            Station u = current.station();
            if (!settled.add(u.getName())) {
                continue;
            }
            if (u == to) {
                break;
            }
            Edge arrivedBy = previousEdge.get(u.getName());
            for (Edge edge : u.getOutgoingEdges()) {
                Station v = edge.getTo();
                if (settled.contains(v.getName())) {
                    continue;
                }
                double newDistance = current.distance() + edge.getDistanceKm();
                int newTransfers = current.transfers();
                if (arrivedBy != null && !arrivedBy.getLineName().equals(edge.getLineName())) {
                    newTransfers++;
                }
                Double oldDistance = bestDistance.get(v.getName());
                boolean improved = oldDistance == null
                        || newDistance < oldDistance - EPS
                        || (Math.abs(newDistance - oldDistance) <= EPS
                                && newTransfers < bestTransfers.getOrDefault(v.getName(), Integer.MAX_VALUE));
                if (improved) {
                    bestDistance.put(v.getName(), newDistance);
                    bestTransfers.put(v.getName(), newTransfers);
                    previousEdge.put(v.getName(), edge);
                    queue.add(new Candidate(newDistance, newTransfers, v));
                }
            }
        }

        if (!settled.contains(to.getName())) {
            throw new SubwayException(
                    "起点 " + from.getName() + " 与终点 " + to.getName() + " 之间不存在可达路径");
        }

        // 从终点沿 previousEdge 回溯，还原完整乘车序列
        List<Edge> edges = new ArrayList<>();
        Station cursor = to;
        while (cursor != from) {
            Edge edge = previousEdge.get(cursor.getName());
            edges.add(edge);
            cursor = edge.getFrom();
        }
        Collections.reverse(edges);

        List<String> stationNames = new ArrayList<>();
        stationNames.add(from.getName());
        for (Edge edge : edges) {
            stationNames.add(edge.getTo().getName());
        }
        double totalDistance = edges.stream().mapToDouble(Edge::getDistanceKm).sum();
        return new RouteResult(stationNames, edges, totalDistance, countTransfers(edges));
    }

    /** 统计换乘次数：相邻两条边的线路名发生变化即计一次换乘 */
    private static int countTransfers(List<Edge> edges) {
        int count = 0;
        for (int i = 1; i < edges.size(); i++) {
            if (!edges.get(i - 1).getLineName().equals(edges.get(i).getLineName())) {
                count++;
            }
        }
        return count;
    }

    /**
     * 按站点序列计算一条路径的总里程（公里），供功能6对用户选中的路径计价。
     * 同一对站点间有多条线路时区间里程相同，取最小值即可。
     */
    public double getPathDistance(List<String> stationNames) {
        if (stationNames == null || stationNames.size() < 2) {
            throw new SubwayException("路径至少需要包含两个站点");
        }
        double total = 0.0;
        for (int i = 1; i < stationNames.size(); i++) {
            total += getAdjacentDistance(stationNames.get(i - 1), stationNames.get(i));
        }
        return total;
    }

    /** 查询相邻两站之间的区间里程，两站不相邻时抛出异常 */
    private double getAdjacentDistance(String fromName, String toName) {
        Station from = getStation(fromName);
        Station to = getStation(toName);
        double shortest = Double.MAX_VALUE;
        for (Edge edge : from.getOutgoingEdges()) {
            if (edge.getTo() == to) {
                shortest = Math.min(shortest, edge.getDistanceKm());
            }
        }
        if (shortest == Double.MAX_VALUE) {
            throw new SubwayException("路径中两站不相邻: " + fromName + ", " + toName);
        }
        return shortest;
    }
}
