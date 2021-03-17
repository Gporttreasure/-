package com.education.business.parser;

import com.education.common.constants.EnumConstants;
import com.education.model.dto.ExcelQuestionData;
import com.education.model.entity.QuestionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 解析txt 试题 模板导入
 * @author zengjintao
 * @version 1.0
 * @create_at 2021/1/16 14:43
 */
public class TxtQuestionImportResult extends QuestionImportResult {

    private final String QUESTION_CONTENT = "[题干]";
    private final String QUESTION_TYPE_VALUE = "[类型]";
    private final String QUESTION_ANSWER = "[答案]";
    private final String QUESTION_ANALYSIS = "[解析]";
    private final int TITLE_LENGTH = "[题干]".length();

    private final Logger logger = LoggerFactory.getLogger(TxtQuestionImportResult.class);

    public TxtQuestionImportResult(InputStream inputStream) {
        super(inputStream);
    }

    public TxtQuestionImportResult(MultipartFile file) throws IOException {
        super(file);
    }

    @Override
    public ExcelQuestionData readTemplate() {
        ExcelQuestionData excelQuestionData = new ExcelQuestionData();
        BufferedReader reader = new BufferedReader(new InputStreamReader(super.getInputStream()));
        String lineContent = null;
        List<QuestionInfo> questionInfoList = new ArrayList();
        try {
            QuestionInfo questionInfo = null;
            Integer questionType = null;

          //  int readTypeValue = 1;
            while ((lineContent = reader.readLine()) != null ) {
                QuestionImportParser excelQuestionParser = null;
                if (questionType != null) {
                    excelQuestionParser = QuestionImportParserManager.build()
                            .createExcelQuestionParser(questionType);
                }
                String tokenStart = lineContent.substring(0, TITLE_LENGTH);
                String content = lineContent.substring(TITLE_LENGTH, lineContent.length());

                // 解析题干
                if (tokenStart.startsWith(QUESTION_CONTENT)) {
                    questionInfo = new QuestionInfo();
                    questionInfo.setContent(content);
                    // 解析试题类型
                } else if (tokenStart.startsWith(QUESTION_TYPE_VALUE)) {
                 //   readTypeValue = 2;
                    for (EnumConstants.QuestionType item : EnumConstants.QuestionType.values()) {
                        if (item.getName().equals(content)) {
                            questionType = item.getValue();
                            break;
                        }
                    }

                    questionInfo.setQuestionType(questionType);
                } else if (tokenStart.startsWith(QUESTION_ANSWER)) {  // 解析试题答案
                 //   readTypeValue = 3;
                    String answer = excelQuestionParser.parseAnswerText(content);
                    questionInfo.setAnswer(answer);
                } else if (tokenStart.startsWith(QUESTION_ANALYSIS)) { // 解析试题选项
                 //   readTypeValue = 4;
                    String optionText = excelQuestionParser.parseOptionText(content);
                    questionInfo.setAnalysis(optionText);
                    questionInfoList.add(questionInfo);
                } else {
                  /*  if (readTypeValue == 1) {
                        questionInfo.setContent(questionInfo.getContent() + content);
                    } else if (readTypeValue == 2) {

                    } else if (readTypeValue == 3) {

                    } else if (readTypeValue == 4) {

                    }*/
                }

            }
            excelQuestionData.setQuestionInfoList(questionInfoList);
        } catch (Exception e) {
            logger.error("试题导入异常, 请检查txt 内容数据是否有换行", e);
        }
        return excelQuestionData;
    }
}
