package setting.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "calibration")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) //JPA용 기본 생성자
@AllArgsConstructor
@Builder
public class SettingCalibration {
    @Id // 기본키 PK
    @GeneratedValue(strategy = GenerationType.IDENTITY) // auto_increment
    private Long id;

    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId; // 회원 ID

    @Column(name = "good_angle")
    private Float goodAngle; // 정자세 기준 각도

    @Column(name = "turtle_threshold")
    private Float turtleThreshold; // 거북목 판정 임계 각도

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // 등록일

    @PrePersist // DB에 처음 저장되기 전에 자동 실행
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }


}
