package io.github.pyvesb.notepad4e.views;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchPartSite;

import io.github.pyvesb.notepad4e.Notepad4e;
import io.github.pyvesb.notepad4e.preferences.PreferenceConstants;

/**
 * Class representing a note tab in the plugin's view.
 * 
 * @author Pyves
 *
 */
public class NoteTab extends StyledText {

	// Used to parse strings.
	private static final String STRING_SEPARATOR = ",";

	// User defined preferences.
	private IEclipsePreferences preferences;

	// Appearance parameters of the note tab.
	private Color fontColor;
	private Color backgroundColor;
	private Font font;

	/**
	 * Constructor. Sets properties of the editor window.
	 * 
	 * @param parent
	 * @param text
	 */
	public NoteTab(Composite parent, String text) {
		// Enable multiple lines and scroll bars.
		super(parent, SWT.V_SCROLL | SWT.H_SCROLL);

		preferences = InstanceScope.INSTANCE.getNode(Notepad4e.PLUGIN_ID);

		// Scroll bars only appear when the text extends beyond the note window.
		setAlwaysShowScrollBars(false);

		setPreferences();

		setText(text);
	}

	/**
	 * Disposes the resources owned by the note tab.
	 */
	@Override
	public void dispose() {
		super.dispose();
		fontColor.dispose();
		backgroundColor.dispose();
		if (font != null)
			font.dispose();
	}

	/**
	 * Sets properties that can be defined by the user in the plugin's preferences page.
	 */
	public void setPreferences() {
		// Line spacing parameter.
		setLineSpacing(preferences.getInt(PreferenceConstants.PREF_LINE_SPACING,
				PreferenceConstants.PREF_LINE_SPACING_DEFAULT));

		// Line wrap parameter.
		if (preferences.getBoolean(PreferenceConstants.PREF_WRAP, PreferenceConstants.PREF_WRAP_DEFAULT) == true)
			setWordWrap(true);
		else
			setWordWrap(false);

		// Text justify parameter.
		if (preferences.getBoolean(PreferenceConstants.PREF_JUSTIFY, PreferenceConstants.PREF_JUSTIFY_DEFAULT) == true)
			setJustify(true);
		else
			setJustify(false);

		// Alignment parameter (left or right).
		if (preferences.get(PreferenceConstants.PREF_ALIGNMENT, PreferenceConstants.PREF_ALIGNMENT_DEFAULT)
				.equals("right")) {
			setAlignment(SWT.RIGHT);
			// Word wrapping must be enabled for right alignment to be effective.
			setWordWrap(true);
		} else {
			setAlignment(SWT.LEFT);
		}

		// Font color parameter.
		String fontColorString = preferences.get(PreferenceConstants.PREF_FONT_COLOR,
				PreferenceConstants.PREF_FONT_COLOR_DEFAULT);
		String[] fontColorRGBStrings = fontColorString.split(STRING_SEPARATOR);
		// The strings in the above array correspond to the red, green and blue colors.
		fontColor = new Color(Display.getCurrent(), Integer.parseInt(fontColorRGBStrings[0]),
				Integer.parseInt(fontColorRGBStrings[1]), Integer.parseInt(fontColorRGBStrings[2]));
		setForeground(fontColor);

		// Background color parameter.
		String backgroundColorString = preferences.get(PreferenceConstants.PREF_BACKGROUND_COLOR,
				PreferenceConstants.PREF_BACKGROUND_COLOR_DEFAULT);
		String[] backgroundColorRGBStrings = backgroundColorString.split(STRING_SEPARATOR);
		// The strings in the above array correspond to the red, green and blue colors.
		backgroundColor = new Color(Display.getCurrent(), Integer.parseInt(backgroundColorRGBStrings[0]),
				Integer.parseInt(backgroundColorRGBStrings[1]), Integer.parseInt(backgroundColorRGBStrings[2]));
		setBackground(backgroundColor);

		// Font parameter; a semicolon is appended by the Eclipse API when retrieving it from the plugin's preference
		// page, it must be deleted.
		String fontString = preferences.get(PreferenceConstants.PREF_FONT, PreferenceConstants.PREF_FONT_DEFAULT)
				.replace(";", "");
		// An empty string is returned when the user has not set the font in the preferences; do not set the font so the
		// plugin will display the default font of the StyledText component instead.
		if (fontString.length() != 0) {
			font = new Font(Display.getCurrent(), new FontData(fontString));
			setFont(font);
		}
	}

	/**
	 * Removes all the text from the note tab.
	 */
	public void clearText() {
		setText("");
	}

	/**
	 * Applies a bold style to the currently selected text.
	 */
	public void boldSelection() {
		addStyleToSelection(SWT.BOLD);
	}

	/**
	 * Applies an italic style to the currently selected text.
	 */
	public void italicSelection() {
		addStyleToSelection(SWT.ITALIC);
	}

	/**
	 * Applies an underlined style to the currently selected text.
	 */
	public void underlineSelection() {
		Point selectionRange = getSelectionRange();
		// Retrieve the current styles in the selection. If the selection (or parts of it) does not have any style,
		// there are no corresponding entries in the following array.
		StyleRange[] currentStyles = getStyleRanges(selectionRange.x, selectionRange.y);

		StyleRange selectionStyleRange = new StyleRange(selectionRange.x, selectionRange.y, null, null);
		selectionStyleRange.underline = true;
		// Apply underlined style to the whole selection range; ranges that previously had no style and that are are not
		// accounted for in currentStyles are now underlined.
		setStyleRange(selectionStyleRange);

		// The above call overwrote the previous styles; the previous styles are re-applied with the additional
		// underlined style.
		for (int style = 0; style < currentStyles.length; ++style) {
			currentStyles[style].underline = true;
			setStyleRange(currentStyles[style]);
		}
	}

