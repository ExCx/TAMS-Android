package com.frekanstan.tatf_demo.view.assignmentdialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.frekanstan.asset_management.app.locations.LocationIconFinder;
import com.frekanstan.asset_management.data.locations.ELocationType;
import com.frekanstan.tatf_demo.R;
import com.frekanstan.tatf_demo.data.Location;

import java.util.ArrayList;
import java.util.List;

import lombok.val;
import lombok.var;

import static com.frekanstan.asset_management.app.helpers.StringExtensions.sFilter;

public class LocationSelectorAdapter extends ArrayAdapter<Location>
{
    private List<Location> mList;

    public LocationSelectorAdapter(Context context, int textViewResourceId, List<Location> list)
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
            row = inflater.inflate(R.layout.location_selector, parent, false);

        val location = getItem(position);
        TextView name = row.findViewById(R.id.location_selector_name);
        name.setText(location.getName());

        ImageView locationIcon = row.findViewById(R.id.location_selector_icon);
        if (location.getLocationType() == ELocationType.Warehouse)
            locationIcon.setImageResource(R.drawable.database_gray_96px);
        else
            locationIcon.setImageResource(LocationIconFinder.findLocationIconId(location.getName()));
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
            return ((Location)resultValue).getName();
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            var results = new FilterResults();

            if (constraint != null) {
                ArrayList<Location> suggestions = new ArrayList<>();
                for (Location location : mList)
                    if (sFilter(location.getName(), constraint.toString()))
                        suggestions.add(location);
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
                    add((Location) object);
            }
            notifyDataSetChanged();
        }
    };
}