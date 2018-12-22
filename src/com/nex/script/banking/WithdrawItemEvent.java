package com.nex.script.banking;

import org.rspeer.runetek.adapter.component.Item;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.Bank;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.ui.Log;

import com.nex.communication.NexHelper;
import com.nex.communication.message.request.MuleRequest;
import com.nex.handler.gear.GearItem;
import com.nex.script.banking.BankEvent.Type;
import com.nex.script.grandexchange.BuyItemEvent;
import com.nex.script.grandexchange.BuyItemHandler;
import com.nex.script.handler.TaskHandler;
import com.nex.script.inventory.InventoryItem;
import com.nex.script.items.GEItem;
import com.nex.script.items.RequiredItem;
import com.nex.script.items.WithdrawItem;
import com.nex.task.mule.WithdrawFromPlayerTask;

public class WithdrawItemEvent extends BankEvent {

	private int amount;
	private int buyAmount;
	private int id;
	private RequiredItem requiredItem;

	public WithdrawItemEvent(RequiredItem requiredItem) {
		this.requiredItem = requiredItem;
		this.amount = requiredItem.getAmount();
		this.id = requiredItem.getItemID();
	}
	public WithdrawItemEvent(InventoryItem item) {
		this.requiredItem = new WithdrawItem(item.getItem().getId(), item.getAmount(), item.getItem().getName(), item.getBuyAmount());
		this.amount = item.getAmount();
		this.buyAmount = item.getBuyAmount();
		Log.fine("AMOUNT WHEN ADDED: " + amount);
		Log.fine("REQ ITEM: " + requiredItem.getItemName() + ":" + requiredItem.getAmount());
		this.id = item.getItem().getId();
	}
	
	public WithdrawItemEvent(GearItem item) {
		this.requiredItem = new WithdrawItem(item.getItem().getId(),1,item.getItem().getName(), 1);
		this.amount = requiredItem.getAmount();
		this.id = requiredItem.getItemID();
	}


	public void execute() {
		if (Bank.isOpen()) {
			if (Bank.getCount(id) >= amount) {
				Bank.withdraw(id, amount);
				Time.sleepUntil(() -> Inventory.contains(id), 5000);
			} else if (id == 995) {
				if(amount < 20000) {
					amount = 20000;
				}
				NexHelper.pushMessage(new MuleRequest("MULE_WITHDRAW:995:" + amount));
				
			} else {
				// TODO STOP SCRIPT
				Log.fine("bank does not contain" + id + " lets stop");
				BuyItemHandler.addItem(new BuyItemEvent(new GEItem(requiredItem)));
			}
		}else {
			Bank.open();
		}
	}

	@Override
	public Type getBankType() {
		return Type.WITHDRAW;
	}

	public RequiredItem getRequiredItem() {
		return requiredItem;
	}

	@Override
	public boolean isFinished() {
		if (Inventory.getCount(id) >= amount) {
			return true;
		}
		return false;
	}

}