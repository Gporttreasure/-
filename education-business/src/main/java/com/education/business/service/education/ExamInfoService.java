package com.education.business.service.education;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.education.business.correct.QuestionCorrect;
import com.education.business.correct.SystemQuestionCorrect;
import com.education.business.correct.TeacherQuestionCorrect;
import com.education.business.mapper.education.ExamInfoMapper;
import com.education.business.message.QueueManager;
import com.education.business.service.BaseService;
import com.education.business.task.TaskParam;
import com.education.business.task.WebSocketMessageListener;
import com.education.common.constants.CacheKey;
import com.education.common.constants.CacheTime;
import com.education.common.constants.SystemConstants;
import com.education.common.constants.EnumConstants;
import com.education.common.exception.BusinessException;
import com.education.common.model.PageInfo;
import com.education.common.utils.*;
import com.education.model.dto.ExamCount;
import com.education.model.dto.QuestionInfoAnswer;
import com.education.model.dto.StudentExamInfoDto;
import com.education.model.dto.TestPaperQuestionDto;
import com.education.model.entity.*;
import com.education.model.request.PageParam;
import com.education.model.request.StudentQuestionRequest;
import com.education.model.response.*;
import com.jfinal.kit.Kv;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/**
 * @author zengjintao
 * @version 1.0
 * @create_at 2020/11/22 16:12
 */
@Service
public class ExamInfoService extends BaseService<ExamInfoMapper, ExamInfo> {

    @Autowired
    private StudentQuestionAnswerService studentQuestionAnswerService;
    @Autowired
    private QuestionInfoService questionInfoService;
    @Autowired
    private TestPaperInfoService testPaperInfoService;
    @Autowired
    private ExamMonitorService examMonitorService;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private TestPaperInfoSettingService testPaperInfoSettingService;
    @Autowired
    private QueueManager queueManager;


    /**
     * ??????????????????
     * @param pageParam
     * @param studentExamInfoDto
     * @return
     */
    public PageInfo<StudentExamInfoDto> selectExamInfoList(PageParam pageParam, StudentExamInfoDto studentExamInfoDto) {
        Page<StudentExamInfoDto> page = new Page(pageParam.getPageNumber(), pageParam.getPageSize());
        return selectPage(baseMapper.selectExamList(page, studentExamInfoDto));
    }


    /**
     * ??????????????????????????????
     * @param pageParam
     * @param studentExamInfoDto
     * @return
     */
    public PageInfo<StudentExamInfoDto> selectStudentExamInfoList(PageParam pageParam, StudentExamInfoDto studentExamInfoDto) {
        studentExamInfoDto.setStudentId(getStudentId());
        Page<StudentExamInfoDto> page = new Page(pageParam.getPageNumber(), pageParam.getPageSize());
        return selectPage(baseMapper.selectStudentExamList(page, studentExamInfoDto));
    }


