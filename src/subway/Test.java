package subway;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 测试类：依次测试武汉地铁模拟运行系统的功能1-7。
 * 测试数据从 subway.txt 读入（每行: 线路名\t起点站\t终点站\t距离km，Tab分隔）。
 *
 * 在项目根目录编译运行：
 *   javac -encoding UTF-8 -d out src/subway/*.java
 *   java -Dstdout.encoding=UTF-8 -cp out subway.Test
 */
public class Test {

    private static final Path DATA_FILE = Paths.get("subway.txt");

    public static void main(String[] args) {
        // 允许通过命令行参数指定数据文件路径，默认取工作目录下的 subway.txt
        Path dataFile = args.length > 0 ? Paths.get(args[0]) : DATA_FILE;
        SubwayNetwork network = new SubwayNetwork();
        network.loadFromFile(dataFile);

        System.out.printf("数据加载完成: 共%d条线路, %d个车站%n",
                network.getLineCount(), network.getStationCount());
        for (SubwayLine line : network.getAllLines()) {
            System.out.printf("  %s: %d站%n", line.getName(), line.getStationCount());
        }
        System.out.println();

        testTransferStations(network);
        testNearbyStations(network);
        PathSearchResult allPaths = testAllPaths(network);
        RouteResult route = testShortestPath(network);
        testRoutePrinting(route);
        testFares(network, allPaths);
    }

    /** 功能1：识别所有中转站 */
    private static void testTransferStations(SubwayNetwork network) {
        System.out.println("[功能1] 所有中转站（至少两条线路经过的站点）");
        Map<String, Set<String>> transferStations = network.findTransferStations();
        System.out.println("共 " + transferStations.size() + " 个:");
        for (Map.Entry<String, Set<String>> entry : transferStations.entrySet()) {
            System.out.printf("<%s, %s>%n", entry.getKey(), String.join("、", entry.getValue()));
        }
        System.out.println();
    }

    /** 功能2：线路距离小于 n 的站点集合，含异常处理演示 */
    private static void testNearbyStations(SubwayNetwork network) {
        System.out.println("[功能2] 与某站线路距离小于n的站点集合");

        // 作业示例场景。示例把珞雄路的实际距离1.009km近似记为1，这里取n=2验证双向展开
        printNearby(network, "华中科技大学", 2.0);
        // 三条线路经过的中转站：不同方向的站点分别记录所在线路
        printNearby(network, "徐家棚", 3.0);
        // 兼容带“站”后缀的输入；n=1时珞雄路(1.009km)已超限，只剩光谷大道
        printNearby(network, "华中科技大学站", 1.0);

        // 输入不合规的异常处理
        try {
            network.findNearbyStations("光谷站", 1.0);
        } catch (SubwayException exc) {
            System.out.println("捕获非法输入: " + exc.getMessage());
        }
        try {
            network.findNearbyStations("华中科技大学", -1.0);
        } catch (SubwayException exc) {
            System.out.println("捕获非法输入: " + exc.getMessage());
        }
        System.out.println();
    }

    private static void printNearby(SubwayNetwork network, String stationName, double maxDistanceKm) {
        List<NearbyStation> nearby = network.findNearbyStations(stationName, maxDistanceKm);
        System.out.printf("距%s线路距离小于%.1f公里的站点(共%d个):%n",
                stationName, maxDistanceKm, nearby.size());
        for (NearbyStation item : nearby) {
            System.out.println("  " + item);
        }
    }

    /** 功能3：起终点间所有无环路径，含异常处理演示；结果供功能6计价复用 */
    private static PathSearchResult testAllPaths(SubwayNetwork network) {
        System.out.println("[功能3] 起点与终点之间的所有无环路径");
        String from = "中南路";
        String to = "洪山广场";
        PathSearchResult result = network.findAllPaths(from, to);
        System.out.printf("%s → %s 共%d条路径%s，前5条如下:%n",
                from, to, result.getPathCount(),
                result.isTruncated() ? "（已达安全上限，更长路径未纳入）" : "");
        List<List<String>> paths = result.getPaths();
        for (int i = 0; i < Math.min(5, paths.size()); i++) {
            System.out.printf("  路径%d: %s%n", i + 1, String.join(" -> ", paths.get(i)));
        }

        // 输入不合规的异常处理
        try {
            network.findAllPaths(from, from);
        } catch (SubwayException exc) {
            System.out.println("捕获非法输入: " + exc.getMessage());
        }
        try {
            network.findAllPaths(from, "光谷站");
        } catch (SubwayException exc) {
            System.out.println("捕获非法输入: " + exc.getMessage());
        }
        System.out.println();
        return result;
    }

