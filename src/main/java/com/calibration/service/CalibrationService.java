package com.calibration.service;


import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CalibrationService {

    private final JdbcTemplate jdbcTemplate;

    public void saveCalibration(Long memberId, Double goodAngle) {

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM calibration WHERE member_id = ?",
                Integer.class,
                memberId
        );

        if (count != null && count > 0) {

            jdbcTemplate.update(
                    """
                    UPDATE calibration
                    SET good_angle = ?
                    WHERE member_id = ?
                    """,
                    goodAngle,
                    memberId
            );

        } else {

            jdbcTemplate.update(
                    """
                    INSERT INTO calibration
                    (member_id, good_angle, turtle_threshold, created_at)
                    VALUES (?, ?, ?, NOW())
                    """,
                    memberId,
                    goodAngle,
                    7
            );
        }
    }
}