package kr.henein.api.repository;

import kr.henein.api.entity.BoardTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoardTypeRepository extends JpaRepository<BoardTypeEntity, Long> {
    Optional<BoardTypeEntity> findByType(String type);
    boolean existsByType(String type);

}
