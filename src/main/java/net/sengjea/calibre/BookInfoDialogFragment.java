package net.sengjea.calibre;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class BookInfoDialogFragment extends DialogFragment {
	public interface BookInfoDialogListener {
		public void confirmDelete(int id);
	}
	private int id;
	private BookInfoDialogListener mListener;
	public BookInfoDialogFragment() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (BookInfoDialogListener) activity;
		} catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement BookInfoDialogListener");
		}
	}
	public Dialog onCreateDialog(Bundle bundle) {
		if (getArguments() != null && getArguments().containsKey("id")) {
			id = getArguments().getInt("id");
		}
		AlertDialog.Builder dialogbuilder = new AlertDialog.Builder(getActivity());
		dialogbuilder.setTitle(R.string.dialog_button_delete);
		dialogbuilder.setMessage(R.string.dialog_deletefile_message);
		dialogbuilder.setCancelable(true);
		dialogbuilder.setNegativeButton(R.string.dialog_button_cancel, null);
		dialogbuilder.setPositiveButton(R.string.dialog_button_delete, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				mListener.confirmDelete(id);
			}

		});
		return dialogbuilder.create();
	}
}
