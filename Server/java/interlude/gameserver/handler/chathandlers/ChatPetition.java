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
package interlude.gameserver.handler.chathandlers;

import interlude.gameserver.handler.IChatHandler;
import interlude.gameserver.instancemanager.PetitionManager;
import interlude.gameserver.model.actor.instance.L2PcInstance;
import interlude.gameserver.network.SystemMessageId;
import interlude.gameserver.network.serverpackets.SystemMessage;

/**
 * A chat handler
 * 
 * @author durgus
 */
public class ChatPetition implements IChatHandler {
	private static final int[] COMMAND_IDS = { 6, 7 };

	/**
	 * Handle chat type 'petition player'
	 * 
	 * @see interlude.gameserver.handler.IChatHandler#handleChat(int,
	 *      interlude.gameserver.model.actor.instance.L2PcInstance,
	 *      java.lang.String)
	 */
	public void handleChat(int type, L2PcInstance activeChar, String target,
			String text) {
		if (!PetitionManager.getInstance().isPlayerInConsultation(activeChar)) {
			activeChar.sendPacket(new SystemMessage(
					SystemMessageId.YOU_ARE_NOT_IN_PETITION_CHAT));
			return;
		}
		PetitionManager.getInstance().sendActivePetitionMessage(activeChar,
				text);
	}

	/**
	 * Returns the chat types registered to this handler
	 * 
	 * @see interlude.gameserver.handler.IChatHandler#getChatTypeList()
	 */
	public int[] getChatTypeList() {
		return COMMAND_IDS;
	}
}