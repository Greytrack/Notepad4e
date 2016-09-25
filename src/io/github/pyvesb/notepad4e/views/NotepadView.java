package io.github.pyvesb.notepad4e.views;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.part.ViewPart;

import io.github.pyvesb.notepad4e.Notepad4e;
import io.github.pyvesb.notepad4e.preferences.PreferenceConstants;

/**
 * Class handling the plugin's view with the different note tabs.
 * 
 * @author Pyves
 *
 */
public class NotepadView extends ViewPart implements IPreferenceChangeListener {

	// The ID of the view as specified by the extension.
	public static final String ID = "notepad4e.views.NotepadView";

	// Keys used to store and retrieve the plugin's view between Eclipse sessions.
	private static final String MEMENTO_COUNT_KEY = "NumOfTabs";
	private static final String MEMENTO_TEXT_PREFIX_KEY = "TabText";
	private static final String MEMENTO_STYLE_PREFIX_KEY = "TabStyle";
	private static final String MEMENTO_TITLE_PREFIX_KEY = "TabTitle";

	// Object storing the plugin's view between Eclipse sessions.
	private IMemento memento;

	// Actions corresponding to the different buttons in the view.
	private Action addNewNoteAction;
	private Action clearNoteAction;
	private Action boldTextAction;
	private Action italicTextAction;
	private Action underlineTextAction;
	private Action clearTextStyleAction;
	private Action saveNoteAction;
	private Action renameNoteAction;
	private Action moveNoteLeftAction;
	private Action moveNoteRightAction;
	private Action preferencesAction;
	private Action websiteAction;

	// Object handling the different tabs.
	private CTabFolder noteTabsFolder;

	// User defined preferences.
	private IEclipsePreferences preferences;

	// Keyboard events listener.
	NoteTabKeyListener noteTabKeyListener;

	/**
	 * Constructor.
	 */
	public NotepadView() {
		noteTabKeyListener = new NoteTabKeyListener(this);
	}

	/**
	 * Allows to create the viewer and initialise it.
	 */
	@Override
	public void createPartControl(Composite parent) {
		preferences = InstanceScope.INSTANCE.getNode(Notepad4e.PLUGIN_ID);
		// Listen to any change to the preferences of the plugin.
		preferences.addPreferenceChangeListener(this);

		// Listen to keyboard events.
		Display.getCurrent().addFilter(SWT.KeyDown, noteTabKeyListener);

		noteTabsFolder = new CTabFolder(parent, SWT.MULTI | SWT.WRAP);

		restoreViewFromPreviousSession();

		PlatformUI.getWorkbench().getHelpSystem().setHelp(noteTabsFolder, "Notepad4e.viewer");

		makeActions();
		contributeToActionBars();
	}

	/**
	 * Allows to restore the plugin's view as it was in a previous session of Eclipse.
	 */
	private void restoreViewFromPreviousSession() {
		Integer numOfTabs = null;
		// Memento can be null if plugin was not previously launched in this working environment.
		if (memento != null) {
			numOfTabs = memento.getInteger(MEMENTO_COUNT_KEY);
		}

		if (numOfTabs == null || numOfTabs == 0) {
			String prefixName = preferences.get(PreferenceConstants.PREF_NAME_PREFIX,
					PreferenceConstants.PREF_NAME_PREFIX_DEFAULT);
			// No tabs were previously opened: create new tab.
			addNewTab(prefixName + " 1", "", "");
			// Set selection on this tab.
			noteTabsFolder.setSelection(0);
		} else {
			// Populate with tabs opened in previous session.
			for (int tab = 0; tab < numOfTabs; ++tab) {
				String tabTitle = memento.getString(MEMENTO_TITLE_PREFIX_KEY + tab);
				String tabText = memento.getString(MEMENTO_TEXT_PREFIX_KEY + tab);
				String tabStyle = memento.getString(MEMENTO_STYLE_PREFIX_KEY + tab);
				addNewTab(tabTitle, tabText, tabStyle);
				// Set selection on the last tab.
				noteTabsFolder.setSelection(numOfTabs - 1);
			}
		}
	}

