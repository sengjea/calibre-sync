package net.sengjea.calibre;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class NoServerDialogFragment extends DialogFragment {
	public interface NoServerDialogListener {
		public void onRetryClick();
	}
	private AlertDialog.Builder dialog_noserv;
	NoServerDialogListener mListener;
    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (NoServerDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoServerDialogListener");
        }
    }
	public NoServerDialogFragment() {
		// TODO Auto-generated constructor stub
	}
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		dialog_noserv = new AlertDialog.Builder(getActivity());
		dialog_noserv.setTitle(R.string.dialog_noserv_title);
		dialog_noserv.setMessage(R.string.dialog_noserv_message);
		dialog_noserv.setCancelable(true);
		dialog_noserv.setPositiveButton(R.string.dialog_button_retry, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				try { 
					mListener.onRetryClick();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		dialog_noserv.setNegativeButton(R.string.dialog_button_cancel, null);
		return dialog_noserv.create();
	}
}