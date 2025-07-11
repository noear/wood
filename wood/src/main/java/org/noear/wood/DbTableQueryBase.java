package org.noear.wood;

import org.noear.wood.cache.CacheUsing;
import org.noear.wood.cache.ICacheController;
import org.noear.wood.cache.ICacheService;
import org.noear.wood.ext.Act1;
import org.noear.wood.ext.Act2;
import org.noear.wood.impl.IPageImpl;
import org.noear.wood.wrap.DbType;

import java.sql.SQLException;
import java.util.*;


/**
 * Created by noear on 14/11/12.
 *
 * $.       //当前表空间
 * $NOW()   //说明这里是一个sql 函数
 * ?
 * ?...     //说明这里是一个数组或查询结果
 */
public class DbTableQueryBase<T extends DbTableQueryBase> extends WhereBase<T> implements ICacheController<DbTableQueryBase> {

    String _table_raw;
    String _table; //表名

    SQLBuilder _builder_bef;
    int _isLog = 0;

    public DbTableQueryBase(DbContext context) {
        super(context);
        _builder_bef = new SQLBuilder();
    }


    /**
     * 标记是否记录日志
     */
    public T log(boolean isLog) {
        _isLog = isLog ? 1 : -1;
        return (T) this;
    }



    /**
     * 通过表达式构建自己
     */
    public T build(Act1<T> builder) {
        builder.run((T) this);
        return (T) this;
    }

    protected T table(String table) { //相当于 from
        if (table.startsWith("#")) {
            _table = table.substring(1);

            _table_raw = _table;
        } else {
            _table_raw = table;

            if (table.indexOf('.') > 0) {
                _table = table;
            } else {
                if (WoodConfig.isUsingSchemaPrefix && _context.schema() != null) {
                    _table = fmtObject(_context.schema() + "." + table);
                } else {
                    _table = fmtObject(table); //"$." + table;
                }
            }
        }

        return (T) this;
    }

    /**
     * 添加SQL with 语句（要确保数据库支持）
     * <p>
     * 例：db.table("user u")
     * .with("a","select type num from group by type")
     * .where("u.type in(select a.type) and u.type2 in (select a.type)")
     * .selectMapList("u.*");
     */
    public T with(String name, String code, Object... args) {
        if (_builder_bef.length() < 6) {
            _builder_bef.append(" WITH ");
        } else {
            _builder_bef.append(",");
        }

        _builder_bef.append(fmtColumn(name))
                .append(" AS (")
                .append(code, args)
                .append(") ");

        return (T) this;
    }

    public T with(String name, SelectQ select) {
        if (_builder_bef.length() < 6) {
            _builder_bef.append(" WITH ");
        } else {
            _builder_bef.append(",");
        }

        _builder_bef.append(fmtColumn(name))
                .append(" AS (")
                .append(select)
                .append(") ");

        return (T) this;
    }


    /**
     * 添加 FROM 语句
     */
    public T from(String table) {
        _builder.append(" FROM ").append(fmtObject(table));
        return (T) this;
    }


    private T join(String style, String table) {
        if (table.startsWith("#")) {
            _builder.append(style).append(table.substring(1));
        } else {
            if (WoodConfig.isUsingSchemaPrefix && _context.schema() != null) {
                _builder.append(style).append(fmtObject(_context.schema() + "." + table));
            } else {
                _builder.append(style).append(fmtObject(table));
            }
        }
        return (T) this;
    }

    /**
     * 添加SQL 内关联语句
     */
    public T innerJoin(String table) {
        return join(" INNER JOIN ", table);
    }

    /**
     * 添加SQL 左关联语句
     */
    public T leftJoin(String table) {
        return join(" LEFT JOIN ", table);
    }

    /**
     * 添加SQL 右关联语句
     */
    public T rightJoin(String table) {
        return join(" RIGHT JOIN ", table);
    }

    /**
     * 添加无限制代码
     */
    public T append(String code, Object... args) {
        _builder.append(code, args);
        return (T) this;
    }

