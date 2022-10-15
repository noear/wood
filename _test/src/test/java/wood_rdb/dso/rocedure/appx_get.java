package wood_rdb.dso.rocedure;

import org.noear.wood.DbContext;
import org.noear.wood.DbQueryProcedure;
import org.noear.wood.wrap.DbType;

public class appx_get extends DbQueryProcedure {
    public appx_get(DbContext context) {
        super(context);
        lazyload(()->{
            if(context.getType() == DbType.Oracle){
                sql("select * from \"$\".\"APPX\" where \"app_id\"=@{id}");
            }else{
                sql("select * from appx where app_id=@{id}");
            }


            set("id",app_id);
        });
    }

    public int app_id;
}
