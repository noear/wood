package webapp;

import org.noear.wood.xml.XmlSqlLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class App {
    public static void main(String[] args){
        XmlSqlLoader.tryLoad();

        SpringApplication.run(App.class, args);
    }
}