    public T on(String code) {
        _builder.append(" ON ").append(code);
        return (T) this;
    }

    public T onEq(String column1, String column2) {
        _builder.append(" ON ").append(fmtColumn(column1)).append("=").append(fmtColumn(column2));
        return (T) this;
    }


    /**
     * 执行插入并返回自增值，使用dataBuilder构建的数据
     */
    public long insert(Act1<IDataItem> dataBuilder) throws SQLException {
        DataItem item = new DataItem();
        dataBuilder.run(item);

        return insert(item);
    }

    /**
     * 执行插入并返回自增值，使用data数据
     */
    public long insert(IDataItem data) throws SQLException {
        if (data == null || data.count() == 0) {
            return 0;
        }

        return insertCompile(data).insert();
    }

    /**
     * 执行插入并返回命令结果
     */
    public Object insertAndResult(IDataItem data) throws SQLException {
        if (data == null || data.count() == 0) {
            return null;
        }

        return insertCompile(data).insertAndResult();
    }

    /**
     * 插入编译并获取命令
     * */
    public Command insertAsCmd(IDataItem data) {
        if (data == null || data.count() == 0) {
            return null;
        }

        return insertCompile(data).getCommand();
    }

    /**
     * 插入编译
     * */
    protected DbQuery insertCompile(IDataItem data) {
        _builder.clear();

        _context.getDialect()
                .buildInsertOneCode(_context, _table, _builder, this::isSqlExpr, _usingNull, data);

        return compile();
    }


    /**
     * 根据约束进行插入
     */
    public long insertBy(IDataItem data, String conditionFields) throws SQLException {
        if (data == null || data.count() == 0) {
            return 0;
        }

        String[] ff = conditionFields.split(",");

        if (ff.length == 0) {
            throw new RuntimeException("Please enter constraints");
        }

        this.where("1=1");

        for (String f : ff) {
            this.andEq(f, data.get(f));
        }

        if (this.selectExists()) {
            return 0;
        }

        return insert(data);
    }

    /**
     * 执行批量合并插入，使用集合数据
     */
    public boolean insertList(List<DataItem> valuesList) throws SQLException {
        if (valuesList == null) {
            return false;
        }

        return insertList(valuesList.get(0), valuesList);
    }

    /**
     * 执行批量合并插入，使用集合数据（由dataBuilder构建数据）
     */
    public <T> boolean insertList(Collection<T> valuesList, Act2<T, DataItem> dataBuilder) throws SQLException {
        List<DataItem> list2 = new ArrayList<>();

        for (T values : valuesList) {
            DataItem item = new DataItem();
            dataBuilder.run(values, item);

            list2.add(item);
        }


        if (list2.size() > 0) {
            return insertList(list2.get(0), list2);
        } else {
            return false;
        }
    }


    protected <T extends GetHandler> boolean insertList(IDataItem cols, Collection<T> valuesList) throws SQLException {
        if (valuesList == null || valuesList.size() == 0) {
            return false;
        }

        if (cols == null || cols.count() == 0) {
            return false;
        }

        _builder.backup();

        _context.getDialect().buildInsertOneCode(_context, _table, _builder, this::isSqlExpr, true, cols);

        List<Object[]> argList = new ArrayList<>();
        String tml = _builder.toString();

        for (GetHandler item : valuesList) {
            List<Object> tmp = new ArrayList<>();
            for (String key : cols.keys()) {
                tmp.add(item.get(key));
            }

            argList.add(tmp.toArray());
        }


        _builder.clear();
        _builder.append(tml, argList.toArray());

        int[] rst = compile().executeBatch();

        _builder.restore();


        return rst.length > 0;
    }

    /**
     * 使用data的数据,根据约束字段自动插入或更新
     * <p>
     * 请改用 upsertBy
     */
    @Deprecated
    public long upsert(IDataItem data, String conditionFields) throws SQLException {
        return upsertBy(data, conditionFields);
    }


