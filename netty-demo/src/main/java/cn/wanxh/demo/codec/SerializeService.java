package cn.wanxh.demo.codec;

/**
 * @program: netty-wanxh
 * @Date: 2022/7/14 23:29
 * @Author: 阿左不是蜗牛
 * @Description: TODO
 */
interface SerializeService {
    Object deserialize(byte[] data);
}
