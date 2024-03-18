package kr.henein.api.repository;

import kr.henein.api.entity.BoardTypeEntity;
import kr.henein.api.enumCustom.BoardType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoardTypeRepository extends JpaRepository<BoardTypeEntity, Long> {
    Optional<BoardTypeEntity> findByName(String type);
    boolean existsByName(String name);

    List<BoardTypeEntity> findAllByBoardType(BoardType boardType);
    List<BoardTypeEntity> findAllByOrderByNumberingAsc ();


}
