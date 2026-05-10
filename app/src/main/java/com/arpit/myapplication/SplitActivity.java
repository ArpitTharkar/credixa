package com.arpit.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SplitActivity extends AppCompatActivity {

    private EditText editGroupName, editAddUser, editTotalAmount;
    private Button btnAddUser, btnSplit, btnCreateGroup, btnDeleteGroup;
    private LinearLayout usersContainer, resultContainer;
    private Spinner spinnerGroups;
    private TextView textGroupInfo;
    private final List<MemberEntry> members = new ArrayList<>();
    private final List<WalletRepository.SplitGroup> groups = new ArrayList<>();
    private WalletRepository repo;
    private String me;
    private String selectedGroupId;

    private static class MemberEntry {
        final String name;
        final View row;
        final EditText paidInput;

        MemberEntry(String name, View row, EditText paidInput) {
            this.name = name;
            this.row = row;
            this.paidInput = paidInput;
        }
    }

    private static class BalanceNode {
        String name;
        long amount;

        BalanceNode(String name, long amount) {
            this.name = name;
            this.amount = amount;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_split);

        repo = new WalletRepository(this);
        me = new UserRepository(this).getCurrentUser();
        if (me == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        editGroupName = findViewById(R.id.editTextGroupName);
        editAddUser = findViewById(R.id.editTextAddUser);
        editTotalAmount = findViewById(R.id.editTextTotalAmount);
        btnAddUser = findViewById(R.id.buttonAddUser);
        btnSplit = findViewById(R.id.buttonSplit);
        btnCreateGroup = findViewById(R.id.buttonCreateGroup);
        btnDeleteGroup = findViewById(R.id.buttonDeleteGroup);
        spinnerGroups = findViewById(R.id.spinnerGroups);
        textGroupInfo = findViewById(R.id.textViewGroupInfo);
        usersContainer = findViewById(R.id.usersContainer);
        resultContainer = findViewById(R.id.resultContainer);

        btnAddUser.setOnClickListener(v -> addUser());
        btnSplit.setOnClickListener(v -> doSplit());
        btnCreateGroup.setOnClickListener(v -> createGroup());
        btnDeleteGroup.setOnClickListener(v -> deleteSelectedGroup());

        addMemberRow(me, true);
        loadGroups();
    }

    private void loadGroups() {
        groups.clear();
        groups.addAll(repo.getAllGroups());
        if (groups.isEmpty()) {
            String id = repo.createGroup("Default Group", me);
            groups.clear();
            groups.addAll(repo.getAllGroups());
            selectedGroupId = id;
        }

        List<String> names = new ArrayList<>();
        int selectedIndex = 0;
        for (int i = 0; i < groups.size(); i++) {
            WalletRepository.SplitGroup g = groups.get(i);
            names.add(g.name + (me.equals(g.creator) ? " (you)" : ""));
            if (selectedGroupId != null && selectedGroupId.equals(g.id)) {
                selectedIndex = i;
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGroups.setAdapter(adapter);
        spinnerGroups.setSelection(selectedIndex);

        spinnerGroups.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedGroupId = groups.get(position).id;
                updateGroupInfo();
                renderDebtsForSelectedGroup();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        updateGroupInfo();
        renderDebtsForSelectedGroup();
    }

    private void updateGroupInfo() {
        WalletRepository.SplitGroup g = getSelectedGroup();
        if (g == null) {
            textGroupInfo.setText("No group selected");
            btnDeleteGroup.setEnabled(false);
            return;
        }
        boolean isCreator = me.equals(g.creator);
        textGroupInfo.setText("Creator: " + g.creator + (isCreator ? " (you)" : ""));
        btnDeleteGroup.setEnabled(isCreator);
    }

    private WalletRepository.SplitGroup getSelectedGroup() {
        if (selectedGroupId == null) return null;
        for (WalletRepository.SplitGroup g : groups) {
            if (selectedGroupId.equals(g.id)) return g;
        }
        return null;
    }

    private void createGroup() {
        String name = editGroupName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Enter group name", Toast.LENGTH_SHORT).show();
            return;
        }
        String id = repo.createGroup(name, me);
        if (id == null) {
            Toast.makeText(this, "Unable to create group", Toast.LENGTH_SHORT).show();
            return;
        }
        selectedGroupId = id;
        editGroupName.setText("");
        Toast.makeText(this, "Group created", Toast.LENGTH_SHORT).show();
        loadGroups();
    }

    private void deleteSelectedGroup() {
        WalletRepository.SplitGroup g = getSelectedGroup();
        if (g == null) return;
        if (!me.equals(g.creator)) {
            Toast.makeText(this, "Only group creator can delete", Toast.LENGTH_SHORT).show();
            return;
        }
        if (repo.deleteGroup(g.id, me)) {
            selectedGroupId = null;
            Toast.makeText(this, "Group deleted", Toast.LENGTH_SHORT).show();
            loadGroups();
        } else {
            Toast.makeText(this, "Unable to delete group", Toast.LENGTH_SHORT).show();
        }
    }

    private void addUser() {
        String u = editAddUser.getText().toString().trim();
        if (u.isEmpty()) { Toast.makeText(this, "Enter user ID or phone", Toast.LENGTH_SHORT).show(); return; }
        addMemberRow(u, false);
        editAddUser.setText("");
    }

    private void addMemberRow(String name, boolean lockRemove) {
        for (MemberEntry e : members) {
            if (e.name.equalsIgnoreCase(name)) {
                Toast.makeText(this, "Member already added", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        View row = getLayoutInflater().inflate(R.layout.item_split_member, usersContainer, false);
        TextView memberName = row.findViewById(R.id.textViewMemberName);
        EditText paidInput = row.findViewById(R.id.editTextMemberPaid);
        Button removeButton = row.findViewById(R.id.buttonRemoveMember);

        memberName.setText(name + (name.equals(me) ? " (You)" : ""));
        paidInput.setText("0");

        MemberEntry entry = new MemberEntry(name, row, paidInput);
        members.add(entry);

        if (lockRemove) {
            removeButton.setEnabled(false);
            removeButton.setText("Owner");
        } else {
            removeButton.setOnClickListener(v -> {
                members.remove(entry);
                usersContainer.removeView(row);
            });
        }

        usersContainer.addView(row);
    }

    private void doSplit() {
        if (members.size() < 2) {
            Toast.makeText(this, "Add at least two members", Toast.LENGTH_SHORT).show();
            return;
        }

        String totalS = editTotalAmount.getText().toString().trim();
        if (totalS.isEmpty()) { Toast.makeText(this, "Enter total amount", Toast.LENGTH_SHORT).show(); return; }
        long total;
        try { total = Long.parseLong(totalS); } catch (NumberFormatException e) { Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show(); return; }

        WalletRepository.SplitGroup selectedGroup = getSelectedGroup();
        if (selectedGroup == null) { Toast.makeText(this, "Create/select a group first", Toast.LENGTH_SHORT).show(); return; }

        long paidSum = 0L;
        List<Long> paidAmounts = new ArrayList<>();
        for (MemberEntry entry : members) {
            String s = entry.paidInput.getText().toString().trim();
            long paid;
            try {
                paid = s.isEmpty() ? 0L : Long.parseLong(s);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid paid amount for " + entry.name, Toast.LENGTH_SHORT).show();
                return;
            }
            if (paid < 0) {
                Toast.makeText(this, "Paid amount cannot be negative", Toast.LENGTH_SHORT).show();
                return;
            }
            paidAmounts.add(paid);
            paidSum += paid;
        }

        if (paidSum != total) {
            Toast.makeText(this, "Sum of paid amounts (₹" + paidSum + ") must equal total expense (₹" + total + ")", Toast.LENGTH_LONG).show();
            return;
        }

        int n = members.size();
        long baseShare = total / n;
        long remainderShare = total % n;

        List<BalanceNode> creditors = new ArrayList<>();
        List<BalanceNode> debtors = new ArrayList<>();

        for (int i = 0; i < members.size(); i++) {
            long share = baseShare + (i < remainderShare ? 1 : 0);
            long balance = paidAmounts.get(i) - share;
            if (balance > 0) creditors.add(new BalanceNode(members.get(i).name, balance));
            else if (balance < 0) debtors.add(new BalanceNode(members.get(i).name, -balance));
        }

        Collections.sort(creditors, (a, b) -> Long.compare(b.amount, a.amount));
        Collections.sort(debtors, (a, b) -> Long.compare(b.amount, a.amount));

        repo.clearDebtsForGroup(selectedGroup.id);

        int i = 0, j = 0;
        while (i < debtors.size() && j < creditors.size()) {
            BalanceNode d = debtors.get(i);
            BalanceNode c = creditors.get(j);
            long amount = Math.min(d.amount, c.amount);

            repo.createDebt(c.name, d.name, amount, selectedGroup.id);

            d.amount -= amount;
            c.amount -= amount;

            if (d.amount == 0) i++;
            if (c.amount == 0) j++;
        }

        renderDebtsForSelectedGroup();
    }

    private void renderDebtsForSelectedGroup() {
        WalletRepository.SplitGroup selectedGroup = getSelectedGroup();
        resultContainer.removeAllViews();
        if (selectedGroup == null) return;

        TextView header = new TextView(this);
        header.setText("Group: " + selectedGroup.name + "\nFinal Settlement:");
        header.setPadding(6,6,6,6);
        resultContainer.addView(header);

        List<WalletRepository.Debt> debts = repo.getDebtsForGroup(selectedGroup.id);
        if (debts.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No split records yet in this group.");
            empty.setPadding(6,6,6,6);
            resultContainer.addView(empty);
            return;
        }

        for (WalletRepository.Debt d : debts) {
            addDebtRow(d);
        }
    }

    private void addDebtRow(WalletRepository.Debt debt) {
        View row = getLayoutInflater().inflate(R.layout.item_split_debt, resultContainer, false);
        TextView main = row.findViewById(R.id.textViewDebtMain);
        TextView st = row.findViewById(R.id.textViewDebtStatus);
        Button markPaid = row.findViewById(R.id.buttonMarkPaid);
        Button reminder = row.findViewById(R.id.buttonReminder);
        Button pay = row.findViewById(R.id.buttonPay);

        main.setText(debt.debtor + " should pay " + debt.creditor + " ₹" + debt.amount);
        setStatusView(st, debt.status);

        markPaid.setOnClickListener(v -> {
            if (repo.markDebtPaid(debt.id)) {
                setStatusView(st, "PAID");
                Toast.makeText(this, debt.debtor + " marked as paid", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Unable to mark paid", Toast.LENGTH_SHORT).show();
            }
        });

        reminder.setOnClickListener(v -> Toast.makeText(this, "Reminder sent to " + debt.debtor, Toast.LENGTH_SHORT).show());

        pay.setOnClickListener(v -> {
            Intent intent = new Intent(this, SendMoneyActivity.class);
            intent.putExtra("prefill_receiver", debt.creditor);
            intent.putExtra("prefill_amount", debt.amount);
            startActivity(intent);
        });

        resultContainer.addView(row);
    }

    private void setStatusView(TextView statusView, String status) {
        if ("PAID".equalsIgnoreCase(status)) {
            statusView.setText("PAID ✅");
        } else {
            statusView.setText("PENDING ❌");
        }
    }
}
