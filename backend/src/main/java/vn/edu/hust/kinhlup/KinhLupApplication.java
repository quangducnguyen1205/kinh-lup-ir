package vn.edu.hust.kinhlup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KinhLupApplication {

    /** Khởi động Spring Boot, tạo các bean service/controller và mở server HTTP. */
    public static void main(String[] args) {
        SpringApplication.run(KinhLupApplication.class, args);
    }
}
