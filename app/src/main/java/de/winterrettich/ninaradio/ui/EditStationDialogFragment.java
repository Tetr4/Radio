package de.winterrettich.ninaradio.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;

import de.winterrettich.ninaradio.R;
import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.event.DatabaseEvent;
import de.winterrettich.ninaradio.model.Station;

public class EditStationDialogFragment extends DialogFragment implements View.OnClickListener {
    private static final String ARG_STATION_ID = "ARG_STATION_ID";

    public static EditStationDialogFragment newInstance() {
        return new EditStationDialogFragment();
    }

    public static EditStationDialogFragment newInstance(Station station) {
        EditStationDialogFragment fragment = new EditStationDialogFragment();

        Bundle args = new Bundle();
        args.putLong(ARG_STATION_ID, station.getId());
        fragment.setArguments(args);

        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // inflate content layout
        View dialogView = View.inflate(getContext(), R.layout.dialog_add_station, null);

        // add or edit a station?
        Station stationArgument = getStationArgument();
        boolean edit = stationArgument != null;

        if (edit) {
            EditText nameEditText = (EditText) dialogView.findViewById(R.id.station_name);
            EditText urlEditText = (EditText) dialogView.findViewById(R.id.station_url);
            nameEditText.setText(stationArgument.name);
            urlEditText.setText(stationArgument.url);
        }

        int titleResource = edit ? R.string.edit_station : R.string.add_station;

        // build alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(titleResource)
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
                positiveButton.setOnClickListener(EditStationDialogFragment.this);
            }
        });

        // alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return alertDialog;
    }

    private Station getStationArgument() {
        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(ARG_STATION_ID)) {
            long stationId = arguments.getLong(ARG_STATION_ID);
            return RadioApplication.sDatabase.findStationById(stationId);
        }
        return null;
    }

    @Override
    public void onClick(View dialogView) {
        Dialog dialog = getDialog();
        EditText nameEditText = (EditText) dialog.findViewById(R.id.station_name);
        EditText urlEditText = (EditText) dialog.findViewById(R.id.station_url);

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
            DatabaseEvent event;
            Station stationArgument = getStationArgument();
            if (stationArgument != null) {
                // edit station
                stationArgument.name = name;
                stationArgument.url = url;
                event = new DatabaseEvent(DatabaseEvent.Operation.UPDATE_STATION, stationArgument);
            } else {
                // new station
                Station station = new Station(name, url);
                event = new DatabaseEvent(DatabaseEvent.Operation.CREATE_STATION, station);
            }
            RadioApplication.sBus.post(event);

            // close dialog
            dialog.dismiss();
        }
    }

}
