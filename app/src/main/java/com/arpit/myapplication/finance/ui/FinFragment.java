package com.arpit.myapplication.finance.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.arpit.myapplication.finance.FinanceStore;

/** Base for the finance tabs: a scrollable column of cards that is rebuilt on {@link #refresh()}. */
public abstract class FinFragment extends Fragment {
    protected FinanceStore store;
    private LinearLayout content;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        Context c = requireContext();
        store = FinanceStore.get(c);
        ScrollView sv = new ScrollView(c);
        sv.setFillViewport(true);
        sv.setClipToPadding(false);
        content = new LinearLayout(c);
        content.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(c, 16);
        content.setPadding(p, Ui.dp(c, 6), p, Ui.dp(c, 28));
        sv.addView(content, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return sv;
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    public void refresh() {
        if (content == null || !isAdded()) return;
        store = FinanceStore.get(requireContext());
        content.removeAllViews();
        render(content);
    }

    protected abstract void render(LinearLayout root);

    protected void emptyState(LinearLayout root) {
        Context c = requireContext();
        LinearLayout card = Ui.card(c, root);
        card.addView(Ui.title(c, "No data yet"));
        Ui.add(card, Ui.sub(c, "Import a statement, or load sample data to explore the app."), 6);
        android.widget.TextView b = Ui.button(c, "Go to Import", com.arpit.myapplication.R.drawable.bg_button_blue);
        b.setOnClickListener(v -> {
            if (getActivity() instanceof com.arpit.myapplication.FinanceActivity) {
                ((com.arpit.myapplication.FinanceActivity) getActivity()).selectTab(com.arpit.myapplication.R.id.nav_import);
            }
        });
        Ui.add(card, b, 16);
    }
}
