package kr.henein.api.repository;

import kr.henein.api.entity.S3File;
import kr.henein.api.enumCustom.S3EntityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface S3FileRepository extends JpaRepository<S3File,Long> {
    S3File findByFileUrl(String fileName);
    List<S3File> findByS3EntityType(S3EntityType s3EntityType);
    List<S3File> findAllByS3EntityTypeAndTypeId(S3EntityType s3EntityType, Long typeId);
}
