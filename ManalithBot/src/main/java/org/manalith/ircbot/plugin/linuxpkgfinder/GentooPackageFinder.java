/*
 	org.manalith.ircbot.plugin.distopkgfinder/GentooPkgFinderRunner.java
 	ManalithBot - An open source IRC bot based on the PircBot Framework.
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
package org.manalith.ircbot.plugin.linuxpkgfinder;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.manalith.ircbot.annotation.Option;
import org.manalith.ircbot.common.stereotype.BotCommand;
import org.springframework.stereotype.Component;

@Component
public class GentooPackageFinder extends PackageFinder {

	protected String keyword;

	@Override
	public String getName() {
		return "젠투";
	}

	@Override
	public String getDescription() {
		return "지정한 이름을 가진 젠투의 패키지를 검색합니다.";
	}

	public String getKeyword() {
		return keyword;
	}

	@Override
	@BotCommand("gen")
	public String find(@Option(name = "키워드", help = "검색할 단어") String keyword) {

		String result = "";
		String url = "https://packages.gentoo.org/packages/search?q=" + keyword;

		try {
			Element elem = Jsoup.connect(url).get().body();

			if (isResultListPage(elem)) {
				Elements elemlist = elem
						.select("div.list-group>a.list-group-item");
				for (Element el : elemlist) {

					if (el.select("h3.kk-search-result-header").get(0).text()
							.split("\\/")[1].equals(keyword)) {
						result = getPackageResult(el.attr("href"));
						break;
					}
				}

				if (result.isEmpty())
					result = createNoResultString();
			} else if (isResultPage(elem)) {
				String pkg = getCanonicalPackageName(elem);
				String ver = getLatestVersion(elem);
				String desc = getPackageDescription(elem);

				result = createResultString(pkg, ver, desc);
			} else
				result = createNoResultString();

		} catch (IOException e) {
			e.printStackTrace();
			result = createNoResultString();
		}

		return result;
	}

	public boolean isResultListPage(Element elem) {
		return (elem.select("h3.kk-search-result-header").size() > 0);
	}

	public boolean isResultPage(Element elem) {
		return (elem.select("h1.stick-top.kk-package-title").size() > 0);
	}

	public String getPackageResult(String suburl) {

		String result = "";
		try {
			Element elem = Jsoup
					.connect("https://packages.gentoo.org" + suburl).get()
					.body();

			String pkg = getCanonicalPackageName(elem);
			String ver = getLatestVersion(elem);
			String desc = getPackageDescription(elem);

			result = createResultString(pkg, ver, desc);

		} catch (IOException e) {
			e.printStackTrace();
			result = createNoResultString();
		}

		return result;
	}

	public String getCanonicalPackageName(Element e) {
		Elements pNameArr = e.select("ol.breadcrumb>li");
		String category = pNameArr.get(2).select("a").get(0).text();
		String pkgname = pNameArr.get(3).text();

		return category + "/" + pkgname;
	}

	public String getPackageDescription(Element e) {
		return e.select("p.lead.kk-package-maindesc").get(0).text();
	}

	public String getLatestVersion(Element e) {
		String result = "";
		Elements pTableArr = e
				.select("table.table.table-bordered.kk-versions-table>tbody>tr");
		for (Element pRow : pTableArr) {
			String tv = pRow.select("td>strong").get(0).text();
			if (tv.contains("9999"))
				continue;
			else {
				result = tv;
				break;
			}
		}

		return result;
	}

	public String createResultString(String name, String ver, String desc) {
		return "[Gentoo] \u0002" + name + "\u0002 (" + ver + ") - " + desc;
	}

	public String createNoResultString() {
		return "[Gentoo] 결과가 없습니다";
	}
}
