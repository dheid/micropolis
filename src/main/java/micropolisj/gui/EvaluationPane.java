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
import micropolisj.engine.CityProblem;
import micropolisj.engine.Micropolis;
import micropolisj.engine.MicropolisMessage;
import micropolisj.engine.Sound;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.NumberFormat;
import java.util.ResourceBundle;

import static micropolisj.gui.MainWindow.formatFunds;

public class EvaluationPane extends JPanel
		implements CityListener
{
	private static final ResourceBundle cstrings = ResourceBundle.getBundle("strings.CityStrings");
	private static final ResourceBundle gstrings = MainWindow.strings;
	private Micropolis engine;
	private JLabel yesLbl;
	private JLabel noLbl;
	private JLabel[] voterProblemLbl;
	private JLabel[] voterCountLbl;
	private JLabel popLbl;
	private JLabel deltaLbl;
	private JLabel assessLbl;
	private JLabel cityClassLbl;
	private JLabel gameLevelLbl;
	private JLabel scoreLbl;
	private JLabel scoreDeltaLbl;

	public EvaluationPane(Micropolis engine)
	{
		super(new BorderLayout());

		JButton dismissBtn = new JButton(gstrings.getString("dismiss-evaluation"));
		dismissBtn.addActionListener(evt -> onDismissClicked());
		add(dismissBtn, BorderLayout.PAGE_END);

		Box b1 = new Box(BoxLayout.LINE_AXIS);
		add(b1, BorderLayout.CENTER);

		b1.add(makePublicOpinionPane());
		b1.add(new JSeparator(SwingConstants.VERTICAL));
		b1.add(makeStatisticsPane());

		assert engine != null;
		setEngine(engine);
	}

	private static String getCityClassName(int cityClass)
	{
		return cstrings.getString("class." + cityClass);
	}

	private static String getGameLevelName(int gameLevel)
	{
		return cstrings.getString("level." + gameLevel);
	}

	public void setEngine(Micropolis newEngine)
	{
		if (engine != null) { //old engine
			engine.removeListener(this);
		}
		engine = newEngine;
		if (engine != null) { //new engine
			engine.addListener(this);
			loadEvaluation();
		}
	}

	private void onDismissClicked()
	{
		setVisible(false);
	}

	private JComponent makePublicOpinionPane()
	{
		JPanel me = new JPanel(new GridBagLayout());
		GridBagConstraints c1 = new GridBagConstraints();
		GridBagConstraints c2 = new GridBagConstraints();
		GridBagConstraints c3 = new GridBagConstraints();

		// c1 is for the full-width headers
		c1.gridx = c1.gridy = 0;
		c1.gridwidth = 2;
		c1.gridheight = 1;
		c1.weightx = 1.0;
		c1.fill = GridBagConstraints.NONE;
		c1.anchor = GridBagConstraints.PAGE_START;

		JLabel headerLbl = new JLabel(gstrings.getString("public-opinion"));
		Font curFont = headerLbl.getFont();
		headerLbl.setFont(
				curFont.deriveFont(curFont.getStyle() | Font.BOLD, (float) (curFont.getSize() * 1.2))
		);
		me.add(headerLbl, c1);

		c1.gridy = 1;
		c1.insets = new Insets(3, 0, 3, 0);
		me.add(new JLabel(gstrings.getString("public-opinion-1")), c1);

		c1.gridy = 4;
		me.add(new JLabel(gstrings.getString("public-opinion-2")), c1);

		c2.gridx = 0;
		c2.gridy = 2;
		c2.gridwidth = c2.gridheight = 1;
		c2.weightx = 1.0;
		c2.anchor = GridBagConstraints.LINE_END;
		c2.insets = new Insets(0, 0, 0, 4);

		me.add(new JLabel(gstrings.getString("public-opinion-yes")), c2);

		c2.gridy = 3;
		me.add(new JLabel(gstrings.getString("public-opinion-no")), c2);

		c3.gridx = 1;
		c3.gridwidth = c3.gridheight = 1;
		c3.weightx = 1.0;
		c3.anchor = GridBagConstraints.LINE_START;
		c3.insets = new Insets(0, 4, 0, 0);

		c3.gridy = 2;
		yesLbl = new JLabel();
		me.add(yesLbl, c3);

		c3.gridy = 3;
		noLbl = new JLabel();
		me.add(noLbl, c3);

		c2.gridy = c3.gridy = 5;

		int numProblems = 4;
		voterProblemLbl = new JLabel[numProblems];
		voterCountLbl = new JLabel[numProblems];
		for (int i = 0; i < numProblems; i++) {
			voterProblemLbl[i] = new JLabel();
			me.add(voterProblemLbl[i], c2);

			voterCountLbl[i] = new JLabel();
			me.add(voterCountLbl[i], c3);

			c2.gridy = ++c3.gridy;
		}

		// add glue so that everything will align towards the top
		c1.gridy = 999;
		c1.weighty = 1.0;
		me.add(new JLabel(), c1);

		return me;
	}

	private JComponent makeStatisticsPane()
	{
		JPanel me = new JPanel(new GridBagLayout());
		GridBagConstraints c1 = new GridBagConstraints();
		GridBagConstraints c2 = new GridBagConstraints();
		GridBagConstraints c3 = new GridBagConstraints();

		c1.gridx = c1.gridy = 0;
		c1.gridwidth = 2;
		c1.gridheight = 1;
		c1.weightx = 1.0;
		c1.fill = GridBagConstraints.NONE;
		c1.anchor = GridBagConstraints.PAGE_START;
		c1.insets = new Insets(0, 0, 3, 0);

		JLabel headerLbl = new JLabel(gstrings.getString("statistics-head"));
		Font curFont = headerLbl.getFont();
		headerLbl.setFont(
				curFont.deriveFont(curFont.getStyle() | Font.BOLD, (float) (curFont.getSize() * 1.2))
		);
		me.add(headerLbl, c1);

		c1.gridy = 20;
		c1.insets = new Insets(9, 0, 3, 0);
		c1.fill = GridBagConstraints.VERTICAL;
		JLabel header2Lbl = new JLabel(gstrings.getString("city-score-head"));
		me.add(header2Lbl, c1);

		c2.gridx = 0;
		c2.gridwidth = c2.gridheight = 1;
		c2.weightx = 0.5;
		c2.anchor = GridBagConstraints.LINE_END;
		c2.insets = new Insets(0, 0, 0, 4);

		c3.gridx = 1;
		c3.gridwidth = c3.gridheight = 1;
		c3.weightx = 0.5;
		c3.anchor = GridBagConstraints.LINE_START;
		c3.insets = new Insets(0, 4, 0, 0);

		c2.gridy = c3.gridy = 1;
		me.add(new JLabel(gstrings.getString("stats-population")), c2);
		popLbl = new JLabel();
		me.add(popLbl, c3);

		c2.gridy = ++c3.gridy;
		me.add(new JLabel(gstrings.getString("stats-net-migration")), c2);
		deltaLbl = new JLabel();
		me.add(deltaLbl, c3);

		c2.gridy = ++c3.gridy;
		me.add(new JLabel(gstrings.getString("stats-last-year")), c2);

		c2.gridy = ++c3.gridy;
		me.add(new JLabel(gstrings.getString("stats-assessed-value")), c2);
		assessLbl = new JLabel();
		me.add(assessLbl, c3);

		c2.gridy = ++c3.gridy;
		me.add(new JLabel(gstrings.getString("stats-category")), c2);
		cityClassLbl = new JLabel();
		me.add(cityClassLbl, c3);

		c2.gridy = ++c3.gridy;
		me.add(new JLabel(gstrings.getString("stats-game-level")), c2);
		gameLevelLbl = new JLabel();
		me.add(gameLevelLbl, c3);

		c2.gridy = c3.gridy = 21;
		me.add(new JLabel(gstrings.getString("city-score-current")), c2);
		scoreLbl = new JLabel();
		me.add(scoreLbl, c3);

		c2.gridy = ++c3.gridy;
		me.add(new JLabel(gstrings.getString("city-score-change")), c2);
		scoreDeltaLbl = new JLabel();
		me.add(scoreDeltaLbl, c3);

		// add glue so that everything will align towards the top
		c1.gridy = 999;
		c1.weighty = 1.0;
		c1.insets = new Insets(0, 0, 0, 0);
		me.add(new JLabel(), c1);

		return me;
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
	public void evaluationChanged()
	{
		loadEvaluation();
	}

	private void loadEvaluation()
	{
		NumberFormat pctFmt = NumberFormat.getPercentInstance();
		yesLbl.setText(pctFmt.format(0.01 * engine.getEvaluation().getCityYes()));
		noLbl.setText(pctFmt.format(0.01 * engine.getEvaluation().getCityNo()));

		for (int i = 0; i < voterProblemLbl.length; i++) {
			CityProblem p = i < engine.getEvaluation().getProblemOrder().length ? engine.getEvaluation().getProblemOrder()[i] : null;
			int numVotes = p != null ? engine.getEvaluation().getProblemVotes().get(p) : 0;

			if (numVotes == 0) {
				voterProblemLbl[i].setVisible(false);
				voterCountLbl[i].setVisible(false);
			} else {
				voterProblemLbl[i].setText(cstrings.getString("problem." + p.name()));
				voterCountLbl[i].setText(pctFmt.format(0.01 * numVotes));
				voterProblemLbl[i].setVisible(true);
				voterCountLbl[i].setVisible(true);
			}
		}

		NumberFormat nf = NumberFormat.getInstance();
		popLbl.setText(nf.format(engine.getEvaluation().getCityPop()));
		deltaLbl.setText(nf.format(engine.getEvaluation().getDeltaCityPop()));
		assessLbl.setText(formatFunds(engine.getEvaluation().getCityAssValue()));
		cityClassLbl.setText(getCityClassName(engine.getEvaluation().getCityClass()));
		gameLevelLbl.setText(getGameLevelName(engine.getGameLevel()));
		scoreLbl.setText(nf.format(engine.getEvaluation().getCityScore()));
		scoreDeltaLbl.setText(nf.format(engine.getEvaluation().getDeltaCityScore()));
	}
}