	/**
	 * Adds a new tab to the view.
	 * 
	 * @param title
	 * @param text
	 * @param style
	 */
	private void addNewTab(String title, String text, String style) {
		CTabItem noteTabItem = new CTabItem(noteTabsFolder, SWT.NONE);
		noteTabItem.setText(title);
		// Add listener to clean up corresponding NoteTab when disposing the CTabItem.
		noteTabItem.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				CTabItem itemToDispose = (CTabItem) e.getSource();
				((NoteTab) itemToDispose.getControl()).dispose();
			}
		});
		NoteTab tab = new NoteTab(noteTabsFolder, text, noteTabKeyListener);
		if (style.length() > 0)
			tab.deserialiseStyle(style);
		noteTabItem.setControl(tab);
	}

	/**
	 * Initialises the view.
	 * 
	 * @param site
	 * @param memento
	 * @throws PartInitException
	 */
	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		this.memento = memento;
	}

	/**
	 * Populates the different action bars of the view.
	 */
	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	/**
	 * Populates the drop down menu action bar.
	 * 
	 * @param manager
	 */
	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(saveNoteAction);
		manager.add(renameNoteAction);
		manager.add(moveNoteLeftAction);
		manager.add(moveNoteRightAction);
		manager.add(new Separator());
		manager.add(preferencesAction);
		manager.add(websiteAction);
	}

	/**
	 * Populates the tool bar.
	 * 
	 * @param manager
	 */
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(boldTextAction);
		manager.add(italicTextAction);
		manager.add(underlineTextAction);
		manager.add(clearTextStyleAction);
		manager.add(new Separator());
		manager.add(addNewNoteAction);
		manager.add(clearNoteAction);
	}

	/**
	 * Defines all the plugin's actions corresponding to the different buttons in the view.
	 */
	private void makeActions() {
		addNewNoteAction = new Action() {
			@Override
			public void run() {
				doNewNote();
			}
		};
		setTextAndImageToAction(addNewNoteAction, "New Note (Ctrl+T)", ViewImages.NEW_NOTE);

		clearNoteAction = new Action() {
			@Override
			public void run() {
				doClearNote();
			}
		};
		setTextAndImageToAction(clearNoteAction, "Clear Note (Ctrl+K)", ViewImages.CLEAR_NOTE);

		boldTextAction = new Action() {
			@Override
			public void run() {
				doBoldText();
			}
		};
		setTextAndImageToAction(boldTextAction, "Bold (Ctrl+B)", ViewImages.BOLD_TEXT);

		italicTextAction = new Action() {
			@Override
			public void run() {
				doItalicText();
			}
		};
		setTextAndImageToAction(italicTextAction, "Italic (Ctrl+I)", ViewImages.ITALIC_TEXT);

		underlineTextAction = new Action() {
			@Override
			public void run() {
				doUnderlineText();
			}
		};
		setTextAndImageToAction(underlineTextAction, "Underline (Ctrl+U)", ViewImages.UNDERLINE_TEXT);

		clearTextStyleAction = new Action() {
			@Override
			public void run() {
				doClearTextStyle();
			}
		};
		setTextAndImageToAction(clearTextStyleAction, "Clear Style (Ctrl+D)", ViewImages.CLEAR_TEXT);

		saveNoteAction = new Action() {
			@Override
			public void run() {
				doSaveNote();
			}
		};
		setTextAndImageToAction(saveNoteAction, "Export File", ViewImages.SAVE_NOTE);

		renameNoteAction = new Action() {
			@Override
			public void run() {
				doRenameNote();
			}
		};
		setTextAndImageToAction(renameNoteAction, "Rename Note", ViewImages.RENAME_NOTE);

		moveNoteLeftAction = new Action() {
			@Override
			public void run() {
				doMoveNoteLeft();
			}
		};
		setTextAndImageToAction(moveNoteLeftAction, "Move Left", ViewImages.MOVE_NOTE_LEFT);

		moveNoteRightAction = new Action() {
			@Override
			public void run() {
				doMoveNoteRight();
			}
		};
		setTextAndImageToAction(moveNoteRightAction, "Move Right", ViewImages.MOVE_NOTE_RIGHT);

		preferencesAction = new Action() {
			@Override
			public void run() {
				doPreferences();
			}
		};
		setTextAndImageToAction(preferencesAction, "Preferences", ViewImages.PREFERENCES);

		websiteAction = new Action() {
			@Override
			public void run() {
				doWebsite();
			}
		};
		setTextAndImageToAction(websiteAction, "Website", ViewImages.WEBSITE);
	}

	/**
	 * Sets the image and the tool tip to an action button.
	 * 
	 * @param action
	 * @param text
	 * @param image
	 */
	private void setTextAndImageToAction(Action action, String text, String image) {
		action.setText(text);
		action.setToolTipText(text);

		// The URL matches an image in Eclipse's platform image bank.
		URL url = null;
		try {
			url = new URL(image);
		} catch (MalformedURLException e) {
		}
		ImageDescriptor imageDescriptor = ImageDescriptor.createFromURL(url);

		action.setImageDescriptor(imageDescriptor);
	}

	/**
	 * Swaps two note tabs in the view.
	 * 
	 * @param selectedIndex
	 * @param swappedIndex
	 */
	private void swapTabs(int selectedIndex, int swappedIndex) {
		NoteTab selectedTab = getNoteTab(selectedIndex);
		NoteTab swappedTab = getNoteTab(swappedIndex);

		noteTabsFolder.getItem(swappedIndex).setControl(selectedTab);
		noteTabsFolder.getItem(selectedIndex).setControl(swappedTab);

		String selectedTitle = noteTabsFolder.getItem(selectedIndex).getText();
		String swappedTitle = noteTabsFolder.getItem(swappedIndex).getText();

		noteTabsFolder.getItem(swappedIndex).setText(selectedTitle);
		noteTabsFolder.getItem(selectedIndex).setText(swappedTitle);

		noteTabsFolder.setSelection(swappedIndex);
	}

	/**
	 * Passes the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		if (noteTabsFolder.getItemCount() == 0) {
			// Give focus to the plugin; hack-ish trick to "steal" focus from other elements in some scenarios (example:
			// no tabs and try to open view again via quick access).
			noteTabsFolder.getAccessible().getControl().setFocus();
		} else {
			// Set focus on the last item in the tabs folder component.
			noteTabsFolder.getItem(noteTabsFolder.getItemCount() - 1).getControl().setFocus();
		}
	}

	/**
	 * Saves the current view's state so it can be restored during the next Eclipse session.
	 * 
	 * @param memento
	 */
	@Override
	public void saveState(IMemento memento) {
		memento.putInteger(MEMENTO_COUNT_KEY, noteTabsFolder.getItemCount());

		for (int tab = 0; tab < noteTabsFolder.getItemCount(); ++tab) {
			memento.putString(MEMENTO_TEXT_PREFIX_KEY + tab, getNoteTab(tab).getText());
			memento.putString(MEMENTO_STYLE_PREFIX_KEY + tab, getNoteTab(tab).serialiseStyle());
			memento.putString(MEMENTO_TITLE_PREFIX_KEY + tab, noteTabsFolder.getItem(tab).getText());
		}
	}

	/**
	 * Refreshes all note tabs when a change in the plugin's preferences is detected.
	 * 
	 * @param event
	 */
	@Override
	public void preferenceChange(PreferenceChangeEvent event) {
		for (int tab = 0; tab < noteTabsFolder.getItemCount(); ++tab) {
			getNoteTab(tab).setPreferences();
		}
	}

	/**
	 * Returns a NoteTab object given an index in the tab folder.
	 * 
	 * @param index
	 * @return
	 */
	private NoteTab getNoteTab(int index) {
		return (NoteTab) (noteTabsFolder.getItem(index).getControl());
	}

	/**
	 * Performs the new note action.
	 */
	public void doNewNote() {
		String namePrefix = preferences.get(PreferenceConstants.PREF_NAME_PREFIX,
				PreferenceConstants.PREF_NAME_PREFIX_DEFAULT);
		// Add a new tab with a number appended to its name (Note 1, Note 2, Note 3, etc.).
		addNewTab(namePrefix + " " + (noteTabsFolder.getItemCount() + 1), "", "");
		noteTabsFolder.setSelection(noteTabsFolder.getItemCount() - 1);
	}

	/**
	 * Performs the clear note action.
	 */
	public void doClearNote() {
		if (noteTabsFolder.getItemCount() == 0)
			return;
		getNoteTab(noteTabsFolder.getSelectionIndex()).clearText();
	}

	/**
	 * Performs the bold text action.
	 */
	public void doBoldText() {
		if (noteTabsFolder.getItemCount() == 0)
			return;
		getNoteTab(noteTabsFolder.getSelectionIndex()).boldSelection();
	}

	/**
	 * Performs the italic text action.
	 */
	public void doItalicText() {
		if (noteTabsFolder.getItemCount() == 0)
			return;
		getNoteTab(noteTabsFolder.getSelectionIndex()).italicSelection();
	}

	/**
	 * Performs the underline text action.
	 */
	public void doUnderlineText() {
		if (noteTabsFolder.getItemCount() == 0)
			return;
		getNoteTab(noteTabsFolder.getSelectionIndex()).underlineSelection();
	}

	/**
	 * Performs the clear text action.
	 */
	public void doClearTextStyle() {
		if (noteTabsFolder.getItemCount() == 0)
			return;
		getNoteTab(noteTabsFolder.getSelectionIndex()).clearSelectionStyles();
	}

	/**
	 * Performs the save note action.
	 */
	public void doSaveNote() {
		if (noteTabsFolder.getItemCount() == 0)
			return;
		getNoteTab(noteTabsFolder.getSelectionIndex()).saveToFile(getSite());
	}

	/**
	 * Performs the rename note action.
	 */
	public void doRenameNote() {
		if (noteTabsFolder.getItemCount() == 0)
			return;

		// Open a dialog window so user can enter the new name of his note.
		InputDialog inputDialog = new InputDialog(getSite().getShell(), "Rename Note",
				"Please select the new name of the note:", null, null);
		int returnValue = inputDialog.open();
		if (returnValue == SWT.CANCEL)
			return;

		noteTabsFolder.getItem(noteTabsFolder.getSelectionIndex()).setText(inputDialog.getValue());
	}

	/**
	 * Performs the move note left action.
	 */
	public void doMoveNoteLeft() {
		// Do not move left if there are no notes (== -1), or if first note.
		if (noteTabsFolder.getSelectionIndex() < 1)
			return;
		swapTabs(noteTabsFolder.getSelectionIndex(), noteTabsFolder.getSelectionIndex() - 1);
	}

	/**
	 * Performs the move note right action.
	 */
	public void doMoveNoteRight() {
		// Do note move right if only one or no notes, or if last note.
		if (noteTabsFolder.getItemCount() < 2
				|| noteTabsFolder.getSelectionIndex() == noteTabsFolder.getItemCount() - 1)
			return;
		swapTabs(noteTabsFolder.getSelectionIndex(), noteTabsFolder.getSelectionIndex() + 1);
	}

	/**
	 * Performs the preferences action.
	 */
	public void doPreferences() {
		// Create preference dialog page that will appear in current workbench window.
		PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(null, "notepad4e.preferences.PreferencePage",
				new String[] { "notepad4e.preferences.PreferencePage" }, null);
		dialog.open();
	}

	/**
	 * Performs the website action.
	 */
	public void doWebsite() {
		// Open website in the user's external browser.
		Program.launch("https://github.com/PyvesB/Notepad4e");
	}

	/**
	 * Checks whether the selected NoteTab in the view has focus.
	 * 
	 * @return true if the selected NoteTab in the view has focus
	 */
	public boolean isFocused() {
		if (noteTabsFolder.getItemCount() == 0)
			return false;
		return (getNoteTab(noteTabsFolder.getSelectionIndex()).isFocusControl() || noteTabsFolder.isFocusControl());
	}

	/**
	 * Unregister listeners and clean up.
	 */
	@Override
	public void dispose() {
		Display.getCurrent().removeFilter(SWT.KeyDown, noteTabKeyListener);
		preferences.removePreferenceChangeListener(this);
		super.dispose();
	}

	/**
	 * Closes the current NoteTab.
	 */
	public void closeCurrentNoteTab() {
		int selectionIndex = noteTabsFolder.getSelectionIndex();
		// Return if not note tabs are open.
		if (selectionIndex < 0)
			return;
		// Clean-up.
		noteTabsFolder.getItem(selectionIndex).dispose();
	}
}
