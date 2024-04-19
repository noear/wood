package org.noear.wood;

import org.noear.wood.ext.Act1;
import org.noear.wood.ext.Act2;
import org.noear.wood.ext.Fun1;

import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author noear 2024/4/19 created
 */
public class DbEventBus {
    private Set<Act2<Command, Throwable>> onException_listener = new LinkedHashSet<>();
    private Set<Act1<Command>> onLog_listener = new LinkedHashSet<>();

    private Set<Act1<Command>> onCommandBuilt_listener = new LinkedHashSet();

    //执行之前
    private Set<Fun1<Boolean, Command>> onExecuteBef_listener = new LinkedHashSet<>();
    //执行声明
    private Set<Act2<Command, Statement>> onExecuteStm_listener = new LinkedHashSet<>();
    //执行之后
    private Set<Act1<Command>> onExecuteAft_listener = new LinkedHashSet();


    protected static boolean isEmpty(CharSequence s) {
        if (s == null) {
            return true;
        } else {
            return s.length() == 0;
        }
    }

    protected void runExceptionEvent(Command cmd, Throwable ex) {
        if (onException_listener.size() > 0) {
            if (cmd != null) {
                cmd.timestop = System.currentTimeMillis();
            }

            onException_listener.forEach(fun -> {
                fun.run(cmd, ex);
            });
        }
    }

    protected void runCommandBuiltEvent(Command cmd) {
        if (onCommandBuilt_listener.size() > 0) {
            onCommandBuilt_listener.forEach(fun -> {
                fun.run(cmd);
            });
        }
    }

    protected boolean runExecuteBefEvent(Command cmd) {
        cmd.timestart = System.currentTimeMillis();

        VarHolder<Boolean> rst = new VarHolder<>();
        rst.value = true;

        if (onExecuteBef_listener.size() > 0) {
            onExecuteBef_listener.forEach(fun -> {
                rst.value = rst.value && fun.run(cmd);
            });
        }

        return rst.value;
    }

    protected void runExecuteStmEvent(Command cmd, Statement stm) {
        if (onExecuteStm_listener.size() > 0) {
            onExecuteStm_listener.forEach(fun -> {
                fun.run(cmd, stm);
            });
        }
    }

    protected void runExecuteAftEvent(Command cmd) {
        try {
            if (cmd.onExecuteAft != null) {
                cmd.onExecuteAft.run(cmd);
                cmd.onExecuteAft = null; //执行之后，就会清掉
            }

            cmd.timestop = System.currentTimeMillis();

            if (onExecuteAft_listener.size() > 0) {
                onExecuteAft_listener.forEach(fun -> {
                    fun.run(cmd);
                });
            }

            if (cmd.isLog > 0 && onLog_listener.size() > 0) {
                onLog_listener.forEach(fun -> fun.run(cmd));
            }
        } catch (Throwable e) {
            //执行后，不能抛出异常，不然影响正常的工作流
            e.printStackTrace();
        }
    }


    //--------------------------------------
    //
    //

    /**
     * 出现异常时
     */
    public void onException(Act2<Command, Throwable> listener) {
        onException_listener.add(listener);
    }

    /**
     * 可以记日志时
     */
    public void onLog(Act1<Command> listener) {
        onLog_listener.add(listener);
    }

    /**
     * 命令构建完成时
     */
    public void onCommandBuilt(Act1<Command> listener) {
        onCommandBuilt_listener.add(listener);
    }

    /**
     * 执行之前
     */
    public void onExecuteBef(Fun1<Boolean, Command> listener) {
        onExecuteBef_listener.add(listener);
    }

    /**
     * 执行之中
     */
    public void onExecuteStm(Act2<Command, Statement> listener) {
        onExecuteStm_listener.add(listener);
    }

    /**
     * 执行之后
     */
    public void onExecuteAft(Act1<Command> listener) {
        onExecuteAft_listener.add(listener);
    }
}
