package org.manalith.ircbot.plugin.dictionary;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.manalith.ircbot.annotation.Option;
import org.manalith.ircbot.common.stereotype.BotCommand;
import org.manalith.ircbot.plugin.SimplePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import dk.dren.hunspell.Hunspell;

@Component
public class SpellCheckerPlugin extends SimplePlugin {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private String englishDictionaryPath;

	private String koreanDictionaryPath;

	public String getEnglishDictionaryPath() {
		return englishDictionaryPath;
	}

	public void setEnglishDictionaryPath(String path) {
		englishDictionaryPath = path;
	}

	public String getKoreanDictionaryPath() {
		return koreanDictionaryPath;
	}

	public void setKoreanDictionaryPath(String path) {
		koreanDictionaryPath = path;
	}

	@Override
	public String getName() {
		return "맞춤법 검사";
	}

	@BotCommand({ "맞춤법" })
	public String checkKorean(
			@Option(name = "문장", help = "맞춤법을 검사할 문자열") String sentence) {
		return checkSpell(sentence, koreanDictionaryPath);
	}

	@BotCommand({ "spell" })
	public String checkEnglish(
			@Option(name = "문장", help = "맞춤법을 검사할 문자열") String sentence) {
		return checkSpell(sentence, englishDictionaryPath);
	}

	private String checkSpell(String sentence, String dictPath) {
		String[] words = StringUtils.split(sentence);
		StringBuilder sb = new StringBuilder();

		try {
			Hunspell.Dictionary dict = Hunspell.getInstance().getDictionary(
					dictPath);

			boolean hasSuggestion = false;
			for (String word : words) {
				if (dict.misspelled(word)) {
					hasSuggestion = true;
					sb.append("[");
					sb.append(word);
					sb.append("] ");

					List<String> suggestions = dict.suggest(word);
					if (CollectionUtils.isNotEmpty(suggestions)) {
						sb.append(StringUtils.join(suggestions, ' '));
					} else {
						sb.append("추천 단어 없음");
					}

					sb.append(" ");
				}
			}

			return hasSuggestion ? sb.toString() : "오류가 없습니다.";
		} catch (FileNotFoundException | UnsupportedEncodingException
				| UnsatisfiedLinkError | UnsupportedOperationException e) {
			logger.error(e.getMessage(), e);

			return "실행중 오류가 발생했습니다. 로그를 참고해주세요.";
		}
	}
}
