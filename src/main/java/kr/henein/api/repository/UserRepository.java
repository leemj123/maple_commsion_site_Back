package kr.henein.api.repository;

import kr.henein.api.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity,Long> {
    Optional<UserEntity> findByUserEmail(String email);
    boolean existsByUserEmail(String email);
    boolean existsByUserName(String userName);
    Optional<UserEntity> findByUserName(String userName);

}
