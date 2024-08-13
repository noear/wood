package wood_rdb.features;

import org.junit.jupiter.api.Test;
import org.noear.wood.BaseMapper;
import org.noear.wood.DbContext;
import org.noear.wood.WoodConfig;
import webapp.model.AppxModel;
import wood_rdb.DbUtil;

import java.sql.SQLException;

/**
 * @author noear 2021/8/27 created
 */
public class NullTest {
    DbContext db2 = DbUtil.db;
    BaseMapper<AppxModel> mapper = db2.mapperBase(AppxModel.class);

    @Test
    public void test() throws SQLException {
        AppxModel temp = db2.table("appx")
                .whereEq("app_id", Integer.MAX_VALUE)
                .selectItem("*", AppxModel.class);

        assert temp != null;

        WoodConfig.isSelectItemEmptyAsNull = true;

        AppxModel temp2 = db2.table("appx")
                .whereEq("app_id", Integer.MAX_VALUE)
                .selectItem("*", AppxModel.class);

        WoodConfig.isSelectItemEmptyAsNull = false;

        assert temp2 == null;
    }


    @Test
    public void test2() throws SQLException {
        AppxModel temp = mapper.selectById(Integer.MAX_VALUE);

        assert temp != null;

        WoodConfig.isSelectItemEmptyAsNull = true;

        AppxModel temp2 = mapper.selectById(Integer.MAX_VALUE);

        WoodConfig.isSelectItemEmptyAsNull = false;

        assert temp2 == null;
    }

}
