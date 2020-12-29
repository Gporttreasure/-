package com.education.api;

import com.baidu.ueditor.ConfigManager;
import com.education.common.utils.FileUtils;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;


/**
 * 管理后台启动类
 * @author zengjintao
 * @version 1.0
 * @create_at 2020/3/8 11:11
 */
@SpringBootApplication(scanBasePackages =
   {
       "com.education"
   }
)
@MapperScan("com.education.business.mapper")
@EnableCaching
public class EducationApiApplication {

    public static void main(String[] args) {
        ApplicationContext applicationContext = SpringApplication.run(EducationApiApplication.class, args);
        Environment environment = applicationContext.getEnvironment();
        String uploadPath = environment.getProperty("file.uploadPath");
        FileUtils.setUploadPath(uploadPath);
        String configFileName = environment.getProperty("ueditor.configFileName");
        ConfigManager.setConfigFileName(configFileName);
    }

}
