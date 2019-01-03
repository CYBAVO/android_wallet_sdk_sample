package com.cybavo.example.wallet.pincode;


import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.cybavo.example.wallet.R;
import com.cybavo.example.wallet.helper.Helpers;
import com.cybavo.wallet.service.api.Callback;
import com.cybavo.wallet.service.auth.Auth;
import com.cybavo.wallet.service.auth.results.GetRestoreQuestionsResult;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

public class AnswerFragment extends Fragment {

    public AnswerFragment() {
        // Required empty public constructor
    }

    public static AnswerFragment newInstance() {
        AnswerFragment fragment = new AnswerFragment();
        return fragment;
    }

    private Auth mAuth;
    private SetupViewModel mSetupViewModel;

    private TextView[] mQuestionTexts = new TextView[3];
    private EditText[] mAnswerEdits = new EditText[3];

    private Button mRecover;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_answer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = Auth.getInstance();

        setupQuestion(view, 0, R.id.question1);
        setupQuestion(view, 1, R.id.question2);
        setupQuestion(view, 2, R.id.question3);

        setupAnswer(view, 0, R.id.answer1);
        setupAnswer(view, 1, R.id.answer2);
        setupAnswer(view, 2, R.id.answer3);

        mRecover = view.findViewById(R.id.action_recover);
        mRecover.setOnClickListener(v -> {
            ((RestorePinFragment) getParentFragment()).goRecoverPin();
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSetupViewModel = ViewModelProviders.of(getParentFragment(),
                new SetupViewModel.Factory(getActivity().getApplication())).get(SetupViewModel.class);

        // sync questions
        for (int i = 0; i < mQuestionTexts.length; i++) {
            final int idx = i;
            mSetupViewModel.getQuestion(idx).observe(this, question -> {
                mQuestionTexts[idx].setText(question);
            });
        }

        fetchRestoreQuestions();
    }

    private void setupQuestion(View parent, int index, @IdRes int viewId) {
        mQuestionTexts[index] = parent.findViewById(viewId);
    }

    private void setupAnswer(View parent, int index, @IdRes int viewId) {
        EditText et = parent.findViewById(viewId);
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                mSetupViewModel.setAnswer(index, s.toString());
            }
        });
        mAnswerEdits[index] = et;
    }

    private void setInProgress(boolean inProgress) {
        mRecover.setEnabled(!inProgress);
        for (EditText answer : mAnswerEdits) {
            answer.setEnabled(!inProgress);
        }
    }

    private void fetchRestoreQuestions() {
        setInProgress(true);
        for (TextView question : mQuestionTexts) {
            question.setText("…");
        }
        mAuth.getRestoreQuestions(new Callback<GetRestoreQuestionsResult>() {
            @Override
            public void onError(Throwable error) {
                Helpers.showToast(getContext(), "getRestoreQuestions failed: " + error.getMessage());
            }

            @Override
            public void onResult(GetRestoreQuestionsResult result) {
                mSetupViewModel.setQuestion(0, result.question1);
                mSetupViewModel.setQuestion(1, result.question1);
                mSetupViewModel.setQuestion(2, result.question1);
                setInProgress(false);
            }
        });
    }

    private void quit() {
        getFragmentManager().popBackStack();
    }
}