    /**
     * 使用data的数据,根据约束字段自动插入或更新
     */
    public long upsertBy(IDataItem data, String conditionFields) throws SQLException {
        if (data == null || data.count() == 0) {
            return 0;
        }

        String[] ff = conditionFields.split(",");

        if (ff.length == 0) {
            throw new RuntimeException("Please enter constraints");
        }

        this.where("1=1");
        for (String f : ff) {
            this.andEq(f, data.get(f));
        }

        if (this.selectExists()) {
            for (String f : ff) {
                data.remove(f);
            }

            return this.update(data);
        } else {
            return this.insert(data);
        }
    }

    public int updateBy(IDataItem data, String conditionFields) throws SQLException {
        String[] ff = conditionFields.split(",");

        if (ff.length == 0) {
            throw new RuntimeException("Please enter constraints");
        }

        this.whereTrue();

        for (String f : ff) {
            this.andEq(f, data.get(f));
        }

        return update(data);
    }

    /**
     * 执行更新并返回影响行数，使用dataBuilder构建的数据
     */
    public int update(Act1<IDataItem> dataBuilder) throws SQLException {
        DataItem item = new DataItem();
        dataBuilder.run(item);

        return update(item);
    }

    /**
     * 执行更新并返回影响行数，使用set接口的数据
     */
    public int update(IDataItem data) throws SQLException {
        if (data == null || data.count() == 0) {
            return 0;
        }


        return updateCompile(data).execute();
    }

    /**
     * 更新编译并返回命令
     * */
    public Command updateAsCmd(IDataItem data) {
        if (data == null || data.count() == 0) {
            return null;
        }


        return updateCompile(data).getCommand();
    }

    /**
     * 更新编译
     * */
    protected DbQuery updateCompile(IDataItem data)  {
        if (WoodConfig.isUpdateMustConditional && _builder.indexOf(" WHERE ") < 0) {
            throw new RuntimeException("Lack of update condition!!!");
        }

        StringBuilder updateSb = new StringBuilder();
        _context.getDialect().updateCmdBegin(updateSb, _table);

        List<Object> setArgs = new ArrayList<Object>();
        StringBuilder setSb = new StringBuilder();

        _context.getDialect().updateCmdSet(setSb, _table);
        updateItemsBuild0(data, setSb, setArgs);

        _builder.backup();
        _builder.insert(updateSb.toString());
        _builder.insertBySymbol(" WHERE ",setSb.toString(), setArgs.toArray());


        if (limit_top > 0) {
            if (dbType() == DbType.MySQL || dbType() == DbType.MariaDB) {
                _builder.append(" LIMIT ?", limit_top);
            }
        }

        DbQuery query = compile();

        _builder.restore();

        return query;
    }

    private void updateItemsBuild0(IDataItem data, StringBuilder buf, List<Object> args) {
        data.forEach((key, value) -> {
            if (value == null) {
                if (_usingNull) {
                    buf.append(fmtColumn(key)).append("=null,");
                }
                return;
            }

            if (value instanceof String) {
                String val2 = (String) value;
                if (isSqlExpr(val2)) {
                    buf.append(fmtColumn(key)).append("=").append(val2.substring(1)).append(",");
                } else {
                    buf.append(fmtColumn(key)).append("=?,");
                    args.add(value);
                }
            } else {
                buf.append(fmtColumn(key)).append("=?,");
                args.add(value);
            }
        });

        buf.deleteCharAt(buf.length() - 1);
    }

    private void updateItemsBuildByFields0(IDataItem data, StringBuilder buf) {
        data.forEach((key, value) -> {
            buf.append(fmtColumn(key)).append("=?,");
        });
        buf.deleteCharAt(buf.length() - 1);
    }


    /**
     * 执行批量合并插入，使用集合数据
     */
    public int[] updateList(List<DataItem> valuesList, String conditionFields) throws SQLException {
        if (valuesList == null || valuesList.size() == 0) {
            return null;
        }

        return updateList(valuesList.get(0), valuesList, conditionFields);
    }

