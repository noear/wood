package wood_rdb.features;

import org.junit.jupiter.api.Test;
import org.noear.wood.DbContext;
import webapp.model.AppxModel;
import wood_rdb.DbUtil;

public class GetTest {
    DbContext db = DbUtil.db;

    @Test
    public void test1() throws Exception {
        assert db.table("appx")
                .whereEq("app_id", 22)
                .selectItem("*", AppxModel.class).app_id == 22;

    }

    @Test
    public void test2() throws Exception {
        assert db.table("appx")
                .whereEq("app_id", 22)
                .selectDataItem("*").count() > 2;

    }

    @Test
    public void test3() throws Exception {
        assert db.table("appx")
                .whereEq("app_id", 22)
                .selectMap("*").size() > 2;

    }


    @Test
    public void test11() throws Exception {
        db.table("appx")
                .whereLt("app_id", 22)
                .selectList("*", AppxModel.class)
                .forEach(m -> m.getApp_id());

    }

    @Test
    public void test12() throws Exception {
        db.table("appx")
                .whereLt("app_id", 22)
                .selectDataList("*")
                .forEach(item -> item.getInt("app_id"));

    }

    @Test
    public void test13() throws Exception {
        db.table("appx")
                .whereLt("app_id", 22)
                .selectMapList("*")
                .forEach(map -> map.get("app_id"));

    }
}
