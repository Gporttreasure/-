package com.education.api.controller;

import com.education.business.message.RabbitMqConfig;
import com.education.common.cache.CacheBean;
import com.education.common.disabled.RateLimitLock;
import com.education.common.utils.Result;
import com.jfinal.kit.HttpKit;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 限流测试接口
 * @author zengjintao
 * @version 1.0
 * @create_date 2020/6/12 10:41
 * @since 1.0.0
 */
@RestController
@RequestMapping("/limit")
public class LimitController {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private CacheBean redisCacheBean;
    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final Map cache = new HashMap();

    private ReentrantLock reentrantLock = new ReentrantLock();

    @GetMapping
    @RateLimitLock(limit = 20)
    public Result limit() {
        return Result.success("访问接口");
    }
    @GetMapping("submit")
    public Result submit(@RequestParam Map params) {


        System.out.println("执行submit方法保存数据" + params);
        return Result.success("success");
    }


  /*  public static void main(String[] args) {
        for (int i = 0; i < 100000; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    System.out.println(HttpKit.get("http://localhost/limit/jdbcTest"));
                }
            }).start();
        }
    }*/

    @GetMapping("jdbcTestCache")
    public Result jdbcTestCache() {
        List<Map<String, Object>> list = redisCacheBean.get("jdbcTest");
        if (list == null) {
            System.err.println("缓存无数据");
            reentrantLock.lock();
            try {
                list = redisCacheBean.get("jdbcTest");
                if (list == null) {
                    System.err.println("查询数据库");
                    list = jdbcTemplate.queryForList("select * from question_info_copy limit 5000, 50");
                    redisCacheBean.put("jdbcTest", list);
                }
            } finally {
                reentrantLock.unlock();
            }
        }
        return Result.success(list);
    }
    @GetMapping("jdbcTest")
    public Result jdbcTest() {
        List<Map<String, Object>> list = jdbcTemplate.queryForList("select * from question_info_copy limit 5000, 50");
        return Result.success(list);
    }

    /**
     * 使用mysql乐观锁更新文章阅读量或者点赞量测试接口
     */
    @GetMapping("mysql")
    public void mysql() {
        jdbcTemplate.update("update exam_info set mark = mark + 1 where id = 1");
    }

    @GetMapping("syncMysql")
    public void syncMysql() {
        synchronized (this) {
            jdbcTemplate.update("update exam_info set mark = mark + 1 where id = 1");
        }
    }

    /**
     * 使用线程池更新文章阅读量或者点赞量测试接口
     */
    @GetMapping("threadPool")
    public void threadPool() {
        threadPoolTaskExecutor.execute(() -> {
            jdbcTemplate.update("update exam_info set mark = mark + 1 where id = 1");
        });
    }

    /**
     * 使用mq消息队列更新文章阅读量或者点赞量测试接口
     */
    @GetMapping("mq")
    public void mq() {
        rabbitTemplate.convertAndSend(RabbitMqConfig.TEST_FANOUT_EXCHANGE, RabbitMqConfig.TEST_QUEUE_ROUTING_KEY, "test", new CorrelationData());
    }

    public static void main(String[] args) throws InterruptedException {
        int number = 0;
        while (true && number < 3) {
            for (int i = 0; i < 1000; i++) {
                new Thread(() -> {
                    String content = HttpKit.get("http://127.0.0.1:8001/limit/threadPool");
                    System.out.println(content);
                }).start();
            }
            Thread.sleep(3000);
            number++;
        }

    }
}
