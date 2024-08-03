package wood_rdb.features;

import org.junit.Test;
import org.noear.wood.BaseMapper;
import org.noear.wood.DbContext;
import org.noear.wood.IDataReader;
import webapp.model.AppxModel;
import wood_rdb.DbUtil;

import java.util.ArrayList;
import java.util.List;

public class _ReaderTest {
    DbContext db2 = DbUtil.db;

    BaseMapper<AppxModel> mapper = db2.mapperBase(AppxModel.class);

    @Test
    public void test_top() {
        assert mapper.selectById(22).app_id == 22;
        System.out.println(db2.lastCommand.text);
    }

    @Test
    public void test_page() throws Exception {
        List<AppxModel> list = new ArrayList<>();

        try (IDataReader<AppxModel> reader = mapper.selectReader(q -> q.orderByAsc(AppxModel::getApp_id).limit(0,10))) {
            AppxModel m;
            do {
                m = reader.next();
                if (m != null) {
                    list.add(m);
                }
            } while (m != null);
        }

        assert list.size() == 10;
        assert list.get(0).app_id == 1;

        System.out.println(db2.lastCommand.text);
    }

    @Test
    public void test_page2() throws Exception {
        List<AppxModel> list = new ArrayList<>();

        try (IDataReader<AppxModel> reader = mapper.selectReader(q -> q.orderByAsc(AppxModel::getApp_id).limit(1, 10))) {
            AppxModel m;
            do {
                m = reader.next();
                if (m != null) {
                    list.add(m);
                }
            } while (m != null);
        }

        assert list.size() == 10;
        assert list.get(0).app_id == 2;

        System.out.println(db2.lastCommand.text);
    }

    @Test
    public void test_page3() throws Exception {
        List<AppxModel> list = new ArrayList<>();

        try (IDataReader<AppxModel> reader = db2.table("appx a")
                .leftJoin("appx_agroup b").onEq("a.agroup_id", "b.agroup_id")
                .orderByAsc("a.app_id")
                .limit(1, 10)
                .selectReader("a.*,b.name agroup_name", AppxModel.class)) {
            AppxModel m;
            do {
                m = reader.next();
                if (m != null) {
                    list.add(m);
                }
            } while (m != null);

        }

        assert list.size() == 10;
        assert list.get(0).app_id == 2;

        System.out.println(db2.lastCommand.text);
    }
}