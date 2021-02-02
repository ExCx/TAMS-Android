package com.frekanstan.asset_management.view.assets;

import android.os.Handler;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.frekanstan.asset_management.R;
import com.frekanstan.asset_management.data.locations.ILocation;
import com.frekanstan.asset_management.view.MainActivityBase;
import com.frekanstan.asset_management.view.shared.ISearchableListFragment;
import com.google.android.material.chip.Chip;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.val;
import lombok.var;

import static com.frekanstan.asset_management.app.helpers.Helpers.areAllTrue;
import static com.frekanstan.asset_management.app.helpers.Helpers.setAllTrue;

public class AssetListFilterManagerBase {
    protected MainActivityBase mContext;
    protected ISearchableListFragment mFragment;
    protected LinearLayout mChipLayout;
    public final boolean[] mFilterLabelSelected = new boolean[]{true, true};
    public final boolean[] mFilterAssignmentSelected = new boolean[]{true, true};
    public final boolean[] mFilterCountingSelected = new boolean[]{true, true};
    public boolean[] mFilterStoragesSelected;
    private ArrayList<ILocation> mStorages;
    public BigDecimal mFilterPrice;
    public Integer mFilterPriceType = -1;

    public AssetListFilterManagerBase(MainActivityBase context, ISearchableListFragment fragment, ArrayList<ILocation> storages) {
        mContext = context;
        mFragment = fragment;

        //storage filter
        mFilterStoragesSelected = new boolean[storages.size()];
        mStorages = storages;
        for (int i = 0; i < storages.size(); i++)
            mFilterStoragesSelected[i] = true;
    }

    public void addFilterChips(LinearLayout chipLayout) {
        mChipLayout = chipLayout;
        if (!areAllTrue(mFilterLabelSelected))
            addFilterChip(mContext.getString(R.string.add_filter_is_labeled_title), chipLayout);
        if (!areAllTrue(mFilterAssignmentSelected))
            addFilterChip(mContext.getString(R.string.add_filter_assignment_title), chipLayout);
        if (!areAllTrue(mFilterStoragesSelected))
            addFilterChip(mContext.getString(R.string.add_filter_storage_title), chipLayout);
        if (!areAllTrue(mFilterCountingSelected))
            addFilterChip(mContext.getString(R.string.add_filter_counting_status_title), chipLayout);
        if (mFilterPriceType != -1)
            addFilterChip(mFilterPrice + mContext.getResources().getStringArray(R.array.price_filter_types)[mFilterPriceType], chipLayout);
    }

