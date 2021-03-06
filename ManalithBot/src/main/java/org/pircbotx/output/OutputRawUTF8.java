package org.pircbotx.output;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

public class OutputRawUTF8 extends OutputRaw {

	public OutputRawUTF8(PircBotX bot) {
		super(bot);
	}

	@Override
	public void rawLineSplit(String prefix, String message, String suffix) {
		// Make sure suffix is valid
		if (message == null)
			return;

		Configuration<?> config = bot.getConfiguration();

		if (config.getEncoding().toString() != "UTF-8") {
			rawLineSplit(prefix, message, suffix);
			return;
		}

		if (suffix == null)
			suffix = "";

		// Make a server side prefix string
		User b = bot.getUserBot();
		String serverSidePrefix = ":" + b.getNick() + "!" + b.getLogin() + "@"
				+ b.getHostmask() + " ";

		byte[] serverSidePrefixByteArray = serverSidePrefix.getBytes();
		byte[] prefixByteArray = prefix.getBytes();
		byte[] messageByteArray = message.getBytes();
		byte[] suffixByteArray = suffix.getBytes();
		byte[] messageByteArrayCopy;

		// Find if final line is going to be shorter than the max line length
		String finalMessage = prefix + message + suffix;

		int realMaxLineLength = config.getMaxLineLength() - 2; // except
																// CR+LF
		if (!config.isAutoSplitMessage()
				|| serverSidePrefixByteArray.length + prefixByteArray.length
						+ messageByteArray.length + suffixByteArray.length < realMaxLineLength) {
			// Length is good (or auto split message is false),
			// just go ahead and send it
			rawLine(finalMessage);
			return;
		}

		// Too long, split it up ( with considering server side prefix length )
		int maxMessageLength = realMaxLineLength
				- (serverSidePrefixByteArray.length + prefixByteArray.length + suffixByteArray.length);

		int startPoint = 0;
		int endPoint = -1;
		byte val;

		while (true) {
			endPoint = ((startPoint + maxMessageLength < messageByteArray.length) ? (startPoint + maxMessageLength)
					: messageByteArray.length) - 1;

			// some characters ( such as cjk character ) need to separate
			// correctly.
			while (true) {

				if (endPoint == messageByteArray.length - 1)
					break;

				val = (byte) (messageByteArray[endPoint] & 0xC0);

				if (val == (byte) 0x80) {
					// upper ASCII area
					endPoint--;
				} else if (val == (byte) 0x40 || val == 0x0) {
					// within ASCII area
					break;
				} else {
					// highest byte of UTF-8 code
					endPoint--;
					break;
				}
			}

			// extract some part of byte array
			messageByteArrayCopy = new byte[endPoint - startPoint + 1];
			System.arraycopy(messageByteArray, startPoint,
					messageByteArrayCopy, 0, endPoint - startPoint + 1);

			// convert from byte array to string and concatenate!
			rawLine(prefix + new String(messageByteArrayCopy) + suffix);

			startPoint = endPoint + 1;

			// if endPoint reached to last index of messageByteArray,
			// stop to separate raw line.
			if (startPoint == messageByteArray.length)
				break;
		}
	}
}
