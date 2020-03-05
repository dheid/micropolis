// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.gui;

import micropolisj.engine.BudgetNumbers;
import micropolisj.engine.FinancialHistory;
import micropolisj.engine.Micropolis;
import micropolisj.engine.Speed;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;

import static micropolisj.gui.MainWindow.formatFunds;
import static micropolisj.gui.MainWindow.formatGameDate;

public class BudgetDialog extends JDialog
{
	private static final ResourceBundle strings = MainWindow.strings;
	private final Micropolis engine;
	private final JSpinner taxRateEntry;
	private final int origTaxRate;
	private final double origRoadPct;
	private final double origFirePct;
	private final double origPolicePct;
	private final JLabel roadFundRequest = new JLabel();
	private final JLabel roadFundAlloc = new JLabel();
	private final JSlider roadFundEntry;
	private final JLabel policeFundRequest = new JLabel();
	private final JLabel policeFundAlloc = new JLabel();
	private final JSlider policeFundEntry;
	private final JLabel fireFundRequest = new JLabel();
	private final JLabel fireFundAlloc = new JLabel();
	private final JSlider fireFundEntry;
	private final JLabel taxRevenueLbl = new JLabel();
	private final JCheckBox autoBudgetBtn = new JCheckBox(strings.getString("budgetdlg.auto_budget"));
	private final JCheckBox pauseBtn = new JCheckBox(strings.getString("budgetdlg.pause_game"));

