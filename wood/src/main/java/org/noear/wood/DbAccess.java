package org.noear.wood;

import org.noear.wood.cache.CacheUsing;
import org.noear.wood.cache.ICacheService;
import org.noear.wood.ext.Act1;
import org.noear.wood.ext.Act2;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by noear on 14-9-5.
 * 数据库方问基类
 */
public abstract class DbAccess<T extends DbAccess> implements IWoodKey,IQuery,Serializable {
    /*查询语句*/
    public String commandText = null;

    /*数据库上下文*/
    public DbContext context;
    /*访问参数*/
    public List<Variate> paramS = new ArrayList<Variate>();

    /*获取执行命令（由子类实现）*/
    protected abstract Command getCommand() throws SQLException;

    /*获取访问标识（由子类实现）*/
    protected abstract String getCommandID();

    private Act1<Command> onCommandExpr = null;

    public T onCommandBuilt(Act1<Command> expr) {
        this.onCommandExpr = expr;
        return (T) this;
    }

    protected void runCommandBuiltEvent(Command cmd) {
        cmd.isLog = _isLog;

        if (onCommandExpr != null) {
            onCommandExpr.run(cmd);
        }

        //全局监听
        cmd.context.runCommandBuiltEvent(cmd);
    }


    public DbAccess(DbContext context) {
        this.context = context;
    }


    /*IWoodKey begin*/
    protected String _woodKey;

    @Override
    public String getWoodKey() {
        return buildWoodKey(paramS);
    }

    protected String buildWoodKey(Collection<Variate> args) {
        if (_woodKey == null) {
            StringBuilder sb = new StringBuilder();

            sb.append(getCommandID()).append(":");

            for (Variate p : args) {
                sb.append("_").append(p.getValue());
            }

            _woodKey = sb.toString();
        }
        return _woodKey;
    }
    /*IWoodKey end*/

    /*获取参数*/
    protected Variate doGet(String paramName) {
        for (Variate p1 : paramS) {
            if (paramName.equals(p1._name)) {
                return p1;
            }
        }

        return null;
    }

    protected Variate doGet(int index) {
        return paramS.get(index);
    }

    /*设置参数值*/
    protected void doSet(String param, Object value) {
        paramS.add(new Variate(param, value));
    }

    protected void doSet(Variate value) {
        paramS.add(value);
    }

    private int _isLog;

    public T log(boolean isLog) {
        _isLog = isLog ? 1 : -1;
        return (T) this;
    }
    //=======================
    //
    // 执行相关代码
    //

    /**
     * 执行插入（返回自增ID）
     */
    public long insert() throws SQLException {
        Command cmd = getCommand();
        return new SQLer(cmd).insert();
    }

    /**
     * 执行更新（返回受影响数）
     */
    public int update() throws SQLException {
        return execute();
    }

    /**
     * 执行删除（返回受影响数）
     */
    public int delete() throws SQLException {
        return execute();
    }

    /**
     * 执行命令（返回受影响数）
     */
    public int execute() throws SQLException {
        Command cmd = getCommand();
        return new SQLer(cmd).execute();
    }

    /**
     * 批量执行命令（返回受影响数）
     * */
    public int[] executeBatch() throws SQLException {
        Command cmd = getCommand();
        cmd.isBatch = true;
        return new SQLer(cmd).executeBatch();
    }

    @Override
    public long getCount() throws SQLException {
        return getVariate().longValue(0L);
    }

    @Override
    public Object getValue() throws SQLException {
        return getVariate().getValue();
    }

    /*执行命令（返回符合条件的第一个值）*/
    @Override
    public <T> T getValue(T def) throws SQLException {
        return getVariate().value(def);
    }

    /*执行命令（返回符合条件的第一个值）*/
    @Override
    public Variate getVariate() throws SQLException {
        return getVariate(null);
    }

    /*执行命令（返回符合条件的第一个值）*/
    @Override
    public Variate getVariate(Act2<CacheUsing, Variate> cacheCondition) throws SQLException {
        Variate rst;
        Command cmd = getCommand();

        if (_cache == null) {
            rst = new SQLer(cmd).getVariate();
        } else {
            _cache.usingCache(cacheCondition);
            rst = _cache.getEx(this.getWoodKey(), Variate.class, () -> (new SQLer(cmd).getVariate()));
        }
        if (rst == null) {
            return new Variate();
        } else {
            return rst;
        }

    }



    @Override
    public <T> List<T> getArray(String column) throws SQLException {
        return getDataList().toArray(column);
    }

