/*
 	org.manalith.ircbot.plugin/PluginManager.java
 	ManalithBot - An open source IRC bot based on the PircBot Framework.
 	Copyright (C) 2005, 2011, 2012  Ki-Beom, Kim
 	Copyright (C) 2012  Seong-ho, Cho  <darkcircle.0426@gmail.com>

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
package org.manalith.ircbot.plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.manalith.ircbot.common.stereotype.BotCommand;
import org.manalith.ircbot.common.stereotype.BotCommand.BotEvent;
import org.manalith.ircbot.common.stereotype.BotFilter;
import org.manalith.ircbot.common.stereotype.BotTimer;
import org.manalith.ircbot.resources.MessageEvent;

public class PluginManager {
	private List<IBotPlugin> list = new ArrayList<IBotPlugin>();
	private Map<Method, Object> commands = new HashMap<Method, Object>();
	private Map<Method, Object> filters = new HashMap<Method, Object>();
	private Map<Method, Object> timers = new HashMap<Method, Object>();

	public void add(IBotPlugin plugin) {
		extractEventDelegates(plugin);

		list.add(plugin);
	}

	public void remove(IBotPlugin plugin) {
		list.remove(plugin);
	}

	private void extractEventDelegates(IBotPlugin plugin) {
		Method[] methods = plugin.getClass().getDeclaredMethods();
		for (Method method : methods) {
			BotCommand command = method.getAnnotation(BotCommand.class);
			if (command != null) {
				commands.put(method, plugin);
				continue;
			}

			BotFilter filter = method.getAnnotation(BotFilter.class);
			if (filter != null) {
				filters.put(method, plugin);
				continue;
			}

			BotTimer timer = method.getAnnotation(BotTimer.class);
			if (timer != null) {
				timers.put(method, plugin);
				continue;
			}
		}
	}

	public void onJoin(String channel, String sender, String login,
			String hostName) {
		for (IBotPlugin plugin : list)
			plugin.onJoin(channel, sender, login, hostName);
	}

	public void onMessage(String channel, String sender, String login,
			String hostName, String message) {
		if (StringUtils.isEmpty(message))
			return;

		MessageEvent msg = new MessageEvent(channel, sender, login, hostName,
				message);

		// 어노테이션(@BotCommand) 기반 플러그인 실행
		for (Method method : commands.keySet()) {
			BotCommand commandMeta = method.getAnnotation(BotCommand.class);

			String[] segments = StringUtils
					.splitByWholeSeparator(message, null);

			if (ArrayUtils.contains(commandMeta.listeners(),
					BotEvent.ON_MESSAGE)
					&& ArrayUtils.contains(commandMeta.value(), segments[0])) {
				IBotPlugin plugin = (IBotPlugin) commands.get(method);

				if (segments.length - 1 != commandMeta.arguments()) {
					plugin.getBot().sendLoggedMessage(channel,
							commandMeta.argumentsLengthErrorMessage());
				}

				try {
					// TODO MethodUtils 사용
					String result = (String) method.invoke(plugin, msg,
							ArrayUtils.subarray(segments, 1,
									segments.length - 1));

					if (StringUtils.isNotBlank(result)) {
						plugin.getBot().sendLoggedMessage(channel, result);
					}

					msg.setExecuted(commandMeta.stopEvent());
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}

				if (msg.isExecuted()) {
					return;
				}
			}
		}

		// 비 어노테이션 기반 플러그인 실행
		for (IBotPlugin plugin : list) {
			plugin.onMessage(msg);
			if (msg.isExecuted()) {
				return;
			}
		}
	}

	public void onPrivateMessage(String sender, String login, String hostName,
			String message) {
		MessageEvent msg = new MessageEvent(sender, login, hostName, message);

		for (IBotPlugin plugin : list) {
			plugin.onPrivateMessage(msg);
			if (msg.isExecuted())
				break;
		}
	}

	public void onPart(String channel, String sender, String login,
			String hostName) {
		for (IBotPlugin plugin : list)
			plugin.onPart(channel, sender, login, hostName);
	}

	public void onQuit(String sourceNick, String sourceLogin,
			String sourceHostname, String reason) {

		for (IBotPlugin plugin : list)
			plugin.onQuit(sourceNick, sourceLogin, sourceHostname, reason);
	}

	public List<IBotPlugin> getList() {
		return list;
	}

	public String getPluginInfo() {
		StringBuilder sb = new StringBuilder();
		String name = "";
		int i = 0; // To make well-formed message
		for (IBotPlugin p : list) {
			name = p.getName();
			if (name != null) {
				if (i != 0)
					sb.append(", "); // To make well-formed message
				else
					i++;

				sb.append(name);

				String commands = p.getCommands();
				if (StringUtils.isNotBlank(commands)) {
					sb.append("(" + commands + ")");
				}
			}
		}

		return sb.toString();
	}

}
