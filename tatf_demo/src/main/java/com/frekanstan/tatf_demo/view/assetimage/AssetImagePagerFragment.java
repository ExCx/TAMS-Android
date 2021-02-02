package com.frekanstan.tatf_demo.view.assetimage;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.Navigation;
import androidx.viewpager.widget.ViewPager;

import com.frekanstan.asset_management.app.helpers.PictureTaker;
import com.frekanstan.asset_management.app.shared.SmartFragmentStatePagerAdapter;
import com.frekanstan.asset_management.data.ObjectBox;
import com.frekanstan.tatf_demo.R;
import com.frekanstan.tatf_demo.data.ImageToUpload;
import com.frekanstan.tatf_demo.view.MainActivity;

import java.io.File;

import lombok.val;

import static android.app.Activity.RESULT_OK;
import static com.frekanstan.tatf_demo.view.MainActivity.assetPhotosFolder;

public class AssetImagePagerFragment extends Fragment {

    public static class AssetImagePagerAdapter extends SmartFragmentStatePagerAdapter {

        private String mAssetCode;

        AssetImagePagerAdapter(FragmentManager fm, String assetCode) {
            super(fm);
            mAssetCode = assetCode;
        }

        @Override
        public int getCount() {
            File[] files = assetPhotosFolder.listFiles();
            int count = 0;
            if (files != null) {
                for (File f : files) {
                    if(f.getName().contains(mAssetCode))
                        count++;
                }
            }
            return count;
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return AssetImageFragment.newInstance(mAssetCode, position);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return (position + 1) + "/" + getCount();
        }
    }

    private MainActivity mContext;
    private String mAssetCode, mAssetImagePath;

    private AssetImagePagerAdapter mAdapter;
    private ViewPager mPager;

    public AssetImagePagerFragment() { }

    public static AssetImagePagerFragment newInstance() {
        return new AssetImagePagerFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity){
            mContext = (MainActivity) context;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mAssetCode = getArguments().getString("mAssetCode");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mContext.showHideFooter(false);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.asset_image_pager, container, false);
        //setRetainInstance(true);
        setHasOptionsMenu(true);
        mPager = view.findViewById(R.id.asset_image_view_pager);
        mAdapter = new AssetImagePagerAdapter(getChildFragmentManager(), mAssetCode);
        mPager.setAdapter(mAdapter);
        mContext.getSupportActionBar().setTitle(mAdapter.getPageTitle(0));
        ViewPager.OnPageChangeListener mPageChangeListener = new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrollStateChanged(int arg0) { }
            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) { }
            @Override
            public void onPageSelected(int pos) {
                mContext.getSupportActionBar().setTitle(mAdapter.getPageTitle(pos));
            }
        };
        mPager.addOnPageChangeListener(mPageChangeListener);
        return view;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.add_picture) {
            takePhoto();
            return true;
        } else if (itemId == R.id.delete_picture) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setMessage(R.string.alert_query_deleting_photo)
                    .setPositiveButton(R.string.yes, (dialog, id) -> deleteCurrentPhoto())
                    .setNegativeButton(R.string.no, (dialog, id) -> dialog.dismiss())
                    .show();
            return true;
        } else if (itemId == R.id.main_picture) {
            if (mPager.getCurrentItem() == 0)
                Toast.makeText(getActivity(), R.string.already_main_picture, Toast.LENGTH_SHORT).show();
            else
                makePhotoMain();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void takePhoto() {
        mAssetImagePath = assetPhotosFolder + File.separator +
                mAssetCode + "-" + mAdapter.getCount() + ".jpg";
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        File file = new File(mAssetImagePath);
        Uri outputFileUri = Uri.fromFile(file);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
        startActivityForResult(cameraIntent, 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            PictureTaker.saveBitmapToFile(new File(mAssetImagePath));
            ObjectBox.get().boxFor(ImageToUpload.class).put(new ImageToUpload(mAssetImagePath, "asset", false, false));
            mAdapter.notifyDataSetChanged();
            mPager.setAdapter(mAdapter);
            mPager.setCurrentItem(mAdapter.getCount());
        }
    }

    private void deleteCurrentPhoto() {
        int currIndex = mPager.getCurrentItem();
        mAssetImagePath = assetPhotosFolder +
                File.separator + mAssetCode + "-" + currIndex + ".jpg";
        File file = new File(mAssetImagePath);
        if(file.delete())
            Toast.makeText(getActivity(), R.string.picture_deleted, Toast.LENGTH_SHORT).show();
        else {
            Toast.makeText(getActivity(), R.string.cannot_delete_picture, Toast.LENGTH_SHORT).show();
            return;
        }
        ObjectBox.get().boxFor(ImageToUpload.class)
                .put(new ImageToUpload(mAssetImagePath, "asset", true, false));
        File photo;
        int photoIndex = currIndex + 1;
        while (true) {
            String path = assetPhotosFolder + File.separator + mAssetCode + "-" + photoIndex + ".jpg";
            photo = new File(path);
            if (photo.exists()) {
                String newPath = assetPhotosFolder + File.separator + mAssetCode + "-" + (photoIndex - 1) + ".jpg";
                //noinspection ResultOfMethodCallIgnored
                photo.renameTo(new File(newPath));
                ObjectBox.get().boxFor(ImageToUpload.class)
                        .put(new ImageToUpload(path, "asset", true, false));
                ObjectBox.get().boxFor(ImageToUpload.class)
                        .put(new ImageToUpload(newPath, "asset", false, false));
            }
            else
                break;
            photoIndex++;
        }
        if (!new File(assetPhotosFolder + File.separator + mAssetCode + "-0.jpg").exists()) {
            Navigation.findNavController(mContext, R.id.nav_host_fragment).popBackStack();
            takePhoto();
        } else {
            mAdapter.notifyDataSetChanged();
            mPager.setAdapter(mAdapter);
            if (currIndex == 0)
                mPager.setCurrentItem(currIndex);
            else
                mPager.setCurrentItem(currIndex - 1);
        }
    }

    private void makePhotoMain() {
        int currIndex = mPager.getCurrentItem();
        val oldMainPhoto = new File(assetPhotosFolder + File.separator + mAssetCode + "-0.jpg");
        val newMainPhoto = new File(assetPhotosFolder + File.separator + mAssetCode + "-" + currIndex + ".jpg");
        val tempPhoto = new File(assetPhotosFolder + File.separator + mAssetCode + "-.jpg");
        if (oldMainPhoto.renameTo(tempPhoto))
            Log.d("rename", "success");
        if (newMainPhoto.renameTo(oldMainPhoto))
            Log.d("rename", "success");
        if (tempPhoto.renameTo(newMainPhoto))
            Log.d("rename", "success");
        mAdapter.notifyDataSetChanged();
        mPager.setAdapter(mAdapter);
        mPager.setCurrentItem(0);
        ObjectBox.get().boxFor(ImageToUpload.class)
                .put(new ImageToUpload(assetPhotosFolder + File.separator + mAssetCode + "-" + currIndex + ".jpg", "asset", false, true));
        Toast.makeText(getActivity(), R.string.main_picture_selected, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mPager = null;
        mAdapter = null;
        mContext.changeTitle("");
    }
}