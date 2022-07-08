package cn.wanxh.reactor;

/**
 * @program: netty-wanxh
 * @Date: 2022/7/8 0:20
 * @Author: 阿左不是蜗牛
 * @Description: 多Reactor主从模型，为了匹配CPU 和 IO的速率，可设计多个Reactor
 *  主Reactor 负责监听连接，然后将连接注册到从Reactor，将I/O转移至从Reactor
 *  从Reactor 负责通道IO的读写，处理器可选择单线程或线程池
 */
public class MultiReactor {
    /**
     * 补充：
     *     多线程版本将业务和IO操作进行分离，Reactor线程只关注事件分发和实际的IO操作，
     *     业务处理如协议的编解码都分配给线程池处理。可能会有这样的情况发生，业务处理很快，
     *     Reactor线程大部分的都在处理IO，导致了CPU闲置，降低了响应速度。
     */

}
