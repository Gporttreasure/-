package com.education.business.correct;

import com.education.business.task.TaskManager;
import com.education.business.task.TaskParam;
import com.education.business.task.WebSocketMessageTask;
import com.education.common.constants.Constants;
import com.education.common.constants.EnumConstants;
import com.education.common.utils.RequestUtils;
import com.education.model.entity.ExamInfo;
import com.education.model.entity.StudentQuestionAnswer;
import com.education.model.entity.StudentWrongBook;
import com.education.model.request.QuestionAnswer;
import com.education.model.request.StudentQuestionRequest;
import java.util.ArrayList;
import java.util.List;


/**
 * @author zengjintao
 * @version 1.0
 * @create_at 2021/3/17 20:44
 */
public abstract class QuestionCorrect {

    private StudentQuestionRequest studentQuestionRequest;
    private TaskManager taskManager;
    protected ExamInfo examInfo;
    private Integer studentId;

    protected int correctQuestionNumber = 0; // 批改试题数量
    private int rightQuestionNumber = 0; // 答对题数
    private int errorQuestionNumber = 0; // 答错题数
    protected int subjectiveQuestionNumber = 0; // 主观题数量
    protected int objectiveQuestionNumber = 0; // 客观题数量
    private int questionNumber; // 试题总数

    private final List<StudentWrongBook> studentWrongBookList = new ArrayList<>(); // 存储学员考试错题

    protected final List<QuestionAnswer> questionAnswerList;

    public QuestionCorrect(StudentQuestionRequest studentQuestionRequest, ExamInfo examInfo) {
        this.studentQuestionRequest = studentQuestionRequest;
        this.questionNumber = studentQuestionRequest.getQuestionAnswerList().size();
        this.studentId = studentQuestionRequest.getStudentId();
        this.examInfo = examInfo;
        this.questionAnswerList = studentQuestionRequest.getQuestionAnswerList();
    }

    public void setTaskManager(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public int getRightQuestionNumber() {
        return rightQuestionNumber;
    }

    public void setRightQuestionNumber(int rightQuestionNumber) {
        this.rightQuestionNumber = rightQuestionNumber;
    }

    public int getErrorQuestionNumber() {
        return errorQuestionNumber;
    }

    public void setErrorQuestionNumber(int errorQuestionNumber) {
        this.errorQuestionNumber = errorQuestionNumber;
    }

    public int getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(int questionNumber) {
        this.questionNumber = questionNumber;
    }

    public static boolean isObjectiveQuestion(int questionType) {
        if (questionType == EnumConstants.QuestionType.SINGLE_QUESTION.getValue()
                || questionType == EnumConstants.QuestionType.MULTIPLE_QUESTION.getValue()
                || questionType == EnumConstants.QuestionType.JUDGMENT_QUESTION.getValue()) {
            return true;
        }
        return false;
    }

    public void addRightNumber() {
        this.rightQuestionNumber++;
    }

    public void addErrorNumber() {
        this.errorQuestionNumber++;
    }

    public List<StudentWrongBook> getStudentWrongBookList() {
        return studentWrongBookList;
    }

    protected void newStudentWrongBook(QuestionAnswer questionAnswer) {
        StudentWrongBook studentWrongBook = new StudentWrongBook(this.studentId,
                questionAnswer.getQuestionInfoId(),
                questionAnswer.getQuestionMark());
        studentWrongBook.setStudentAnswer(questionAnswer.getStudentAnswer());
        studentWrongBook.setCorrectStatus(EnumConstants.CorrectStatus.ERROR.getValue());
        this.addErrorNumber(); // 错题数加1
        this.studentWrongBookList.add(studentWrongBook);
    }

    protected StudentQuestionAnswer createStudentQuestionAnswer(QuestionAnswer questionAnswer) {
        StudentQuestionAnswer studentQuestionAnswer = new StudentQuestionAnswer();
        studentQuestionAnswer.setQuestionInfoId(questionAnswer.getQuestionInfoId());
        studentQuestionAnswer.setStudentId(this.studentId);
        studentQuestionAnswer.setQuestionPoints(questionAnswer.getQuestionMark());
        return studentQuestionAnswer;
    }

    protected void sendStudentMessage() {
       /* TaskParam taskParam = new TaskParam(WebSocketMessageTask.class);
        taskParam.put("message_type", EnumConstants.MessageType.EXAM_CORRECT.getValue());
        taskParam.put("sessionId", RequestUtils.getCookieValue(Constants.DEFAULT_SESSION_ID));
        taskParam.put("studentId", studentId);
        taskParam.put("testPaperInfoId", examInfo.getTestPaperInfoId());
        taskManager.pushTask(taskParam);*/
    }

    protected long getExamTime() {
        return studentQuestionRequest.getExamTime();
    }
    /**
     * 批改试题
     */
    public abstract void correctStudentQuestion();

    /**
     * 获取考试记录
     * @return
     */
    public abstract ExamInfo getExamInfo();
}
