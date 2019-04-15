package com.nex.task.actions.mule;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

import com.nex.communication.NexHelper;
import com.nex.communication.message.request.RequestAccountInfo;
import com.nex.script.Exchange;
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

	public static ArrayList<String> untradeableItems = new ArrayList<String>(
			Arrays.asList("Cowhide", "Oak logs", "Yew logs", "Shrimps", "Mind rune", "Air rune", "Fire rune", "Water rune", "Earth rune", "Feather", "Raw shrimps"));

	// used to check when last time we checked items was
	public static long last_check = 0;

	public static void execute(boolean weNeedMoneyNow, int min_sale) {
		int totalPrice = 0;
		if (Bank.isOpen()) {
			if (!Inventory.isEmpty()) {
				Bank.depositInventory();
				Time.sleepUntil(Inventory::isEmpty, 200, 3000);
			}
			if (Inventory.isEmpty()){
				ArrayList<SellItemEvent> itemsToSell = new ArrayList<SellItemEvent>();
				ArrayList<Integer> bankContents = new ArrayList<>();
				for (Item item : Bank.getItems()) {
					if(!item.isExchangeable()) continue;
					bankContents.add(item.getId());
				}
				Exchange.preCache(bankContents);
				for (Item item : Bank.getItems()) {
					if(!item.isExchangeable()) continue;
					if(item.getId() == 995) {
						totalPrice += Bank.getCount(995);
					}else {
						RSItem rsItem = new RSItem(item.getName(), item.getId());
						int itemValue = rsItem.getItemPrice() * item.getStackSize();
						Log.fine(rsItem.getName() + ":  price: " + itemValue);
						if (itemValue > min_sale && TaskHandler.canSellItem(item) && (Quest.getQuestPoints() >= 7 || !untradeableItems.contains(item.getName()))) {
							itemsToSell.add((new SellItemEvent(new GESellItem(rsItem))));
							totalPrice += itemValue;
						}
					}
				}

				Log.fine("TOTAL PRICE:" + totalPrice);
				int muleThreshold = Nex.MULE_THRESHOLD_NOW();
				if(totalPrice - Nex.MONEY_NEEDED > muleThreshold && NexHelper.activelyConnected()) {
					itemsToSell.forEach(event ->{
						SellItemHandler.addItem(event);
					});
					TaskHandler.addTaskAndResetStack(new PrepareForMuleDepositTask());
				}else if(weNeedMoneyNow){
					itemsToSell.forEach(event ->{
						SellItemHandler.addItem(event);
					});
				}
				last_check = System.currentTimeMillis();
			}
		}else {
			Bank.open();
			Time.sleepUntil(() -> Bank.isOpen(), 200, 5000);
		}
	}

	public static long getNextCheckInMilli() {
		return ((last_check + 1800 * 1000));
	}

	public static boolean dontSell = false;
	public static long getTimeTilNextCheckInMinutes() {
		if (dontSell || "MASTER_MULE".equalsIgnoreCase(RequestAccountInfo.account_type))
			return 120000;
		return (getNextCheckInMilli() - System.currentTimeMillis()) / 60000;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}

}
