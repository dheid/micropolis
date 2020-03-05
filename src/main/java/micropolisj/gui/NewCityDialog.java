// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.gui;

import micropolisj.engine.GameLevel;
import micropolisj.engine.MapGenerator;
import micropolisj.engine.Micropolis;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Stack;

import static micropolisj.gui.MainWindow.EXTENSION;

public class NewCityDialog extends JDialog
{
	private static final ResourceBundle strings = MainWindow.strings;
	private final JButton previousMapBtn;
	private final Stack<Micropolis> previousMaps = new Stack<>();
	private final Stack<Micropolis> nextMaps = new Stack<>();
	private final OverlayMapView mapPane;
	private final Map<Integer, JRadioButton> levelBtns = new HashMap<>();
	private Micropolis engine;

	public NewCityDialog(MainWindow owner, boolean showCancelOption)
	{
		super(owner);
		setTitle(strings.getString("welcome.caption"));
		setModal(true);

		assert owner != null;

		JPanel p1 = new JPanel(new BorderLayout());
		p1.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
		getContentPane().add(p1, BorderLayout.CENTER);

		engine = new Micropolis();
		new MapGenerator(engine).generateNewCity();

		mapPane = new OverlayMapView(engine);
		mapPane.setBorder(BorderFactory.createLoweredBevelBorder());
		p1.add(mapPane, BorderLayout.LINE_START);

		JPanel p2 = new JPanel(new BorderLayout());
		p1.add(p2, BorderLayout.CENTER);

		Box levelBox = new Box(BoxLayout.PAGE_AXIS);
		levelBox.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
		p2.add(levelBox, BorderLayout.CENTER);

		levelBox.add(Box.createVerticalGlue());
		JRadioButton radioBtn;
		for (int lev = GameLevel.MIN_LEVEL; lev <= GameLevel.MAX_LEVEL; lev++) {
			int x = lev;
			radioBtn = new JRadioButton(strings.getString("menu.difficulty." + lev));
			radioBtn.addActionListener(evt -> setGameLevel(x));
			levelBox.add(radioBtn);
			levelBtns.put(lev, radioBtn);
		}
		levelBox.add(Box.createVerticalGlue());
		setGameLevel(GameLevel.MIN_LEVEL);

		JPanel buttonPane = new JPanel();
		getContentPane().add(buttonPane, BorderLayout.PAGE_END);

		JButton btn;
		btn = new JButton(strings.getString("welcome.previous_map"));
		btn.addActionListener(evt -> onPreviousMapClicked());
		btn.setEnabled(false);
		buttonPane.add(btn);
		previousMapBtn = btn;

		btn = new JButton(strings.getString("welcome.play_this_map"));
		btn.addActionListener(evt -> onPlayClicked());
		buttonPane.add(btn);
		getRootPane().setDefaultButton(btn);

		btn = new JButton(strings.getString("welcome.next_map"));
		btn.addActionListener(evt -> onNextMapClicked());
		buttonPane.add(btn);

		btn = new JButton(strings.getString("welcome.load_city"));
		btn.addActionListener(evt -> onLoadCityClicked());
		buttonPane.add(btn);

		if (showCancelOption) {
			btn = new JButton(strings.getString("welcome.cancel"));
			btn.addActionListener(evt -> onCancelClicked());
		} else {
			btn = new JButton(strings.getString("welcome.quit"));
			btn.addActionListener(evt -> onQuitClicked());
		}
		buttonPane.add(btn);

		pack();
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(owner);
		getRootPane().registerKeyboardAction(evt -> dispose(),
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	private static void onQuitClicked()
	{
		System.exit(0);
	}

	private void onPreviousMapClicked()
	{
		if (previousMaps.isEmpty())
			return;

		nextMaps.push(engine);
		engine = previousMaps.pop();
		mapPane.setEngine(engine);

		previousMapBtn.setEnabled(!previousMaps.isEmpty());
	}

	private void onNextMapClicked()
	{
		if (nextMaps.isEmpty()) {
			Micropolis m = new Micropolis();
			new MapGenerator(m).generateNewCity();
			nextMaps.add(m);
		}

		previousMaps.push(engine);
		engine = nextMaps.pop();
		mapPane.setEngine(engine);

		previousMapBtn.setEnabled(true);
	}

	private void onLoadCityClicked()
	{
		try {
			JFileChooser fc = new JFileChooser();
			FileNameExtensionFilter filter1 = new FileNameExtensionFilter(strings.getString("cty_file"), EXTENSION);
			fc.setFileFilter(filter1);

			int rv = fc.showOpenDialog(this);
			if (rv == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile();
				Micropolis newEngine = new Micropolis();
				newEngine.load(file);
				startPlaying(newEngine, file);
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
			JOptionPane.showMessageDialog(this, e, strings.getString("main.error_caption"),
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void startPlaying(Micropolis newEngine, File file)
	{
		MainWindow win = (MainWindow) getOwner();
		win.setEngine(newEngine);
		win.setCurrentFile(file);
		win.makeClean();
		dispose();
	}

	private void onPlayClicked()
	{
		engine.setGameLevel(getSelectedGameLevel());
		engine.setFunds(GameLevel.getStartingFunds(engine.getGameLevel()));
		startPlaying(engine, null);
	}

	private void onCancelClicked()
	{
		dispose();
	}

	private int getSelectedGameLevel()
	{
		for (Map.Entry<Integer, JRadioButton> entry : levelBtns.entrySet()) {
			if (entry.getValue().isSelected()) {
				return entry.getKey();
			}
		}
		return GameLevel.MIN_LEVEL;
	}

	private void setGameLevel(int level)
	{
		for (Map.Entry<Integer, JRadioButton> entry : levelBtns.entrySet()) {
			entry.getValue().setSelected(entry.getKey() == level);
		}
	}
}
