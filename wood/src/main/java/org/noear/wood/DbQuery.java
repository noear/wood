package org.noear.wood;

/**
 * Created by noear on 14-9-5.
 *
 * 查询语句访问类
 *
 * \$.tableName  --$ 代表当表db context schema
 * \@paramName   --@ 为参数名的开头
 */
public class DbQuery extends DbAccess<DbQuery> {

    public DbQuery(DbContext context)
    {
        super(context);
    }

    public DbQuery sql(SQLBuilder sqlBuilder) {
        this.commandText = sqlBuilder.toString();
        this.paramS.clear();
        this._woodKey = null;
        for (Object p1 : sqlBuilder.paramS) {
            doSet("", p1);
        }

        return this;
    }

    @Override
    protected String getCommandID() {
        return this.commandText;
    }

    @Override
    protected Command getCommand() {

        Command cmd = new Command(this.context);

        cmd.key     = getCommandID();
        cmd.paramS = this.paramS;

        StringBuilder sb = new StringBuilder(commandText);

        //1.如果全局设置的替换schema为true，则替换schema
        if(WoodConfig.isUsingSchemaExpression){
            int idx=0;
            while (true) {
                idx = sb.indexOf("$",idx);
                if(idx>0) {
                    if(context.schema() == null){
                        sb.replace(idx, idx + 2, ""); //去掉$.
                    }else {
                        sb.replace(idx, idx + 1, context.schema());
                    }
                    idx++;
                }
                else {
                    break;
                }
            }
        }

        cmd.text = sb.toString();

        runCommandBuiltEvent(cmd);

        return cmd;
    }
}
