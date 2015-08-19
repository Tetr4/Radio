package de.winterrettich.ninaradio.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;

import de.winterrettich.ninaradio.R;
import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.event.AddStationEvent;
import de.winterrettich.ninaradio.model.Station;

public class AddStationDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // inflate content layout
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_station, null);
        final EditText nameEditText = (EditText) dialogView.findViewById(R.id.station_name);
        final EditText urlEditText = (EditText) dialogView.findViewById(R.id.station_url);

        // build alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.add_station)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, null) // see below
                .setNegativeButton(android.R.string.cancel, null);
        final AlertDialog alertDialog = builder.create();

        // Custom positive listener (when dialog is shown and button is accessible).
        // Otherwise dismiss would be called automatically preventing validation of input.
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {

                Button positiveButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String name = nameEditText.getText().toString();
                        String url = urlEditText.getText().toString();

                        // validate name and url
                        boolean error = false;
                        if (name.isEmpty()) {
                            nameEditText.setError(getString(R.string.invalid_name));
                            error = true;
                        }
                        if (!URLUtil.isValidUrl(url)) {
                            urlEditText.setError(getString(R.string.invalid_url));
                            error = true;
                        }

                        if (!error) {
                            Station station = new Station(name, url);
                            RadioApplication.sBus.post(new AddStationEvent(station));
                            alertDialog.dismiss();
                        }
                    }
                });

            }
        });

        // alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return alertDialog;
    }

}
