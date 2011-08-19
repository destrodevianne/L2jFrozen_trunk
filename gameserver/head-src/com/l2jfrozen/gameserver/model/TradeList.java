/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package com.l2jfrozen.gameserver.model;

import java.util.List;
import java.util.logging.Logger;

import javolution.util.FastList;

import com.l2jfrozen.Config;
import com.l2jfrozen.gameserver.datatables.OfflineTradeTable;
import com.l2jfrozen.gameserver.datatables.sql.ItemTable;
import com.l2jfrozen.gameserver.model.actor.instance.L2ItemInstance;
import com.l2jfrozen.gameserver.model.actor.instance.L2PcInstance;
import com.l2jfrozen.gameserver.network.SystemMessageId;
import com.l2jfrozen.gameserver.network.serverpackets.InventoryUpdate;
import com.l2jfrozen.gameserver.network.serverpackets.ItemList;
import com.l2jfrozen.gameserver.network.serverpackets.StatusUpdate;
import com.l2jfrozen.gameserver.network.serverpackets.SystemMessage;
import com.l2jfrozen.gameserver.templates.L2EtcItemType;
import com.l2jfrozen.gameserver.templates.L2Item;
import com.l2jfrozen.gameserver.util.Util;

/**
 * @author Advi
 */
public class TradeList
{
	public class TradeItem
	{
		private int _objectId;
		private L2Item _item;
		private int _enchant;
		private int _count;
		private int _price;

		/** Augmented Item */
		private L2Augmentation _augmentation = null;

		public TradeItem(L2ItemInstance item, int count, int price)
		{
			_objectId = item.getObjectId();
			_item = item.getItem();
			_enchant = item.getEnchantLevel();
			_count = count;
			_price = price;
		}

		public TradeItem(L2Item item, int count, int price)
		{
			_objectId = 0;
			_item = item;
			_enchant = 0;
			_count = count;
			_price = price;
		}

		public TradeItem(TradeItem item, int count, int price)
		{
			_objectId = item.getObjectId();
			_item = item.getItem();
			_enchant = item.getEnchant();
			_count = count;
			_price = price;
		}

		public void setObjectId(int objectId)
		{
			_objectId = objectId;
		}

		public int getObjectId()
		{
			return _objectId;
		}

		public L2Item getItem()
		{
			return _item;
		}

		public void setEnchant(int enchant)
		{
			_enchant = enchant;
		}

		public int getEnchant()
		{
			return _enchant;
		}

		public void setCount(int count)
		{
			_count = count;
		}

		public int getCount()
		{
			return _count;
		}

		public void setPrice(int price)
		{
			_price = price;
		}

		public int getPrice()
		{
			return _price;
		}

		/**
		 * Returns whether this item is augmented or not
		 * 
		 * @return true if augmented
		 */
		public boolean isAugmented()
		{
			return _augmentation == null ? false : true;
		}
	}

	private static Logger _log = Logger.getLogger(TradeList.class.getName());

	private L2PcInstance _owner;
	private L2PcInstance _partner;
	private List<TradeItem> _items;
	private String _title;
	private boolean _packaged;

	private boolean _confirmed = false;
	private boolean _locked = false;

	public TradeList(L2PcInstance owner)
	{
		_items = new FastList<TradeItem>();
		_owner = owner;
	}

	public L2PcInstance getOwner()
	{
		return _owner;
	}

	public void setPartner(L2PcInstance partner)
	{
		_partner = partner;
	}

	public L2PcInstance getPartner()
	{
		return _partner;
	}

	public void setTitle(String title)
	{
		_title = title;
	}

	public String getTitle()
	{
		return _title;
	}

	public boolean isLocked()
	{
		return _locked;
	}

	public boolean isConfirmed()
	{
		return _confirmed;
	}

	public boolean isPackaged()
	{
		return _packaged;
	}

	public void setPackaged(boolean value)
	{
		_packaged = value;
	}

	/**
	 * Retrieves items from TradeList
	 */
	public TradeItem[] getItems()
	{
		return _items.toArray(new TradeItem[_items.size()]);
	}

	/**
	 * Returns the list of items in inventory available for transaction
	 * 
	 * @return L2ItemInstance : items in inventory
	 */
	public TradeList.TradeItem[] getAvailableItems(PcInventory inventory)
	{
		List<TradeList.TradeItem> list = new FastList<TradeList.TradeItem>();

		for(TradeList.TradeItem item : _items)
		{
			item = new TradeItem(item, item.getCount(), item.getPrice());
			inventory.adjustAvailableItem(item);
			list.add(item);
		}

		return list.toArray(new TradeList.TradeItem[list.size()]);
	}

