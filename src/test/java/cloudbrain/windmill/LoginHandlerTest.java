package cloudbrain.windmill;

import cloudbrain.windmill.Server;
import cloudbrain.windmill.constant.LoginConstant;
import cloudbrain.windmill.utils.AESUtil;
import cloudbrain.windmill.utils.ConfReadUtils;
import cloudbrain.windmill.web.LoginHandler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.MySQLClient;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(VertxUnitRunner.class)
public class LoginHandlerTest extends LoginHandler {
  private static final long TOKEN_TIMEOUT = 1800;
  static SQLClient mysqlclient;
  public static int vertx_port;
  private static JsonObject serverConf;

  @Before
  public void init(TestContext context) throws Exception {
    Vertx vert = Vertx.vertx();


    serverConf = ConfReadUtils.getServerConfByJson("conf.json");
    JsonObject mysqlConf =serverConf.getJsonObject("mysql");


    mysqlclient = MySQLClient.createNonShared(vert, mysqlConf);



  }

  @Test
  public void wxLoginFirstGetUrl(TestContext context) {

  }

  @Test
  public void wxLoginCallBack(TestContext context) {
    Async async = context.async();

    JsonObject result=new JsonObject();

    JsonObject userJsonFromWx = new JsonObject();
    userJsonFromWx.put("openid", "1234").put("nickname", "����").put("sex", "1").put("unionid", "888766").put("country","CN").put("province","�㽭").put("city","����").put("headimgurl","http://wx.qlogo.cn/mmopen/g3MonUZtNHkdmzicIlibx6iaFqAc56vxLSUfpb6n5WKSYVY0ChQKkiaJSgQ1dZuTOgvLLrhJbERQQ4eMsv84eavHiaiceqxibJxCfHe/0");

    String unionid = userJsonFromWx.getString("unionid");
    System.out.println(unionid);

    //��ѯ���ݿ��Ƿ��д��û�
    mysqlclient.query("SELECT * FROM T_USER T WHERE T.`unionid`='" + unionid + "' ", mySqlRes -> {
      List<JsonObject> userFromDB = mySqlRes.result().getRows();
      System.out.println(userFromDB);
      if (userFromDB.size() == 0) {//����
        System.out.println("insert");
        String insertSql = userDAO.getInsertSql(userJsonFromWx);
        System.out.println(insertSql);
        mysqlclient.update(insertSql, insertRes -> {
        });
      } else { //����
        System.out.println("update");
        String updateSql = userDAO.getUpdateSql(userJsonFromWx);
        System.out.println(updateSql);
        mysqlclient.update(updateSql, insertRes -> {
        });

      }

      //����token ��������redis��
      try {
        String beforeToken = userJsonFromWx.getString("unionid") + ":" + String.valueOf(System.currentTimeMillis() + LoginConstant.TOKEN_TIMEOUT) + ":" + (int) (Math.random() * 1000);
       System.out.println("����ǰ��token:"+beforeToken);
        String token = AESUtil.encrypt(beforeToken);
        result.put("success","true").put("token", token).put("message","").put("user",userJsonFromWx);
        System.out.println("���"+result);//uuLG22vjBwqA33JZgAOtunxvJVhv0fs7Ie5oZ5RLLDc=
        async.complete();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }


  @Test
  public void getUserByToken(TestContext context) {
    Async async = context.async();
    JsonObject response = new JsonObject().put("success", "false").put("message", "token�ѹ��������µ�¼");
    try {
      //��ʼ������ֵ
      JsonObject tokenJson = new JsonObject().put("token", "e2FTJ9fcmJafBlNpIP9VTZA69ck1U3EUyv6ioAN7Fz0=");
      //����token
      String token = AESUtil.decrypt(tokenJson.getString("token"));
      System.out.println(token);
      //���token
      String[] tokenArray = token.split(":");

      if (System.currentTimeMillis() <= Long.valueOf(tokenArray[1])) {//�鿴�Ƿ�ʱ
        mysqlclient.query("SELECT T.`HEADIMGURL` headimgurl,T.`SEX` sex,T.`UNIONID` unionid,T.`PROVINCE` provice,T.`COUNTRY` country,T.`CITY` city,T.`NICKNAME` nickname FROM `t_user` t WHERE t.`unionid`='" + tokenArray[0] + "'", res -> {
          if (res.result().getRows().size() > 0) {
            response.put("success","true").put("message","");
            response.put("user", res.result().getRows().get(0));
          } else {
            response.put("success", "false").put("message", "tokenֵ����");
          }
          System.out.println(response);
          async.complete();
        });
      }else {
        System.out.println(response);
        async.complete();
      }

    } catch (Exception e) {
      e.printStackTrace();
      response.put("success", "false").put("message", "��������ȷ��token��Ч");
      System.out.println(response);
    }
  }
}