    /**
     * 执行批量合并插入，使用集合数据（由dataBuilder构建数据）
     */
    public <T> int[] updateList(Collection<T> valuesList, Act2<T, DataItem> dataBuilder, String conditionFields) throws SQLException {
        if (valuesList == null || valuesList.size() == 0) {
            return null;
        }

        List<DataItem> list2 = new ArrayList<>();

        for (T values : valuesList) {
            DataItem item = new DataItem();
            dataBuilder.run(values, item);

            list2.add(item);
        }


        if (list2.size() > 0) {
            return updateList(list2.get(0), list2, conditionFields);
        } else {
            return null;
        }
    }


    protected <T extends GetHandler> int[] updateList(IDataItem cols, Collection<T> valuesList, String conditionFields) throws SQLException {
        if (valuesList == null || valuesList.size() == 0) {
            return null;
        }

        if (cols == null || cols.count() == 0) {
            return null;
        }

        String[] ff = conditionFields.split(",");

        if (ff.length == 0) {
            throw new RuntimeException("Please enter constraints");
        }


        _builder.backup();

        this.where("1=1");
        for (String f : ff) {
            this.and(f + "=?");
        }

        List<Object[]> argList = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        _context.getDialect().updateCmdBegin(sb, _table);
        _context.getDialect().updateCmdSet(sb, _table);

        updateItemsBuildByFields0(cols, sb);

        for (GetHandler item : valuesList) {
            List<Object> tmp = new ArrayList<>();
            for (String key : cols.keys()) {
                tmp.add(item.get(key));
            }

            for (String key : ff) {
                tmp.add(item.get(key));
            }
            argList.add(tmp.toArray());
        }


        _builder.insert(sb.toString(), argList.toArray());

        int[] rst = compile().executeBatch();

        _builder.restore();

        return rst;
    }


    /**
     * 执行删除，并返回影响行数
     */
    public int delete() throws SQLException {
        return deleteCompile().execute();
    }

    /**
     * 删除编译并返回命令
     * */
    public Command deleteAsCmd(){
        return deleteCompile().getCommand();
    }

    /**
     * 删除编译
     * */
    protected DbQuery deleteCompile()  {
        StringBuilder sb = new StringBuilder();

        _context.getDialect().deleteCmd(sb, _table, _builder.indexOf(" FROM ") < 0);

        _builder.insert(sb.toString());

        if (limit_top > 0) {
            if (dbType() == DbType.MySQL || dbType() == DbType.MariaDB) {
                _builder.append(" LIMIT ?", limit_top);
            }
        }


        if (WoodConfig.isDeleteMustConditional && _builder.indexOf(" WHERE ") < 0) {
            throw new RuntimeException("Lack of delete condition!!!");
        }

        return compile();
    }


    protected int limit_start, limit_size;

    /**
     * 添加SQL paging语句
     */
    public T limit(int start, int size) {
        limit_start = start;
        limit_size = size;
        //_builder.append(" LIMIT " + start + "," + rows + " ");
        return (T) this;
    }

    protected int limit_top = 0;

    /**
     * 添加SQL top语句
     */
    public T limit(int size) {
        limit_top = size;
        //_builder.append(" LIMIT " + rows + " ");
        return (T) this;
    }


    /**
     * 添加SQL paging语句
     */
    public T paging(int start, int size) {
        limit_start = start;
        limit_size = size;
        //_builder.append(" LIMIT " + start + "," + rows + " ");
        return (T) this;
    }

    /**
     * 添加SQL top语句
     */
    public T top(int size) {
        limit_top = size;
        //_builder.append(" LIMIT " + rows + " ");
        return (T) this;
    }



    String _hint = null;

    public T hint(String hint) {
        _hint = hint;
        return (T) this;
    }

    @Deprecated
    public boolean exists() throws SQLException {
        return selectExists();
    }

    @Deprecated
    public long count() throws SQLException {
        return selectCount();
        //return count("COUNT(*)");
    }

    @Deprecated
    public long count(String code) throws SQLException {
        return selectCount(code);
        //return selectDo(code).getVariate().longValue(0l);
    }


    @Deprecated
    public IQuery select(String columns) {
        return selectDo(columns);
    }

