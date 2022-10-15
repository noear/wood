package webapp;


import org.noear.solon.Solon;
import org.noear.wood.xml.XmlSqlLoader;

public class App {
    public static void main(String[] args) {
        XmlSqlLoader.tryLoad();

        Solon.start(App.class, args);
    }
}
