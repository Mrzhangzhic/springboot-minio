package minio.until;

import lombok.extern.slf4j.Slf4j;
import minio.config.MinioProperties;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Autowired;
import io.minio.ObjectStat;
import io.minio.Result;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.iherus.codegen.qrcode.QrcodeConfig;
import org.iherus.codegen.qrcode.SimpleQrcodeGenerator;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.xmlpull.v1.XmlPullParserException;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
/**
 * @author zhangzhic
 * @version 1.0
 * @date 2020/11/28 9:56
 */
@Component
@Slf4j
public class MinioUtils {

    @Autowired
    private MinioProperties properties;
    @Autowired
    private MinioClient minioClient;
    /**
     * 文件上传
     *
     * @param file file
     */
    public void upload(MultipartFile file) {
        try {
            // 使用MinIO服务的URL，端口，Access key和Secret key创建一个MinioClient对象

            // 检查存储桶是否已经存在
            boolean isExist = minioClient.bucketExists(properties.getBucketName());
            if (!isExist) {
                // 创建一个名为test的存储桶，用于存储照片的zip文件。
                minioClient.makeBucket(properties.getBucketName());
            }
            InputStream inputStream = file.getInputStream();
            // 使用putObject上传一个文件到存储桶中。
            minioClient.putObject(properties.getBucketName(), file.getOriginalFilename(), inputStream, inputStream.available(), file.getContentType());
            //关闭
            inputStream.close();

        } catch (MinioException | NoSuchAlgorithmException | IOException | InvalidKeyException | XmlPullParserException e) {
            System.out.println("Error occurred: " + e);
        }
    }

    /**
     * 下载
     *
     * @param response response
     * @param fileName fileName
     */
    public void download(HttpServletResponse response, String fileName) {
        InputStream inputStream = null;
        try {
            ObjectStat stat = minioClient.statObject(properties.getBucketName(), fileName);
            inputStream = minioClient.getObject(properties.getBucketName(), fileName);
            response.setContentType(stat.contentType());
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, String.valueOf(StandardCharsets.UTF_8)));
            IOUtils.copy(inputStream, response.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取文件url
     *
     * @param objectName objectName
     * @return url
     */
    public String getObject(String objectName) {
        try {
            return minioClient.getObjectUrl(properties.getBucketName(), objectName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 获取所有
     *
     * @return
     */
    public List<Album> list() {
        try {
            List<Album> list = new ArrayList<Album>();
            Iterable<Result<Item>> results = minioClient.listObjects(properties.getBucketName());
            for (Result<Item> result : results) {
                Item item = result.get();
                // Create a new Album Object
                Album album = new Album();
                System.out.println(item.objectName());
                // Set the presigned URL in the album object
                album.setUrl(minioClient.getObjectUrl(properties.getBucketName(), item.objectName()));
                album.setDescription(item.objectName() + "," + item.lastModified() + ",size:" + item.size());
                // Add the album object to the list holding Album objects
                list.add(album);
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 文件删除
     *
     * @param name 文件名
     */
    public void delete(String name) {
        try {
            minioClient.removeObject(properties.getBucketName(), name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 上传生成的二维码
     */
    public void generator() {
        String uuid = UUID.randomUUID().toString();
        InputStream inputStream = bufferedImageToInputStream(qrcode(uuid));
        try {
            minioClient.putObject(properties.getBucketName(), uuid + ".png", bufferedImageToInputStream(qrcode(uuid)), inputStream.available(), MediaType.IMAGE_PNG_VALUE);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public BufferedImage qrcode(String content) {
        QrcodeConfig config = new QrcodeConfig()
                .setBorderSize(2)
                .setPadding(12)
                .setMasterColor("#00BFFF")
                .setLogoBorderColor("#B0C4DE")
                .setHeight(250).setWidth(250);
        return new SimpleQrcodeGenerator(config).setLogo("src/main/resources/logo.png").generate(content).getImage();
    }

    /**
     * @param image image
     * @return InputStream
     */
    public InputStream bufferedImageToInputStream(BufferedImage image) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", os);
            return new ByteArrayInputStream(os.toByteArray());
        } catch (IOException e) {
            e.fillInStackTrace();
        } finally {
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static class Album {
        private String url;
        private String description;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
