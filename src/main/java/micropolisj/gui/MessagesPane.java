// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.gui;

import micropolisj.engine.MicropolisMessage;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import java.util.ResourceBundle;

public class MessagesPane extends JTextPane
{
	private static final ResourceBundle cityMessageStrings = ResourceBundle.getBundle("strings.CityMessages");

	public MessagesPane()
	{
		setEditable(false);
	}

	public void appendCityMessage(MicropolisMessage message)
	{
		appendMessageText(cityMessageStrings.getString(message.name()));
	}

	private void appendMessageText(String messageText)
	{
		try {
			StyledDocument doc = getStyledDocument();
			if (doc.getLength() != 0) {
				doc.insertString(doc.getLength(), "\n", null);
			}
			doc.insertString(doc.getLength(), messageText, null);
		} catch (BadLocationException e) {
			throw new RuntimeException("unexpected", e);
		}
	}
}
