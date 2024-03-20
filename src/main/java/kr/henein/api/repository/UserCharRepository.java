package kr.henein.api.repository;

import kr.henein.api.entity.UserCharEntity;
import kr.henein.api.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCharRepository extends JpaRepository<UserCharEntity,Long> {

    UserCharEntity findByUserEntityAndId(UserEntity userEntity,Long id);
    List<UserCharEntity> findAllByUserEntity(UserEntity userEntity);

}
