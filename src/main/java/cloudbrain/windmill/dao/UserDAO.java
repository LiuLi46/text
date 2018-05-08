package cloudbrain.windmill.dao;

import io.vertx.core.json.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class UserDAO {
  // json-dbName
  private static  Map<String,String> mappingMap;

  static {
    mappingMap=new HashMap<>();
   // mappingMap.put("openid","openid");
    mappingMap.put("nickname","nickname");
    mappingMap.put("sex","sex");
    mappingMap.put("province","province");
    mappingMap.put("city","city");
    mappingMap.put("country","country");
    mappingMap.put("headimgurl","headimgurl");
    mappingMap.put("privilege","privilege");
    mappingMap.put("unionid","unionid");
    mappingMap.put("create_time","create_time");
  }

  public String getInsertSql(JsonObject jsonObject) {
 
    String createTime = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    jsonObject.put("create_time",createTime);

    StringBuffer condition = new StringBuffer();
    StringBuffer values = new StringBuffer();

    Iterator<Map.Entry<String, Object>> iterable = jsonObject.iterator();
    while (iterable.hasNext()) {
      Map.Entry<String, Object> v = iterable.next();

      if(null==mappingMap.get(v.getKey())) continue;

      condition.append("," + v.getKey());
      values.append(",'" + v.getValue()+"'");
    }

    String sql = "insert into `t_user` (" + condition + ") values(" + values + ");";
    return sql.replaceAll("\\(,","(");
  }


  public String getUpdateSql(JsonObject jsonObject) {
    StringBuffer setSql=new StringBuffer();

    Iterator<Map.Entry<String, Object>> iterable = jsonObject.iterator();
    while (iterable.hasNext()) {
      Map.Entry<String, Object> v = iterable.next();

      if(null==mappingMap.get(v.getKey())) continue;

      setSql.append(",").append(v.getKey()).append("=").append(v.getValue());
    }
    String sql="UPDATE  `t_user`  t SET "+setSql.toString().replaceFirst(",","")+" WHERE t.`unionid`='"+jsonObject.getString("unionid")+"'";
    return sql;
  }
}