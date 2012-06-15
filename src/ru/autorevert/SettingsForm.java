package ru.autorevert;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * User: dima
 * Date: 14/06/2012
 */
public class SettingsForm {
	public JPanel root;
	private JComboBox minutesTillRevertComboBox;
	private JCheckBox TODOCheckBox1;
	private JCheckBox TODOCheckBox3;
	private JCheckBox TODOCheckBox2;

	private final Settings initialState;
	private Settings currentState;
	private boolean isUpdatingUI;

	public SettingsForm(Settings initialState) {
		this.initialState = initialState;
		this.currentState = new Settings();
		currentState.loadState(initialState);
		updateUIFromState();

		minutesTillRevertComboBox.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				updateStateFromUI();
				updateUIFromState();
			}
		});
	}

	public void updateUIFromState() {
		if (isUpdatingUI) return;
		isUpdatingUI = true;

		minutesTillRevertComboBox.setSelectedItem(String.valueOf(currentState.minutesTillRevert));

		isUpdatingUI = false;
	}

	private void updateStateFromUI() {
		try {
			Integer value = Integer.valueOf((String) minutesTillRevertComboBox.getSelectedItem());
			if (value >= Settings.MIN_MINUTES_TO_REVERT && value <= Settings.MAX_MINUTES_TO_REVERT) {
				currentState.minutesTillRevert = value;
			}
		} catch (NumberFormatException ignored) {
		}
	}

	public boolean isModified() {
		return !currentState.equals(initialState);
	}

	public Settings applyChanges() {
		initialState.loadState(currentState);
		return initialState;
	}

	public void resetChanges() {
		currentState.loadState(initialState);
	}
}
