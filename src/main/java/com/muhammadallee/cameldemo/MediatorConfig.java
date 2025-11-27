package com.muhammadallee.cameldemo;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MediatorConfig {
    private GlobalConfig global;
    private List<SystemConfig> systems;
}
