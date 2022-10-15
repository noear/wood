package benchmark.jmh.wood;

import benchmark.jmh.BaseService;
import benchmark.jmh.DataSourceHelper;
import benchmark.jmh.wood.mapper.WoodSQLUserMapper;
import benchmark.jmh.wood.model.WoodSQLSysUser;
import benchmark.jmh.wood.model.WoodSysCustomer;
import org.noear.wood.BaseMapper;
import org.noear.wood.DbContext;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class WoodService implements BaseService {

    WoodSQLUserMapper userMapper;
    BaseMapper<WoodSysCustomer> customerMapper;
    AtomicInteger idGen = new AtomicInteger(1000);

    DbContext db;

    public void init() {
        DataSource dataSource = DataSourceHelper.ins();

        this.db = new DbContext("user", dataSource);
        this.userMapper = db.mapper(WoodSQLUserMapper.class);
        this.customerMapper = db.mapperBase(WoodSysCustomer.class);
    }


    @Override
    public void addEntity() {
        WoodSQLSysUser sqlSysUser = new WoodSQLSysUser();
        sqlSysUser.setId(idGen.getAndIncrement());
        sqlSysUser.setCode("abc");

       Long tmp =  userMapper.insert(sqlSysUser, false);

        //System.out.println(tmp);
    }


    @Override
    public Object getEntity() {
        Object tmp=  userMapper.selectById(1);

        //System.out.println(tmp);
        return tmp;
    }



    @Override
    public void lambdaQuery() {
        List<WoodSQLSysUser> list = userMapper.selectList(wq -> wq.whereEq(WoodSQLSysUser::getId, 1));
    }

    @Override
    public void executeJdbcSql() {
        WoodSQLSysUser user = userMapper.selectById(1);
    }

    public void executeJdbcSql2() throws SQLException{
        WoodSQLSysUser user = db.sql("select * from sys_user where id = ?",1)
                .getItem(WoodSQLSysUser.class);
    }

    @Override
    public void executeTemplateSql() {
        WoodSQLSysUser user = userMapper.selectTemplateById(1);
    }

    public void executeTemplateSql2() throws SQLException {
        WoodSQLSysUser user = db.call("select * from sys_user where id = @{id}")
                .set("id",1).getItem(WoodSQLSysUser.class);
    }

    @Override
    public void sqlFile() {
        WoodSQLSysUser user = userMapper.userSelect(1);
        //System.out.println(user);
    }

    @Override
    public void one2Many() {

    }


    @Override
    public void pageQuery() {
        List<WoodSQLSysUser> list = userMapper.queryPage("用户一", 1, 5);
        long count = userMapper.selectCount(wq->wq.whereEq("code","用户一"));
        //System.out.println(list);
    }

    @Override
    public void complexMapping() {

    }
}
