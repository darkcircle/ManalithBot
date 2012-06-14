/*
 	org.manalith.ircbot/ManalithBot.java
 	ManalithBot - An open source IRC bot based on the PircBot Framework.
 	Copyright (C) 2005, 2011  Ki-Beom, Kim
 	Copyright (C) 2011, 2012  Seong-ho, Cho <darkcircle.0426@gmail.com>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.manalith.ircbot;

import java.util.List;

import org.apache.log4j.Logger;
import org.manalith.ircbot.plugin.EventDispatcher;
import org.manalith.ircbot.plugin.IBotPlugin;
import org.manalith.ircbot.plugin.PluginManager;
import org.manalith.ircbot.plugin.relay.RelayPlugin;
import org.manalith.ircbot.util.AppContextUtil;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.managers.ListenerManager;

public class ManalithBot extends PircBotX {
	private Logger logger = Logger.getLogger(getClass());

	private PluginManager pluginManager = new PluginManager();

	public ManalithBot(List<IBotPlugin> plugins) {
		@SuppressWarnings("unchecked")
		ListenerManager<ManalithBot> mgr = (ListenerManager<ManalithBot>) getListenerManager();
		mgr.addListener(new EventDispatcher(pluginManager));

		pluginManager.load(plugins);
	}

	public static ManalithBot getInstance() {
		return AppContextUtil.getApplicationContext()
				.getBean(ManalithBot.class);
	}

	public PluginManager getPluginManager() {
		return pluginManager;
	}

	/**
	 * sendMessage(target, message)가 final 메서드이므로 로깅을 위해 이 메시지를 사용한다.
	 * 
	 * @param target
	 * @param message
	 */
	public void sendLoggedMessage(String target, String message,
			boolean redirectToRelayBot) {
		// TODO sendMessage 오버라이드
		logger.trace(String.format("MESSAGE(LOCAL) : %s / %s", target, message));

		sendMessage(target, message);

		if (redirectToRelayBot && RelayPlugin.RELAY_BOT != null)
			RelayPlugin.RELAY_BOT.sendMessage(target, message);
	}

	public void sendLoggedMessage(String target, String message) {
		sendLoggedMessage(target, message, true);
	}

	// private void sendMessage(Message m){
	// //너무 긴 문자는 자른다.
	// if(m.getMessage().length() > 180)
	// m.setMessage(m.getMessage().substring(0, 179));
	// sendMessage(m.getChannel(), m.getSender() + ", " + m.getMessage());
	// }
}