// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.gui;

import micropolisj.engine.Micropolis;
import micropolisj.engine.MicropolisMessage;
import micropolisj.engine.ZoneStatus;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ResourceBundle;

import static micropolisj.gui.ColorParser.parseColor;

public class NotificationPane extends JPanel
{
	private static final Dimension VIEWPORT_SIZE = new Dimension(160, 160);
	private static final Color QUERY_COLOR = new Color(255, 165, 0);
	private static final ResourceBundle strings = MainWindow.strings;
	private static final ResourceBundle mstrings = ResourceBundle.getBundle("strings.CityMessages");
	private static final ResourceBundle s_strings = ResourceBundle.getBundle("strings.StatusMessages");
	private final JLabel headerLbl;
	private final JViewport mapViewport;
	private final MicropolisDrawingArea mapView;
	private final JPanel mainPane;
	private JComponent infoPane;

	public NotificationPane(Micropolis engine)
	{
		super(new BorderLayout());
		setVisible(false);

		headerLbl = new JLabel();
		headerLbl.setOpaque(true);
		headerLbl.setHorizontalAlignment(SwingConstants.CENTER);
		headerLbl.setBorder(BorderFactory.createRaisedBevelBorder());
		add(headerLbl, BorderLayout.PAGE_START);

		JButton dismissBtn = new JButton(strings.getString("notification.dismiss"));
		dismissBtn.addActionListener(evt -> onDismissClicked());
		add(dismissBtn, BorderLayout.PAGE_END);

		mainPane = new JPanel(new BorderLayout());
		add(mainPane, BorderLayout.CENTER);

		JPanel viewportContainer = new JPanel(new BorderLayout());
		viewportContainer.setBorder(
				BorderFactory.createCompoundBorder(
						BorderFactory.createEmptyBorder(8, 4, 8, 4),
						BorderFactory.createLineBorder(Color.BLACK)
				));
		mainPane.add(viewportContainer, BorderLayout.LINE_START);

		mapViewport = new JViewport();
		mapViewport.setPreferredSize(VIEWPORT_SIZE);
		mapViewport.setMaximumSize(VIEWPORT_SIZE);
		mapViewport.setMinimumSize(VIEWPORT_SIZE);
		viewportContainer.add(mapViewport, BorderLayout.CENTER);

		mapView = new MicropolisDrawingArea(engine);
		mapViewport.setView(mapView);
	}

	private void onDismissClicked()
	{
		setVisible(false);
	}

	private void setPicture(Micropolis engine, int xpos, int ypos)
	{
		Dimension sz = VIEWPORT_SIZE;

		mapView.setEngine(engine);
		Rectangle r = mapView.getTileBounds(xpos, ypos);

		mapViewport.setViewPosition(new Point(
				r.x + r.width / 2 - sz.width / 2,
				r.y + r.height / 2 - sz.height / 2
		));
	}

	public void showMessage(Micropolis engine, MicropolisMessage msg, int xpos, int ypos)
	{
		setPicture(engine, xpos, ypos);

		if (infoPane != null) {
			mainPane.remove(infoPane);
			infoPane = null;
		}

		headerLbl.setText(mstrings.getString(msg.name() + ".title"));
		headerLbl.setBackground(parseColor(mstrings.getString(msg.name() + ".color")));

		JLabel myLabel = new JLabel("<html><p>" +
				mstrings.getString(msg.name() + ".detail") + "</p></html>");
		myLabel.setPreferredSize(new Dimension(1, 1));

		infoPane = myLabel;
		mainPane.add(myLabel, BorderLayout.CENTER);

		setVisible(true);
	}

	public void showZoneStatus(Micropolis engine, int xpos, int ypos, ZoneStatus zone)
	{
		headerLbl.setText(strings.getString("notification.query_hdr"));
		headerLbl.setBackground(QUERY_COLOR);

		String buildingStr = zone.getBuilding() == -1 ? "" : s_strings.getString("zone." + zone.getBuilding());
		String popDensityStr = s_strings.getString("status." + zone.getPopDensity());
		String landValueStr = s_strings.getString("status." + zone.getLandValue());
		String crimeLevelStr = s_strings.getString("status." + zone.getCrimeLevel());
		String pollutionStr = s_strings.getString("status." + zone.getPollution());
		String growthRateStr = s_strings.getString("status." + zone.getGrowthRate());

		setPicture(engine, xpos, ypos);

		if (infoPane != null) {
			mainPane.remove(infoPane);
			infoPane = null;
		}

		JPanel p = new JPanel(new GridBagLayout());
		mainPane.add(p, BorderLayout.CENTER);
		infoPane = p;

		GridBagConstraints c1 = new GridBagConstraints();
		GridBagConstraints c2 = new GridBagConstraints();

		c1.gridx = 0;
		c2.gridx = 1;
		c1.gridy = c2.gridy = 0;
		c1.anchor = GridBagConstraints.LINE_START;
		c2.anchor = GridBagConstraints.LINE_START;
		c1.insets = new Insets(0, 0, 0, 8);
		c2.weightx = 1.0;

		p.add(new JLabel(strings.getString("notification.zone_lbl")), c1);
		p.add(new JLabel(buildingStr), c2);

		c1.gridy = ++c2.gridy;
		p.add(new JLabel(strings.getString("notification.density_lbl")), c1);
		p.add(new JLabel(popDensityStr), c2);

		c1.gridy = ++c2.gridy;
		p.add(new JLabel(strings.getString("notification.value_lbl")), c1);
		p.add(new JLabel(landValueStr), c2);

		c1.gridy = ++c2.gridy;
		p.add(new JLabel(strings.getString("notification.crime_lbl")), c1);
		p.add(new JLabel(crimeLevelStr), c2);

		c1.gridy = ++c2.gridy;
		p.add(new JLabel(strings.getString("notification.pollution_lbl")), c1);
		p.add(new JLabel(pollutionStr), c2);

		c1.gridy = ++c2.gridy;
		p.add(new JLabel(strings.getString("notification.growth_lbl")), c1);
		p.add(new JLabel(growthRateStr), c2);

		c1.gridy++;
		c1.gridwidth = 2;
		c1.weighty = 1.0;
		p.add(new JLabel(), c1);

		setVisible(true);
	}
}