	/**
	 * Returns Item List size
	 */
	public int getItemCount()
	{
		return _items.size();
	}

	/**
	 * Adjust available item from Inventory by the one in this list
	 * 
	 * @param item : L2ItemInstance to be adjusted
	 * @return TradeItem representing adjusted item
	 */
	public TradeItem adjustAvailableItem(L2ItemInstance item)
	{
		if(item.isStackable())
		{
			for(TradeItem exclItem : _items)
				if(exclItem.getItem().getItemId() == item.getItemId())
					if(item.getCount() <= exclItem.getCount())
						return null;
					else
						return new TradeItem(item, item.getCount() - exclItem.getCount(), item.getReferencePrice());
		}

		return new TradeItem(item, item.getCount(), item.getReferencePrice());
	}

	/**
	 * Adjust ItemRequest by corresponding item in this list using its <b>ObjectId</b>
	 * 
	 * @param item : ItemRequest to be adjusted
	 */
	public void adjustItemRequest(ItemRequest item)
	{
		for(TradeItem filtItem : _items)
			if(filtItem.getObjectId() == item.getObjectId())
			{
				if(filtItem.getCount() < item.getCount())
				{
					item.setCount(filtItem.getCount());
				}

				return;
			}

		item.setCount(0);
	}

	/**
	 * Adjust ItemRequest by corresponding item in this list using its <b>ItemId</b>
	 * 
	 * @param item : ItemRequest to be adjusted
	 */
	public void adjustItemRequestByItemId(ItemRequest item)
	{
		for(TradeItem filtItem : _items)
			if(filtItem.getItem().getItemId() == item.getItemId())
			{
				if(filtItem.getCount() < item.getCount())
				{
					item.setCount(filtItem.getCount());
				}

				return;
			}

		item.setCount(0);
	}

	/**
	 * Add simplified item to TradeList
	 * 
	 * @param objectId : int
	 * @param count : int
	 * @return
	 */
	public synchronized TradeItem addItem(int objectId, int count)
	{
		return addItem(objectId, count, 0);
	}

	/**
	 * Add item to TradeList
	 * 
	 * @param objectId : int
	 * @param count : int
	 * @param price : int
	 * @return
	 */
	public synchronized TradeItem addItem(int objectId, int count, int price)
	{
		if(isLocked())
		{
			Util.handleIllegalPlayerAction(_owner, "Player " + _owner.getName() + " Attempt to modify locked TradeList! ", Config.DEFAULT_PUNISH);
			_log.warning(_owner.getName() + ": Attempt to modify locked TradeList!");
			return null;
		}

		L2Object o = L2World.getInstance().findObject(objectId);

		if(o == null || !(o instanceof L2ItemInstance))
		{
			Util.handleIllegalPlayerAction(_owner, "Player " + _owner.getName() + " Attempt to add invalid item to TradeList! ",  Config.DEFAULT_PUNISH);
			_log.warning(_owner.getName() + ": Attempt to add invalid item to TradeList!");
			return null;
		}

		L2ItemInstance item = (L2ItemInstance) o;

		if(!item.isTradeable() || (getOwner().isGM() && Config.GM_TRADE_RESTRICTED_ITEMS)
			|| item.getItemType() == L2EtcItemType.QUEST)
			return null;

		if(count > item.getCount())
			return null;

		if(!item.isStackable() && count > 1)
		{
			_log.warning(_owner.getName() + ": Attempt to add non-stackable item to TradeList with count > 1!");
			return null;
		}

		for(TradeItem checkitem : _items)
		{
			if(checkitem.getObjectId() == objectId)
				return null;
		}

		TradeItem titem = new TradeItem(item, count, price);
		_items.add(titem);

		// If Player has already confirmed this trade, invalidate the confirmation
		invalidateConfirmation();

		item = null;
		o = null;

		return titem;
	}

