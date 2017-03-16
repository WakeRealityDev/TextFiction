package de.onyxbits.textfiction.input;

import java.io.File;

import org.json.JSONArray;

import de.onyxbits.textfiction.FileUtil;
import de.onyxbits.textfiction.R;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ViewFlipper;


/**
 * UI interface between the player and the engine's input buffer.
 */
public class InputFragment extends Fragment implements OnClickListener,
		OnEditorActionListener {

	/**
	 * Name of the file (relative to the gamedata dir where to quick commmands
	 * settings are kept.
	 */
	public static final String CMDFILE = "quickcommands.json";

	protected EditText cmdLine;
	private ImageButton submit;
	private ImageButton expand;
	private ImageButton expandKeymode;
	private LinearLayout buttonBar;
	protected ViewFlipper flipper;

	protected ImageButton forwards;
	protected ImageButton left;
	protected ImageButton right;
	protected ImageButton up;
	protected ImageButton down;
	protected Button keyboardLetter0;
	protected Button keyboardLetter1;

	// Magic numbers! See: §3.8 of the zmachine standard document.
	public static final char[] C_UP = { 129 };
	public static final char[] C_DOWN = { 130 };
	public static final char[] C_LEFT = { 131 };
	public static final char[] C_RIGHT = { 132 };
	public static final char[] ENTER = { 13 };
	public static final char[] SPACE = { 32 };
	public static final char[] DELETE = { 8 };

	protected boolean hasVerb = false;

	protected InputProcessor inputProcessor;
	private boolean autoCollapse;

	public InputFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		flipper = (ViewFlipper) inflater.inflate(R.layout.fragment_input,
				container, false);
		buttonBar = (LinearLayout) flipper.findViewById(R.id.quickcmdcontainer);
		cmdLine = (EditText) flipper.findViewById(R.id.userinput);
		submit = (ImageButton) flipper.findViewById(R.id.submit);
		expand = (ImageButton) flipper.findViewById(R.id.expand);
		expandKeymode = (ImageButton) flipper.findViewById(R.id.expand_keymode);
		submit.setOnClickListener(this);
		cmdLine.setOnEditorActionListener(this);
		expand.setOnClickListener(this);
		expandKeymode.setOnClickListener(this);

		forwards = (ImageButton) flipper.findViewById(R.id.forwards);
		down = (ImageButton) flipper.findViewById(R.id.cursor_down);
		left = (ImageButton) flipper.findViewById(R.id.cursor_left);
		right = (ImageButton) flipper.findViewById(R.id.cursor_right);
		up = (ImageButton) flipper.findViewById(R.id.cursor_up);

		forwards.setOnClickListener(this);
		down.setOnClickListener(this);
		left.setOnClickListener(this);
		right.setOnClickListener(this);
		up.setOnClickListener(this);

		((KeyboardButton) flipper.findViewById(R.id.keyboard))
				.setInputProcessor(inputProcessor);

		File commands = new File(FileUtil.getDataDir(inputProcessor.getStory()),
				CMDFILE);

		Context ctx = getActivity();
		CommandChanger changer = new CommandChanger(cmdLine, buttonBar, commands);

		try {
			String buttonDef = getActivity().getString(R.string.defaultcommands);
			JSONArray buttons = new JSONArray(buttonDef);
			if (commands.exists()) {
				buttonDef = FileUtil.getContents(commands);
				buttons = new JSONArray(buttonDef);
			}
			for (int i = 0; i < buttons.length(); i++) {
				ImageButton b = (ImageButton) inflater.inflate(
						R.layout.style_cmdbutton, null).findViewById(R.id.protocmdbutton);
				CmdIcon ico = CmdIcon.fromJSON(buttons.getJSONObject(i));
				b.setTag(ico);
				b.setImageResource(CmdIcon.ICONS[ico.imgid]);
				b.setOnClickListener(this);
				b.setOnLongClickListener(changer);
				b.setContentDescription(ico.cmd);
				buttonBar.addView(b);
			}
		}
		catch (Exception e) {
			Log.w(getClass().getName(), e);
		}
		flipper.setInAnimation(AnimationUtils.loadAnimation(ctx,
				R.anim.slide_in_right));
		flipper.setOutAnimation(AnimationUtils.loadAnimation(ctx,
				R.anim.slide_out_left));

		return flipper;
	}

	/**
	 * Set whether or not the keyboard should collapse when the "DONE" button is
	 * pressed.
	 * 
	 * @param ac
	 *          true to automatically collapse the keyboard on DONE
	 */
	public void setAutoCollapse(boolean ac) {
		autoCollapse = ac;
	}

	/**
	 * Toggle between commandline and keypress input
	 */
	public void toggleInput() {
		flipper.showNext();
	}

	/**
	 * Query the input style
	 * 
	 * @return true if showing the commandline
	 */
	public boolean isPrompt() {
		return (flipper.getCurrentView().getId() == R.id.inputcontainer);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			inputProcessor = (InputProcessor) activity;
		}
		catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnArticleSelectedListener");
		}
	}

	@Override
	public void onClick(View v) {
		if (v == submit) {
			executeCommand();
			return;
		}
		if (v == expand) {
			getActivity().openOptionsMenu();
			return;
		}
		if (v == expandKeymode) {
			getActivity().openOptionsMenu();
			return;
		}
		if (v == forwards) {
			inputProcessor.executeCommand(ENTER);
		}
		if (v == up) {
			inputProcessor.executeCommand(C_UP);
		}
		if (v == down) {
			inputProcessor.executeCommand(C_DOWN);
		}
		if (v == left) {
			inputProcessor.executeCommand(C_LEFT);
		}
		if (v == right) {
			inputProcessor.executeCommand(C_RIGHT);
		}

		if (v.getTag() instanceof CmdIcon) {
			CmdIcon ci = (CmdIcon) v.getTag();
			if (ci.atOnce) {
				inputProcessor.executeCommand((ci.cmd + "\n").toCharArray());
			}
			else {
				// Allow the player to select either the verb or an object first. If
				// an object is selected first and a verb second, assume the input to
				// be finished and execute it. The other way around: allow more objects
				// to be added.
				String tmp = "";
				if (hasVerb == false) {
					tmp = cmdLine.getEditableText().toString().trim();
				}
				cmdLine.setText(ci.cmd.trim() + " " + tmp);
				cmdLine.setSelection(cmdLine.getEditableText().toString().length());
				hasVerb = true;
				if (tmp.length() > 0) {
					executeCommand();
				}
			}
		}
	}

	/**
	 * Call after running a command to clear the commandline
	 */
	public void reset() {
		cmdLine.setText("");
		hasVerb = false;
	}

	/**
	 * Append a "word" to the prompt.
	 * 
	 * @param str
	 *          the string to add (does not require whitespaces at the start or
	 *          end).
	 */
	public void appendWord(String str) {
		if (str.length() > 0) {
			String tmp = cmdLine.getText().toString().trim();
			tmp = tmp.trim() + " " + str.trim();
			cmdLine.setText(tmp);
			cmdLine.setSelection(tmp.length());
		}
	}

	/**
	 * Delete the rightmost word on the prompt.
	 */
	public void removeWord() {
		String tmp = cmdLine.getText().toString().trim();
		int idx = tmp.lastIndexOf(' ');
		if (idx > 0) {
			tmp = tmp.substring(0, idx);
			cmdLine.setText(tmp);
			cmdLine.setSelection(tmp.length());
		}
		else {
			reset();
		}
	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		// Issue #17 https://github.com/onyxbits/TextFiction/issues/17
		if (event != null) {
			if (event.getAction() == KeyEvent.ACTION_UP) {
				if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
					// Direct requestFocus() does not seem to change behavior. Posting a Runnable
					//  without delay seems to restore focus on hard keyboard use.
					cmdLine.post(new Runnable() {
						@Override
						public void run() {
							try {
								cmdLine.requestFocus();
							} catch (Exception e) {
								Log.e("InputFragment", "Exception cmdLine.requestFocus() ", e);
							}
						}
					});
				} else {
					// Log.i("InputFragment", "[inputFocus] keycode " + event.getKeyCode());
				}
				// skip sending to fiction engine, we only want the key down.
				return false;
			}
		}

		executeCommand();
		return !autoCollapse;
	}

	protected void executeCommand() {
		inputProcessor.executeCommand((cmdLine.getText().toString() + "\n")
				.toCharArray());
	}

	public void setupLimitedKeyboard() {
	}
}
