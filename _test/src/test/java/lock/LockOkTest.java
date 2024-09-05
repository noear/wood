package lock;

import lock.web.App;
import org.junit.jupiter.api.Test;
import org.noear.snack.ONode;
import org.noear.solon.test.HttpTestBase;
import org.noear.solon.test.SolonTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author noear 2022/4/4 created
 */
@SolonTest(App.class)
public class LockOkTest extends HttpTestBase {

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
        assert rstList.size() == tmpList.size();
    }

    private final ReentrantLock SYNC_LOCK = new ReentrantLock();

    private void exe0(String str) {
        SYNC_LOCK.tryLock();
        try {
            String tmp = path("/contains").data("key", str).post();

            if ("0".equals(tmp)) {
                path("/add").data("key", str).post();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            SYNC_LOCK.unlock();
        }
    }
}
