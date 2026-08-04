package com.stats.testsupport;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Hibernate Statistics 로 쿼리 횟수와 엔티티 로딩 수를 센다.
 *
 * <p>응답 시간을 assert 하지 않는 이유: CI 러너 성능이 매번 달라서 flaky test 가 된다.
 * 대신 "세션이 5개일 때와 30개일 때 쿼리 수가 같은가" 같은 <b>성질</b>을 검증한다.
 * 이건 하드웨어와 무관하게 항상 같은 값이 나온다.
 *
 * <p>필수 프로퍼티:
 * <pre>spring.jpa.properties.hibernate.generate_statistics=true</pre>
 *
 * <p><b>주의:</b> 네이티브 쿼리를 JdbcTemplate 으로 빼면 이 카운터에 안 잡힌다.
 * DB 는 똑같이 때리는데 숫자만 줄어들어서 측정 자체가 거짓말이 된다.
 * 반드시 {@code @Query(nativeQuery = true)} 로 리포지토리에 두어야 한다.
 */
@Component
public class HibernateQueryCounter {

    private final Statistics statistics;
    private final EntityManager entityManager;

    public HibernateQueryCounter(EntityManagerFactory entityManagerFactory, EntityManager entityManager) {
        this.statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        this.entityManager = entityManager;
    }

    /**
     * @param queryCount      실행된 JDBC PreparedStatement 수
     * @param entityLoadCount 영속성 컨텍스트에 올라온 엔티티 수
     * @param elapsedMillis   참고용. assert 에는 쓰지 말고 로그로만 볼 것.
     */
    public record Measurement(long queryCount, long entityLoadCount, long elapsedMillis) {

        @Override
        public String toString() {
            return String.format("쿼리 %d회 / 엔티티 %d건 / %dms", queryCount, entityLoadCount, elapsedMillis);
        }
    }

    public <T> Result<T> measure(Supplier<T> action) {
        // 1차 캐시가 남아 있으면 두 번째 호출부터 엔티티 로딩 수가 줄어든다.
        // 측정 전에 반드시 비운다.
        entityManager.flush();
        entityManager.clear();
        statistics.clear();

        long startedAt = System.nanoTime();
        T value = action.get();
        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;

        Measurement measurement = new Measurement(
                statistics.getPrepareStatementCount(),
                statistics.getEntityLoadCount(),
                elapsedMillis
        );

        return new Result<>(value, measurement);
    }

    public record Result<T>(T value, Measurement measurement) {
    }
}
