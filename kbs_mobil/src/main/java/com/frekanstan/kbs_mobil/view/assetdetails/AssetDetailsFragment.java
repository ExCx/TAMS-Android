package com.frekanstan.kbs_mobil.view.assetdetails;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.frekanstan.asset_management.app.helpers.PictureTaker;
import com.frekanstan.asset_management.data.ObjectBox;
import com.frekanstan.kbs_mobil.R;
import com.frekanstan.kbs_mobil.app.assets.AssetDAO;
import com.frekanstan.kbs_mobil.app.assets.BrandDAO;
import com.frekanstan.kbs_mobil.app.assets.ModelDAO;
import com.frekanstan.kbs_mobil.app.labeling.LabelPrinter;
import com.frekanstan.kbs_mobil.data.Asset;
import com.frekanstan.kbs_mobil.data.Brand;
import com.frekanstan.kbs_mobil.data.Brand_;
import com.frekanstan.kbs_mobil.data.ImageToUpload;
import com.frekanstan.kbs_mobil.data.ImageToUpload_;
import com.frekanstan.kbs_mobil.data.Model;
import com.frekanstan.kbs_mobil.data.Model_;
import com.frekanstan.kbs_mobil.databinding.AssetDetailsFragmentBinding;
import com.frekanstan.kbs_mobil.view.MainActivity;
import com.google.android.material.tabs.TabLayout;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

import lombok.val;
import lombok.var;

import static android.app.Activity.RESULT_OK;