    /** 功能4：最短路径，含距离校验与异常处理演示；结果供功能5复用 */
    private static RouteResult testShortestPath(SubwayNetwork network) {
        System.out.println("[功能4] 两站之间的最短路径（里程最小）");

        // 同线短途，便于人工核对: 华中科技大学→光谷广场 应为 1.009+0.794=1.803 公里
        printRouteSummary(network.findShortestPath("华中科技大学", "光谷广场"));
        // 需要换乘的常规出行
        RouteResult route = network.findShortestPath("汉口火车站", "武汉火车站");
        printRouteSummary(route);
        // 横跨全网两端的极端场景（阳逻线终点 → 16号线终点）
        printRouteSummary(network.findShortestPath("金台", "通航机场"));

        // 输入不合规的异常处理
        try {
            network.findShortestPath("光谷广场", "光谷广场");
        } catch (SubwayException exc) {
            System.out.println("捕获非法输入: " + exc.getMessage());
        }
        try {
            network.findShortestPath("关山大道", "光谷广场");
        } catch (SubwayException exc) {
            System.out.println("捕获非法输入: " + exc.getMessage());
        }
        System.out.println();
        return route;
    }

    private static void printRouteSummary(RouteResult route) {
        List<String> names = route.getStationNames();
        System.out.printf("%s → %s: 共%d站, 里程%.3f公里, 换乘%d次%n",
                names.get(0), names.get(names.size() - 1), route.getStationCount(),
                route.getTotalDistanceKm(), route.getTransferCount());
        // 短途输出完整站点序列；长途只展示首尾各3站，避免刷屏
        if (names.size() <= 20) {
            System.out.println("  站点序列: " + String.join(" -> ", names));
        } else {
            System.out.printf("  站点序列(首尾各3站): %s -> ... -> %s%n",
                    String.join(" -> ", names.subList(0, 3)),
                    String.join(" -> ", names.subList(names.size() - 3, names.size())));
        }
    }

    /** 功能5：最短路径的简洁展示 */
    private static void testRoutePrinting(RouteResult route) {
        System.out.println("[功能5] 最短乘车路径的简洁展示");
        route.printItinerary();
        System.out.println();
    }

    /** 功能6、功能7：用户选中路径的单程票计价，及武汉通、定期票费用 */
    private static void testFares(SubwayNetwork network, PathSearchResult allPaths) {
        System.out.println("[功能6] 用户选中路径的普通单程票票价");

        // 模拟用户从功能3的路径集合中选择一条（取第一条含绕行的路径，验证按实际里程计价）
        List<String> chosen = chooseDemoPath(allPaths.getPaths());
        double distanceKm = network.getPathDistance(chosen);
        System.out.println("用户选择路径: " + String.join(" -> ", chosen));
        System.out.printf("路径里程 %.3f 公里, 普通单程票票价 %d元%n",
                distanceKm, FareCalculator.singleTicketFareYuan(distanceKm));

        // 分段计价规则边界自检
        System.out.print("计价边界自检: ");
        double[] samples = {4.0, 4.1, 8.0, 12.0, 12.1, 18.0, 24.0, 24.1, 32.0, 32.1};
        for (double sample : samples) {
            System.out.printf("%.1fkm=%d元 ", sample, FareCalculator.singleTicketFareYuan(sample));
        }
        System.out.println();
        System.out.println();

        System.out.println("[功能7] 同一路径下武汉通与定期票的费用");
        for (FareCalculator.TicketType type : FareCalculator.TicketType.values()) {
            int rideFareJiao = FareCalculator.rideFareJiao(distanceKm, type);
            int priceYuan = FareCalculator.ticketPriceYuan(type);
            String note = priceYuan > 0
                    ? String.format("（票价%d元/张, 有效期内不限次数）", priceYuan)
                    : "";
            System.out.printf("  %s: 本次乘车费用 %s%s%n",
                    type.getDisplayName(), FareCalculator.formatFare(rideFareJiao), note);
        }
        System.out.println();
    }

    /** 选一条站点数不少于4的绕行路径作演示；若不存在则退回第一条 */
    private static List<String> chooseDemoPath(List<List<String>> paths) {
        for (List<String> path : paths) {
            if (path.size() >= 4) {
                return path;
            }
        }
        return paths.get(0);
    }
}
