package cloudbrain.windmill;

import cloudbrain.windmill.constant.UrlConstant;
import cloudbrain.windmill.utils.ConfReadUtils;
import cloudbrain.windmill.web.LoginHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.MySQLClient;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class StartServer extends AbstractVerticle {
  public static int vertx_port;
  private static JsonObject serverConf;
  public static SQLClient mysqlclient;
  public static RedisClient redisClient;

  public static WebClient webClient;
  private static final Logger logger = LogManager.getLogger();

  //初始化Handler
  protected static final LoginHandler loginHandler = new LoginHandler();


  public static void main(String[] args) {
    Vertx vert = Vertx.vertx();
    vert.deployVerticle(Server.class, new DeploymentOptions());
  }

  @Override
  public void start() throws Exception {
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    //初始化配置
    initConfig(router);

    //初始化映射
    initMapping(router);

    HttpServer httpServer = vertx.createHttpServer();
    httpServer.requestHandler(router::accept).listen(vertx_port);
  }


  /**
   * 初始化映射关系
   *
   * @param
   * @return
   * @author jiwei
   * @time 2018/4/23   10:16
   */
  private void initMapping(Router router) {
    router.get(UrlConstant.WX_LOGIN_URL).handler(loginHandler::wxLoginFirstGetUrl);
    router.get(UrlConstant.WX_LOGIN_CALL_BACK).handler(loginHandler::wxLoginCallBack);
  }

  /**
   * 初始化配置
   *
   * @param
   * @return
   * @author jiwei
   * @time 2018/4/20   11:40
   */
  private void initConfig(Router router) {
    try {
      serverConf = ConfReadUtils.getServerConfByJson("conf.json");
    } catch (IOException e) {
      e.printStackTrace();
      logger.error(e);
    }
    vertx_port = serverConf.getJsonObject("vertx").getInteger("port");

    initMySqlConfig(router);
    initRedis(router);
    initWebClient(router);
  }



  /**
   * 初始化Mysql配置
   *
   * @param
   * @return
   * @author jiwei
   * @time 2018/4/20   11:40
   */
  private void initMySqlConfig(Router router) {
    JsonObject mysqlConf = serverConf.getJsonObject("mysql");
    mysqlclient = MySQLClient.createNonShared(vertx, mysqlConf);

    Handler<RoutingContext> mysqlHandler = routingContext
            -> mysqlclient.getConnection(res -> {
              if (res.failed()) {
                routingContext.fail(res.cause());
              } else {
                SQLConnection conn = res.result();
                routingContext.put("mysqlconn", conn);
                routingContext.addHeadersEndHandler(done
                        -> conn.close(v -> {
                }));
                routingContext.next();
              }
            }
    );

    Handler<RoutingContext> mysqlfailHandler = routingContext -> {
      SQLConnection conn = routingContext.get("mysqlconn");
      if (conn != null) {
        conn.close(v -> {
        });
      }
    };
    router.route("/").handler(mysqlHandler)
            .handler(TimeoutHandler.create(3 * 1000))
            .failureHandler(mysqlfailHandler);
  }

  /**
   * 初始化web客户端
   *
   * @param
   * @return
   * @author jiwei
   * @time 2018/4/23   10:37
   */
  private void initWebClient(Router router) {
    webClient = WebClient.create(vertx);
  }
  /**
   * 初始化Redis配置
   *
   * @param
   * @return
   * @author jiwei
   * @time 2018/4/20   14:44
   */
  private void initRedis(Router router) {
    RedisOptions config = new RedisOptions()
            .setHost("127.0.0.1");
    redisClient = RedisClient.create(vertx, config);
  }
}
