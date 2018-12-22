package com.nex.task.actions.mule;

import java.util.ArrayList;
import java.util.Arrays;

import org.rspeer.runetek.adapter.component.Item;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.Bank;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.scene.Players;
import org.rspeer.ui.Log;

import com.nex.script.Nex;
import com.nex.script.Quest;
import com.nex.script.grandexchange.SellItemEvent;
import com.nex.script.grandexchange.SellItemHandler;
import com.nex.script.handler.TaskHandler;
import com.nex.script.items.GESellItem;
import com.nex.script.items.RSItem;
import com.nex.task.action.Action;
import com.nex.task.mule.PrepareForMuleDepositTask;
import com.nex.task.quests.tutorial.sections.QuestSection;

public class CheckIfWeShallSellItems extends Action {

	private static ArrayList<String> untradeableItems = new ArrayList<String>(
			Arrays.asList("Oak logs", "Shrimps", "Mind rune", "Air rune", "Fire rune", "Water rune", "Earth rune"));
	// used to check when last time we checked items was
	public static long last_check = 0;

	public static void execute() {
		int totalPrice = 0;
		if (Bank.isOpen()) {
			if (!Inventory.isEmpty()) {
				Bank.depositInventory();
			} else {
				ArrayList<SellItemEvent> itemsToSell = new ArrayList<SellItemEvent>();
				for (Item item : Bank.getItems()) {
					if(item.getId() == 995) {
						totalPrice += Bank.getCount(995);
					}else {
					RSItem rsItem = RSItem.getItem(item.getName(), item.getId());
					int itemValue = rsItem.getItemPrice() * Bank.getCount(item.getId());
					Log.fine(rsItem.getName() + ":  price: " + itemValue);
					if (itemValue > 5000 && TaskHandler.canSellItem(item) && (Quest.getQuestPoints() >= 7 || !untradeableItems.contains(item.getName()))) {
						itemsToSell.add((new SellItemEvent(new GESellItem(rsItem))));
						totalPrice += itemValue;
					}
					}
				}
				
				Log.fine("TOTAL PRICE:" + totalPrice);
				if(totalPrice > Nex.MULE_THRESHOLD) {
					itemsToSell.forEach(event ->{
						SellItemHandler.addItem(event);
					});
					TaskHandler.addTaskAndResetStack(new PrepareForMuleDepositTask());
				}
				last_check = System.currentTimeMillis();
			}
		}else {
			Bank.open();
			Time.sleepUntil(() -> Bank.isOpen(), 5000);
		}

	}

	public static long getNextCheckInMilli() {
		return ((last_check + 1800 * 1000));

	}

	public static long getTimeTilNextCheckInMinutes() {
		return (getNextCheckInMilli() - System.currentTimeMillis()) / 60000;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}

}