    @Transactional
    public QuestionCorrectResponse commitTestPaperInfoQuestion(StudentQuestionRequest studentQuestionRequest) {
        QuestionCorrectResponse questionCorrectResponse = new QuestionCorrectResponse();
        TestPaperInfoSetting testPaperInfoSetting = null;
        Integer testPaperInfoId = studentQuestionRequest.getTestPaperInfoId();
        // ????????????????????????????????????????????????
        testPaperInfoSetting = cacheBean.get(CacheKey.PAPER_INFO_SETTING, testPaperInfoId);

        RLock lock = redissonClient.getLock(CacheKey.PAPER_INFO_SETTING_LOCK);
        if (testPaperInfoSetting == null) {
            try {
                lock.lock();
                // ??????????????????????????????????????????
                testPaperInfoSetting = cacheBean.get(CacheKey.PAPER_INFO_SETTING, testPaperInfoId);
                if (ObjectUtils.isEmpty(testPaperInfoSetting)) {
                    testPaperInfoSetting = testPaperInfoSettingService.selectByTestPaperInfoId(testPaperInfoId);
                    cacheBean.put(CacheKey.PAPER_INFO_SETTING, testPaperInfoId, testPaperInfoSetting, CacheTime.ONE_DAY_SECOND);
                }
            } finally {
                lock.unlock();
            }
        }
        ExamInfo examInfo = new ExamInfo();
        studentQuestionRequest.setStudentId(getStudentId());
        QuestionCorrect questionCorrect = new SystemQuestionCorrect(studentQuestionRequest, examInfo,
                queueManager, getQuestionAnswerInfoByPaperId(testPaperInfoId));
        questionCorrect.correctStudentQuestion();
        int commitAfterType = EnumConstants.CommitAfterType.SHOW_MARK_AFTER_CORRECT.getValue();
        if (testPaperInfoSetting != null) {
            commitAfterType = testPaperInfoSetting.getCommitAfterType();
        }
        examInfo = questionCorrect.getExamInfo();
        // ?????????????????????????????????????????????, ????????????rabbitmq ?????????????????????????????????????????????
        if (commitAfterType == EnumConstants.CommitAfterType.SHOW_MARK_NOW.getValue()) {
            // redis ?????????????????????
            Set<ZSetOperations.TypedTuple<StudentInfo>> tuples = new HashSet<>();
            Integer systemMark = examInfo.getSystemMark();
            StudentInfo studentInfo = new StudentInfo();
            studentInfo.setId(getStudentId());
            studentInfo.setName(getStudentInfo().getName());
            DefaultTypedTuple tuple = new DefaultTypedTuple(studentInfo, systemMark.doubleValue());
            tuples.add(tuple);
            String sortKey = CacheKey.EXAM_SORT_KEY + testPaperInfoId;
            redisTemplate.opsForZSet().add(sortKey, tuples);
            // ???????????????1-10?????????
            Set<StudentInfo> studentScore = redisTemplate.opsForZSet().reverseRange(sortKey, 1, 10);
            questionCorrectResponse.setStudentInfoSet(studentScore);
            questionCorrectResponse.setStudentMark(systemMark); // ??????????????????
        }
        examMonitorService.removeStudent(getStudentId(), testPaperInfoId); // ??????????????????
        questionCorrectResponse.setExamTime(questionCorrect.getExamInfo().getExamTime());
        return questionCorrectResponse;
    }


    /**
     * ????????????????????????????????? (????????????????????????????????????????????????)
     * @param testPaperInfoId
     * @return
     */
    public Map<Integer, String> getQuestionAnswerInfoByPaperId(Integer testPaperInfoId) {
        List<TestPaperQuestionDto> testPaperQuestionDtoList = testPaperInfoService.selectPaperQuestionListByCache(testPaperInfoId); // cacheBean.get(CacheKey.TEST_PAPER_INFO_CACHE, testPaperInfoId);
        Map<Integer, String> questionAnswerInfo = new HashMap<>();
        testPaperQuestionDtoList.forEach(questionItem -> {
            questionAnswerInfo.put(questionItem.getQuestionInfoId(), questionItem.getAnswer());
        });
        return questionAnswerInfo;
    }


