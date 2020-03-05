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
import micropolisj.engine.CityRect;
import micropolisj.engine.Disaster;
import micropolisj.engine.EarthquakeListener;
import micropolisj.engine.GameLevel;
import micropolisj.engine.MapState;
import micropolisj.engine.Micropolis;
import micropolisj.engine.MicropolisMessage;
import micropolisj.engine.MicropolisTool;
import micropolisj.engine.Sound;
import micropolisj.engine.Speed;
import micropolisj.engine.ToolResult;
import micropolisj.engine.ToolStroke;
import micropolisj.engine.ZoneStatus;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class MainWindow extends JFrame
		implements CityListener, EarthquakeListener
{
	static final ResourceBundle strings = ResourceBundle.getBundle("strings.GuiStrings");
	static final String EXTENSION = "cty";
	private static final ImageIcon appIcon;
	private static final String PRODUCT_NAME = strings.getString("PRODUCT");
	private static final String SOUNDS_PREF = "enable_sounds";

	static {
		appIcon = new ImageIcon(MainWindow.class.getResource("/micropolism.png"));
	}

	private final MicropolisDrawingArea drawingArea;
	private final JScrollPane drawingAreaScroll;
	private final DemandIndicator demandInd;
	private final MessagesPane messagesPane;
	private final JLabel mapLegendLbl;
	private final OverlayMapView mapView;
	private final NotificationPane notificationPane;
	private final EvaluationPane evaluationPane;
	private final GraphsPane graphsPane;
	private final Map<MapState, JMenuItem> mapStateMenuItems = new EnumMap<>(MapState.class);
	private File currentFile;
	private Micropolis engine;
	private JLabel dateLbl;
	private JLabel fundsLbl;
	private JLabel popLbl;
	private JLabel currentToolLbl;
	private JLabel currentToolCostLbl;
	private Map<MicropolisTool, JToggleButton> toolButtons;
	private MicropolisTool currentTool;
	private boolean doSounds;
	private boolean dirty1;  //indicates if a tool was successfully applied since last save
	private boolean dirty2;  //indicates if simulator took a step since last save
	private long lastSavedTime;  //real-time clock of when file was last saved
	private boolean autoBudgetPending;
	private JMenuItem autoBudgetMenuItem;
	private JMenuItem autoBulldozeMenuItem;
	private JMenuItem disastersMenuItem;
	private JMenuItem soundsMenuItem;
	private Map<Speed, JMenuItem> priorityMenuItems;
	private Map<Integer, JMenuItem> difficultyMenuItems;
	// used when a tool is being pressed
	private ToolStroke toolStroke;
	// where the tool was last applied during the current drag
	private int lastX;
	private int lastY;
	private Timer simTimer;
	private Timer shakeTimer;
	private EarthquakeStepper currentEarthquake;
	public MainWindow()
	{
		this(new Micropolis());
	}
	private MainWindow(Micropolis engine)
	{
		setIconImage(appIcon.getImage());

		this.engine = engine;

		JPanel mainArea = new JPanel(new BorderLayout());
		add(mainArea, BorderLayout.CENTER);

		drawingArea = new MicropolisDrawingArea(engine);
		drawingAreaScroll = new JScrollPane(drawingArea);
		mainArea.add(drawingAreaScroll);

		makeMenu();
		JToolBar tb = makeToolbar();
		mainArea.add(tb, BorderLayout.LINE_START);

		Box evalGraphsBox = new Box(BoxLayout.PAGE_AXIS);
		mainArea.add(evalGraphsBox, BorderLayout.PAGE_END);

		graphsPane = new GraphsPane(engine);
		graphsPane.setVisible(false);
		evalGraphsBox.add(graphsPane);

		evaluationPane = new EvaluationPane(engine);
		evaluationPane.setVisible(false);
		evalGraphsBox.add(evaluationPane, BorderLayout.PAGE_END);

		JPanel leftPane = new JPanel(new GridBagLayout());
		add(leftPane, BorderLayout.LINE_START);

		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = constraints.gridy = 0;
		constraints.anchor = GridBagConstraints.LAST_LINE_START;
		constraints.insets = new Insets(4, 4, 4, 4);
		constraints.weightx = 1.0;

		demandInd = new DemandIndicator();
		leftPane.add(demandInd, constraints);

		constraints.gridx = 1;
		constraints.weightx = 0.0;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.insets = new Insets(4, 20, 4, 4);

		leftPane.add(makeDateFunds(), constraints);

		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.gridwidth = 2;
		constraints.weighty = 0.0;
		constraints.anchor = GridBagConstraints.PAGE_START;
		constraints.insets = new Insets(0, 0, 0, 0);

		JPanel mapViewContainer = new JPanel(new BorderLayout());
		mapViewContainer.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		leftPane.add(mapViewContainer, constraints);

		JMenuBar mapMenu = new JMenuBar();
		mapViewContainer.add(mapMenu, BorderLayout.PAGE_START);

		JMenu zonesMenu = new JMenu(strings.getString("menu.zones"));
		setupKeys(zonesMenu, "menu.zones");
		mapMenu.add(zonesMenu);

		zonesMenu.add(makeMapStateMenuItem("menu.zones.ALL", MapState.ALL));
		zonesMenu.add(makeMapStateMenuItem("menu.zones.RESIDENTIAL", MapState.RESIDENTIAL));
		zonesMenu.add(makeMapStateMenuItem("menu.zones.COMMERCIAL", MapState.COMMERCIAL));
		zonesMenu.add(makeMapStateMenuItem("menu.zones.INDUSTRIAL", MapState.INDUSTRIAL));
		zonesMenu.add(makeMapStateMenuItem("menu.zones.TRANSPORT", MapState.TRANSPORT));

		JMenu overlaysMenu = new JMenu(strings.getString("menu.overlays"));
		setupKeys(overlaysMenu, "menu.overlays");
		mapMenu.add(overlaysMenu);

		overlaysMenu.add(makeMapStateMenuItem("menu.overlays.POPDEN_OVERLAY", MapState.POPDEN_OVERLAY));
		overlaysMenu.add(makeMapStateMenuItem("menu.overlays.GROWTHRATE_OVERLAY", MapState.GROWTHRATE_OVERLAY));
		overlaysMenu.add(makeMapStateMenuItem("menu.overlays.LANDVALUE_OVERLAY", MapState.LANDVALUE_OVERLAY));
		overlaysMenu.add(makeMapStateMenuItem("menu.overlays.CRIME_OVERLAY", MapState.CRIME_OVERLAY));
		overlaysMenu.add(makeMapStateMenuItem("menu.overlays.POLLUTE_OVERLAY", MapState.POLLUTE_OVERLAY));
		overlaysMenu.add(makeMapStateMenuItem("menu.overlays.TRAFFIC_OVERLAY", MapState.TRAFFIC_OVERLAY));
		overlaysMenu.add(makeMapStateMenuItem("menu.overlays.POWER_OVERLAY", MapState.POWER_OVERLAY));
		overlaysMenu.add(makeMapStateMenuItem("menu.overlays.FIRE_OVERLAY", MapState.FIRE_OVERLAY));
		overlaysMenu.add(makeMapStateMenuItem("menu.overlays.POLICE_OVERLAY", MapState.POLICE_OVERLAY));

		mapMenu.add(Box.createHorizontalGlue());
		mapLegendLbl = new JLabel();
		mapMenu.add(mapLegendLbl);

		mapView = new OverlayMapView(engine);
		mapView.connectView(drawingArea, drawingAreaScroll);
		mapViewContainer.add(mapView, BorderLayout.CENTER);

		setMapState(MapState.ALL);

		constraints.gridx = 0;
		constraints.gridy = 2;
		constraints.gridwidth = 2;
		constraints.weighty = 1.0;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.insets = new Insets(0, 0, 0, 0);

		messagesPane = new MessagesPane();
		JScrollPane scroll2 = new JScrollPane(messagesPane);
		scroll2.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scroll2.setPreferredSize(new Dimension(0, 0));
		scroll2.setMinimumSize(new Dimension(0, 0));
		leftPane.add(scroll2, constraints);

		constraints.gridy = 3;
		constraints.weighty = 0.0;
		notificationPane = new NotificationPane(engine);
		leftPane.add(notificationPane, constraints);

		pack();
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		setLocationRelativeTo(null);

		InputMap inputMap = ((JComponent) getContentPane()).getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		inputMap.put(KeyStroke.getKeyStroke("ADD"), "zoomIn");
		inputMap.put(KeyStroke.getKeyStroke("shift EQUALS"), "zoomIn");
		inputMap.put(KeyStroke.getKeyStroke("SUBTRACT"), "zoomOut");
		inputMap.put(KeyStroke.getKeyStroke("MINUS"), "zoomOut");
		inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "escape");

		ActionMap actionMap = ((JComponent) getContentPane()).getActionMap();
		actionMap.put("zoomIn", new ZoomInAction());
		actionMap.put("zoomOut", new ZoomOutAction());
		actionMap.put("escape", new EscapeAction());

		MouseAdapter mouse = new DrawingAreaMouseAdapter();
		drawingArea.addMouseListener(mouse);
		drawingArea.addMouseMotionListener(mouse);
		drawingArea.addMouseWheelListener(mouse);

		addWindowListener(new MainWindowAdapter());

		Preferences prefs = Preferences.userNodeForPackage(MainWindow.class);
		doSounds = prefs.getBoolean(SOUNDS_PREF, true);

		// start things up
		mapView.setEngine(engine);
		engine.addListener(this);
		engine.addEarthquakeListener(this);
		reloadFunds();
		reloadOptions();
		startTimer();
		makeClean();
	}

	private static void setupKeys(JMenu menu, String prefix)
	{
		if (strings.containsKey(prefix + ".key")) {
			String mnemonic = strings.getString(prefix + ".key");
			menu.setMnemonic(
					KeyStroke.getKeyStroke(mnemonic).getKeyCode()
			);
		}
	}

	private static void setupKeys(JMenuItem menuItem, String prefix)
	{
		if (strings.containsKey(prefix + ".key")) {
			String mnemonic = strings.getString(prefix + ".key");
			menuItem.setMnemonic(
					KeyStroke.getKeyStroke(mnemonic).getKeyCode()
			);
		}
		if (strings.containsKey(prefix + ".shortcut")) {
			String shortcut = strings.getString(prefix + ".shortcut");
			menuItem.setAccelerator(
					KeyStroke.getKeyStroke(shortcut)
			);
		}
	}

	static String formatFunds(int funds)
	{
		return MessageFormat.format(
				strings.getString("funds"), funds
		);
	}

	static String formatGameDate(int cityTime)
	{
		Calendar c = Calendar.getInstance();
		c.set(1900 + cityTime / 48,
				cityTime % 48 / 4,
				cityTime % 4 * 7 + 1
		);

		return MessageFormat.format(
				strings.getString("citytime"),
				c.getTime()
		);
	}

	public void setEngine(Micropolis newEngine)
	{
		if (engine != null) { // old engine
			engine.removeListener(this);
			engine.removeEarthquakeListener(this);
		}

		engine = newEngine;

		if (engine != null) { // new engine
			engine.addListener(this);
			engine.addEarthquakeListener(this);
		}

		boolean timerEnabled = isTimerActive();
		if (timerEnabled) {
			stopTimer();
		}
		stopEarthquake();

		drawingArea.setEngine(engine);
		mapView.setEngine(engine);   //must change mapView after drawingArea
		evaluationPane.setEngine(engine);
		demandInd.setEngine(engine);
		graphsPane.setEngine(engine);
		reloadFunds();
		reloadOptions();
		notificationPane.setVisible(false);

		if (timerEnabled) {
			startTimer();
		}
	}

	private boolean needsSaved()
	{
		if (dirty1)    //player has built something since last save
			return true;

		if (!dirty2)   //no simulator ticks since last save
			return false;

		// simulation time has passed since last save, but the player
		// hasn't done anything. Whether we need to prompt for save
		// will depend on how much real time has elapsed.
		// The threshold is 30 seconds.

		return System.currentTimeMillis() - lastSavedTime > 30000L;
	}

	private boolean maybeSaveCity()
	{
		if (needsSaved()) {
			boolean timerEnabled = isTimerActive();
			if (timerEnabled) {
				stopTimer();
			}

			try {
				int rv = JOptionPane.showConfirmDialog(
						this,
						strings.getString("main.save_query"),
						PRODUCT_NAME,
						JOptionPane.YES_NO_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE);
				if (rv == JOptionPane.CANCEL_OPTION)
					return false;

				if (rv == JOptionPane.YES_OPTION) {
					if (!onSaveCityClicked()) {
						// canceled save dialog
						return false;
					}
				}
			} finally {
				if (timerEnabled) {
					startTimer();
				}
			}
		}
		return true;
	}

	private void closeWindow()
	{
		if (maybeSaveCity()) {
			dispose();
		}
	}

	private JComponent makeDateFunds()
	{
		JPanel pane = new JPanel(new GridBagLayout());
		GridBagConstraints c0 = new GridBagConstraints();
		GridBagConstraints c1 = new GridBagConstraints();

		c0.gridx = 0;
		c1.gridx = 1;
		c0.gridy = c1.gridy = 0;
		c0.weightx = 1.0;
		c0.weighty = c1.weighty = 1.0;
		c0.anchor = GridBagConstraints.LINE_START;
		c1.anchor = GridBagConstraints.LINE_END;

		pane.add(new JLabel(strings.getString("main.date_label")), c0);
		dateLbl = new JLabel();
		pane.add(dateLbl, c1);

		c0.gridy = c1.gridy = 1;

		pane.add(new JLabel(strings.getString("main.funds_label")), c0);
		fundsLbl = new JLabel();
		pane.add(fundsLbl, c1);

		c0.gridy = c1.gridy = 2;

		pane.add(new JLabel(strings.getString("main.population_label")), c0);
		popLbl = new JLabel();
		pane.add(popLbl, c1);

		return pane;
	}

	private void makeMenu()
	{
		JMenuBar menuBar = new JMenuBar();

		JMenu gameMenu = new JMenu(strings.getString("menu.game"));
		setupKeys(gameMenu, "menu.game");
		menuBar.add(gameMenu);

		JMenuItem menuItem;
		menuItem = new JMenuItem(strings.getString("menu.game.new"));
		setupKeys(menuItem, "menu.game.new");
		menuItem.addActionListener(wrapActionListener(
				ev -> onNewCityClicked()));
		gameMenu.add(menuItem);

		menuItem = new JMenuItem(strings.getString("menu.game.load"));
		setupKeys(menuItem, "menu.game.load");
		menuItem.addActionListener(wrapActionListener(
				ev -> onLoadGameClicked()));
		gameMenu.add(menuItem);

		menuItem = new JMenuItem(strings.getString("menu.game.save"));
		setupKeys(menuItem, "menu.game.save");
		menuItem.addActionListener(wrapActionListener(
				ev -> onSaveCityClicked()));
		gameMenu.add(menuItem);

		menuItem = new JMenuItem(strings.getString("menu.game.save_as"));
		setupKeys(menuItem, "menu.game.save_as");
		menuItem.addActionListener(wrapActionListener(
				ev -> onSaveCityAsClicked()));
		gameMenu.add(menuItem);

		menuItem = new JMenuItem(strings.getString("menu.game.exit"));
		setupKeys(menuItem, "menu.game.exit");
		menuItem.addActionListener(wrapActionListener(
				ev -> closeWindow()));
		gameMenu.add(menuItem);

		JMenu optionsMenu = new JMenu(strings.getString("menu.options"));
		setupKeys(optionsMenu, "menu.options");
		menuBar.add(optionsMenu);

		JMenu levelMenu = new JMenu(strings.getString("menu.difficulty"));
		setupKeys(levelMenu, "menu.difficulty");
		optionsMenu.add(levelMenu);

		difficultyMenuItems = new HashMap<>();
		for (int i = GameLevel.MIN_LEVEL; i <= GameLevel.MAX_LEVEL; i++) {
			int level = i;
			menuItem = new JRadioButtonMenuItem(strings.getString("menu.difficulty." + level));
			setupKeys(menuItem, "menu.difficulty." + level);
			menuItem.addActionListener(wrapActionListener(
					evt -> onDifficultyClicked(level)));
			levelMenu.add(menuItem);
			difficultyMenuItems.put(level, menuItem);
		}

		autoBudgetMenuItem = new JCheckBoxMenuItem(strings.getString("menu.options.auto_budget"));
		setupKeys(autoBudgetMenuItem, "menu.options.auto_budget");
		autoBudgetMenuItem.addActionListener(wrapActionListener(
				ev -> onAutoBudgetClicked()));
		optionsMenu.add(autoBudgetMenuItem);

		autoBulldozeMenuItem = new JCheckBoxMenuItem(strings.getString("menu.options.auto_bulldoze"));
		setupKeys(autoBulldozeMenuItem, "menu.options.auto_bulldoze");
		autoBulldozeMenuItem.addActionListener(wrapActionListener(
				ev -> onAutoBulldozeClicked()));
		optionsMenu.add(autoBulldozeMenuItem);

		disastersMenuItem = new JCheckBoxMenuItem(strings.getString("menu.options.disasters"));
		setupKeys(disastersMenuItem, "menu.options.disasters");
		disastersMenuItem.addActionListener(wrapActionListener(
				ev -> onDisastersClicked()));
		optionsMenu.add(disastersMenuItem);

		soundsMenuItem = new JCheckBoxMenuItem(strings.getString("menu.options.sound"));
		setupKeys(soundsMenuItem, "menu.options.sound");
		soundsMenuItem.addActionListener(wrapActionListener(
				ev -> onSoundClicked()));
		optionsMenu.add(soundsMenuItem);

		menuItem = new JMenuItem(strings.getString("menu.options.zoom_in"));
		setupKeys(menuItem, "menu.options.zoom_in");
		menuItem.addActionListener(wrapActionListener(
				ev -> doZoom(1)));
		optionsMenu.add(menuItem);

		menuItem = new JMenuItem(strings.getString("menu.options.zoom_out"));
		setupKeys(menuItem, "menu.options.zoom_out");
		menuItem.addActionListener(wrapActionListener(
				ev -> doZoom(-1)));
		optionsMenu.add(menuItem);

		JMenu disastersMenu = new JMenu(strings.getString("menu.disasters"));
		setupKeys(disastersMenu, "menu.disasters");
		menuBar.add(disastersMenu);

		menuItem = new JMenuItem(strings.getString("menu.disasters.MONSTER"));
		setupKeys(menuItem, "menu.disasters.MONSTER");
		menuItem.addActionListener(wrapActionListener(
				ev -> onInvokeDisasterClicked(Disaster.MONSTER)));
		disastersMenu.add(menuItem);

		menuItem = new JMenuItem(strings.getString("menu.disasters.FIRE"));
		setupKeys(menuItem, "menu.disasters.FIRE");
		menuItem.addActionListener(wrapActionListener(
				ev -> onInvokeDisasterClicked(Disaster.FIRE)));
		disastersMenu.add(menuItem);

		menuItem = new JMenuItem(strings.getString("menu.disasters.FLOOD"));
		setupKeys(menuItem, "menu.disasters.FLOOD");
		menuItem.addActionListener(wrapActionListener(
				ev -> onInvokeDisasterClicked(Disaster.FLOOD)));
		disastersMenu.add(menuItem);

		menuItem = new JMenuItem(strings.getString("menu.disasters.MELTDOWN"));
		setupKeys(menuItem, "menu.disasters.MELTDOWN");
		menuItem.addActionListener(wrapActionListener(
				ev -> onInvokeDisasterClicked(Disaster.MELTDOWN)));
		disastersMenu.add(menuItem);

		menuItem = new JMenuItem(strings.getString("menu.disasters.TORNADO"));
		setupKeys(menuItem, "menu.disasters.TORNADO");
		menuItem.addActionListener(wrapActionListener(
				ev -> onInvokeDisasterClicked(Disaster.TORNADO)));
		disastersMenu.add(menuItem);

		menuItem = new JMenuItem(strings.getString("menu.disasters.EARTHQUAKE"));
		setupKeys(menuItem, "menu.disasters.EARTHQUAKE");
		menuItem.addActionListener(wrapActionListener(
				ev -> onInvokeDisasterClicked(Disaster.EARTHQUAKE)));
		disastersMenu.add(menuItem);

		JMenu priorityMenu = new JMenu(strings.getString("menu.speed"));
		setupKeys(priorityMenu, "menu.speed");
		menuBar.add(priorityMenu);

		priorityMenuItems = new EnumMap<>(Speed.class);
		menuItem = new JRadioButtonMenuItem(strings.getString("menu.speed.SUPER_FAST"));
		setupKeys(menuItem, "menu.speed.SUPER_FAST");
		menuItem.addActionListener(wrapActionListener(
				ev -> onPriorityClicked(Speed.SUPER_FAST)));
		priorityMenu.add(menuItem);
		priorityMenuItems.put(Speed.SUPER_FAST, menuItem);

		menuItem = new JRadioButtonMenuItem(strings.getString("menu.speed.FAST"));
		setupKeys(menuItem, "menu.speed.FAST");
		menuItem.addActionListener(wrapActionListener(
				ev -> onPriorityClicked(Speed.FAST)));
		priorityMenu.add(menuItem);
		priorityMenuItems.put(Speed.FAST, menuItem);

		menuItem = new JRadioButtonMenuItem(strings.getString("menu.speed.NORMAL"));
		setupKeys(menuItem, "menu.speed.NORMAL");
		menuItem.addActionListener(wrapActionListener(
				ev -> onPriorityClicked(Speed.NORMAL)));
		priorityMenu.add(menuItem);
		priorityMenuItems.put(Speed.NORMAL, menuItem);

		menuItem = new JRadioButtonMenuItem(strings.getString("menu.speed.SLOW"));
		setupKeys(menuItem, "menu.speed.SLOW");
		menuItem.addActionListener(wrapActionListener(
				ev -> onPriorityClicked(Speed.SLOW)));
		priorityMenu.add(menuItem);
		priorityMenuItems.put(Speed.SLOW, menuItem);

		menuItem = new JRadioButtonMenuItem(strings.getString("menu.speed.PAUSED"));
		setupKeys(menuItem, "menu.speed.PAUSED");
		menuItem.addActionListener(wrapActionListener(
				ev -> onPriorityClicked(Speed.PAUSED)));
		priorityMenu.add(menuItem);
		priorityMenuItems.put(Speed.PAUSED, menuItem);

		JMenu windowsMenu = new JMenu(strings.getString("menu.windows"));
		setupKeys(windowsMenu, "menu.windows");
		menuBar.add(windowsMenu);

		menuItem = new JMenuItem(strings.getString("menu.windows.budget"));
		setupKeys(menuItem, "menu.windows.budget");
		menuItem.addActionListener(wrapActionListener(
				ev -> onViewBudgetClicked()));
		windowsMenu.add(menuItem);

		menuItem = new JMenuItem(strings.getString("menu.windows.evaluation"));
		setupKeys(menuItem, "menu.windows.evaluation");
		menuItem.addActionListener(wrapActionListener(
				ev -> onViewEvaluationClicked()));
		windowsMenu.add(menuItem);

		menuItem = new JMenuItem(strings.getString("menu.windows.graph"));
		setupKeys(menuItem, "menu.windows.graph");
		menuItem.addActionListener(wrapActionListener(
				ev -> onViewGraphClicked()));
		windowsMenu.add(menuItem);

		JMenu helpMenu = new JMenu(strings.getString("menu.help"));
		setupKeys(helpMenu, "menu.help");
		menuBar.add(helpMenu);

		menuItem = new JMenuItem(strings.getString("menu.help.about"));
		setupKeys(menuItem, "menu.help.about");
		menuItem.addActionListener(wrapActionListener(
				ev -> onAboutClicked()));
		helpMenu.add(menuItem);

		setJMenuBar(menuBar);
	}

	private void onAutoBudgetClicked()
	{
		dirty1 = true;
		engine.toggleAutoBudget();
	}

	private void onAutoBulldozeClicked()
	{
		dirty1 = true;
		engine.toggleAutoBulldoze();
	}

	private void onDisastersClicked()
	{
		dirty1 = true;
		engine.toggleDisasters();
	}

	private void onSoundClicked()
	{
		doSounds = !doSounds;
		Preferences prefs = Preferences.userNodeForPackage(MainWindow.class);
		prefs.putBoolean(SOUNDS_PREF, doSounds);
		reloadOptions();
	}

	void makeClean()
	{
		dirty1 = false;
		dirty2 = false;
		lastSavedTime = System.currentTimeMillis();
		if (currentFile != null) {
			String fileName = currentFile.getName();
			if (fileName.endsWith("." + EXTENSION)) {
				fileName = fileName.substring(0, fileName.length() - 1 - EXTENSION.length());
			}
			setTitle(MessageFormat.format(strings.getString("main.caption_named_city"), fileName));
		} else {
			setTitle(strings.getString("main.caption_unnamed_city"));
		}
	}

	private boolean onSaveCityClicked()
	{
		if (currentFile == null) {
			return onSaveCityAsClicked();
		}

		try {
			engine.save(currentFile);
			makeClean();
			return true;
		} catch (IOException e) {
			e.printStackTrace(System.err);
			JOptionPane.showMessageDialog(this, e, strings.getString("main.error_caption"),
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	private boolean onSaveCityAsClicked()
	{
		boolean timerEnabled = isTimerActive();
		if (timerEnabled) {
			stopTimer();
		}
		try {
			JFileChooser fc = new JFileChooser();
			FileNameExtensionFilter filter1 = new FileNameExtensionFilter(strings.getString("cty_file"), EXTENSION);
			fc.setFileFilter(filter1);
			int rv = fc.showSaveDialog(this);
			if (rv == JFileChooser.APPROVE_OPTION) {
				currentFile = fc.getSelectedFile();
				if (!currentFile.getName().endsWith("." + EXTENSION)) {
					currentFile = new File(currentFile.getPath() + "." + EXTENSION);
				}
				engine.save(currentFile);
				makeClean();
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
			JOptionPane.showMessageDialog(this, e, strings.getString("main.error_caption"),
					JOptionPane.ERROR_MESSAGE);
		} finally {
			if (timerEnabled) {
				startTimer();
			}
		}
		return false;
	}

	private void onLoadGameClicked()
	{
		// check if user wants to save their current city
		if (!maybeSaveCity()) {
			return;
		}

		boolean timerEnabled = isTimerActive();
		if (timerEnabled) {
			stopTimer();
		}

		try {
			JFileChooser fc = new JFileChooser();
			FileNameExtensionFilter filter1 = new FileNameExtensionFilter(strings.getString("cty_file"), EXTENSION);
			fc.setFileFilter(filter1);

			assert !isTimerActive();

			int rv = fc.showOpenDialog(this);
			if (rv == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile();
				Micropolis newEngine = new Micropolis();
				newEngine.load(file);
				setEngine(newEngine);
				currentFile = file;
				makeClean();
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
			JOptionPane.showMessageDialog(this, e, strings.getString("main.error_caption"),
					JOptionPane.ERROR_MESSAGE);
		} finally {
			if (timerEnabled) {
				startTimer();
			}
		}
	}

	private JToggleButton makeToolBtn(MicropolisTool tool)
	{
		String iconName = strings.containsKey("tool." + tool.name() + ".icon") ?
				strings.getString("tool." + tool.name() + ".icon") :
				"/graphics/tools/" + tool.name().toLowerCase(Locale.ENGLISH) + ".png";
		String iconSelectedName = strings.containsKey("tool." + tool.name() + ".selected_icon") ?
				strings.getString("tool." + tool.name() + ".selected_icon") :
				iconName;
		String tipText = strings.containsKey("tool." + tool.name() + ".tip") ?
				strings.getString("tool." + tool.name() + ".tip") :
				tool.name();

		JToggleButton btn = new JToggleButton();
		btn.setIcon(new ImageIcon(MainWindow.class.getResource(iconName)));
		btn.setSelectedIcon(new ImageIcon(MainWindow.class.getResource(iconSelectedName)));
		btn.setToolTipText(tipText);
		btn.setMargin(new Insets(0, 0, 0, 0));
		btn.setBorderPainted(false);
		btn.addActionListener(ev -> selectTool(tool));
		toolButtons.put(tool, btn);
		return btn;
	}

	private JToolBar makeToolbar()
	{
		toolButtons = new EnumMap<>(MicropolisTool.class);

		JToolBar toolBar = new JToolBar(strings.getString("main.tools_caption"), SwingConstants.VERTICAL);
		toolBar.setFloatable(false);
		toolBar.setRollover(false);

		JPanel gridBox = new JPanel(new GridBagLayout());
		toolBar.add(gridBox);

		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = constraints.gridy = 0;
		constraints.anchor = GridBagConstraints.PAGE_START;
		constraints.insets = new Insets(8, 0, 0, 0);
		currentToolLbl = new JLabel(" ");
		gridBox.add(currentToolLbl, constraints);

		constraints.gridy = 1;
		constraints.insets = new Insets(0, 0, 12, 0);
		currentToolCostLbl = new JLabel(" ");
		gridBox.add(currentToolCostLbl, constraints);

		constraints.gridy++;
		constraints.fill = GridBagConstraints.NONE;
		constraints.weightx = 1.0;
		constraints.insets = new Insets(0, 0, 0, 0);
		Box b0 = new Box(BoxLayout.LINE_AXIS);
		gridBox.add(b0, constraints);

		b0.add(makeToolBtn(MicropolisTool.BULLDOZER));
		b0.add(makeToolBtn(MicropolisTool.WIRE));
		b0.add(makeToolBtn(MicropolisTool.PARK));

		constraints.gridy++;
		Box b1 = new Box(BoxLayout.LINE_AXIS);
		gridBox.add(b1, constraints);

		b1.add(makeToolBtn(MicropolisTool.ROADS));
		b1.add(makeToolBtn(MicropolisTool.RAIL));

		constraints.gridy++;
		Box b2 = new Box(BoxLayout.LINE_AXIS);
		gridBox.add(b2, constraints);

		b2.add(makeToolBtn(MicropolisTool.RESIDENTIAL));
		b2.add(makeToolBtn(MicropolisTool.COMMERCIAL));
		b2.add(makeToolBtn(MicropolisTool.INDUSTRIAL));

		constraints.gridy++;
		Box b3 = new Box(BoxLayout.LINE_AXIS);
		gridBox.add(b3, constraints);

		b3.add(makeToolBtn(MicropolisTool.FIRE));
		b3.add(makeToolBtn(MicropolisTool.QUERY));
		b3.add(makeToolBtn(MicropolisTool.POLICE));

		constraints.gridy++;
		Box b4 = new Box(BoxLayout.LINE_AXIS);
		gridBox.add(b4, constraints);

		b4.add(makeToolBtn(MicropolisTool.POWERPLANT));
		b4.add(makeToolBtn(MicropolisTool.NUCLEAR));

		constraints.gridy++;
		Box b5 = new Box(BoxLayout.LINE_AXIS);
		gridBox.add(b5, constraints);

		b5.add(makeToolBtn(MicropolisTool.STADIUM));
		b5.add(makeToolBtn(MicropolisTool.SEAPORT));

		constraints.gridy++;
		Box b6 = new Box(BoxLayout.LINE_AXIS);
		gridBox.add(b6, constraints);

		b6.add(makeToolBtn(MicropolisTool.AIRPORT));

		// add glue to make all elements align toward top
		constraints.gridy++;
		constraints.weighty = 1.0;
		gridBox.add(new JLabel(), constraints);

		return toolBar;
	}

	private void selectTool(MicropolisTool newTool)
	{
		toolButtons.get(newTool).setSelected(true);
		if (newTool == currentTool) {
			return;
		}

		if (currentTool != null) {
			toolButtons.get(currentTool).setSelected(false);
		}

		currentTool = newTool;

		currentToolLbl.setText(
				strings.containsKey("tool." + currentTool.name() + ".name") ?
						strings.getString("tool." + currentTool.name() + ".name") :
						currentTool.name()
		);

		int cost = currentTool.getCost();
		currentToolCostLbl.setText(cost != 0 ? formatFunds(cost) : " ");
	}

	private void onNewCityClicked()
	{
		if (maybeSaveCity()) {
			doNewCity(false);
		}
	}

	public void doNewCity(boolean firstTime)
	{
		boolean timerEnabled = isTimerActive();
		if (timerEnabled) {
			stopTimer();
		}

		new NewCityDialog(this, !firstTime).setVisible(true);

		if (timerEnabled) {
			startTimer();
		}
	}


	private void doZoom(int dir, Point mousePt)
	{
		int oldZoom = drawingArea.getTileWidth();
		int newZoom = dir < 0 ? oldZoom / 2 : oldZoom * 2;
		if (newZoom < 8) {
			newZoom = 8;
		}
		if (newZoom > 32) {
			newZoom = 32;
		}

		if (oldZoom != newZoom) {
			// preserve effective mouse position in viewport when changing zoom level
			double zoomFactor = (double) newZoom / oldZoom;
			Point pos = drawingAreaScroll.getViewport().getViewPosition();
			int newX = (int) Math.round(mousePt.x * zoomFactor - (mousePt.x - pos.x));
			int newY = (int) Math.round(mousePt.y * zoomFactor - (mousePt.y - pos.y));
			drawingArea.selectTileSize(newZoom);
			drawingAreaScroll.validate();
			drawingAreaScroll.getViewport().setViewPosition(new Point(newX, newY));
		}
	}

	private void doZoom(int dir)
	{
		Rectangle rect = drawingAreaScroll.getViewport().getViewRect();
		doZoom(dir,
				new Point(rect.x + rect.width / 2,
						rect.y + rect.height / 2
				)
		);
	}

	private void updateDateLabel()
	{
		dateLbl.setText(formatGameDate(engine.getCityTime()));

		NumberFormat nf = NumberFormat.getInstance();
		popLbl.setText(nf.format(engine.getCityPopulation()));
	}

	private void startTimer()
	{
		Micropolis engine = this.engine;
		int count = engine.getSimSpeed().simStepsPerUpdate;

		assert !isTimerActive();

		if (engine.getSimSpeed() == Speed.PAUSED)
			return;

		if (currentEarthquake != null) {
			int interval = 3000 / MicropolisDrawingArea.SHAKE_STEPS;
			shakeTimer = new Timer(interval, evt -> {
				currentEarthquake.oneStep();
				if (currentEarthquake.count == 0) {
					stopTimer();
					currentEarthquake = null;
					startTimer();
				}
			});
			shakeTimer.start();
			return;
		}

		ActionListener taskPerformer = evt -> {
			for (int i = 0; i < count; i++) {
				engine.animate();
				if (!engine.isAutoBudget() && engine.isBudgetTime()) {
					showAutoBudget();
					return;
				}
			}
			updateDateLabel();
			dirty2 = true;
		};
		taskPerformer = wrapActionListener(taskPerformer);

		simTimer = new Timer(engine.getSimSpeed().animationDelay, taskPerformer);
		simTimer.start();
	}

	private ActionListener wrapActionListener(ActionListener actionListener)
	{
		return evt -> {
			try {
				actionListener.actionPerformed(evt);
			} catch (Throwable e) {
				showErrorMessage(e);
			}
		};
	}

	private void showErrorMessage(Throwable e)
	{
		StringWriter w = new StringWriter();
		e.printStackTrace(new PrintWriter(w));

		JTextPane stackTracePane = new JTextPane();
		stackTracePane.setEditable(false);
		stackTracePane.setText(w.toString());

		JScrollPane detailsPane = new JScrollPane(stackTracePane);
		detailsPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		detailsPane.setPreferredSize(new Dimension(480, 240));
		detailsPane.setMinimumSize(new Dimension(0, 0));

		int rv = JOptionPane.showOptionDialog(this, e,
				strings.getString("main.error_unexpected"),
				JOptionPane.DEFAULT_OPTION,
				JOptionPane.ERROR_MESSAGE,
				null,
				new String[]{
						strings.getString("main.error_show_stacktrace"),
						strings.getString("main.error_close"),
						strings.getString("main.error_shutdown")
				},
				1
		);
		if (rv == 0) {
			JOptionPane.showMessageDialog(this, detailsPane,
					strings.getString("main.error_unexpected"),
					JOptionPane.ERROR_MESSAGE);
		}
		if (rv == 2) {
			rv = JOptionPane.showConfirmDialog(
					this,
					strings.getString("error.shutdown_query"),
					strings.getString("main.error_unexpected"),
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.WARNING_MESSAGE);
			if (rv == JOptionPane.OK_OPTION) {
				System.exit(1);
			}
		}
	}

	@Override
	public void earthquakeStarted()
	{
		if (isTimerActive()) {
			stopTimer();
		}

		currentEarthquake = new EarthquakeStepper();
		currentEarthquake.oneStep();
		startTimer();
	}

	private void stopEarthquake()
	{
		drawingArea.shake(0);
		currentEarthquake = null;
	}

	private void stopTimer()
	{
		assert isTimerActive();

		if (simTimer != null) {
			simTimer.stop();
			simTimer = null;
		}
		if (shakeTimer != null) {
			shakeTimer.stop();
			shakeTimer = null;
		}
	}

	private boolean isTimerActive()
	{
		return simTimer != null || shakeTimer != null;
	}



	private void onDifficultyClicked(int newDifficulty)
	{
		engine.setGameLevel(newDifficulty);
	}

	private void onPriorityClicked(Speed newSpeed)
	{
		if (isTimerActive()) {
			stopTimer();
		}

		engine.setSpeed(newSpeed);
		startTimer();
	}

	private void onInvokeDisasterClicked(Disaster disaster)
	{
		dirty1 = true;
		switch (disaster) {
			case FIRE:
				engine.makeFire();
				break;
			case FLOOD:
				engine.makeFlood();
				break;
			case MONSTER:
				engine.makeMonster();
				break;
			case MELTDOWN:
				if (!engine.makeMeltdown()) {
					messagesPane.appendCityMessage(MicropolisMessage.NO_NUCLEAR_PLANTS);
				}
				break;
			case TORNADO:
				engine.makeTornado();
				break;
			case EARTHQUAKE:
				engine.makeEarthquake();
				break;
			default:
				assert false; //unknown disaster
		}
	}

	private void reloadFunds()
	{
		fundsLbl.setText(formatFunds(engine.getBudget().getTotalFunds()));
	}

	@Override
	public void cityMessage(MicropolisMessage message, CityLocation loc)
	{
		messagesPane.appendCityMessage(message);

		if (message.isUseNotificationPane() && loc != null) {
			notificationPane.showMessage(engine, message, loc.getX(), loc.getY());
		}
	}

	@Override
	public void fundsChanged()
	{
		reloadFunds();
	}

	@Override
	public void optionsChanged()
	{
		reloadOptions();
	}

	private void reloadOptions()
	{
		autoBudgetMenuItem.setSelected(engine.isAutoBudget());
		autoBulldozeMenuItem.setSelected(engine.isAutoBulldoze());
		disastersMenuItem.setSelected(!engine.isNoDisasters());
		soundsMenuItem.setSelected(doSounds);
		for (Map.Entry<Speed, JMenuItem> entry : priorityMenuItems.entrySet()) {
			entry.getValue().setSelected(engine.getSimSpeed() == entry.getKey());
		}
		for (int i = GameLevel.MIN_LEVEL; i <= GameLevel.MAX_LEVEL; i++) {
			difficultyMenuItems.get(i).setSelected(engine.getGameLevel() == i);
		}
	}

	@Override
	public void citySound(Sound sound, CityLocation loc)
	{
		if (!doSounds)
			return;

		URL audioFile = sound.getAudioFile();
		if (audioFile == null)
			return;

		boolean offScreen = !drawingAreaScroll.getViewport().getViewRect().contains(
				drawingArea.getTileBounds(loc.getX(), loc.getY())
		);
		if (sound == Sound.HONKHONK_LOW && offScreen)
			return;

		try {
			Clip clip = AudioSystem.getClip();
			clip.open(AudioSystem.getAudioInputStream(audioFile));
			clip.start();
			clip.addLineListener(event -> {
				if (event.getType().equals(LineEvent.Type.STOP)){
					event.getLine().close();
				}
			});
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	private void onViewBudgetClicked()
	{
		dirty1 = true;
		showBudgetWindow();
	}

	private void onViewEvaluationClicked()
	{
		evaluationPane.setVisible(true);
	}

	private void onViewGraphClicked()
	{
		graphsPane.setVisible(true);
	}

	private void showAutoBudget()
	{
		if (toolStroke == null) {
			showBudgetWindow();
		} else {
			autoBudgetPending = true;
		}
	}

	private void showBudgetWindow()
	{
		boolean timerEnabled = isTimerActive();
		if (timerEnabled) {
			stopTimer();
		}

		BudgetDialog dlg = new BudgetDialog(this, engine);
		dlg.setModal(true);
		dlg.setVisible(true);

		if (timerEnabled) {
			startTimer();
		}
	}

	private JMenuItem makeMapStateMenuItem(String stringPrefix, MapState state)
	{
		String caption = strings.getString(stringPrefix);
		JMenuItem menuItem = new JRadioButtonMenuItem(caption);
		setupKeys(menuItem, stringPrefix);
		menuItem.addActionListener(evt -> setMapState(state));
		mapStateMenuItems.put(state, menuItem);
		return menuItem;
	}

	private void setMapState(MapState state)
	{
		mapStateMenuItems.get(mapView.getMapState()).setSelected(false);
		mapStateMenuItems.get(state).setSelected(true);
		mapView.setMapState(state);
		setMapLegend(state);
	}

	private void setMapLegend(MapState state)
	{
		String key = "legend_image." + state.name();
		java.net.URL iconUrl = null;
		if (strings.containsKey(key)) {
			String iconName = strings.getString(key);
			iconUrl = MainWindow.class.getResource(iconName);
		}
		if (iconUrl != null) {
			mapLegendLbl.setIcon(new ImageIcon(iconUrl));
		} else {
			mapLegendLbl.setIcon(null);
		}
	}

	private void onAboutClicked()
	{
		String version = getClass().getPackage().getImplementationVersion();
		String versionStr = MessageFormat.format(strings.getString("main.version_string"), version);
		versionStr = versionStr.replace("%java.version%", System.getProperty("java.version"));
		versionStr = versionStr.replace("%java.vendor%", System.getProperty("java.vendor"));

		JLabel appNameLbl = new JLabel(versionStr);
		JLabel appDetailsLbl = new JLabel(strings.getString("main.about_text"));
		JComponent[] inputs = {appNameLbl, appDetailsLbl};
		JOptionPane.showMessageDialog(this,
				inputs,
				strings.getString("main.about_caption"),
				JOptionPane.PLAIN_MESSAGE,
				appIcon);
	}

	public void setCurrentFile(File currentFile)
	{
		this.currentFile = currentFile;
	}

	private class EarthquakeStepper
	{
		private int count;

		private void oneStep()
		{
			count = (count + 1) % MicropolisDrawingArea.SHAKE_STEPS;
			drawingArea.shake(count);
		}

	}

	private class ZoomInAction extends AbstractAction
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			doZoom(1);
		}
	}

	private class ZoomOutAction extends AbstractAction
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			doZoom(-1);
		}
	}

	private class EscapeAction extends AbstractAction
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			onEscapePressed();
		}

		private void onEscapePressed()
		{
			// if currently dragging a tool...
			if (toolStroke != null) {
				// cancel the current mouse operation
				toolStroke = null;
				drawingArea.setToolPreview(null);
				drawingArea.setToolCursor(null);
			} else {
				// dismiss any alerts currently visible
				notificationPane.setVisible(false);
			}
		}

	}

	private class DrawingAreaMouseAdapter extends MouseAdapter
	{
		@Override
		public void mousePressed(MouseEvent ev)
		{
			try {
				onToolDown(ev);
			} catch (Throwable e) {
				showErrorMessage(e);
			}
		}

		private void doQueryTool(int xpos, int ypos)
		{
			if (!engine.testBounds(xpos, ypos))
				return;

			ZoneStatus z = engine.queryZoneStatus(xpos, ypos);
			notificationPane.showZoneStatus(engine, xpos, ypos, z);
		}

		private void previewTool()
		{
			assert toolStroke != null;
			assert currentTool != null;

			drawingArea.setToolCursor(
					toolStroke.getBounds(),
					currentTool
			);
			drawingArea.setToolPreview(
					toolStroke.getPreview()
			);
		}

		private void onToolDown(MouseEvent ev)
		{
			if (ev.getButton() == MouseEvent.BUTTON3) {
				CityLocation loc = drawingArea.getCityLocation(ev.getX(), ev.getY());
				doQueryTool(loc.getX(), loc.getY());
				return;
			}

			if (ev.getButton() != MouseEvent.BUTTON1)
				return;

			if (currentTool == null)
				return;

			CityLocation loc = drawingArea.getCityLocation(ev.getX(), ev.getY());
			int x = loc.getX();
			int y = loc.getY();

			if (currentTool == MicropolisTool.QUERY) {
				doQueryTool(x, y);
				toolStroke = null;
			} else {
				toolStroke = currentTool.beginStroke(engine, x, y);
				previewTool();
			}

			lastX = x;
			lastY = y;
		}


		private void onToolDrag(MouseEvent ev)
		{
			if (currentTool == null)
				return;
			if ((ev.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == 0)
				return;

			CityLocation loc = drawingArea.getCityLocation(ev.getX(), ev.getY());
			int x = loc.getX();
			int y = loc.getY();
			if (x == lastX && y == lastY)
				return;

			if (toolStroke != null) {
				toolStroke.dragTo(x, y);
				previewTool();
			} else if (currentTool == MicropolisTool.QUERY) {
				doQueryTool(x, y);
			}

			lastX = x;
			lastY = y;
		}

		private void onToolHover(MouseEvent ev)
		{
			if (currentTool == null || currentTool == MicropolisTool.QUERY) {
				drawingArea.setToolCursor(null);
				return;
			}

			CityLocation loc = drawingArea.getCityLocation(ev.getX(), ev.getY());
			int x = loc.getX();
			int y = loc.getY();
			int w = currentTool.getSize();
			int h = currentTool.getSize();

			if (w >= 3)
				x--;
			if (h >= 3)
				y--;

			drawingArea.setToolCursor(new CityRect(x, y, w, h), currentTool);
		}

		private void onToolExited()
		{
			drawingArea.setToolCursor(null);
		}

		private void showToolResult(CityLocation loc, ToolResult result)
		{
			switch (result) {
				case SUCCESS:
					citySound(currentTool == MicropolisTool.BULLDOZER ? Sound.BULLDOZE : Sound.BUILD, loc);
					dirty1 = true;
					break;

				case NONE:
					break;
				case UH_OH:
					messagesPane.appendCityMessage(MicropolisMessage.BULLDOZE_FIRST);
					citySound(Sound.UHUH, loc);
					break;

				case INSUFFICIENT_FUNDS:
					messagesPane.appendCityMessage(MicropolisMessage.INSUFFICIENT_FUNDS);
					citySound(Sound.SORRY, loc);
					break;

				default:
					assert false;
			}
		}

		@Override
		public void mouseReleased(MouseEvent ev)
		{
			try {
				onToolUp(ev);
			} catch (Throwable e) {
				showErrorMessage(e);
			}
		}

		private void onToolUp(MouseEvent ev)
		{
			if (toolStroke != null) {
				drawingArea.setToolPreview(null);

				CityLocation loc = toolStroke.getLocation();
				ToolResult tr = toolStroke.apply();
				showToolResult(loc, tr);
				toolStroke = null;
			}

			onToolHover(ev);

			if (autoBudgetPending) {
				autoBudgetPending = false;
				showBudgetWindow();
			}
		}

		@Override
		public void mouseDragged(MouseEvent ev)
		{
			try {
				onToolDrag(ev);
			} catch (Throwable e) {
				showErrorMessage(e);
			}
		}

		@Override
		public void mouseMoved(MouseEvent ev)
		{
			try {
				onToolHover(ev);
			} catch (Throwable e) {
				showErrorMessage(e);
			}
		}

		@Override
		public void mouseExited(MouseEvent ev)
		{
			try {
				onToolExited();
			} catch (Throwable e) {
				showErrorMessage(e);
			}
		}

		@Override
		public void mouseWheelMoved(MouseWheelEvent evt)
		{
			try {
				onMouseWheelMoved(evt);
			} catch (Throwable e) {
				showErrorMessage(e);
			}
		}

		private void onMouseWheelMoved(MouseWheelEvent evt)
		{
			if (evt.getWheelRotation() < 0) {
				doZoom(1, evt.getPoint());
			} else {
				doZoom(-1, evt.getPoint());
			}
		}

	}

	private class MainWindowAdapter extends WindowAdapter
	{
		@Override
		public void windowClosing(WindowEvent e)
		{
			closeWindow();
		}

		@Override
		public void windowClosed(WindowEvent e)
		{
			onWindowClosed();
		}

		private void onWindowClosed()
		{
			if (isTimerActive()) {
				stopTimer();
			}
		}
	}
}
