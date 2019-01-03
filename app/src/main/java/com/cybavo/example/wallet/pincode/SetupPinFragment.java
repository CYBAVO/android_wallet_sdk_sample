package com.cybavo.example.wallet.pincode;


import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.cybavo.example.wallet.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

public class SetupPinFragment extends Fragment {

    public SetupPinFragment() {
        // Required empty public constructor
    }

    public static SetupPinFragment newInstance() {
        SetupPinFragment fragment = new SetupPinFragment();
        return fragment;
    }

    private SetupViewModel mSetupViewModel;
    private EditText mPinCodeEdit;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_setup_pin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPinCodeEdit = view.findViewById(R.id.newPinCode);
        mPinCodeEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                mSetupViewModel.setPinCode(s.toString());
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSetupViewModel = ViewModelProviders.of(getParentFragment(),
                new SetupViewModel.Factory(getActivity().getApplication())).get(SetupViewModel.class);
    }
}
