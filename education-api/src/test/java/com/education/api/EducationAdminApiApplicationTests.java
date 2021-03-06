package com.education.api;



import com.alibaba.fastjson.JSON;
import com.education.business.service.education.QuestionInfoService;
import com.education.business.service.system.SystemAdminService;
import com.education.business.task.TaskManager;
import com.education.business.task.TaskParam;
import com.education.common.cache.CacheBean;
import com.education.common.cache.EhcacheBean;
import com.education.common.utils.ObjectUtils;
import com.education.model.entity.StudentInfo;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;


@SpringBootTest
@RunWith(SpringRunner.class)
@Slf4j
public class EducationAdminApiApplicationTests {

    @Autowired
    private QuestionInfoService questionInfoService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    private CacheBean cacheBean = new EhcacheBean();

    @Autowired
    private SystemAdminService systemAdminService;

    static final String SCORE_RANK = "score_rank";

    @Test
    public void testRedis() {
       // redisTemplate.opsForList().rightPop("test_token");

        redisTemplate.boundHashOps("token_teste").increment("id:1", 1);

        System.out.println(redisTemplate.boundHashOps("token_teste").get("id:1"));
     //   System.out.println(redisTemplate.hasKey("test_token"));
    }

    @Test
    public void redisSort() {
        Set<ZSetOperations.TypedTuple<StudentInfo>> tuples = new HashSet<>();
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1; i++) {
            StudentInfo studentInfo = new StudentInfo();
            studentInfo.setId(i);
           // DefaultTypedTuple<String> tuple = new DefaultTypedTuple<String>("??????" + i, 1D + i);

            DefaultTypedTuple<StudentInfo> tuple = new DefaultTypedTuple(studentInfo, 1D + i);
            tuples.add(tuple);
        }
        System.out.println("????????????:" +( System.currentTimeMillis() - start));
        Long num = redisTemplate.opsForZSet().add(SCORE_RANK, tuples);
        System.out.println("??????????????????:" +(System.currentTimeMillis() - start));
        System.out.println("??????????????????" + num);

        list();
    }

    @Test
    public void list() {
        Set<String> range = redisTemplate.opsForZSet().reverseRange(SCORE_RANK, 0, 10);
        System.out.println("????????????????????????:" + JSON.toJSONString(range));
        Set<ZSetOperations.TypedTuple<String>> rangeWithScores = redisTemplate.opsForZSet().reverseRangeWithScores(SCORE_RANK, 0, 10);
        System.out.println("?????????????????????????????????:" + JSON.toJSONString(rangeWithScores));
    }

}
