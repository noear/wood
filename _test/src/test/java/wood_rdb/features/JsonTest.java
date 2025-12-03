package wood_rdb.features;

import org.junit.jupiter.api.Test;
import org.noear.snack4.ONode;
import org.noear.wood.DataItem;
import org.noear.wood.DataList;
import org.noear.wood.DbContext;
import wood_rdb.DbUtil;

/**
 * @author noear 2023/3/16 created
 */
public class JsonTest {
    DbContext db = DbUtil.db;

    @Test
    public void test1() throws Exception {
        DataList list = db.table("appx")
                .whereEq("app_id", 1)
                .selectDataList("*");

        System.out.println(db.lastCommand.text);

        String json = ONode.serialize(list);
        System.out.println(json);

        assert json.equals("[{\"app_id\":1,\"app_key\":\"apFsrHfPSj1v6rEB\",\"akey\":\"19526083dd0341cdbedcb875fb6c42db\",\"ugroup_id\":1,\"agroup_id\":1,\"name\":\"iOS-官方\",\"note\":\"测试2\",\"ar_is_setting\":1,\"ar_is_examine\":0,\"ar_examine_ver\":0,\"log_fulltime\":1503676784000}]");
    }

    @Test
    public void test2() throws Exception {
        DataItem item = db.table("appx")
                .whereEq("app_id", 1)
                .selectDataItem("*");

        System.out.println(db.lastCommand.text);

        String json = ONode.serialize(item);
        System.out.println(json);
        assert json.equals("{\"app_id\":1,\"app_key\":\"apFsrHfPSj1v6rEB\",\"akey\":\"19526083dd0341cdbedcb875fb6c42db\",\"ugroup_id\":1,\"agroup_id\":1,\"name\":\"iOS-官方\",\"note\":\"测试2\",\"ar_is_setting\":1,\"ar_is_examine\":0,\"ar_examine_ver\":0,\"log_fulltime\":1503676784000}");
    }
}
