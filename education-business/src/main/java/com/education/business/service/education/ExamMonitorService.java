package com.education.business.service.education;

import com.education.common.constants.CacheKey;
import com.education.model.dto.ExamMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 试卷监控中心
 * @author zengjintao
 * @version 1.0
 * @create_at 2021/1/29 15:20
 */
@Service
public class ExamMonitorService {

    @Autowired
    private RedisTemplate redisTemplate;

    public void addStudentToExamMonitor(Integer testPaperInfoId, ExamMonitor examMonitor) {
        Integer studentId = examMonitor.getStudentInfo().getId();
        redisTemplate.boundHashOps(CacheKey.EXAM_MONITOR_CACHE_KEY + testPaperInfoId)
                .put(studentId, examMonitor);
    }

    public ExamMonitor getExamMonitorStudent(Integer testPaperInfoId, Integer studentId) {
       return (ExamMonitor) redisTemplate.boundHashOps(CacheKey.EXAM_MONITOR_CACHE_KEY + testPaperInfoId).get(studentId);
    }

    public List<ExamMonitor> getExamMonitorByTestPaperInfoId(Integer testPaperInfoId) {
        return redisTemplate.boundHashOps(CacheKey.EXAM_MONITOR_CACHE_KEY + testPaperInfoId)
                .values();
    }

    public void removeStudent(Integer studentId, Integer testPaperInfoId) {
        redisTemplate.boundHashOps(CacheKey.EXAM_MONITOR_CACHE_KEY + testPaperInfoId).delete(studentId);
    }
}
