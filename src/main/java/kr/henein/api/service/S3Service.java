package kr.henein.api.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;
import kr.henein.api.entity.S3File;
import kr.henein.api.enumCustom.S3EntityType;
import kr.henein.api.repository.S3FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    private final AmazonS3 amazonS3;
    private final S3FileRepository s3FileRepository;

    public String uploadImageBeforeSavedBoardEntity(MultipartFile image) throws IOException {
        List<MultipartFile> s3FileList = new ArrayList<>();
        s3FileList.add(image);
        List<S3File> resultList = this.existsFiles(s3FileList);
        //아직 글쓰기 중인 유저가 저장한 이미지로 연결되지 않은 것으로 표기함. 추후 글쓰기 완료되면 바꿔야함
        resultList.get(0).setEntityData(S3EntityType.NON_USED, null);
        s3FileRepository.save(resultList.get(0));

        return resultList.get(0).getFileUrl();
    }
    public void uploadImageUserPicture(MultipartFile image, Long id) throws IOException {
        List<MultipartFile> s3FileList = new ArrayList<>();

        s3FileList.add(image);
        List<S3File> newS3List = this.existsFiles(s3FileList);
        List<S3File> oldS3File = s3FileRepository.findAllByS3EntityTypeAndTypeId(S3EntityType.USER,id);

        if (oldS3File.size()!=0) {
            oldS3File.get(0).setEntityData(S3EntityType.NON_USED,null);
        }

        newS3List.get(0).setEntityData(S3EntityType.USER, id);
        s3FileRepository.save(newS3List.get(0));
    }

    private List<S3File> existsFiles(List<MultipartFile> imageList) throws IOException {
        List<S3File> List = new ArrayList<>();
        for (MultipartFile image : imageList) {
            String key = image.getOriginalFilename();
            if (amazonS3.doesObjectExist(bucket, key)) {
                continue;
            }
            String imageName = UUID.randomUUID() + "-" + key;
            InputStream inputStream = image.getInputStream();
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(image.getSize());
            amazonS3.putObject(new PutObjectRequest(bucket, imageName, inputStream, metadata));
            S3File file = S3File.builder()
                    .fileName(imageName)
                    .fileUrl(amazonS3.getUrl(bucket, imageName).toString())
                    .build();
            List.add(file);
        }
        return List;
    }
    public void changeImageInfo(List<String> imageNameList, S3EntityType s3EntityType, Long typeId ){
        List<S3File> s3FileList = new ArrayList<>();
        for (int i = 0; i < imageNameList.toArray().length; i++){
            S3File s3File = s3FileRepository.findByFileUrl(imageNameList.get(i));
            s3FileList.add(s3File);
        }
        s3FileList.stream().forEach(s3File -> s3File.setEntityData(s3EntityType, typeId));
    }
    public void deleteImage(List<S3File> nonUsedImageList) {
        try {
            for (S3File s3File : nonUsedImageList) {
                amazonS3.deleteObject(bucket, s3File.getFileName());
            }
        }catch (AmazonServiceException e) {
            throw new RuntimeException(e);
        }
    }
    public byte[] downloadImage(String key) throws IOException {
        byte[] content;
        final S3Object s3Object = amazonS3.getObject(bucket, key);
        final S3ObjectInputStream stream = s3Object.getObjectContent();
        try {
            content = IOUtils.toByteArray(stream);
            s3Object.close();
        } catch(final IOException ex) {
            throw new IOException("IO Error Message= " + ex.getMessage());
        }
        return content;
    }
}