public class AssetDetailsFragment extends Fragment implements ViewPager.OnPageChangeListener, View.OnClickListener {
    private MainActivity context;
    private Asset asset;
    private TabLayout.OnTabSelectedListener mOnTabSelectedListener;
    private String mAssetImagePath;
    private AssetDetailsTabPagerAdapter mAdapter;
    private AssetDetailsFragmentBinding view;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity){
            this.context = (MainActivity) context;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            val assetId = getArguments().getLong("assetId");
            asset = AssetDAO.getDao().get(assetId);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = AssetDetailsFragmentBinding.inflate(inflater, container, false);
        initializeObjects();
        setListeners();
        populateInfo();
        setHasOptionsMenu(true);
        return view.getRoot();
    }

    private void initializeObjects() {
        mAssetImagePath = MainActivity.assetPhotosFolder + File.separator + asset.getAssetCode() + "-0.jpg";
        mAdapter = new AssetDetailsTabPagerAdapter(getChildFragmentManager(), asset);
        view.pager.setAdapter(mAdapter);
        view.pager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(view.tabLayout));
        view.tabLayout.setupWithViewPagerAndKeepIcons(view.pager);
    }

    private void setListeners() {
        mOnTabSelectedListener = new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                view.pager.setCurrentItem(tab.getPosition());
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        };
        view.tabLayout.addOnTabSelectedListener(mOnTabSelectedListener);

        view.assetImageSmall.setOnClickListener(v -> {
            if (v.equals(view.assetImageSmall)) {
                File imgFile = new File(mAssetImagePath);
                if (imgFile.exists()) {
                    var bundle = new Bundle();
                    bundle.putString("mAssetCode", asset.getAssetCode());
                    context.nav.navigate(R.id.action_assetDetailsFragment_to_assetImagePagerFragment, bundle);
                }
                else
                    PictureTaker.dispatchTakePictureIntent(imgFile, this);
            }
        });
        view.serialNoB.setOnClickListener(this);
        view.brandB.setOnClickListener(this);
        view.modelB.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        val dao = AssetDAO.getDao();
        if (v.getId() == R.id.serialNoB) {
            View viewInflated = LayoutInflater.from(context).inflate(R.layout.single_input_dialog, (ViewGroup) getView(), false);
            final EditText input = viewInflated.findViewById(R.id.input_text);
            input.setFilters(new InputFilter[]{new InputFilter.AllCaps()});
            input.setText(asset.getSerialNo());
            val builder = new AlertDialog.Builder(context);
            builder.setView(viewInflated).setTitle(R.string.serial_no)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        dialog.dismiss();
                        val serialNo = input.getText().toString();
                        view.assetDetailsSerialNo.setText(serialNo);
                        asset.setSerialNo(serialNo);
                        asset.setIsUpdated(true);
                        dao.put(asset);
                    });
            builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                    .show();
        } else if (v.getId() == R.id.brandB || v.getId() == R.id.modelB) {
            View viewInflated = LayoutInflater.from(context).inflate(R.layout.single_dropdown_dialog, (ViewGroup) getView(), false);
            final Spinner dropdown = viewInflated.findViewById(R.id.dropdown);
            if (v.getId() == R.id.brandB) {
                val ddAdapter = new ArrayAdapter<>(
                        context,
                        android.R.layout.simple_spinner_item,
                        new ArrayList<>(BrandDAO.getDao().getBox().query().equal(Brand_.parentTypeId, asset.getAssetTypeId()).build().find()));
                //adapter.insert(new Brand(0, null, mContext.getString(R.string.institution_name), true), 0);
                ddAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                dropdown.setAdapter(ddAdapter);
                if (asset.getBrandNameId() != 0) {
                    val brand = BrandDAO.getDao().get(asset.getBrandNameId());
                    dropdown.setSelection(ddAdapter.getPosition(brand));
                }
            } else if (v.getId() == R.id.modelB) {
                if (asset.getBrandNameId() == 0) {
                    Toast.makeText(context, context.getString(R.string.no_brand_selected), Toast.LENGTH_LONG).show();
                    return;
                }
                val ddAdapter = new ArrayAdapter<Model>(
                        context,
                        android.R.layout.simple_spinner_item,
                        new ArrayList<>(ModelDAO.getDao().getBox().query().equal(Model_.parentBrandId, asset.getBrandNameId()).build().find()));
                //adapter.insert(new Brand(0, null, mContext.getString(R.string.institution_name), true), 0);
                ddAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                dropdown.setAdapter(ddAdapter);
                if (asset.getModelNameId() != 0) {
                    val model = ModelDAO.getDao().get(asset.getModelNameId());
                    dropdown.setSelection(ddAdapter.getPosition(model));
                }
            }
            val builder = new AlertDialog.Builder(context);
            if (v.getId() == R.id.brandB)
                builder.setTitle(R.string.brand).setView(viewInflated)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            dialog.dismiss();
                            val brand = (Brand) dropdown.getSelectedItem();
                            if (!brand.getDefinition().equals(asset.getBrandNameDefinition())) {
                                view.assetDetailsBrand.setText(brand.getDefinition());
                                asset.setBrandNameId(brand.getId());
                                asset.setBrandNameDefinition(brand.getDefinition());
                                //empty model
                                view.assetDetailsModel.setText("");
                                asset.setModelNameId(0);
                                asset.setModelNameDefinition(null);
                                asset.setIsUpdated(true);
                                dao.put(asset);
                            }
                        });
            else if (v.getId() == R.id.modelB)
                builder.setTitle(R.string.model).setView(viewInflated)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            dialog.dismiss();
                            val model = (Model) dropdown.getSelectedItem();
                            if (!model.getDefinition().equals(asset.getModelNameDefinition())) {
                                view.assetDetailsModel.setText(model.getDefinition());
                                asset.setModelNameId(model.getId());
                                asset.setModelNameDefinition(model.getDefinition());
                                asset.setIsUpdated(true);
                                dao.put(asset);
                            }
                        });
            builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

    @Override
    public void onPageSelected (int position) {
        switch (position){
            case 0 : ((AssetDetailsGeneralFragment)mAdapter.getItem(position)).populateInfo();
            case 1 : ((AssetDetailsAssignmentFragment)mAdapter.getItem(position)).populateInfo();
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) { }

    private void populateInfo() {
        showMainImage();
        view.assetDetailsBrand.setText(asset.getBrandNameDefinition());
        view.assetDetailsModel.setText(asset.getModelNameDefinition());
        view.assetDetailsSerialNo.setText(asset.getSerialNo());
        view.assetDetailsPrice.setText(String.format(context.getLocale(), "%1$.2f\u20BA", asset.getPrice()));
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.asset_actions, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.print) {
            ArrayList<Long> idList = new ArrayList<>();
            idList.add(asset.getId());
            if (asset.getLabelingDateTime() != null) {
                new AlertDialog.Builder(context).setMessage(R.string.scanned_label_already_printed_single)
                        .setPositiveButton(context.getString(R.string.confirm), (dialog, id) ->
                                new LabelPrinter(context, idList, "asset", null).print())
                        .setNegativeButton(context.getString(R.string.cancel_title), (dialog, id) ->
                                dialog.dismiss())
                        .show();
            } else
                new LabelPrinter(context, idList, "asset", null).print();
            return true;
        } else if (itemId == R.id.assign) {
            var bundle = new Bundle();
            bundle.putLongArray("assetIds", new long[]{asset.getId()});
            bundle.putLong("personId", asset.getAssignedPersonId());
            bundle.putLong("locationId", asset.getAssignedLocationId());
            context.nav.navigate(R.id.action_assetDetailsFragment_to_assignmentDialogFragment, bundle);
            return true;
        } else if (itemId == R.id.find) {
            if (context.controlRfid()) {
                context.rfidManager.setTargetTag(asset.getRfidCode());
                context.nav.navigate(R.id.action_assetDetailsFragment_to_tagFinderDialogFragment);
            }
            else
                context.nav.navigate(R.id.settingsFragment);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        view.tabLayout.removeOnTabSelectedListener(mOnTabSelectedListener);
        view.assetImageSmall.setImageBitmap(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        context.changeTitle(asset.getRemoteId());
        context.actionButton.hide();
        context.showHideFooter(false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PictureTaker.REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val box = ObjectBox.get().boxFor(ImageToUpload.class);
            val itu = box.query().equal(ImageToUpload_.path, mAssetImagePath).build().findFirst();
            if (itu == null)
                box.put(new ImageToUpload(mAssetImagePath, "asset", false, false));
            showMainImage();
        }
    }

    private void showMainImage() {
        mAssetImagePath = MainActivity.assetPhotosFolder + File.separator + asset.getAssetCode() + "-0.jpg";
        File imgFile = new File(mAssetImagePath);
        if (imgFile.exists()) {
            view.assetImageSmall.setPadding(0, 0, 0, 0);
            view.assetImageSmall.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Picasso.get().load(imgFile).memoryPolicy(MemoryPolicy.NO_STORE,
                    MemoryPolicy.NO_CACHE).resize(context.width / 2, 0).into(view.assetImageSmall);
        }
    }
}