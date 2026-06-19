package com.setting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Setting {

    @Id
    @GeneratedValue
    private int id;

    @Column
    private boolean screenToast;

    @Column
    private boolean osDesktop;

    @Column
    private boolean alertSound;

    @Column
    private boolean tts;

//    @Enumerated
//    private ttsVoice ttsVoice;
//
//    @Enumerated
//    private stretchCycle stretchCycle;

    @Column
    private int badAlertSec;

    @Column
    private boolean dedupAlert;


}
