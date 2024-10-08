package wood_rdb.xml;

import org.junit.jupiter.api.Test;
import org.noear.solon.Utils;
import org.noear.wood.xml.XmlSqlCompiler;

import java.net.URL;

public class XmlTest {
    @Test
    public void test1() throws Exception {

        URL url = Utils.getResource("bak/test2.xml");

        String code = XmlSqlCompiler.parse(url);

        System.out.println(code);
    }

    @Test
    public void test2() throws Exception {

        URL url = Utils.getResource("wood/SqlMapper.xml");

        String code = XmlSqlCompiler.parse(url);

        System.out.println(code);
    }
}
