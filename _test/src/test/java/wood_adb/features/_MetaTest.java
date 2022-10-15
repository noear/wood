package wood_adb.features;

import org.junit.Test;
import org.noear.wood.DbContext;
import wood_adb.DbUtil;

public class _MetaTest {
    DbContext db = DbUtil.db;

    @Test
    public void test1() throws Exception{

        db.getMetaData().getTableAll().forEach(tw->{
            System.out.println("Table: "+tw.getName());
            tw.getColumns().forEach(cw->{
                System.out.print(cw.getName()+";");
            });
            System.out.println("");
        });

        System.out.println(db.getMetaData().getTableAll().size());

        assert  db.getMetaData().getTableAll().size() > 0;
    }
}