    public void removeFilters() {
        setAllTrue(mFilterLabelSelected);
        setAllTrue(mFilterAssignmentSelected);
        setAllTrue(mFilterStoragesSelected);
        setAllTrue(mFilterCountingSelected);
        mFilterPriceType = -1;
    }

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
            if (text.equals(mContext.getString(R.string.add_filter_is_labeled_title)))
                showFilterLabelDialog();
            else if (text.equals(mContext.getString(R.string.add_filter_assignment_title)))
                showFilterAssignmentDialog();
            else if (text.equals(mContext.getString(R.string.add_filter_storage_title)))
                showFilterStoragesDialog();
            else if (text.equals(mContext.getString(R.string.add_filter_counting_status_title)))
                showFilterCountingDialog();
            else if (text.contains("TL"))
                showFilterPriceDialog();
        });
        chip.setOnCloseIconClickListener(v -> {
            chipLayout.removeView(chip);
            if (text.equals(mContext.getString(R.string.add_filter_is_labeled_title)) && !areAllTrue(mFilterLabelSelected))
                setAllTrue(mFilterLabelSelected);
            else if (text.equals(mContext.getString(R.string.add_filter_assignment_title)) && !areAllTrue(mFilterAssignmentSelected))
                setAllTrue(mFilterAssignmentSelected);
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

    protected void showMultiChoiceFilterDialog(int optionsResId, boolean[] filterArray, int chipTitleResId) {
        String[] options = mContext.getResources().getStringArray(optionsResId);
        new AlertDialog.Builder(mContext).setTitle(R.string.add_filter_status_dialog_title)
                .setMultiChoiceItems(options, filterArray, (dialog, which, val) -> filterArray[which] = val)
                .setPositiveButton(mContext.getString(R.string.confirm), (dialog, id) -> {
                    addFilterChip(mContext.getString(chipTitleResId), mChipLayout);
                    mFragment.refreshList();
                })
                .setNegativeButton(mContext.getString(R.string.cancel_title), (dialog, id) -> {
                    System.arraycopy(filterArray, 0, filterArray, 0, 1);
                    dialog.dismiss();
                })
                .show();
    }

    public void showFilterLabelDialog() {
        showMultiChoiceFilterDialog(
                R.array.label_status,
                mFilterLabelSelected,
                R.string.add_filter_is_labeled_title);
    }

    public void showFilterAssignmentDialog() {
        showMultiChoiceFilterDialog(
                R.array.assignment_status,
                mFilterAssignmentSelected,
                R.string.add_filter_assignment_title);
    }

    public void showFilterCountingDialog() {
        showMultiChoiceFilterDialog(
                R.array.counting_status,
                mFilterCountingSelected,
                R.string.add_filter_counting_status_title);
    }

    public void showFilterStoragesDialog() {
        ArrayList<String> depoIsimleri = new ArrayList<>();
        for (ILocation depo : mStorages)
            depoIsimleri.add(depo.getName());
        String[] filterStoragesOptions = depoIsimleri.toArray(new String[0]);
        final boolean[] tempFilterStoragesSelected = mFilterStoragesSelected;
        new AlertDialog.Builder(mContext)
                .setMultiChoiceItems(filterStoragesOptions, mFilterStoragesSelected, (dialog, which, val) -> mFilterStoragesSelected[which] = val)
                .setPositiveButton(mContext.getString(R.string.confirm), (dialog, id) -> {
                    addFilterChip(mContext.getString(R.string.add_filter_storage_title), mChipLayout);
                    mFragment.refreshList();
                })
                .setNegativeButton(mContext.getString(R.string.cancel_title), (dialog, id) -> {
                    mFilterStoragesSelected = tempFilterStoragesSelected;
                    dialog.dismiss();
                })
                .show();
    }

    public void showFilterPriceDialog() {
        LinearLayout ll = new LinearLayout(mContext);
        final EditText priceText = new EditText(mContext);
        priceText.setWidth(100);
        priceText.setGravity(Gravity.END);
        priceText.setInputType(InputType.TYPE_CLASS_NUMBER);
        InputFilter[] filterArray = new InputFilter[1];
        filterArray[0] = new InputFilter.LengthFilter(6);
        priceText.setFilters(filterArray);
        final Spinner priceFilterType = new Spinner(mContext);
        List<String> priceFilterTypes = Arrays.asList(mContext.getResources().getStringArray(R.array.price_filter_types));
        ArrayAdapter<String> priceFilterAdapter = new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_item, priceFilterTypes);
        priceFilterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        priceFilterType.setAdapter(priceFilterAdapter);
        ll.addView(priceText);
        ll.addView(priceFilterType);
        new AlertDialog.Builder(mContext)
                .setView(ll)
                .setPositiveButton(mContext.getString(R.string.confirm), (dialog, id) -> {
                    mFilterPrice = new BigDecimal(priceText.getText().toString());
                    mFilterPriceType = priceFilterType.getSelectedItemPosition() + 1;
                    addFilterChip(priceText.getText().toString() + priceFilterType.getSelectedItem().toString(), mChipLayout);
                    mFragment.refreshList();
                })
                .setNegativeButton(mContext.getString(R.string.cancel_title), (dialog, id) -> dialog.dismiss())
                .show();
    }
}
