package wood_rdb.features;

import org.noear.wood.WoodConfig;

public class ConfigDemo {

    public void test1() {
        //监听异常
        WoodConfig.onException((cmd, ex) -> {
            ex.printStackTrace();
        });


        //记录行为
        WoodConfig.onLog((cmd) -> {
            if (cmd.isLog >= 0) { //isLog: -1,不需要记录；0,默认；1,需要记录
                //cmd.text;         //执行代码
                //cmd.paramS;   	//执行参数
                //cmd.paramMap();   //执行参数Map化
            }
        });

        //监听性能
        WoodConfig.onExecuteAft((cmd) -> {
            //cmd.timespan()
        });

        WoodConfig.onExecuteBef((cmd) -> {
            if (cmd.text.indexOf("DELETE ") >= 0) {
                return false;
            }
            return true;
        });
    }
}
