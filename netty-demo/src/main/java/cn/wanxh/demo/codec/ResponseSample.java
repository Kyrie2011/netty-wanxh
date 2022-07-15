package cn.wanxh.demo.codec;

/**
 * @program: netty-wanxh
 * @Date: 2022/7/15 23:48
 * @Author: 阿左不是蜗牛
 * @Description: TODO
 */
public class ResponseSample {
    private String code;
    private String data;
    private long timestamp;

    public ResponseSample(String ok, String data, long currentTimeMillis) {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
