package wood_rdb.features;

import org.junit.jupiter.api.Test;
import org.noear.wood.DbContext;
import wood_rdb.DbUtil;
import webapp.model.AppxModel;

import java.sql.SQLException;


public class TableJoinTest {
    DbContext db = DbUtil.db;

    @Test
    public void join_select() throws Exception {
        AppxModel m = db.table("appx a")
                .innerJoin("appx_agroup g").onEq("a.agroup_id", "g.agroup_id")
                .whereEq("a.app_id", 22)
                .selectItem("a.*,g.name agroup_name", AppxModel.class);

        assert m.app_id == 22;

        System.out.println(db.lastCommand.text);
    }

    @Test
    public void join_update() throws SQLException {
        db.table("#appx a, appx_agroup g")
                .set("a.note", "测试2")
                .where("a.agroup_id = g.agroup_id").andEq("a.app_id", 1)
                .update();

        System.out.print(db.lastCommand.text);
    }

    @Test
    public void join_update2() throws SQLException {
        db.table("appx a")
                .innerJoin("appx_agroup g").onEq("a.agroup_id", "g.agroup_id")
                .set("a.note", "测试2")
                .whereEq("a.app_id", 1)
                .update();

        System.out.print(db.lastCommand.text);
    }
}
