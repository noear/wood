package org.noear.wood;

import org.noear.wood.cache.ICacheServiceEx;
import org.noear.wood.dialect.DbDialect;
import org.noear.wood.dialect.DbDialectRegistry;
import org.noear.wood.ext.Act1;
import org.noear.wood.ext.Act2;
import org.noear.wood.ext.Fun1;
import org.noear.wood.impl.IMapperAdaptorImpl;
import org.noear.wood.mapper.IMapperAdaptor;
import org.noear.wood.wrap.PrimaryKeyStrategy;
import org.noear.wood.wrap.NamingStrategy;
import org.noear.wood.wrap.TypeConverter;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by noear on 14/11/20.
 */
public final class WoodConfig {
    public static boolean isDebug = false;
    public static boolean isUsingValueExpression = true;
    public static boolean isUsingValueNull = false;
    public static boolean isUsingSchemaPrefix = false;

    /**
     * 是否使用当前上下文的 schema 替换表达式里的 $
     */
    public static boolean isUsingSchemaExpression = true;
    public static boolean isUpdateMustConditional = true;
    public static boolean isDeleteMustConditional = true;
    public static boolean isUsingUnderlineColumnName = true;
    public static boolean isSelectItemEmptyAsNull = false;

    /**
     * 非注解的命名策略
     */
    public static NamingStrategy namingStrategy = new NamingStrategy();

    /**
     * 非注解的字段主键策略
     */
    public static PrimaryKeyStrategy primaryKeyStrategy = new PrimaryKeyStrategy();

    /**
     * 字段类型转换器
     */
    public static TypeConverter typeConverter = new TypeConverter();

    /**
     * Mapper 适配器
     */
    public static IMapperAdaptor mapperAdaptor = new IMapperAdaptorImpl();

    /**
     * 链接工厂
     */
    public static DbConnectionFactory connectionFactory = new DbConnectionFactory();

    public static Map<String, ICacheServiceEx> libOfCache = new ConcurrentHashMap<>();
    public static Map<String, DbContext> libOfDb = new ConcurrentHashMap<>();

    protected static DbEventBus eventBus = new DbEventBus();

    /**
     * 全局方言注册表。启动时已预填内置方言；外部可追加；实例级条目优先于这里。
     */
    public static DbDialectRegistry globalDialectRegistry = DbDialectRegistry.builtin();

    /**
     * 便捷方法：向全局注册表注册一个方言 + 匹配器
     */
    public static void registerDialect(DbDialect dialect, Fun1<Boolean, Connection> matcher) {
        globalDialectRegistry.register(dialect, matcher);
    }


    protected static boolean isEmpty(CharSequence s) {
        if (s == null) {
            return true;
        } else {
            return s.length() == 0;
        }
    }

    protected static void runExceptionEvent(Command cmd, Throwable ex) {
        eventBus.runExceptionEvent(cmd, ex);
    }

    protected static void runCommandBuiltEvent(Command cmd) {
        eventBus.runCommandBuiltEvent(cmd);
    }

    protected static boolean runExecuteBefEvent(Command cmd) {
        return eventBus.runExecuteBefEvent(cmd);
    }

    protected static void runExecuteStmEvent(Command cmd, Statement stm) {
        eventBus.runExecuteStmEvent(cmd, stm);
    }

    protected static void runExecuteAftEvent(Command cmd) {
        eventBus.runExecuteAftEvent(cmd);
    }


    //--------------------------------------
    //
    //

    /**
     * 出现异常时
     */
    public static void onException(Act2<Command, Throwable> listener) {
        eventBus.onException(listener);
    }

    /**
     * 可以记日志时
     */
    public static void onLog(Act1<Command> listener) {
        eventBus.onLog(listener);
    }

    /**
     * 命令构建完成时
     */
    public static void onCommandBuilt(Act1<Command> listener) {
        eventBus.onCommandBuilt(listener);
    }

    /**
     * 执行之前
     */
    public static void onExecuteBef(Fun1<Boolean, Command> listener) {
        eventBus.onExecuteBef(listener);
    }

    /**
     * 执行之中
     */
    public static void onExecuteStm(Act2<Command, Statement> listener) {
        eventBus.onExecuteStm(listener);
    }

    /**
     * 执行之后
     */
    public static void onExecuteAft(Act1<Command> listener) {
        eventBus.onExecuteAft(listener);
    }
}
