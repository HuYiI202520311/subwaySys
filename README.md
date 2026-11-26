# 武汉地铁模拟运行系统

Java 课程大作业实现。基于武汉地铁 12 条线路的站间里程数据（`subway.txt`），
实现**线路查询**与**计价**两大模块，覆盖作业全部 7 个评分功能。

## 1. 编译与运行

需要 JDK 16 以上（开发环境为 JDK 21）。在项目根目录执行：

```bash
# 编译（Windows cmd 下通配符写法为 src\subway\*.java）
javac -encoding UTF-8 -d out src/subway/*.java

# 运行测试类（默认读取工作目录下的 subway.txt）
java -Dstdout.encoding=UTF-8 -cp out subway.Test

# 也可用第一个参数指定数据文件路径
java -Dstdout.encoding=UTF-8 -cp out subway.Test D:/data/subway.txt
```

`-Dstdout.encoding=UTF-8` 用于保证 Windows 控制台下中文输出不乱码。

## 2. 目录结构

```
subwaySys/
├── README.md                 本文档
├── 要求.txt                   作业说明与线路里程数据（参考资料）
├── subway.txt                测试数据：每行 Tab 分隔 = 线路名 起点站 终点站 距离km
├── src/subway/               源代码（package subway）
│   ├── SubwayNetwork.java    核心：加载数据建图 + 功能1~4 + 路径里程计算
│   ├── FareCalculator.java   功能6/7：分段计价、武汉通9折、定期票
│   ├── RouteResult.java      功能4结果 + 功能5（分段、简洁打印）
│   ├── Station.java          车站（站名、所属线路、邻接边）
│   ├── Edge.java             相邻站区间有向边（含线路名、里程）
│   ├── SubwayLine.java       线路（有序站点、区间距离）
│   ├── NearbyStation.java    功能2结果记录
│   ├── PathSearchResult.java 功能3结果记录（含截断标记）
│   ├── RideSegment.java      功能5乘车段记录
│   ├── SubwayException.java  自定义业务异常
│   └── Test.java             含 main 的测试类
└── docs/
    ├── 用例图.png / 类图.png   渲染好的图（matplotlib 脚本生成）
    ├── 用例图.puml / 类图.puml PlantUML 图源（可粘贴到 plantuml.com 渲染）
    ├── 用例详细描述.md          用例 UC-05 的课本式规格描述
    └── render_use_case.py / render_class_diagram.py   图的渲染脚本
```

## 3. 评分功能与代码对照

| 分值 | 功能 | 实现入口 |
|---|---|---|
| 10 | 识别所有中转站（≥2条线经过），返回 <站名, 线路集合> | `SubwayNetwork.findTransferStations()` |
| 10 | 与某站线路距离小于 n 的站点集合（含异常处理） | `SubwayNetwork.findNearbyStations()` |
| 10 | 起终点间所有无环路径集合 | `SubwayNetwork.findAllPaths()` |
| 20 | 最短路径：起点到终点经过的所有站点数组（含异常处理） | `SubwayNetwork.findShortestPath()` |
| 10 | 最短路径简洁打印（每段乘车的线路与起讫站） | `RouteResult.printItinerary()` |
| 10 | 用户从路径集合选一条，计算普通单程票票价 | `SubwayNetwork.getPathDistance()` + `FareCalculator.singleTicketFareYuan()` |
| 10 | 同一路径的武汉通（9折）与定期票（0元）费用 | `FareCalculator.rideFareJiao()` / `ticketPriceYuan()` |

实测结果摘录（`subway.txt`：12 条线路、250 个车站、33 个中转站）：

- 华中科技大学 → 光谷广场：3 站、1.803 公里、换乘 0 次（= 1.009 + 0.794，可人工核对）
- 汉口火车站 → 武汉火车站：16 站、20.418 公里、换乘 4 次
- 中南路 → 洪山广场：枚举出 486 条无环路径
- 16.131 公里路径：单程票 5 元、武汉通 4.5 元、定期票 0 元

## 4. 系统建模

### 4.1 用例图

PlantUML 源码（见 [docs/用例图.puml](docs/用例图.puml)）：
![use_case.png](docs%2Fuse_case.png)

用例说明：

| 用例 | 含义 | 对应功能 |
|---|---|---|
| 查询线路与站点信息 | 某站拥有的线路数、某条线路拥有的站点数 | 题面查询模块 |
| 查询中转站 | 至少两条线路经过的站点及其线路集合 | 功能1 |
| 查询附近站点 | 沿同线累计里程小于 n 的站点（站名、线路、距离） | 功能2 |
| 查询两点间全部无环路径 | DFS 枚举简单路径 | 功能3 |
| 查询最短路径 | Dijkstra 求里程最小路径 | 功能4 |
| 简洁展示乘车方案 | "从A乘坐X线至B，在B换乘Y线至C" | 功能5 |
| 计算乘车票价 | 按所选路径里程计价 | 功能6、7 |