	/**
	 * Add item to TradeList
	 * 
	 * @param objectId : int
	 * @param count : int
	 * @param price : int
	 * @return
	 */
	public synchronized TradeItem addItemByItemId(int itemId, int count, int price)
	{
		if(isLocked())
		{
			Util.handleIllegalPlayerAction(_owner, "Player " + _owner.getName() + " Attempt to modify locked TradeList! Banned ",  Config.DEFAULT_PUNISH);
			_log.warning(_owner.getName() + ": Attempt to modify locked TradeList!");
			return null;
		}

		L2Item item = ItemTable.getInstance().getTemplate(itemId);

		if(!_owner.validateItemManipulationByItemId(itemId, "Modify TradeList"))
		{
			Util.handleIllegalPlayerAction(_owner, "Player " + _owner.getName() + " Attempt to modify TradeList without valid conditions! ",  Config.DEFAULT_PUNISH);
			_log.warning(_owner.getName() + ": Attempt to modify TradeList without valid conditions!");
			return null;
		}
		
		
		if(item == null)
		{
			Util.handleIllegalPlayerAction(_owner, "Player " + _owner.getName() + " Attempt to add invalid item to TradeList! Banned ",  Config.DEFAULT_PUNISH);
			_log.warning(_owner.getName() + ": Attempt to add invalid item to TradeList!");
			return null;
		}

		if(!item.isTradeable() || item.getItemType() == L2EtcItemType.QUEST)
			return null;

		if(!item.isStackable() && count > 1)
		{
			_log.warning(_owner.getName() + ": Attempt to add non-stackable item to TradeList with count > 1!");
			return null;
		}

		TradeItem titem = new TradeItem(item, count, price);
		_items.add(titem);

		// If Player has already confirmed this trade, invalidate the confirmation
		invalidateConfirmation();

		item = null;

		return titem;
	}

	/**
	 * Remove item from TradeList
	 * 
	 * @param objectId : int
	 * @param count : int
	 * @return
	 */
	public synchronized TradeItem removeItem(int objectId, int itemId, int count)
	{
		if(isLocked())
		{
			Util.handleIllegalPlayerAction(_owner, "Player " + _owner.getName() + " Attempt to modify locked TradeList! Banned ",  Config.DEFAULT_PUNISH);
			_log.warning(_owner.getName() + ": Attempt to modify locked TradeList!");
			return null;
		}

		for(TradeItem titem : _items)
			if(titem.getObjectId() == objectId || titem.getItem().getItemId() == itemId)
			{
				// If Partner has already confirmed this trade, invalidate the confirmation
				if(_partner != null)
				{
					TradeList partnerList = _partner.getActiveTradeList();
					if(partnerList == null)
					{
						_log.warning(_partner.getName() + ": Trading partner (" + _partner.getName() + ") is invalid in this trade!");
						return null;
					}
					partnerList.invalidateConfirmation();
					partnerList = null;
				}

				// Reduce item count or complete item
				if(count != -1 && titem.getCount() > count)
				{
					titem.setCount(titem.getCount() - count);
				}
				else
				{
					_items.remove(titem);
				}

				return titem;
			}

		return null;
	}

	/**
	 * Update items in TradeList according their quantity in owner inventory
	 */
	public synchronized void updateItems()
	{
		for(TradeItem titem : _items)
		{
			L2ItemInstance item = _owner.getInventory().getItemByObjectId(titem.getObjectId());

			if(item == null || titem.getCount() < 1)
			{
				removeItem(titem.getObjectId(), -1, -1);
			}
			else if(item.getCount() < titem.getCount())
			{
				titem.setCount(item.getCount());
			}

			item = null;
		}
	}

	/**
	 * Lockes TradeList, no further changes are allowed
	 */
	public void lock()
	{
		_locked = true;
	}

	/**
	 * Clears item list
	 */
	public void clear()
	{
		_items.clear();
		_locked = false;
	}

	/**
	 * Confirms TradeList
	 * 
	 * @return : boolean
	 */
	public boolean confirm()
	{
		if(_confirmed)
			return true; // Already confirmed

		// If Partner has already confirmed this trade, proceed exchange
		if(_partner != null)
		{
			TradeList partnerList = _partner.getActiveTradeList();
			if(partnerList == null)
			{
				_log.warning(_partner.getName() + ": Trading partner (" + _partner.getName() + ") is invalid in this trade!");
				return false;
			}

			// Synchronization order to avoid deadlock
			TradeList sync1, sync2;
			if(getOwner().getObjectId() > partnerList.getOwner().getObjectId())
			{
				sync1 = partnerList;
				sync2 = this;
			}
			else
			{
				sync1 = this;
				sync2 = partnerList;
			}

			synchronized (sync1)
			{
				synchronized (sync2)
				{
					_confirmed = true;
					if(partnerList.isConfirmed())
					{
						partnerList.lock();
						lock();

						if(!partnerList.validate())
							return false;

						if(!validate())
							return false;

						doExchange(partnerList);
					}
					else
					{
						_partner.onTradeConfirm(_owner);
					}
				}
			}

			partnerList = null;
			sync1 = null;
			sync2 = null;
		}
		else
		{
			_confirmed = true;
		}

		return _confirmed;
	}

