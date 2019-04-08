/**
 * Copyright (c) 2019 CYBAVO, Inc.
 * https://www.cybavo.com
 *
 * All rights reserved.
 */

package com.cybavo.example.wallet.pincode;


import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.cybavo.example.wallet.R;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

public class BackupFragment extends Fragment {

    public static BackupFragment newInstance() {
        BackupFragment fragment = new BackupFragment();
        return fragment;
    }

    public BackupFragment() {
        // Required empty public constructor
    }

    private SetupViewModel mSetupViewModel;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_backup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mSetupViewModel = ViewModelProviders.of(getParentFragment(),
                new SetupViewModel.Factory(getActivity().getApplication())).get(SetupViewModel.class);

        setupQandA(getView(), R.id.question1, R.id.answer1, 0);
        setupQandA(getView(), R.id.question2, R.id.answer2, 1);
        setupQandA(getView(), R.id.question3, R.id.answer3, 2);
    }

    private void setupQandA(View parent, @IdRes int spinnerId, @IdRes int editId, int index) {

        // questions
        final Spinner spinner = parent.findViewById(spinnerId);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(parent.getContext(), R.layout.dropdown_item_question);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) {
                mSetupViewModel.setQuestion(index, "");
            }

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSetupViewModel.setQuestion(index, adapter.getItem(position));
            }
        });
        mSetupViewModel.getAvailableQuestions(index).observe(this, questions -> {
            adapter.clear();
            adapter.addAll(questions);
        });

        final EditText answer = parent.findViewById(editId);
        answer.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                mSetupViewModel.setAnswer(index, s.toString());
            }
        });
    }
}
