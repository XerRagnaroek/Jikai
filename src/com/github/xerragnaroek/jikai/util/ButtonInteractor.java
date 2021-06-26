package com.github.xerragnaroek.jikai.util;

import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

/**
 * 
 */
public interface ButtonInteractor {

	public String getIdentifier();

	public void handleButtonClick(String[] data, ButtonClickEvent event);
}
