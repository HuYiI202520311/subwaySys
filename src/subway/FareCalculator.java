package subway;

/**
 * 票价计算（功能6、功能7）。
 *
 * 武汉地铁现行按里程分段计价规则：
 *   起步价 2 元，可乘坐 4 公里（含）；
 *   4-12 公里（含），每乘坐 4 公里加 1 元；
 *   12-24 公里（含），每乘坐 6 公里加 1 元；
 *   24 公里以上，每乘坐 8 公里加 1 元。
 *
 * 武汉通刷卡按单程票价的 9 折计费；
 * 定期票在有效期内不限次数乘坐轨道交通，单次乘车费用为 0 元，
 * 票本身售价：1日票 18 元/张，3日票 45 元/张，7日票 90 元/张。
 *
 * 金额内部统一以“角”（0.1元）为单位的整数计算，避免浮点误差。
 */
public final class FareCalculator {

    /** 票种 */
    public enum TicketType {

        SINGLE("普通单程票"),
        WUHAN_PASS("武汉通"),
        DAY_PASS_1("1日票"),
        DAY_PASS_3("3日票"),
        DAY_PASS_7("7日票");

        private final String displayName;

        TicketType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /** 里程边界比较容差：区间里程为三位小数，累加误差远小于该值 */
    private static final double EPS = 1e-6;

    private FareCalculator() {
    }

    /**
     * 普通单程票票价（元）。
     */
    public static int singleTicketFareYuan(double distanceKm) {
        validateDistance(distanceKm);
        if (distanceKm <= 4.0 + EPS) {
            return 2;
        }
        if (distanceKm <= 12.0 + EPS) {
            return 2 + tierCount(distanceKm - 4.0, 4.0);
        }
        if (distanceKm <= 24.0 + EPS) {
            return 4 + tierCount(distanceKm - 12.0, 6.0);
        }
        return 6 + tierCount(distanceKm - 24.0, 8.0);
    }

    /**
     * 某票种下本次乘车的费用（角）。
     * 武汉通 9 折：票价(元) × 10 × 0.9 = 票价 × 9（角），结果为精确整数；
     * 定期票有效期内不限次数，单次乘车费用为 0。
     */
    public static int rideFareJiao(double distanceKm, TicketType ticketType) {
        int singleFareYuan = singleTicketFareYuan(distanceKm);
        return switch (ticketType) {
            case SINGLE -> singleFareYuan * 10;
            case WUHAN_PASS -> singleFareYuan * 9;
            case DAY_PASS_1, DAY_PASS_3, DAY_PASS_7 -> 0;
        };
    }

    /** 定期票购票价格（元/张）；非定期票票种返回 0 */
    public static int ticketPriceYuan(TicketType ticketType) {
        return switch (ticketType) {
            case DAY_PASS_1 -> 18;
            case DAY_PASS_3 -> 45;
            case DAY_PASS_7 -> 90;
            case SINGLE, WUHAN_PASS -> 0;
        };
    }

    /** 把以角为单位的金额格式化为元的字符串，如 27 -> “2.7元”，20 -> “2元” */
    public static String formatFare(int fareJiao) {
        if (fareJiao % 10 == 0) {
            return (fareJiao / 10) + "元";
        }
        return String.format("%.1f元", fareJiao / 10.0);
    }

    /** 超出基础里程后，按 stepKm 一档向上取整的档数 */
    private static int tierCount(double excessKm, double stepKm) {
        // 减去微小容差，防止浮点累加误差把恰好落在边界上的里程多算一档
        return (int) Math.ceil((excessKm - EPS) / stepKm);
    }

    private static void validateDistance(double distanceKm) {
        if (Double.isNaN(distanceKm) || Double.isInfinite(distanceKm) || distanceKm < 0) {
            throw new SubwayException("里程数值不合法: " + distanceKm);
        }
    }
}
