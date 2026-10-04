package com.arpit.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.arpit.myapplication.finance.FinTxn;
import com.arpit.myapplication.finance.FinanceStore;
import com.arpit.myapplication.finance.ui.DashboardFragment;
import com.arpit.myapplication.finance.ui.FinFragment;
import com.arpit.myapplication.finance.ui.ImportFragment;
import com.arpit.myapplication.finance.ui.InsightsFragment;
import com.arpit.myapplication.finance.ui.PlannerFragment;
import com.arpit.myapplication.finance.ui.TransactionsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

/** Home screen after login: five finance tabs on a dark theme. The wallet lives behind the top-right button. */
public class FinanceActivity extends AppCompatActivity {
    private BottomNavigationView nav;
    private TextView title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this,
                androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT));
        setContentView(R.layout.activity_finance);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.financeRoot), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        title = findViewById(R.id.financeTitle);
        nav = findViewById(R.id.financeNav);
        findViewById(R.id.buttonWalletHub).setVisibility(android.view.View.GONE);

        nav.setOnItemSelectedListener(item -> {
            show(item.getItemId());
            return true;
        });
        if (savedInstanceState == null) nav.setSelectedItemId(R.id.nav_dashboard);
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncWallet();
    }

    public void selectTab(int id) { nav.setSelectedItemId(id); }

    private void show(int id) {
        Fragment f;
        String t;
        if (id == R.id.nav_insights) { f = new InsightsFragment(); t = "Insights"; }
        else if (id == R.id.nav_transactions) { f = new TransactionsFragment(); t = "Transactions"; }
        else if (id == R.id.nav_planner) { f = new PlannerFragment(); t = "Planner"; }
        else if (id == R.id.nav_import) { f = new ImportFragment(); t = "Import"; }
        else { f = new DashboardFragment(); t = "Dashboard"; }
        title.setText(t);
        getSupportFragmentManager().beginTransaction().replace(R.id.financeContainer, f).commit();
    }

    /** Pulls the user's wallet transfers from the server so they show up in the analytics too. */
    private void syncWallet() {
        final UserRepository users = ServiceLocator.provideUserRepository();
        Long backendId = users.getBackendUserId();
        if (backendId == null) return;
        ServiceLocator.provideWalletRepository().getTransactionsAsync(String.valueOf(backendId), (ok, list) -> {
            if (!ok || list == null || isFinishing() || isDestroyed()) return;
            List<FinTxn> out = new ArrayList<>();
            for (WalletRepository.Transaction w : list) {
                if ("ADD".equals(w.type)) continue; // topping up your own wallet is not income or spending
                FinTxn t = new FinTxn();
                t.id = "w-" + w.time + "-" + w.type + "-" + w.amount;
                t.time = w.time;
                t.amount = w.amount;
                t.received = "RECEIVED".equals(w.type);
                t.counterparty = "Wallet transfer";
                t.method = "Wallet";
                t.status = "FAILED".equalsIgnoreCase(w.status) ? "FAILED" : "SUCCESS";
                t.source = "wallet";
                out.add(t);
            }
            FinanceStore.get(this).replaceWalletTxns(out);
            Fragment cur = getSupportFragmentManager().findFragmentById(R.id.financeContainer);
            if (cur instanceof FinFragment) ((FinFragment) cur).refresh();
        });
    }
}