    public Command selectAsCmd(String columns){
       return selectCompile(columns).getCommand();
    }

    protected IQuery selectDo(String columns) {
        DbQuery rst = selectCompile(columns);

        if (_cache != null) {
            rst.cache(_cache);
        }

        return rst;
    }

    protected DbQuery selectCompile(String columns) {
        return selectCompile(columns, true);
    }

    /**
     * 查询编译
     * @param columns 查询的列
     * @param doFormat 是否对查询的列做格式化
     * @return
     */
    protected DbQuery selectCompile(String columns, boolean doFormat) {
        select_do(columns, doFormat);

        DbQuery rst = compile();

        _builder.restore();

        return rst;
    }

    public boolean selectExists() throws SQLException {
        int bak = limit_top;
        limit(1);
        select_do(" 1 ", false);
        limit(bak);

        DbQuery rst = compile();

        if (_cache != null) {
            rst.cache(_cache);
        }

        _builder.restore();

        return rst.getValue() != null;
    }

    public long selectCount() throws SQLException {
        return selectCount("COUNT(*)");
    }

    public long selectCount(String column) throws SQLException {
        if (column.indexOf("(") < 0) {
            column = "COUNT(" + column + ")";
        }

        //备份
        int limit_start_bak = limit_start;
        int limit_size_bak = limit_size;
        int limit_top_bak = limit_top;
        StringBuilder _orderBy_bak = _orderBy;

        limit_start = 0;
        limit_size = 0;
        limit_top = 0;
        _orderBy = null;

        try {
            if(_hasGroup){
                //有分组时，需要用子表的形式查询
                DbQuery groupQuery = selectCompile("1 as woodflag", false);

                _builder.backup();

                DbTableQuery countQuery = new DbTableQuery(_context);
                DbTableQuery table = countQuery.table(" (" + groupQuery.commandText + ") a");
                table._builder.paramS = _builder.paramS;
                return table.selectCount("1");
            }

            return selectDo(column).getVariate().longValue(0L);
        } finally {
            //恢复
            limit_start = limit_start_bak;
            limit_size = limit_size_bak;
            limit_top = limit_top_bak;
            _orderBy = _orderBy_bak;
            if(_hasGroup){
                _builder.restore();
            }
        }
    }

    public Variate selectVariate(String column) throws SQLException {
        return selectDo(column).getVariate();
    }

    public Object selectValue(String column) throws SQLException {
        return selectDo(column).getValue();
    }

    public <T> T selectValue(String column, T def) throws SQLException {
        return selectDo(column).getValue(def);
    }

    public <T> T selectItem(String columns, Class<T> clz) throws SQLException {
        return selectDo(columns).getItem(clz);
    }

    public <T> List<T> selectList(String columns, Class<T> clz) throws SQLException {
        return selectDo(columns).getList(clz);
    }

    /**
     * @since 2024/06/12
     * */
    public <T> IDataReader<T> selectReader(String columns, int fetchSize, Class<T> clz) throws SQLException {
        return selectDo(columns).getDataReader(fetchSize).toEntityReader(clz);
    }

    public <T> IDataReader<T> selectReader(String columns, Class<T> clz) throws SQLException {
        return selectReader(columns, 0, clz);
    }

    public <T> IPage<T> selectPage(String columns, Class<T> clz) throws SQLException {
        long total = selectCount();
        List<T> list = selectDo(columns).getList(clz);

        return new IPageImpl<>(list, total, limit_size);
    }

    public DataItem selectDataItem(String columns) throws SQLException {
        return selectDo(columns).getDataItem();
    }

    public DataList selectDataList(String columns) throws SQLException {
        return selectDo(columns).getDataList();
    }

    /**
     * @since 2024/06/12
     * */
    public DataReader selectDataReader(String columns, int fetchSize) throws SQLException {
        return selectDo(columns).getDataReader(fetchSize);
    }

    public DataReader selectDataReader(String columns) throws SQLException {
        return selectDataReader(columns, 0);
    }

