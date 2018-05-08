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
  //΢�Ŷ�ά���ַ
  private static final String QRCODE_URL = "https://open.weixin.qq.com/connect/qrconnect?appid=wxbdc5610cc59c1631&redirect_uri=https%3A%2F%2Fpassport." +
          "yhd.com%2Fwechat%2Fcallback.do&response_type=code&scope=snsapi_login&state=3d6be0a4035d839573b04816624a415e#wechat_redirect";

  private static final String GET_ACCESS_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=APPID&secret=SECRET&code=?1&grant_type=authorization_code";
  //?access_token=ACCESS_TOKEN&openid=OPENID ����
  private static final String GET_USER_BY_ACCESS_TOKEN_URL = "https://api.weixin.qq.com/sns/userinfo";

  private static final long TOKEN_TIMEOUT = 1800;
  /**
   * ���΢�ŵ�½��ť
   *
   * @param
   * @return ��ά��url
   * @author jiwei
   * @time 2018/4/23   10:28
   */
  public void wxLoginFirstGetUrl(RoutingContext context) {
    JsonObject obj = new JsonObject();
    obj.put("qr_code", QRCODE_URL);
    toResponse(context, obj);
  }


  /**
   * ɨ��ά��ص�����(΢�Ž������ִ�����)
   *
   * @param
   * @return
   * @author jiwei
   * @time 2018/4/23   10:31
   */
  public void wxLoginCallBack(RoutingContext context) {
    //�õ�΢�ŷ��صĲ���code
    JsonObject result = new JsonObject();

    HttpServerRequest request = context.request();
    String code = request.getParam("code");
    String state = request.getParam("state");

    //��ȡurl
    String url = getAccessTokenUrl(code);
    
    
    //��΢�ŷ��������ȡaccessToken
    Future<HttpResponse<Buffer>> getAccessTokenFuture = Future.future();
    Future<HttpResponse<Buffer>> getUserByWxFuture = Future.future();

    //openid ��ȡ΢��User��Ϣ
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

      //��ȡ΢��user��Ϣ
      HttpRequest<Buffer> getRequest = StartServer.webClient.get(String.valueOf(StartServer.vertx_port), GET_USER_BY_ACCESS_TOKEN_URL);
      getRequest.putHeader("content-type", "application/json").putHeader("openid", openid).putHeader("access_token", accessToken).send(getUserByWxFuture.completer());
    } else { //ɨ��ʧ��
      result.put("success", false).put("message", "�û�ɨ��ʧ��");
      toResponse(context, result);
      return;
    }
  }

  private void getUserByWx(RoutingContext context, JsonObject result, AsyncResult<HttpResponse<Buffer>> res) {
    JsonObject userJsonFromWx = res.result().bodyAsJsonObject();
    String unionid = userJsonFromWx.getString("unionid");

    //��ѯ���ݿ��Ƿ��д��û�
    StartServer.mysqlclient.query("SELECT * FROM T_USER T WHERE T.`unionid`='" + unionid + "' ", mySqlRes -> {
      List<JsonObject> userFromDB = mySqlRes.result().getRows();
      if (userFromDB.size() == 0) {//����
        String insertSql = userDAO.getInsertSql(userJsonFromWx);
        StartServer.mysqlclient.update(insertSql, insertRes -> {
        });
      } else { //����
        String updateSql = userDAO.getUpdateSql(userJsonFromWx);
        StartServer.mysqlclient.update(updateSql, insertRes -> {
        });
      }

      //����token ��������redis��
      try {
        //����
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
   * ��code����URL
   */
  private String getAccessTokenUrl(String code) {
    return this.GET_ACCESS_TOKEN_URL.replace("?1", code);
  }
  
  
  /**
   * @return
   * �鿴�û�״̬
   */
  private String getUserState(RoutingContext context) {
    
    return null;
    }

  /**
   * ����һ��json��ʽ����Ӧ
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
