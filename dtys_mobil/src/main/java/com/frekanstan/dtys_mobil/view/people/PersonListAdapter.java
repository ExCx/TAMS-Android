package com.frekanstan.dtys_mobil.view.people;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.daimajia.numberprogressbar.NumberProgressBar;
import com.frekanstan.asset_management.data.people.EPersonType;
import com.frekanstan.asset_management.view.shared.ICanShootPhoto;
import com.frekanstan.asset_management.view.widgets.CircularTextView;
import com.frekanstan.dtys_mobil.R;
import com.frekanstan.dtys_mobil.app.assets.AssetDAO;
import com.frekanstan.dtys_mobil.app.labeling.LabelPrinter;
import com.frekanstan.dtys_mobil.app.people.PersonDAO;
import com.frekanstan.dtys_mobil.data.Person;
import com.frekanstan.dtys_mobil.view.MainActivity;
import com.frekanstan.dtys_mobil.view.labeling.LabelingTabsFragment;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import lombok.val;
import lombok.var;

import static com.frekanstan.asset_management.view.MainActivityBase.personPhotosFolder;

public class PersonListAdapter extends PagedListAdapter<Person, PersonListAdapter.PersonListViewHolder> {

    private MainActivity context;
    private Fragment fragment;
    Person lastClickedItem;
    private ActionMode actionMode;
    private ArrayList<Long> selectedIds;
    private AssetDAO assetDAO;
    private Bundle args;

