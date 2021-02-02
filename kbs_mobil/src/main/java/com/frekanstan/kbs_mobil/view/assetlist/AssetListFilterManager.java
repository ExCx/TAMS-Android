package com.frekanstan.kbs_mobil.view.assetlist;

import android.os.Handler;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.frekanstan.asset_management.view.MainActivityBase;
import com.frekanstan.asset_management.view.assets.AssetListFilterManagerBase;
import com.frekanstan.asset_management.view.shared.ISearchableListFragment;
import com.frekanstan.kbs_mobil.R;
import com.frekanstan.kbs_mobil.app.acquisition.BudgetDAO;
import com.frekanstan.kbs_mobil.app.locations.LocationRepository;
import com.frekanstan.kbs_mobil.data.Budget_;
import com.google.android.material.chip.Chip;

import lombok.val;
import lombok.var;

import static com.frekanstan.asset_management.app.helpers.Helpers.areAllTrue;
import static com.frekanstan.asset_management.app.helpers.Helpers.setAllTrue;

public class AssetListFilterManager extends AssetListFilterManagerBase {
    final boolean[] mFilterTagNoSelected = new boolean[]{true, true};
    final boolean[] mFilterWSSelected = new boolean[]{true, true, true, true};
    boolean[] mFilterBudgetSelected;
    final boolean[] mFilterDeploymentSelected = new boolean[]{true, true};

    AssetListFilterManager(MainActivityBase context, ISearchableListFragment fragment) {
        super(context, fragment, LocationRepository.getAllStorages());
        var budgets = BudgetDAO.getDao().getAll();
        mFilterBudgetSelected = new boolean[budgets.size()];
        for (int i = 0; i < budgets.size(); i++)
            mFilterBudgetSelected[i] = true;
    }

    @Override
    public void addFilterChips(LinearLayout chipLayout) {
        super.addFilterChips(chipLayout);
        if (!areAllTrue(mFilterTagNoSelected))
            addFilterChip(mContext.getString(R.string.add_filter_tag_no_title), chipLayout);
        if (!areAllTrue(mFilterDeploymentSelected))
            addFilterChip(mContext.getString(R.string.add_filter_deployment_title), chipLayout);
        if (!areAllTrue(mFilterBudgetSelected))
            addFilterChip(mContext.getString(R.string.add_filter_budget_type_title), chipLayout);
        if (!areAllTrue(mFilterWSSelected))
            addFilterChip(mContext.getString(R.string.add_filter_working_status_title), chipLayout);
    }

    @Override
    public void removeFilters() {
        super.removeFilters();
        setAllTrue(mFilterTagNoSelected);
        setAllTrue(mFilterDeploymentSelected);
        setAllTrue(mFilterBudgetSelected);
        setAllTrue(mFilterWSSelected);
    }

