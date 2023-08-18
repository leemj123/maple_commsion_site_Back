package com.example.demo.repository;

import com.example.demo.entity.UserCharEntity;
import com.example.demo.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCharRepository extends JpaRepository<UserCharEntity,Long> {

    Optional<UserCharEntity> findByNickName(String nickName);
    boolean existsByNickName(String nickName);
    List<UserCharEntity> findAllByUserEntity(UserEntity userEntity);

}
