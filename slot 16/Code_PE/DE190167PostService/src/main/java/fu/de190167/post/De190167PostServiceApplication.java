package fu.de190167.post;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class De190167PostServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(De190167PostServiceApplication.class, args);
    }
}