    @Override
    public <T> List<T> getArray(int columnIndex) throws SQLException {
        return getDataList().toArray(columnIndex);
    }

    // -->
    @Override
    public <T> List<T> getList(Class<T> cls) throws SQLException {
        return getDataList().toEntityList(cls);
    }

    @Override
    public <T> List<T> getList(Class<T> cls, Act2<CacheUsing, List<T>> cacheCondition) throws SQLException {
        if (cacheCondition == null) {
            return getList(cls);
        }

        VarHolder _tmp = new VarHolder();

        DataList list = getDataList((cu, dl) -> {
            _tmp.value = dl.toEntityList(cls);
            cacheCondition.run(cu, (List<T>) _tmp.value);
        });

        if (_tmp.value == null) {
            //说明是缓存里拿出来的 // 没有经过 cacheCondition 处理
            return list.toEntityList(cls);
        } else {
            return (List<T>) _tmp.value;
        }
    }

    @Override
    public <T> T getItem(Class<T> cls) throws SQLException {
        DataItem item = getDataItem();

        // nullable 处理
        if (item.count() == 0) {
            if (WoodConfig.isSelectItemEmptyAsNull) {
                return null;
            }
        }

        return item.toEntity(cls);
    }

    @Override
    public <T> T getItem(Class<T> cls, Act2<CacheUsing, T> cacheCondition) throws SQLException {
        if (cacheCondition == null) {
            return getItem(cls);
        }

        VarHolder _tmp = new VarHolder();

        DataItem item = getDataItem((cu, di) -> {
            _tmp.value = di.toEntity(cls);
            cacheCondition.run(cu, (T) _tmp.value);
        });

        // nullable 处理
        if (item.count() == 0) {
            if (WoodConfig.isSelectItemEmptyAsNull) {
                return null;
            }
        }

        if (_tmp.value == null) {
            //说明是缓存里拿出来的 // 没有经过 cacheCondition 处理
            return item.toEntity(cls);
        } else {
            return (T) _tmp.value;
        }
    }
    // <--

    @Override
    public DataList getDataList() throws SQLException {
        return getDataList(null);
    }

    @Override
    public DataList getDataList(Act2<CacheUsing, DataList> cacheCondition) throws SQLException {
        DataList rst;
        Command cmd = getCommand();

        if (_cache == null) {
            rst = new SQLer(cmd).getTable();
        } else {
            _cache.usingCache(cacheCondition);
            rst = _cache.getEx(this.getWoodKey(), DataList.class, () -> (new SQLer(cmd).getTable()));
        }

        if (rst == null) {
            return new DataList();
        } else {
            return rst;
        }
    }

    @Override
    public DataReader getDataReader(int fetchSize) throws SQLException {
        Command cmd = getCommand();
        return new SQLer(cmd).getReader(fetchSize);
    }

    @Override
    public List<Map<String, Object>> getMapList() throws SQLException {
        return getDataList().getMapList();
    }

    @Override
    public DataItem getDataItem() throws SQLException {
        return getDataItem(null);
    }

    @Override
    public DataItem getDataItem(Act2<CacheUsing, DataItem> cacheCondition) throws SQLException {
        DataItem rst;
        Command cmd = getCommand();

        if (_cache == null) {
            rst = new SQLer(cmd).getRow();
        } else {
            _cache.usingCache(cacheCondition);
            rst = _cache.getEx(this.getWoodKey(), DataItem.class, () -> (new SQLer(cmd).getRow()));
        }

        if (rst == null) {
            return new DataItem();
        } else {
            return rst;
        }
    }

    @Override
    public Map<String, Object> getMap() throws SQLException {
        return getDataItem().getMap();
    }

    //=======================
    //
    // 缓存控制相关
    //

    protected CacheUsing _cache = null;
    public CacheUsing cacheUsing(){
        return _cache;
    }

    /*引用一个缓存服务*/
    @Override
    public IQuery caching(ICacheService service) {
        _cache = new CacheUsing(service);
        return this;
    }

    /*是否使用缓存*/
    @Override
    public IQuery usingCache(boolean isCache) {
        _cache.usingCache(isCache);
        return this;
    }

    /*使用缓存时间（单位：秒）*/
    @Override
    public IQuery usingCache(int seconds) {
        _cache.usingCache(seconds);
        return this;
    }

    /*添加缓存标签*/
    @Override
    public IQuery cacheTag(String tag) {
        _cache.cacheTag(tag);
        return this;
    }

    protected T cache(CacheUsing cacheUsing) {
        _cache = cacheUsing;
        return (T) this;
    }
}
