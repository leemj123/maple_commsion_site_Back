package kr.henein.api.repository;

import kr.henein.api.entity.BannerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BannerRepository extends JpaRepository<BannerEntity, Long> {
    List<BannerEntity> findAllByOrderByNumberingAsc();

}
