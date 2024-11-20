package wood_rdb.features;

import org.junit.jupiter.api.Test;
import org.noear.wood.DbContext;
import wood_rdb.DbUtil;

public class _MetaTest {
    DbContext db = DbUtil.db;

    @Test
    public void test1() throws Exception {

        db.getMetaData().getTableAll().forEach(tw -> {
            System.out.println("Table: " + tw.getName());
            tw.getColumns().forEach(cw -> {
                System.out.print(cw.getName() + ";");
            });
            System.out.println("");
        });

        System.out.println(db.getMetaData().getTableAll().size());

        assert db.getMetaData().getTableAll().size() > 0;
    }

    @Test
    public void test2() throws Exception {

        db.getMetaData().getTableAll().forEach(tw -> {
            System.out.println("Table: " + tw.getName());
            tw.getColumns().forEach(cw -> {
                System.out.print(cw.getName() + ";");
            });
            System.out.println("");
        });

        System.out.println(db.getMetaData().getTableAll().size());

        assert db.getMetaData().getTableAll().size() > 0;
        int old_size = db.getMetaData().getTableAll().size();


        //刷新表
        db.getMetaData().refresh();

        db.getMetaData().getTableAll().forEach(tw -> {
            System.out.println("Table2: " + tw.getName());
            tw.getColumns().forEach(cw -> {
                System.out.print(cw.getName() + ";");
            });
            System.out.println("");
        });

        System.out.println(db.getMetaData().getTableAll().size());

        assert db.getMetaData().getTableAll().size() > 0;
        assert old_size == db.getMetaData().getTableAll().size();
    }

    @Test
    public void test3() {
        assert db.getMetaData().getTable("test")
                .refresh()
                .hasColumn("v1");

        assert db.getMetaData().getTable("test")
                .refresh()
                .hasColumn("v1");

        assert db.getMetaData().getTable("test")
                .refresh()
                .hasColumn("v1");

        assert db.getMetaData().getTable("test")
                .refresh()
                .hasColumn("v1");
    }
}
