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
}
