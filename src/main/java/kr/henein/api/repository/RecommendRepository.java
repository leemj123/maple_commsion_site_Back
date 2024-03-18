package kr.henein.api.repository;

import kr.henein.api.entity.BoardEntity;
import kr.henein.api.entity.RecommendEntity;
import kr.henein.api.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendRepository extends JpaRepository<RecommendEntity,Long> {
    RecommendEntity findByBoardEntityAndUserEntity(BoardEntity boardEntity, UserEntity userEntity);
}
