package kr.henein.api.repository;

import kr.henein.api.entity.ComplaintEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintRepository extends JpaRepository<ComplaintEntity, Long> {
}