	/**
	 * Cancels TradeList confirmation
	 */
	public void invalidateConfirmation()
	{
		_confirmed = false;
	}

	/**
	 * Validates TradeList with owner inventory
	 */
	private boolean validate()
	{
		// Check for Owner validity
		if(_owner == null || L2World.getInstance().findObject(_owner.getObjectId()) == null)
		{
			_log.warning("Invalid owner of TradeList");
			return false;
		}

		// Check for Item validity
		for(TradeItem titem : _items)
		{
			L2ItemInstance item = _owner.checkItemManipulation(titem.getObjectId(), titem.getCount(), "transfer");

			if(item == null || titem.getCount() < 1)
			{
				//_log.warning(_owner.getName() + ": Invalid Item in TradeList");
				return false;
			}

			item = null;
		}

		return true;
	}

	/**
	 * Transfers all TradeItems from inventory to partner
	 */
	private boolean TransferItems(L2PcInstance partner, InventoryUpdate ownerIU, InventoryUpdate partnerIU)
	{
		for(TradeItem titem : _items)
		{
			L2ItemInstance oldItem = _owner.getInventory().getItemByObjectId(titem.getObjectId());
			if(oldItem == null)
				return false;

			L2ItemInstance newItem = _owner.getInventory().transferItem("Trade", titem.getObjectId(), titem.getCount(), partner.getInventory(), _owner, _partner);
			if(newItem == null)
				return false;

			// Add changes to inventory update packets
			if(ownerIU != null)
			{
				if(oldItem.getCount() > 0 && oldItem != newItem)
				{
					ownerIU.addModifiedItem(oldItem);
				}
				else
				{
					ownerIU.addRemovedItem(oldItem);
				}
			}

			if(partnerIU != null)
			{
				if(newItem.getCount() > titem.getCount())
				{
					partnerIU.addModifiedItem(newItem);
				}
				else
				{
					partnerIU.addNewItem(newItem);
				}
			}

			oldItem = null;
			newItem = null;
		}
		return true;
	}

	/**
	 * Count items slots
	 */

	public int countItemsSlots(L2PcInstance partner)
	{
		int slots = 0;

		for(TradeItem item : _items)
		{
			if(item == null)
			{
				continue;
			}

			L2Item template = ItemTable.getInstance().getTemplate(item.getItem().getItemId());
			if(template == null)
			{
				continue;
			}

			if(!template.isStackable())
			{
				slots += item.getCount();
			}
			else if(partner.getInventory().getItemByItemId(item.getItem().getItemId()) == null)
			{
				slots++;
			}

			template = null;
		}

		return slots;
	}

	/**
	 * Calc weight of items in tradeList
	 */

	public int calcItemsWeight()
	{
		int weight = 0;

		for(TradeItem item : _items)
		{
			if(item == null)
			{
				continue;
			}

			L2Item template = ItemTable.getInstance().getTemplate(item.getItem().getItemId());
			if(template == null)
			{
				continue;
			}

			weight += item.getCount() * template.getWeight();
			template = null;
		}

		return weight;
	}

