package org.manalith.ircbot.plugin.stackoverflow;

import org.apache.commons.lang3.StringUtils;
import org.manalith.ircbot.plugin.SimplePlugin;
import org.manalith.ircbot.resources.MessageEvent;
import org.springframework.stereotype.Component;

@Component
public class SOPlugin extends SimplePlugin {

	private String googleAPIKey;
	private String cxStr;

	@Override
	public String getName() {
		return "스택오버플로우";
	}

	@Override
	public String getCommands() {
		return "!so";
	}

	@Override
	public String getHelp() {
		return "설  명: 스택 오버플로 링크를 가져옵니다, 사용법: !so [키워드]";
	}

	public void setGoogleAPIKey(String googleAPIKey) {
		this.googleAPIKey = googleAPIKey;
	}

	public void setCxStr(String cxStr) {
		this.cxStr = cxStr;
	}

	@Override
	public void onMessage(MessageEvent event) {
		String[] command = event.getMessage().replaceAll("(\\s){2,}", " ")
				.split("\\s", 2);
		if (StringUtils.equals(command[0], "!so")) {
			String[] result = new SOFetcher(googleAPIKey, cxStr)
					.fetchResult(command[1]);

			if (result.length == 0) {
				event.respond("[StackOverflow] 결과가 없습니다.");
				return;
			}

			for (String msg : result)
				event.respond(msg);
		}
	}
}