    public IPage<DataItem> selectDataPage(String columns) throws SQLException {
        long total = selectCount();
        List<DataItem> list = selectDo(columns).getDataList().getRows();

        return new IPageImpl<>(list, total, limit_size);
    }

    public Map<String, Object> selectMap(String columns) throws SQLException {
        return selectDo(columns).getMap();
    }

    public List<Map<String, Object>> selectMapList(String columns) throws SQLException {
        return selectDo(columns).getMapList();
    }

    public IPage<Map<String, Object>> selectMapPage(String columns) throws SQLException {
        long total = selectCount();
        List<Map<String, Object>> list = selectDo(columns).getMapList();

        return new IPageImpl<>(list, total, limit_size);
    }

    public <T> List<T> selectArray(String column) throws SQLException {
        return selectDo(column).getArray();
    }


    public SelectQ selectQ(String columns) {
        select_do(columns, true);

        return new SelectQ(_builder);
    }

    private void select_do(String columns, boolean doFormat) {
        _builder.backup();

        //1.构建 xxx... FROM table
        StringBuilder sb = new StringBuilder(_builder.builder.length() + 100);
        sb.append(" ");//不能去掉
        if (doFormat) {
            sb.append(fmtMutColumns(columns)).append(" FROM ").append(_table);
        } else {
            sb.append(columns).append(" FROM ").append(_table);
        }
        sb.append(_builder.builder);

        _builder.builder = sb;

        //2.尝试构建分页
        if (limit_top > 0) {
            _context.getDialect().buildSelectTopCode(_context, _table_raw, _builder, _orderBy, limit_top);
        } else if (limit_size > 0) {
            _context.getDialect().buildSelectRangeCode(_context, _table_raw, _builder, _orderBy, limit_start, limit_size);
        } else {
            _builder.insert(0, "SELECT ");
            if (_orderBy != null) {
                _builder.append(_orderBy);
            }
        }

        //3.构建hint
        if (_hint != null) {
            sb.append(_hint);
            _builder.insert(0, _hint);
        }

        //4.构建whith
        if (_builder_bef.length() > 0) {
            _builder.insert(_builder_bef);
        }
    }

    //编译（成DbQuery）
    private DbQuery compile() {
        DbQuery temp = new DbQuery(_context).sql(_builder);

        _builder.clear();

        return temp.onCommandBuilt((cmd) -> {
            cmd.isLog = _isLog;
            cmd.tag = _table;
        });
    }

    private boolean _usingNull = WoodConfig.isUsingValueNull;
    protected boolean usingNull(){
        return _usingNull;
    }

    /**
     * 充许使用null插入或更新
     */
    public T usingNull(boolean isUsing) {
        _usingNull = isUsing;
        return (T) this;
    }

    private boolean _usingExpression = WoodConfig.isUsingValueExpression;

    protected boolean usingExpr(){
        return _usingExpression;
    }
    /**
     * 充许使用$表达式构建sql
     */
    public T usingExpr(boolean isUsing) {
        _usingExpression = isUsing;
        return (T) this;
    }

    private boolean isSqlExpr(String txt) {
        if (_usingExpression == false) {
            return false;
        }

        if (txt.startsWith("$")
                && txt.indexOf(" ") < 0
                && txt.length() < 100) { //不能出现空隔，且100字符以内。否则视为普通字符串值
            return true;
        } else {
            return false;
        }
    }

    //=======================
    //
    // 缓存控制相关
    //

    protected CacheUsing _cache = null;

    /**
     * 使用一个缓存服务
     */
    public T caching(ICacheService service) {
        _cache = new CacheUsing(service);
        return (T) this;
    }

    /**
     * 是否使用缓存
     */
    public T usingCache(boolean isCache) {
        _cache.usingCache(isCache);
        return (T) this;
    }

    /**
     * 使用缓存时间（单位：秒）
     */
    public T usingCache(int seconds) {
        _cache.usingCache(seconds);
        return (T) this;
    }

    /**
     * 为缓存添加标签
     */
    public T cacheTag(String tag) {
        _cache.cacheTag(tag);
        return (T) this;
    }
}
