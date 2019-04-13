package com.example.jum.dailynote;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingFragment extends Fragment {
    View view;
    Button btn_service_on;
    Button btn_service_off;

    public SettingFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_setting, container, false);

        btn_service_on = view.findViewById(R.id.btn_service_on);
        btn_service_on.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), notificationService.class);
                getActivity().startService(intent);
            }
        });

        btn_service_off = view.findViewById(R.id.btn_service_off);
        btn_service_off.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), notificationService.class);
                getActivity().stopService(intent);
            }
        });

        return view;
    }

}
