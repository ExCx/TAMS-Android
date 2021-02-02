package com.frekanstan.tatf_demo.view.assetimage;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.frekanstan.tatf_demo.R;
import com.frekanstan.tatf_demo.view.MainActivity;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;

public class AssetImageFragment extends Fragment
{
    private MainActivity mContext;
    private String mAssetCode, mAssetImagePath;
    private ImageView mImageView;
    private int mPosition;

    public AssetImageFragment() { }

    public static AssetImageFragment newInstance(String assetCode, int position) {
        AssetImageFragment fragment = new AssetImageFragment();
        Bundle args = new Bundle();
        args.putString("mAssetCode", assetCode);
        args.putInt("mPosition", position);
        fragment.setArguments(args);
        return fragment;
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
            mPosition = getArguments().getInt("mPosition");
        }
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.asset_image_fragment, container, false);
        mImageView = view.findViewById(R.id.assetImageBig);
        mAssetImagePath = MainActivity.assetPhotosFolder + File.separator + mAssetCode + "-" + mPosition + ".jpg";
        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_image, menu);
        if (mPosition == 0)
            menu.findItem(R.id.main_picture).setIcon(R.drawable.ic_star_yellow);
        else
            menu.findItem(R.id.main_picture).setIcon(R.drawable.ic_star_border_white);
    }

    @Override
    public void onResume() {
        super.onResume();
        mContext.actionButton.hide();
        mContext.showHideFooter(false);
        File imgFile = new File(mAssetImagePath);
        if (imgFile.exists())
            Picasso.get().load(imgFile).memoryPolicy(MemoryPolicy.NO_STORE,
                    MemoryPolicy.NO_CACHE).resize(mContext.width, 0).into(mImageView);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