	/**
	 * Proceeds with trade
	 */
	private void doExchange(TradeList partnerList)
	{
		boolean success = false;
		// check weight and slots
		if(!getOwner().getInventory().validateWeight(partnerList.calcItemsWeight()) || !partnerList.getOwner().getInventory().validateWeight(calcItemsWeight()))
		{
			partnerList.getOwner().sendPacket(new SystemMessage(SystemMessageId.WEIGHT_LIMIT_EXCEEDED));
			getOwner().sendPacket(new SystemMessage(SystemMessageId.WEIGHT_LIMIT_EXCEEDED));
		}
		else if(!getOwner().getInventory().validateCapacity(partnerList.countItemsSlots(getOwner())) || !partnerList.getOwner().getInventory().validateCapacity(countItemsSlots(partnerList.getOwner())))
		{
			partnerList.getOwner().sendPacket(new SystemMessage(SystemMessageId.SLOTS_FULL));
			getOwner().sendPacket(new SystemMessage(SystemMessageId.SLOTS_FULL));
		}
		else
		{
			// Prepare inventory update packet
			InventoryUpdate ownerIU = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();
			InventoryUpdate partnerIU = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();

			// Transfer items
			partnerList.TransferItems(getOwner(), partnerIU, ownerIU);
			TransferItems(partnerList.getOwner(), ownerIU, partnerIU);

			// Send inventory update packet
			if(ownerIU != null)
			{
				_owner.sendPacket(ownerIU);
			}
			else
			{
				_owner.sendPacket(new ItemList(_owner, false));
			}

			if(partnerIU != null)
			{
				_partner.sendPacket(partnerIU);
			}
			else
			{
				_partner.sendPacket(new ItemList(_partner, false));
			}

			// Update current load as well
			StatusUpdate playerSU = new StatusUpdate(_owner.getObjectId());
			playerSU.addAttribute(StatusUpdate.CUR_LOAD, _owner.getCurrentLoad());
			_owner.sendPacket(playerSU);
			playerSU = null;

			playerSU = new StatusUpdate(_partner.getObjectId());
			playerSU.addAttribute(StatusUpdate.CUR_LOAD, _partner.getCurrentLoad());
			_partner.sendPacket(playerSU);
			playerSU = null;

			success = true;

			ownerIU = null;
			partnerIU = null;
		}
		// Finish the trade
		partnerList.getOwner().onTradeFinish(success);
		getOwner().onTradeFinish(success);
	}

