package com.setting.repository;

import com.setting.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettingRepository extends JpaRepository<Setting, Integer> {

    Optional<Setting> findByUserId(Long id);
}
