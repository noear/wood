package org.noear.wood;

import org.noear.wood.utils.EntityUtils;

import java.sql.SQLException;
import java.util.*;


/**
 * Created by noear on 17-6-12.
 * 查询过程访问类（模拟存储过程）
 */
public class DbQueryProcedure extends DbProcedure {

    private Map<String, Object> _paramS2 = new HashMap<>(); //中间变量，不要清掉

    public DbQueryProcedure(DbContext context){
        super(context);
    }


    //---------

    protected DbQueryProcedure sql(String sqlCode) {
        this.commandText = sqlCode;
        this.paramS.clear();
        this._woodKey = null;

        return this;
    }

    private  DbQueryProcedure doSqlItem(String sqlCode){
        this.commandText = sqlCode;
        this.paramS.clear();
        this._woodKey = null;

        return this;
    }


    private void set_do(String param, Object value){
        _paramS2.put(param, value);
    }
    @Override
    public DbProcedure set(String param, Object value) {
        if(param.startsWith("@")){
            set_do(param.substring(1),value);
        }else{
            set_do(param,value);
        }

        return this;
    }

    @Override
    public DbProcedure setMap(Map<String, Object> map) {
        if (map != null) {
            map.forEach((k, v) -> {
                set_do( k, v);
            });
        }
        return this;
    }
    @Override
    public DbProcedure setEntity(Object obj) {
        EntityUtils.fromEntity(obj,(k, v)->{
            set_do(k, v);
        });
        return this;
    }

    //
    //===========================================
    //
    @Override
    public String getWoodKey() {
        return buildWoodKey(_paramS2.values());
    }

    @Override
    protected String getCommandID() {
        tryLazyload();

        return this.commandText;
    }


    @Override
    protected Command getCommand() throws SQLException {
        tryLazyload();

        Command cmd = new Command(this.context);

        cmd.key = getCommandID();

        String sqlTxt = this.commandText;

        build(cmd, sqlTxt);

        runCommandBuiltEvent(cmd);

        return cmd;
    }

    protected void build(Command cmd, String tml) {
        Map<String, String> tmpList = new HashMap<>();
        TmlBlock block = TmlAnalyzer.get(tml, _paramS2);

        tml = block.sql2;

        //1.构建参数
        for (TmlMark tm : block.marks) {
            Object val = _paramS2.get(tm.name);

            if (WoodConfig.isDebug) {
                if (val == null) {
                    throw new RuntimeException("Lack of parameter:" + tm.name);
                }
            }

            if (val instanceof Iterable) { //支持数组型参数
                StringBuilder sb = new StringBuilder();
                for (Object p2 : (Iterable) val) {
                    doSet(tm.name, p2);//对this.paramS进行设值

                    sb.append("?").append(",");
                }

                int len = sb.length();
                if (len > 0) {
                    sb.deleteCharAt(len - 1);
                }

                tmpList.put(tm.mark, sb.toString());
            } else {
                if (tm.mark.startsWith("@")) {
                    doSet(tm.name, val);
                }
            }
        }

        //2.替换部分未编译的参数
        //
        if (tmpList.size() > 0) {
            //按长度倒排KEY
            //
            List<String> keyList = new ArrayList<>(tmpList.keySet());
            Collections.sort(keyList, (o1, o2) -> {
                int len = o2.length() - o1.length();
                if (len > 0) {
                    return 1;
                } else if (len < 0) {
                    return -1;
                } else {
                    return 0;
                }
            });

            for (String key : keyList) {
                tml = tml.replace(key, tmpList.get(key));
            }
        }


        //3.替换schema
        if (WoodConfig.isUsingSchemaExpression) {
            if (tml.indexOf("$") >= 0) {
                if (context.schema() == null) {
                    tml = tml.replace("$.", "");
                } else {
                    tml = tml.replace("$", context.schema());
                }
            }
        }

        //4.为命令赋值
        cmd.paramS = this.paramS;
        cmd.text = tml;
        cmd.attachment = this._paramS2;
    }

    @Override
    public int execute() throws SQLException {
        tryLazyload();

        if(context.isAllowMultiQueries()){
            return super.execute();
        }else {
            int num = 0;
            String[] sqlList = commandText.split(";"); //支持多段SQL执行
            for (String sql : sqlList) {
                if (sql.length() > 10) {
                    doSqlItem(sql);

                    num += super.execute();
                }
            }

            return num;
        }
    }
}
