package wood_rdb.features;

import org.junit.jupiter.api.Test;
import org.noear.wood.DbContext;
import org.noear.wood.wrap.DbType;
import wood_rdb.DbUtil;
import webapp.model.AppxModel;

public class SqlTest {
    DbContext db = DbUtil.db;

    @Test
    public void test1() throws Exception {
        String code = null;
        if(db.getType() == DbType.Oracle){
            code = "select * from \"$\".\"APPX\" where \"app_id\"=?";
        }else{
            code = "select * from $.appx where app_id=?";
        }
        try {
            assert db.sql(code, 32)
                    .getItem(AppxModel.class)
                    .app_id == 32;
        }finally {
            System.out.println(db.lastCommand.text);
        }
        
    }

    @Test
    public void test2() throws Exception {
         db.table("appx").whereGt("app_id",1).limit(1).selectMap("*");
         System.out.println(db.lastCommand.toSqlString());
    }

    @Test
    public void test3() throws Exception {
        db.table("appx").whereGt("app_id",1).selectMap("*");
        System.out.println(db.lastCommand.toSqlString());
    }
}
