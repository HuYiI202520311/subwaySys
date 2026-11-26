package subway;

/**
 * 地铁系统业务异常。
 * 输入不合规（站点不存在、距离参数非法、起终点相同等）时统一抛出，便于调用方捕获处理。
 */
public class SubwayException extends RuntimeException {

    public SubwayException(String message) {
        super(message);
    }

    public SubwayException(String message, Throwable cause) {
        super(message, cause);
    }
}
