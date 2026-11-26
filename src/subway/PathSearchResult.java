package subway;

import java.util.Collections;
import java.util.List;

/**
 * 功能3的结果：起点到终点的所有无环路径（每条路径为站点名称序列）。
 * 大型线网中简单路径数量可能组合爆炸，枚举达到安全上限时停止，
 * 并通过 truncated 标记告知调用方结果可能不完整。
 */
public class PathSearchResult {

    private final List<List<String>> paths;
    private final boolean truncated;

    public PathSearchResult(List<List<String>> paths, boolean truncated) {
        this.paths = List.copyOf(paths);
        this.truncated = truncated;
    }

    public List<List<String>> getPaths() {
        return paths;
    }

    public int getPathCount() {
        return paths.size();
    }

    public boolean isTruncated() {
        return truncated;
    }
}