    /**
     * ??????????????????????????????
     * @param
     * @param
     * @param
     * @return
     */
   /* private Integer batchSaveStudentQuestionAnswer(StudentQuestionRequest studentQuestionRequest,
                                                Integer studentId, ExamInfo examInfo) {
        Integer testPaperInfoId = studentQuestionRequest.getTestPaperInfoId();
        Date now = new Date();
        List<StudentQuestionAnswer> studentQuestionAnswerList = new ArrayList<>();
        int systemMark = 0;
        int subjectiveQuestionNumber = 0; // ???????????????
        int rightNumber = 0;
        int errorNumber = 0;
        int teacherMark = 0;
        int teacherErrorNumber = 0; // ??????????????????????????????????????????
        int questionNumber = studentQuestionRequest.getQuestionAnswerList().size();
        List<StudentWrongBook> studentWrongBookList = new ArrayList<>(); // ????????????????????????
        for (QuestionAnswer item : studentQuestionRequest.getQuestionAnswerList()) {
            StudentQuestionAnswer studentQuestionAnswer = new StudentQuestionAnswer();
            studentQuestionAnswer.setQuestionInfoId(item.getQuestionInfoId());
            studentQuestionAnswer.setStudentId(studentId);
            studentQuestionAnswer.setQuestionPoints(item.getQuestionMark());
            String studentAnswer = item.getStudentAnswer();
            // ?????????????????????????????? // ?????????????????????????????????
            if (QuestionCorrect.isObjectiveQuestion(item.getQuestionType()) && !studentQuestionRequest.isTeacherCorrectFlag()) {
                String questionAnswer = item.getAnswer().replaceAll(",", "");
                String studentAnswerProxy = null;
                String questionAnswerProxy = ObjectUtils.charSort(questionAnswer);
                if (ObjectUtils.isNotEmpty(studentAnswer)) {
                    studentAnswerProxy = ObjectUtils.charSort(studentAnswer.replaceAll(",", ""));
                }
                // ??????????????????????????? ??????????????????  ????????????????????????????????????B,A,C, ????????????A,B,C
                if (questionAnswerProxy.equals(studentAnswerProxy)) {
                    studentQuestionAnswer.setMark(item.getQuestionMark());
                    systemMark += item.getQuestionMark();
                    rightNumber++;
                    studentQuestionAnswer.setCorrectStatus(EnumConstants.CorrectStatus.RIGHT.getValue());
                } else {
                    studentWrongBookList.add(studentWrongBookService.newStudentWrongBook(studentId, item));
                    errorNumber++;
                    studentQuestionAnswer.setCorrectStatus(EnumConstants.CorrectStatus.ERROR.getValue());
                }
            } else {
                if (studentQuestionRequest.isTeacherCorrectFlag()) {
                    // ?????????????????????????????????????????????
                    if (ObjectUtils.isEmpty(item.getStudentAnswer())) {
                        continue;
                    }
                    studentQuestionAnswer.setMark(item.getStudentMark());
                    teacherMark += item.getStudentMark();
                    // ?????????????????????????????????
                    if (item.isErrorQuestionFlag()) {
                        teacherErrorNumber++; // ?????????+1
                        studentQuestionAnswer.setCorrectStatus(EnumConstants.CorrectStatus.ERROR.getValue());
                        studentWrongBookList.add(studentWrongBookService.newStudentWrongBook(studentId, item));
                    } else {
                        studentQuestionAnswer.setCorrectStatus(EnumConstants.CorrectStatus.CORRECTED.getValue());
                    }
                } else {
                    // ?????????????????????????????????????????? // ?????????+1
                    if (ObjectUtils.isEmpty(item.getStudentAnswer())) {
                        studentQuestionAnswer.setCorrectStatus(EnumConstants.CorrectStatus.ERROR.getValue());
                        studentWrongBookList.add(studentWrongBookService.newStudentWrongBook(studentId, item));
                        errorNumber++;
                    } else {
                        studentQuestionAnswer.setCorrectStatus(EnumConstants.CorrectStatus.CORRECT_RUNNING.getValue());
                    }
                }
                subjectiveQuestionNumber++;
            }

            studentQuestionAnswer.setStudentAnswer(studentAnswer);
            studentQuestionAnswer.setCreateDate(now);
            studentQuestionAnswer.setComment(item.getComment());
            studentQuestionAnswerList.add(studentQuestionAnswer);
        }

        if (studentWrongBookList.size() > 0) {
            studentWrongBookService.saveBatch(studentWrongBookList); // ??????????????????
        }

        if (!studentQuestionRequest.isTeacherCorrectFlag()) {
            // ?????????????????????
            examInfo.setStudentId(studentId);
            examInfo.setQuestionNumber(questionNumber);
            examInfo.setTestPaperInfoId(testPaperInfoId);
            examInfo.setCreateDate(now);
            examInfo.setSystemMark(systemMark);
            examInfo.setMark(systemMark);
            examInfo.setSubjectiveQuestionNumber(subjectiveQuestionNumber);
            long examTime = studentQuestionRequest.getExamTime();
            examInfo.setExamTime(DateUtils.getDate(examTime));
            examInfo.setExamTimeLongValue(examTime);
            if (subjectiveQuestionNumber == 0) { // ???????????????????????????????????????????????????0????????????????????????????????????
                examInfo.setCorrectFlag(EnumConstants.Flag.YES.getValue());
                examInfo.setCorrectType(EnumConstants.CorrectType.SYSTEM.getValue());
            }
            examInfo.setRightNumber(rightNumber);
            examInfo.setErrorNumber(errorNumber);
            examInfo.setCreateDate(now);
            super.save(examInfo);
            // ????????????????????????
            testPaperInfoService.updateCacheExamNumber(examInfo.getTestPaperInfoId());

            examMonitorService.removeStudent(studentId, testPaperInfoId); // ??????????????????
        } else {
            // ??????????????????0??? ?????????????????????
            if (examInfo.getSubjectiveQuestionNumber() == 0) {
                examInfo.setCorrectType(EnumConstants.CorrectType.TEACHER.getValue());
            } else {
                // ?????? + ????????????
                examInfo.setCorrectType(EnumConstants.CorrectType.SYSTEM_AND_TEACHER.getValue());
            }
            examInfo.setTeacherMark(teacherMark);
            examInfo.setMark(examInfo.getSystemMark() + teacherMark);
            examInfo.setUpdateDate(now);
            examInfo.setErrorNumber(examInfo.getErrorNumber() + teacherErrorNumber);
            examInfo.setCorrectFlag(EnumConstants.Flag.YES.getValue());
            examInfo.setAdminId(getAdminUserId());
            super.updateById(examInfo);

            // ????????????????????????
        *//*    TaskParam taskParam = new TaskParam(WebSocketMessageTask.class);
            taskParam.put("message_type", EnumConstants.MessageType.EXAM_CORRECT.getValue());
            taskParam.put("sessionId", RequestUtils.getCookieValue(Constants.DEFAULT_SESSION_ID));
            taskParam.put("studentId", studentId);
            taskParam.put("testPaperInfoId", examInfo.getTestPaperInfoId());
            taskManager.pushTask(taskParam);*//*
        }
        studentQuestionAnswerList.stream().forEach(item -> item.setExamInfoId(examInfo.getId()));
        // ??????????????????????????????
        studentQuestionAnswerService.saveBatch(studentQuestionAnswerList);
        return examInfo.getId();
    }*/


