package kr.henein.api.repository;

import kr.henein.api.entity.BoardEntity;
import kr.henein.api.entity.BoardTypeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BoardRepository extends JpaRepository<BoardEntity, Long> {

//    @Query("SELECT b FROM BoardEntity b WHERE NOT b.boardType = kr.henein.api.enumCustom.BoardType.Notice ORDER BY b.id DESC")
//    Page<BoardEntity> findAllNotNotice(Pageable pageable);

    @Query(value = "SELECT * FROM henein.board " +
            "WHERE MATCH(title, text) AGAINST (?1 IN BOOLEAN MODE)",
            countQuery = "SELECT count(*) FROM henein.board " +
                    "WHERE MATCH(title, text) AGAINST (?1 IN BOOLEAN MODE)",
            nativeQuery = true)
    Page<BoardEntity> searchByText(String key, Pageable pageable);

    @Query(value = "SELECT * FROM henein.board " +
            "WHERE henein.board.type = ?2 AND MATCH(title, text) AGAINST (?1 IN BOOLEAN MODE)",
            countQuery = "SELECT count(*) FROM henein.board " +
                    "WHERE henein.board.type = ?2 AND MATCH(title, text) AGAINST (?1 IN BOOLEAN MODE)",
            nativeQuery = true)
    Page<BoardEntity> searchByTextWithType(String key, BoardTypeEntity boardType, Pageable pageable);
}
