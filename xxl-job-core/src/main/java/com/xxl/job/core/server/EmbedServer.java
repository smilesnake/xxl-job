package com.xxl.job.core.server;

import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.impl.ExecutorBizImpl;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.BizUriEnum;
import com.xxl.job.core.exception.XxlJobException;
import com.xxl.job.core.thread.ExecutorRegistryThread;
import com.xxl.job.core.util.GsonTool;
import com.xxl.job.core.util.ThrowableUtil;
import com.xxl.job.core.util.XxlJobRemotingUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

/**
 * 内嵌的服务器
 * <p>Copy from : https://github.com/xuxueli/xxl-rpc</p>
 *
 * @author xuxueli 2020-04-11 21:25
 */
@Slf4j
public class EmbedServer {

  /**
   * 业务执行器
   */
  private ExecutorBiz executorBiz;
  /**
   * 内嵌服务线程
   */
  private Thread thread;

  /**
   * 启动内置的服务.
   *
   * @param address     地址
   * @param port        端口
   * @param appName     执行器名称
   * @param accessToken 配置的accessToken
   */
  public void start(final String address, final int port, final String appName,
      final String accessToken) {
    executorBiz = new ExecutorBizImpl();
    thread = new Thread(() -> {

      // 负责接收请求，
      EventLoopGroup bossGroup = new NioEventLoopGroup();
      // 负责处理请求
      EventLoopGroup workerGroup = new NioEventLoopGroup();
      // 负责处理业务
      ThreadPoolExecutor bizThreadPool = new ThreadPoolExecutor(0, 200, 60L, TimeUnit.SECONDS,
          new LinkedBlockingQueue<>(2000),
          r -> new Thread(r, "xxl-rpc, EmbedServer bizThreadPool-" + r.hashCode()),
          (r, executor) -> {
            //拒绝策略为没有资源直接抛出（内嵌业务线程资源出尽）
            throw new XxlJobException("xxl-job, EmbedServer bizThreadPool is EXHAUSTED!");
          });
      try {
        // 启动服务
        ServerBootstrap bootstrap = new ServerBootstrap();

        bootstrap.group(bossGroup, workerGroup);
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          public void initChannel(SocketChannel channel) {
            channel.pipeline()
                //每30秒进行一次心跳检测,检测3次，如果空闲就关闭
                .addLast(
                    new IdleStateHandler(0, 0, TimeUnit.SECONDS.toSeconds(90), TimeUnit.SECONDS))
                //http服务编解码器
                .addLast(new HttpServerCodec())
                //一个HTTP请求最少也会在HttpRequestDecoder里分成两次往后传递，第一次是消息行和消息头，第二次是消息体，哪怕没有消息体，也会传一个空消息体。
                // 如果发送的消息体比较大的话，可能还会分成好几个消息体来处理，往后传递多次，这样使得我们后续的处理器可能要写多个逻辑判断，比较麻烦.
                // 使用HttpObjectAggregator能把消息都整合成一个完整的，再往后传递
                //这里合并的消息最大内容长度为5M
                .addLast(
                    new HttpObjectAggregator(5 * 1024 * 1024))  // merge request & reponse to FULL
                .addLast(new EmbedHttpServerHandler(executorBiz, accessToken, bizThreadPool));
          }
        });
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

        // 绑定服务端口
        ChannelFuture future = bootstrap.bind(port).sync();

        log.info(">>>>>>>>>>> xxl-job remoting server start success, nettype = {}, port = {}",
            EmbedServer.class, port);

        // 开始注册
        startRegistry(appName, address);

        // 等待服务端监听端口关闭.
        future.channel().closeFuture().sync();

      } catch (InterruptedException e) {
        log.info(">>>>>>>>>>> xxl-job remoting server stop.");
        Thread.currentThread().interrupt();
      } catch (Exception e) {
        log.error(">>>>>>>>>>> xxl-job remoting server error.", e);
      } finally {
        // 优雅退出，释放 NIO 线程组
        try {
          workerGroup.shutdownGracefully();
          bossGroup.shutdownGracefully();
        } catch (Exception e) {
          log.error(e.getMessage(), e);
        }
      }

    });
    // daemon, service jvm, user thread leave >>> daemon leave >>> jvm leave
    // 守护进程，服务jvm，用户线程离开 >>> 守护进程离开 >>> jvm离开
    thread.setDaemon(true);
    thread.start();
  }

  /**
   * 销毁线程.
   */
  public void stop() {
    // destroy server thread
    if (thread != null && thread.isAlive()) {
      thread.interrupt();
    }

    // 停止注册
    stopRegistry();
    log.info(">>>>>>>>>>> xxl-job remoting server destroy success.");
  }

