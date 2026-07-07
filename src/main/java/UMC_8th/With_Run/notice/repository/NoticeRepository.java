package UMC_8th.With_Run.notice.repository;

import UMC_8th.With_Run.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findByReceiverId(Long userId);
}
