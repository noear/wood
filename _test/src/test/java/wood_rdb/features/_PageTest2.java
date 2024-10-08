package wood_rdb.features;

import org.junit.jupiter.api.Test;
import org.noear.wood.BaseMapper;
import org.noear.wood.DbContext;
import org.noear.wood.IPage;
import webapp.model.AppxModel;
import wood_rdb.DbUtil;

public class _PageTest2 {
    DbContext db2 = DbUtil.db;

    BaseMapper<AppxModel> mapper = db2.mapperBase(AppxModel.class);

    @Test
    public void test_top(){
        assert  mapper.selectById(22).app_id == 22;
        System.out.println(db2.lastCommand.text);
    }

    @Test
    public void test_page() throws Exception {

        IPage<AppxModel> list = mapper.selectPage(0, 10, q->q.orderByAsc(AppxModel::getApp_id));
        assert list.getList().size() == 10;
        assert list.getList().get(0).app_id == 1;
        assert list.getTotal() > 10;
        assert list.getPages() > 0;
        assert list.getSize() == 10;

        System.out.println(db2.lastCommand.text);
    }

    @Test
    public void test_page2() throws Exception{
        IPage<AppxModel> list =  mapper.selectPage(1,10,q->q.orderByAsc(AppxModel::getApp_id));
        assert  list.getList().size() == 10;
        assert list.getList().get(0).app_id == 2;
        assert list.getTotal() > 10;
        assert list.getPages() > 0;
        assert list.getSize() == 10;

        System.out.println(db2.lastCommand.text);
    }

    @Test
    public void test_page3() throws Exception {
        IPage<AppxModel> list = db2.table("appx a")
                .leftJoin("appx_agroup b").onEq("a.agroup_id", "b.agroup_id")
                .orderByAsc("a.app_id")
                .limit(1, 10)
                .selectPage("a.*,b.name agroup_name", AppxModel.class);

        assert  list.getList().size() == 10;
        assert list.getList().get(0).app_id == 2;
        assert list.getTotal() > 10;
        assert list.getPages() > 0;
        assert list.getSize() == 10;

        System.out.println(db2.lastCommand.text);
    }

    @Test
    public void test_page4() throws Exception{
        IPage<AppxModel> list = db2.table("appx")
                .whereEq("app_id",2)
                .orderByAsc("app_id")
                .selectPage("*", AppxModel.class);

        assert  list.getList().size() == 1;
        assert list.getList().get(0).app_id == 2;

        System.out.println(db2.lastCommand.text);
    }
}