	/**
	 * Buy items from this PrivateStore list
	 * 
	 * @return : boolean true if success
	 */
	public synchronized boolean PrivateStoreBuy(L2PcInstance player, ItemRequest[] items, int price)
	{
		if(_locked)
			return false;

		if(items == null || items.length == 0){
			return false;
		}
		
		if(!validate())
		{
			lock();
			return false;
		}

		int slots = 0;
		int weight = 0;

		for(ItemRequest item : items)
		{
			if(item == null)
			{
				continue;
			}

			L2Item template = ItemTable.getInstance().getTemplate(item.getItemId());
			if(template == null)
			{
				continue;
			}
			
			boolean found = false;
			for(TradeItem ti : _items){
				if(ti.getObjectId() == item.getObjectId()){
					
					found = true;
					
					if(ti.getPrice() != item.getPrice()){
						return false;
					}
				}
			}
			
			//store is not selling that item...
			if(!found){
				String msg = "Requested Item is not available to buy... You are perfoming illegal operation, it has been segnalated";
				_log.warning("ATTENTION: Player "+player.getName()+" has performed buy illegal operation..");
				player.sendMessage(msg);
				msg = null;
				return false;
			}
			
			
			weight += item.getCount() * template.getWeight();
			if(!template.isStackable())
			{
				slots += item.getCount();
			}
			else if(player.getInventory().getItemByItemId(item.getItemId()) == null)
			{
				slots++;
			}

			template = null;
		}

		if(!player.getInventory().validateWeight(weight))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.WEIGHT_LIMIT_EXCEEDED));
			return false;
		}

		if(!player.getInventory().validateCapacity(slots))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.SLOTS_FULL));
			return false;
		}

		PcInventory ownerInventory = _owner.getInventory();
		PcInventory playerInventory = player.getInventory();

		// Prepare inventory update packets
		InventoryUpdate ownerIU = new InventoryUpdate();
		InventoryUpdate playerIU = new InventoryUpdate();

		if(Config.SELL_BY_ITEM){
			// Transfer Item
			if(price > playerInventory.getInventoryItemCount(Config.SELL_ITEM, -1))
			{
				//no useful lock to seller
				//lock();
				return false;
			}
			
			L2ItemInstance item = playerInventory.getItemByItemId(Config.SELL_ITEM);
			
			if(item==null){
				System.out.println("Buyer Medals are null");
				//no useful lock to seller
				//lock();
				return false;
			}
			
			// Check if requested item is available for manipulation
			L2ItemInstance oldItem = player.checkItemManipulation(item.getObjectId(), price, "sell");
			if(oldItem == null)
			{
				System.out.println("Buyer old medals null");
				//no useful lock to seller
				//lock();
				return false;
			}

			// Proceed with item transfer
			L2ItemInstance newItem = playerInventory.transferItem("PrivateStore", item.getObjectId(), price, ownerInventory, player, _owner);
			if(newItem == null){
				System.out.println("Buyer new medals null");
				return false;
			}
			
			// Add changes to inventory update packets
			if(oldItem.getCount() > 0 && oldItem != newItem)
			{
				playerIU.addModifiedItem(oldItem);
			}
			else
			{
				playerIU.addRemovedItem(oldItem);
			}

			if(newItem.getCount() > item.getCount())
			{
				ownerIU.addModifiedItem(newItem);
			}
			else
			{
				ownerIU.addNewItem(newItem);
			}

			// Send messages about the transaction to both players
			SystemMessage msg = SystemMessage.sendString("You obtained "+price+" "+item.getItemName());
			_owner.sendPacket(msg);
			msg = null;
			
			SystemMessage msg2 = SystemMessage.sendString("You spent "+price+" "+item.getItemName());
			player.sendPacket(msg2);
			msg = null;

			newItem = null;
			oldItem = null;

			/*L2ItemInstance item = playerInventory.getItemByItemId(Config.SELL_ITEM);
			playerInventory.destroyItem("PrivateStore", item.getObjectId(), price, player, _owner);
			playerIU.addItem(item);
			L2ItemInstance item2 = ownerInventory.getItemByItemId(Config.SELL_ITEM);
			ownerInventory.addItem("PrivateStore", item2.getObjectId(), price, _owner, player);
			ownerIU.addItem(ownerInventory.getItemByItemId(Config.SELL_ITEM));
			*/
			
		}else{
			// Transfer adena
			if(price > playerInventory.getAdena())
			{
				lock();
				return false;
			}

			L2ItemInstance adenaItem = playerInventory.getAdenaInstance();
			playerInventory.reduceAdena("PrivateStore", price, player, _owner);
			playerIU.addItem(adenaItem);
			ownerInventory.addAdena("PrivateStore", price, _owner, player);
			ownerIU.addItem(ownerInventory.getAdenaInstance());

		}
		
		
		// Transfer items
		for(ItemRequest item : items)
		{
			//Check if requested item is sill on the list and adjust its count
			adjustItemRequest(item);
			if(item.getCount() == 0)
			{
				continue;
			}

			// Check if requested item is available for manipulation
			L2ItemInstance oldItem = _owner.checkItemManipulation(item.getObjectId(), item.getCount(), "sell");
			if(oldItem == null)
			{
				lock();
				return false;
			}

			// Proceed with item transfer
			L2ItemInstance newItem = ownerInventory.transferItem("PrivateStore", item.getObjectId(), item.getCount(), playerInventory, _owner, player);
			if(newItem == null)
				return false;

			removeItem(item.getObjectId(), -1, item.getCount());

			// Add changes to inventory update packets
			if(oldItem.getCount() > 0 && oldItem != newItem)
			{
				ownerIU.addModifiedItem(oldItem);
			}
			else
			{
				ownerIU.addRemovedItem(oldItem);
			}

			if(newItem.getCount() > item.getCount())
			{
				playerIU.addModifiedItem(newItem);
			}
			else
			{
				playerIU.addNewItem(newItem);
			}

			// Send messages about the transaction to both players
			if(newItem.isStackable())
			{
				SystemMessage msg = new SystemMessage(SystemMessageId.S1_PURCHASED_S3_S2_S);
				msg.addString(player.getName());
				msg.addItemName(newItem.getItemId());
				msg.addNumber(item.getCount());
				_owner.sendPacket(msg);
				msg = null;

				msg = new SystemMessage(SystemMessageId.PURCHASED_S3_S2_S_FROM_S1);
				msg.addString(_owner.getName());
				msg.addItemName(newItem.getItemId());
				msg.addNumber(item.getCount());
				player.sendPacket(msg);
				msg = null;
			}
			else
			{
				SystemMessage msg = new SystemMessage(SystemMessageId.S1_PURCHASED_S2);
				msg.addString(player.getName());
				msg.addItemName(newItem.getItemId());
				_owner.sendPacket(msg);
				msg = null;

				msg = new SystemMessage(SystemMessageId.PURCHASED_S2_FROM_S1);
				msg.addString(_owner.getName());
				msg.addItemName(newItem.getItemId());
				player.sendPacket(msg);
				msg = null;
			}

			newItem = null;
			oldItem = null;
		}

		// Send inventory update packet
		_owner.sendPacket(ownerIU);
		player.sendPacket(playerIU);

		if(_owner.isOffline())
			OfflineTradeTable.storeOffliner(_owner);
		
		ownerIU = null;
		playerIU = null;
		//adenaItem = null;
		ownerInventory = null;
		playerInventory = null;

		return true;
	}

	/**
	 * Sell items to this PrivateStore list
	 * 
	 * @return : boolean true if success
	 */
	public synchronized boolean PrivateStoreSell(L2PcInstance player, ItemRequest[] items, int price)
	{
		if(_locked){
			
			if(Config.DEBUG){
				_log.info("[PrivateStoreSell] Locked, return false");
			}
			return false;
		}
		
		if(items==null || items.length == 0){
			if(Config.DEBUG){
				_log.info("[PrivateStoreSell] items==null || items.length == 0, return false");
			}
			return false;
		}

		PcInventory ownerInventory = _owner.getInventory();
		PcInventory playerInventory = player.getInventory();
		
		// Prepare inventory update packet
		InventoryUpdate ownerIU = new InventoryUpdate();
		InventoryUpdate playerIU = new InventoryUpdate();

		//we must check item are available before begining transaction, TODO: should we remove that check when transfering items as it's done here? (there might be synchro problems if player clicks fast if we remove it)
		// also check if augmented items are traded. If so, cancel it...
		for(ItemRequest item : items)
		{
			// Check if requested item is available for manipulation
			L2ItemInstance oldItem = player.checkItemManipulation(item.getObjectId(), item.getCount(), "sell");
			if(oldItem == null){
				if(Config.DEBUG){
					_log.info("[PrivateStoreSell] player.checkItemManipulation(item.getObjectId(), item.getCount(), 'sell') null, return false");
				}
				return false;
			}
			
			boolean found = false;
			for(TradeItem ti : _items){
				
				if(ti.getItem().getItemId() == item.getItemId()){
					found = true;
					
					if(ti.getPrice() != item.getPrice()){
						if(Config.DEBUG){
							_log.info("[PrivateStoreSell] ti.getPrice() != item.getPrice(), return false");
						}
						
						return false;
					}
				}
				
			}
			
			//store is not buying that item...
			if(!found){
				String msg = "Requested Item is not available to sell... You are perfoming illegal operation, it has been segnalated";
				_log.warning("ATTENTION: Player "+player.getName()+" has performed sell illegal operation..");
				player.sendMessage(msg);
				msg = null;
				return false;
			}
			
			if(oldItem.getAugmentation() != null)
			{
				String msg = "Transaction failed. Augmented items may not be exchanged.";
				_owner.sendMessage(msg);
				player.sendMessage(msg);
				msg = null;
				return false;
			}

			oldItem = null;
		}

		// Transfer items
		for(ItemRequest item : items)
		{
			// Check if requested item is sill on the list and adjust its count
			adjustItemRequestByItemId(item);
			if(item.getCount() == 0)
			{
				continue;
			}

			// Check if requested item is available for manipulation
			L2ItemInstance oldItem = player.checkItemManipulation(item.getObjectId(), item.getCount(), "sell");
			if(oldItem == null){
				if(Config.DEBUG){
					_log.info("[PrivateStoreSell] oldItem == null, return false");
				}
				return false;
			}

			// Proceed with item transfer
			L2ItemInstance newItem = playerInventory.transferItem("PrivateStore", item.getObjectId(), item.getCount(), ownerInventory, player, _owner);
			if(newItem == null){
				if(Config.DEBUG){
					_log.info("[PrivateStoreSell] newItem == null, return false");
				}
				
				return false;
			}

			removeItem(-1, item.getItemId(), item.getCount());

			// Add changes to inventory update packets
			if(oldItem.getCount() > 0 && oldItem != newItem)
			{
				playerIU.addModifiedItem(oldItem);
			}
			else
			{
				playerIU.addRemovedItem(oldItem);
			}

			if(newItem.getCount() > item.getCount())
			{
				ownerIU.addModifiedItem(newItem);
			}
			else
			{
				ownerIU.addNewItem(newItem);
			}

			// Send messages about the transaction to both players
			if(newItem.isStackable())
			{
				SystemMessage msg = new SystemMessage(SystemMessageId.PURCHASED_S3_S2_S_FROM_S1);
				msg.addString(player.getName());
				msg.addItemName(newItem.getItemId());
				msg.addNumber(item.getCount());
				_owner.sendPacket(msg);
				msg = null;

				msg = new SystemMessage(SystemMessageId.S1_PURCHASED_S3_S2_S);
				msg.addString(_owner.getName());
				msg.addItemName(newItem.getItemId());
				msg.addNumber(item.getCount());
				player.sendPacket(msg);
				msg = null;
			}
			else
			{
				SystemMessage msg = new SystemMessage(SystemMessageId.PURCHASED_S2_FROM_S1);
				msg.addString(player.getName());
				msg.addItemName(newItem.getItemId());
				_owner.sendPacket(msg);
				msg = null;

				msg = new SystemMessage(SystemMessageId.S1_PURCHASED_S2);
				msg.addString(_owner.getName());
				msg.addItemName(newItem.getItemId());
				player.sendPacket(msg);
				msg = null;
			}

			newItem = null;
			oldItem = null;
		}

		if(Config.SELL_BY_ITEM){
			// Transfer Item
			if(price > ownerInventory.getInventoryItemCount(Config.SELL_ITEM, -1))
			{
				lock();
				if(Config.DEBUG){
					_log.info("[PrivateStoreSell] price > ownerInventory.getInventoryItemCount(Config.SELL_ITEM, -1), return false");
				}
				
				return false;
			}
			
			L2ItemInstance item = ownerInventory.getItemByItemId(Config.SELL_ITEM);
			
			if(item==null){
				if(Config.DEBUG){
					_log.info("[PrivateStoreSell] item==null, return false");
				}
				
				lock();
				return false;
			}
			
			
			// Check if requested item is available for manipulation
			L2ItemInstance oldItem = _owner.checkItemManipulation(item.getObjectId(), price, "sell");
			if(oldItem == null)
			{
				lock();
				if(Config.DEBUG){
					_log.info("[PrivateStoreSell] _owner.checkItemManipulation(item.getObjectId(), price, 'sell')==null, return false");
				}
				
				return false;
			}

			// Proceed with item transfer
			L2ItemInstance newItem = ownerInventory.transferItem("PrivateStore", item.getObjectId(), price, playerInventory,_owner, player);
			if(newItem == null){
				if(Config.DEBUG){
					_log.info("[PrivateStoreSell] newItem = ownerInventory.transferItem('PrivateStore', item.getObjectId(), price, playerInventory,_owner, player) == null, return false");
				}
				
				return false;
			}
			
			// Add changes to inventory update packets
			if(oldItem.getCount() > 0 && oldItem != newItem)
			{
				ownerIU.addModifiedItem(oldItem);
			}
			else
			{
				ownerIU.addRemovedItem(oldItem);
			}

			if(newItem.getCount() > item.getCount())
			{
				playerIU.addModifiedItem(newItem);
			}
			else
			{
				playerIU.addNewItem(newItem);
			}

			// Send messages about the transaction to both players
			SystemMessage msg = SystemMessage.sendString("You obtained "+price+" "+item.getItemName());
			player.sendPacket(msg);
			msg = null;
			
			SystemMessage msg2 = SystemMessage.sendString("You spent "+price+" "+item.getItemName());
			_owner.sendPacket(msg2);
			msg = null;

			newItem = null;
			oldItem = null;

		}else{
			
			// Transfer adena
			if(price > ownerInventory.getAdena()){
				if(Config.DEBUG){
					_log.info("[PrivateStoreSell] price > ownerInventory.getAdena(), return false");
				}
				
				return false;
			}

			L2ItemInstance adenaItem = ownerInventory.getAdenaInstance();
			ownerInventory.reduceAdena("PrivateStore", price, _owner, player);
			ownerIU.addItem(adenaItem);
			playerInventory.addAdena("PrivateStore", price, player, _owner);
			playerIU.addItem(playerInventory.getAdenaInstance());

		}
		

		// Send inventory update packet
		_owner.sendPacket(ownerIU);
		player.sendPacket(playerIU);

		if(_owner.isOffline())
			OfflineTradeTable.storeOffliner(_owner);
		
		ownerIU = null;
		playerIU = null;
		ownerInventory = null;
		playerInventory = null;

		return true;
	}

	/**
	 * @param objectId
	 * @return
	 */
	public TradeItem getItem(int objectId)
	{
		for(TradeItem item : _items)
		{
			if(item.getObjectId() == objectId)
				return item;
		}
		return null;
	}
}
