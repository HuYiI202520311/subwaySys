#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""渲染武汉地铁模拟运行系统类图为 PNG（UML 类图，手工定位布局）。"""

from __future__ import annotations

from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import FancyArrowPatch, Rectangle

OUTPUT_PATH = Path(__file__).parent / "类图.png"

plt.rcParams["font.sans-serif"] = ["Microsoft YaHei"]
plt.rcParams["axes.unicode_minus"] = False

fig, ax = plt.subplots(figsize=(19, 13))
ax.set_xlim(0, 34)
ax.set_ylim(0, 22.6)
ax.set_aspect("equal")
ax.axis("off")

LINE_H = 0.5
FONT_MEMBER = 8.6
FONT_NAME = 11.5


def draw_class(
        x: float,
        y_top: float,
        w: float,
        name: str,
        attrs: list,
        methods: list,
        stereotype: str = "",
) -> dict:
    """画三格类框，返回各边锚点"""
    name_h = 1.15 if stereotype else 0.85
    attr_h = max(len(attrs), 1) * LINE_H
    meth_h = max(len(methods), 1) * LINE_H
    h = name_h + attr_h + meth_h
    ax.add_patch(Rectangle((x, y_top - h), w, h, facecolor="white", edgecolor="black", lw=1.3, zorder=3))
    ax.plot([x, x + w], [y_top - name_h, y_top - name_h], color="k", lw=1.0, zorder=4)
    ax.plot([x, x + w], [y_top - name_h - attr_h, y_top - name_h - attr_h], color="k", lw=1.0, zorder=4)

    if stereotype:
        ax.text(x + w / 2, y_top - 0.35, stereotype, ha="center", va="center",
                fontsize=9, fontstyle="italic", color="#444444", zorder=4)
        ax.text(x + w / 2, y_top - 0.8, name, ha="center", va="center",
                fontsize=FONT_NAME, fontweight="bold", zorder=4)
    else:
        ax.text(x + w / 2, y_top - name_h / 2, name, ha="center", va="center",
                fontsize=FONT_NAME, fontweight="bold", zorder=4)

    y_line = y_top - name_h - LINE_H / 2 + 0.02
    for attr in attrs:
        ax.text(x + 0.18, y_line, attr, ha="left", va="center", fontsize=FONT_MEMBER, zorder=4)
        y_line -= LINE_H
    y_line = y_top - name_h - attr_h - LINE_H / 2 + 0.02
    for meth in methods:
        ax.text(x + 0.18, y_line, meth, ha="left", va="center", fontsize=FONT_MEMBER, zorder=4)
        y_line -= LINE_H

    return {
        "left": (x, y_top - h / 2),
        "right": (x + w, y_top - h / 2),
        "top": (x + w / 2, y_top),
        "bottom": (x + w / 2, y_top - h),
        "y_top": y_top,
        "y_bottom": y_top - h,
        "x_left": x,
        "x_right": x + w,
    }


def draw_polyline(points: list, dashed: bool = False, diamond: bool = False,
                  arrow: bool = True, hollow_arrow: bool = False) -> None:
    """折线连接；diamond 在起点画实心菱形（组合），arrow 在终点画箭头"""
    xs = [p[0] for p in points]
    ys = [p[1] for p in points]
    ax.plot(xs, ys, color="k", lw=1.1, linestyle="--" if dashed else "-", zorder=2)
    if diamond:
        ax.plot(xs[0], ys[0], marker="D", color="k", markersize=7, zorder=3)
    if arrow:
        style = "-|>" if hollow_arrow else "->"
        patch = FancyArrowPatch(
            points[-2], points[-1],
            arrowstyle=style, mutation_scale=18,
            facecolor="white" if hollow_arrow else "none",
            edgecolor="black", lw=1.1, zorder=3,
        )
        ax.add_patch(patch)


def label(text: str, xy: tuple, size: float = 8.6, rotate: float = 0.0) -> None:
    ax.text(xy[0], xy[1], text, ha="center", va="center", fontsize=size,
            fontstyle="italic", color="#333333", rotation=rotate, zorder=5)


test = draw_class(0.6, 21.4, 5.6, "Test",
                  ["-DATA_FILE: Path"],
                  ["+main(args: String[]): void",
                   "-testAllPaths(network): PathSearchResult",
                   "-testShortestPath(network): RouteResult",
                   "-testFares(network, allPaths): void"])

network = draw_class(8.2, 21.6, 7.6, "SubwayNetwork",
                     ["-stations: Map<String, Station>",
                      "-lines: Map<String, SubwayLine>"],
                     ["+loadFromFile(filePath: Path): void",
                      "+getStation(name: String): Station",
                      "+findTransferStations(): Map<String, Set<String>>",
                      "+findNearbyStations(name: String, n: double): List<NearbyStation>",
                      "+findAllPaths(from: String, to: String): PathSearchResult",
                      "+findShortestPath(from: String, to: String): RouteResult",
                      "+getPathDistance(path: List<String>): double"])

station = draw_class(18.8, 21.6, 6.0, "Station",
                     ["-name: String",
                      "-lineNames: Set<String>",
                      "-outgoingEdges: List<Edge>"],
                     ["+isTransferStation(): boolean",
                      "+getLineNames(): Set<String>"])

