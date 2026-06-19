package com.setting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Setting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long userId;

    @Column
    private boolean screenToast;

    @Column
    private boolean osDesktop;

    @Column
    private boolean alertSound;

    @Column
    private boolean tts;


//    @Enumerated
//    private TtsVoice ttsVoice;
//
//    @Enumerated
//    private StretchCycle stretchCycle;

    @Column
    private int badAlertSec;
    @Column
    private boolean dedupAlert;

    public Setting( Long userId) {

        this.userId = userId;
        this.screenToast = true;
        this.osDesktop = true;
        this.alertSound = true;
        this.tts = true;
        this.badAlertSec = 5;
        this.dedupAlert = true;
    }
}
