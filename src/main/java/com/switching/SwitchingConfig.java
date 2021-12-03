package com.switching;

import net.runelite.client.config.*;

@ConfigGroup("switchingTrainer")
public interface SwitchingConfig extends Config
{

	@ConfigSection(
			name = "Occasional Training Mode",
			description = "Settings related to the occasional-update training mode",
			position = 1
	)
	String occasionalMode = "Occasional Training Mode";

	@ConfigItem(
			keyName = "trackAnyWaySwitches",
			name = "Track Any Way Switches",
			description = "If true, allows any # of switches instead of just the ones specified in 'Track # Way Switches (,)'"
	)
	default boolean trackAnyWaySwitches() {	return true; }

	@ConfigItem(
		keyName = "trackWaySwitches",
		name = "Track # Way Switches (,)",
		description = "Comma separated list of numbers to track certain switches (e.g. '2,4,8')"
	)
	default String trackWaySwitches() {	return SwitchingPlugin.DEFAULT_WAY_SWITCHES; }

	@ConfigItem(
			keyName = "skillingInterface",
			name = "Show Skilling Interface",
			description = "Whether or not to show the 'switching' skill for dopamine numbers."
	)
	default boolean skillingInterface() { return true; };

	@ConfigItem(
			keyName = "trainingMode",
			name = "Mode",
			description = "Whether to give instant feedback on switches via an overlay or to give occasional updates in chat."
	)
	default TrainingMode trainingMode() { return TrainingMode.BOTH; }

	@Range(
			min=0
	)
	@ConfigItem(
			keyName = "updateFrequency",
			name = "Update Frequency (min)",
			description = "How many minutes to update the player on their progress",
			section = occasionalMode
	)
	default int updateFrequency() { return 2; }
}