line = draw_class(27.0, 21.6, 6.4, "SubwayLine",
                  ["-name: String",
                   "-stationNames: List<String>",
                   "-segmentDistances: List<Double>"],
                  ["+indexOfStation(name: String): int",
                   "+getStationCount(): int"])

edge = draw_class(18.8, 15.6, 6.0, "Edge",
                  ["-from: Station",
                   "-to: Station",
                   "-lineName: String",
                   "-distanceKm: double"],
                  ["+getDistanceKm(): double"])

nearby = draw_class(0.6, 14.6, 6.2, "NearbyStation",
                    ["-stationName: String",
                     "-lineName: String",
                     "-distanceKm: double"],
                    ["+toString(): String"])

path_result = draw_class(0.6, 9.6, 6.2, "PathSearchResult",
                         ["-paths: List<List<String>>",
                          "-truncated: boolean"],
                         ["+getPathCount(): int",
                          "+isTruncated(): boolean"])

route = draw_class(8.2, 12.6, 7.6, "RouteResult",
                   ["-stationNames: List<String>",
                    "-edges: List<Edge>",
                    "-totalDistanceKm: double",
                    "-transferCount: int"],
                   ["+splitIntoSegments(): List<RideSegment>",
                    "+printItinerary(): void"])

segment = draw_class(8.2, 6.6, 7.6, "RideSegment",
                     ["-lineName: String",
                      "-fromStation: String",
                      "-toStation: String",
                      "-stationCount: int",
                      "-distanceKm: double"],
                     [])

fare = draw_class(18.8, 9.8, 7.8, "FareCalculator",
                  ["-EPS: double"],
                  ["+singleTicketFareYuan(km: double): int  {static}",
                   "+rideFareJiao(km: double, t: TicketType): int  {static}",
                   "+ticketPriceYuan(t: TicketType): int  {static}",
                   "+formatFare(fareJiao: int): String  {static}"])

ticket = draw_class(18.8, 5.2, 7.8, "TicketType",
                    ["SINGLE", "WUHAN_PASS", "DAY_PASS_1", "DAY_PASS_3", "DAY_PASS_7"],
                    [],
                    stereotype="«enumeration»")

exception = draw_class(28.0, 14.6, 5.4, "SubwayException",
                       [], [],
                       stereotype="«exception»")

# Test 的依赖
draw_polyline([(6.2, 19.7), (7.2, 19.7), (7.2, 18.9), (8.2, 18.9)], dashed=True)
draw_polyline([(1.2, 18.05), (0.25, 18.05), (0.25, 0.5), (17.4, 0.5), (17.4, 7.0), (18.8, 7.0)], dashed=True)

# 组合关系（实心菱形在整体一侧）
draw_polyline([(15.8, 20.0), (18.8, 20.0)], diamond=True, arrow=False)
label("1", (16.2, 20.3))
label("0..*", (18.4, 20.3))

draw_polyline([(12.0, 21.6), (12.0, 22.2), (30.2, 22.2), (30.2, 21.6)], diamond=True, arrow=False)
label("1", (12.4, 21.95))
label("0..*", (29.7, 21.95))

draw_polyline([(24.8, 19.9), (27.0, 19.9)], arrow=False)
label("2..*", (25.3, 20.15))
label("1", (26.6, 20.15))
label("经过", (25.9, 19.55))

draw_polyline([(21.8, 18.25), (21.8, 15.6)], diamond=True, arrow=False)
label("1", (22.1, 17.9))
label("0..*", (22.15, 15.95))

draw_polyline([(14.2, 12.6), (14.2, 11.75), (20.6, 11.75), (20.6, 12.25)], diamond=True, arrow=False)
label("1", (14.5, 12.35))
label("1..*", (21.0, 11.95))

# 依赖关系（虚线开放箭头）
draw_polyline([(8.2, 16.9), (7.5, 16.9), (7.5, 12.9), (6.8, 12.9)], dashed=True)
label("«create»", (7.5, 14.9), rotate=90)
draw_polyline([(8.2, 16.6), (7.15, 16.6), (7.15, 8.2), (6.8, 8.2)], dashed=True)
label("«create»", (7.15, 10.2), rotate=90)
draw_polyline([(12.0, 16.25), (12.0, 12.6)], dashed=True)
label("«create»", (12.6, 14.4))
draw_polyline([(15.8, 16.9), (26.4, 16.9), (26.4, 13.5), (28.0, 13.5)], dashed=True)
label("«throw»", (20.5, 17.2))
draw_polyline([(10.2, 8.75), (10.2, 6.6)], dashed=True)
label("«create»", (10.8, 7.7))
draw_polyline([(22.7, 6.45), (22.7, 5.2)], dashed=True)
draw_polyline([(26.6, 7.6), (30.7, 7.6), (30.7, 12.45)], dashed=True)
label("«throw»", (28.6, 7.9))

fig.savefig(OUTPUT_PATH, dpi=150, bbox_inches="tight", facecolor="white")
print(f"已生成: {OUTPUT_PATH}")
