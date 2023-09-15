package wood_demo.demo.cache;

import org.noear.wood.DbContext;
import org.noear.wood.DbProcedure;
import org.noear.wood.cache.ICacheService;
import org.noear.wood.cache.ICacheServiceEx;
import wood_demo.config.DbConfig;
import wood_demo.demo.model.UserInfoModel;

import java.sql.SQLException;

/**
 * Created by noear on 2017/7/22.
 */
public class Cache1Demo {
    //普通的缓存控制
    public static void demo_cache() throws SQLException{
        DbContext db = DbConfig.pc_bcf;
        ICacheServiceEx cache = null;

        db.call("user_get").set("xxx", 1)
                .caching(cache)
                .usingCache(60 * 1000)
                .getItem(UserInfoModel.class);

    }

    public static void demo_cache3() throws SQLException{
        DbContext db = DbConfig.pc_bcf;
        ICacheServiceEx cache = null;

        //1.缓存并添加简易标签
        db.call("user_get").set("xxx", 1)
                .caching(cache)
                .cacheTag("user_"+ 1)
                .usingCache(60 * 1000)
                .getItem(UserInfoModel.class);


        //2.1.可根据标签清除缓存
        cache.clear("user_" + 1);

        //2.2.可根据标签更新缓存
        cache.update("user_" + 1, UserInfoModel.class, (UserInfoModel m)->{
            m.name = "xxx";
            return m;
        });

    }

    //带条件判断的缓存控制
    public static void demo_cache_condition() throws SQLException {
        DbContext db = DbConfig.pc_bcf;
        ICacheService cache = null;

        db.call("user_get").set("xxx", 1)
                .caching(cache)
                .usingCache(60 * 100)
                .getItem(UserInfoModel.class, (cu, t) -> {
                    if (t.user_id == 0) {
                        cu.usingCache(false);
                    }
                });
    }

    //分割参数值，并实现对应分别缓存
    public static void demo_cache_split() throws SQLException{
        DbContext db = DbConfig.pc_bcf;
        ICacheService cache = null;


        DbProcedure sp = db.call("user_get")
                .set("user_ids", "1,2,3,4,5,6");

        sp.caching(cache).usingCache(60 * 100);//与上面分开写（不然,返回类型需要转换）

        sp.getDataList();
    }
}
