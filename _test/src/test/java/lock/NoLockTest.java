package lock;

import lock.web.App;
import org.junit.jupiter.api.Test;
import org.noear.snack.ONode;
import org.noear.solon.test.HttpTester;
import org.noear.solon.test.SolonTest;

import java.util.*;

/**
 * @author noear 2022/4/4 created
 */
@SolonTest(App.class)
public class NoLockTest extends HttpTester {

    @Test
    public void test() throws Exception {
        List<String> tmpList = new ArrayList<>();
        List<String> aryList = new ArrayList<>();

        tmpList.add(UUID.randomUUID().toString());
        tmpList.add(UUID.randomUUID().toString());
        tmpList.add(UUID.randomUUID().toString());
        tmpList.add(UUID.randomUUID().toString());
        tmpList.add(UUID.randomUUID().toString());

        for (int i = 0; i < 100; i++) {
            tmpList.forEach(s -> aryList.add(s));
        }

        aryList.parallelStream().forEach(s -> exe0(s));

        String rstJson = path("/getAll").get();
        List<String> rstList = ONode.deserialize(rstJson);
        System.out.println(rstJson);
        System.out.println(rstList.size() + ":::" + tmpList.size());
        assert rstList.size() != tmpList.size();
    }


    private void exe0(String str) {
        try {
            String tmp = path("/contains").data("key", str).post();
            if ("0".equals(tmp)) {
                path("/add").data("key", str).post();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
