package cloudbrain.windmill;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.MySQLClient;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class Server extends AbstractVerticle {
  private static JsonObject serverConf;
  protected SQLClient mysqlclient;
  private static final Logger logger = LogManager.getLogger();


  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(Server.class, new DeploymentOptions());
  }


  @Override
  public void start() throws Exception {
    serverConf = getServerConf("conf.json");
    int vertx_port = serverConf.getJsonObject("vertx").getInteger("port");
    JsonObject mysqlConf = serverConf.getJsonObject("mysql");
    mysqlclient = MySQLClient.createNonShared(vertx, mysqlConf);

    Router router = Router.router(vertx);
    router.route().handler(LoggerHandler.create(LoggerFormat.DEFAULT));
    router.route().handler(BodyHandler.create());

    Handler<RoutingContext> mysqlHandler = routingContext
      -> mysqlclient.getConnection(res -> {
          if (res.failed()) {
            routingContext.fail(res.cause());
          } else {
            SQLConnection conn = res.result();
            routingContext.put("mysqlconn", conn);
            routingContext.addHeadersEndHandler(done
              -> conn.close(v -> { }));
            routingContext.next();
          }
        }
    );

    Handler<RoutingContext> mysqlfailHandler = routingContext -> {
        SQLConnection conn = routingContext.get("mysqlconn");
        if (conn != null) {
          conn.close(v -> {});
        }
      };

    router.route("/*").handler(mysqlHandler)
      .handler(TimeoutHandler.create(3*1000))
      .failureHandler(mysqlfailHandler);

    //register uri handlers

    //start http server
    vertx.createHttpServer().requestHandler(router::accept)
      .listen(vertx_port);
    logger.info("server started");
  }


  /**
   * read server conf and return as jsonobject
   */
  private JsonObject getServerConf(String filename) {
    Path path = Paths.get(filename);
    try {
      String data = new String(Files.readAllBytes(path));
      return new JsonObject(data);
    }
    catch (Exception e) {
      logger.error(e);
    }

    return null;
  }


}
