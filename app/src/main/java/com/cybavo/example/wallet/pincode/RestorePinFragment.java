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
import com.cybavo.example.wallet.helper.ToolbarHelper;
import com.cybavo.wallet.service.api.Callback;
import com.cybavo.wallet.service.api.Error;
import com.cybavo.wallet.service.auth.Auth;
import com.cybavo.wallet.service.auth.BackupChallenge;
import com.cybavo.wallet.service.auth.results.RestorePinCodeResult;
import com.cybavo.wallet.service.auth.results.VerifyRestoreQuestionsResult;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

public class RestorePinFragment extends Fragment {

    private static final String TAG = RestorePinFragment.class.getSimpleName();

    public RestorePinFragment() {
        // Required empty public constructor
    }

    public static RestorePinFragment newInstance() {
        RestorePinFragment fragment = new RestorePinFragment();
        return fragment;
    }

    private Auth mAuth;
    private Step mStep = Step.VERIFY_CODE;
    private SetupViewModel mSetupViewModel;
    private Button mSubmit;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_restore_pin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ToolbarHelper.setupToolbar(view, R.id.appBar)
                .title(R.string.title_restore_pin)
                .onBack(v -> quit())
                .done();

        mAuth = Auth.getInstance();

        mSubmit = view.findViewById(R.id.submit);
        mSubmit.setOnClickListener(v -> next());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSetupViewModel = ViewModelProviders.of(this,
                new SetupViewModel.Factory(getActivity().getApplication())).get(SetupViewModel.class);
        showStep(Step.ANSWER);
    }

    private void showStep(Step step) {
        switch (step) {
            case ANSWER:
                if (!fragmentExists(AnswerFragment.class)) {
                    showFragment(AnswerFragment.newInstance());
                    mSubmit.setText(R.string.action_next);
                }
                break;
            case PIN:
                if (!fragmentExists(SetupPinFragment.class)) {
                    showFragment(SetupPinFragment.newInstance());
                    mSubmit.setText(R.string.action_done);
                }
                break;
        }
        mStep = step;
    }

    public <F extends Fragment> boolean fragmentExists(Class<F> clz) {
        return getChildFragmentManager().findFragmentByTag(clz.getSimpleName()) != null;
    }

    private void showFragment(Fragment fragment) {
        final String tag = fragment.getClass().getSimpleName();

        if (getChildFragmentManager().findFragmentByTag(tag) == null) {
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.restoreRoot, fragment, fragment.getClass().getSimpleName())
                    .commit();
        }
    }

    private void next() {
        switch (mStep) {
            case ANSWER:
                final String question1 = mSetupViewModel.getQuestion(0).getValue();
                final String question2 = mSetupViewModel.getQuestion(1).getValue();
                final String question3 = mSetupViewModel.getQuestion(2).getValue();

                final String answer1 = mSetupViewModel.getAnswer(0).getValue();
                final String answer2 = mSetupViewModel.getAnswer(1).getValue();
                final String answer3 = mSetupViewModel.getAnswer(2).getValue();

                verifyAnswer(question1, answer1, question2, answer2, question3, answer3);
                break;
            case PIN:
                if (!Helpers.isPinCodeValid(mSetupViewModel.getPinCode().getValue())) {
                    Helpers.showToast(getContext(), getString(R.string.message_invalid_pin));
                    return;
                }
                restore();
                break;
        }
    }

    private void setInProgress(boolean inProgress) {
        mSubmit.setEnabled(!inProgress);
    }

    private void verifyAnswer(String question1, String answer1, String question2, String answer2, String question3, String answer3) {
        if (question1.isEmpty() || question2.isEmpty() || question3.isEmpty() // questions
                || answer1.isEmpty() || answer2.isEmpty() || answer3.isEmpty()) { // answers
            return;
        }

        setInProgress(true);
        mAuth.verifyRestoreQuestions(
                BackupChallenge.make(question1, answer1),
                BackupChallenge.make(question2, answer2),
                BackupChallenge.make(question3, answer3), new Callback<VerifyRestoreQuestionsResult>() {
                    @Override
                    public void onError(Throwable error) {
                        setInProgress(false);
                        if (error instanceof Error && ((Error) error).getCode() == Error.Code.ErrInvalidBackupAnswer) {
                            Helpers.showToast(getContext(), getString(R.string.message_incorrect_restore_answers));
                        } else {
                            Helpers.showToast(getContext(), "verifyRestoreQuestions failed: " + error.getMessage());
                        }
                    }

                    @Override
                    public void onResult(VerifyRestoreQuestionsResult result) {
                        setInProgress(false);
                        showStep(Step.PIN);
                    }
                });
    }

    private void restore() {

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

        mAuth.restorePinCode(pinCode,
                BackupChallenge.make(question1, answer1),
                BackupChallenge.make(question2, answer2),
                BackupChallenge.make(question3, answer3),
                new Callback<RestorePinCodeResult>() {
                    @Override
                    public void onError(Throwable error) {
                        Log.w(TAG, "changePinCode failed", error);
                        setInProgress(false);
                        Helpers.showToast(getContext(), "changePinCode failed: " + error.getMessage());
                    }

                    @Override
                    public void onResult(RestorePinCodeResult result) {
                        setInProgress(false);
                        Helpers.showToast(getContext(), getString(R.string.message_restore_success));
                        quit();
                    }
                });
    }

    void goRecoverPin() {
        quit();
        NavFragment.find(this).goRecoverPin();
    }

    private void quit() {
        getFragmentManager().popBackStack();
    }
}
