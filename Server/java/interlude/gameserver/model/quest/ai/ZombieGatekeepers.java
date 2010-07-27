/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package interlude.gameserver.model.quest.ai;

import javolution.util.FastList;
import javolution.util.FastMap;

import interlude.gameserver.ai.CtrlIntention;
import interlude.gameserver.model.L2Attackable;
import interlude.gameserver.model.L2Character;
import interlude.gameserver.model.L2ItemInstance;
import interlude.gameserver.model.actor.instance.L2NpcInstance;
import interlude.gameserver.model.actor.instance.L2PcInstance;
import interlude.gameserver.model.quest.Quest;

public class ZombieGatekeepers extends Quest implements Runnable
{
	public ZombieGatekeepers(int questId, String name, String descr)
	{
		super(questId, name, descr);
		super.addAttackId(22136);
		super.addAggroRangeEnterId(22136);
	}

	private FastMap<Integer, FastList<L2Character>> _attackersList = new FastMap<Integer, FastList<L2Character>>();

	@Override
	public String onAttack(L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		int npcObjId = npc.getObjectId();

		L2Character target = isPet ? attacker.getPet() : attacker;

		if (_attackersList.get(npcObjId) == null)
		{
			FastList<L2Character> player = new FastList<L2Character>();
			player.add(target);
			_attackersList.put(npcObjId, player);
		}
		else if (!_attackersList.get(npcObjId).contains(target))
			_attackersList.get(npcObjId).add(target);

		return super.onAttack(npc, attacker, damage, isPet);
	}

	@Override
	public String onAggroRangeEnter(L2NpcInstance npc, L2PcInstance player, boolean isPet)
	{
		int npcObjId = npc.getObjectId();

		L2Character target = isPet ? player.getPet() : player;

		L2ItemInstance VisitorsMark = player.getInventory().getItemByItemId(8064);
		L2ItemInstance FadedVisitorsMark = player.getInventory().getItemByItemId(8065);
		L2ItemInstance PagansMark = player.getInventory().getItemByItemId(8067);

		long mark1 = VisitorsMark == null ? 0 : VisitorsMark.getCount();
		long mark2 = FadedVisitorsMark == null ? 0 : FadedVisitorsMark.getCount();
		long mark3 = PagansMark == null ? 0 : PagansMark.getCount();

		if ((mark1 == 0) && (mark2 == 0) && (mark3 == 0))
		{
			((L2Attackable) npc).addDamageHate(target, 0, 999);
			npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
		}
		else
		{
			if (_attackersList.get(npcObjId) == null || !_attackersList.get(npcObjId).contains(target))
				((L2Attackable) npc).getAggroList().remove(target);
			else
			{
				((L2Attackable) npc).addDamageHate(target, 0, 999);
				npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
			}
		}

		return super.onAggroRangeEnter(npc, player, isPet);
	}

	@Override
	public String onKill(L2NpcInstance npc, L2PcInstance killer, boolean isPet)
	{
		int npcObjId = npc.getObjectId();
		if (_attackersList.get(npcObjId) != null)
			_attackersList.get(npcObjId).clear();

		return super.onKill(npc, killer, isPet);
	}

	@Override
	public void run()
	{
		//new ZombieGatekeepers(-1, "ZombieGatekeepers", "ai");
	}
}