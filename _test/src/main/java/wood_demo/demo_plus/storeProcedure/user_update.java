package wood_demo.demo_plus.storeProcedure;

import org.noear.wood.DbStoredProcedure;
import wood_demo.config.DbConfig;

import java.util.Date;

/**
 * Created by noear on 2017/7/22.
 */
public class user_update extends DbStoredProcedure {
    public user_update() {
        super(DbConfig.test);

        lazyload(()->{
            call("user_update");
            set("_userID",  userID);
            set("_city",  city);
            set("_vipTime",  vipTime);
        });
    }

    public long userID;
    public String city;
    public Date vipTime;
}
