// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.gui;

import micropolisj.engine.CityListener;
import micropolisj.engine.CityLocation;
import micropolisj.engine.Micropolis;
import micropolisj.engine.MicropolisMessage;
import micropolisj.engine.Sound;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.ResourceBundle;

import static micropolisj.gui.ColorParser.parseColor;

public class GraphsPane extends JPanel
		implements CityListener
{
	private static final ResourceBundle strings = MainWindow.strings;
	private static final int LEFT_MARGIN = 4;
	private static final int RIGHT_MARGIN = 4;
	private static final int TOP_MARGIN = 2;
	private static final int BOTTOM_MARGIN = 2;
	private static final int LEGEND_PADDING = 6;
	private static final GraphData[] NO_GRAPH_DATA = new GraphData[0];
	private final JToggleButton tenYearsBtn;
	private final JToggleButton onetwentyYearsBtn;
	private final GraphArea graphArea;
	private final Map<GraphData, JToggleButton> dataBtns = new EnumMap<>(GraphData.class);
	private Micropolis engine;
	public GraphsPane(Micropolis engine)
	{
		super(new BorderLayout());

		assert engine != null;
		this.engine = engine;
		engine.addListener(this);

		JButton dismissBtn = new JButton(strings.getString("dismiss_graph"));
		dismissBtn.addActionListener(evt -> onDismissClicked());
		add(dismissBtn, BorderLayout.PAGE_END);

		JPanel b1 = new JPanel(new BorderLayout());
		add(b1, BorderLayout.CENTER);

		JPanel toolsPane = new JPanel(new GridBagLayout());
		b1.add(toolsPane, BorderLayout.LINE_START);

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = c.gridy = 0;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(1, 1, 1, 1);
		tenYearsBtn = new JToggleButton(strings.getString("ten_years"));
		tenYearsBtn.setMargin(new Insets(0, 0, 0, 0));
		tenYearsBtn.addActionListener(evt -> setTimePeriod(TimePeriod.TEN_YEARS));
		toolsPane.add(tenYearsBtn, c);

		c.gridy++;
		onetwentyYearsBtn = new JToggleButton(strings.getString("onetwenty_years"));
		onetwentyYearsBtn.setMargin(new Insets(0, 0, 0, 0));
		onetwentyYearsBtn.addActionListener(evt -> setTimePeriod(TimePeriod.ONETWENTY_YEARS));
		toolsPane.add(onetwentyYearsBtn, c);

		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.PAGE_START;
		c.weightx = 0.5;
		toolsPane.add(makeDataBtn(GraphData.RESPOP), c);

		c.gridy = 3;
		toolsPane.add(makeDataBtn(GraphData.COMPOP), c);

		c.gridy = 4;
		toolsPane.add(makeDataBtn(GraphData.INDPOP), c);

		c.gridx = 1;
		c.gridy = 2;
		toolsPane.add(makeDataBtn(GraphData.MONEY), c);

		c.gridy = 3;
		toolsPane.add(makeDataBtn(GraphData.CRIME), c);

		c.gridy = 4;
		toolsPane.add(makeDataBtn(GraphData.POLLUTION), c);

		graphArea = new GraphArea();
		b1.add(graphArea, BorderLayout.CENTER);

		setTimePeriod(TimePeriod.TEN_YEARS);
		dataBtns.get(GraphData.MONEY).setSelected(true);
		dataBtns.get(GraphData.POLLUTION).setSelected(true);
	}

	public void setEngine(Micropolis newEngine)
	{
		if (engine != null) {  //old engine
			engine.removeListener(this);
		}
		engine = newEngine;
		if (engine != null) {  //new engine
			engine.addListener(this);
			graphArea.repaint();
		}
	}

	private void onDismissClicked()
	{
		setVisible(false);
	}

	@Override
	public void cityMessage(MicropolisMessage message, CityLocation loc)
	{
	}

	@Override
	public void citySound(Sound sound, CityLocation loc)
	{
	}

	@Override
	public void fundsChanged()
	{
	}

	@Override
	public void optionsChanged()
	{
	}

	@Override
	public void censusChanged()
	{
		graphArea.repaint();
	}

	private JToggleButton makeDataBtn(GraphData graph)
	{
		String icon1name = strings.getString("graph_button." + graph.name());
		String icon2name = strings.getString("graph_button." + graph.name() + ".selected");

		Icon icon1 = new ImageIcon(getClass().getResource("/" + icon1name));
		Icon icon2 = new ImageIcon(getClass().getResource("/" + icon2name));

		JToggleButton btn = new JToggleButton();
		btn.setIcon(icon1);
		btn.setSelectedIcon(icon2);
		btn.setBorder(null);
		btn.setBorderPainted(false);
		btn.setFocusPainted(false);
		btn.setContentAreaFilled(false);
		btn.setMargin(new Insets(0, 0, 0, 0));

		btn.addActionListener(evt -> graphArea.repaint());

		dataBtns.put(graph, btn);
		return btn;
	}

	private void setTimePeriod(TimePeriod period)
	{
		tenYearsBtn.setSelected(period == TimePeriod.TEN_YEARS);
		onetwentyYearsBtn.setSelected(period == TimePeriod.ONETWENTY_YEARS);
		graphArea.repaint();
	}

	enum TimePeriod
	{
		TEN_YEARS,
		ONETWENTY_YEARS
	}

	enum GraphData
	{
		RESPOP,
		COMPOP,
		INDPOP,
		MONEY,
		CRIME,
		POLLUTION
	}

	private class GraphArea extends JComponent
	{
		private GraphArea()
		{
			setBorder(BorderFactory.createLoweredBevelBorder());
		}

		@Override
		public void paintComponent(Graphics g)
		{
			Graphics2D gr = (Graphics2D) g;
			FontMetrics fm = gr.getFontMetrics();

			gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			gr.setColor(Color.WHITE);
			gr.fill(gr.getClipBounds());

			// determine length of longest label
			int maxLabelWidth = 0;
			for (GraphData gd : GraphData.values()) {
				String labelStr = strings.getString("graph_label." + gd.name());
				int adv = fm.stringWidth(labelStr);
				if (adv > maxLabelWidth) {
					maxLabelWidth = adv;
				}
			}

			int leftEdge = getInsets().left + LEFT_MARGIN;
			int topEdge = getInsets().top + TOP_MARGIN + fm.getHeight() * 2;
			int bottomEdge = getHeight() - getInsets().bottom - getInsets().top - BOTTOM_MARGIN;
			int rightEdge = getWidth() - getInsets().right - getInsets().left - RIGHT_MARGIN - maxLabelWidth - LEGEND_PADDING;

			// draw graph lower, upper borders
			gr.setColor(Color.BLACK);
			gr.drawLine(leftEdge, topEdge, rightEdge, topEdge);
			gr.drawLine(leftEdge, bottomEdge, rightEdge, bottomEdge);

			// draw vertical bars and label the dates
			boolean isOneTwenty = onetwentyYearsBtn.isSelected();
			int unitPeriod = isOneTwenty ? 12 * Micropolis.CENSUSRATE : Micropolis.CENSUSRATE;
			int hashPeriod = isOneTwenty ? 10 * unitPeriod : 12 * unitPeriod;
			int startTime = (engine.getHistory().getCityTime() / unitPeriod - 119) * unitPeriod;

			double xInterval = (rightEdge - leftEdge) / 120.0;
			for (int i = 0; i < 120; i++) {
				int t = startTime + i * unitPeriod;  // t might be negative
				if (t % hashPeriod == 0) {
					// year
					int year = 1900 + t / (12 * Micropolis.CENSUSRATE);
					int numHashes = t / hashPeriod;
					int x = (int) Math.round(leftEdge + i * xInterval);
					int y = getInsets().top + TOP_MARGIN +
							(numHashes % 2 == 0 ? fm.getHeight() : 0) +
							fm.getAscent();
					gr.drawString(Integer.toString(year), x, y);
					gr.drawLine(x, topEdge, x, bottomEdge);
				}
			}

			int history = isOneTwenty ? 239 : 119;
			Map<GraphData, Path2D.Double> paths = new EnumMap(GraphData.class);
			double scale = Math.max(256.0, getHistoryMax());
			for (GraphData gd : GraphData.values()) {
				if (dataBtns.get(gd).isSelected()) {

					Path2D.Double path = new Path2D.Double();
					for (int i = 0; i < 120; i++) {
						double xp = leftEdge + i * xInterval;
						double yp = bottomEdge - getHistoryValue(gd, history - i) * (bottomEdge - topEdge) / scale;
						if (i == 0) {
							path.moveTo(xp, yp);
						} else {
							path.lineTo(xp, yp);
						}
					}
					paths.put(gd, path);
				}
			}

			GraphData[] myGraphs = paths.keySet().toArray(NO_GRAPH_DATA);
			Arrays.sort(myGraphs, (a, b) -> {
				double y0 = paths.get(a).getCurrentPoint().getY();
				double y1 = paths.get(b).getCurrentPoint().getY();
				return -Double.compare(y0, y1);
			});

			int lbottom = bottomEdge;
			for (GraphData gd : myGraphs) {
				String labelStr = strings.getString("graph_label." + gd.name());
				String colStr = strings.getString("graph_color." + gd.name());
				Color col = parseColor(colStr);
				Path2D.Double path = paths.get(gd);

				gr.setColor(col);
				gr.setStroke(new BasicStroke(2));
				gr.draw(path);

				int x = rightEdge + LEGEND_PADDING;
				int y = (int) Math.round(path.getCurrentPoint().getY() + fm.getAscent() / 2.0);
				y = Math.min(lbottom, y);
				lbottom = y - fm.getAscent();

				gr.setColor(col);
				gr.drawString(labelStr, x - 1, y);
				gr.drawString(labelStr, x, y - 1);

				gr.setColor(Color.BLACK);
				gr.drawString(labelStr, x, y);
			}
		}

		private int getHistoryMax()
		{
			int max = 0;
			for (GraphData g : GraphData.values()) {
				for (int pos = 0; pos < 240; pos++) {
					max = Math.max(max, getHistoryValue(g, pos));
				}
			}
			return max;
		}


		private int getHistoryValue(GraphData graph, int pos)
		{
			assert pos >= 0 && pos < 240;
			switch (graph) {
				case RESPOP:
					return engine.getHistory().getRes()[pos];
				case COMPOP:
					return engine.getHistory().getCom()[pos];
				case INDPOP:
					return engine.getHistory().getInd()[pos];
				case MONEY:
					return engine.getHistory().getMoney()[pos];
				case CRIME:
					return engine.getHistory().getCrime()[pos];
				case POLLUTION:
					return engine.getHistory().getPollution()[pos];
				default:
					throw new Error("unexpected");
			}
		}

	}
}
