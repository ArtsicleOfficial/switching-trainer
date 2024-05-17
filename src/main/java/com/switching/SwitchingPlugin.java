package com.switching;

import com.google.inject.Provides;
import javax.inject.Inject;
import javax.sound.sampled.*;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.*;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.http.api.item.ItemStats;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import static net.runelite.api.Experience.getLevelForXp;

@Slf4j
@PluginDescriptor(
	name = "Switching Trainer"
)
public class SwitchingPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private SwitchingConfig config;
	@Inject
	private ConfigManager configManager;
	@Inject
	private ChatMessageManager chatMessageManager;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private ClientThread clientThread;
	@Inject
	private ItemManager itemManager;

	private Item[] previousEquipment;

	private int ticksSinceStarted = 0;

	private int ticksSinceLastSwitch = 0;

	private int numberWaySwitch = 0;

	private ArrayList<Integer> waySwitchesToTrack = new ArrayList<>();

	public static final String DEFAULT_WAY_SWITCHES = "2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28";

	public int globalTickCounter = 0;

	public ArrayList<SwitchData> sessionData = new ArrayList<>();

	public File file = new File(RuneLite.RUNELITE_DIR, "switching-trainer/");
	public String textFile = "count.txt";
	public String textFileTemp = "count2.txt";
	public String textFileOld = "count_old.txt";

	public int switchingXP = 0;

	public XPOverlay overlay;
	public DropsOverlay dropsOverlay;

	public Clip levelup = null;

	//https://github.com/MarbleTurtle/MoreFireworks
	Integer[] fireWorks = {199,1388,1389};

	@Override
	protected void startUp()
	{
		loadWaySwitches(config.trackWaySwitches());
		switchingXP = loadFile();
		overlay = new XPOverlay(this);
		dropsOverlay = new DropsOverlay(this);

		try {
			levelup = AudioSystem.getClip();
			AudioInputStream sound = AudioSystem.getAudioInputStream(this.getClass().getResourceAsStream("/levelup.wav"));
			levelup.open(sound);
		} catch (LineUnavailableException | IOException | UnsupportedAudioFileException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void shutDown()
	{
		saveFile();

		overlayManager.remove(overlay);
		overlayManager.remove(dropsOverlay);

		if(levelup != null) {
			levelup.close();
		}
	}

	@Subscribe
	public void onClientShutdown(ClientShutdown clientShutdown) {
		saveFile();
	}

	public void saveFile() {
		file.mkdir();
		try {
			File written = new File(file, textFileTemp);
			DataOutputStream writer = new DataOutputStream(new FileOutputStream(written));
			writer.writeInt(switchingXP);
			writer.close();
			File previous = new File(file, textFile);
			File old = new File(file, textFileOld);
			if(old.exists()) { old.delete(); }
			if(previous.exists()) { previous.renameTo(old); }
			written.renameTo(new File(file, textFile));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public int loadFile() {
		try {
			DataInputStream reader = new DataInputStream(new FileInputStream(new File(file,textFile)));
			int out = reader.readInt();
			reader.close();
			return out;
		} catch (IOException e) {
			e.printStackTrace();
		}

		// If we failed to load the main file, perhaps we have a backup here.
		try {
			DataInputStream reader = new DataInputStream(new FileInputStream(new File(file,textFileOld)));
			int out = reader.readInt();
			reader.close();
			return out;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return 0;
	}

	public boolean loadWaySwitches(String commaSeparatedList) {
		String[] arr = commaSeparatedList.split(",");
		waySwitchesToTrack.clear();
		try {
			for (String s : arr) {
				if(s.length() == 0) continue;
				waySwitchesToTrack.add(Integer.parseInt(s));
			}
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged) {
		if(config.trackWaySwitches().split(",").length <= 0 || !loadWaySwitches(config.trackWaySwitches())) {
			configManager.setConfiguration("switchingTrainer","trackWaySwitches",DEFAULT_WAY_SWITCHES);
			loadWaySwitches(DEFAULT_WAY_SWITCHES);
			client.addChatMessage(ChatMessageType.CONSOLE,"","Switching Plugin Error: \"# Way Switches was invalid\"",null);
			client.addChatMessage(ChatMessageType.CONSOLE,"","Switching Plugin Loaded default value \"" + DEFAULT_WAY_SWITCHES + "\"",null);
		}
	}

	public void updateXPOverlay() {
		if(!config.skillingInterface()) {
			overlayManager.remove(overlay);
			overlayManager.remove(dropsOverlay);
			return;
		}
		overlayManager.add(overlay);
		overlayManager.add(dropsOverlay);
	}

	public void startTimer() {
		if(ticksSinceStarted > 0) return;
		ticksSinceStarted = 1;
	}

	public void updateTimer() {
		if(ticksSinceStarted <= 0) return;
		ticksSinceStarted++;
	}

	public void stopTimer() {
		if(ticksSinceStarted <= 0) return;
		ticksSinceStarted -= 100;
	}

	public int getTimer() {
		if(ticksSinceStarted < 0) return ticksSinceStarted + 100;
		return ticksSinceStarted;
	}

	public int getSlot(Item i) {
		ItemStats itemStats = itemManager.getItemStats(i.getId(), false);
		if(itemStats == null) {
			return 0;
		}
		if(itemStats.getEquipment() == null) {
			return 0;
		}
		return itemStats.getEquipment().getSlot();
	}

	@Subscribe
	public void onItemContainerChanged (ItemContainerChanged itemContainerChanged) {
		if(itemContainerChanged.getContainerId() == InventoryID.EQUIPMENT.getId()) {
			if(previousEquipment == null) {
				previousEquipment = client.getItemContainer(InventoryID.EQUIPMENT).getItems();
			}
			Item[] newItems = itemContainerChanged.getItemContainer().getItems();

			ArrayList<Integer> slots = new ArrayList<>();

			int amt = 0;

			//Gear put-ons
			for(Item i : newItems) {
				boolean isNew = true;
				for(Item j : previousEquipment) {
					if(i.getId() == j.getId()) {
						isNew = false;
						break;
					}
				}
				if(isNew) {
					amt++;
					slots.add(getSlot(i));
				}
			}
			//Gear take-offs
			mainLoop: for(Item i : previousEquipment) {
				boolean existedBefore = false;
				for(Integer integer : slots) {
					if(integer.equals(getSlot(i))) {
						continue mainLoop;
					}
				}
				for(Item j : newItems) {
					if(i.getId() == j.getId()) {
						existedBefore = true;
						break;
					}
				}
				if(!existedBefore) {
					amt++;
				}
			}
			previousEquipment = newItems;

			if(amt == 0) {
				return;
			}

			startTimer();
			numberWaySwitch += amt;
			ticksSinceLastSwitch = 0;
		}
	}

	private void giveXP(int amt) {
		amt *= Experience.getLevelForXp(switchingXP);
		if(Experience.getLevelForXp(switchingXP) != Experience.getLevelForXp(switchingXP + amt)) {
			client.addChatMessage(ChatMessageType.GAMEMESSAGE,"","Congratulations, you've just advanced your Switching level. You are now level " + Experience.getLevelForXp(switchingXP + amt) + ".",null);
			levelup.loop(0);
			client.getLocalPlayer().setGraphic(Experience.getLevelForXp(switchingXP + amt) == 99 ? fireWorks[2] : (Experience.getLevelForXp(switchingXP) % 10 == 0 ? fireWorks[1] : fireWorks[0]));
			client.getLocalPlayer().setSpotAnimFrame(0);
		}
		switchingXP += amt;
		dropsOverlay.xpDrops.add(new XPDrop(amt));
	}

	@Subscribe
	public void onGameTick(GameTick gameTick) {

		//Completed Switch
		if(ticksSinceLastSwitch > 0 && numberWaySwitch > 0) {
			ticksSinceStarted -= ticksSinceLastSwitch;
			stopTimer();
			if(waySwitchesToTrack.contains(numberWaySwitch) || config.trackAnyWaySwitches()) {
				SwitchData newData = new SwitchData(numberWaySwitch, getTimer(), globalTickCounter);
				giveXP(numberWaySwitch);
				sessionData.add(newData);
				if(config.trainingMode() == TrainingMode.BOTH || config.trainingMode() == TrainingMode.LIVE_FEEDBACK) {
					sendMessage(getRatingColor(newData.getSwitchesPerTick()),newData.toString() + " (" + getRating(newData.getSwitchesPerTick()) + ")");
				}
				saveFile();
			}
			numberWaySwitch = 0;
		}

		if(config.trainingMode() == TrainingMode.OCCASIONAL_UPDATES || config.trainingMode() == TrainingMode.BOTH) {
			int tickFrequency = Math.round(config.updateFrequency() * 60 / 0.6f);
			//No divide by zero here, sir
			if(tickFrequency == 0) tickFrequency = 1;
			if(globalTickCounter % tickFrequency == 0) {
				ArrayList<SwitchData> latestData = new ArrayList<>();
				for(int i = sessionData.size()-1; i >= 0; i--) {
					if(globalTickCounter - sessionData.get(i).timestamp < tickFrequency) {
						latestData.add(sessionData.get(i));
					} else {
						break;
					}
				}
				if(latestData.size() > 0) {
					int totalSwitches = 0, totalTicks = 0;

					for(SwitchData data : latestData) {
						totalSwitches += data.getNumberWaySwitch();
						totalTicks += data.getTicksTaken();
					}

					//in case this somehow wants to divide by zero
					float avgSwitchesPerTick;
					if(totalTicks == 0) {
						avgSwitchesPerTick = 0;
					} else {
						avgSwitchesPerTick = (float)totalSwitches/(float)totalTicks;
					}



					sendMessage(Color.CYAN,"*Switching Training Progress Update*");
					sendMessage(Color.DARK_GRAY,"Switching Level: " + getLevelForXp(switchingXP) + " (" + switchingXP + " xp)");
					sendMessage(Color.GRAY,"In the past " + config.updateFrequency() + " minutes, you've done:");
					sendMessage(Color.GRAY,totalSwitches + " switches using " + totalTicks + " ticks, making for");
					sendMessage(Color.LIGHT_GRAY,avgSwitchesPerTick + " switches per tick.");
					sendMessage(getRatingColor(avgSwitchesPerTick),"Average Rating: " + getRating(avgSwitchesPerTick));
				}
			}
		}

		updateTimer();
		ticksSinceLastSwitch++;
		globalTickCounter++;

		updateXPOverlay();
	}

	public void sendMessage(Color color, String message) {
		String last = new ChatMessageBuilder()
				.append(color, message)
				.build();
		chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.runeLiteFormattedMessage(last)
				.build());
	}

	public int lerp(int x, int y, float a) {
		return Math.round(x + (y-x)*a);
	}
	public Color lerpColor(Color x, Color y, float a) {
		a = Math.max(0,Math.min(1,a));
		return new Color(lerp(x.getRed(),y.getRed(),a), lerp(x.getGreen(),y.getGreen(),a), lerp(x.getBlue(),y.getBlue(),a));
	}
	//x->y->c
	public Color lerpThreeColors(Color x, Color y, Color c, float a) {
		a = Math.max(0,Math.min(1,a));
		if(a <= 0.5) {
			return new Color(lerp(x.getRed(),y.getRed(),a), lerp(x.getGreen(),y.getGreen(),a), lerp(x.getBlue(),y.getBlue(),a));
		} else {
			return new Color(lerp(y.getRed(),c.getRed(),a), lerp(y.getGreen(),c.getGreen(),a), lerp(y.getBlue(),c.getBlue(),a));
		}
	}

	public Color getRatingColor(float average) {
		return lerpThreeColors(Color.RED,Color.YELLOW,Color.GREEN,((average - 1) / 4.0f));
	}

	public String getRating(float average) {
		if(average <= 1.f) {
			return "F";
		} else if(average <= 1.1f) {
			return "D-";
		} else if(average <= 1.2f) {
			return "D";
		} else if(average <= 1.3f) {
			return "D+";
		} else if(average <= 1.4f) {
			return "C-";
		} else if(average <= 1.5f) {
			return "C";
		} else if(average <= 1.6f) {
			return "C+";
		} else if(average <= 1.75f) {
			return "B-";
		} else if(average <= 2.f) {
			return "B";
		} else if(average <= 2.5f) {
			return "B+";
		} else if(average <= 2.75f) {
			return "A-";
		} else if(average <= 3.25f) {
			return "A";
		} else if(average <= 4f) {
			return "A+";
		} else {
			return "God-like";
		}
	}

	@Provides
	SwitchingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SwitchingConfig.class);
	}
}
