#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""渲染武汉地铁模拟运行系统用例图为 PNG（UML 用例图，手绘布局）。"""

from __future__ import annotations

from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import Circle, Ellipse, FancyArrowPatch, Rectangle

OUTPUT_PATH = Path(__file__).parent / "用例图.png"

plt.rcParams["font.sans-serif"] = ["Microsoft YaHei"]
plt.rcParams["axes.unicode_minus"] = False

fig, ax = plt.subplots(figsize=(16, 10))
ax.set_xlim(0, 16)
ax.set_ylim(0, 10)
ax.set_aspect("equal")
ax.axis("off")


def draw_actor(x: float, y: float, label: str) -> None:
    """画小人图标：y 为头部圆心纵坐标"""
    ax.add_patch(Circle((x, y), 0.22, fill=False, lw=1.6))
    ax.plot([x, x], [y - 0.22, y - 0.9], color="k", lw=1.6)
    ax.plot([x - 0.4, x + 0.4], [y - 0.55, y - 0.55], color="k", lw=1.6)
    ax.plot([x, x - 0.35], [y - 0.9, y - 1.45], color="k", lw=1.6)
    ax.plot([x, x + 0.35], [y - 0.9, y - 1.45], color="k", lw=1.6)
    ax.text(x, y - 1.85, label, ha="center", va="center", fontsize=13)


def draw_usecase(x: float, y: float, w: float, h: float, text: str) -> None:
    """画用例椭圆（白色填充盖住连线的多余部分）"""
    ax.add_patch(Ellipse((x, y), w, h, facecolor="white", edgecolor="black", lw=1.4, zorder=3))
    ax.text(x, y, text, ha="center", va="center", fontsize=11, zorder=4, linespacing=1.4)


def draw_association(x1: float, y1: float, x2: float, y2: float) -> None:
    """参与者与用例之间的实线关联"""
    ax.plot([x1, x2], [y1, y2], color="k", lw=1.1, zorder=1)


def draw_dashed_arrow(p1: tuple, p2: tuple, label: str, label_xy: tuple, rad: float = 0.0) -> None:
    """虚线箭头（include/extend 依赖），标签放在指定位置"""
    arrow = FancyArrowPatch(
        p1, p2,
        arrowstyle="->", mutation_scale=16,
        linestyle="dashed", color="black", lw=1.2, zorder=2,
        connectionstyle=f"arc3,rad={rad}",
    )
    ax.add_patch(arrow)
    ax.text(label_xy[0], label_xy[1], label, ha="center", va="center",
            fontsize=9.5, fontstyle="italic", color="#333333")


def draw_generalization(child_xy: tuple, parent_xy: tuple) -> None:
    """泛化：子用例指向父用例的实线空心三角箭头"""
    arrow = FancyArrowPatch(
        child_xy, parent_xy,
        arrowstyle="-|>", mutation_scale=26,
        facecolor="white", edgecolor="black", lw=1.2, zorder=2,
    )
    ax.add_patch(arrow)


# 系统边界
ax.add_patch(Rectangle((3.4, 0.15), 12.2, 9.55, fill=False, lw=1.6))
ax.text(9.5, 9.42, "武汉地铁模拟运行系统", ha="center", va="center", fontsize=15, fontweight="bold")

# 参与者
draw_actor(1.5, 6.35, "乘客")

# 主列用例
draw_usecase(6.8, 8.65, 4.4, 1.15, "查询线路与站点信息\n(某站线路数、某线站点数)")
draw_usecase(6.8, 7.05, 3.2, 0.8, "查询中转站")
draw_usecase(6.8, 5.65, 4.4, 1.15, "查询附近站点\n(线路距离小于n的站点)")
draw_usecase(6.8, 4.15, 4.0, 0.8, "查询两点间全部无环路径")
draw_usecase(6.8, 2.85, 3.2, 0.8, "查询最短路径")
draw_usecase(6.8, 1.65, 3.2, 0.8, "计算乘车票价")

# 右侧用例
draw_usecase(12.3, 2.85, 3.6, 0.8, "简洁展示乘车方案")

# 票种泛化子用例
draw_usecase(4.6, 0.62, 2.6, 0.72, "单程票计价")
draw_usecase(7.5, 0.62, 3.0, 0.72, "武汉通计价(9折)")
draw_usecase(10.6, 0.62, 3.2, 0.72, "定期票计价(0元)")

# 参与者关联线
for uc_y in (8.65, 7.05, 5.65, 4.15, 2.85, 1.65):
    draw_association(1.5, 5.2, 6.8, uc_y)

# include 关系
draw_dashed_arrow((8.4, 2.85), (10.5, 2.85), "<<include>>", (9.45, 3.12))
draw_dashed_arrow((5.4, 1.85), (5.1, 3.9), "<<include>>", (4.9, 2.7), rad=0.45)

# 泛化关系（空心三角指向“计算乘车票价”）
draw_generalization((4.9, 1.0), (6.1, 1.32))
draw_generalization((7.2, 1.0), (6.95, 1.24))
draw_generalization((10.1, 1.0), (8.3, 1.4))

fig.savefig(OUTPUT_PATH, dpi=150, bbox_inches="tight", facecolor="white")
print(f"已生成: {OUTPUT_PATH}")