    public QuestionGroupResponse selectExamQuestionAnswer(Integer studentId, Integer examInfoId) {
        StudentExamInfoDto studentExamInfoDto = this.getExamInfoById(examInfoId);
        List<QuestionInfoAnswer> examQuestionAnswerList = studentQuestionAnswerService
                .getQuestionAnswerByExamInfoId(studentId, examInfoId);
        List<QuestionGroupItemResponse> list = questionInfoService.groupQuestion(examQuestionAnswerList);
        ExamQuestionGroupResponse examQuestionResponse = new ExamQuestionGroupResponse();
        examQuestionResponse.setQuestionGroupItemResponseList(list);
        examQuestionResponse.setTotalQuestion(list.size());
        examQuestionResponse.setStudentExamInfoDto(studentExamInfoDto);
        return examQuestionResponse;
    }

    public StudentExamInfoDto getExamInfoById(Integer examInfoId) {
        return baseMapper.selectById(examInfoId);
    }

    /**
     * ??????????????????
     * @param studentQuestionRequest
     */
    @Transactional
    public void correctStudentExam(StudentQuestionRequest studentQuestionRequest) {
        ExamInfo examInfo = super.getById(studentQuestionRequest.getExamInfoId());
        if (examInfo.getCorrectFlag().intValue() == ResultCode.SUCCESS) {
            throw new BusinessException(new ResultCode(ResultCode.FAIL, "??????????????????"));
        }
        Integer studentId = studentQuestionRequest.getStudentId();

        // ???????????????????????????????????????
        studentQuestionAnswerService.deleteByExamInfoId(studentId, examInfo.getId());
        studentQuestionRequest.setTestPaperInfoId(examInfo.getTestPaperInfoId());

        QuestionCorrect questionCorrect = new TeacherQuestionCorrect(studentQuestionRequest, examInfo,
                getQuestionAnswerInfoByPaperId(studentQuestionRequest.getTestPaperInfoId()));
        questionCorrect.correctStudentQuestion();


        examInfo = questionCorrect.getExamInfo();
        examInfo.setAdminId(getAdminUserId());
        super.updateById(examInfo);

        // ????????????????????????
        TaskParam taskParam = new TaskParam(WebSocketMessageListener.class);
        taskParam.put("message_type", EnumConstants.MessageType.EXAM_CORRECT.getValue());
        taskParam.put("sessionId", RequestUtils.getCookieValue(SystemConstants.DEFAULT_SESSION_COOKIE_NAME));
        taskParam.put("studentId", studentId);
        taskParam.put("testPaperInfoId", examInfo.getTestPaperInfoId());
        taskManager.pushTask(taskParam);

        // this.batchSaveStudentQuestionAnswer(studentQuestionRequest, studentQuestionRequest.getStudentId(), examInfo);
    }

