package me.hwproj.mafiagame.content.phases.doctor;

import me.hwproj.mafiagame.content.phases.abstractpick.PickAction;
import me.hwproj.mafiagame.phase.PlayerAction;

class DoctorAction extends PlayerAction {
    private final PickAction pickAction;

    public DoctorAction(PickAction pickAction) {
        this.pickAction = pickAction;
    }

    public PickAction getPickAction() {
        return pickAction;
    }
}