    @Override
    protected void addFilterChip(final String text, LinearLayout chipLayout) {
        //4 filtreden fazla eklenmez
        if (chipLayout.getChildCount() == 4) {
            Toast.makeText(mContext, mContext.getString(R.string.filter_limit_reached), Toast.LENGTH_SHORT).show();
            return;
        }
        //filtreler tümünü kapsayacak şekilde onaylanmışsa chip'i kaldır
        else if (text.equals(mContext.getString(R.string.add_filter_is_labeled_title)) && areAllTrue(mFilterLabelSelected)) {
            for (int i = 0; i < chipLayout.getChildCount(); i++) {
                if (((Chip) chipLayout.getChildAt(i)).getText().equals(text))
                    chipLayout.removeViewAt(i);
            }
            return;
        }
        else if (text.equals(mContext.getString(R.string.add_filter_assignment_title)) && areAllTrue(mFilterAssignmentSelected)) {
            for (int i = 0; i < chipLayout.getChildCount(); i++) {
                if (((Chip) chipLayout.getChildAt(i)).getText().equals(text))
                    chipLayout.removeViewAt(i);
            }
            return;
        }
        else if (text.equals(mContext.getString(R.string.add_filter_deployment_title)) && areAllTrue(mFilterDeploymentSelected)) {
            for (int i = 0; i < chipLayout.getChildCount(); i++) {
                if (((Chip) chipLayout.getChildAt(i)).getText().equals(text))
                    chipLayout.removeViewAt(i);
            }
            return;
        }
        else if (text.equals(mContext.getString(R.string.add_filter_tag_no_title)) && areAllTrue(mFilterTagNoSelected)) {
            for (int i = 0; i < chipLayout.getChildCount(); i++) {
                if (((Chip) chipLayout.getChildAt(i)).getText().equals(text))
                    chipLayout.removeViewAt(i);
            }
            return;
        }
        else if (text.equals(mContext.getString(R.string.add_filter_storage_title)) && areAllTrue(mFilterStoragesSelected)) {
            for (int i = 0; i < chipLayout.getChildCount(); i++) {
                if (((Chip) chipLayout.getChildAt(i)).getText().equals(text))
                    chipLayout.removeViewAt(i);
            }
        }
        else if (text.equals(mContext.getString(R.string.add_filter_counting_status_title)) && areAllTrue(mFilterCountingSelected)) {
            for (int i = 0; i < chipLayout.getChildCount(); i++) {
                if (((Chip) chipLayout.getChildAt(i)).getText().equals(text))
                    chipLayout.removeViewAt(i);
            }
        }
        else if (text.equals(mContext.getString(R.string.add_filter_budget_type_title)) && areAllTrue(mFilterBudgetSelected)) {
            for (int i = 0; i < chipLayout.getChildCount(); i++) {
                if (((Chip) chipLayout.getChildAt(i)).getText().equals(text))
                    chipLayout.removeViewAt(i);
            }
        }
        else if (text.equals(mContext.getString(R.string.add_filter_working_status_title)) && areAllTrue(mFilterWSSelected)) {
            for (int i = 0; i < chipLayout.getChildCount(); i++) {
                if (((Chip) chipLayout.getChildAt(i)).getText().equals(text))
                    chipLayout.removeViewAt(i);
            }
            return;
        }
        else if (text.equals(mContext.getString(R.string.add_filter_storage_title)) && areAllTrue(mFilterStoragesSelected)) {
            for (int i = 0; i < chipLayout.getChildCount(); i++) {
                if (((Chip) chipLayout.getChildAt(i)).getText().equals(text))
                    chipLayout.removeViewAt(i);
            }
        }
        //fiyatsa eski fiyatlıyı kaldır
        else if (text.contains("TL")) {
            for (int i = 0; i < chipLayout.getChildCount(); i++) {
                if (((Chip) chipLayout.getChildAt(i)).getText().toString().contains("TL"))
                    chipLayout.removeViewAt(i);
            }
        }
        for (int i = 0; i < chipLayout.getChildCount(); i++) {
            if(((Chip) chipLayout.getChildAt(i)).getText().equals(text))
                return;
        }

        final Chip chip = new Chip(mContext);
        chip.setText(text);
        chip.setCloseIconVisible(true);
        chip.setOnClickListener(v -> {
            if (text.equals(mContext.getString(R.string.add_filter_tag_no_title)))
                showFilterTagNoDialog();
            else if (text.equals(mContext.getString(R.string.add_filter_is_labeled_title)))
                showFilterLabelDialog();
            else if (text.equals(mContext.getString(R.string.add_filter_assignment_title)))
                showFilterAssignmentDialog();
            else if (text.equals(mContext.getString(R.string.add_filter_budget_type_title)))
                showFilterBudgetTypeDialog();
            else if (text.equals(mContext.getString(R.string.add_filter_working_status_title)))
                showFilterWSDialog();
            else if (text.equals(mContext.getString(R.string.add_filter_storage_title)))
                showFilterStoragesDialog();
            else if (text.equals(mContext.getString(R.string.add_filter_counting_status_title)))
                showFilterCountingDialog();
            else if (text.contains("TL"))
                showFilterPriceDialog();
        });
        chip.setOnCloseIconClickListener(v -> {
            chipLayout.removeView(chip);
            if (text.equals(mContext.getString(R.string.add_filter_tag_no_title)) && !areAllTrue(mFilterTagNoSelected))
                setAllTrue(mFilterTagNoSelected);
            else if (text.equals(mContext.getString(R.string.add_filter_is_labeled_title)) && !areAllTrue(mFilterLabelSelected))
                setAllTrue(mFilterLabelSelected);
            else if (text.equals(mContext.getString(R.string.add_filter_assignment_title)) && !areAllTrue(mFilterAssignmentSelected))
                setAllTrue(mFilterAssignmentSelected);
            else if (text.equals(mContext.getString(R.string.add_filter_budget_type_title)) && !areAllTrue(mFilterBudgetSelected))
                setAllTrue(mFilterBudgetSelected);
            else if (text.equals(mContext.getString(R.string.add_filter_working_status_title)) && !areAllTrue(mFilterWSSelected))
                setAllTrue(mFilterWSSelected);
            else if (text.equals(mContext.getString(R.string.add_filter_storage_title)))
                setAllTrue(mFilterStoragesSelected);
            else if (text.equals(mContext.getString(R.string.add_filter_counting_status_title)))
                setAllTrue(mFilterCountingSelected);
            else if (text.contains("TL"))
                mFilterPriceType = -1;
            if (chipLayout.getChildCount() == 0) {
                var lp = (RelativeLayout.LayoutParams) chipLayout.getLayoutParams();
                lp.setMargins(0, 0, 0, 0);
                chipLayout.setLayoutParams(lp);
            }
            mFragment.refreshList();
        });
        chipLayout.addView(chip);
        var lp = (RelativeLayout.LayoutParams) chipLayout.getLayoutParams();
        lp.setMargins(4, 4, 4, 4);
        chipLayout.setLayoutParams(lp);
        val handler = new Handler();
        handler.postDelayed(chip::requestLayout, 250);
    }

    void showFilterTagNoDialog() {
        showMultiChoiceFilterDialog(
                R.array.tag_no_status,
                mFilterTagNoSelected,
                R.string.add_filter_tag_no_title);
    }

    void showFilterDeploymentDialog() {
        showMultiChoiceFilterDialog(
                R.array.deployment_status,
                mFilterDeploymentSelected,
                R.string.add_filter_deployment_title);
    }

    public void showFilterBudgetTypeDialog() {
        String[] filterBudgetOptions = BudgetDAO.getDao().getBox().query().build().property(Budget_.definition).findStrings();
        final boolean[] tempFilterBudgetsSelected = mFilterBudgetSelected;
        new AlertDialog.Builder(mContext)
                .setMultiChoiceItems(filterBudgetOptions, mFilterBudgetSelected, (dialog, which, val) -> mFilterBudgetSelected[which] = val)
                .setPositiveButton(mContext.getString(com.frekanstan.asset_management.R.string.confirm), (dialog, id) -> {
                    addFilterChip(mContext.getString(com.frekanstan.asset_management.R.string.add_filter_budget_type_title), mChipLayout);
                    mFragment.refreshList();
                })
                .setNegativeButton(mContext.getString(com.frekanstan.asset_management.R.string.cancel_title), (dialog, id) -> {
                    mFilterBudgetSelected = tempFilterBudgetsSelected;
                    dialog.dismiss();
                })
                .show();
    }

    void showFilterWSDialog() {
        showMultiChoiceFilterDialog(
                R.array.working_status,
                mFilterWSSelected,
                R.string.add_filter_working_status_title);
    }
}
