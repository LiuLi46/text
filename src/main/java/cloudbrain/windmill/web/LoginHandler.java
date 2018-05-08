package cloudbrain.windmill.web;

import cloudbrain.windmill.StartServer;
import cloudbrain.windmill.dao.UserDAO;
import cloudbrain.windmill.utils.AESUtil;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;

import java.util.List;

public class LoginHandler {
  public UserDAO userDAO=new UserDAO();
  //微信二维码地址
  private static final String QRCODE_URL = "https://open.weixin.qq.com/connect/qrconnect?appid=wxbdc5610cc59c1631&redirect_uri=https%3A%2F%2Fpassport." +
          "yhd.com%2Fwechat%2Fcallback.do&response_type=code&scope=snsapi_login&state=3d6be0a4035d839573b04816624a415e#wechat_redirect";

  private static final String GET_ACCESS_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=APPID&secret=SECRET&code=?1&grant_type=authorization_code";
  //?access_token=ACCESS_TOKEN&openid=OPENID 参数
  private static final String GET_USER_BY_ACCESS_TOKEN_URL = "https://api.weixin.qq.com/sns/userinfo";

  private static final long TOKEN_TIMEOUT = 1800;
  /**
   * 点击微信登陆按钮
   *
   * @param
   * @return 二维码url
   * @author jiwei
   * @time 2018/4/23   10:28
   */
  public void wxLoginFirstGetUrl(RoutingContext context) {
    JsonObject obj = new JsonObject();
    obj.put("qr_code", QRCODE_URL);
    toResponse(context, obj);
  }


  /**
   * 扫二维码回调方法(微信交互部分待测试)
   *
   * @param
   * @return
   * @author jiwei
   * @time 2018/4/23   10:31
   */
  public void wxLoginCallBack(RoutingContext context) {
    //拿到微信返回的参数code
    JsonObject result = new JsonObject();

    HttpServerRequest request = context.request();
    String code = request.getParam("code");
    String state = request.getParam("state");

    //获取url
    String url = getAccessTokenUrl(code);
    
    
    //给微信发送请求获取accessToken
    Future<HttpResponse<Buffer>> getAccessTokenFuture = Future.future();
    Future<HttpResponse<Buffer>> getUserByWxFuture = Future.future();

    //openid 换取微信User信息
    getUserByWxFuture.setHandler(res ->
            getUserByWx(context, result, res)
    );

    getAccessTokenFuture.setHandler(res ->
            getAccessToken(context, result, getUserByWxFuture, res)
    );

    HttpRequest<Buffer> getRequest = StartServer.webClient.get(String.valueOf(StartServer.vertx_port), url);
    getRequest.putHeader("content-type", "application/json;charset=utf-8").send(getAccessTokenFuture.completer());
  }

  private void getAccessToken(RoutingContext context, JsonObject result, Future<HttpResponse<Buffer>> getUserByWxFuture, AsyncResult<HttpResponse<Buffer>> res) {
    JsonObject respnseBody = res.result().bodyAsJsonObject();
    if (respnseBody.containsKey("openid")) {
      String openid = respnseBody.getString("openid");
      String accessToken = respnseBody.getString("access_token");

      //获取微信user信息
      HttpRequest<Buffer> getRequest = StartServer.webClient.get(String.valueOf(StartServer.vertx_port), GET_USER_BY_ACCESS_TOKEN_URL);
      getRequest.putHeader("content-type", "application/json").putHeader("openid", openid).putHeader("access_token", accessToken).send(getUserByWxFuture.completer());
    } else { //扫码失败
      result.put("success", false).put("message", "用户扫码失败");
      toResponse(context, result);
      return;
    }
  }

  private void getUserByWx(RoutingContext context, JsonObject result, AsyncResult<HttpResponse<Buffer>> res) {
    JsonObject userJsonFromWx = res.result().bodyAsJsonObject();
    String unionid = userJsonFromWx.getString("unionid");

    //查询数据库是否有此用户
    StartServer.mysqlclient.query("SELECT * FROM T_USER T WHERE T.`unionid`='" + unionid + "' ", mySqlRes -> {
      List<JsonObject> userFromDB = mySqlRes.result().getRows();
      if (userFromDB.size() == 0) {//新增
        String insertSql = userDAO.getInsertSql(userJsonFromWx);
        StartServer.mysqlclient.update(insertSql, insertRes -> {
        });
      } else { //更新
        String updateSql = userDAO.getUpdateSql(userJsonFromWx);
        StartServer.mysqlclient.update(updateSql, insertRes -> {
        });
      }

      //生成token 并保存至redis中
      try {
        //加密
        String beforeToken = userJsonFromWx.getString("unionid") + ":" + String.valueOf(System.currentTimeMillis());
        String token = AESUtil.encrypt(beforeToken);

        StartServer.redisClient.setex(token, TOKEN_TIMEOUT, userJsonFromWx.toString(), redisRes -> {
          result.put("success",true).put("token", token).put("user",userJsonFromWx);
          toResponse(context, result);
        });
      } catch (Exception e) {
        e.printStackTrace();
        result.put("success", false).put("message", e.getMessage());
        toResponse(context, result);
      }
    });
  }

  
  /**
   * @param code
   * @return
   * 将code加入URL
   */
  private String getAccessTokenUrl(String code) {
    return this.GET_ACCESS_TOKEN_URL.replace("?1", code);
  }
  
  
  /**
   * @return
   * 查看用户状态
   */
  private String getUserState(RoutingContext context) {
    
    return null;
    }

  /**
   * 返回一个json格式的响应
   *
   * @param
   * @return
   * @author jaw
   * @time 2018/4/23   10:28
   */
  private void toResponse(RoutingContext context, JsonObject result) {
    context.response().putHeader("content-type", "application/json")
            .end(result.encodePrettily());
  }
  
  
}
