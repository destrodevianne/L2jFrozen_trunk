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
package interlude.gameserver.network.clientpackets;

import interlude.gameserver.model.L2Object;
import interlude.gameserver.model.L2World;
import interlude.gameserver.model.actor.instance.L2PcInstance;
import interlude.gameserver.network.serverpackets.ActionFailed;

/**
 * This class ...
 *
 * @version $Revision: 1.7.2.1.2.2 $ $Date: 2005/03/27 15:29:30 $
 */
public final class AttackRequest extends L2GameClientPacket
{
	// cddddc
	private int _objectId;
	@SuppressWarnings("unused")
	private int _originX;
	@SuppressWarnings("unused")
	private int _originY;
	@SuppressWarnings("unused")
	private int _originZ;
	@SuppressWarnings("unused")
	private int _attackId;
	private static final String _C__0A_ATTACKREQUEST = "[C] 0A AttackRequest";

	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_originX = readD();
		_originY = readD();
		_originZ = readD();
		_attackId = readC(); // 0 for simple click 1 for shift-click
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null) {
			return;
		}
		// avoid using expensive operations if not needed
		L2Object target;
		if (activeChar.getTargetId() == _objectId) {
			target = activeChar.getTarget();
		} else {
			target = L2World.getInstance().findObject(_objectId);
		}
		// Update next commit
		// if (!Config.ENABLE_FP && !activeChar.getFloodProtectors().getActionMwx().tryPerformAction("_objectId"))
		// {
		// activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		// return;
		// }
		if (target == null) {
			return;
		}
		if (activeChar.getTarget() != target)
		{
			target.onAction(activeChar);
		}
		else
		{
			if (target.getObjectId() != activeChar.getObjectId() && activeChar.getPrivateStoreType() == 0 && activeChar.getActiveRequester() == null)
			{
				// _log.config("Starting ForcedAttack");
				target.onForcedAttack(activeChar);
				// _log.config("Ending ForcedAttack");
			}
			else
			{
				sendPacket(ActionFailed.STATIC_PACKET);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see interlude.gameserver.network.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__0A_ATTACKREQUEST;
	}
}
