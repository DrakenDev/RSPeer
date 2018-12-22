package com.nex.script.banking;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.rspeer.runetek.api.commons.BankLocation;
import com.nex.script.walking.WalkTo;

import org.rspeer.runetek.api.movement.Movement;
import org.rspeer.runetek.api.movement.position.Area;
import org.rspeer.runetek.api.scene.Players;
import org.rspeer.ui.Log;

import com.nex.task.actions.mule.CheckIfWeShallSellItems;

public class BankHandler {
	private static Stack<BankEvent> bankEvents = new Stack<BankEvent>();

	public static void executeEvent(BankEvent bankEvent) {
		if(bankEvent.isFinished()) {
			bankEvents.remove(bankEvent);
		}else if(!playerInBank(bankEvent) && Movement.buildPath(bankEvent.getBankArea().getCenter()) != null) {
			WalkTo.execute(bankEvent.getBankArea().getCenter());
			Log.fine("lets walk");
		}else if(CheckIfWeShallSellItems.getTimeTilNextCheckInMinutes() <= 0) {
			CheckIfWeShallSellItems.execute();
		}else {
			bankEvent.execute();
		}
	}

	public static void addBankEvent(BankEvent event) {
		if (!bankEvents.contains(event)) {
			bankEvents.push(event);
		}
	}

	private static boolean playerInBank(BankEvent bankEvent) {
		if (bankEvent.getBankArea() != null && bankEvent.getBankArea().contains(Players.getLocal())) {
			return true;
		}
		if (Area.surrounding(BankLocation.GRAND_EXCHANGE.getPosition(), 20).contains(Players.getLocal())) {
			return true;
		}
		return false;
	}
	
	public static BankEvent getDepositEvent() {
		if(bankEvents != null && !bankEvents.isEmpty()) {
			for(BankEvent bankEvent: bankEvents) {
				if(bankEvent.getBankType() == BankEvent.Type.DEPOSIT) {
					return bankEvent;
				}
			}
		}
		return null;
	}

	public static BankEvent getWithdrawEvent() {
		if(bankEvents != null && !bankEvents.isEmpty()) {
			for(BankEvent bankEvent: bankEvents) {
				if(bankEvent.getBankType() == BankEvent.Type.WITHDRAW) {
					return bankEvent;
				}
			}
		}
		return null;
	}
}