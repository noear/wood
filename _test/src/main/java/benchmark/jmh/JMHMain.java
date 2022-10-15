package benchmark.jmh;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.RunnerException;
import benchmark.jmh.jdbc.JdbcService;
import benchmark.jmh.wood.WoodService;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

/**
 * 性能测试入口,数据是Throughput，越大越好
 */
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Threads(1)
@Fork(1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class JMHMain {
    JdbcService jdbcService = null;
    WoodService woodService = null;

    @Setup
    public void init() {

        jdbcService = new JdbcService();
        jdbcService.init();

        woodService = new WoodService();
        woodService.init();

    }


    /*   JDBC,基准，有些方法性能飞快    */
    @Benchmark
    public void jdbcInsert() {
        jdbcService.addEntity();
    }

    @Benchmark
    public void jdbcSelectById() {
        jdbcService.getEntity();
    }

    @Benchmark
    public void jdbcExecuteJdbc() {
        jdbcService.executeJdbcSql();
    }


    /*   Wood    */
    @Benchmark
    public void woodInsert() {
        woodService.addEntity();
    }

    @Benchmark
    public void woodSelectById() {
        woodService.getEntity();
    }

    @Benchmark
    public void woodLambdaQuery() {
        woodService.lambdaQuery();
    }

    @Benchmark
    public void woodExecuteJdbc() {
        woodService.executeJdbcSql();
    }

    @Benchmark
    public void woodExecuteJdbc2() throws SQLException {
        woodService.executeJdbcSql2();
    }

    @Benchmark
    public void woodExecuteTemplate() {
        woodService.executeTemplateSql();
    }

    @Benchmark
    public void woodExecuteTemplate2() throws SQLException{
        woodService.executeTemplateSql2();
    }

    @Benchmark
    public void woodFile() {
        woodService.sqlFile();
    }

    @Benchmark
    public void woodPageQuery() {
        woodService.pageQuery();
    }


    public static void main(String[] args) throws RunnerException {

          test();
//        Options opt = new
//                OptionsBuilder()
//                .include(JMHMain.class.getSimpleName())
//                .build();
//        new Runner(opt).run();
    }

    /**
     * 先单独运行一下保证每个测试都没有错误
     */
    public static void test() {
        JMHMain jmhMain = new JMHMain();
        jmhMain.init();
        for (int i = 0; i < 3; i++) {
            Method[] methods = jmhMain.getClass().getMethods();
            for (Method method : methods) {
                if (method.getAnnotation(Benchmark.class) == null) {
                    continue;
                }
                try {

                    method.invoke(jmhMain, new Object[0]);

                } catch (Exception ex) {
                    throw new IllegalStateException(" method " + method.getName(), ex);
                }

            }
        }

    }


}