// ---------------------- registry ----------------------

  /**
   * netty的http处理器
   * <p>
   * Copy from : https://github.com/xuxueli/xxl-rpc
   *
   * @author xuxueli 2015-11-24 22:25:15
   */
  @Slf4j
  @AllArgsConstructor
  public static class EmbedHttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    /**
     * 业务执行器
     */
    private ExecutorBiz executorBiz;
    /**
     * 执行器通讯TOKEN
     */
    private String accessToken;
    /**
     * 业务线程id
     */
    private ThreadPoolExecutor bizThreadPool;

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, FullHttpRequest msg) {

      // 解析请求
      String requestData = msg.content().toString(CharsetUtil.UTF_8);
      //得到请求的URI,即请求URL除地址的部分，如：192.168.0.1：9999/beat,那么uri为/beat
      String uri = msg.uri();
      //请求方式
      HttpMethod httpMethod = msg.method();
      // 是否保活
      boolean keepAlive = HttpUtil.isKeepAlive(msg);
      //得到执行器通讯TOKEN
      String accessTokenReq = msg.headers().get(XxlJobRemotingUtil.XXL_JOB_ACCESS_TOKEN);

      // 调度
      bizThreadPool.execute(() -> {
        // 去调度
        ReturnT<?> responseObj = process(httpMethod, uri, requestData, accessTokenReq);

        // 结果转JSON
        String responseJson = GsonTool.toJson(responseObj);

        // write response
        writeResponse(ctx, keepAlive, responseJson);
      });
    }

    /**
     * 调度过程.
     *
     * @param httpMethod     方法类型
     * @param uri            uri
     * @param requestData    请求数据
     * @param accessTokenReq 请求通讯TOKEN
     * @return 对象通用类
     */
    private ReturnT<?> process(HttpMethod httpMethod, String uri, String requestData,
        String accessTokenReq) {

      // 验证
      if (HttpMethod.POST != httpMethod) {
        return new ReturnT<>(ReturnT.FAIL_CODE, "invalid request, HttpMethod not support.");
      }
      if (StringUtils.isBlank(uri)) {
        return new ReturnT<>(ReturnT.FAIL_CODE, "invalid request, uri-mapping empty.");
      }
      if (StringUtils.isNotBlank(accessToken) && !accessToken.equals(accessTokenReq)) {
        return new ReturnT<>(ReturnT.FAIL_CODE, "The access token is wrong.");
      }

      try {
        // 服务映射
        BizUriEnum bizUri = BizUriEnum.match(uri);
        if (bizUri == null) {
          return new ReturnT<>(ReturnT.FAIL_CODE,
              "invalid request, uri-mapping(" + uri + ") not found.");
        }
        return bizUri.mapper(executorBiz, requestData);
      } catch (Exception e) {
        log.error(e.getMessage(), e);
        return new ReturnT<>(ReturnT.FAIL_CODE, "request error:" + ThrowableUtil.toString(e));
      }
    }

    /**
     * 写入响应
     */
    private void writeResponse(ChannelHandlerContext ctx, boolean keepAlive, String responseJson) {
      // write response
      //响应JSON内容
      ByteBuf content = Unpooled.copiedBuffer(responseJson, CharsetUtil.UTF_8);
      FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
          HttpResponseStatus.OK, content);
      response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=UTF-8");
      response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
      if (keepAlive) {
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
      }
      //写入到Channel
      ctx.writeAndFlush(response);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
      //读取了完整的数据
      ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      // 异常处理
      log.error(">>>>>>>>>>> xxl-job provider netty_http server caught exception", cause);
      ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
      // 心跳检测
      if (evt instanceof IdleStateEvent) {
        ctx.channel().close();      // beat 3N, close if idle
        log.debug(">>>>>>>>>>> xxl-job provider netty_http server close an idle channel.");
      } else {
        super.userEventTriggered(ctx, evt);
      }
    }

  }

  // ---------------------- registry ----------------------

  /**
   * 注册.
   *
   * @param appName 应用名称
   * @param address 地址（ip:port）
   */
  private void startRegistry(final String appName, final String address) {
    // start registry
    ExecutorRegistryThread.getInstance().start(appName, address);
  }

  /**
   * 停止注册.
   */
  private void stopRegistry() {
    // stop registry
    ExecutorRegistryThread.getInstance().toStop();
  }


}
