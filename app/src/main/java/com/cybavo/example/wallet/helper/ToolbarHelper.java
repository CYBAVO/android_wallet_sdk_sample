/**
 * Copyright (c) 2019 CYBAVO, Inc.
 * https://www.cybavo.com
 *
 * All rights reserved.
 */

package com.cybavo.example.wallet.helper;

import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.MenuRes;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

public class ToolbarHelper {

    public static class Helper {
        private final Toolbar mToolbar;

        private Helper(Toolbar toolbar) {
            mToolbar = toolbar;
        }

        public Helper title(@StringRes int titleIdRes) {
            mToolbar.setTitle(titleIdRes);
            return this;
        }

        public Helper title(CharSequence title) {
            mToolbar.setTitle(title);
            return this;
        }

        public Helper menu(@MenuRes int menuResId, Toolbar.OnMenuItemClickListener menuListener) {
            mToolbar.inflateMenu(menuResId);
            mToolbar.setOnMenuItemClickListener(menuListener);
            return this;
        }

        public Helper onBack(View.OnClickListener listener) {
            mToolbar.setNavigationOnClickListener(listener);
            return this;
        }

        public Helper noBack() {
            mToolbar.setNavigationIcon(null);
            mToolbar.setNavigationOnClickListener(null);
            return this;
        }

        public Toolbar done() {
            return mToolbar;
        }
    }


    public static Helper setupToolbar(View parent, @IdRes int toolBarId) {
        return new Helper(parent.findViewById(toolBarId));
    }
}