	/**
	 * Applies a new style to the currently selected text.
	 * 
	 * @param newStyle
	 */
	private void addStyleToSelection(int newStyle) {
		Point selectionRange = getSelectionRange();
		// Retrieve the current styles in the selection. If the selection (or parts of it) does not have any style,
		// there are no corresponding entries in the following array.
		StyleRange[] currentStyles = getStyleRanges(selectionRange.x, selectionRange.y);

		StyleRange selectionStyleRange = new StyleRange(selectionRange.x, selectionRange.y, null, null, newStyle);
		// Apply the style to the whole selection range; ranges that previously had no style and that are are not
		// accounted for in currentStyles now have the wanted style.
		setStyleRange(selectionStyleRange);

		// The above call overwrote the previous styles; the previous styles are re-applied with the additional
		// new one.
		for (int style = 0; style < currentStyles.length; ++style) {
			currentStyles[style].fontStyle |= newStyle;
			setStyleRange(currentStyles[style]);
		}
	}

	/**
	 * Removes all styles from the current selection.
	 */
	public void clearSelectionStyles() {
		Point selectionRange = getSelectionRange();
		// No colors are specified as they are defined by the plugin's preferences.
		StyleRange styleRange = new StyleRange(selectionRange.x, selectionRange.y, null, null, SWT.NORMAL);
		setStyleRange(styleRange);
	}

	/**
	 * Creates as string giving a description of the styles in the current note tab.
	 * 
	 * @return CSV string containing a serialised representation of the styles
	 */
	public String serialiseStyle() {
		StringBuilder styleSerialisation = new StringBuilder();
		StyleRange[] currentStyles = getStyleRanges();
		// Append integers corresponding to various information of each style range object, separated by
		// STRING_SEPARATOR.
		for (int style = 0; style < currentStyles.length; ++style) {
			styleSerialisation.append(currentStyles[style].start);
			styleSerialisation.append(STRING_SEPARATOR);
			styleSerialisation.append(currentStyles[style].length);
			styleSerialisation.append(STRING_SEPARATOR);
			styleSerialisation.append(currentStyles[style].fontStyle);
			styleSerialisation.append(STRING_SEPARATOR);
			// If underlined, 1, else 0.
			styleSerialisation.append((currentStyles[style].underline) ? 1 : 0);
			styleSerialisation.append(STRING_SEPARATOR);
		}
		return styleSerialisation.toString();
	}

	/**
	 * Applies styles to the current note tab based on a styles' serialisation string.
	 * 
	 * @param serialisation
	 */
	public void deserialiseStyle(String serialisation) {
		String[] integers = serialisation.split(STRING_SEPARATOR);
		StyleRange[] styles = new StyleRange[integers.length / 4];
		// Do the parsing.
		for (int style = 0; style < styles.length; ++style) {
			// Each StyleRange object has 4 corresponding integers in the CSV string.
			int integerIndex = 4 * style;
			styles[style] = new StyleRange();
			styles[style].start = Integer.parseInt(integers[integerIndex]);
			styles[style].length = Integer.parseInt(integers[integerIndex + 1]);
			styles[style].fontStyle = Integer.parseInt(integers[integerIndex + 2]);
			styles[style].underline = (Integer.parseInt(integers[integerIndex + 3]) == 1) ? true : false;
		}
		// Apply the parsed styles.
		setStyleRanges(styles);
	}

	/**
	 * Exports the brute text in the current note tab as a text file.
	 * 
	 * @param iWorkbenchPartSite
	 */
	public void saveToFile(IWorkbenchPartSite iWorkbenchPartSite) {
		// Retrieve the file to save to with an explorer window.
		FileDialog fileDialog = new FileDialog(iWorkbenchPartSite.getShell(), SWT.SAVE);
		fileDialog.setText("Save to File");
		String fileName = fileDialog.open();
		// Invalid name specified.
		if (fileName == null || fileName.length() == 0)
			return;

		File file = new File(fileName);
		if (file.exists()) {
			boolean overwrite = MessageDialog.openQuestion(iWorkbenchPartSite.getShell(), "File already exists",
					"Do you want to overwrite?");
			if (!overwrite)
				return;
		}

		FileOutputStream outStream = null;
		PrintWriter printStream = null;
		// Write the current note tab's text to the file, with handling of IO exceptions.
		try {
			outStream = new FileOutputStream(file);
			printStream = new PrintWriter(new FileOutputStream(file));
			printStream.print(getText());
			printStream.flush();
		} catch (IOException e) {
			MessageDialog.openInformation(iWorkbenchPartSite.getShell(), "Error",
					"Error while attempting to save the file.");
			e.printStackTrace();
		} finally {
			if (printStream != null)
				printStream.close();
			try {
				if (outStream != null) {
					outStream.close();
					MessageDialog.openInformation(iWorkbenchPartSite.getShell(), "File Saved",
							"The file has been succesfully saved.");
				}
			} catch (IOException e) {
				MessageDialog.openInformation(iWorkbenchPartSite.getShell(), "Error",
						"Error while attempting to save the file.");
				e.printStackTrace();
			}
		}
	}
}
