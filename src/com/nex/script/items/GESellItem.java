package com.nex.script.items;


import com.nex.script.Exchange;
import com.nex.script.banking.WithdrawItemEvent;
import com.nex.script.grandexchange.SellItemEvent;
import com.nex.task.tanning.TanningTask;
import org.rspeer.runetek.api.Game;
import org.rspeer.ui.Log;

import java.util.HashMap;

public class GESellItem {

	private int itemPrice;
	private int originalItemPrice;
	private int timesLowered = 0;

	private boolean hasBeenWithdrawn = false;
	private boolean hasBeenSold = false; 
	private int itemID;
	private String itemName;

	public static HashMap<String, Integer> MinimimPrices = new HashMap<>();

	public GESellItem(RSItem item){
		this(item, null);

	}
	public GESellItem(RSItem item, Boolean sellCheap) {
		setItemID(item.getId());
		setItemName(item.getName());
		//setAmount(amount);
		if(sellCheap == null)
		{
			sellCheap = false;
//			lowerItemPrice();
			timesLowered = 0;
			if (TanningTask.ANY_HIDE_NAME.test(item.getName()))
				sellCheap = false;
		}
		if (sellCheap)
			setItemPrice(1);
		else {
			setItemPrice((int)(Exchange.getSellPrice(item.getId()) * 0.15));
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) return  false;
		if (!(obj instanceof GESellItem)) return false;
		GESellItem other = (GESellItem)obj;
		if(other.itemName == null || this.itemName == null) {
			if(other.itemID != this.itemID)
				return false;
		}
		else if(other.itemName != this.itemName) return false;
		return true;
	}
	
	public void setItemID(int id) {
		this.itemID = id;
	}
	public int getItemID() {
		return itemID;
	}
	
	public void setItemName(String name) {
		this.itemName = name;
	}
	public String getItemName() {
		return itemName;
	}

	public void setItemPrice(int price) {
		if(itemPrice == 0)
			originalItemPrice = price;
		this.itemPrice = price - 1;
	}

	
	public int getItemPrice() {
		return itemPrice;
	}
	
	public void setHasBeenWithdrawn(boolean hasBeenWithdrawn) {
		this.hasBeenWithdrawn = hasBeenWithdrawn;
	}

	public boolean hasBeenWithdrawnFromBank() {
		return hasBeenWithdrawn;
	}
	
	public void setHasBeenSold(boolean hasBeenSold) {
		this.hasBeenSold = hasBeenSold;
	}

	public boolean hasBeenSold() {
		return hasBeenSold;
	}

	public  int getTimesLowered(){
		return timesLowered;
	}

	public boolean lowerItemPrice() {
		if(this.itemPrice <= 1){
			this.itemPrice = 1;
			return false;
		}
		if(timesLowered > 3) //On the third try
			this.itemPrice /= 2;
		else
			this.itemPrice = (int) (this.itemPrice * 0.85) - 1;
		if(this.itemPrice < 1) this.itemPrice = 1;
		if(MinimimPrices.containsKey(itemName) && this.itemPrice < MinimimPrices.get(itemName)) {
			Log.fine("At minimum price for " + itemName + " with " + this.itemPrice);
			this.itemPrice = MinimimPrices.get(itemName);
			return false;
		}
		timesLowered++;
		return true;
	}
	public int getMinimumItemPrice(){
		if(MinimimPrices.containsKey(itemName)) {
			return MinimimPrices.get(itemName);
		}
		return 1;
	}

}
