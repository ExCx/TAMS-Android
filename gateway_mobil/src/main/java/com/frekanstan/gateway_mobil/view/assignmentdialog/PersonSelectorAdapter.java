package com.frekanstan.gateway_mobil.view.assignmentdialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.frekanstan.asset_management.data.people.EPersonType;
import com.frekanstan.gateway_mobil.R;
import com.frekanstan.gateway_mobil.data.Person;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import lombok.val;
import lombok.var;

import static com.frekanstan.asset_management.app.helpers.StringExtensions.sFilter;
import static com.frekanstan.asset_management.view.MainActivityBase.personPhotosFolder;

public class PersonSelectorAdapter extends ArrayAdapter<Person>
{
    private List<Person> mList;

    public PersonSelectorAdapter(Context context, int textViewResourceId, List<Person> list)
    {
        super(context, textViewResourceId, list);
        mList = new ArrayList<>(list.size());
        mList.addAll(list);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent)
    {
        View row = convertView;
        LayoutInflater inflater = LayoutInflater.from(getContext());
        if (row == null)
            row = inflater.inflate(R.layout.person_selector, parent, false);
        val person = getItem(position);
        TextView nameSurname = row.findViewById(R.id.person_selector_name_surname);
        nameSurname.setText(person.getNameSurname());
        TextView identityNo = row.findViewById(R.id.person_selector_identity_no);
        if (person.getIdentityNo() != null)
            identityNo.setText(person.getIdentityNo());

        ImageView personImage = row.findViewById(R.id.person_selector_image);
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
        return row;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return mFilter;
    }
    
    private Filter mFilter = new Filter() {
        @Override
        public String convertResultToString(Object resultValue) {
            val person = ((Person)resultValue);
            if (person.getIdentityNo() == null)
                return ((Person)resultValue).getNameSurname();
            else
                return ((Person)resultValue).getNameSurname() + " #" + ((Person)resultValue).getIdentityNo();
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            var results = new FilterResults();

            if (constraint != null) {
                ArrayList<Person> suggestions = new ArrayList<>();
                for (Person person : mList) {
                    if (sFilter(person.getNameSurname(), constraint.toString()) ||
                            sFilter(person.getIdentityNo(), constraint.toString())) {
                        suggestions.add(person);
                    }
                }
                results.values = suggestions;
                results.count = suggestions.size();
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            clear();
            val result = (List<?>) results.values;
            if (result != null) {
                for (Object object : result)
                    add((Person) object);
            }
            notifyDataSetChanged();
        }
    };
}