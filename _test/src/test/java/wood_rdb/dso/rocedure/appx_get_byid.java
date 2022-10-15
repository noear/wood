package wood_rdb.dso.rocedure;

import org.noear.wood.DbContext;
import org.noear.wood.DbStoredProcedure;

public class appx_get_byid extends DbStoredProcedure {
    public appx_get_byid(DbContext context) {
        super(context);

        lazyload(()->{
            call("appx_get_byid");
            set("_app_id",app_id);
        });
    }

    public int app_id;
}
