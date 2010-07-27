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
package interlude.gameserver.skills.l2skills;

import interlude.Config;
import interlude.gameserver.model.L2Character;
import interlude.gameserver.model.L2Effect;
import interlude.gameserver.model.L2ItemInstance;
import interlude.gameserver.model.L2Object;
import interlude.gameserver.model.L2Skill;
import interlude.gameserver.model.actor.instance.L2PcInstance;
import interlude.gameserver.network.SystemMessageId;
import interlude.gameserver.network.serverpackets.EtcStatusUpdate;
import interlude.gameserver.network.serverpackets.SystemMessage;
import interlude.gameserver.skills.Formulas;
import interlude.gameserver.skills.effects.EffectCharge;
import interlude.gameserver.templates.L2WeaponType;
import interlude.gameserver.templates.StatsSet;

public class L2SkillChargeDmg extends L2Skill
{
	final int numCharges;
	final int chargeSkillId;

	public L2SkillChargeDmg(StatsSet set)
	{
		super(set);
		numCharges = set.getInteger("num_charges", getLevel());
		chargeSkillId = set.getInteger("charge_skill_id");
	}

	@Override
	public boolean checkCondition(L2Character activeChar, L2Object target, boolean itemOrWeapon)
	{
		if (activeChar instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) activeChar;
			EffectCharge e = (EffectCharge) player.getFirstEffect(chargeSkillId);
			if (e == null || e.numCharges < numCharges)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
				sm.addSkillName(getId());
				activeChar.sendPacket(sm);
				return false;
			}
		}
		return super.checkCondition(activeChar, target, itemOrWeapon);
	}

	@Override
	public void useSkill(L2Character caster, L2Object[] targets)
	{
		if (caster.isAlikeDead())
		{
			return;
		}
		// get the effect
		EffectCharge effect = (EffectCharge) caster.getFirstEffect(chargeSkillId);
		if (effect == null || effect.numCharges < numCharges)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(getId());
			caster.sendPacket(sm);
			return;
		}
		double modifier = 0;
		modifier = (effect.numCharges - numCharges) * 0.33;
		if (getTargetType() != SkillTargetType.TARGET_AREA && getTargetType() != SkillTargetType.TARGET_MULTIFACE) {
			effect.numCharges -= numCharges;
		}
		if (caster instanceof L2PcInstance) {
			caster.sendPacket(new EtcStatusUpdate((L2PcInstance) caster));
		}
		if (effect.numCharges == 0)
		{
			effect.exit();
		}
		for (L2Object target2 : targets) {
			L2ItemInstance weapon = caster.getActiveWeaponInstance();
			L2Character target = (L2Character) target2;
			if (target.isAlikeDead()) {
				continue;
			}
			// Calculate skill evasion
			boolean skillIsEvaded = Formulas.calcPhysicalSkillEvasion(target, this);
			if (skillIsEvaded)
			{
				if (caster instanceof L2PcInstance)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.S1_DODGES_ATTACK);
					sm.addString(target.getName());
					((L2PcInstance) caster).sendPacket(sm);
				}
				if (target instanceof L2PcInstance)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.AVOIDED_S1_ATTACK2);
					sm.addString(caster.getName());
					((L2PcInstance) target).sendPacket(sm);
				}
				// no futher calculations needed.
				continue;
			}
			// TODO: should we use dual or not?
			// because if so, damage are lowered but we dont do anything
			// special with dual then
			// like in doAttackHitByDual which in fact does the calcPhysDam
			// call twice
			boolean shld = Formulas.getInstance().calcShldUse(caster, target);
			boolean crit = false;
			if (getBaseCritRate() > 0) {
				crit = Formulas.getInstance().calcCrit(getBaseCritRate() * 10 * Formulas.getInstance().getSTRBonus(caster));
			}
			boolean soul = weapon != null && weapon.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT && weapon.getItemType() != L2WeaponType.DAGGER;
			// damage calculation, crit is static 2x
			int damage = (int) Formulas.getInstance().calcPhysDam(caster, target, this, shld, false, false, soul);
			if (crit) {
				damage *= 2;
			}
			if (caster instanceof L2PcInstance)
			{
				L2PcInstance activeCaster = (L2PcInstance) caster;
				if (activeCaster.isGM() && activeCaster.getAccessLevel() < Config.GM_CAN_GIVE_DAMAGE) {
					damage = 0;
				}
			}
			if (damage > 0)
			{
				double finalDamage = damage;
				finalDamage = finalDamage + modifier * finalDamage;
				target.reduceCurrentHp(finalDamage, caster);
				caster.sendDamageMessage(target, (int) finalDamage, false, crit, false);
				if (soul && weapon != null) {
					weapon.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
				}
			}
			else
			{
				caster.sendDamageMessage(target, 0, false, false, true);
			}
		} // effect self :]
		L2Effect seffect = caster.getFirstEffect(getId());
		if (seffect != null && seffect.isSelfEffect())
		{
			// Replace old effect with new one.
			seffect.exit();
		}
		// cast self effect if any
		getEffectsSelf(caster);
	}
}
