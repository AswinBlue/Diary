package com.example.jum.dailynote;


import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.Calendar;

public class DiaryFragment extends Fragment {
    View view;
    java.util.Calendar calendar = java.util.Calendar.getInstance();
	
    // year, month, day, hour, minute
    int year = calendar.get(java.util.Calendar.YEAR);
    int month = calendar.get(java.util.Calendar.MONTH) + 1;
    int day = calendar.get(java.util.Calendar.DAY_OF_MONTH);

    Button button_day[] = new Button[7]; // title line of calendar day
	Button button_date[] = new Button[35]; // body line of calendar date

    public DiaryFragment() {
        calendar.set(Calendar.DATE, 1);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_diary, container, false);

        // settings for Floating Action Button
        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment_layer, new MainFragment()).commit();
                Snackbar.make(view, "다이어리 저장", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });

        // setting for calendar
        String pkg = getActivity().getPackageName();
        for(int i=0; i<7; i++){
            int k = getResources().getIdentifier("day" + (i+1), "id", pkg);
            button_day[i] = view.findViewById(k);
        }
        for(int i=0; i<5; i++){
            for(int j=0; j<7; j++) {
                int k = getResources().getIdentifier("w" + (i+1) + "d" + (j+1), "id", pkg);
                button_date[i*7+j] = view.findViewById(k);
            }
        }


        return view;
    }
}
