package org.noear.wood;

import org.noear.wood.ext.Act1;
import org.noear.wood.ext.Act2;
import org.noear.wood.wrap.Property;

import java.util.List;
import java.util.Map;

/**
 * @author noear
 */
public interface BaseMapper<T> {
    /**
     * 当前数据源
     */
    DbContext db();

    /**
     * 当前表名
     */
    String tableName();

    /**
     * 当前表主键
     */
    String tablePk();

    /**
     * 当前实体类
     */
    Class<?> entityClz();


    Long insert(T entity, boolean excludeNull);

    void insertList(List<T> list);

    Integer deleteById(Object id);

    Integer deleteByIds(Iterable idList);

    Integer deleteByMap(Map<String, Object> columnMap);

    Integer delete(Act1<MapperWhereQ> condition);

    /**
     * @param excludeNull 排除null
     */
    Integer updateById(T entity, boolean excludeNull);

    /**
     * @param entity      待更新的实体
     * @param dataBuilder 组装data的方式，方便支持部分属性允许设置为null，部分不允许
     */
    Integer updateById(T entity, Act2<T, DataItem> dataBuilder);

    Integer update(T entity, boolean excludeNull, Act1<MapperWhereQ> condition);

    /**
     * @param entity      待更新的实体
     * @param dataBuilder 组装data的方式，方便支持部分属性允许设置为null，部分不允许
     * @param condition   更新数据的条件
     * @return
     */
    Integer update(T entity, Act2<T, DataItem> dataBuilder, Act1<MapperWhereQ> condition);

    int[] updateList(List<T> list, Act2<T, DataItem> dataBuilder, Property<T, ?>... conditionFields);

    Long upsert(T entity, boolean excludeNull);

    Long upsertBy(T entity, boolean excludeNull, String conditionFields);

    boolean existsById(Object id);

    boolean exists(Act1<MapperWhereQ> condition);

    T selectById(Object id);

    List<T> selectByIds(Iterable idList);

    List<T> selectByMap(Map<String, Object> columnMap);

    T selectItem(T entity);

    T selectItem(Act1<MapperWhereQ> condition);

    Map<String, Object> selectMap(Act1<MapperWhereQ> condition);

    Object selectValue(String column, Act1<MapperWhereQ> condition);

    Long selectCount(Act1<MapperWhereQ> condition);

    List<T> selectList(Act1<MapperWhereQ> condition);

    List<Map<String, Object>> selectMapList(Act1<MapperWhereQ> condition);

    List<Object> selectArray(String column, Act1<MapperWhereQ> condition);


    List<T> selectList(int start, int size, Act1<MapperWhereQ> condition);

    List<Map<String, Object>> selectMapList(int start, int size, Act1<MapperWhereQ> condition);

    List<Object> selectArray(String column, int start, int size, Act1<MapperWhereQ> condition);

    /**
     * @param start 从0开始
     */
    IPage<T> selectPage(int start, int size, Act1<MapperWhereQ> condition);

    IPage<Map<String, Object>> selectMapPage(int start, int size, Act1<MapperWhereQ> condition);

    List<T> selectTop(int size, Act1<MapperWhereQ> condition);

    List<Map<String, Object>> selectMapTop(int size, Act1<MapperWhereQ> condition);
}
