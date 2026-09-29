package vn.edu.hust.kinhlup;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import vn.edu.hust.kinhlup.api.ReindexController;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class KinhLupApplicationTests {
    @Autowired ApplicationContext context;

    @Test
    void contextLoads() {
        assertTrue(context.getBeansOfType(ReindexController.class).isEmpty());
    }
}