	public BudgetDialog(Window owner, Micropolis engine)
	{
		super(owner);
		setTitle(strings.getString("budgetdlg.title"));

		this.engine = engine;
		origTaxRate = engine.getCityTax();
		origRoadPct = engine.getRoadPercent();
		origFirePct = engine.getFirePercent();
		origPolicePct = engine.getPolicePercent();

		// give text fields of the fund-level spinners a minimum size
		taxRateEntry = new JSpinner(new SpinnerNumberModel(7, 0, 20, 1));

		// widgets to set funding levels
		roadFundEntry = new JSlider(SwingConstants.HORIZONTAL, 0, 100, 100);
		adjustSliderSize(roadFundEntry);
		fireFundEntry = new JSlider(SwingConstants.HORIZONTAL, 0, 100, 100);
		adjustSliderSize(fireFundEntry);
		policeFundEntry = new JSlider(SwingConstants.HORIZONTAL, 0, 100, 100);
		adjustSliderSize(policeFundEntry);

		ChangeListener change = ev -> applyChange();
		taxRateEntry.addChangeListener(change);
		roadFundEntry.addChangeListener(change);
		fireFundEntry.addChangeListener(change);
		policeFundEntry.addChangeListener(change);

		Box mainBox = new Box(BoxLayout.PAGE_AXIS);
		mainBox.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		add(mainBox, BorderLayout.CENTER);

		mainBox.add(makeTaxPane());

		JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
		mainBox.add(sep);

		mainBox.add(makeFundingRatesPane());

		JSeparator sep1 = new JSeparator(SwingConstants.HORIZONTAL);
		mainBox.add(sep1);

		mainBox.add(makeBalancePane());

		JSeparator sep2 = new JSeparator(SwingConstants.HORIZONTAL);
		mainBox.add(sep2);

		mainBox.add(makeOptionsPane());

		JPanel buttonPane = new JPanel();
		add(buttonPane, BorderLayout.PAGE_END);

		JButton continueBtn = new JButton(strings.getString("budgetdlg.continue"));
		continueBtn.addActionListener(ev -> onContinueClicked());
		buttonPane.add(continueBtn);

		JButton resetBtn = new JButton(strings.getString("budgetdlg.reset"));
		resetBtn.addActionListener(ev -> onResetClicked());
		buttonPane.add(resetBtn);

		loadBudgetNumbers(true);
		setAutoRequestFocus_compat();
		pack();
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(owner);
		getRootPane().registerKeyboardAction(evt -> dispose(),
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	private static void adjustSliderSize(JSlider slider)
	{
		Dimension sz = slider.getPreferredSize();
		slider.setPreferredSize(
				new Dimension(80, sz.height)
		);
	}

	private void applyChange()
	{
		int newTaxRate = ((Number) taxRateEntry.getValue()).intValue();
		int newRoadPct = ((Number) roadFundEntry.getValue()).intValue();
		int newPolicePct = ((Number) policeFundEntry.getValue()).intValue();
		int newFirePct = ((Number) fireFundEntry.getValue()).intValue();

		engine.setCityTax(newTaxRate);
		engine.setRoadPercent(newRoadPct / 100.0);
		engine.setPolicePercent(newPolicePct / 100.0);
		engine.setFirePercent(newFirePct / 100.0);

		loadBudgetNumbers(false);
	}

	private void loadBudgetNumbers(boolean updateEntries)
	{
		BudgetNumbers b = engine.generateBudget();
		if (updateEntries) {
			taxRateEntry.setValue(b.getTaxRate());
			roadFundEntry.setValue((int) Math.round(b.getRoadPercent() * 100.0));
			policeFundEntry.setValue((int) Math.round(b.getPolicePercent() * 100.0));
			fireFundEntry.setValue((int) Math.round(b.getFirePercent() * 100.0));
		}

		taxRevenueLbl.setText(formatFunds(b.getTaxIncome()));

		roadFundRequest.setText(formatFunds(b.getRoadRequest()));
		roadFundAlloc.setText(formatFunds(b.getRoadFunded()));

		policeFundRequest.setText(formatFunds(b.getPoliceRequest()));
		policeFundAlloc.setText(formatFunds(b.getPoliceFunded()));

		fireFundRequest.setText(formatFunds(b.getFireRequest()));
		fireFundAlloc.setText(formatFunds(b.getFireFunded()));
	}

	private void setAutoRequestFocus_compat()
	{
		try {
			if (getClass().getMethod("setAutoRequestFocus", boolean.class) != null) {
				setAutoRequestFocus(false);
			}
		} catch (NoSuchMethodException e) {
			// ok to ignore
		}
	}

	private JComponent makeFundingRatesPane()
	{
		JPanel fundingRatesPane = new JPanel(new GridBagLayout());
		fundingRatesPane.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

		GridBagConstraints c0 = new GridBagConstraints();
		c0.gridx = 0;
		c0.weightx = 0.25;
		c0.anchor = GridBagConstraints.LINE_START;
		GridBagConstraints c1 = new GridBagConstraints();
		c1.gridx = 1;
		c1.weightx = 0.25;
		c1.anchor = GridBagConstraints.LINE_END;
		GridBagConstraints c2 = new GridBagConstraints();
		c2.gridx = 2;
		c2.weightx = 0.5;
		c2.anchor = GridBagConstraints.LINE_END;
		GridBagConstraints c3 = new GridBagConstraints();
		c3.gridx = 3;
		c3.weightx = 0.5;
		c3.anchor = GridBagConstraints.LINE_END;

		c1.gridy = c2.gridy = c3.gridy = 0;
		fundingRatesPane.add(new JLabel(strings.getString("budgetdlg.funding_level_hdr")), c1);
		fundingRatesPane.add(new JLabel(strings.getString("budgetdlg.requested_hdr")), c2);
		fundingRatesPane.add(new JLabel(strings.getString("budgetdlg.allocation_hdr")), c3);

		c0.gridy = c1.gridy = c2.gridy = c3.gridy = 1;
		fundingRatesPane.add(new JLabel(strings.getString("budgetdlg.road_fund")), c0);
		fundingRatesPane.add(roadFundEntry, c1);
		fundingRatesPane.add(roadFundRequest, c2);
		fundingRatesPane.add(roadFundAlloc, c3);

		c0.gridy = c1.gridy = c2.gridy = c3.gridy = 2;
		fundingRatesPane.add(new JLabel(strings.getString("budgetdlg.police_fund")), c0);
		fundingRatesPane.add(policeFundEntry, c1);
		fundingRatesPane.add(policeFundRequest, c2);
		fundingRatesPane.add(policeFundAlloc, c3);

		c0.gridy = c1.gridy = c2.gridy = c3.gridy = 3;
		fundingRatesPane.add(new JLabel(strings.getString("budgetdlg.fire_fund")), c0);
		fundingRatesPane.add(fireFundEntry, c1);
		fundingRatesPane.add(fireFundRequest, c2);
		fundingRatesPane.add(fireFundAlloc, c3);

		return fundingRatesPane;
	}

	private JComponent makeOptionsPane()
	{
		JPanel optionsPane = new JPanel(new GridBagLayout());
		optionsPane.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

		GridBagConstraints c0 = new GridBagConstraints();
		GridBagConstraints c1 = new GridBagConstraints();

		c0.gridx = 0;
		c1.gridx = 1;
		c0.anchor = c1.anchor = GridBagConstraints.LINE_START;
		c0.gridy = c1.gridy = 0;
		c0.weightx = c1.weightx = 0.5;
		optionsPane.add(autoBudgetBtn, c0);
		optionsPane.add(pauseBtn, c1);

		autoBudgetBtn.setSelected(engine.isAutoBudget());
		pauseBtn.setSelected(engine.getSimSpeed() == Speed.PAUSED);

		return optionsPane;
	}

	private JComponent makeTaxPane()
	{
		JPanel pane = new JPanel(new GridBagLayout());
		pane.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

		GridBagConstraints c0 = new GridBagConstraints();
		GridBagConstraints c1 = new GridBagConstraints();
		GridBagConstraints c2 = new GridBagConstraints();

		c0.gridx = 0;
		c0.anchor = GridBagConstraints.LINE_START;
		c0.weightx = 0.25;
		c1.gridx = 1;
		c1.anchor = GridBagConstraints.LINE_END;
		c1.weightx = 0.25;
		c2.gridx = 2;
		c2.anchor = GridBagConstraints.LINE_END;
		c2.weightx = 0.5;

		c0.gridy = c1.gridy = c2.gridy = 0;
		pane.add(new JLabel(strings.getString("budgetdlg.tax_rate_hdr")), c1);
		pane.add(new JLabel(strings.getString("budgetdlg.annual_receipts_hdr")), c2);

		c0.gridy = c1.gridy = c2.gridy = 1;
		pane.add(new JLabel(strings.getString("budgetdlg.tax_revenue")), c0);
		pane.add(taxRateEntry, c1);
		pane.add(taxRevenueLbl, c2);

		return pane;
	}

	private void onContinueClicked()
	{
		if (autoBudgetBtn.isSelected() != engine.isAutoBudget()) {
			engine.toggleAutoBudget();
		}
		if (pauseBtn.isSelected() && engine.getSimSpeed() != Speed.PAUSED) {
			engine.setSpeed(Speed.PAUSED);
		} else if (!pauseBtn.isSelected() && engine.getSimSpeed() == Speed.PAUSED) {
			engine.setSpeed(Speed.NORMAL);
		}

		dispose();
	}

	private void onResetClicked()
	{
		engine.setCityTax(origTaxRate);
		engine.setRoadPercent(origRoadPct);
		engine.setFirePercent(origFirePct);
		engine.setPolicePercent(origPolicePct);
		loadBudgetNumbers(true);
	}

	private JComponent makeBalancePane()
	{
		JPanel balancePane = new JPanel(new GridBagLayout());
		balancePane.setBorder(BorderFactory.createEmptyBorder(8, 24, 8, 24));

		GridBagConstraints c0 = new GridBagConstraints();
		GridBagConstraints c1 = new GridBagConstraints();

		c0.anchor = GridBagConstraints.LINE_START;
		c0.weightx = 0.5;
		c0.gridx = 0;
		c0.gridy = 0;

		JLabel thLbl = new JLabel(strings.getString("budgetdlg.period_ending"));
		Font origFont = thLbl.getFont();
		Font headFont = origFont.deriveFont(Font.ITALIC);
		thLbl.setFont(headFont);
		thLbl.setForeground(Color.MAGENTA);
		balancePane.add(thLbl, c0);

		c0.gridy++;
		balancePane.add(new JLabel(strings.getString("budgetdlg.cash_begin")), c0);
		c0.gridy++;
		balancePane.add(new JLabel(strings.getString("budgetdlg.taxes_collected")), c0);
		c0.gridy++;
		balancePane.add(new JLabel(strings.getString("budgetdlg.capital_expenses")), c0);
		c0.gridy++;
		balancePane.add(new JLabel(strings.getString("budgetdlg.operating_expenses")), c0);
		c0.gridy++;
		balancePane.add(new JLabel(strings.getString("budgetdlg.cash_end")), c0);

		c1.anchor = GridBagConstraints.LINE_END;
		c1.weightx = 0.25;
		c1.gridx = 0;

		for (int i = 0; i < 2; i++) {

			if (i + 1 >= engine.getFinancialHistory().size()) {
				break;
			}

			FinancialHistory f = engine.getFinancialHistory().get(i);
			FinancialHistory fPrior = engine.getFinancialHistory().get(i + 1);
			int cashFlow = f.getTotalFunds() - fPrior.getTotalFunds();
			int capExpenses = -(cashFlow - f.getTaxIncome() + f.getOperatingExpenses());

			c1.gridx++;
			c1.gridy = 0;

			thLbl = new JLabel(formatGameDate(f.getCityTime() - 1));
			thLbl.setFont(headFont);
			thLbl.setForeground(Color.MAGENTA);
			balancePane.add(thLbl, c1);

			c1.gridy++;
			JLabel previousBalanceLbl = new JLabel();
			previousBalanceLbl.setText(formatFunds(fPrior.getTotalFunds()));
			balancePane.add(previousBalanceLbl, c1);

			c1.gridy++;
			JLabel taxIncomeLbl = new JLabel();
			taxIncomeLbl.setText(formatFunds(f.getTaxIncome()));
			balancePane.add(taxIncomeLbl, c1);

			c1.gridy++;
			JLabel capExpensesLbl = new JLabel();
			capExpensesLbl.setText(formatFunds(capExpenses));
			balancePane.add(capExpensesLbl, c1);

			c1.gridy++;
			JLabel opExpensesLbl = new JLabel();
			opExpensesLbl.setText(formatFunds(f.getOperatingExpenses()));
			balancePane.add(opExpensesLbl, c1);

			c1.gridy++;
			JLabel newBalanceLbl = new JLabel();
			newBalanceLbl.setText(formatFunds(f.getTotalFunds()));
			balancePane.add(newBalanceLbl, c1);
		}

		return balancePane;
	}
}