关系解释：

- **关联**：乘客直接发起 6 个主用例；
- **`UC05 --include--> UC06`**：找到最短路径后必然以简洁形式呈现给用户（功能5 依附于功能4 的结果）；
- **`UC07 --include--> UC04`**：计价流程要求"用户从路径集合中选择一条路径"，因此必须先包含全部路径查询；
- **泛化**：单程票、武汉通、定期票三种计价是"计算乘车票价"的特化——同一计价入口、按票种执行不同规则（全价 / 9 折 / 0 元）。

### 4.2 类图

PlantUML 源码（见 [docs/类图.puml](docs/类图.puml)）：
![class.png](docs%2Fclass.png)

类职责说明：

| 类 | 职责 |
|---|---|
| `SubwayNetwork` | 核心门面：解析 `subway.txt` 建图，持有全部车站与线路，提供功能1~4 及路径里程计算 |
| `Station` | 车站节点；站名全网唯一，同名即同一换乘节点；记录所属线路与邻接边 |
| `Edge` | 相邻站区间有向边，携带线路归属与里程；同一站对的多条线路各自建边 |
| `SubwayLine` | 一条线路的有序站点表与区间距离表，供功能2 沿线路展开 |
| `NearbyStation` / `PathSearchResult` / `RouteResult` / `RideSegment` | 四个查询结果的值对象（data class） |
| `FareCalculator` | 静态计价工具：分段票价、各票种乘车费用、定期票售价 |
| `TicketType` | 票种枚举：单程票、武汉通、1/3/7 日票 |
| `SubwayException` | 输入不合规时统一抛出的业务异常 |
| `Test` | 含 `main` 的测试类，逐项演示 7 个功能与异常处理 |

关系解释：

- **组合（实心菱形）**：`Station`、`SubwayLine` 由 `SubwayNetwork` 在加载时创建并拥有，生命周期一致；
  `Edge` 由 `Station` 的 `outgoingEdges` 拥有；一条 `RouteResult` 拥有其乘车区间边序列（1 对 1..*）；
- **关联**：`SubwayLine 1 — 2..* Station`（一条线路至少经过两站，一个站可被多条线路经过），
  代码中表现为 `SubwayLine.stationNames` 与 `Station.lineNames` 互记；
- **依赖 «create»**：`SubwayNetwork` 的各查询方法构造并返回结果对象；`RouteResult` 构造乘车段；
- **依赖 «throw»**：`SubwayNetwork`、`FareCalculator` 在输入非法时抛出 `SubwayException`；
- **依赖**：`FareCalculator` 依赖 `TicketType` 区分票种；`Test` 组装并调用 `SubwayNetwork` 与 `FareCalculator`。

### 4.3 用例详细描述

按课本示例对用例 **UC-05 查询最短路径** 的完整规格（参与者、前置/后置条件、
基本流、备选流、异常流、业务规则）见 [docs/用例详细描述.md](docs/用例详细描述.md)。

## 5. 关键设计决策

- **同名即同站**：不同线路站名相同视为同一换乘节点；数据中"武汉火车站 / 武汉站东广场 /
  武汉站西广场"名称不同，因此不算换乘——换乘识别完全由数据驱动；
- **功能2 语义**：沿经过该站的每条线路向两个方向累计区间里程，取小于 n 的站点，
  记录含所在线路；作业示例把珞雄路实际 1.009km 近似记为 1，与本实现的精确值略有出入；
- **功能3 安全上限**：大线网中简单路径数量可能组合爆炸，枚举设三重上限
  （2 万条路径 / 单路径 30 站 / 访问 200 万节点），超限即停止并在结果中置 `truncated` 标记，
  长度上限只剪当前分支、不影响其他分支的完整性；
- **功能4 平局规则**：Dijkstra 以里程为主关键字，里程相同时优先换乘次数少的路径；
- **金额用"角"整数计算**：避免浮点误差；武汉通 9 折即 票价(元) × 9（角），结果恒为精确值；
- **加载鲁棒性**：数据文件单行非法（列数不足、距离非数字等）只告警跳过，不影响整体加载。

## 6. 计价规则（武汉地铁现行按里程分段计价）

| 里程区间 | 单程票价 |
|---|---|
| ≤4 km | 2 元 |
| 4~8 km | 3 元 |
| 8~12 km | 4 元 |
| 12~18 km | 5 元 |
| 18~24 km | 6 元 |
| 24~32 km | 7 元 |
| 32~40 km | 8 元 |
| 40 km 以上 | 每增加 8 km 加 1 元 |

- 武汉通：单程票价 9 折；
- 定期票：1 日票 18 元/张、3 日票 45 元/张、7 日票 90 元/张，有效期内不限次数乘坐，单次乘车费用记 0 元。