    public PageInfo<TestPaperInfoReport> selectExamReportList(PageParam pageParam, TestPaperInfo testPaperInfo) {
        Page<TestPaperInfoReport> page = new Page(pageParam.getPageNumber(), pageParam.getPageSize());
        return selectPage(baseMapper.selectExamReportList(page, testPaperInfo));
    }

    public ExamInfoDetail examDetailReport(Integer testPaperInfoId) {
        TestPaperInfo testPaperInfo = testPaperInfoService.getById(testPaperInfoId);
        Integer mark = testPaperInfo.getMark();
        double passMark = NumberUtils.doubleToBigDecimal(mark * SystemConstants.PASS_MARK_RATE);
        double niceMark = NumberUtils.doubleToBigDecimal(mark * SystemConstants.NICE_MARK_RATE);
        Kv params = Kv.create().set("testPaperInfoId", testPaperInfoId).set("passMark", passMark)
                .set("niceMark", niceMark);
        ExamInfoDetail examInfoDetail = baseMapper.selectExamInfoDetail(params);
        examInfoDetail.setPassExamMark(passMark);
        examInfoDetail.setNiceExamMark(niceMark);
        examInfoDetail.setExamTime(DateUtils.getDate(testPaperInfo.getExamTime()));
        examInfoDetail.setExamNumber(testPaperInfo.getExamNumber());
        return examInfoDetail;
    }

    public PageInfo<ExamInfoReport> getExamRankingList(PageParam pageParam, Integer testPaperInfoId) {
        Page<ExamInfoReport> page = new Page(pageParam.getPageNumber(), pageParam.getPageSize());
        return selectPage(baseMapper.selectExamListByTestPaperInfoId(page, testPaperInfoId));
    }

    /**
     * ?????????????????????????????????
     * @return
     */
    public List<ExamCount> selectExamInfoData() {
        Date now = new Date();
        String startTime = DateUtils.getDayBefore(DateUtils.getSecondDate(now), 7);
        String endTime = DateUtils.getDayBefore(DateUtils.getSecondDate(now), 1);
        Map params = new HashMap<>();
        // ?????????????????????????????????????????????
        params.put("startTime", startTime + " 00:00:00");
        params.put("endTime", endTime + " 23:59:59");

        List<ExamCount> dataList = baseMapper.countByDateTime(startTime, endTime);
        Map<String, Integer> dataTimeMap = new HashMap<>();
        dataList.forEach(data -> {
            String day = data.getDayGroup();
            Integer examNumber = data.getExamNumber();
            dataTimeMap.put(day, examNumber);
        });

        List<String> weekDateList = DateUtils.getSectionByOneDay(8);
        // ?????????????????????
        weekDateList.remove(weekDateList.size() - 1); // ?????????????????????????????????????????????
        List<ExamCount> resultDataList = new ArrayList<>();
        weekDateList.forEach(day -> {
            ExamCount item = new ExamCount();
            item.setDayGroup(day);
            item.setExamNumber(ObjectUtils.isNotEmpty(dataTimeMap.get(day)) ? dataTimeMap.get(day) : 0);
            resultDataList.add(item);
        });

        return resultDataList;
    }
}
