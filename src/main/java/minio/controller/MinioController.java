package minio.controller;

import lombok.extern.slf4j.Slf4j;
import minio.until.MinioUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * @author zhangzhic
 * @version 1.0
 * @date 2020/11/28 10:28
 */


/**
 *springboot+minio  实现文件的上传下载
 *
 */

@RestController
@Slf4j
@RequestMapping("/file")
public class MinioController {
    @Resource
    private MinioUtils minioUtils;
    @PostMapping(value = "/upload")
    public void upload(@RequestParam("file") MultipartFile file) {
        log.info("请求了我");
        minioUtils.upload(file);
    }

    @GetMapping(value = "/download")
    public void download(HttpServletResponse response, @RequestParam(value = "fileName") String fileName) throws UnsupportedEncodingException {
        minioUtils.download(response, fileName);
    }

    @GetMapping(value = "/list")
    public List<MinioUtils.Album> list()
    {
        log.info("获取list接口");
        return minioUtils.list();
    }

    @GetMapping(value = "/objectName")
    public String getObject(@RequestParam(value = "fileName") String fileName) {
        return minioUtils.getObject(fileName);
    }

    @DeleteMapping(value = "/delete/{name}")
    public void delete(@PathVariable String name) {
        minioUtils.delete(name);
    }

}
