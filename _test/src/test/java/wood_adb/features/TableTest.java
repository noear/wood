package wood_adb.features;

import org.junit.Test;
import org.noear.wood.DbContext;
import webapp.model.AppxModel;
import wood_adb.DbUtil;

public class TableTest {
    DbContext db = DbUtil.db;


    @Test
    public void test1() throws Exception {
        assert db.table("appx")
                .whereEq("app_id", 22)
                .selectItem("*", AppxModel.class).app_id == 22;

        System.out.println(db.lastCommand.text);
    }

    @Test
    public void test12() throws Exception {
        assert db.table("appx")
                .whereEq("app_id", null)
                .selectItem("*", AppxModel.class).app_id == null;

        System.out.println(db.lastCommand.text);
    }
}
