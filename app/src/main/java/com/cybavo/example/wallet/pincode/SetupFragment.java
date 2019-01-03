package com.cybavo.example.wallet.pincode;


import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.cybavo.example.wallet.NavFragment;
import com.cybavo.example.wallet.R;
import com.cybavo.example.wallet.helper.Helpers;
import com.cybavo.example.wallet.main.MainViewModel;
import com.cybavo.wallet.service.api.Callback;
import com.cybavo.wallet.service.api.Error;
import com.cybavo.wallet.service.auth.Auth;
import com.cybavo.wallet.service.auth.BackupChallenge;
import com.cybavo.wallet.service.auth.results.SetPinCodeResult;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

public class SetupFragment extends Fragment {

    private static final String TAG = SetupFragment.class.getSimpleName();

    public SetupFragment() {
        // Required empty public constructor
    }

    public static SetupFragment newInstance() {
        SetupFragment fragment = new SetupFragment();
        return fragment;
    }

    private Step mStep = Step.VERIFY_CODE;
    private MainViewModel mMainViewModel;
    private SetupViewModel mSetupViewModel;

    private Button mSubmit;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_setup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSubmit = view.findViewById(R.id.submit);
        mSubmit.setOnClickListener(v -> next());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mMainViewModel = ViewModelProviders.of(getParentFragment(),
                new MainViewModel.Factory(getActivity().getApplication()))
                .get(MainViewModel.class);
        mMainViewModel.getUserState().observe(this, userState -> {
            if (userState != null && userState.setPin) {
                quit();
            }
        });

        mSetupViewModel = ViewModelProviders.of(this,
                new SetupViewModel.Factory(getActivity().getApplication())).get(SetupViewModel.class);
        showStep(Step.PIN);
    }

    public <F extends Fragment> boolean fragmentExists(Class<F> clz) {
        return getChildFragmentManager().findFragmentByTag(clz.getSimpleName()) != null;
    }

    private void showFragment(Fragment fragment) {
        final String tag = fragment.getClass().getSimpleName();

        if (getChildFragmentManager().findFragmentByTag(tag) == null) {
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.recoverRoot, fragment, fragment.getClass().getSimpleName())
                    .commit();
        }
    }

    private void showStep(Step step) {
        switch (step) {
            case PIN:
                if (!fragmentExists(SetupPinFragment.class)) {
                    showFragment(SetupPinFragment.newInstance());
                    mSubmit.setText(R.string.action_next);
                }
                break;
            case BACKUP:
                if (!fragmentExists(BackupFragment.class)) {
                    showFragment(BackupFragment.newInstance());
                    mSubmit.setText(R.string.action_done);
                }
                break;
        }
        mStep = step;
    }

    private void next() {
        switch (mStep) {
            case PIN:
                if (!Helpers.isPinCodeValid(mSetupViewModel.getPinCode().getValue())) {
                    Helpers.showToast(getContext(), getString(R.string.message_invalid_pin));
                    return;
                }
                showStep(Step.BACKUP);
                break;
            case BACKUP:
                setupPin();
        }
    }

    private void setupPin() {
        final String pinCode = mSetupViewModel.getPinCode().getValue();
        final String question1 = mSetupViewModel.getQuestion(0).getValue(),
                question2 = mSetupViewModel.getQuestion(1).getValue(),
                question3 = mSetupViewModel.getQuestion(2).getValue();

        final String answer1 = mSetupViewModel.getAnswer(0).getValue(),
                answer2 = mSetupViewModel.getAnswer(1).getValue(),
                answer3 = mSetupViewModel.getAnswer(2).getValue();

        if (question1.isEmpty() || question2.isEmpty() || question3.isEmpty() // questions
                || answer1.isEmpty() || answer2.isEmpty() || answer3.isEmpty() // answers
                || pinCode.isEmpty()) { // pinCode
            return;
        }

        mSubmit.setEnabled(false);
        Auth.getInstance().setupPinCode(pinCode,
                BackupChallenge.make(question1, answer1),
                BackupChallenge.make(question2, answer2),
                BackupChallenge.make(question3, answer3),
                new Callback<SetPinCodeResult>() {
            @Override
            public void onError(Throwable error) {
                Log.w(TAG, "setupPinCode failed", error);
                mSubmit.setEnabled(true);
                Helpers.showToast(getContext(), "setupPinCode failed: " + error.getMessage());
            }

            @Override
            public void onResult(SetPinCodeResult result) {
                mSubmit.setEnabled(true);
                mMainViewModel.fetchUserState();
            }
        });
    }

    private void quit() {
        NavFragment.find(this).leaveSetup();
    }
}