    PersonListAdapter(MainActivity context, Fragment fragment, Bundle args) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.fragment = fragment;
        selectedIds = new ArrayList<>();
        assetDAO = AssetDAO.getDao();
        this.args = args;
    }

    private static final DiffUtil.ItemCallback<Person> DIFF_CALLBACK = new DiffUtil.ItemCallback<Person>() {
        @Override
        public boolean areItemsTheSame(Person oldItem, Person newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Person oldItem, @NonNull Person newItem) {
            return oldItem.getNameSurname().equals(newItem.getNameSurname());
        }
    };

    @NonNull
    @Override
    public PersonListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (args.getString("listType", "").isEmpty())
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.person_card, parent, false);
        else
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.person_labeling_card, parent, false);
        return new PersonListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final PersonListViewHolder holder, final int position) {
        final Person person = getItem(position);
        if (person != null)
            holder.bindTo(person);
        else
            holder.clear();
    }

    class PersonListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        ImageView personImage, labelingState;
        TextView nameSurname, identityNo;
        NumberProgressBar completionP;
        CircularTextView assetCount;

        PersonListViewHolder(View itemView) {
            super(itemView);
            personImage = itemView.findViewById(R.id.person_card_image);
            nameSurname = itemView.findViewById(R.id.person_card_name_surname);
            identityNo = itemView.findViewById(R.id.person_card_identity_no);
            if (args.getString("listType", "").isEmpty()) {
            completionP = itemView.findViewById(R.id.person_card_counting_progress);
            assetCount = itemView.findViewById(R.id.person_card_asset_count);
            }
            else {
                labelingState = itemView.findViewById(R.id.person_card_status_image);
                labelingState.setOnClickListener(this);
            }

            personImage.setOnClickListener(this);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        void bindTo(Person person) {
            nameSurname.setText(person.getNameSurname());
            if (person.getIdentityNo() != null)
                identityNo.setText(person.getIdentityNo());
            final File imgFile = new File(personPhotosFolder + File.separator + person.getId() + ".jpg");
            if (imgFile.exists()) {
                personImage.setPadding(0, 0, 0, 0);
                Picasso.get().load(imgFile)
                        .resize(128, 128)
                        .centerCrop()
                        .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                        .into(personImage);
            } else {
                if (person.getPersonType() == EPersonType.Recorder)
                    personImage.setImageResource(R.drawable.recorder_gray_96px);
                else
                    personImage.setImageResource(R.drawable.person_gray_96px);
            }
            var bundle = new Bundle();
            bundle.putLong("personId", person.getId());
            if (args.getString("listType", "").isEmpty()) {
            val count = assetDAO.count(bundle);
                assetCount.setSolidColor("#2962FF");
            assetCount.setText(String.valueOf(count));
                if (args.getString("operation").equals("counting"))
                    bundle.putBooleanArray("filterCountingSelected", new boolean[]{false, true});
                else if (args.getString("operation").equals("labeling"))
                    bundle.putBooleanArray("filterLabelSelected", new boolean[]{false, true});
                int rPercent = Math.round((float) assetDAO.count(bundle) / (float) count * 100F);
            completionP.setProgress(rPercent);
                if (actionMode != null) {
                    if (selectedIds.contains(person.getId()))
                        itemView.setBackgroundColor(ContextCompat.getColor(context,
                            R.color.blue_100));
                else
                        itemView.setBackgroundColor(ContextCompat.getColor(context,
                            R.color.white));
                } else if (rPercent == 100)
                    itemView.setBackgroundColor(ContextCompat.getColor(context,
                        R.color.green_200));
            else if (rPercent > 50)
                    itemView.setBackgroundColor(ContextCompat.getColor(context,
                        R.color.green_100));
            else if (rPercent > 0)
                    itemView.setBackgroundColor(ContextCompat.getColor(context,
                        R.color.yellow_100));
            else if (rPercent == 0)
                    itemView.setBackgroundColor(ContextCompat.getColor(context,
                        R.color.white));
        }
            else {
                if (person.getLabelingDateTime() == null)
                    labelingState.setImageResource(R.drawable.no_tag_48px_red);
                else {
                    var cal = Calendar.getInstance();
                    cal.setTime(person.getLabelingDateTime());
                    if (cal.get(Calendar.YEAR) == 1986)
                        labelingState.setImageResource(R.drawable.tag_48px_orange);
                    else
                        labelingState.setImageResource(R.drawable.tag_48px_green);
                }
                if (actionMode != null) {
                    if (selectedIds.contains(person.getId()))
                        itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.light_green_200));
                    else
                        itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
                }
            }
        }

        void clear() {
            nameSurname.setText("");
            identityNo.setText("");
        }

        @Override
        public void onClick(View v) {
            lastClickedItem = getItem(getAdapterPosition());
            if (v.getId() == personImage.getId()) {
                final File imgFile = new File(personPhotosFolder + File.separator + lastClickedItem.getId() + ".jpg");
                if (imgFile.exists()) {
                    var bundle = new Bundle();
                    bundle.putLong("personId", lastClickedItem.getId());
                    context.nav.navigate(R.id.action_personListFragment_to_personImageFragment, bundle);
                }
                else
                    ((ICanShootPhoto) fragment).shootPhoto();
            }
            else if (actionMode == null) {
                if (!args.getString("listType", "").isEmpty() && v.getId() == labelingState.getId()) //person labeling and clicked icon
                {
                    val person = PersonDAO.getDao().get(lastClickedItem.getId());
                    ArrayList<Long> idList = new ArrayList<>();
                    idList.add(person.getId());
                    if (context.rfidManager != null && context.rfidManager.isDeviceOnline()) { //rfid varsa
                        val builder = new AlertDialog.Builder(context);
                        if (person.getLabelingDateTime() == null)
                            builder.setMessage(R.string.clicked_label_not_printed_single_rfid);
                        else
                            builder.setMessage(context.getString(R.string.clicked_label_already_printed_single_rfid, String.valueOf(person.getPersonCode())));
                        builder.setPositiveButton(context.getString(R.string.transfer_to_rfid_tag), (dialog, id) -> {
                            LabelingTabsFragment.setNewTag("C05E2" + String.format(Locale.US, "%019d", person.getId()));
                            Toast.makeText(context, R.string.scan_the_rfid_tag_to_overwrite, Toast.LENGTH_SHORT).show();
                        })
                                .setNeutralButton(context.getString(R.string.print_qrcode), (dialog, id) ->
                                        new LabelPrinter(context, idList, "person", null).print())
                                .setNegativeButton(context.getString(R.string.mark_as_labeled), (dialog, id) -> {
                                    person.setAsLabeled(true);
                                    PersonDAO.getDao().put((Person) person);
                                    PersonDAO.getDao().setLabeledStateChange(person.getId(), person.getLabelingDateTime());
                                })
                                .show();
                    }
                    else {
                        val builder = new AlertDialog.Builder(context);
                        if (person.getLabelingDateTime() == null)
                            builder.setMessage(R.string.clicked_label_not_printed_single)
                                    .setNeutralButton(context.getString(R.string.mark_as_labeled), (dialog, id) -> {
                                        person.setAsLabeled(true);
                                        PersonDAO.getDao().put((Person) person);
                                        PersonDAO.getDao().setLabeledStateChange(person.getId(), person.getLabelingDateTime());
                                    });
                        else
                            builder.setMessage(R.string.clicked_label_already_printed_single)
                                    .setNeutralButton(context.getString(R.string.mark_as_not_labeled), (dialog, id) -> {
                                        person.setAsLabeled(false);
                                        PersonDAO.getDao().put((Person) person);
                                        PersonDAO.getDao().setLabeledStateChange(person.getId(), null);
                                    });

                        builder.setPositiveButton(context.getString(R.string.print_label), (dialog, id) ->
                                new LabelPrinter(context, idList, "person", null).print())
                                .setNegativeButton(context.getString(R.string.cancel_title), (dialog, id) -> dialog.dismiss())
                                .show();
            }
                }
                else {
                var bundle = new Bundle();
                bundle.putLong("personId", lastClickedItem.getId());
                    val op = args.getString("operation");
                    assert op != null;
                    switch (op) {
                        case "counting":
                            context.nav.navigate(R.id.action_personListFragment_to_countingTabsFragment, bundle);
                            break;
                        case "labeling":
                            context.nav.navigate(R.id.action_personListFragment_to_labelingTabsFragment, bundle);
                            break;
                    }
                }
            }
            else
                selectItem(lastClickedItem.getId());
        }

        @Override
        public boolean onLongClick(View v) {
            lastClickedItem = getItem(getAdapterPosition());
            if (lastClickedItem != null)
                selectItem(lastClickedItem.getId());
            return true;
        }
    }

    private void selectItem(Long id) {
        if (actionMode == null) {
            selectedIds.add(id);
            actionMode = context.startSupportActionMode(actionModeCallback);
        }
        else {
            if (selectedIds.contains(id)) {
                selectedIds.remove(id);
                if (selectedIds.size() == 0)
                    actionMode.finish();
            }
            else
                selectedIds.add(id);
        }
        if (actionMode != null)
            actionMode.setTitle(context.getString(R.string.selected_person_count, selectedIds.size()));
        notifyDataSetChanged();
    }

    private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.asset_actions, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.print) {
                new LabelPrinter(context, selectedIds, "person", null).print();
                mode.finish();
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            selectedIds = new ArrayList<>();
            notifyDataSetChanged();
        }
    };

    @Override
    public int getItemCount() {
        return super.getItemCount();
    }
}