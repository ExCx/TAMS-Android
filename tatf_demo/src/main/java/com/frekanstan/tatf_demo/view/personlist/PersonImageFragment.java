package com.frekanstan.tatf_demo.view.personlist;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.frekanstan.asset_management.app.helpers.PictureTaker;
import com.frekanstan.asset_management.data.ObjectBox;
import com.frekanstan.tatf_demo.R;
import com.frekanstan.tatf_demo.data.ImageToUpload;
import com.frekanstan.tatf_demo.view.MainActivity;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;

import static android.app.Activity.RESULT_OK;
import static com.frekanstan.tatf_demo.view.MainActivity.personPhotosFolder;

public class PersonImageFragment extends Fragment {

    private String mPersonId, mImagePath;
    private ImageView mImageView;
    private MainActivity mContext;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity)
            mContext = (MainActivity) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mPersonId = getArguments().getString("personId");
        }
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.person_image_fragment, container, false);
        mImageView = view.findViewById(R.id.assetImageBig);
        mImagePath = personPhotosFolder + File.separator + mPersonId + ".jpg";
        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_person_image, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.delete_picture) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setMessage(R.string.alert_query_deleting_photo)
                    .setPositiveButton(R.string.yes, (dialog, id) -> deleteCurrentPhoto())
                    .setNegativeButton(R.string.no, (dialog, id) -> dialog.dismiss())
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void takePhoto() {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        File file = new File(mImagePath);
        Uri outputFileUri = Uri.fromFile(file);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
        startActivityForResult(cameraIntent, 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            PictureTaker.saveBitmapToFile(new File(mImagePath));
            ObjectBox.get().boxFor(ImageToUpload.class)
                    .put(new ImageToUpload(mImagePath, "person", false, false));
        }
    }

    private void deleteCurrentPhoto() {
        File file = new File(mImagePath);
        if(file.delete())
            Toast.makeText(getActivity(), R.string.picture_deleted, Toast.LENGTH_SHORT).show();
        else {
            Toast.makeText(getActivity(), R.string.cannot_delete_picture, Toast.LENGTH_SHORT).show();
            return;
        }
        ObjectBox.get().boxFor(ImageToUpload.class).put(new ImageToUpload(mImagePath, "person", true, false));
        mContext.nav.popBackStack();
        takePhoto();
    }

    @Override
    public void onResume() {
        super.onResume();
        mContext.actionButton.hide();
        mContext.showHideFooter(false);
        File imgFile = new File(mImagePath);
        if (imgFile.exists())
            Picasso.get().load(imgFile).memoryPolicy(MemoryPolicy.NO_STORE,
                    MemoryPolicy.NO_CACHE).resize(mContext.width, 0).into(mImageView);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext.changeTitle("");
    }
}
