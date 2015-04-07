package net.azib.photos.cast;

import android.app.Activity;
import android.content.res.Resources;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

public class PhotoDirsSuggestionAdapter extends ArrayAdapter<String> {
//    protected static final String TAG = "SuggestionAdapter";
    private List<String> suggestions;

    public PhotoDirsSuggestionAdapter(Activity context) {
        super(context, android.R.layout.simple_dropdown_item_1line);
        suggestions = new ArrayList<>();
    }
 
    @Override
    public int getCount() {
        return suggestions.size();
    }
 
    @Override
    public String getItem(int index) {
        return suggestions.get(index);
    }

    public List<String> getSuggestions(String dir) {
        try {
            List<String> result = new ArrayList<>();
            Resources resources = getContext().getResources();
            URL js = new URL(resources.getString(R.string.photos_dirs_url) + "?dir=" + dir + "&accessToken=" + resources.getString(R.string.photos_dirs_access_token));
            URLConnection jc = js.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(jc.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) result.add(line);
            return result;
        }
        catch (Exception e1) {
            return emptyList();
        }
    }
 
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if (constraint != null) {
                    suggestions = getSuggestions(constraint.toString());
                    filterResults.values = suggestions;
                    filterResults.count = suggestions.size();
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence contraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged();
                }
                else {
                    notifyDataSetInvalidated();
                }
            }
        };
    }
 
}