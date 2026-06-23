package com.analysis.repository;

import com.analysis.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    @Query("SELECT a.id FROM Analysis a WHERE a.memberId = :memberId")
    List<Long> findIdsByMemberId(@Param("memberId") Long memberId);

    //전달 받은 memberId 에 해당되는 데이터 삭제 후 반환없음
    void deleteByMemberId(Long memberId);
}
