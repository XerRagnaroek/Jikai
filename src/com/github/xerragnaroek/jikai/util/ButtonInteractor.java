package com.github.xerragnaroek.jikai.util;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

/**
 *
 */
public interface ButtonInteractor {

    String getIdentifier();

    void handleButtonClick(String[] data, ButtonInteractionEvent event);
}
